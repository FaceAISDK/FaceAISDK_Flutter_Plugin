package com.faceAI.face_ai_sdk.base.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

import androidx.annotation.NonNull
import androidx.core.content.ContextCompat

import com.faceAI.face_ai_sdk.R
import com.faceAI.face_ai_sdk.base.utils.ScreenUtils
import kotlin.math.max
import kotlin.math.min

/**
 * [性能优化版] 人脸识别覆盖视图
 * 优化点：移除 saveLayer 离屏渲染，改用 Path 奇偶填充规则实现挖孔
 */
class FaceVerifyCoverView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- 核心属性 ---
    private var mFlashColor: Int
    private var mStartColor: Int
    private var mEndColor: Int
    private var mShowProgress: Boolean
    private var mCircleMargin: Int
    private var mCirclePaddingBottom: Int

    // --- 尺寸相关 ---
    private val mCenterPoint = Point()
    private var mTargetRadius = 0f
    private var mCurrentRadius = 0f
    private var mBgArcWidth = 0f

    // --- 绘制对象 ---
    private lateinit var mBackgroundPaint: Paint   // 背景画笔
    private lateinit var mBgArcPaint: Paint        // 进度条底色画笔
    private lateinit var mProgressPaint: Paint     // 进度条画笔

    // 优化：使用 Path 实现挖孔，代替 Xfermode
    private val mHolePath = Path()
    private val mFullRect = RectF() // 视图全屏区域
    private val mArcRectF = RectF() // 进度条区域

    private lateinit var mSweepGradient: SweepGradient
    private val mGradientMatrix = Matrix() // 用于旋转渐变

    // --- 动画 ---
    private var mOpenAnimator: ValueAnimator? = null
    private var mCurrentProgressAngle = 0f

    companion object {
        // 调整起始角度：270度代表从12点钟方向开始
        private const val START_ANGLE = 270
        private const val MAX_ANGLE = 360
    }

    init {
        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.FaceVerifyCoverView)
            mCircleMargin = array.getDimensionPixelSize(R.styleable.FaceVerifyCoverView_circle_margin, 30)
            mCirclePaddingBottom = array.getDimensionPixelSize(R.styleable.FaceVerifyCoverView_circle_padding_bottom, 0)
            mFlashColor = array.getColor(R.styleable.FaceVerifyCoverView_flash_color, Color.WHITE)
            mStartColor = array.getColor(R.styleable.FaceVerifyCoverView_progress_start_color, Color.LTGRAY)
            mEndColor = array.getColor(R.styleable.FaceVerifyCoverView_progress_end_color, Color.LTGRAY)
            mShowProgress = array.getBoolean(R.styleable.FaceVerifyCoverView_show_progress, true)
            array.recycle()
        } else {
            mCircleMargin = 33
            mCirclePaddingBottom = 0
            mFlashColor = Color.WHITE
            mStartColor = Color.LTGRAY
            mEndColor = Color.LTGRAY
            mShowProgress = true
        }

        initPaints(context)
    }

    private fun initPaints(context: Context) {
        mBgArcWidth = ScreenUtils.dp2px(context, 2f).toFloat()

        // 1. 背景画笔 (直接画出带孔的背景)
        mBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mFlashColor
            style = Paint.Style.FILL
        }

        // 2. 进度条底色
        mBgArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.half_grey)
            style = Paint.Style.STROKE
            strokeWidth = mBgArcWidth
            strokeCap = Paint.Cap.ROUND
        }

        // 3. 进度条
        mProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = mBgArcWidth
            strokeCap = Paint.Cap.ROUND
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mCircleMargin = mCircleMargin + getScreenAspectRatioDx()

        // 记录全屏范围
        mFullRect.set(0f, 0f, w.toFloat(), h.toFloat())

        // 计算圆心 X Y
        mCenterPoint.x = w / 2
        mCenterPoint.y = h / 2 - mCirclePaddingBottom

        // 最大半径
        mTargetRadius = min(w / 2f, h / 2f) - mCircleMargin

        // 预计算进度条区域
        val halfStroke = mBgArcWidth / 2f
        mArcRectF.set(
            mCenterPoint.x - mTargetRadius - halfStroke,
            mCenterPoint.y - mTargetRadius - halfStroke,
            mCenterPoint.x + mTargetRadius + halfStroke,
            mCenterPoint.y + mTargetRadius + halfStroke
        )

        // 初始化渐变 & 矩阵旋转
        updateGradient()
    }

    /**
     * 根据屏幕宽高比计算 0-10 的评分
     * 10分 = 16:9 (传统屏幕，较短)
     * 0分  = 20:9 (全面屏，很长)
     * @return 0 到 10 之间的整数
     */
    fun getScreenAspectRatioDx(): Int {
        val score: Float
        val height = max(height, width)
        val width = min(height, width)

        // 1. 计算当前宽高比 (注意转为 float)
        val currentRatio = width.toFloat() / height

        // 2. 定义阈值
        val RATIO_9_16 = 9f / 16f // 0.5625 (较宽/短) -> 对应 10 分
        val RATIO_9_20 = 9f / 20f // 0.4500 (较窄/长) -> 对应 0 分

        // 3. 处理边界情况
        score = if (currentRatio >= RATIO_9_16) {
            12f
        } else if (currentRatio <= RATIO_9_20) {
            0f
        } else {
            // 4. 线性插值计算
            val range = RATIO_9_16 - RATIO_9_20
            val offset = currentRatio - RATIO_9_20
            (offset / range) * 12
        }
        val scale = resources.displayMetrics.density
        return (score * scale + 0.5f).toInt()
    }

    private fun updateGradient() {
        mSweepGradient = SweepGradient(mCenterPoint.x.toFloat(), mCenterPoint.y.toFloat(), mStartColor, mEndColor)
        // 旋转渐变，使其起始颜色对齐到 START_ANGLE (270度)
        mGradientMatrix.setRotate(START_ANGLE.toFloat(), mCenterPoint.x.toFloat(), mCenterPoint.y.toFloat())
        mSweepGradient.setLocalMatrix(mGradientMatrix)
        mProgressPaint.shader = mSweepGradient
    }

    override fun onDraw(canvas: Canvas) {
        // 1. 绘制带"挖孔"的背景 (高性能方式)
        drawHollowBackgroundPath(canvas)

        // 2. 绘制进度条
        if (mShowProgress) {
            // 绘制底色圆环
            canvas.drawArc(mArcRectF, START_ANGLE.toFloat(), MAX_ANGLE.toFloat(), false, mBgArcPaint)
            // 绘制彩色进度 (Shader已在onSizeChanged处理)
            canvas.drawArc(mArcRectF, START_ANGLE.toFloat(), mCurrentProgressAngle, false, mProgressPaint)
        }
    }

    /**
     * 使用 Path.FillType.EVEN_ODD 实现挖孔
     * 原理：路径包含一个大矩形和一个小圆。奇偶规则下，重叠区域不填充。
     */
    private fun drawHollowBackgroundPath(canvas: Canvas) {
        mHolePath.reset()
        // 添加全屏矩形
        mHolePath.addRect(mFullRect, Path.Direction.CW)
        // 添加中间圆 (半径为动画值)
        if (mCurrentRadius > 0) {
            mHolePath.addCircle(mCenterPoint.x.toFloat(), mCenterPoint.y.toFloat(), mCurrentRadius, Path.Direction.CW)
        }
        // 关键：设置填充模式为奇偶填充
        mHolePath.fillType = Path.FillType.EVEN_ODD

        canvas.drawPath(mHolePath, mBackgroundPaint)
    }

    // --- 动画与控制 ---

    fun setProgress(percent: Float) {
        if (!mShowProgress) return
        val targetAngle = MAX_ANGLE * percent
        this.mCurrentProgressAngle = min(targetAngle, MAX_ANGLE.toFloat())
        invalidate()
    }

    /**
     * 炫彩活体更新屏幕颜色，人脸要离屏幕近一点
     */
    fun setFlashColor(color: Int) {
        mFlashColor = color
        mBackgroundPaint.color = mFlashColor // 记得更新画笔
        invalidate()
    }

    // 更新底部间距
    fun setCirclePaddingBottom(paddingBottom: Int) {
        if (this.mCirclePaddingBottom != paddingBottom) {
            this.mCirclePaddingBottom = paddingBottom
            requestLayout()
        }
    }

    fun setMargin(newMargin: Int) {
        if (this.mCircleMargin != newMargin) {
            mCircleMargin = newMargin
            requestLayout()
        }
    }

    private fun startOpenAnimation() {
        if (mOpenAnimator != null && mOpenAnimator!!.isRunning) {
            mOpenAnimator!!.cancel()
        }
        mOpenAnimator = ValueAnimator.ofFloat(0f, mTargetRadius).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                mCurrentRadius = animation.animatedValue as Float
                invalidate()
            }
        }
        mOpenAnimator!!.start()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            post { startOpenAnimation() }
        } else {
            mOpenAnimator?.cancel()
            mCurrentRadius = 0f
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mOpenAnimator != null) {
            mOpenAnimator!!.cancel()
            mOpenAnimator = null
        }
    }
}
