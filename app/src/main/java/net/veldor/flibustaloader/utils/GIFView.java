package net.veldor.flibustaloader.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.io.IOException;

public class GIFView extends View {
    private final Movie movie;
    private long movie_start;
    public GIFView(Context context) throws IOException {
        super(context);
        movie=Movie.decodeStream(getResources().getAssets().open("loading.gif"));
    }
    public GIFView(Context context, AttributeSet attrs) throws IOException{
        super(context, attrs);
        movie=Movie.decodeStream(getResources().getAssets().open("loading.gif"));
    }
    public GIFView(Context context, AttributeSet attrs, int defStyle) throws IOException {
        super(context, attrs, defStyle);
        movie=Movie.decodeStream(getResources().getAssets().open("loading.gif"));
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now=android.os.SystemClock.uptimeMillis();
        @SuppressLint("DrawAllocation") Paint p = new Paint();
        p.setAntiAlias(true);
        if (movie_start == 0)
            movie_start = now;
        int relTime;
        relTime = (int)((now - movie_start) % movie.duration());
        movie.setTime(relTime);
        movie.draw(canvas,0,0);
        this.invalidate();
    }
}
