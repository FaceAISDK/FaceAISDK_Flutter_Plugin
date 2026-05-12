package com.faceAI.face_ai_sdk.base.view

import com.faceAI.face_ai_sdk.R
import com.faceAI.face_ai_sdk.base.utils.ScreenUtils
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.min

/**
 * 人脸识别覆盖视图
 * - 半透明遮罩 + 圆形镂空（支持展开动画）
 * - 环形进度条（渐变色）
 * - 上方双行提示文本（支持淡入淡出）
 */
class FaceVerifyCoverView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // ==================== 可配置属性 ====================
    private var mFlashColor: Int = Color.WHITE
    private val mStartColor: Int
    private val mEndColor: Int
    private val mShowProgress: Boolean
    private val mTipTextColor: Int
    private val mTipTextBgColor: Int
    private val mTipTextSize: Float
    private var mCircleMargin = -1
    private var mCirclePaddingBottom = 0

    // ==================== 几何参数 ====================
    private val mCenterPoint = PointF()
    private var mTargetRadius = 0f
    private var mCurrentRadius = 0f
    private var mBgArcWidth = 0f

    // ==================== 绘图对象 ====================
    private val mBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mBgArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTipsPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mSecondTipsPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTextBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mHolePath = Path()
    private val mFullRect = RectF()
    private val mArcRectF = RectF()
    private val mTextBgRect = RectF()
    private val mGradientMatrix = Matrix()

    // ==================== 文本与动画 ====================
    private var mTipsText = ""
    private var mSecondTipsText = ""
    private var mTipsWidth = 0f
    private var mSecondTipsWidth = 0f

    private var mSecondTipsAlpha = 0
    private var mTargetSecondAlpha = -1 // 记录动画目标值，避免高频打断
    private var mSecondTipsAnimator: ValueAnimator? = null

    private var mTextSpacing = 0f
    private var mTextPaddingHorizontal = 0f
    private var mTextPaddingVertical = 0f
    private var mTextBgRadius = 0f

    private var mOpenAnimator: ValueAnimator? = null
    private var mCurrentProgressAngle = 0f

    init {
        val array: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.FaceVerifyCoverView)

        // 读取通用属性
        mFlashColor = array.getColor(R.styleable.FaceVerifyCoverView_flash_color, Color.WHITE)
        mStartColor = array.getColor(R.styleable.FaceVerifyCoverView_progress_start_color, Color.LTGRAY)
        mEndColor = array.getColor(R.styleable.FaceVerifyCoverView_progress_end_color, Color.LTGRAY)
        mShowProgress = array.getBoolean(R.styleable.FaceVerifyCoverView_show_progress, true)

        // 读取文字属性
        mTipTextColor = array.getColor(R.styleable.FaceVerifyCoverView_tip_text_color, Color.WHITE)
        mTipTextBgColor = array.getColor(
            R.styleable.FaceVerifyCoverView_tip_text_bg_color,
            ContextCompat.getColor(context, R.color.face_main_color)
        )

        // 默认 19sp
        val defaultTextSize = 19 * context.resources.displayMetrics.scaledDensity
        mTipTextSize = array.getDimension(R.styleable.FaceVerifyCoverView_tip_text_size, defaultTextSize)

        array.recycle()
        initPaints(context)
    }

    private fun initPaints(context: Context) {
        mBgArcWidth = ScreenUtils.dp2px(context, 2f).toFloat()

        mBackgroundPaint.apply {
            color = mFlashColor
            style = Paint.Style.FILL
        }

        mBgArcPaint.apply {
            color = ContextCompat.getColor(context, R.color.half_grey)
            style = Paint.Style.STROKE
            strokeWidth = mBgArcWidth
            strokeCap = Paint.Cap.ROUND
        }

        mProgressPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = mBgArcWidth
            strokeCap = Paint.Cap.ROUND
        }

        mTipsPaint.apply {
            color = mTipTextColor
            textSize = mTipTextSize
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        mSecondTipsPaint.apply {
            color = mTipTextColor
            textSize = mTipTextSize * 0.88f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        mTextBgPaint.apply {
            color = mTipTextBgColor
            style = Paint.Style.FILL
        }

        mTextSpacing = ScreenUtils.dp2px(context, 7f).toFloat()
        mTextPaddingHorizontal = ScreenUtils.dp2px(context, 17f).toFloat()
        mTextPaddingVertical = ScreenUtils.dp2px(context, 5f).toFloat()
        mTextBgRadius = ScreenUtils.dp2px(context, 20f).toFloat()

        setTipsText(R.string.keep_face_tips)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateLayoutParameters(w, h)
    }

    private fun updateLayoutParameters(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return

        mFullRect.set(0f, 0f, w.toFloat(), h.toFloat())
        val shorterSide = min(w, h)

        // 动态调整 Margin Size
        MARGIN_SIZE = if (w > h) 5 else 8

        if (mCircleMargin < 0) mCircleMargin = shorterSide / MARGIN_SIZE

        val basePadding = shorterSide / MARGIN_SIZE
        mCirclePaddingBottom = if (w > h) 0 else basePadding

        mCenterPoint.set(w / 2f, (h / 2f) - mCirclePaddingBottom.toFloat())
        mTargetRadius = (shorterSide / 2f) - mCircleMargin.toFloat()

        val halfStroke = mBgArcWidth / 2f
        mArcRectF.set(
            mCenterPoint.x - mTargetRadius - halfStroke,
            mCenterPoint.y - mTargetRadius - halfStroke,
            mCenterPoint.x + mTargetRadius + halfStroke,
            mCenterPoint.y + mTargetRadius + halfStroke
        )

        val sweepGradient = SweepGradient(mCenterPoint.x, mCenterPoint.y, mStartColor, mEndColor)
        mGradientMatrix.setRotate(START_ANGLE.toFloat(), mCenterPoint.x, mCenterPoint.y)
        sweepGradient.setLocalMatrix(mGradientMatrix)
        mProgressPaint.shader = sweepGradient

        if (mOpenAnimator == null || !mOpenAnimator!!.isRunning) {
            mCurrentRadius = mTargetRadius
        }
    }

    override fun onDraw(canvas: Canvas) {
        mHolePath.reset()
        mHolePath.addRect(mFullRect, Path.Direction.CW)
        if (mCurrentRadius > 0) {
            mHolePath.addCircle(mCenterPoint.x, mCenterPoint.y, mCurrentRadius, Path.Direction.CW)
        }
        mHolePath.fillType = Path.FillType.EVEN_ODD
        canvas.drawPath(mHolePath, mBackgroundPaint)

        if (mShowProgress) {
            canvas.drawArc(mArcRectF, START_ANGLE.toFloat(), MAX_ANGLE.toFloat(), false, mBgArcPaint)
            canvas.drawArc(mArcRectF, START_ANGLE.toFloat(), mCurrentProgressAngle, false, mProgressPaint)
        }

        drawTipsText(canvas)
    }

    private fun drawTipsText(canvas: Canvas) {
        val hasFirst = mTipsText.isNotEmpty()
        val hasSecond = mSecondTipsAlpha > 0 && mSecondTipsText.isNotEmpty()
        if (!hasFirst && !hasSecond) return

        val secondTipsY = mCenterPoint.y - mTargetRadius - mTextSpacing - mTextPaddingVertical - mSecondTipsPaint.descent()
        val secondBgTop = secondTipsY + mSecondTipsPaint.ascent() - mTextPaddingVertical
        val firstTipsY = secondBgTop - mTextSpacing - mTextPaddingVertical - mTipsPaint.descent()

        if (hasSecond) {
            val savedAlpha = mTextBgPaint.alpha
            mSecondTipsPaint.alpha = mSecondTipsAlpha
            mTextBgPaint.alpha = mSecondTipsAlpha
            drawTextWithBackground(canvas, mSecondTipsText, mSecondTipsWidth, mCenterPoint.x, secondTipsY, mSecondTipsPaint)
            mTextBgPaint.alpha = savedAlpha
        }

        if (hasFirst) {
            drawTextWithBackground(canvas, mTipsText, mTipsWidth, mCenterPoint.x, firstTipsY, mTipsPaint)
        }
    }

    private fun drawTextWithBackground(canvas: Canvas, text: String, textWidth: Float, x: Float, y: Float, paint: Paint) {
        val bgWidth = textWidth + (mTextPaddingHorizontal * 2f)
        mTextBgRect.set(
            x - (bgWidth / 2f),
            y + paint.ascent() - mTextPaddingVertical,
            x + (bgWidth / 2f),
            y + paint.descent() + mTextPaddingVertical
        )
        canvas.drawRoundRect(mTextBgRect, mTextBgRadius, mTextBgRadius, mTextBgPaint)
        canvas.drawText(text, x, y, paint)
    }

    // ==================== 公开 API ====================

    fun setTipsText(tips: String?) {
        val newTips = tips ?: ""
        if (this.mTipsText == newTips) return
        this.mTipsText = newTips
        this.mTipsWidth = mTipsPaint.measureText(mTipsText)
        if (mTipsText.isNotEmpty()) {
            mSecondTipsAnimator?.cancel()
            mSecondTipsAlpha = 0
            mTargetSecondAlpha = 0
        }
        invalidate()
    }

    fun setTipsText(@StringRes resId: Int) {
        setTipsText(if (resId == 0) "" else context.getString(resId))
    }

    fun setSecondTipsText(tips: String?) {
        if (tips.isNullOrEmpty()) {
            animateSecondTipsAlpha(0)
            return
        }
        if (tips == mSecondTipsText) {
            animateSecondTipsAlpha(255)
            return
        }
        this.mSecondTipsText = tips
        this.mSecondTipsWidth = mSecondTipsPaint.measureText(mSecondTipsText)
        animateSecondTipsAlpha(255)
    }

    fun setSecondTipsText(@StringRes resId: Int) {
        setSecondTipsText(if (resId == 0) "" else context.getString(resId))
    }

    fun setProgress(percent: Float) {
        if (!mShowProgress) return
        mCurrentProgressAngle = min(MAX_ANGLE.toFloat() * percent, MAX_ANGLE.toFloat())
        invalidate()
    }

    fun setFlashColor(@ColorInt color: Int) {
        if (mFlashColor != color) {
            mFlashColor = color
            mBackgroundPaint.color = mFlashColor
            invalidate()
        }
    }

    fun setCirclePaddingBottom(paddingBottom: Int) {
        if (this.mCirclePaddingBottom != paddingBottom) {
            this.mCirclePaddingBottom = paddingBottom
            updateLayoutParameters(width, height)
            invalidate()
        }
    }

    fun setMargin(newMargin: Int) {
        if (this.mCircleMargin != newMargin) {
            this.mCircleMargin = newMargin
            updateLayoutParameters(width, height)
            invalidate()
        }
    }

    // ==================== 动画处理 ====================

    private fun animateSecondTipsAlpha(targetAlpha: Int) {
        if (mSecondTipsAlpha == targetAlpha || mTargetSecondAlpha == targetAlpha) return
        mTargetSecondAlpha = targetAlpha
        mSecondTipsAnimator?.cancel()

        val baseDuration = if (targetAlpha == 255) 200 else 400
        val duration = (abs(targetAlpha - mSecondTipsAlpha).toFloat() / 255f * baseDuration).toLong()

        mSecondTipsAnimator = ValueAnimator.ofInt(mSecondTipsAlpha, targetAlpha).apply {
            setDuration(maxOf(duration, 50L))
            addUpdateListener { animation ->
                mSecondTipsAlpha = animation.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    private fun startOpenAnimation() {
        if (mOpenAnimator?.isRunning == true) {
            mOpenAnimator?.cancel()
        }
        mOpenAnimator = ValueAnimator.ofFloat(0f, mTargetRadius).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                mCurrentRadius = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // ==================== 生命周期 ====================

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
        mOpenAnimator?.apply {
            cancel()
            mOpenAnimator = null
        }
        mSecondTipsAnimator?.apply {
            cancel()
            mSecondTipsAnimator = null
        }
    }

    companion object {
        var MARGIN_SIZE = 8
        private const val START_ANGLE = 270
        private const val MAX_ANGLE = 360
    }
}