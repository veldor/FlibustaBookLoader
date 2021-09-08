package net.veldor.flibustaloader.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View

class GIFView : View {
    private val movie: Movie
    private var movieStart: Long = 0

    constructor(context: Context?) : super(context) {
        movie = Movie.decodeStream(resources.assets.open("loading.gif"))
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        movie = Movie.decodeStream(resources.assets.open("loading.gif"))
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        movie = Movie.decodeStream(resources.assets.open("loading.gif"))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()
        @SuppressLint("DrawAllocation") val p = Paint()
        p.isAntiAlias = true
        if (movieStart == 0L) movieStart = now
        val relTime: Int = ((now - movieStart) % movie.duration()).toInt()
        movie.setTime(relTime)
        movie.draw(canvas, 0f, 0f)
        this.invalidate()
    }
}