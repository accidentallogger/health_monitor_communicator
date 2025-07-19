package com.kl.visionsdkdemo.view.ecg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.kl.visionsdkdemo.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChartView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    /**
     * 每30帧刷新一次屏幕
     **/
    public static final int TIME_IN_FRAME = 30;

    private SurfaceHolder mHolder;
    private Canvas mCanvas;
    private Thread t;
    private boolean isRunning;
    private Paint linePaint;
    private List<PointF> datas;
    private float gapX = 0.2f;
    private int delGap = 50;
    private int index = 0;
    private PointF startPoint;
    private PointF endPoint;
    private boolean isDelEffect = true;
    private boolean isDraw = false;
    private Path path1, path2;
    private int pointIndex;
    private List<Integer> nativeDatas = null;
    private volatile ThreadPoolExecutor singleThreadExecutor;
    private int sampleRate = 512;
    private int drawPointCostTime = Math.round(1000F/512);
    private int pagerSpeed = 1;
    private float gain = 1f;
    private int allDataSize;
    private float totalLattices;
    private float dataSpacing;
    private float mViewWidth;
    private float mViewHalfHeight;
    private float xS;
    private boolean isClearData = false;

    private float mTotalWidth = 0;
    private float mViewportLeft = 0;
    private boolean mIsManualScrolling = false;
    private float mViewportWidth;
    private boolean mAutoScroll = true;
    private float mLastTouchX = -1;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.0f;
    private float mMaxScaleFactor = 5.0f;
    private float mMinScaleFactor = 0.5f;
    private Paint mGridPaint;
    private Paint mTextPaint;
    private boolean mShowGrid = true;

    public ChartView(Context context) {
        this(context, null);
    }

    public ChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mHolder = getHolder();
        mHolder.addCallback(this);

        setZOrderOnTop(true);
        mHolder.setFormat(PixelFormat.TRANSLUCENT);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setKeepScreenOn(true);
        linePaint = new Paint();
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setColor(getResources().getColor(R.color.colorPrimary));
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        mGridPaint = new Paint();
        mGridPaint.setColor(Color.GRAY);
        mGridPaint.setStrokeWidth(1f);
        mGridPaint.setAlpha(100);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(24f);
        mTextPaint.setAntiAlias(true);

        datas = new ArrayList<PointF>();
        nativeDatas = new ArrayList<>();

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());


        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleDetector.onTouchEvent(event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mLastTouchX = event.getX();
                        mIsManualScrolling = true;
                        mAutoScroll = false;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (!mScaleDetector.isInProgress()) {
                            float dx = event.getX() - mLastTouchX;
                            mViewportLeft -= dx / mScaleFactor;
                            mLastTouchX = event.getX();
                            isDraw = true;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mIsManualScrolling = false;
                        if (mViewportLeft >= mTotalWidth - mViewportWidth / mScaleFactor * 1.5f) {
                            mAutoScroll = true;
                        }
                        break;
                }
                return true;
            }
        });

    }
    private boolean isGraphCutOff() {
        return mTotalWidth > mViewportWidth / mScaleFactor;
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        t = new Thread(this);
        t.start();
        startSingleThreadExecutor();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = getWidth();
        mViewportWidth = mViewWidth;
        mViewHalfHeight = getHeight() / 2.0f;
        xS = EcgBackgroundView.xS;
        totalLattices = EcgBackgroundView.totalLattices;
        final float dataPerLattice = sampleRate / (25.0f * pagerSpeed);
        allDataSize = (int) (totalLattices * dataPerLattice);
        dataSpacing = xS / dataPerLattice;
        if (isGraphCutOff()) {
            float requiredScale = mViewportWidth / mTotalWidth;
            mScaleFactor = Math.max(mMinScaleFactor, Math.min(requiredScale, mMaxScaleFactor));
            isDraw = true;
        }
    }

    public void fitGraphToView() {
        if (datas == null || datas.isEmpty()) return;

        mTotalWidth = datas.size() * dataSpacing;
        float requiredScale = mViewportWidth / mTotalWidth;
        mScaleFactor = Math.max(mMinScaleFactor, Math.min(requiredScale, mMaxScaleFactor));
        mViewportLeft = 0;
        mAutoScroll = false;
        isDraw = true;
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;

        if (datas != null) {
            datas.clear();
        }

        if (mHolder != null) {
            mHolder.removeCallback(null);
        }

        if (singleThreadExecutor != null && !singleThreadExecutor.isShutdown()) {
            singleThreadExecutor.shutdownNow();
        }

        index = 0;
    }

    @Override
    public void run() {
        while (isRunning) {
            if (isDraw) {
                long startTime = System.currentTimeMillis();
                draw();
                isDraw = false;
                long endTime = System.currentTimeMillis();

                int diffTime = (int) (endTime - startTime);

                while (diffTime <= TIME_IN_FRAME) {
                    diffTime = (int) (System.currentTimeMillis() - startTime);
                    Thread.yield();
                }
            }
        }
    }

    public synchronized void draw() {
        try {
            mCanvas = mHolder.lockCanvas();
            if (mCanvas != null) {

                mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                mCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));

                if (mShowGrid) {
                    drawGrid(mCanvas);
                }
                drawLine();

                drawTimeIndicators(mCanvas);
            }
        } catch (Exception e) {
            Log.e("ChartView", "Error in drawing: " + e.getMessage());
        } finally {
            if (mCanvas != null) {
                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }

    private void drawGrid(Canvas canvas) {
        float gridSpacing = mViewHalfHeight / 5;
        for (int i = 0; i <= 10; i++) {
            float y = i * gridSpacing;
            canvas.drawLine(0, y, mViewWidth, y, mGridPaint);
        }
        float secondsSpacing = sampleRate * dataSpacing;
        float startX = -mViewportLeft * mScaleFactor % secondsSpacing;
        for (float x = startX; x < mViewWidth; x += secondsSpacing * mScaleFactor) {
            canvas.drawLine(x, 0, x, mViewHalfHeight * 2, mGridPaint);
        }
    }



    private void drawTimeIndicators(Canvas canvas) {
        float secondsSpacing = sampleRate * dataSpacing;
        float startX = -mViewportLeft * mScaleFactor % secondsSpacing;
        for (float x = startX; x < mViewWidth; x += secondsSpacing * mScaleFactor) {
            int seconds = (int) ((mViewportLeft + x / mScaleFactor) / secondsSpacing);
            canvas.drawText(seconds + "s", x + 5, 30, mTextPaint);
        }
    }

    private void drawLine() {
        if (datas == null || datas.size() < 2) {
            return;
        }
        mTotalWidth = datas.size() * dataSpacing;
        if (isGraphCutOff() && mAutoScroll) {
            float requiredScale = mViewportWidth / mTotalWidth;
            mScaleFactor = Math.max(mMinScaleFactor, Math.min(requiredScale, mMaxScaleFactor));
        }
if (mAutoScroll && !mIsManualScrolling) {
            float targetLeft = mTotalWidth - mViewportWidth / mScaleFactor;
            mViewportLeft = targetLeft;
        }
        mViewportLeft = Math.max(0, Math.min(mViewportLeft, Math.max(0, mTotalWidth - mViewportWidth / mScaleFactor)));
        int startIndex = (int) Math.floor(mViewportLeft / dataSpacing);
        int endIndex = (int) Math.ceil((mViewportLeft + mViewportWidth / mScaleFactor) / dataSpacing);
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(datas.size() - 1, endIndex);
        Path path = new Path();
        boolean firstPoint = true;
        for (int i = startIndex; i <= endIndex; i++) {
            PointF point = datas.get(i);
            float x = (point.x - mViewportLeft) * mScaleFactor;
            float y = point.y;

            if (firstPoint) {
                path.moveTo(x, y);
                firstPoint = false;
            } else {
                path.lineTo(x, y);
            }
        }

        mCanvas.drawPath(path, linePaint);
    }
    public synchronized void addPoint(int y) {
        if (!isRunning) {
            return;
        }
        isClearData = false;
        nativeDatas.add(y);

        if (sampleRate == 200) {
            if (nativeDatas.size() >= 64) {
                addPointThreadExecutor(nativeDatas);
                nativeDatas = new ArrayList<>();
            }
        } else if (sampleRate == 512) {
            if (nativeDatas.size() >= 256) {
                addPointThreadExecutor(nativeDatas);
                nativeDatas = new ArrayList<>();
            }
        }
    }

    public synchronized void addPointThreadExecutor(final List<Integer> datas) {
        if (!isRunning || datas == null) {
            return;
        }

        if (singleThreadExecutor == null || singleThreadExecutor.isShutdown()) {
            startSingleThreadExecutor();
            return;
        }

        int threadQueueSize = singleThreadExecutor.getQueue().size();
        if (threadQueueSize >= 5) {
            Log.e("ChartView", "Queue size: " + threadQueueSize);
            return;
        }

        try {
            singleThreadExecutor.execute(() -> {
                List<Integer> nativeData = datas;
                for (int i = 0; i < nativeData.size(); i++) {
                    if (isClearData) {
                        break;
                    }

                    if (sampleRate == 512) {
                        if (threadQueueSize > 2) {
                            if (i % 8 == 0) {
                                SystemClock.sleep(drawPointCostTime * 8L - 2);
                            }
                        } else {
                            if (i % 8 == 0) {
                                SystemClock.sleep(drawPointCostTime * 8L - 1);
                            }
                        }
                    } else if (sampleRate == 200) {
                        if (threadQueueSize > 2) {
                            if (i % 2 == 0) {
                                SystemClock.sleep(8);
                            }
                        } else {
                            if (i % 2 == 0) {
                                SystemClock.sleep(10);
                            }
                        }
                    }
                    addNativePoint(nativeData.get(i));
                }
            });
        } catch (Exception e) {
            Log.e("ChartView", "Error in thread executor: " + e.getMessage());
        }
    }


    public void addNativePoint(int y) {
        if (!isRunning || isClearData) {
            return;
        }
        float xPos = datas.size() * dataSpacing;
        if (this.sampleRate == 512) {
            y = (int) (mViewHalfHeight + y * 18.3 / 128 * xS / 100 * gain);
        } else if (this.sampleRate == 200) {
            float realMv = calcRealMv(y);
            y = (int) (mViewHalfHeight + realMv * xS * gain * 10);
        }
        y = Math.max(0, Math.min(y, (int) (mViewHalfHeight * 2)));
        datas.add(new PointF(xPos, y));
        mTotalWidth = xPos + dataSpacing;
        if (mAutoScroll || (xPos >= mViewportLeft && xPos <= mViewportLeft + mViewportWidth / mScaleFactor)) {
            isDraw = true;
        }
    }

    private float calcRealMv(int point) {
        return (float) (((point) * 12.247 / 9.5 / 8) / 1000);
    }

    public void clearDatas() {
        if (datas != null) {
            datas.clear();
        }
        isClearData = true;
        startSingleThreadExecutor();
        mTotalWidth = 0;
        mViewportLeft = 0;
        mAutoScroll = true;
        mScaleFactor = 1.0f;
        isDraw = true;
    }

    public void drawView() {
        isDraw = true;
    }

    protected void startSingleThreadExecutor() {
        if (singleThreadExecutor != null && !singleThreadExecutor.isShutdown()) {
            singleThreadExecutor.shutdownNow();
        }

        singleThreadExecutor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10));
    }

    public void setGain(float gain) {
        this.gain = gain;
    }

    public float getGain() {
        return gain;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        drawPointCostTime = Math.round(1000F / sampleRate);
        updateDataParameters();
    }
    public synchronized void setDataPoints(List<Integer> ecgData) {
        if (!isRunning || ecgData == null) return;
        datas.clear();
        float xPos = 0;
        for (Integer y : ecgData) {
            float yPos = mViewHalfHeight + y * 18.3f / 128 * xS / 100 * gain;
            yPos = Math.max(0, Math.min(yPos, mViewHalfHeight * 2));
            datas.add(new PointF(xPos, yPos));
            xPos += dataSpacing;
        }
        mTotalWidth = xPos;
        isDraw = true;
    }

    private void updateDataParameters() {
        final float dataPerLattice = sampleRate / (25.0f * pagerSpeed);
        allDataSize = (int) (totalLattices * dataPerLattice);
        dataSpacing = xS / dataPerLattice;
    }

    public void setShowGrid(boolean show) {
        mShowGrid = show;
        isDraw = true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = mScaleFactor;
            mScaleFactor *= detector.getScaleFactor();
            float minScaleToFit = mViewportWidth / mTotalWidth;
            mMinScaleFactor = Math.min(minScaleToFit, 0.5f);
            mScaleFactor = Math.max(mMinScaleFactor, Math.min(mScaleFactor, mMaxScaleFactor));

            float focusX = detector.getFocusX();
            float viewportCenter = mViewportLeft + focusX / oldScale;
            mViewportLeft = viewportCenter - focusX / mScaleFactor;
            mViewportLeft = Math.max(0, Math.min(mViewportLeft, mTotalWidth - mViewportWidth / mScaleFactor));

            isDraw = true;
            return true;
        }
    }


    public Bitmap createFullGraphBitmap() {
        int width = (int) Math.ceil(mTotalWidth);
        int height = getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        if (mShowGrid) {
            drawGrid(canvas, width, height);
        }
        drawCompleteGraph(canvas, width, height);

        return bitmap;
    }

    private void drawCompleteGraph(Canvas canvas, int width, int height) {
        if (datas == null || datas.size() < 2) return;

        Path path = new Path();
        path.moveTo(datas.get(0).x, datas.get(0).y);

        for (int i = 1; i < datas.size(); i++) {
            path.lineTo(datas.get(i).x, datas.get(i).y);
        }

        Paint paint = new Paint(linePaint);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path, paint);
    }

    private void drawGrid(Canvas canvas, int width, int height) {
        float gridSpacing = height / 10f;
        for (int i = 0; i <= 10; i++) {
            float y = i * gridSpacing;
            canvas.drawLine(0, y, width, y, mGridPaint);
        }
       float secondsSpacing = sampleRate * dataSpacing;
        for (float x = 0; x < width; x += secondsSpacing) {
            canvas.drawLine(x, 0, x, height, mGridPaint);
        }
    }
}