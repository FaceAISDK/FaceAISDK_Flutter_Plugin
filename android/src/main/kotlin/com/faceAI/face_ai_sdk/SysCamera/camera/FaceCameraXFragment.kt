package com.faceAI.face_ai_sdk.SysCamera.camera

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup

import androidx.annotation.NonNull
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

import com.ai.face.base.view.camera.AbsFaceCameraXFragment
import com.ai.face.base.view.camera.CameraXBuilder
import com.faceAI.face_ai_sdk.R
import com.google.common.util.concurrent.ListenableFuture

import java.util.Arrays
import java.util.Collections
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 更高的兼容性改造，2025.12.26。 炫彩活体改造版本基于CameraX 1.4.2，AbsFaceCameraXFragment
 * 低配置设备要加快设备首次启动时间参考配置[com.faceAI.face_ai_sdk.FaceApplication]
 * 定制设备需要工程师根据设备情况调整相机分辨率，宽高比等管理。
 *
 * @author FaceAISDK.Service@gmail.com
 */
class FaceCameraXFragment : AbsFaceCameraXFragment() {

    companion object {
        private const val KEY_LINEAR_ZOOM = "CAMERA_LINEAR_ZOOM"
        private const val KEY_LENS_FACING = "CAMERA_LENS_FACING"
        private const val KEY_ROTATION = "CAMERA_ROTATION"
        private const val KEY_IS_HIGH_RES = "CAMERA_SIZE_HIGH"
        private const val TAG = "FaceCameraXFragment"

        @JvmStatic
        fun newInstance(builder: CameraXBuilder): FaceCameraXFragment {
            val fragment = FaceCameraXFragment()
            val args = Bundle()
            args.putInt(KEY_LENS_FACING, builder.cameraLensFacing)
            args.putFloat(KEY_LINEAR_ZOOM, builder.linearZoom)
            args.putInt(KEY_ROTATION, builder.rotation)
            args.putBoolean(KEY_IS_HIGH_RES, builder.cameraSizeHigh)
            fragment.arguments = args
            return fragment
        }
    }

    // 配置参数
    private var mCameraLensFacing = CameraSelector.LENS_FACING_FRONT
    private var mLinearZoom = 0f
    private var mRotation = Surface.ROTATION_0
    private var isHighResolution = false

    // 运行时状态
    @Volatile
    private var mImageWidth = 0
    @Volatile
    private var mImageHeight = 0

    // CameraX 组件
    private var mCameraProvider: ProcessCameraProvider? = null
    private lateinit var mCameraControl: CameraControl
    private lateinit var mPreviewView: PreviewView
    private var mExecutorService: ExecutorService? = null
    private var mAnalyzeListener: onAnalyzeData? = null

    fun setOnAnalyzerListener(callback: onAnalyzeData) {
        this.mAnalyzeListener = callback
    }

    interface onAnalyzeData {
        //用于SDK内部数据分析
        fun analyze(@NonNull imageProxy: ImageProxy)
        //回调图片帧大小，以便画框UI处理
        fun backImageSize(imageWidth: Int, imageHeight: Int) {}
    }

    override fun getCameraControl(): CameraControl {
        return mCameraControl
    }

    override fun getCameraLensFacing(): Int {
        return mCameraLensFacing
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            mCameraLensFacing = args.getInt(KEY_LENS_FACING, CameraSelector.LENS_FACING_FRONT)
            mLinearZoom = args.getFloat(KEY_LINEAR_ZOOM, 0f)
            mRotation = args.getInt(KEY_ROTATION, Surface.ROTATION_0)
            isHighResolution = args.getBoolean(KEY_IS_HIGH_RES, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.face_camerax_fragment, container, false)
        mPreviewView = rootView.findViewById(R.id.previewView)
        initCameraX()
        return rootView
    }

    override fun onDestroy() {
        super.onDestroy()
        //关闭线程池，防止内存泄漏
        if (mExecutorService != null && !mExecutorService!!.isShutdown) {
            mExecutorService!!.shutdown()
        }
    }

    private fun initCameraX() {
        mImageWidth = 0
        mImageHeight = 0

        mExecutorService = Executors.newSingleThreadExecutor()
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            if (!isAdded || context == null) {
                return@addListener
            }
            try {
                mCameraProvider = cameraProviderFuture.get()
                // --- 新增：打印支持的分辨率看看 ---
                // printSupportedResolutions(mCameraProvider!!, mCameraLensFacing)
                // ---------------------------------
                bindCameraUseCases()
            } catch (e: ExecutionException) {
                Log.e(TAG, "CameraProvider init failed", e)
            } catch (e: InterruptedException) {
                Log.e(TAG, "CameraProvider init failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        // 1. 配置 ImageAnalysis
        val analysisBuilder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setTargetRotation(mRotation)

        if (isHighResolution) {
            // 远距离识别，但是性能会下降，定制设备需要配置摄像头支持的分辨率。请开发工程师切换后调试效果！
            analysisBuilder.setTargetResolution(Size(1280, 720))
        } else {
            // 默认场景，性能优先
            analysisBuilder.setTargetAspectRatio(AspectRatio.RATIO_4_3)
        }
        val mImageAnalysis = analysisBuilder.build()

        // 2. 配置 Preview
        val mPreview = Preview.Builder()
            .setTargetRotation(mRotation)
            .build()

        // 3. 配置 PreviewView
        mPreviewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        mPreviewView.scaleType = PreviewView.ScaleType.FIT_CENTER
        mPreview.setSurfaceProvider(mPreviewView.surfaceProvider)

        // 4. 构建 CameraSelector (兼容逻辑)
        val mCameraSelector = createCompatibleCameraSelector()
        //记住当前的摄像头，下次启动的时候不再搜索可以加快启动速度 setAvailableCamerasLimiter

        // 5. 设置分析器
        mImageAnalysis.setAnalyzer(mExecutorService!!) { imageProxy ->
            // 这里位于子线程
            if (mImageWidth == 0 || mImageHeight == 0) {
                mImageWidth = imageProxy.width
                mImageHeight = imageProxy.height
                mAnalyzeListener?.backImageSize(mImageWidth, mImageHeight)
            }

            mAnalyzeListener?.analyze(imageProxy)
            // 必须关闭，否则不会收到下一帧
            imageProxy.close()
        }

        // 6. 绑定生命周期
        try {
            mCameraProvider!!.unbindAll()
            val mCamera = mCameraProvider!!.bindToLifecycle(
                viewLifecycleOwner,
                mCameraSelector,
                mPreview,
                mImageAnalysis
            )

            mCameraControl = mCamera.cameraControl
            mCameraControl.setLinearZoom(mLinearZoom)
        } catch (e: Exception) {
            Log.e(TAG, "Camera bind failed: ${e.message}")
        }
    }

    /**
     * 构建兼容的 CameraSelector
     * 针对部分 RK 设备/工控机接口定义混乱的情况进行降级处理
     */
    private fun createCompatibleCameraSelector(): CameraSelector {
        val builder = CameraSelector.Builder()

        val preferredLensFacing = mCameraLensFacing
        // 另一种可能的 LensFacing (如果首选是 Front，备选就是 Back)
        val fallbackLensFacing = if (preferredLensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }

        if (hasCamera(mCameraProvider!!, preferredLensFacing)) {
            // 1. 完美匹配
            builder.requireLensFacing(preferredLensFacing)
        } else if (hasCamera(mCameraProvider!!, fallbackLensFacing)) {
            // 2. 降级匹配：找不到指定方向，就用另一个方向
            Log.w(TAG, "Preferred camera not found, fallback to opposite facing.")
            builder.requireLensFacing(fallbackLensFacing)
        } else {
            // 3. 暴力兜底：都找不到（可能是 External USB 摄像头），不过滤，接受任意摄像头
            Log.w(TAG, "Standard cameras not found, allowing ALL cameras (External/USB).")
            builder.addCameraFilter { cameraInfos -> cameraInfos }
        }

        return builder.build()
    }

    private fun hasCamera(provider: ProcessCameraProvider, lensFacing: Int): Boolean {
        return try {
            provider.hasCamera(CameraSelector.Builder().requireLensFacing(lensFacing).build())
        } catch (e: Exception) {
            false
        }
    }

    fun isFrontCamera(): Boolean {
        return mCameraLensFacing == CameraSelector.LENS_FACING_FRONT
    }

    /**
     * 打印或获取指定摄像头支持的分辨率列表
     * @param provider ProcessCameraProvider 实例
     * @param lensFacing 摄像头方向 (CameraSelector.LENS_FACING_FRONT / BACK)
     */
    private fun printSupportedResolutions(provider: ProcessCameraProvider, lensFacing: Int) {
        try {
            // 1. 筛选出目标摄像头（前置或后置）的 CameraInfo
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            // 注意：provider.getAvailableCameraInfos() 返回的是所有摄像头的列表
            val cameraInfos = provider.availableCameraInfos

            for (info in cameraInfos) {
                // 2. 检查这个 info 是否符合我们想要的方向
                try {
                    if (selector.filter(Collections.singletonList(info)).isEmpty()) {
                        continue // 不是我们想要的摄像头，跳过
                    }
                } catch (e: Exception) {
                    continue
                }

                // 3. 【关键步骤】将 CameraX 的 CameraInfo 转为 Camera2 的对象
                val camera2Info = Camera2CameraInfo.from(info)

                // 4. 获取 StreamConfigurationMap (包含所有尺寸信息)
                val map = camera2Info.getCameraCharacteristic(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                ) ?: continue

                // 5. 获取特定格式的尺寸列表
                // 对于 ImageAnalysis，通常关心 ImageFormat.YUV_420_888
                // 对于 预览 Preview，通常关心 SurfaceTexture.class
                val sizes = map.getOutputSizes(ImageFormat.YUV_420_888)

                if (sizes != null) {
                    Log.d("CameraResolution", "--- 摄像头 $lensFacing 支持的 YUV 分辨率 ---")
                    // 排序，方便查看（从大到小）
                    Arrays.sort(sizes) { o1, o2 ->
                        Integer.compare(o2.width * o2.height, o1.width * o1.height)
                    }

                    for (size in sizes) {
                        Log.d(
                            "CameraResolution", "Size: ${size.width} x ${size.height}" +
                                    " (比例: ${String.format("%.2f", size.width.toFloat() / size.height)})"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CameraResolution", "获取分辨率失败", e)
        }
    }
}
