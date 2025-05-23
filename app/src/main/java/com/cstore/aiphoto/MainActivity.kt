package com.cstore.aiphoto

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.cstore.aiphoto.databinding.ActivityMainBinding

/**
 * Created by zhiya.zhang
 * on 2024/2/2 10:32.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private val localText = arrayOf(
        "五楼实验室",
        "四楼实验室",
        "五.五楼左",
        "五.五楼中",
        "五.五楼右",
        "五.五楼升降机",
        "开发测试",
        "ZZY"
    )
    private var local = "192.168.7.88"
    private var ipName = "五楼实验室"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        val adapter = ArrayAdapter(
            baseContext,
            com.google.android.material.R.layout.support_simple_spinner_dropdown_item,
            localText
        )
        viewBinding.ipSpinner.adapter = adapter
        viewBinding.ipSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                local = when (position) {
                    0 -> "192.168.7.88"
                    1 -> "192.168.0.223"
                    2 -> "192.168.3.199"
                    3 -> "192.168.3.192"
                    4 -> "192.168.3.218"
                    5 -> "192.168.3.191"
                    6 -> "192.168.3.213"
                    7 -> "192.168.3.88"
                    else -> "192.168.3.88"
                }
                ipName = localText[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        viewBinding.btn.setOnClickListener {
            val intent = Intent(baseContext, CameraActivity::class.java)
            intent.putExtra("ip", local)
            intent.putExtra("ip_name", ipName)
            this.startActivity(
                intent,
                ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
            )
        }
    }
}