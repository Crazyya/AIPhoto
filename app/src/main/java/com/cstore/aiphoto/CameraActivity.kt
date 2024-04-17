package com.cstore.aiphoto

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cstore.aiphoto.databinding.ActivityCameraBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random.Default.nextInt


/**
 * Created by zhiya.zhang
 * on 2022/2/16 9:33.
 */
class CameraActivity : AppCompatActivity() {
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewBinding: ActivityCameraBinding
    private lateinit var outputDirectory: File
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var vm: CameraViewModel

    private val connText = "链接信息:"
    private val pickText = "拍照状态:"
    private val sendText = "上传状态:"
    private fun getOutputDirectory(): File {
        val fileDir = this.externalCacheDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if(fileDir != null && fileDir.exists()) {
            fileDir
        } else {
            this.filesDir
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        val ip = intent.getStringExtra("ip") ?: "192.168.7.88"
        vm = ViewModelProvider(this)[CameraViewModel::class.java]
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewBinding.viewFinder.post {
            setUpCamera()
        }
        vm.mState.observe(this) {
            Log.e("Act", "State:${it}")
            if("start" in it) {
                val t = pickText + "拍照"
                viewBinding.pickState.text = t
                takePhoto(it)
            } else if(it == "update") {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    installPermission()
                } else {
                    FileUtil.installApk(DownloadUtil.apkFilePath, "com.cstore.aiphoto.fileprovider")
                }
            }
        }
        vm.updateState.observe(this) {
            Log.e("Act", "Start Install APK!")
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                installPermission()
            } else {
                FileUtil.installApk(DownloadUtil.apkFilePath, "com.cstore.aiphoto.fileprovider")
            }
        }
        vm.sendState.observe(this) {
            viewBinding.sendState.text = it
        }
        vm.connState.observe(this) {
            if(it) {
                viewBinding.tryConn.visibility = View.GONE
            } else {
//                viewBinding.tryConn.visibility = View.VISIBLE
                vm.tryConn(ip)
            }
        }
        vm.socketMsg.observe(this) {
            val t = connText + it
            viewBinding.connState.text = t
        }
        vm.socketMsg.observe(this) {
            viewBinding.msg.text = it
        }
        vm.sendCount.observe(this) {
            try {
                val msg = "已拍:${vm.picCount.value} 已上传:${vm.sendCount.value}"
                viewBinding.countState.text = msg
            } catch(_: Exception) {

            }
        }
        vm.picCount.observe(this) {
            try {
                val msg = "已拍:${vm.picCount.value} 已上传:${vm.sendCount.value}"
                viewBinding.countState.text = msg
            } catch(_: Exception) {

            }
        }
        viewBinding.tryConn.setOnClickListener {
            vm.tryConn(ip)
        }
        if(!hasPermissions(this)) {
            val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if(it) { // Take the user to the success fragment when permission is granted
                    Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
                    vm.connectSocket(ip)
                } else {
                    Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
                }
            }
            permLauncher.launch(Manifest.permission.CAMERA)
        } else {
            vm.connectSocket(ip)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val registLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        installPermission()
    }

    //    @RequiresApi(Build.VERSION_CODES.O)
    private fun installPermission() {
        FileUtil.installApk(DownloadUtil.apkFilePath, "com.cstore.aiphoto.fileprovider")/*val hip = this.packageManager.canRequestPackageInstalls()
        if (hip) {
            FileUtil.installApk(DownloadUtil.apkFilePath,
                                "com.cstore.aiphoto.fileprovider")
        } else {
            val packageUri = Uri.fromParts("package",
                                           packageName,
                                           null)
            val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                           packageUri)
            registLauncher.launch(i)
            Toast.makeText(this,
                           "请开启安装未知应用权限",
                           Toast.LENGTH_LONG)
                    .show()
        }*/
    }

    private fun getSelectLocation(): String {
        return (viewBinding.radio.cb1.takeIf { it.isChecked }?.text ?: "").toString() + (viewBinding.radio.cb2.takeIf { it.isChecked }?.text ?: "").toString() + (viewBinding.radio.cb3.takeIf { it.isChecked }?.text ?: "").toString() + (viewBinding.radio.cb4.takeIf { it.isChecked }?.text ?: "").toString() + (viewBinding.radio.cb5.takeIf { it.isChecked }?.text ?: "").toString() + (viewBinding.radio.cb6.takeIf { it.isChecked }?.text ?: "").toString() + (viewBinding.radio.cb7.takeIf { it.isChecked }?.text ?: "").toString()
    }

    private val model = Build.MODEL
    fun getValue(data: List<String>, index: Int): String {
        return try {
            data[index]
        } catch(e: Exception) {
            ""
        }
    }

    private fun takePhoto(value: String) {
        val values = value.split("$")
        val light = getValue(values, 1)
        val barcode = getValue(values, 2)
        val location = getValue(values, 3)
        val zp = getValue(values, 4)
        val group = getValue(values, 5)
        val tzid = getValue(values, 6)
        val phoneTag = viewBinding.phoneTag.text.toString().takeIf { it.isNotEmpty() } ?: "Null"
        imageCapture?.let { imageCapture ->
            val fileName = "${phoneTag}_${model}_${
                SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.CHINA).format(System.currentTimeMillis())
            }_${
                nextInt(10000, 99999)
            }_${getSelectLocation()}_${location}.jpg"

            val photoFile = File(outputDirectory, fileName)
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val saveUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                    // 测试
//                    vm.sendFile(saveUri)
                    vm.sendFile(saveUri, light, barcode, phoneTag, zp, group, tzid)
                    val t = pickText + "等待"
                    MainScope().launch {
                        vm.picCount.postValue((vm.picCount.value ?: 0) + 1)
                        viewBinding.pickState.text = t
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    val t = pickText + "异常！:" + exception.toString()
                    MainScope().launch {
                        viewBinding.pickState.text = t
                    }
                }

            })
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(kotlinx.coroutines.Runnable {
            cameraProvider = cameraProviderFuture.get()
            lifecycleScope.launch {
                bindCameraUseCases()
            }
        }, ContextCompat.getMainExecutor(this))

    }

    @SuppressLint("UnsafeOptInUsageError")
    private suspend fun bindCameraUseCases() {
        val extensionsManager = ExtensionsManager.getInstanceAsync(this, cameraProvider!!).await()
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
        } // 效果：CAPTURE_MODE_MAXIMIZE_QUALITY   速度：CAPTURE_MODE_MINIMIZE_LATENCY
        imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
        try {
            cameraProvider?.also {
                cameraProvider?.unbindAll()
                val camera = if(extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.AUTO)) {
                    Log.e("Camera", "自动最高")
                    val bokehCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(cameraSelector, ExtensionMode.AUTO)
                    it.bindToLifecycle(this, bokehCameraSelector, preview, imageCapture)
                } else {
                    Log.e("Camera", "无法生效")
                    it.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                }
                val mCameraInfo = camera.cameraInfo
                val mCameraControl = camera.cameraControl
                initCameraListener(mCameraInfo, mCameraControl)
            }
        } catch(e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    private fun initCameraListener(mCameraInfo: CameraInfo, mCameraControl: CameraControl) {
//        val zoomState = mCameraInfo.zoomState
//        val listener = CameraXPreviewViewTouchListener(this)
//        listener.setCustomTouchListener(object : CameraXPreviewViewTouchListener.CustomTouchListener {
//            override fun zoom(delta: Float) {
//                val currentZoomRatio = zoomState.value!!.zoomRatio
//                mCameraControl.setZoomRatio(currentZoomRatio * delta)
//                Log.e(TAG, "触发放大")
//            }
//        })
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scale = mCameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                mCameraControl.setZoomRatio(scale)
                return true
            }
        }
        val scaleGestureDetector = ScaleGestureDetector(baseContext, listener)
        viewBinding.viewFinder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
//        viewBinding.viewFinder.setOnTouchListener(ttt)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraActivity"


        private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA, Manifest.permission.REQUEST_INSTALL_PACKAGES)

        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}