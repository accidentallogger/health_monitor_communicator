package com.kl.visionsdkdemo.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path; // Import Path class
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ccl on 2017/8/30.
 * 画PPG（光电容积脉搏波描记）波形图实例
 * Updated to include a transparent filled region under the wave.
 */

public class PPGDrawWave extends DrawWave<Integer> {

    // Define PPG wave line color (cyan)
    private final static int waveColor = Color.parseColor("#11C8B5"); // Your specified cyan color
    // Define the transparency level for the fill (e.g., 20% opacity)
    private final static int waveFillColor = Color.parseColor("#3311C8B5"); // #33 is 20% alpha
    // Define wave line thickness
    private final static float waveStrokeWidth = 2f;
    // Define X-axis data spacing
    private final static int X_INTERVAL = 2;

    private float mViewWidth;
    private float mViewHeight;
    private float dataMax;
    private float dataMin;
    private float dp; // Data point scaling factor

    private Paint mWavePaint; // Paint for the wave line
    private Paint mFillPaint; // Paint for the filled area under the wave

    public PPGDrawWave() {
        super();
        // Initialize paint for the wave line
        mWavePaint = newPaint(waveColor, waveStrokeWidth);
        mWavePaint.setStyle(Paint.Style.STROKE); // Ensure it's for stroke

        // Initialize paint for the filled area
        mFillPaint = new Paint();
        mFillPaint.setColor(waveFillColor);
        mFillPaint.setStyle(Paint.Style.FILL); // Set style to FILL for the area
        mFillPaint.setAntiAlias(true); // Enable anti-aliasing for smooth edges
    }

    @Override
    public void initWave(float width, float height) {
        mViewWidth = width;
        mViewHeight = height;
        allDataSize = mViewWidth / X_INTERVAL;
    }

    @Override
    public void clear() {
        super.clear();
        dataMax = dataMin = 0;
        dp = 0f;
    }

    @Override
    public void drawWave(Canvas canvas) {
        final List<Integer> list = new ArrayList<>(dataList);
        int size = list.size();

        if (size >= 2) {
            // Find min/max data values for scaling
            dataMax = dataMin = list.get(0);
            for (int i = 0; i < size; i++) {
                try {
                    float dataI = list.get(i);
                    if (dataI < dataMin) {
                        dataMin = dataI;
                    }
                    if (dataI > dataMax) {
                        dataMax = dataI;
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            // Calculate data point scaling factor
            // Ensure dataMax - dataMin is not zero to avoid division by zero
            float effectiveHeight = mViewHeight - (mViewHeight / 10 * 2); // Height excluding top/bottom margins
            dp = (dataMax - dataMin) / effectiveHeight;
            if (dp == 0) {
                dp = 1f; // Prevent division by zero if all data points are the same
            }

            // Create a Path for the filled area
            Path fillPath = new Path();

            // Start the path at the bottom-left of the graph area (or where the first point would be on the baseline)
            // Assuming the baseline is mViewHeight - mViewHeight / 10
            float startX = getX(0, size);
            float baselineY = mViewHeight - mViewHeight / 10;
            fillPath.moveTo(startX, baselineY);

            // Add the wave points to the path
            for (int i = 0; i < size; i++) {
                Integer ppgData = list.get(i);
                float x = getX(i, size);
                float y = getY(ppgData);
                if (i == 0) {
                    fillPath.lineTo(x, y); // Move to the first actual data point
                } else {
                    fillPath.lineTo(x, y);
                }
            }

            // Close the path by drawing down to the baseline and back to the start
            float endX = getX(size - 1, size);
            fillPath.lineTo(endX, baselineY); // Go down from the last point to the baseline
            fillPath.lineTo(startX, baselineY); // Go along the baseline back to the start
            fillPath.close(); // Close the path to form a complete shape

            // Draw the filled area first
            canvas.drawPath(fillPath, mFillPaint);

            // Now draw the wave line on top
            for (int i = 0; i < size - 1; i++) {
                Integer ppgDataCurr;
                Integer ppgDataNext;
                try {
                    ppgDataCurr = list.get(i);
                } catch (IndexOutOfBoundsException e) {
                    ppgDataCurr = list.get(i - 1);
                }
                try {
                    ppgDataNext = list.get(i + 1);
                } catch (IndexOutOfBoundsException e) {
                    ppgDataNext = list.get(i);
                }

                float x1 = getX(i, size);
                float x2 = getX(i + 1, size);
                float y1 = getY(ppgDataCurr);
                float y2 = getY(ppgDataNext);
                canvas.drawLine(x1, y1, x2, y2, mWavePaint);
            }
        } else {
            // Draw a flatline when no data is available
            if (mViewWidth > 0 && mViewHeight > 0) {
                float centerY = mViewHeight / 2;
                canvas.drawLine(0, centerY, mViewWidth, centerY, mWavePaint);
            }
        }
    }

    @Override
    protected float getX(int value, int size) {
        try {
            return mViewWidth - ((size - 1 - value) * X_INTERVAL);
        } catch (NullPointerException e) {
            return 0;
        }
    }

    @Override
    protected float getY(Integer ppgData) {
        try {
            // The Y-axis is inverted in Android (0 at top, max at bottom)
            // mViewHeight - mViewHeight / 10 is the bottom margin/baseline
            // (ppgData - dataMin) / dp scales the data to the view height
            return mViewHeight - mViewHeight / 10 - (ppgData - dataMin) / (dp);
        } catch (NullPointerException e) {
            return 0;
        }
    }
}
