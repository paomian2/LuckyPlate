package com.linxz.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.linxz.R;

/**
 * 功能描述:幸运抽奖盘
 * 作者：Linxz
 * E-mail：lin_xiao_zhang@13.com
 * 版本信息：V1.0.0
 * 时间：2017年04月18日  14:54.
 **/
public class LuckyPlate extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private SurfaceHolder mHolder;
    private Canvas mCanvas;

    /**
     * 用于绘制线程的线程
     */
    private Thread thread;

    /**
     * 线程控制开关
     */
    private boolean isRunning;

    /**
     * 奖项
     */
    private String[] mStr = new String[]{"祎博", "嘉华", "重新转", "校章", "小敏", "重新转"};

    /**
     * 奖项对应的图片
     */
    private int[] mImgs = new int[]
            {R.drawable.danfan, R.drawable.ipad, R.drawable.f040, R.drawable.iphone, R.drawable.meizi, R.drawable.f040};

    /**
     * 与图片对应的Bitmap
     */
    private Bitmap[] mImgBitmap;

    /**
     * 盘快对应的颜色
     */
    private int[] mColor = new int[]{0xFFFFC300, 0XFFF17E01, 0xFFFFC300, 0XFFF17E01, 0xFFFFC300, 0XFFF17E01};

    /**
     * 盘快对应的数量
     */
    private int mItemCount = 6;

    /**
     * 整个盘块的范围
     */
    private RectF mRange = new RectF();

    /**
     * 整个盘块的直径
     */
    private int mRadius;

    /**
     * 绘制盘块的画笔
     */
    private Paint mArcPaint;

    /**
     * 绘制文本的画笔
     */
    private Paint mTextPaint;

    /**
     * 盘块滚动的速度
     */
    private double mSpeed = 0;

    /**
     * 盘块绘制角度(volatile:两个线程同时更新，保证线程间线程的可见性)
     */
    private volatile float mStartAngle = 0;

    /**
     * 是否点击了停止按钮
     */
    private boolean isShouldEnd;

    /**
     * 转盘的中心位置
     */
    private int mCenter;

    /**
     * 直接与panddingLeft为准
     */
    private int mPadding;

    /**
     * 转盘背景图
     */
    private Bitmap mBgBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg2);

    /**
     * 转盘文本字体大小(20sp)
     */
    private float mTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());

    public LuckyPlate(Context context) {
        super(context);

    }

    public LuckyPlate(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHolder = getHolder();
        mHolder.addCallback(this);
        //可获取焦点
        setFocusable(true);
        setFocusableInTouchMode(true);
        //设置常亮
        setKeepScreenOn(true);
    }

    public LuckyPlate(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    /**
     * 设置盘为正方形，以最小的边长为准
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = Math.min(getMeasuredWidth(), getMeasuredHeight());

        mPadding = getPaddingLeft();
        //以paddingLeft为基准
        mRadius = width - mPadding * 2;
        //中心点
        mCenter = width / 2;
        //设置宽高一直
        setMeasuredDimension(width, width);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //初始化绘制盘块的画笔
        mArcPaint = new Paint();
        //绘制位图时取出锯齿  http://blog.csdn.net/lovexieyuan520/article/details/50732023
        mArcPaint.setAntiAlias(true);
        //设置防抖动
        mArcPaint.setDither(true);

        //初始化绘制文本的画笔
        mTextPaint = new Paint();
        //画笔颜色，默认白色
        mTextPaint.setColor(0xffffffff);
        mTextPaint.setTextSize(mTextSize);

        //初始化盘块的范围
        mRange = new RectF(mPadding, mPadding, mPadding + mRadius, mPadding + mRadius);

        //初始化图片
        mImgBitmap = new Bitmap[mItemCount];

        for (int i = 0; i < mItemCount; i++) {
            mImgBitmap[i] = BitmapFactory.decodeResource(getResources(), mImgs[i]);
        }

        isRunning = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;
    }


    @Override
    public void run() {
        //不断进行绘制
        while (isRunning) {
            long start = System.currentTimeMillis();//绘制开始时间
            draw();
            long end = System.currentTimeMillis();//绘制结束时间
            //50毫秒绘制一遍
            if (end - start < 50) {
                try {
                    thread.sleep(50 - (end - start));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 进行绘制(核心)
     */
    private void draw() {
        try {
            mCanvas = mHolder.lockCanvas();
            //手机点击HOME键有可能会出现canvas为空的情况
            if (mCanvas != null) {
                //draw something
                //绘制背景
                drawBg();
                //绘制盘块

                float tmpAngle = mStartAngle;
                float sweepAngle = 360 / mItemCount;

                for (int i = 0; i < mItemCount; i++) {
                    //设置没个盘块的颜色
                    mArcPaint.setColor(mColor[i]);
                    mCanvas.drawArc(mRange, tmpAngle, sweepAngle, true, mArcPaint);
                    //绘制文本
                    drawText(tmpAngle, sweepAngle, mStr[i]);
                    //绘制icon
                    drawIcon(tmpAngle, mImgBitmap[i]);
                    tmpAngle += sweepAngle;
                }

                //设置起始角度(不断绘制draw的时候起始角度不一样，出现转动视觉)，转动转盘
                mStartAngle += mSpeed;

                //如果点击了停止方法，缓慢停止转盘
                if (isShouldEnd) {
                    mSpeed--;
                }
                if (mSpeed <= 0) {
                    mSpeed = 0;
                    isShouldEnd = false;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //最终都要停止绘制
            if (mCanvas != null) {
                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }

    /**
     * 绘制背景
     */
    private void drawBg() {
        mCanvas.drawColor(0xffffffff);
        mCanvas.drawBitmap(mBgBitmap, null,
                new RectF(mPadding / 2, mPadding / 2, getMeasuredWidth() - mPadding / 2, getMeasuredHeight() - mPadding / 2), mArcPaint);
    }

    /**
     * 绘制每个盘块的文本
     *
     * @param tmpAngle
     * @param sweepAngle
     * @param string     绘制的文本
     */
    private void drawText(float tmpAngle, float sweepAngle, String string) {
        Path path = new Path();
        path.addArc(mRange, tmpAngle, sweepAngle);
        //mCanvas.drawTextOnPath(string,path,0,0,mTextPaint);这样在弧度上，需要设置水平垂直偏移量
        //让文字居中，水平偏移量为(弧度/2-文字长度/2)
        float textWidth = mTextPaint.measureText(string);
        //水平偏移量,弧度：周长(半径*pi)/平均角度
        int hOffset = (int) (mRadius * Math.PI / mItemCount / 2 - textWidth / 2);
        //垂直偏移量(半径的1/4)
        float vOffset = mRadius / 2 / 4;
        mCanvas.drawTextOnPath(string, path, hOffset, vOffset, mTextPaint);
    }

    /**
     * 绘制每个盘块的icon
     *
     * @param tmpAngle
     * @param bitmap
     */
    private void drawIcon(float tmpAngle, Bitmap bitmap) {
        //确定图片的宽高(直径的1/8)、位置
        //图片中心位置：X坐标：mCenter+r*cos(a) a:弧度角度/2
        //              Y坐标: mCenter+r*sin(a) r:半径/2
        //设置图片的宽度为直径的1/8
        int imgWidth = mRadius / 8;
        // Math.PI/180
        float angle = (float) ((tmpAngle + 360 / mItemCount / 2) * Math.PI / 180);
        //图片中心点X坐标
        int x = (int) (mCenter + mRadius / 2 / 2 * Math.cos(angle));
        //图片中心点Y坐标
        int y = (int) (mCenter + mRadius / 2 / 2 * Math.sin(angle));
        //确定图片的位置
        Rect rect = new Rect(x - imgWidth, y - imgWidth / 2, x + imgWidth / 2, y + imgWidth / 2);
        mCanvas.drawBitmap(bitmap, null, rect, null);
    }


    /**
     * 点击启动旋转(无参：随机)
     */
    public void luckyStart() {
        mSpeed = 50;
        isShouldEnd = false;
    }

    /**
     * 点击启动旋转(指定停止位置)
     *
     * @param index
     */
    public void luckyStart(int index) {
        //每个盘块的角度
        float angle = 360 / mItemCount;

        //计算每一项中奖范围(当前index)  1->150~210
        //                               0->210~270
        float from = 270 - (index + 1) * angle;
        float end = from + angle;

        //设置停下了需要旋转的距离
        float targetFrom = 4 * 360 + from;
        float targetEnd = 4 * 360 + end;

        /**
         * 等差数列计算公式
         * <pe>
         *     v1 -> 0
         *     且每次-1
         *     (v1+0)*(v1+1)/2=targetFrom
         *     v1*v1 + v1- 2*targetFrom=0;
         *     v1=(-1+Math.sqrt(1+8*targetFrom))/2
         * </pe>
         * */
        float v1 = (float) ((-1 + Math.sqrt(1 + 8 * targetFrom)) / 2);
        float v2 = (float) ((-1 + Math.sqrt(1 + 8 * targetEnd)) / 2);

        mSpeed = v1 + Math.random() * (v2 - v1);
        isShouldEnd = false;
    }

    /**
     * 点击停止旋转(指定概率)
     */
    public void luckyEnd() {
        mStartAngle = 0;
        isShouldEnd = true;
    }

    /**
     * 点击停止(随机概率)
     */
    public void lucyEndRondom() {
        isShouldEnd = true;
    }

    /**
     * 是否正在旋转
     */
    public boolean isStart() {
        return mSpeed != 0;
    }

    /**
     * 停止按钮是否已经点击了
     */
    public boolean isShouldEnd() {
        return isShouldEnd;
    }
}
