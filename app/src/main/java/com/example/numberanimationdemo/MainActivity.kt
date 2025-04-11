package com.example.numberanimationdemo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var numberAnimationView: NumberAnimationView
    private lateinit var startButton: Button
    private lateinit var fromInput: EditText
    private lateinit var toInput: EditText
    private lateinit var prefixInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化视图
        initViews()
        // 设置点击事件
        setupClickListeners()
    }

    private fun initViews() {
        numberAnimationView = findViewById(R.id.numberAnimationView)
        startButton = findViewById(R.id.startButton)
        fromInput = findViewById(R.id.fromInput)
        toInput = findViewById(R.id.toInput)
        prefixInput = findViewById(R.id.prefixInput)

        // 设置默认值
        fromInput.setText("123")
        toInput.setText("111")
        prefixInput.setText("￥")
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener {
            val fromNumber = fromInput.text.toString()
            val toNumber = toInput.text.toString()
            val prefix = prefixInput.text.toString()

            // 验证输入
            if (fromNumber.isEmpty() || toNumber.isEmpty()) {
                Toast.makeText(this, "请输入数字", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!fromNumber.all { it.isDigit() } || !toNumber.all { it.isDigit() }) {
                Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 开始动画
            numberAnimationView.setNumbers(fromNumber, toNumber, prefix)
            numberAnimationView.startAnimation()
        }
    }

    private fun showCase1() {
        numberAnimationView.setNumbers("123", "9", "￥")
        numberAnimationView.startAnimation()
    }

    private fun showCase2() {
        numberAnimationView.setNumbers("99", "123", "￥")
        numberAnimationView.startAnimation()
    }

    private fun showCase3() {
        numberAnimationView.setNumbers("321", "111", "￥")
        numberAnimationView.startAnimation()
    }

    private fun showCase4() {
        numberAnimationView.setNumbers("555", "555", "￥")
        numberAnimationView.startAnimation()
    }

    private fun showCase5() {
        numberAnimationView.setNumbers("102", "199", "￥")
        numberAnimationView.startAnimation()
    }
} 