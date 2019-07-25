package com.gystry.runview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

public class RunwaySurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private SurfaceHolder surfaceHolder;
    private Canvas canvas;
    private volatile boolean isDrawing;

    //跑道长度 米
    public static final int RUNWAY_LENGTH = 400;
    //弧度长度 米
    public final static double RADIANLENGTH = 100;
    //跑道宽度
    public static final int RUNWAY_WIDTH = 84;

    //直线长度 米    单条直线跑道的长度
    private static final double LINELENGTH = 100;

    public final static int COLOR_BACKGROUND_A = Color.parseColor("#ffffff");//白色
    /**
     * 内部细的跑带颜色
     */
    public final static int COLOR_BACKGROUND_B = Color.parseColor("#20333333");//浅灰
    /**
     * 跑条渐变色开头
     */
    public final static int COLOR_RUN_STAR = Color.parseColor("#DD1717");//金色

    /**
     * 跑条件变色结尾颜色
     */
    public final static int COLOR_BACKGROUND_D = Color.parseColor("#ffffff");//黑色

    //圆环跑道半径 米
    private double mRatioRadius;
    //场地宽度
    private int mAreaWidth;
    //场地高度
    private int mAreaHeight;

    //跑道水平长度
    private int mWidth;
    //垂直高度
    private int mHeight;
    //每米距离对应的像素
    private double mPixelValue;
    //跑道起点
    private int mStartX;
    private int mStartY;

    //为了增加效率 仅在第一次时候进行测量
    private boolean isFirst = true;

    //====================绘制工具=====================
    private Paint mPaint;
    //用于画圆弧
    protected RectF mOvalRight;
    protected RectF mOvalLeft;
    private Path mPath;
    private Shader mShader;

    private Path mRunPath;
    private PathMeasure mPathMeasure;

    //当前圈数
    private int mCircleNum = 0;

    //画圆点
    private float[] mCurrentPos = new float[2];
    private float[] mCurrentTan = new float[2];

    private float[] mOtherCurrentPos = new float[2];
    private float[] mOtherCurrentTan = new float[2];

    private RadialGradient mRadialGradient;
    private Matrix mMatrix;

    //起点
    private Bitmap originBitmap;

    private float originWidth;
    private float originHeight;
    private RectF originRectF;
    private Paint mOtherPaint;

    private List<ViewData> mOtherList = new ArrayList<>();

    private boolean isDrawOrigin;
    private Paint mTestPaint;

    public RunwaySurfaceView(Context context) {
        this(context, null);
    }

    public RunwaySurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RunwaySurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        setFocusable(true);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);//抗锯齿
        mPaint.setDither(true);//防抖动
        mPaint.setStyle(Paint.Style.STROKE);//填充样式改为描边
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        //测试的画笔
        mTestPaint = new Paint();
        mTestPaint.setAntiAlias(true);//抗锯齿
        mTestPaint.setDither(true);//防抖动
        mTestPaint.setStyle(Paint.Style.STROKE);//填充样式改为描边
        mTestPaint.setColor(0xFF046484);
        mTestPaint.setStrokeWidth(2);

        mOtherPaint = new Paint();
        mOtherPaint.setAntiAlias(true);//抗锯齿
        mOtherPaint.setDither(true);
        mOtherPaint.setStyle(Paint.Style.FILL);
        mOtherPaint.setColor(0xFF046484);

        //要绘制的其他用户
        mOtherList = new ArrayList<>();

        //左右圆环跑道的区域
        mOvalRight = new RectF();
        mOvalLeft = new RectF();

        //整个跑道的路径
        mPath = new Path();
        //整个跑道的测量器
        mPathMeasure = new PathMeasure();

        //用户跑道的轨迹路径以及路径的效果
        mRunPath = new Path();
        mRadialGradient = new RadialGradient(mCurrentPos[0], mCurrentPos[1], 200, COLOR_RUN_STAR, COLOR_BACKGROUND_D, Shader.TileMode.MIRROR);
        mMatrix = new Matrix();
        mRadialGradient.setLocalMatrix(mMatrix);

        //起点图片，起点宽高以及七点的区域
        originBitmap = ((BitmapDrawable) context.getResources().getDrawable(R.mipmap.ic_origin)).getBitmap();

        originHeight = RUNWAY_WIDTH - 8;
        originWidth = 16;
        originRectF = new RectF();

        circleRectF = new RectF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isFirst) {
            isFirst = false;
            mAreaWidth = getMeasuredWidth();
            //获取宽度
            mWidth = mAreaWidth - RUNWAY_WIDTH * 2;
            //获取圆环跑道的半径
            mRatioRadius = RADIANLENGTH / Math.PI;

            //计算每米对应像素 宽度/（单条直线部分加上弧度直径）  每一米对应的像素值
            mPixelValue = (mWidth / (mRatioRadius * 2 + LINELENGTH));
            //计算高度像素 根据长宽比值*像素值
            mHeight = (int) Math.round(mRatioRadius * 2 * mPixelValue);
            //整个view的高度,这个高度是根据跑道的比例计算出来的，跟view设置的原始高度可能不一样，在后边需要重新设置view的宽高
            mAreaHeight = mHeight + RUNWAY_WIDTH * 2;
            //初始化起点
            mStartX = (int) (RUNWAY_WIDTH + mRatioRadius * mPixelValue);
            mStartY = mAreaHeight - RUNWAY_WIDTH;

            mOvalRight.set(
                    (float) ((LINELENGTH * mPixelValue) + RUNWAY_WIDTH),
                    RUNWAY_WIDTH,
                    mAreaWidth - RUNWAY_WIDTH,
                    mAreaHeight - RUNWAY_WIDTH);
            mOvalLeft.set(RUNWAY_WIDTH,
                    RUNWAY_WIDTH,
                    (float) (mRatioRadius * 2 * mPixelValue + RUNWAY_WIDTH),
                    mAreaHeight - RUNWAY_WIDTH);

            setRacetrack(mPath);
            mRunPath.moveTo(mStartX, mStartY);
            //跑带外围   渐变
            mShader = new LinearGradient(0, 0, mAreaWidth, mAreaHeight,
                    Color.parseColor("#999999"),
                    Color.parseColor("#999999"), Shader.TileMode.REPEAT);

            mPathMeasure.setPath(mPath, false);

            //其实图片绘制的区域
            originRectF.set(mStartX - originWidth / 2, mStartY - originHeight / 2, mStartX + originWidth / 2, mStartY + originHeight / 2);

        }
        //重新计算宽高
        setMeasuredDimension(mAreaWidth, mAreaHeight);
    }

    /**
     * 绘制跑到的path
     *
     * @param path
     */
    private void setRacetrack(Path path) {
        //设置到起跑点
        path.moveTo(mStartX, mStartY);
        path.lineTo((float) (mStartX + LINELENGTH * mPixelValue), mStartY);
        path.arcTo(mOvalRight, 90.5f, -180);
        path.lineTo(mStartX, RUNWAY_WIDTH);
        path.arcTo(mOvalLeft, 270, -180);
        path.close();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isDrawing = true;
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isDrawing = false;
        surfaceHolder.removeCallback(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run() {
        while (isDrawing) {
            Log.e("RunwaySurfaceView", "run: ");
            draw();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void draw() {
        try {
            canvas = surfaceHolder.lockCanvas();
            //下边是具体的绘制
            canvas.drawColor(0xffffffff);
            //绘制跑道
            mPaint.setStyle(Paint.Style.STROKE);//填充样式改为描边

            //绘制底层跑带
            mPaint.setStrokeWidth(RUNWAY_WIDTH);//设置画笔宽度
            mPaint.setShader(mShader);
            canvas.drawPath(mPath, mPaint);

            //绘制内一层跑带
            mPaint.setShader(null);
            mPaint.setColor(COLOR_BACKGROUND_A);
            mPaint.setStrokeWidth(RUNWAY_WIDTH - 8);
            canvas.drawPath(mPath, mPaint);

            //绘制内二层的跑带
            mPaint.setColor(COLOR_BACKGROUND_B);
            mPaint.setStrokeWidth(RUNWAY_WIDTH - 36);
            canvas.drawPath(mPath, mPaint);

            //绘制内三层的跑带
            mPaint.setColor(COLOR_BACKGROUND_A);
            mPaint.setStrokeWidth(RUNWAY_WIDTH - 40);
            canvas.drawPath(mPath, mPaint);

            //绘制内四层跑带
            mPaint.setColor(COLOR_BACKGROUND_B);
            mPaint.setStrokeWidth(RUNWAY_WIDTH - 68);
            canvas.drawPath(mPath, mPaint);

            //绘制内五层跑带
            mPaint.setColor(COLOR_BACKGROUND_A);
            mPaint.setStrokeWidth(14);
            canvas.drawPath(mPath, mPaint);
            //绘制跑带总结：上边绘制的跑带时底层最大，二层缩小一点，所处内外层的两条线，依次缩小，绘制叠层。

            //当前用户跑的位置的绘制
            if (mCurrentPos[0] != 0) {
                mMatrix.setTranslate(mCurrentPos[0], mCurrentPos[1]);
                mRadialGradient.setLocalMatrix(mMatrix);
                mPaint.setShader(mRadialGradient);
                mPaint.setStrokeWidth(8);
                canvas.drawPath(mRunPath, mPaint);
                //画圆点
                mPaint.setStyle(Paint.Style.FILL);//填充样式改为填充
                canvas.drawCircle(mCurrentPos[0], mCurrentPos[1], 8, mPaint);
            }

            //起点
            canvas.drawBitmap(originBitmap, null, originRectF, null);

            //绘制跑到中其他用户的位置
            int i = 10;
            for (ViewData viewData : mOtherList) {
                if (viewData.getmOtherCurrentPos()[0] != 0 && viewData.getBitmap() != null) {
                    i += 3;
                    if (i > 30) {
                        i = 10;
                    }
//                    canvas.drawBitmap(viewData.getBitmap(), viewData.getmOtherCurrentPos()[0] - i, viewData.getmOtherCurrentPos()[1] - i, mOtherPaint);
                    drawCircleBitmap(canvas, viewData, mOtherPaint, i);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null) {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private RectF circleRectF;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void drawCircleBitmap(Canvas canvas, ViewData viewData, Paint paint, int i) {
        Log.e("RunwaySurfaceView", "drawCircleBitmap: "+viewData.getBitmap().getByteCount() );
        circleRectF.set(viewData.getmOtherCurrentPos()[0] - i, viewData.getmOtherCurrentPos()[1] - i,
                viewData.getmOtherCurrentPos()[0] - i + viewData.getBitmap().getWidth(), viewData.getmOtherCurrentPos()[1] - i + viewData.getBitmap().getHeight());
        int layerSign = canvas.saveLayer(circleRectF, paint);
        canvas.drawOval(circleRectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(viewData.getBitmap(), viewData.getmOtherCurrentPos()[0] - i, viewData.getmOtherCurrentPos()[1] - i, paint);
        paint.setXfermode(null);
        canvas.restoreToCount(layerSign);
    }

    public void setDataList(List<OtherOneData> data) {
        //清空clear
        mOtherList.clear();
        if (data != null) {
            for (OtherOneData datum : data) {
                setOtherOne(datum.getDistance(), datum.getBitmap());
            }
        }
//        invalidate();
    }

    //计算位置
    private void setOtherOne(float totalDistance, Bitmap bitmap) {
        float[] mOtherPos = new float[2];
        final float distance = totalDistance % RUNWAY_LENGTH;
        final float percentage = distance / (RUNWAY_LENGTH);
        final float stopD = mPathMeasure.getLength() * percentage;

        mPathMeasure.getPosTan(stopD, mOtherPos, mOtherCurrentTan);

        if (bitmap != null) {
            ViewData viewData = new ViewData(mOtherPos, bitmap);
            mOtherList.add(viewData);
        }
    }

    //计算位置
    public void setmMovingDistance(float totalDistance) {
        final float distance = totalDistance % RUNWAY_LENGTH;
        final float percentage = distance / (RUNWAY_LENGTH);
        final float stopD = mPathMeasure.getLength() * percentage;
        final float startD = stopD - 200;
        mRunPath.reset();
        mPathMeasure.getSegment(startD, stopD, mRunPath, true);

        mPathMeasure.getPosTan(stopD, mCurrentPos, mCurrentTan);

//        invalidate();
        countCircleNum((int) totalDistance);
    }

    public void countCircleNum(int totalDistance) {
        //计算圈数
        final int num = totalDistance / RUNWAY_LENGTH;
        if (num > mCircleNum) {
            mCircleNum = num;
//            mRunPath.reset();
        }
    }

}
