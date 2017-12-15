package com.drolmen.nokotlintest;

import android.graphics.Canvas;

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
     * 需要绘制的原始的点
     */
    private ArrayList<Node> mNodeArrays ;

    /**
     * 需要绘制的贝塞尔曲线对应的点
     */
    private ArrayList<Node> mSmoothNodeArrays ;

    private Bezier mBezierHelp ;    //用于绘制贝塞尔曲线，使线条平滑

    private boolean needAlpha ;

    public BrushElement(boolean needAlpha) {
        mNodeArrays = new ArrayList<>();
        mSmoothNodeArrays = new ArrayList<>();
        mBezierHelp = new Bezier();
        this.needAlpha = needAlpha;
    }

    public void addNode(Node node, double curs) {

        mNodeArrays.add(node);
        if (size() == 2) {
            mBezierHelp.init(mNodeArrays.get(size() - 2), mNodeArrays.get(size() - 1));
            return;
        }
        mBezierHelp.addNode(node);
        if (curs != 0) {
            moveNeetToDo(curs, mNodeArrays.get(size() - 2).mBrush);
        }
    }

    public void addEndNode(Node node, double curs) {
        mNodeArrays.add(node);
        if (size() == 2) {
            mBezierHelp.init(mNodeArrays.get(size() - 2), mNodeArrays.get(size() - 1));
            return;
        }
        mBezierHelp.addNode(node);
        if (curs != 0) {
            moveNeetToDo(curs, mNodeArrays.get(size() - 2).mBrush);
        }
        mBezierHelp.end();
        if (curs != 0) {
            moveNeetToDo(curs, mNodeArrays.get(size() - 2).mBrush);
        }
    }

    protected void moveNeetToDo(double curDis, PorterDuffView.Brush brush) {
        int steps = 1 + (int) curDis / STEPFACTOR;
        double step = 1.0 / steps;
        for (double t = 0; t < 1.0; t += step) {
            Node point = mBezierHelp.getPoint(t);
            getWithPointAlphaPoint(point);
            point.mBrush = brush;
            mSmoothNodeArrays.add(point);
        }
    }

    private void getWithPointAlphaPoint(Node point) {
        int alpha = (int) (255 * point.percent / 2);
        if (alpha < 10) {
            alpha = 10;
        } else if (alpha > 255) {
            alpha = 255;
        }
        point.alpha = alpha;
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

        Node lastNode = mNodeArrays.get(size() - 1);

        if (size() == 1) {
            lastNode.mBrush.move((int)lastNode.x, (int)lastNode.y, lastNode.percent);
            lastNode.mBrush.drawSelf(canvas);
            return;
        }

        Node secondLastNode = mNodeArrays.get(size() - 2);

        if (size() == 2) {
            Node tempNode = new Node();
            tempNode.mBrush = secondLastNode.mBrush;
            tempNode.level = secondLastNode.level;
            tempNode.set(secondLastNode.x, secondLastNode.y, secondLastNode.percent);
            drawLine(lastNode.mBrush, canvas, secondLastNode, lastNode);
            return;
        }

        drawLine(lastNode.mBrush, canvas, secondLastNode, lastNode);
    }

    public void drawNode(Canvas canvas) {
        if (mSmoothNodeArrays.size() == 0) {
            return;
        }

        Node lastNode = mSmoothNodeArrays.get(0);

        if (size() == 1) {
            lastNode.mBrush.move((int)lastNode.x, (int)lastNode.y, lastNode.percent);
            lastNode.mBrush.drawSelf(canvas);
            return;
        }

        for (int i = 1; i < mSmoothNodeArrays.size(); i++) {
            Node point = mSmoothNodeArrays.get(i);
            drawLine(lastNode.mBrush, canvas, lastNode, point);
            lastNode = point;
        }

    }

    protected void drawLine(PorterDuffView.Brush brush, Canvas canvas, Node fromNode,
                            Node endNode) {
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
            //第一个Rect 代表要绘制的bitmap 区域，第二个 Rect 代表的是要将bitmap 绘制在屏幕的什么地方
            if (needAlpha) {
                PorterDuffView.Brush.testPaint.setAlpha(a);
                brush.drawSelf(canvas, PorterDuffView.Brush.testPaint);
            } else {
                brush.drawSelf(canvas);
            }
            x += deltaX;
            y += deltaY;
            fromPercent += deltaW;
            a += deltaA;
        }
    }

    public Node getLastNode() {
        return mNodeArrays.get(mNodeArrays.size() - 1);
    }

    public static class Node {
        protected PorterDuffView.Brush mBrush ;
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
                    ", alpha=" + alpha +
                    ", level=" + level +
                    ", percent=" + percent +
                    '}';
        }
    }
}
