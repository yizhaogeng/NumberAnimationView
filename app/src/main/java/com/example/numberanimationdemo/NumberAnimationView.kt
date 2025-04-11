package com.example.numberanimationdemo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import android.util.TypedValue

/**
 * 数字滚动动画视图
 * 支持数字的滚动动画效果，包括数字增减、高位消失等特效
 */
class NumberAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TOTAL_STEPS = 10  // 动画总步数，决定数字滚动的过渡帧数
    }

    // region Properties
    // 数字相关属性
    private var fromNumber = ""        // 动画起始数字
    private var toNumber = ""          // 动画目标数字
    private val currentDigits = mutableListOf<Int>()  // 当前显示的数字列表
    private var finalNumber = ""       // 最终要显示的数字（动画结束后的数字）
    private val nextDigits = mutableListOf<Int>()  // 存储每个位置的下一个要显示的数字

    // 前缀相关属性
    private var prefix: String = ""    // 数字前的前缀文本（如：￥、$等）
    private var prefixWidth: Float = 0f // 前缀文本的宽度

    // 画笔相关属性
    private var numberPaint = Paint(Paint.ANTI_ALIAS_FLAG)    // 数字绘制画笔
    private var prefixPaint = Paint(Paint.ANTI_ALIAS_FLAG)    // 前缀绘制画笔
    private var textSize: Float = 40f                         // 数字文本大小
    private var prefixTextSize: Float = textSize              // 前缀文本大小
    private var digitColor: Int = Color.BLACK                 // 数字颜色
    private var prefixColor: Int = Color.BLACK                // 前缀颜色

    // 间距相关属性
    private var digitWidth: Float = 0f             // 单个数字的宽度
    private var digitSpacing: Float = 20f          // 数字垂直滚动时的间距
    private var digitHorizontalSpacing: Float = 0f // 数字之间的水平间距
    private var prefixSpacing: Float = 0f          // 前缀和数字之间的间距

    // 动画相关属性
    private val digitAnimators = mutableListOf<ValueAnimator>() // 存储所有数字的动画器
    private val digitOffsets = mutableListOf<Float>()          // 存储每个数字的垂直偏移量
    private val digitVisibility = mutableListOf<Float>()       // 存储每个数字的可见性（透明度）
    private var isAnimating = false                           // 是否正在执行动画
    private var baseAnimationDuration: Long = 600L            // 基础动画持续时间
    private var interpolator: TimeInterpolator                 // 动画插值器
    private var drawInterval = 16L                            // 重绘时间间隔（毫秒）
    private var lastDrawTime = 0L                             // 上次重绘时间戳

    // 字体相关属性
    private var numberTypeface: Typeface? = null    // 数字字体
    private var prefixTypeface: Typeface? = null    // 前缀字体
    // endregion

    /**
     * 初始化视图的基本属性
     */
    init {
        // 读取自定义属性
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.NumberAnimationView,
            defStyleAttr,
            0
        ).apply {
            try {
                // 读取文本大小
                textSize = getDimension(R.styleable.NumberAnimationView_textSize, 40f)
                // 读取数字颜色
                digitColor = getColor(R.styleable.NumberAnimationView_digitColor, Color.BLACK)
                // 读取前缀颜色
                prefixColor = getColor(R.styleable.NumberAnimationView_prefixColor, Color.BLACK)
                // 读取数字水平间距
                digitHorizontalSpacing =
                    getDimension(R.styleable.NumberAnimationView_digitHorizontalSpacing, 0f)
                // 读取前缀间距
                prefixSpacing = getDimension(R.styleable.NumberAnimationView_prefixSpacing, 0f)
                // 读取动画时长
                baseAnimationDuration =
                    getInteger(R.styleable.NumberAnimationView_animationDuration, 600).toLong()
                // 读取数字垂直间距
                digitSpacing = getDimension(R.styleable.NumberAnimationView_digitSpacing, 20f)
                // 读取前缀文本大小
                prefixTextSize =
                    getDimension(R.styleable.NumberAnimationView_prefixTextSize, textSize)
                // 读取绘制间隔
                drawInterval = getInteger(R.styleable.NumberAnimationView_drawInterval, 16).toLong()
            } finally {
                recycle()
            }
        }

        // 初始化数字画笔
        numberPaint.apply {
            textAlign = Paint.Align.CENTER  // 文本居中对齐
            color = digitColor
            textSize = this@NumberAnimationView.textSize
        }

        // 初始化前缀画笔
        prefixPaint.apply {
            textAlign = Paint.Align.CENTER  // 文本居中对齐
            color = prefixColor
            textSize = prefixTextSize
        }

        // 计算单个数字的宽度
        digitWidth = numberPaint.measureText("0")
        // 设置动画插值器
        interpolator = PathInterpolator(0.84f, 0.00f, 0.16f, 1.00f)
    }

    /**
     * 设置起始数字、目标数字和前缀
     * @param from 起始数字
     * @param to 目标数字
     * @param prefix 前缀文本
     */
    fun setNumbers(from: String, to: String, prefix: String = "") {
        // 验证输入的数字字符串是否合法
        if (!from.all { it.isDigit() } || !to.all { it.isDigit() }) return

        clearAnimationState()  // 清除之前的动画状态
        setupPrefix(prefix)    // 设置前缀
        setupNumbers(from, to) // 设置数字
        invalidate()          // 触发重绘
    }

    /**
     * 开始执行动画
     */
    fun startAnimation() {
        if (fromNumber.isEmpty() || toNumber.isEmpty()) return

        setupAnimationState() // 设置动画初始状态
        createAnimators()     // 创建动画器
    }

    /**
     * 创建单个数字的动画器
     * @param index 数字在字符串中的索引
     * @param startDigit 起始数字
     * @param endDigit 目标数字
     */
    private fun createDigitAnimator(index: Int, startDigit: Int, endDigit: Int) {
        // 根据位置计算动画时长，越高位动画开始越晚
        val duration = baseAnimationDuration + (index * 100L)

        ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = this@NumberAnimationView.interpolator

            // 动画更新监听器
            addUpdateListener {
                updateDigit(it.animatedValue as Float, index, startDigit, endDigit)
            }

            // 动画结束监听器
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 检查是否所有动画都已结束
                    if (digitAnimators.none { it.isRunning }) {
                        onAllAnimationsEnd()
                    }
                }
            })

            digitAnimators.add(this)
            start()
        }
    }

    /**
     * 更新单个数字的动画状态
     * @param progress 动画进度 (0-1)
     * @param index 数字索引
     * @param startDigit 起始数字
     * @param endDigit 目标数字
     */
    private fun updateDigit(progress: Float, index: Int, startDigit: Int, endDigit: Int) {
        // 判断是否是需要消失的高位数字
        val isVanishingDigit = fromNumber.length > toNumber.length &&
                index < (fromNumber.length - toNumber.length)

        // 计算当前动画步骤和偏移量
        val currentStep = (progress * TOTAL_STEPS).toInt()
        digitOffsets[index] = progress * TOTAL_STEPS - currentStep

        // 计算当前应显示的数字
        val valuePair = calculateCurrentValue(currentStep, startDigit, endDigit)

        nextDigits[index] = valuePair.second

        // 处理高位数字的淡出效果
        if (isVanishingDigit) {
            val alphaProgress = (progress * 1.5f).coerceAtMost(1f)
            digitVisibility[index] = 1f - alphaProgress
        }

        // 更新当前显示的数字
        updateCurrentNumber(index, valuePair.first)
        // 触发重绘
        tryInvalidate()
    }

    /**
     * 计算当前应该显示的数字
     * @param currentStep 当前动画步骤 (0-9)
     * @param startDigit 起始数字
     * @param endDigit 目标数字
     * @return 当前应该显示的数字和下一个要展示的数字
     */
    private fun calculateCurrentValue(
        currentStep: Int,
        startDigit: Int,
        endDigit: Int
    ): Pair<Int, Int> {
        return if (currentStep <= startDigit) {
            // 第一阶段：从起始数字递减到0
            val currentValue = startDigit - currentStep
            // 区分第一阶段结束
            val nextValue = if (currentValue == 0) {
                (9 + endDigit - startDigit) % 10
            } else {
                currentValue - 1
            }
            Pair(currentValue, nextValue)
        } else {
            val n = 9 + endDigit - startDigit
            if (startDigit >= endDigit) {
                // x>=y 的情况
                val remainingSteps = currentStep - startDigit
                val value = n - (remainingSteps - 1)
                val currentValue = if (value < endDigit) endDigit else value
                val nextValue = if (currentValue == endDigit) endDigit else value - 1
                Pair(currentValue, nextValue)
            } else {
                // x<y 的情况
                val remainingSteps = currentStep - startDigit
                val currentN = n - (remainingSteps - 1)
                val currentValue = currentN % 10
                val nextValue = if (currentValue == endDigit) endDigit else (currentN - 1) % 10
                Pair(currentValue, nextValue)
            }
        }
    }

    private fun createAnimators() {
        digitAnimators.clear()

        val maxLength = maxOf(fromNumber.length, toNumber.length)
        val paddedFrom = fromNumber.padStart(maxLength, '0')
        val paddedTo = toNumber.padStart(maxLength, '0')

        for (i in 0 until maxLength) {
            val startDigit = paddedFrom[i].toString().toInt()
            val endDigit = paddedTo[i].toString().toInt()
            createDigitAnimator(i, startDigit, endDigit)
        }
    }

    private fun updateCurrentNumber(index: Int, value: Int) {
        if (index < currentDigits.size) {
            currentDigits[index] = value
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentDigits.isEmpty()) return

        // 保存画布当前状态
        canvas.save()
        // 裁剪画布，限制绘制区域为视图的实际高度
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())

        val baselineY = calculateBaselineY()
        val startX = calculateStartX()

        drawPrefix(canvas, startX, baselineY)

        if (isAnimating) {
            drawAnimatingDigits(canvas, startX, baselineY)
        } else {
            drawStaticDigits(canvas, startX, baselineY)
        }

        // 恢复画布状态
        canvas.restore()
    }

    private fun calculateBaselineY(): Float {
        val numberFontMetrics = numberPaint.fontMetrics
        return height / 2f - (numberFontMetrics.bottom + numberFontMetrics.top) / 2
    }

    private fun calculateStartX(): Float {
        val visibleCount = if (isAnimating) {
            digitVisibility.count { it > 0f }
        } else {
            currentDigits.size
        }

        val totalWidth = calculateTotalWidth(visibleCount)
        return (width - totalWidth) / 2
    }

    private fun calculateTotalWidth(visibleCount: Int): Float {
        return digitWidth * visibleCount +
                prefixWidth +
                (if (visibleCount > 1) digitHorizontalSpacing * (visibleCount - 1) else 0f) +
                (if (prefix.isNotEmpty()) prefixSpacing else 0f)
    }

    private fun drawPrefix(canvas: Canvas, startX: Float, baselineY: Float) {
        if (prefix.isNotEmpty()) {
            // 计算垂直居中的基准线
            val centerY = height / 2f
            val baseline = centerY + (prefixPaint.fontMetrics.bottom - prefixPaint.fontMetrics.top) / 2 - prefixPaint.fontMetrics.bottom
            canvas.drawText(prefix, startX + prefixWidth / 2, baseline, prefixPaint)
        }
    }

    private fun drawAnimatingDigits(canvas: Canvas, startX: Float, baselineY: Float) {
        val fontMetrics = numberPaint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        var drawnCount = 0

        // 计算垂直居中的基准线
        val centerY = height / 2f
        // 计算动画状态下两个数字的总高度
        val totalHeight = textHeight * 2 + digitSpacing
        // 计算顶部偏移，使绘制内容垂直居中
        val topOffset = (height - totalHeight) / 2

        for (i in currentDigits.indices) {
            if ((digitVisibility.getOrNull(i) ?: 1f) <= 0f) continue

            drawAnimatingDigit(canvas, i, startX, centerY, textHeight, drawnCount, topOffset)
            drawnCount++
        }

        numberPaint.alpha = 255
    }

    private fun drawAnimatingDigit(
        canvas: Canvas,
        index: Int,
        startX: Float,
        centerY: Float,
        textHeight: Float,
        drawnCount: Int,
        topOffset: Float
    ) {
        val x = startX + prefixWidth + prefixSpacing +
                drawnCount * (digitWidth + digitHorizontalSpacing) +
                digitWidth / 2

        val currentDigit = currentDigits[index]
        val nextDigit = nextDigits[index]
        val offset = digitOffsets.getOrNull(index) ?: 0f

        numberPaint.alpha = ((digitVisibility.getOrNull(index) ?: 1f) * 255).toInt()

        // 计算当前数字的基准线位置
        val currentBaseline = centerY + (numberPaint.fontMetrics.bottom - numberPaint.fontMetrics.top) / 2 - numberPaint.fontMetrics.bottom
        // 应用动画偏移
        val currentY = currentBaseline + offset * (textHeight + digitSpacing)
        
        canvas.drawText(currentDigit.toString(), x, currentY, numberPaint)
        
        if (nextDigit != currentDigit) {
            // 计算下一个数字的位置
            val nextY = currentY - (textHeight + digitSpacing)
            canvas.drawText(nextDigit.toString(), x, nextY, numberPaint)
        }
    }

    private fun drawStaticDigits(canvas: Canvas, startX: Float, baselineY: Float) {
        // 计算垂直居中的基准线
        val centerY = height / 2f
        val baseline = centerY + (numberPaint.fontMetrics.bottom - numberPaint.fontMetrics.top) / 2 - numberPaint.fontMetrics.bottom

        for (i in currentDigits.indices) {
            val x = startX + prefixWidth + prefixSpacing +
                    i * (digitWidth + digitHorizontalSpacing) +
                    digitWidth / 2
            canvas.drawText(currentDigits[i].toString(), x, baseline, numberPaint)
        }
    }

    private fun clearAnimationState() {
        digitAnimators.forEach { it.cancel() }
        digitAnimators.clear()
        digitOffsets.clear()
        digitVisibility.clear()
    }

    private fun setupPrefix(prefix: String) {
        this.prefix = prefix
        updatePrefixWidth()
    }

    private fun updatePrefixWidth() {
        prefixWidth = if (prefix.isNotEmpty()) {
            prefixPaint.measureText(prefix)
        } else 0f
    }

    private fun setupNumbers(from: String, to: String) {
        fromNumber = from
        toNumber = to
        finalNumber = to
        currentDigits.clear()
        currentDigits.addAll(fromNumber.map { it.toString().toInt() })
        isAnimating = false
        digitVisibility.addAll(List(fromNumber.length) { 1f })
        requestLayout()
    }

    private fun setupAnimationState() {
        isAnimating = true

        val maxLength = maxOf(fromNumber.length, toNumber.length)
        currentDigits.clear()
        currentDigits.addAll(fromNumber.padStart(maxLength, '0').map { it.toString().toInt() })

        digitOffsets.clear()
        digitOffsets.addAll(List(maxLength) { 0f })
        digitVisibility.clear()
        digitVisibility.addAll(List(maxLength) { 1f })
        nextDigits.clear()
        nextDigits.addAll(List(maxLength) { 0 })
        
        requestLayout()
    }

    private fun tryInvalidate() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDrawTime >= drawInterval) {
            lastDrawTime = currentTime
            invalidate()
        }
    }

    private fun onAllAnimationsEnd() {
        isAnimating = false
        currentDigits.clear()
        currentDigits.addAll(finalNumber.map { it.toString().toInt() })
        digitVisibility.clear()
        digitVisibility.addAll(List(finalNumber.length) { 1f })
        digitOffsets.clear()
        digitOffsets.addAll(List(finalNumber.length) { 0f })
        
        requestLayout()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        digitAnimators.forEach { it.cancel() }
        digitAnimators.clear()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 计算可见数字的数量
        val visibleCount = if (isAnimating) {
            digitVisibility.count { it > 0f }
        } else {
            finalNumber.length
        }

        // 使用统一的宽度计算方法
        val desiredWidth = calculateTotalWidth(visibleCount).toInt()

        // 计算期望高度：文字高度 + 垂直滚动间距
        val fontMetrics = numberPaint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        val desiredHeight = (textHeight + digitSpacing).toInt()

        val width = resolveSize(desiredWidth + paddingLeft + paddingRight, widthMeasureSpec)
        val height = resolveSize(desiredHeight + paddingTop + paddingBottom, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    /**
     * 设置数字的字体
     * @param typeface 字体
     */
    fun setNumberTypeface(typeface: Typeface?) {
        numberTypeface = typeface
        numberPaint.typeface = typeface
        digitWidth = numberPaint.measureText("0")  // 重新计算数字宽度
        requestLayout()
        invalidate()
    }

    /**
     * 设置前缀的字体
     * @param typeface 字体
     */
    fun setPrefixTypeface(typeface: Typeface?) {
        prefixTypeface = typeface
        prefixPaint.typeface = typeface
        updatePrefixWidth()  // 重新计算前缀宽度
        requestLayout()
        invalidate()
    }

    /**
     * 同时设置数字和前缀的字体
     * @param typeface 字体
     */
    fun setTypeface(typeface: Typeface?) {
        setNumberTypeface(typeface)
        setPrefixTypeface(typeface)
    }

    /**
     * 设置数字的字体大小
     * @param size 字体大小（px）
     */
    fun setNumberTextSize(size: Float) {
        textSize = size
        numberPaint.textSize = size
        digitWidth = numberPaint.measureText("0")  // 重新计算数字宽度
        requestLayout()
        invalidate()
    }

    /**
     * 设置前缀的字体大小
     * @param size 字体大小（px）
     */
    fun setPrefixTextSize(size: Float) {
        prefixTextSize = size
        prefixPaint.textSize = size
        updatePrefixWidth()  // 重新计算前缀宽度
        requestLayout()
        invalidate()
    }

    /**
     * 同时设置数字和前缀的字体大小
     * @param size 字体大小（px）
     */
    fun setTextSize(size: Float) {
        setNumberTextSize(size)
        setPrefixTextSize(size)
    }

    /**
     * 设置数字的字体大小
     * @param unit 单位（如：TypedValue.COMPLEX_UNIT_SP）
     * @param size 字体大小
     */
    fun setNumberTextSize(unit: Int, size: Float) {
        setNumberTextSize(TypedValue.applyDimension(unit, size, resources.displayMetrics))
    }

    /**
     * 设置前缀的字体大小
     * @param unit 单位（如：TypedValue.COMPLEX_UNIT_SP）
     * @param size 字体大小
     */
    fun setPrefixTextSize(unit: Int, size: Float) {
        setPrefixTextSize(TypedValue.applyDimension(unit, size, resources.displayMetrics))
    }

    /**
     * 同时设置数字和前缀的字体大小
     * @param unit 单位（如：TypedValue.COMPLEX_UNIT_SP）
     * @param size 字体大小
     */
    fun setTextSize(unit: Int, size: Float) {
        setTextSize(TypedValue.applyDimension(unit, size, resources.displayMetrics))
    }

    /**
     * 设置数字的颜色
     * @param color 颜色值
     */
    fun setNumberColor(color: Int) {
        digitColor = color
        numberPaint.color = color
        invalidate()
    }

    /**
     * 设置前缀的颜色
     * @param color 颜色值
     */
    fun setPrefixColor(color: Int) {
        prefixColor = color
        prefixPaint.color = color
        invalidate()
    }

    /**
     * 同时设置数字和前缀的颜色
     * @param color 颜色值
     */
    fun setTextColor(color: Int) {
        setNumberColor(color)
        setPrefixColor(color)
    }

    /**
     * 设置数字的颜色资源
     * @param colorRes 颜色资源ID
     */
    fun setNumberColorResource(colorRes: Int) {
        setNumberColor(context.getColor(colorRes))
    }

    /**
     * 设置前缀的颜色资源
     * @param colorRes 颜色资源ID
     */
    fun setPrefixColorResource(colorRes: Int) {
        setPrefixColor(context.getColor(colorRes))
    }

    /**
     * 同时设置数字和前缀的颜色资源
     * @param colorRes 颜色资源ID
     */
    fun setTextColorResource(colorRes: Int) {
        setTextColor(context.getColor(colorRes))
    }
}
