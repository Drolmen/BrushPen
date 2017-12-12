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

    private Node mLastNode;
    
    
    private ArrayList<BrushElement> mElementArrays ;
    
    public static final float MAX_VELOCITY = 124f;

    private VelocityTracker mVelocityTracker = VelocityTracker.obtain();

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

        mElementArrays = new ArrayList<>();
        
        mPaint = new Paint();
        mPaint.setAlpha(255);

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
        mVelocityTracker.addMovement(event);
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
        mLastNode.set(event.getX(), event.getY(), 0.8f, 0);

        BrushElement element = new BrushElement();
        element.addNode(mLastNode, 0);

        mElementArrays.add(element);
        invalidate();
    }

    private void onMove(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        int index = getIndex(x - mLastNode.x, y - mLastNode.y);

        Node currentNode = new Node();
        currentNode.mBrush = mBrushList.get(index);
        currentNode.set(x, y);

        double deltaX = x - mLastNode.x;
        double deltaY = y - mLastNode.y;
        //deltaX和deltay平方和的二次方根 想象一个例子 1+1的平方根为1.4 （x²+y²）开根号
        //同理，当滑动的越快的话，deltaX+deltaY的值越大，这个越大的话，curDis也越大
        double curDis = Math.hypot(deltaX, deltaY);

        Log.d("PorterDuffView", "curDis:" + curDis);

        //我们求出的这个值越小，画的点或者是绘制椭圆形越多，这个值越大的话，绘制的越少，笔就越细，宽度越小
        double curVel = curDis * BrushElement.DIS_VEL_CAL_FACTOR;
        double currentPercent;
        //点的集合少，我们得必须改变宽度,每次点击的down的时候，这个事件
        if (getLastElement().size() < 2) {
            currentPercent = BrushElement.calcNewPercent(curVel, mLastNode.level, curDis, 1.5,
                    mLastNode.percent);
            currentNode.percent = (float) currentPercent;
        } else {
            //由于我们手机是触屏的手机，滑动的速度也不慢，所以，一般会走到这里来
            //阐明一点，当滑动的速度很快的时候，这个值就越小，越慢就越大，依靠着mlastWidth不断的变换
            currentPercent = BrushElement.calcNewPercent(curVel, mLastNode.level, curDis, 1.5,
                    mLastNode.percent);
            currentNode.level = curVel;
            currentNode.percent = (float) currentPercent;
        }
        //每次移动的话，这里赋值新的值
        getLastElement().addNode(currentNode, curDis);
        mLastNode = currentNode;
        getLastElement().drawNode(mCacheCanvas.getCanvas());
        invalidate();
    }

    private void onUp(MotionEvent event) {

        mVelocityTracker.computeCurrentVelocity(50);

        //手指抬起一瞬间的速度
        double v = Math.hypot(mVelocityTracker.getXVelocity(), mVelocityTracker.getYVelocity());
        //由速度决定笔锋长度
        Node endNode = new Node();

//        Node lastNode = getLastElement().getLastNode();
//        float endX = (event.getX() - lastNode.x) * 2 + lastNode.x;
//        float endY = (event.getY() - lastNode.y) * 2 + lastNode.y;
//        endNode.set(endX, endY);
        endNode.set(event.getX(), event.getY());
        endNode.percent = 0.1f;

        double deltaX = event.getX() - mLastNode.x;
        double deltaY = event.getY() - mLastNode.y;
        double curDis = Math.hypot(deltaX, deltaY);
        Log.d("PorterDuffView_for_v", "v:" + v + "   curDis = " + curDis);
        //如果用笔画的画我的屏幕，记录他宽度的和压力值的乘，但是哇，这个是不会变的
        // TODO: 2017/12/12  drolmen add --- 这个值 0 还是其他 ？？？？

        getLastElement().addNode(endNode, v*10);
        getLastElement().end();

        mLastNode = endNode;
        getLastElement().drawNode(mCacheCanvas.getCanvas());
        invalidate();
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

        int angle = 30;

        if (acos <= angle || acos >= 360 - angle || (acos >= 180 - angle && acos <= 180 + angle)) {    //左→右、右→左
            result = 2;
        } else {
            if ((acos > angle && acos < 90 - angle) || (acos > 90 + angle && acos < 180 - angle)) { //左下→右上，右下→左上
                result = 1;
            } else if ((acos >= 90 - angle && acos <= 90 + angle) || (acos >= 270 - angle && acos <= 270 + angle)) { //上→下、下→上
                result = 4;
            } else if ((acos > 180 + angle && acos < 270 - angle) || (acos > 270 + angle && acos < 360 - angle)) {  //左上→右下 右上→左下
                result = 3;
            }
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

        public static final Paint testPaint = new Paint();
        {
            testPaint.setColor(Color.BLACK);
            testPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        }

        public Brush(Bitmap brushBitmap) {
            mBrushBitmap = brushBitmap;
            width = mBrushBitmap.getWidth() / 2;
            height = mBrushBitmap.getHeight() / 2;
            mSrc = new Rect(0, 0, width, height);
            mDes = new Rect(0, 0, width, height);
        }

        public void move(int x, int y , float percent) {
            int half_width = (int) (percent * width / 2);
            int half_height = (int) (percent * height / 2);
            mDes.set(x - half_width, y - half_height,
                    x + half_width, y + half_height);
        }

        public void drawSelf(Canvas canvas) {
            canvas.drawBitmap(mBrushBitmap, null, mDes, null);
        }

        public void drawSelf(Canvas canvas, Paint paint) {
            canvas.drawBitmap(mBrushBitmap, null, mDes, paint);
//            canvas.drawRect(mDes,testPaint);
        }
    }

    public static class Node {
        protected Brush mBrush ;
        protected float x;
        protected float y;
        protected int alpha = 255;
        protected double level ;  // TODO: 2017/12/12  drolmen add --- 这个属性的位置要考虑一下
        protected float percent ; //宽百分比，使用该实行，要保证所有图片尺寸一致

        public Node() {
        }

        public void set(float x, float y) {
            this.x = x;
            this.y = y;
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

        public void set(float x, float y, float percent,double level) {
            this.x = x;
            this.y = y;
            this.percent = percent;
            this.level = level;
        }

        public void set(Node node) {
            this.x = node.x;
            this.y = node.y;
            this.alpha = node.alpha;
            this.level = node.level;
            this.percent = node.percent;
            this.mBrush = node.mBrush;
        }


        @Override
        public String toString() {
            return "Node{" +
                    "x=" + x +
                    ", y=" + y +
                    ", y=" + y +
                    ", level=" + level +
                    ", percent=" + percent +
                    '}';
        }
    }

    public void clear() {
        mElementArrays.clear();
        mCacheCanvas.getCanvas().drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
        invalidate();
    }


    public BrushElement getLastElement() {
        return mElementArrays.get(mElementArrays.size() - 1);
    }
}
