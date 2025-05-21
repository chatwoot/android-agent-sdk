package com.chatwoot.sdk.utils

import android.graphics.*
import android.graphics.drawable.Drawable

class TextDrawable private constructor(builder: Builder) : Drawable() {
    private val paint: Paint
    private val text: String
    private val color: Int
    private val height: Int
    private val width: Int
    private val fontSize: Int
    private val radius: Float

    init {
        height = builder.height
        width = builder.width
        radius = builder.radius
        text = if (builder.toUpperCase) builder.text.uppercase() else builder.text
        color = builder.color
        fontSize = builder.fontSize

        paint = Paint().apply {
            color = this@TextDrawable.color
            isAntiAlias = true
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textSize = this@TextDrawable.fontSize.toFloat()
        }
    }

    override fun draw(canvas: Canvas) {
        val r = Rect()
        r.set(0, 0, width, height)
        paint.color = color
        canvas.drawCircle(
            width / 2f,
            height / 2f,
            radius,
            paint
        )
        
        paint.color = Color.WHITE
        canvas.drawText(
            text,
            width / 2f,
            height / 2f - ((paint.descent() + paint.ascent()) / 2),
            paint
        )
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        paint.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return width
    }

    override fun getIntrinsicHeight(): Int {
        return height
    }

    companion object {
        fun create(text: String): TextDrawable {
            return Builder()
                .beginConfig()
                .width(60)
                .height(60)
                .fontSize(24)
                .endConfig()
                .buildRound(text.take(2), Color.parseColor("#1976D2"))
        }
    }

    private class Builder {
        var text = ""
        var color: Int = Color.GRAY
        var width = 0
        var height = 0
        var fontSize = 0
        var radius = 0f
        var toUpperCase = false

        fun beginConfig(): Builder = apply {
            return this
        }

        fun endConfig(): Builder = apply {
            return this
        }

        fun width(width: Int): Builder = apply {
            this.width = width
        }

        fun height(height: Int): Builder = apply {
            this.height = height
        }

        fun fontSize(fontSize: Int): Builder = apply {
            this.fontSize = fontSize
        }

        fun buildRound(text: String, color: Int): TextDrawable {
            this.text = text
            this.color = color
            this.radius = Math.min(width, height) / 2f
            return TextDrawable(this)
        }
    }
} 