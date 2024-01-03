package com.cstore.aiphoto

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View


/**
 * Created by zhiya.zhang
 * on 2022/11/2 17:31.
 */
class CameraXPreviewViewTouchListener(context: Context?) : View.OnTouchListener {

    /**
     * 缩放相关
     */
    private val mScaleGestureDetector: ScaleGestureDetector
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        mScaleGestureDetector.onTouchEvent(event!!)
        return true
    }

    /**
     * 缩放监听
     */
    interface CustomTouchListener {
        /**
         * 放大
         */
        fun zoom(delta: Float)
    }

    private var mCustomTouchListener: CustomTouchListener? = null
    fun setCustomTouchListener(customTouchListener: CustomTouchListener?) {
        mCustomTouchListener = customTouchListener
    }

    /**
     * 缩放监听
     */
    private var onScaleGestureListener: OnScaleGestureListener = object : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val delta = detector.scaleFactor
            if(mCustomTouchListener != null) {
                mCustomTouchListener!!.zoom(delta)
            }
            return true
        }
    }

    init {
        mScaleGestureDetector = ScaleGestureDetector(context!!, onScaleGestureListener)
    }
}