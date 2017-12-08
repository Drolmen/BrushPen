package com.drolmen.nokotlintest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by drolmen on 2017/12/7.
 */

public class PorterDuffView extends View {

    private ArrayList<Brush> mBrushList;

    private CacheCanvas mCacheCanvas ;

    private Paint mPaint ;

    private VelocityTracker mTracker ;

    public PorterDuffView(Context context) {
        super(context);
        init();
    }

    public PorterDuffView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
        mBrushList = new ArrayList<>();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        mBrushList.add(new Brush(BitmapFactory.decodeResource(getResources(), R.mipmap._0, options)));
        mBrushList.add(new Brush(BitmapFactory.decodeResource(getResources(), R.mipmap._1, options)));
        mBrushList.add(new Brush(BitmapFactory.decodeResource(getResources(), R.mipmap._2, options)));
        mBrushList.add(new Brush(BitmapFactory.decodeResource(getResources(), R.mipmap._3, options)));
        mBrushList.add(new Brush(BitmapFactory.decodeResource(getResources(), R.mipmap._4, options)));
        mBrushList.add(new Brush(BitmapFactory.decodeResource(getResources(), R.mipmap._5, options)));

        mPaint = new Paint();
        mPaint.setAlpha(255);

        mTracker = VelocityTracker.obtain();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (oldw == 0 && oldh == 0 && w > 0 && h > 0) {
            mCacheCanvas = new CacheCanvas(w, h);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onDown(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                onMove(event);
                break;
            case MotionEvent.ACTION_UP:
                onUp(event);
                break;
        }
        return super.onTouchEvent(event);
    }

    private void onDown(MotionEvent event) {
//        mCacheCanvas.getCanvas().drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
        mTracker.addMovement(event);
        Bitmap bitmap = mBrushList.get(5).mBrushBitmap;
        drawBitmapToCache(bitmap,
                event.getX() - bitmap.getWidth() / 2,
                event.getY() - bitmap.getHeight() / 2);
        invalidate();
    }

    private void onMove(MotionEvent event) {
        mTracker.addMovement(event);
        mTracker.computeCurrentVelocity(100, 124f);
        float v = (float) Math.hypot(mTracker.getXVelocity(), mTracker.getXVelocity());
        drawBitmapToCache(4, (int) event.getX(), (int) event.getY(), 1 - computePercent(v));
        invalidate();
    }

    private void onUp(MotionEvent event) {
        mTracker.clear();
    }

    private void drawBitmapToCache(Bitmap bitmap, float left, float top) {
        mCacheCanvas.getCanvas().drawBitmap(bitmap, left, top, mPaint);
    }

    private void drawBitmapToCache(int index, int centerX, int centerY, float percent) {
        Log.d("------>", "drawBitmapToCache() called with: index = [" + index + "], centerX = [" + centerX + "], centerY = [" + centerY + "], percent = [" + percent + "]");
        Brush brush = mBrushList.get(index);
        brush.move(centerX, centerY, percent);
        brush.drawSelf(mCacheCanvas.getCanvas());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mCacheCanvas != null) {
            mCacheCanvas.draw(canvas);
        }
    }

    private float computePercent(float v) {
        if (Math.abs(v) > 124) {
            v = 124;
        }
        return v / 124;
    }


    public static class CacheCanvas {
        private Bitmap mBitmap ;
        private Canvas mCanvas ;
        public CacheCanvas(int width, int height) {
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        public void draw(Canvas canvas) {
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }

        public Canvas getCanvas() {
            return mCanvas;
        }
    }

    public static class Brush {
        private Bitmap mBrushBitmap ;    //笔尖
        private Rect mSrc ;     // 原来展示区域
        private Rect mDes ;     // 目标展示区域
        private int width ;
        private int height ;

        public Brush(Bitmap brushBitmap) {
            mBrushBitmap = brushBitmap;
            width = mBrushBitmap.getWidth();
            height = mBrushBitmap.getHeight();
            mSrc = new Rect(0, 0, brushBitmap.getWidth(), brushBitmap.getHeight());
            mDes = new Rect(0, 0, brushBitmap.getWidth(), brushBitmap.getHeight());
        }

        public void move(int x, int y , float percent) {
            mSrc.offsetTo(x - width, y - height);

            int x_change = (int) ((1 - percent) * width);
            int y_change = (int) ((1 - percent) * height);
            mDes.set(mSrc.left - x_change, mSrc.top - y_change,
                    mSrc.right + x_change, mSrc.bottom + y_change);
        }

        public void drawSelf(Canvas canvas) {
            canvas.drawBitmap(mBrushBitmap, null, mDes, null);
        }
    }


    public void clear() {
        mCacheCanvas.getCanvas().drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
    }
}
