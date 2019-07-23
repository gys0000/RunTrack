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
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.annotation.GlideExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * Created by 六掌大人 on 2018/5/3.
 */

public class RunwayView extends View {

    //跑道长度 米
    public static final int RUNWAY_LENGTH = 400;
    //弧度长度 米
    public final static double mRadianLength = 100;
    //跑道宽度
    public static final int RUNWAY_WIDTH = 84;

    public final static int COLOR_BACKGROUND_A = Color.parseColor("#ffffff");
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

    //跑道半径 米
    private double mRatioRadius;
    //场地宽度
    private int mAreaWidth;
    //场地高度
    private int mAreaHeight;
    //直线长度 米
    private double mLineLength;
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

    private Bitmap otherBitmap;

    private RectF otherRect;

    private float originWidth;
    private float originHeight;
    private RectF originRectF;
    private Paint mOtherPaint;

    private List<ViewData> mOtherList = new ArrayList<>();


    public RunwayView(Context context) {
        this(context, null);
    }

    public RunwayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RunwayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context);
    }

    private void init(Context context) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);//抗锯齿
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);//填充样式改为描边
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        mOtherPaint = new Paint();
        mOtherPaint.setAntiAlias(true);//抗锯齿
        mOtherPaint.setDither(true);
        mOtherPaint.setStyle(Paint.Style.FILL);//填充样式改为描边
        mOtherPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        mOtherList = new ArrayList<>();

        mOvalRight = new RectF();
        mOvalLeft = new RectF();

        mPath = new Path();

        mRunPath = new Path();
        mPathMeasure = new PathMeasure();

        mRadialGradient = new RadialGradient(mCurrentPos[0], mCurrentPos[1], 200, COLOR_RUN_STAR, COLOR_BACKGROUND_D, Shader.TileMode.MIRROR);
        mMatrix = new Matrix();
        mRadialGradient.setLocalMatrix(mMatrix);

        //起点图片
        originBitmap = ((BitmapDrawable) context.getResources().getDrawable(R.mipmap.ic_origin)).getBitmap();
        //起点宽高
        originHeight = RUNWAY_WIDTH - 8;
        originWidth = 16;
        originRectF = new RectF();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isFirst) {
            isFirst = false;
            mAreaWidth = getMeasuredWidth();
            //获取宽度
            mWidth = mAreaWidth - RUNWAY_WIDTH * 2;
            //计算单单条直线的比值 400米减去弧度部分
            mLineLength = RUNWAY_LENGTH / 2 - mRadianLength;
            mRatioRadius = mRadianLength / Math.PI;
            //计算每米对应像素 宽度/（单条直线部分加上弧度直径）
            mPixelValue = (mWidth / (mRatioRadius * 2 + mLineLength));
            //计算高度像素 根据长宽比值*像素值
            mHeight = (int) Math.round(mRatioRadius * 2 * mPixelValue);
            mAreaHeight = mHeight + RUNWAY_WIDTH * 2;
            //初始化起点
            mStartX = (int) (RUNWAY_WIDTH + mRatioRadius * mPixelValue);
            mStartY = mAreaHeight - RUNWAY_WIDTH;
            mOvalRight.set((float) ((mLineLength * mPixelValue) + RUNWAY_WIDTH),
                    RUNWAY_WIDTH,
                    mAreaWidth - RUNWAY_WIDTH,
                    mAreaHeight - RUNWAY_WIDTH);
            mOvalLeft.set(RUNWAY_WIDTH,
                    RUNWAY_WIDTH,
                    (float) (mRatioRadius * 2 * mPixelValue + RUNWAY_WIDTH),
                    mAreaHeight - RUNWAY_WIDTH);

            drawRacetrack(mPath);
            mRunPath.moveTo(mStartX, mStartY);
            //跑带外围
            mShader = new LinearGradient(0, 0, mAreaWidth, mAreaHeight,
                    Color.parseColor("#999999"),
                    Color.parseColor("#999999"), Shader.TileMode.REPEAT);

            mPathMeasure.setPath(mPath, false);

            originRectF.set(mStartX - originWidth / 2, mStartY - originHeight / 2, mStartX + originWidth / 2, mStartY + originHeight / 2);

        }
        setMeasuredDimension(mAreaWidth, mAreaHeight);
    }

    /**
     * 绘制跑到的path
     *
     * @param path
     */
    private void drawRacetrack(Path path) {
        //设置到起跑点
        path.moveTo(mStartX, mStartY);
        path.lineTo((float) (mStartX + mLineLength * mPixelValue), mStartY);
        path.arcTo(mOvalRight, 90.5f, -180);
        path.lineTo(mStartX, RUNWAY_WIDTH);
        path.arcTo(mOvalLeft, 270, -180);
        path.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制跑道
        mPaint.setStyle(Paint.Style.STROKE);//填充样式改为描边

        mPaint.setStrokeWidth(RUNWAY_WIDTH);//设置画笔宽度
        mPaint.setShader(mShader);
        canvas.drawPath(mPath, mPaint);

        mPaint.setShader(null);
        mPaint.setColor(COLOR_BACKGROUND_A);
        mPaint.setStrokeWidth(RUNWAY_WIDTH - 8);
        canvas.drawPath(mPath, mPaint);

        mPaint.setColor(COLOR_BACKGROUND_B);
        mPaint.setStrokeWidth(RUNWAY_WIDTH - 36);
        canvas.drawPath(mPath, mPaint);

        mPaint.setColor(COLOR_BACKGROUND_A);
        mPaint.setStrokeWidth(RUNWAY_WIDTH - 40);
        canvas.drawPath(mPath, mPaint);

        mPaint.setColor(COLOR_BACKGROUND_B);
        mPaint.setStrokeWidth(RUNWAY_WIDTH - 68);
        canvas.drawPath(mPath, mPaint);

        mPaint.setColor(COLOR_BACKGROUND_A);
        mPaint.setStrokeWidth(14);
        canvas.drawPath(mPath, mPaint);
        if (mCurrentPos[0] != 0) {
            mMatrix.setTranslate(mCurrentPos[0], mCurrentPos[1]);
            mRadialGradient.setLocalMatrix(mMatrix);
            mPaint.setShader(mRadialGradient);
            mPaint.setStrokeWidth(8);
            canvas.drawPath(mRunPath, mPaint);
            //画圆点
            mPaint.setStyle(Paint.Style.FILL);//填充样式改为描边
            canvas.drawCircle(mCurrentPos[0], mCurrentPos[1], 8, mPaint);
        }
        //起点
        canvas.drawBitmap(originBitmap, null, originRectF, null);

        int i = 10;
        for (ViewData viewData : mOtherList) {
            if (viewData.getmOtherCurrentPos()[0] != 0 && viewData.getBitmap() != null) {
                i += 3;
                if (i > 30) {
                    i = 10;
                }
                canvas.drawBitmap(viewData.getBitmap(), viewData.getmOtherCurrentPos()[0] - i, viewData.getmOtherCurrentPos()[1] - i, mOtherPaint);
            }
        }
//        canvas.save();
//        canvas.restore();
    }

    public void setDataList(List<OtherOneData> data) {
        Log.e("RunwayView", "setDataList: " + data);
        //清空clear
        mOtherList.clear();
        if (data != null) {
            for (OtherOneData datum : data) {
                setOtherOne(datum.getDistance(), datum.getBitmap());
            }
        }
        invalidate();
    }

    //计算位置
    public void setOtherOne(float totalDistance, Bitmap bitmap) {
        float[] mOtherPos = new float[2];
        final float distance = totalDistance % RUNWAY_LENGTH;
        final float percentage = distance / (RUNWAY_LENGTH);
        final float stopD = mPathMeasure.getLength() * percentage;

        mPathMeasure.getPosTan(stopD, mOtherPos, mOtherCurrentTan);

        if (bitmap != null) {
            otherBitmap = bitmap;
            ViewData viewData = new ViewData(mOtherPos, bitmap);
            mOtherList.add(viewData);
        }
//        countCircleNum((int) totalDistance);
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

        invalidate();
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
