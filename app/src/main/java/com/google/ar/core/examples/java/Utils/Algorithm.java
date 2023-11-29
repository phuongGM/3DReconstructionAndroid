package com.google.ar.core.examples.java.Utils;


import static java.lang.Math.exp;
import static java.lang.Math.floor;
import static java.lang.Math.log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Algorithm {
    static public float[] interpolation(
            int[] colorImage,
            float x,
            float y,
            int width,
            int height,
            float[] FGHistogram,
            float[] BGHistogram,
            int binShift,
            int numBins)
    {
        int color, redIdx = 0, greenIdx = 0, blueIdx = 0;
        int pixelIdx, colorIdx;
        float iPcRf = 0, iPcRb = 0;
        float logPcRf = 0, logPcRb = 0;

        if (x == 0 | x == width - 1 | y == 0 | y == height - 1)
        {
            pixelIdx = (int) (y * width + x);
            color = colorImage[pixelIdx]; // ARGB
//
            // idx in histogram
            redIdx = ((color & 0x00FF0000) >> 16) >> binShift;
            greenIdx = ((color & 0x0000FF00) >> 8) >> binShift;
            blueIdx = (color & 0x000000FF) >> binShift;

            colorIdx = (redIdx * numBins + greenIdx) * numBins + blueIdx;

            iPcRf = FGHistogram[colorIdx] + 1e-10f;
            iPcRb = BGHistogram[colorIdx] + 1e-10f;

            logPcRf = (float) log(iPcRf);
            logPcRb = (float) log(1 - iPcRb);


            return new float[]{(float) exp(logPcRf), (float) exp(logPcRb)};
        }
        int ix = (int) x;
        int iy = (int) y;
        float dx = x - ix;
        float dy = y - iy;
        float dxdy = dx * dy;

        pixelIdx = iy * width + ix;
        color = colorImage[pixelIdx]; // ARGB

        redIdx = ((color & 0x00FF0000) >> 16) >> binShift;
        greenIdx = ((color & 0x0000FF00) >> 8) >> binShift;
        blueIdx = (color & 0x000000FF) >> binShift;

        colorIdx = (redIdx * numBins + greenIdx) * numBins + blueIdx;

        iPcRf = FGHistogram[colorIdx] + 1e-10f;
        iPcRb = BGHistogram[colorIdx] + 1e-10f;

        logPcRf += (1 - dx - dy + dxdy) * (float) log(iPcRf);
        logPcRb += (1 - dx - dy + dxdy) * (float) log(1 - iPcRb);

        // --------------------------------------------------

        pixelIdx = iy * width + ix + 1;
        color = colorImage[pixelIdx]; // ARGB

        redIdx = ((color & 0x00FF0000) >> 16) >> binShift;
        greenIdx = ((color & 0x0000FF00) >> 8) >> binShift;
        blueIdx = (color & 0x000000FF) >> binShift;

        colorIdx = (redIdx * numBins + greenIdx) * numBins + blueIdx;

        iPcRf = FGHistogram[colorIdx] + 1e-10f;
        iPcRb = BGHistogram[colorIdx] + 1e-10f;

        logPcRf += (dx - dxdy) * (float) log(iPcRf);
        logPcRb += (dx - dxdy) * (float) log(1 - iPcRb);

        // --------------------------------------------------
        pixelIdx = iy * width + ix + width;
        color = colorImage[pixelIdx]; // ARGB

        redIdx = ((color & 0x00FF0000) >> 16) >> binShift;
        greenIdx = ((color & 0x0000FF00) >> 8) >> binShift;
        blueIdx = (color & 0x000000FF) >> binShift;

        colorIdx = (redIdx * numBins + greenIdx) * numBins + blueIdx;

        iPcRf = FGHistogram[colorIdx] + 1e-10f;
        iPcRb = BGHistogram[colorIdx] + 1e-10f;

        logPcRf += (dy - dxdy) * (float) log(iPcRf);
        logPcRb += (dy - dxdy) * (float) log(1 - iPcRb);
        // --------------------------------------------------
        pixelIdx = iy * width + ix + width + 1;
        color = colorImage[pixelIdx]; // ARGB

        redIdx = ((color & 0x00FF0000) >> 16) >> binShift;
        greenIdx = ((color & 0x0000FF00) >> 8) >> binShift;
        blueIdx = (color & 0x000000FF) >> binShift;

        colorIdx = (redIdx * numBins + greenIdx) * numBins + blueIdx;


        iPcRf = FGHistogram[colorIdx] + 1e-10f;
        iPcRb = BGHistogram[colorIdx] + 1e-10f;

        logPcRf += dxdy * (float) log(iPcRf);
        logPcRb += dxdy * (float) log(1 - iPcRb);

        return new float[]{(float) exp(logPcRf), 1 - (float) exp(logPcRb)};
    }
}

