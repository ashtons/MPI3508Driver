package com.schimera.example.touch;

import android.util.Log;

import java.io.IOException;
import java.io.Serializable;

import eu.vranckaert.driver.touch.driver.XPT2046Driver;
import eu.vranckaert.driver.touch.exception.TouchDriverReadingException;
import eu.vranckaert.driver.touch.profile.SPIDriverProfile;

public class MPI3508Driver extends XPT2046Driver implements Serializable {
    private static final String TAG = MPI3508Driver.class.getSimpleName();

    public MPI3508Driver(SPIDriverProfile driverProfile) {
        super(driverProfile, true, true, true, true, true);
    }

    @Override
    public int getSpiChannel() {
        return 1;
    }

    @Override
    public boolean isPressing(byte[] buffer) {
        return buffer[1] != 0;
    }

    @Override
    public int getVersion() {
        return 1;
    }
    @Override
    public TouchInput getTouchInput() throws TouchDriverReadingException {
        return MPI3508(); // Parameters
    }

    private TouchInput MPI3508() throws TouchDriverReadingException {
        try {
            mTouchscreen.transfer(xRead, xBuffer, 3);
            mTouchscreen.transfer(yRead, yBuffer, 3);
        } catch (IOException e) {
            Log.e(TAG, "Cannot process input readings from touch screen", e);
            Log.w(TAG, "Shutting down driver due to errors in reading!");
            throw new TouchDriverReadingException();
        }

        byte[] buffer = concat(xBuffer, yBuffer);
        boolean press = isPressing(buffer);

        int screenWidth = getDriverProfile().getScreenDimension().getWidth();
        int screenHeight = getDriverProfile().getScreenDimension().getHeight();
        float halfScreenWidth = screenWidth / 2f;
        float halfScreenHeight = screenHeight / 2f;

        int originalX = (buffer[2] + (buffer[1] << 8) >> 4);
        int originalY = (buffer[5] + (buffer[4] << 8) >> 4);
        if (switchXY) {
            int temp = originalY;
            originalY = originalX;
            originalX = temp;
        }
        Log.d(TAG, "Original X = " + originalX + ", Original Y = " + originalY);
        int x = (int) ((originalX / 2030f) * screenWidth);
        int y = (int) ((originalY / 2100f) * screenHeight);

        int yErrorMargin = 24; // TODO make parameter
        float halfYDistance = halfScreenHeight - yErrorMargin;
        float travelledYDistance = y < halfScreenHeight ? halfYDistance - y - yErrorMargin : y - halfScreenHeight - yErrorMargin;
        int applicableYErrorMargin = (int) (((1 / halfYDistance) * travelledYDistance) * yErrorMargin);
        if (y < halfScreenHeight) {
            y = Math.max(0, y - applicableYErrorMargin);
        } else if (y > halfScreenHeight) {
            y = Math.min(screenHeight, y + applicableYErrorMargin);
        }

        int xErrorMargin = 20; // TODO make parameter
        float halfXDistance = halfScreenWidth - xErrorMargin;
        float travelledXDistance = x < halfScreenWidth ? halfXDistance - x - xErrorMargin : x - halfScreenWidth - xErrorMargin;
        int applicableXErrorMargin = (int) (((1 / halfXDistance) * travelledXDistance) * xErrorMargin);
        if (x < halfScreenWidth) {
            x = Math.max(0, x - applicableXErrorMargin);
        } else {
            x = Math.min(screenWidth, x + applicableXErrorMargin);
        }

        if (inverseX) {
            x = (int) (x < halfScreenWidth ? halfScreenWidth+ (Math.abs(halfScreenWidth - x)) : halfScreenWidth - (Math.abs(halfScreenWidth - x)));
        }
        if (inverseY) {
            y = (int) (y < halfScreenHeight ? halfScreenHeight + (Math.abs(halfScreenHeight - y)) : halfScreenHeight - (Math.abs(halfScreenHeight - y)));
        }

        long millisSinceLastTouch = System.currentTimeMillis() - xyTime;
        boolean outlierX = false;
        boolean outlierY = false;
        boolean shiverring = false;
        boolean keepsPressing = press && mIsPressing;
        if (keepsPressing && !mOutlinerDetected) {
            boolean fastXyTracking = millisSinceLastTouch <= 50;
            if (fastXyTracking && cX != -1) {
                int xOffset = Math.abs(x - cX);
                if (flakeynessCorrection && xOffset > 12) {
                    outlierX = true;
                    x = cX;
                } else if (shiverringCorrection && xOffset <= 12 && xOffset > 0) {
                    shiverring = true;
                    x = cX;
                }
            }
            if (fastXyTracking && cY != -1) {
                int yOffset = Math.abs(y - cY);
                if (flakeynessCorrection && yOffset > 12) {
                    outlierY = true;
                    y = cY;
                } else if (shiverringCorrection && yOffset <= 12 && yOffset > 0) {
                    shiverring = true;
                    y = cY;
                }
            }
        }
        mOutlinerDetected = outlierX || outlierY;

        if (press) {
            Log.v(TAG, "x,y=" + originalX + "," + originalY + " | x,y=" + x + "," + y + " | cx,cy=" + cX + "," + cY + " | dx,dy=" + applicableXErrorMargin + "," + applicableYErrorMargin + " (" + millisSinceLastTouch + "ms)" + (outlierX ? " CORRECTED-X!!!" : "") + (outlierY ? " CORRECTED-Y!!!" : "") + (shiverring ? " SHIVERRING!!!" : ""));
        } else if (mIsPressing && !press) {
            Log.v(TAG, "release");
        }

        mIsPressing = press;
        TouchInput touchInput = new TouchInput(x, y, press);

        if (press) {
            xyTime = System.currentTimeMillis();
            cX = x;
            cY = y;
        } else {
            cX = -1;
            cY = -1;
        }

        return touchInput;
    }
}
