package com.drolmen.nokotlintest;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by drolmen on 2017/12/11.
 */

public class BrushElement {

    //这个控制笔锋的控制值
    static float DIS_VEL_CAL_FACTOR = 0.02f;
    //手指在移动的控制笔的变化率  这个值越大，线条的粗细越加明显
    static float WIDTH_THRES_MAX = 0.6f;
    //绘制计算的次数，数值越小计算的次数越多，需要折中
    static int STEPFACTOR = 10;

    /**
     * 需要绘制的点
     */
    private ArrayList<PorterDuffView.Node> mNodeArrays ;


    public BrushElement() {
        mNodeArrays = new ArrayList<>();
    }

    public void addNode(PorterDuffView.Node node) {
        Log.d("BrushElement", "node:" + node);
        mNodeArrays.add(node);
    }

    /**
     * 根据参数计算宽度
     * @param maxWidth  最大宽度
     * @param curVel    改值与两点之间距离成正比
     * @param lastVel   同上，上一次level,可能为0
     * @param curDis    两点之间的距离
     * @param factor    一个参数，恒为1.5
     * @param lastWidth 上一次计算的宽度
     * @return
     */
    private static double calcNewWidth(float maxWidth, double curVel, double lastVel, double curDis,
                               double factor, double lastWidth) {
        double calVel = curVel * 0.6 + lastVel * (1 - 0.6);
        //返回指定数字的自然对数
        //手指滑动的越快，这个值越小，为负数
        double vfac = Math.log(factor * 2.0f) * (-calVel);
        //此方法返回值e，其中e是自然对数的基数。
        //Math.exp(vfac) 变化范围为0 到1 当手指没有滑动的时候 这个值为1 当滑动很快的时候无线趋近于0
        //在次说明下，当手指抬起来，这个值会变大，这也就说明，抬起手太慢的话，笔锋效果不太明显
        //这就说明为什么笔锋的效果不太明显
        double calWidth = maxWidth * Math.exp(vfac);
        //滑动的速度越快的话，mMoveThres也越大
        double mMoveThres = curDis * 0.01f;
        //对之值最大的地方进行控制
        if (mMoveThres > WIDTH_THRES_MAX) {
            mMoveThres = WIDTH_THRES_MAX;
        }
        //滑动的越快的话，第一个判断会走
        if (Math.abs(calWidth - maxWidth) / maxWidth > mMoveThres) {

            if (calWidth > maxWidth) {
                calWidth = maxWidth * (1 + mMoveThres);
            } else {
                calWidth = maxWidth * (1 - mMoveThres);
            }
            //滑动的越慢的话，第二个判断会走
        } else if (Math.abs(calWidth - lastWidth) / lastWidth > mMoveThres) {

            if (calWidth > lastWidth) {
                calWidth = lastWidth * (1 + mMoveThres);
            } else {
                calWidth = lastWidth * (1 - mMoveThres);
            }
        }
        return calWidth;
    }


    public static double calcNewPercent(double curVel, double lastVel, double curDis,
                                      double factor, double lastPercent) {
        double calVel = curVel * 0.6 + lastVel * (1 - 0.6);
        //返回指定数字的自然对数
        //手指滑动的越快，这个值越小，为负数
        double vfac = Math.log(factor * 2.0f) * (-calVel);
        //此方法返回值e，其中e是自然对数的基数。
        //Math.exp(vfac) 变化范围为0 到1 当手指没有滑动的时候 这个值为1 当滑动很快的时候无线趋近于0
        //在次说明下，当手指抬起来，这个值会变大，这也就说明，抬起手太慢的话，笔锋效果不太明显
        //这就说明为什么笔锋的效果不太明显
        double calWidth = Math.exp(vfac);
        //滑动的速度越快的话，mMoveThres也越大
        double mMoveThres = curDis * 0.01f;
        //对之值最大的地方进行控制
        if (mMoveThres > WIDTH_THRES_MAX) {
            mMoveThres = WIDTH_THRES_MAX;
        }
        //滑动的越快的话，第一个判断会走
        if (Math.abs(calWidth - 1) > mMoveThres) {

            if (calWidth > 1) {
                calWidth = 1 + mMoveThres;
            } else {
                calWidth = 1 - mMoveThres;
            }
            //滑动的越慢的话，第二个判断会走
        } else if (Math.abs(calWidth - lastPercent) / lastPercent > mMoveThres) {

            if (calWidth > lastPercent) {
                calWidth = lastPercent * (1 + mMoveThres);
            } else {
                calWidth = lastPercent * (1 - mMoveThres);
            }
        }
        return calWidth;
    }


    public int size() {
        return mNodeArrays.size() ;
    }

    public void drawLastNode(Canvas canvas) {
        if (size() == 0) {
            return;
        }

        PorterDuffView.Node lastNode = mNodeArrays.get(size() - 1);

        if (size() == 1) {
            lastNode.mBrush.move((int)lastNode.x, (int)lastNode.y, lastNode.percent);
            lastNode.mBrush.drawSelf(canvas);
            return;
        }

        PorterDuffView.Node secondLastNode = mNodeArrays.get(size() - 2);

        if (size() == 2) {
            PorterDuffView.Node tempNode = new PorterDuffView.Node();
            tempNode.mBrush = secondLastNode.mBrush;
            tempNode.level = secondLastNode.level;
            tempNode.set(secondLastNode.x, secondLastNode.y, secondLastNode.percent);
            drawLine(lastNode.mBrush, canvas, secondLastNode, lastNode);
            return;
        }

        drawLine(lastNode.mBrush, canvas, secondLastNode, lastNode);
    }

    protected void drawLine(PorterDuffView.Brush brush, Canvas canvas, PorterDuffView.Node fromNode,
                            PorterDuffView.Node endNode) {

        float x_distance = -(fromNode.x - endNode.x);
        float y_distance = -(fromNode.y - endNode.y);

        double curDis = Math.hypot(x_distance, y_distance);
        int factor = 2;
//        if (paint.getStrokeWidth() < 6) {
//            factor = 1;
//        } else if (paint.getStrokeWidth() > 60) {
//            factor = 3;
//        }
        int steps = 1 + (int) (curDis / factor);
        double deltaX = x_distance / steps;
        double deltaY = y_distance / steps;
        double deltaW = -(fromNode.percent - endNode.percent) / steps;
        double deltaA = -(fromNode.alpha - endNode.alpha) / steps;
        float x = fromNode.x;
        float y = fromNode.y;
        float fromPercent = fromNode.percent;
        int a = fromNode.alpha;

        for (int i = 0; i < steps; i++) {
            if (fromPercent < 0.05f) {
                fromPercent = 0.05f;
            }
            //根据点的信息计算出需要把bitmap绘制在什么地方
            brush.move((int) x, (int) y, fromPercent);
            //每次到这里来的话，这个笔的透明度就会发生改变，但是呢，这个笔不用同一个的话，有点麻烦
            //我在这里做了个不是办法的办法，每次呢？我都从新new了一个新的笔，每次循环就new一个，内存就有很多的笔了
            //这里new 新的笔  我放到外面去做了
            //Paint newPaint = new Paint(paint);
            //当这里很小的时候，透明度就会很小，个人测试在3.0左右比较靠谱
            // TODO: 2017/12/12  drolmen add --- alpha值
//            paint.setAlpha((int) (a / 3.0f));
            //第一个Rect 代表要绘制的bitmap 区域，第二个 Rect 代表的是要将bitmap 绘制在屏幕的什么地方
            brush.drawSelf(canvas, null);
            x += deltaX;
            y += deltaY;
            fromPercent += deltaW;
            a += deltaA;
        }
    }


}
