package uk.co.barbuzz.beerprogressview

import android.graphics.Canvas
import android.graphics.Paint

/**
 * The bubble class tracks the size, location and colour of a single bubble.
 */
class Bubble {

    //standard beer size, speed & fps
    private val BUBBLE_SIZE: Int = 20
    private val SPEED: Int = 30
    private val FPS: Int = 30

    private var step: Int = 0
    private var amp: Double = 0.0
    private var freq: Double = 0.0
    private var skew: Double = 0.0
    private var x: Float = 0F
    private var y: Float = 0F
    private var radius: Float = 0F
    private var maxRadius: Float = 0F
    private var popped: Boolean = false
    private var paint: Paint = Paint()

    /**
     * Create a bubble, passing in width & height of view
     *
     * @param width
     * @param height
     */
    constructor(width: Int, height: Int, topMargin: Int, bubbleColour: Int) {
        paint.color = bubbleColour
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        recycle(true, width, height, topMargin)
    }

    /**
     * Re-initialises the Bubble properties so that it appears to be a new
     * bubble.
     *
     * Although a bit of elegance is sacrificed this seemed to result in a
     * performance boost during initial testing.
     *
     * @param initial
     * @param width
     * @param height
     */
    fun recycle(initial: Boolean, width: Int, height: Int, topMargin: Int) {
        y = if (initial) {
            randRange(topMargin, height).toFloat()
        } else {
            // Start at the bottom if not initial
            (height + (randRange(0, 21) - 10)).toFloat()
        }
        x = randRange(0, width).toFloat()
        radius = 1F
        maxRadius = randRange(3, BUBBLE_SIZE).toFloat()
        paint.alpha = randRange(100, 250)
        popped = false
        step = 0
        amp = Math.random() * 3
        freq = Math.random() * 2
        skew = Math.random() - 0.5
    }

    /**
     * Update the size and position of a Bubble.
     *
     * @param fps The current FPS
     * @param angle The angle of the device
     */
    fun update(fps: Int, angle: Float) {
        val speed = SPEED / FPS * Math.log(radius.toDouble())
        y -= speed.toFloat()
        x += (amp * Math.sin(freq * (step++ * speed)) + skew).toFloat()
        if (radius < maxRadius) {
            radius += maxRadius / (fps.toFloat() / SPEED * radius)
            if (radius > maxRadius) radius = maxRadius
        }
    }

    /**
     * Test whether a bubble is no longer visible.
     *
     * @param width Canvas width
     * @param height Canvas height
     * @param topMargin offset from top of view that bubble will be popped
     * @return A boolean indicating that the Bubble has drifted off allowable area
     */
    fun popped(width: Int, height: Int, topMargin: Int): Boolean {
        return y + radius <= -20 ||
                y - radius >= height ||
                x + radius <= 0 ||
                x - radius >= width ||
                y - radius <= topMargin
    }

    /**
     * Unified method for drawing the bubble.
     *
     * @param canvas The canvas to draw on
     */
    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, radius, paint)
    }

    /**
     * Simple function for getting a random range.
     *
     * @param min The minimum int.
     * @param max The maximum int.
     * @return The random value.
     */
    fun randRange(min: Int, max: Int): Int {
        val mod = max - min
        val `val` = Math.ceil(Math.random() * 1000000) % mod
        return `val`.toInt() + min
    }


}