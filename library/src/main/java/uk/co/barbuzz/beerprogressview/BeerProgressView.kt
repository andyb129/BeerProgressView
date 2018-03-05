package uk.co.barbuzz.beerprogressview


import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.nineoldandroids.animation.ObjectAnimator

class BeerProgressView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var paint: Paint? = null
    private var borderPaint: Paint? = null
    private var borderRectF: RectF? = null
    private var path: Path? = null
    private var angleAnim: ObjectAnimator? = null

    private val borderRadius: Float
    private val angularVelocity = 2.0f
    private var beerProgressHeight = 50f
    private val borderWidth: Int
    private var beerWidth: Int = 0
    private var beerHeight: Int = 0

    private val amplitude: Int
    private var angle = 0
    private var waveMax: Int
    var maxBubbleCount: Int = 0

    //bubble vars
    private var drawBubblesRunnable: Runnable? = null
    private var arrayOfBubbles: Array<Bubble>
    private val bubbleHandler = Handler()
    private var startMilli: Long = 0
    private var bubbleHeight: Int = 0
    private var bubbleTopMargin: Int = 0
    private var bubbleWidth: Int = 0

    private val isViewVisiable: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            visibility == View.VISIBLE && alpha * 255 > 0
        } else {
            visibility == View.VISIBLE
        }

    var beerProgress: Int = DEFAULT_BEER_PROGRESS
        set(value) {
            field = when {
                value > waveMax -> waveMax
                value < 0 -> 0
                else -> value
            }

            val percent = field * 1.0f / waveMax
            beerProgressHeight = percent * beerHeight
            invalidate()
        }

    var beerColor: Int = DEFAULT_BEER_COLOR
        set(value) {
            field = value
            paint?.color = field
            borderPaint?.color = field
        }

    var bubbleColor: Int = DEFAULT_BUBBLE_COLOR
        set(value) {
            field = value
            arrayOfBubbles.forEach { bubble -> bubble.updateColour(field) }
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BeerProgressView)

        borderWidth = a.getDimensionPixelSize(R.styleable.BeerProgressView_waveBorderWidth, DEFAULT_BORDER_WIDTH)
        amplitude = a.getDimensionPixelSize(R.styleable.BeerProgressView_waveAmplitude, DEFAULT_AMPLITUDE)
        borderRadius = a.getDimensionPixelSize(R.styleable.BeerProgressView_waveBorderRadius, DEFAULT_BORDER_RADIUS).toFloat()

        beerColor = a.getColor(R.styleable.BeerProgressView_beerColor, DEFAULT_BEER_COLOR)
        waveMax = a.getInt(R.styleable.BeerProgressView_waveMax, DEFAULT_WAVE_MAX)
        beerProgress = a.getInteger(R.styleable.BeerProgressView_beerProgress, DEFAULT_BEER_PROGRESS)
        maxBubbleCount = a.getInteger(R.styleable.BeerProgressView_maxBubbleCount, DEFAULT_MAX_BUBBLE_COUNT)
        arrayOfBubbles = Array(maxBubbleCount, { Bubble(bubbleWidth, bubbleHeight, bubbleTopMargin, bubbleColor) })
        bubbleColor = a.getColor(R.styleable.BeerProgressView_bubbleColor, DEFAULT_BUBBLE_COLOR)

        a.recycle()

        init()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //draw wave
        if (beerProgressHeight > 0) {
            updatePath()

            canvas.drawPath(path, paint)
            if (borderWidth != 0) {
                canvas.drawRoundRect(borderRectF, borderRadius, borderRadius, borderPaint)
            }
        }

        //draw bubbles
        if ((beerProgressHeight > 0) and (beerProgress > 10) and (maxBubbleCount > 0)) {
            drawBubbles(canvas)
        } else {
            bubbleHandler.removeCallbacks(drawBubblesRunnable)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)


        /**
         * If the device is an arm-based device, the getHeight() & getWidth() will always give 0 & 0 respectively.
         * So,  it is always preffered to use getMeasuredWidth() & getMeasuredHeight() respectively.
         * These method wors on all type of devices.
         *
         * Refernces: http://stackoverflow.com/questions/42529912/getheight-is-always-0-onmeasure-on-arm-based-devices
         */
        val halfBorderRadius = borderRadius / 2

        beerWidth = (measuredWidth - halfBorderRadius).toInt()
        beerHeight = (measuredHeight - halfBorderRadius).toInt()


        borderRectF?.set(halfBorderRadius, halfBorderRadius, beerWidth.toFloat(), beerHeight.toFloat())
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable(STATE_INSTANCE, super.onSaveInstanceState())
        bundle.putInt(STATE_MAX, waveMax)
        bundle.putInt(STATE_PROGRESS, beerProgress)
        bundle.putInt(STATE_WAVE_COLOR, beerColor)
        bundle.putInt(STATE_BUBBLE_COLOR, bubbleColor)
        bundle.putInt(STATE_MAX_BUBBLE_COUNT, maxBubbleCount)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            waveMax = state.getInt(STATE_MAX, DEFAULT_WAVE_MAX)
            beerProgress = state.getInt(STATE_PROGRESS, DEFAULT_BEER_PROGRESS)
            beerColor = state.getInt(STATE_WAVE_COLOR, DEFAULT_BEER_COLOR)
            bubbleColor = state.getInt(STATE_BUBBLE_COLOR, DEFAULT_BUBBLE_COLOR)
            maxBubbleCount = state.getInt(STATE_MAX_BUBBLE_COUNT, DEFAULT_MAX_BUBBLE_COUNT)
            super.onRestoreInstanceState(state.getParcelable(STATE_INSTANCE))
            return
        }
        super.onRestoreInstanceState(state)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupAngleAnim()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAngleAnim()
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        startOrCancelAngleAnim()
    }

    override fun setAlpha(alpha: Float) {
        super.setAlpha(alpha)
        startOrCancelAngleAnim()
    }

    private fun init() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint?.color = beerColor

        borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint?.color = beerColor
        borderPaint?.strokeWidth = borderWidth.toFloat()
        borderPaint?.style = Paint.Style.STROKE

        borderRectF = RectF()
        path = Path()

        setupAngleAnim()

        drawBubblesRunnable = Runnable { invalidate() }
    }

    private fun drawBubbles(canvas: Canvas) {
        startMilli = SystemClock.uptimeMillis()
        bubbleHeight = beerHeight
        bubbleTopMargin = bubbleHeight - beerProgressHeight.toInt() + 20
        bubbleWidth = canvas.width

        /* Draw each Bubble */
        for (bubble in arrayOfBubbles) {
            /* Move the Bubble */
            bubble.update(DEFAULT_BUBBLE_FPS, 0f)

            /* Draw circle */
            bubble.draw(canvas)

            if (bubble.popped(bubbleWidth, bubbleHeight, bubbleTopMargin)) {
                bubble.recycle(false, bubbleWidth, bubbleHeight, bubbleTopMargin)
            }
        }

        val duration = SystemClock.uptimeMillis() - startMilli
        bubbleHandler.postDelayed(drawBubblesRunnable, 1000 / DEFAULT_BUBBLE_FPS - duration)
    }

    private fun setupAngleAnim() {
        if (!isViewVisiable) {
            return
        }
        if (angleAnim == null) {
            angleAnim = ObjectAnimator.ofInt(this, "angle", 0, 360)
            angleAnim?.duration = 800
            angleAnim?.repeatMode = ObjectAnimator.RESTART
            angleAnim?.repeatCount = ObjectAnimator.INFINITE
            angleAnim?.interpolator = LinearInterpolator()
        }
        if (angleAnim?.isRunning!!.not()) {
            angleAnim?.start()
        }
    }

    private fun cancelAngleAnim() {
        if (angleAnim != null) {
            angleAnim?.cancel()
        }
    }

    private fun updatePath() {
        val halfBorderRadius = borderRadius / 2

        //default progress
//        beerProgress = mBeerProgress
        this.path?.reset()
        for (i in 0 until beerWidth) {
            val y = clamp(
                    amplitude * Math.sin((i * angularVelocity + angle * Math.PI) / 180.0f) + (beerHeight - beerProgressHeight),
                    halfBorderRadius.toDouble(),
                    beerHeight.toDouble()
            ).toInt()
            if (i == 0) {
                this.path?.moveTo(i.toFloat(), y.toFloat())
            }
            this.path?.quadTo(i.toFloat(), y.toFloat(), (i + 1).toFloat(), y.toFloat())
        }
        this.path?.lineTo(beerWidth.toFloat(), beerHeight.toFloat())
        this.path?.lineTo(0f, beerHeight.toFloat())
        this.path?.close()
    }

    private fun startOrCancelAngleAnim() {
        if (isViewVisiable) {
            setupAngleAnim()
        } else {
            cancelAngleAnim()
        }
    }

    /**
     * set the angle of the wave
     *
     * @param angle
     */
    fun setAngle(angle: Int) {
        this.angle = angle
        invalidate()
    }

    companion object {
        // TODO: Consider the user of @JvmField for those non compile time constants

        private const val TAG = "BeerProgressView"
        private const val STATE_INSTANCE = "state_instance"
        private const val STATE_MAX = "state_max"
        private const val STATE_PROGRESS = "state_progress"
        private const val STATE_WAVE_COLOR = "state_wave_color"
        private const val STATE_BUBBLE_COLOR = "state_bubble_color"
        private const val STATE_MAX_BUBBLE_COUNT = "state_bubble_count"
        private val DEFAULT_BEER_COLOR = Color.parseColor("#EFA601")
        private val DEFAULT_BUBBLE_COLOR = Color.parseColor("#B67200")
        private const val DEFAULT_MAX_BUBBLE_COUNT = 20
        private const val DEFAULT_BUBBLE_FPS = 30
        private val DEFAULT_BORDER_WIDTH = dp2px(3)
        private const val DEFAULT_WAVE_MAX = 100
        private val DEFAULT_AMPLITUDE = dp2px(3)
        private const val DEFAULT_BORDER_RADIUS = 0 //dp2px(2)
        private const val DEFAULT_BEER_PROGRESS = 0

        private fun clamp(value: Double, max: Double, min: Double): Double {
            return Math.max(Math.min(value, min), max)
        }

        private fun dp2px(dp: Int): Int {
            return (Resources.getSystem().displayMetrics.density * dp).toInt()
        }
    }

}

