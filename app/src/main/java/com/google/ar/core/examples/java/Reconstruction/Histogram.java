package com.google.ar.core.examples.java.Reconstruction;


import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.log;

public class Histogram {
    String TAG = "HISTOGRAM";
    private int numBins, sizeOfBin, binShift;
    private int totalF, totalB;

    private float alphaF, alphaB;

    private boolean isInitialized; // Normalized Histogram whether has values or not

    private final int[] notNormalizedFG;
    private final int[] notNormalizedBG;

    private final float[] normalizedFG;
    private final float[] normalizedBG;

    public Histogram(int noBins){
        this.numBins = noBins;
        this.isInitialized = false;
        this.sizeOfBin = 256 / numBins;
        this.binShift = (int) (8 - log(numBins) / log(2));

        this.alphaF = 0.1f;
        this.alphaB = 0.2f;

//        this.alphaF = 0.3f;
//        this.alphaB = 0.4f;


        this.notNormalizedFG = new int[numBins * numBins * numBins];
        this.notNormalizedBG = new int[numBins * numBins * numBins];

        this.normalizedFG = new float[numBins * numBins * numBins];
        this.normalizedBG = new float[numBins * numBins * numBins];

        reset();
    }

    public static Bitmap overlay(int[] colorImage, byte[] maskImage, int width, int height){
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        int[] overlayImage = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++){
                int id = y * width + x;
                if (maskImage[id] == 0)
                    overlayImage[id] = colorImage[id];
                else
                    overlayImage[id] = Color.argb(1, 0,0, 255);

            }
        }
        bmp.setPixels(overlayImage, 0, width, 0, 0, width, height);
        return bmp;
    }


    /**
     * Update histogram and normalize
     * @param colorImage row ordered color image, data type ARGB_8888.
     * @param maskImage row ordered mask identifying foreground and background,data type ARGGB_8888.
     */
    public void update(int[] colorImage, byte[] maskImage, int width, int height) {
//        Bitmap c = overlay(colorImage, maskImage, width, height);

        long start = System.currentTimeMillis();
        buildHistogram(colorImage, maskImage);
        long end = System.currentTimeMillis();
        Log.d(TAG, String.valueOf(end - start));
        normalizeHistogram();

        if (!isInitialized) isInitialized = true;
    }

    public void update(List<int[]> colorImages, List<byte[]> maskImages) {
        buildHistogram(colorImages, maskImages);
        normalizeHistogram();

//        Bitmap x = generateAnotherMask(colorImages.get(0), 1080, 898);
//        int a = x.getHeight();
////        Bitmap c = overlay(colorImages.get(0), maskImages.get(0), width, height);

        if (!isInitialized) isInitialized = true;
    }

    public void reset() {
        Arrays.fill(notNormalizedFG, 0);
        Arrays.fill(notNormalizedBG, 0);

        Arrays.fill(normalizedFG, 0);
        Arrays.fill(normalizedBG, 0);

        isInitialized = false;
        totalB = 0; totalF = 0;
    }

    /**
     * Build not normalized histogram.
     * @param colorImage row ordered color image, data type ARGB_8888.
     * @param maskImage row ordered mask identifying foreground and background,data type ARGGB_8888.
     */
    private void buildHistogram(int[] colorImage, byte[] maskImage){
        int kernelSize = 5;

        totalB = 0; totalF = 0;
        Arrays.fill(notNormalizedFG, 0);
        Arrays.fill(notNormalizedBG, 0);

        int redIdx, greenIdx, blueIdx;
        int color, mask;
        int pixelIdx, colorIdx;

        for (pixelIdx = 0; pixelIdx < colorImage.length; pixelIdx++) {
            // get color and mask infomation
            color = colorImage[pixelIdx]; // ARGB
            mask = maskImage[pixelIdx];

            // idx in histogram
            redIdx = ((color & 0x00FF0000) >> 16) >> binShift;
            greenIdx = ((color & 0x0000FF00) >> 8) >> binShift;
            blueIdx = ((color & 0x000000FF)) >> binShift;

            colorIdx = (redIdx * numBins + greenIdx) * numBins + blueIdx;
            if ( mask!= 0){
                totalF += 1;
                notNormalizedFG[colorIdx] += 1;
            }
            else {
                totalB += 1;
                notNormalizedBG[colorIdx] += 1;
            }


        }
    }

    private void buildHistogram(List<int[]> colorImages, List<byte[]> maskImages){

        totalB = 0; totalF = 0;
        Arrays.fill(notNormalizedFG, 0);
        Arrays.fill(notNormalizedBG, 0);

        int redIdx, greenIdx, blueIdx;
        int color, mask;
        int pixelIdx, colorIdx;
        for (int i = 0; i < colorImages.size(); i++){
            int[] colorImage = colorImages.get(i);
            byte[] maskImage = maskImages.get(i);
            for (pixelIdx = 0; pixelIdx < colorImage.length; pixelIdx++) {
                // get color and mask infomation
                color = colorImage[pixelIdx]; // ARGB
                mask = maskImage[pixelIdx];

                // idx in histogram
                redIdx = ((color & 0x00FF0000) >> 16) >> binShift;
                greenIdx = ((color & 0x0000FF00) >> 8) >> binShift;
                blueIdx = ((color & 0x000000FF)) >> binShift;

                colorIdx = (redIdx * numBins + greenIdx) * numBins + blueIdx;
                if (mask != 0) {
                    totalF += 1;
                    notNormalizedFG[colorIdx] += 1;
                } else {
                    totalB += 1;
                    notNormalizedBG[colorIdx] += 1;
                }
            }
        }
    }
    private void normalizeHistogram(){
        float sumF = totalF != 0 ? (float) (1.0 / (float) totalF) : 0;
        float sumB = totalB != 0 ? (float) (1.0 / (float) totalB) : 0;
        for (int colorIdx = 0; colorIdx < numBins * numBins * numBins; colorIdx++) {
            if (isInitialized) {
                normalizedFG[colorIdx] = normalizedFG[colorIdx] * alphaF
                        + (float)notNormalizedFG[colorIdx] * sumF * (1 - alphaF);

                normalizedBG[colorIdx] = normalizedBG[colorIdx] * alphaB
                        + (float)notNormalizedBG[colorIdx] * sumB * (1 - alphaB);
            }
            else {
                normalizedFG[colorIdx] = (float)notNormalizedFG[colorIdx] * sumF;
                normalizedBG[colorIdx] = (float)notNormalizedBG[colorIdx] * sumB;
            }
        }
    }

    public float[] getForegroundHistogram() { return normalizedFG; }
    public float[] getBackgroundHistogram() { return normalizedBG; }
    public int getNumBins() { return numBins; }
    public int getSizeOfBin() { return sizeOfBin; }
    public int getBinShift() {return binShift; }
}
