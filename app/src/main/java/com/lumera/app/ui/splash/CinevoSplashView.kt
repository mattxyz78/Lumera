package com.lumera.app.ui.splash

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View
import kotlin.math.min

/** Resolution-independent Cinevo intro; no video decoding, network or sound. */
class CinevoSplashView(
    context: Context,
    private val isAppReady: () -> Boolean,
    private val onFinished: () -> Unit,
    private val clock: () -> Long = { SystemClock.uptimeMillis() }
) : View(context) {
    private val started = clock()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lettering = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val caption = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        color = Color.rgb(197, 201, 224)
    }
    private var finished = false
    private var fadeStarted: Long? = null
    private var glow: Shader? = null
    private var spectrum: Shader? = null

    init {
        contentDescription = "Cinevo — Your cinema. Your way."
        // Keep the overlay from passing touch events into the screen beneath it.
        isClickable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        val span = min(w.toFloat(), h * 1.7f)
        lettering.textSize = span * 0.16f
        caption.textSize = span * 0.022f
        glow = RadialGradient(w * 0.5f, h * 0.48f, span * 0.60f,
            intArrayOf(Color.rgb(54, 16, 90), Color.rgb(19, 14, 43), Color.rgb(5, 6, 15)), null, Shader.TileMode.CLAMP)
        spectrum = LinearGradient(w * 0.20f, 0f, w * 0.80f, h.toFloat(),
            intArrayOf(Color.rgb(0, 229, 255), Color.rgb(142, 92, 255), Color.rgb(255, 52, 153)),
            null, Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (finished) return
        val now = clock()
        val elapsed = (now - started).coerceAtLeast(0L)
        // Slow startup has a bounded hold; recreation/destroy never leaves a video player behind.
        if (fadeStarted == null && ((elapsed >= 1_600 && isAppReady()) || elapsed >= 6_000)) fadeStarted = now
        val opacity = 1f - (fadeStarted?.let { ((now - it) / 280f).coerceIn(0f, 1f) } ?: 0f)
        if (opacity <= 0f) {
            finished = true
            post { if (isAttachedToWindow) onFinished() }
            return
        }
        val layer = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), (opacity * 255).toInt())
        paint.shader = glow
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        val span = min(width.toFloat(), height * 1.7f)
        val intro = (elapsed / 1_000f).coerceIn(0f, 1f)
        paint.shader = spectrum
        paint.strokeWidth = span * 0.002f
        paint.alpha = (36 * intro).toInt()
        for (i in -3..3) {
            val x = width * 0.5f + i * span * 0.12f
            canvas.drawLine(x - span * 0.24f, height * 0.12f, x + span * 0.24f, height * 0.88f, paint)
        }
        paint.alpha = 255
        lettering.shader = spectrum
        lettering.alpha = (255 * intro).toInt()
        val save = canvas.save()
        val scale = 0.94f + intro * 0.06f
        canvas.scale(scale, scale, width * 0.5f, height * 0.5f)
        canvas.drawText("CINEVO", width * 0.5f, height * 0.5f - (lettering.ascent() + lettering.descent()) / 2f, lettering)
        canvas.restoreToCount(save)
        caption.alpha = (210 * ((intro - 0.3f) / 0.7f).coerceIn(0f, 1f)).toInt()
        canvas.drawText("YOUR CINEMA. YOUR WAY.", width * 0.5f, height * 0.5f + span * 0.11f, caption)
        canvas.restoreToCount(layer)
        if (isAttachedToWindow) postInvalidateOnAnimation()
    }
}
