package com.drolmen.nokotlintest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.icu.text.LocaleDisplayNames;
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

    private Node mLastNode;

    public static final int SENSITIVITY = 2;    //笔触灵敏度，每次绘制点间隔
    public static final float MAX_VELOCITY = 124f;

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

        mLastNode = new Node();
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
        mLastNode.set(event.getX(), event.getY(), 1f);

        mTracker.addMovement(event);
        Bitmap bitmap = mBrushList.get(5).mBrushBitmap;
        drawBitmapToCache(bitmap,
                event.getX() - bitmap.getWidth() / 2,
                event.getY() - bitmap.getHeight() / 2);
        invalidate();
    }

    private void onMove(MotionEvent event) {
        mTracker.addMovement(event);
        mTracker.computeCurrentVelocity(200, MAX_VELOCITY);

        float v = (float) Math.hypot(mTracker.getXVelocity(), mTracker.getYVelocity());
        float percent = 1 - computePercent(v);

        int x = (int) event.getX();
        int y = (int) event.getY();
        int index = getIndex(x - mLastNode.x, y - mLastNode.y);
        drawBitmapToCache(index, x, y, percent);
        invalidate();
    }

    private void onUp(MotionEvent event) {
        mTracker.clear();
        Bitmap bitmap = mBrushList.get(5).mBrushBitmap;
        drawBitmapToCache(bitmap,
                event.getX() - bitmap.getWidth() / 2,
                event.getY() - bitmap.getHeight() / 2);
        invalidate();
    }

    private void drawBitmapToCache(Bitmap bitmap, float left, float top) {
        mCacheCanvas.getCanvas().drawBitmap(bitmap, left, top, mPaint);
    }

    private void drawBitmapToCache(int index, int centerX, int centerY, float percent) {
        Brush brush = mBrushList.get(index);

        float x_distance = centerX - mLastNode.x;
        float y_distance = centerY - mLastNode.y;
        float percent_distance = percent - mLastNode.percent;

        //两点之间直线距离
        float hypot = (float) Math.hypot(x_distance, y_distance);
//        if (hypot < SENSITIVITY) {
//            mLastNode.x = centerX ;
//            mLastNode.y = centerY;
//            mLastNode.percent = percent;
//            brush.move(centerX, centerY, percent);
//            brush.drawSelf(mCacheCanvas.getCanvas());
//            return;
//        }

        //steps 等于需要绘制的次数
        float steps = hypot / SENSITIVITY;

        //计算每一步的变化量
        float x_per_step = x_distance / steps;
        float y_per_step = y_distance / steps;
        float v_per_step = percent_distance / steps;

        float beginX = mLastNode.x ;
        float beginY = mLastNode.y ;
        float beginPercent = mLastNode.percent;

        do {
            beginX += x_per_step;
            beginY += y_per_step;
            beginPercent += v_per_step;

            brush.move((int) beginX, (int) beginY, beginPercent);
            brush.drawSelf(mCacheCanvas.getCanvas());
            steps--;
        } while (steps >= 0);

        mLastNode.set(centerX, centerY, percent);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mCacheCanvas != null) {
            mCacheCanvas.draw(canvas);
        }
    }

    private float computePercent(float v) {
        if (v > MAX_VELOCITY) {
            v = MAX_VELOCITY - 10;
        } else if (v < MAX_VELOCITY / 2) {
            v = MAX_VELOCITY / 2;
        }
        return v / 124;
    }

    private int getIndex(float x_vector, float y_vector) {
        int result = 0 ;

        //求两个向量之间的夹角(x_vector, y_vector) (1,0)
        double acos = Math.acos(x_vector / (Math.hypot(x_vector, y_vector))) / Math.PI * 180;

        if (x_vector > 0 && y_vector > 0) { //第一象限
            //不处理
        } else if (x_vector < 0 && y_vector > 0) {  //第二象限
            //也不处理
        } else if (x_vector < 0 && y_vector < 0) {  //第三象限
            acos = 360 - acos;
        } else if (x_vector > 0 && y_vector < 0) {  //第四象限
            acos = 360 - acos;
        }

        System.out.print("arc = " + acos + "    ");

        if (acos <= 10 || acos >= 350 || (acos >= 170 && acos <= 190)) {    //左→右、右→左
            result =  2;
        } else if ((acos > 10 && acos < 80) || (acos > 100 && acos < 170)) { //左下→右上，右下→左上
            result = 1;
        } else if ((acos >= 80 && acos <= 100) || (acos >= 260 && acos <= 280)) { //上→下、下→上
            result = 4 ;
        } else if ((acos > 190 && acos < 260) || (acos > 280 && acos < 350)) {  //左上→右下 右上→左下
            result = 3 ;
        }

        return result;
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
            width = mBrushBitmap.getWidth() / 2;
            height = mBrushBitmap.getHeight() / 2;
            mSrc = new Rect(0, 0, width, height);
            mDes = new Rect(0, 0, width, height);
        }

        public void move(int x, int y , float percent) {
            Log.d("------>", "move() called with: x = [" + x + "], y = [" + y + "], percent = [" + percent + "]");
            mSrc.offsetTo(x - width / 2, y - height / 2);

            int x_change = (int) (percent * width) / 2;
            int y_change = (int) (percent * height) / 2;
            mDes.set(mSrc.left + x_change, mSrc.top + y_change,
                    mSrc.right - x_change, mSrc.bottom - y_change);
        }

        public void drawSelf(Canvas canvas) {
            canvas.drawBitmap(mBrushBitmap, null, mDes, null);

        }
    }

    public static class Node {
        private float x;
        private float y;
        private float percent ; //宽百分比，使用该实行，要保证所有图片尺寸一致

        public Node() {
        }

        public Node(float x, float y, float percent) {
            this.x = x;
            this.y = y;
            this.percent = percent;
        }

        public void set(float x, float y, float percent) {
            this.x = x;
            this.y = y;
            this.percent = percent;
        }
    }

    public void clear() {
        mCacheCanvas.getCanvas().drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
    }
}
