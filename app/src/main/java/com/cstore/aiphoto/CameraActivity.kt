package com.cstore.aiphoto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
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
import kotlin.random.Random

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        vm = ViewModelProvider(this)[CameraViewModel::class.java]
        outputDirectory = getOutputDirectory(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewBinding.viewFinder.post {
            setUpCamera()
        }
        vm.mState.observe({ lifecycle }) {
            Log.e("Act", "State:${it}")
            if (it == "start") {
                val t = pickText + "拍照"
                viewBinding.pickState.text = t
                takePhoto()
            }
        }
        vm.sendState.observe({ lifecycle }) {
            val t = if (it) {
                sendText + "上传中"
            } else {
                sendText + "等待上传"
            }
            viewBinding.sendState.text = t
        }
        vm.connState.observe({ lifecycle }) {
            if (it) {
                viewBinding.tryConn.visibility = View.GONE
            } else {
                viewBinding.tryConn.visibility = View.VISIBLE
            }
        }
        vm.socketMsg.observe({ lifecycle }) {
            val t = connText + it
            viewBinding.connState.text = t
        }
        vm.socketMsg.observe({ lifecycle }) {
            viewBinding.msg.text = it
        }
        viewBinding.tryConn.setOnClickListener {
            vm.tryConn()
            viewBinding.msg.text = ""
        }
        if (!hasPermissions(this)) {
            val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    // Take the user to the success fragment when permission is granted
                    Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
                    vm.connectSocket()
                } else {
                    Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
                }
            }
            permLauncher.launch(Manifest.permission.CAMERA)
        } else {
            // If permissions have already been granted, proceed
            vm.connectSocket()
        }
    }

    private fun takePhoto() {
        imageCapture?.let { imageCapture ->
            val fileName = "${SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.CHINA).format(System.currentTimeMillis())}_${Random.nextInt(10000, 99999)}.jpg"
            val photoFile = File(outputDirectory, fileName)
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val saveUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                    vm.sendFile(saveUri)
                    val t = pickText + "等待"
                    MainScope().launch {
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
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
        }
        imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraActivity"
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val fileDir = context.externalCacheDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            return if (fileDir != null && fileDir.exists()) {
                fileDir
            } else {
                appContext.filesDir
            }
        }

        private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}