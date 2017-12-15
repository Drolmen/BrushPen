package com.drolmen.nokotlintest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by drolmen on 2017/12/7.
 */

public class PorterDuffView extends View {

    private static ArrayList<BrushElement.Brush> mLevelList ;
    private BrushElement.Brush mCurrentBrush;

    private CacheCanvas mCacheCanvas ;

    private ArrayList<BrushElement> mElementArrays ;

    private SparseArray<BrushElement> mActivePath;

    public static final float MAX_VELOCITY = 124f;

    public static boolean useAlpha = false;

    public PorterDuffView(Context context) {
        super(context);
        init();
    }

    public PorterDuffView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;

        options.inMutable = true;
        mLevelList = new ArrayList<>();
        mLevelList.add(new BrushElement.Brush(BitmapFactory.decodeResource(getResources(),
                R.mipmap.level_0, options)));
        mLevelList.add(new BrushElement.Brush(BitmapFactory.decodeResource(getResources(),
                R.mipmap.level_1, options)));
        mLevelList.add(new BrushElement.Brush(BitmapFactory.decodeResource(getResources(),
                R.mipmap.level_2, options)));
        mCurrentBrush = mLevelList.get(0);

        mElementArrays = new ArrayList<>();
        mActivePath = new SparseArray<>();
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
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            BrushElement element = mActivePath.get(i);
            if (element != null) {
                continue;
            }
            BrushElement.Node firstNode = new BrushElement.Node();
            firstNode.set(event.getX(), event.getY(), 0.8f, 0);

            element = new BrushElement();
            element.addNode(firstNode, 0);

            mElementArrays.add(element);
        }
        invalidate();
    }

    private void onMove(MotionEvent event) {

        int pointerCount = event.getPointerCount();

        for (int i = 0; i < pointerCount; i++) {
            BrushElement element = mActivePath.get(pointerCount);
            BrushElement.Node lastNode = element.getLastNode();

            int x = (int) event.getX(i);
            int y = (int) event.getY(i);

            BrushElement.Node newMovetNode = new BrushElement.Node();
            newMovetNode.mBrush = mCurrentBrush;
            newMovetNode.set(x, y);

            double deltaX = x - lastNode.x;
            double deltaY = y - lastNode.y;
            //deltaX和deltay平方和的二次方根 想象一个例子 1+1的平方根为1.4 （x²+y²）开根号
            //同理，当滑动的越快的话，deltaX+deltaY的值越大，这个越大的话，curDis也越大
            double curDis = Math.hypot(deltaX, deltaY);

            //我们求出的这个值越小，画的点或者是绘制椭圆形越多，这个值越大的话，绘制的越少，笔就越细，宽度越小
            double curVel = curDis * BrushElement.DIS_VEL_CAL_FACTOR;
            double currentPercent;
            //点的集合少，我们得必须改变宽度,每次点击的down的时候，这个事件
            if (getLastElement().size() < 2) {
                currentPercent = BrushElement.calcNewPercent(curVel, lastNode.level, curDis, 1.5,
                        lastNode.percent);
                newMovetNode.percent = (float) currentPercent;
            } else {
                //由于我们手机是触屏的手机，滑动的速度也不慢，所以，一般会走到这里来
                //阐明一点，当滑动的速度很快的时候，这个值就越小，越慢就越大，依靠着mlastWidth不断的变换
                currentPercent = BrushElement.calcNewPercent(curVel, lastNode.level, curDis, 1.5,
                        lastNode.percent);
                newMovetNode.level = curVel;
                newMovetNode.percent = (float) currentPercent;
            }
            //每次移动的话，这里赋值新的值
            element.addNode(newMovetNode, curDis);
            element.drawNode(mCacheCanvas.getCanvas());
        }


        invalidate();
    }

    private void onUp(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {

            BrushElement element = mActivePath.get(pointerCount);
            BrushElement.Node mLastNode = element.getLastNode();

            //由速度决定笔锋长度
            BrushElement.Node endNode = new BrushElement.Node();
            endNode.set(event.getX(), event.getY());
            endNode.percent = 0.1f;

            double deltaX = event.getX() - mLastNode.x;
            double deltaY = event.getY() - mLastNode.y;
            double curDis = Math.hypot(deltaX, deltaY);
            //如果用笔画的画我的屏幕，记录他宽度的和压力值的乘，但是哇，这个是不会变的
            // TODO: 2017/12/12  drolmen add --- 这个值 0 还是其他 ？？？？

            element.addNode(endNode, curDis);
            element.drawNode(mCacheCanvas.getCanvas());
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mCacheCanvas != null) {
            mCacheCanvas.draw(canvas);
        }
    }

    private BrushElement.Node getLastNodeOfElement(int elementIndex) {
        return mElementArrays.get(elementIndex).getLastNode();
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

    public void setOrCancleAlpha() {
        useAlpha = !useAlpha;
    }

    public void setColor(int color) {
        HandPaintConfig.currentColor = color;
        Canvas canvas = new Canvas(mCurrentBrush.mBrushBitmap);
        canvas.drawColor(color,PorterDuff.Mode.SRC_IN);
    }

    public void setStroke(int level) {
        // 0 1 2
        mCurrentBrush = mLevelList.get(level);
        setColor(Color.BLACK);
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

    public void clear() {
        mElementArrays.clear();
        mCacheCanvas.getCanvas().drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
        invalidate();
    }


    public BrushElement getLastElement() {
        return mElementArrays.get(mElementArrays.size() - 1);
    }
}
