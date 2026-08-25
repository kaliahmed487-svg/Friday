package com.jarvis.assistant.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.jarvis.assistant.core.VisualizerMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Premium single-circle visualizer. Perfectly still and silent at idle — no
 * breathing, no rotation, no wobble — a calm, motionless orb. Only comes
 * alive with real motion when it hears or speaks, so every animation frame
 * carries meaning instead of being decorative filler.
 */
class AudioVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mode: VisualizerMode = VisualizerMode.IDLE
    private var targetAmplitude = 0f
    private var smoothedAmplitude = 0f

    private val sampleCount = 64
    private val samples = FloatArray(sampleCount)
    private var writeIndex = 0

    private var phase = 0f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val wavePath = Path()

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 16
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { onTick() }
    }

    init {
        setBackgroundColor(Color.BLACK)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    fun setMode(newMode: VisualizerMode) {
        mode = newMode
    }

    fun pushAmplitude(level: Float) {
        targetAmplitude = level.coerceIn(0f, 1f)
    }

    private fun onTick() {
        // Idle stays perfectly still — no phase advance, no synthetic motion.
        if (mode != VisualizerMode.IDLE) {
            phase += 0.045f
            if (phase > 1000f) phase = 0f
        }

        smoothedAmplitude += (targetAmplitude - smoothedAmplitude) * 0.25f

        val sample = when (mode) {
            VisualizerMode.IDLE -> 0f
            VisualizerMode.THINKING -> 0.16f + Random.nextFloat() * 0.05f
            VisualizerMode.LISTENING, VisualizerMode.SPEAKING -> smoothedAmplitude
        }
        samples[writeIndex] = sample
        writeIndex = (writeIndex + 1) % sampleCount

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = min(width, height) * 0.19f

        val (r, g, b) = colorForMode(mode)

        // Soft ambient glow — subtle even at idle, so the orb never looks fully dead.
        glowPaint.shader = RadialGradient(
            cx, cy, baseRadius * 2.6f,
            Color.argb(if (mode == VisualizerMode.IDLE) 45 else 90, r, g, b),
            Color.argb(0, r, g, b),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, baseRadius * 2.6f, glowPaint)

        if (mode == VisualizerMode.IDLE) {
            // Perfectly still premium orb: a crisp core, a fine rim ring, nothing else moving.
            corePaint.shader = RadialGradient(
                cx, cy, baseRadius,
                Color.argb(255, minOf(255, r + 50), minOf(255, g + 50), minOf(255, b + 50)),
                Color.argb(220, r, g, b),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, baseRadius, corePaint)

            rimPaint.color = Color.argb(140, minOf(255, r + 80), minOf(255, g + 80), minOf(255, b + 80))
            canvas.drawCircle(cx, cy, baseRadius * 1.12f, rimPaint)
        } else {
            // Active states: fluid waveform ring around the core.
            wavePath.reset()
            val points = 128
            for (i in 0..points) {
                val angle = (i.toDouble() / points) * 2 * PI
                val sampleIdx = (((angle / (2 * PI)) * sampleCount).toInt() + writeIndex) % sampleCount
                val amp = samples[sampleIdx]
                val wobble = sin(angle * 10 + phase * 10) * amp * 0.32
                val radius = baseRadius * (1f + amp * 1.5f + wobble.toFloat())
                val x = cx + (radius * cos(angle)).toFloat()
                val y = cy + (radius * sin(angle)).toFloat()
                if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
            }
            wavePath.close()

            ringPaint.color = Color.rgb(r, g, b)
            ringPaint.alpha = 230
            canvas.drawPath(wavePath, ringPaint)

            corePaint.shader = RadialGradient(
                cx, cy, baseRadius,
                Color.argb(255, minOf(255, r + 60), minOf(255, g + 60), minOf(255, b + 60)),
                Color.argb(160, r, g, b),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, baseRadius * 0.55f, corePaint)
        }
    }

    /** Premium palette: deep violet at rest, warm gold family when active — no primary-color cyan/green. */
    private fun colorForMode(mode: VisualizerMode): Triple<Int, Int, Int> = when (mode) {
        VisualizerMode.IDLE -> Triple(124, 92, 255)       // soft violet, calm
        VisualizerMode.LISTENING -> Triple(255, 200, 87)  // warm gold, alert
        VisualizerMode.THINKING -> Triple(180, 130, 255)  // brighter violet, processing
        VisualizerMode.SPEAKING -> Triple(255, 158, 66)   // amber, talking
    }
}

