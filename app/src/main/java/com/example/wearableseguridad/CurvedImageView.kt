package com.example.wearableseguridad

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class CurvedImageView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {
    private val path = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        path.reset()
        // Empezamos arriba a la izquierda
        path.moveTo(0f, 0f)
        // Línea hasta arriba a la derecha
        path.lineTo(w.toFloat(), 0f)
        // Bajamos por la derecha
        path.lineTo(w.toFloat(), h.toFloat())
        // Curva hacia ARRIBA en el centro para dar el efecto de la imagen
        path.quadTo(w / 2f, h * 0.6f, 0f, h.toFloat())
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.clipPath(path)
        super.onDraw(canvas)
    }
}