class NumberAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var fromNumber = ""
    private var toNumber = ""
    private var currentNumber = ""
    private var numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.BLACK
        textSize = 40f
    }
    
    private val digitAnimators = mutableListOf<ValueAnimator>()
    private val digitOffsets = mutableListOf<Float>()
    
    fun setNumbers(from: String, to: String) {
        // 停止之前的动画
        digitAnimators.forEach { it.cancel() }
        digitAnimators.clear()
        digitOffsets.clear()
        
        fromNumber = from
        toNumber = to
        currentNumber = fromNumber
        
        invalidate()
    }
    
    fun startAnimation() {
        if (fromNumber.isEmpty() || toNumber.isEmpty()) {
            return
        }
        
        // 对齐位数，较短的数字前面补0
        val maxLength = maxOf(fromNumber.length, toNumber.length)
        currentNumber = fromNumber.padStart(maxLength, '0')
        val targetNumber = toNumber.padStart(maxLength, '0')
        
        digitOffsets.clear()
        digitOffsets.addAll(List(maxLength) { 0f })
        
        // 为每一位数字创建动画，并设置延迟启动
        for (i in currentNumber.indices) {
            val startDigit = currentNumber[i].toString().toInt()
            val endDigit = targetNumber[i].toString().toInt()
            
            if (startDigit != endDigit) {
                // 计算需要滚动的总步数：从起始数字到0，再从9到目标数字
                val stepsToZero = startDigit
                val stepsFromNineToTarget = 9 - endDigit
                val totalSteps = stepsToZero + stepsFromNineToTarget + 1
                
                val animator = ValueAnimator.ofFloat(0f, totalSteps.toFloat())
                animator.duration = 1500L
                animator.interpolator = AccelerateDecelerateInterpolator()
                // 设置延迟时间，从左到右依次增加10ms
                animator.startDelay = (i * 10L)
                
                animator.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    val step = value.toInt()
                    val offset = value - step
                    digitOffsets[i] = offset
                    
                    // 计算当前应该显示的数字
                    val currentDigitValue = when {
                        step <= startDigit -> startDigit - step
                        step == startDigit + 1 -> 9
                        else -> 9 - (step - startDigit - 1)
                    }
                    
                    val chars = currentNumber.toCharArray()
                    chars[i] = currentDigitValue.toString()[0]
                    currentNumber = String(chars)
                    
                    invalidate()
                }
                
                digitAnimators.add(animator)
                animator.start()
            }
        }
    }
    
    /**
     * 停止所有动画
     */
    fun stopAnimation() {
        digitAnimators.forEach { it.cancel() }
        digitAnimators.clear()
        // 重置所有偏移量
        digitOffsets.clear()
        digitOffsets.addAll(List(currentNumber.length) { 0f })
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // View detach时停止所有动画
        stopAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // View重新attach时确保状态正确
        digitOffsets.clear()
        digitOffsets.addAll(List(currentNumber.length) { 0f })
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (currentNumber.isEmpty()) return
        
        val digitWidth = numberPaint.measureText("0")
        val startX = (width - digitWidth * currentNumber.length) / 2 + digitWidth / 2
        val centerY = height / 2f
        
        for (i in currentNumber.indices) {
            val currentDigit = currentNumber[i].toString().toInt()
            val nextDigit = if (currentDigit == 0) 9 else currentDigit - 1
            
            val x = startX + i * digitWidth
            val offset = digitOffsets.getOrNull(i) ?: 0f
            
            // 绘制当前数字
            val currentY = centerY + offset * height
            canvas.drawText(currentDigit.toString(), x, currentY, numberPaint)
            
            // 绘制下一个数字
            val nextY = currentY - height
            canvas.drawText(nextDigit.toString(), x, nextY, numberPaint)
        }
    }
}
