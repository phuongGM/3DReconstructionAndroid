package com.google.ar.core.examples.java.Reconstruction;

import static java.lang.Math.log;

import android.util.Log;

import com.google.ar.core.examples.java.Utils.MyMath;

import java.util.Arrays;
import java.util.List;

public class PieHistogram {
    String TAG = "HISTOGRAM";
    private int numBins, sizeOfBin, binShift;
    private int[] totalFs;
    private int[] totalBs;
    private int noHists;

    private float alphaF, alphaB;

    private final int[][] notNormalizedFGs;
    private final int[][] notNormalizedBGs;

    private final float[][] normalizedFGs;
    private final float[][] normalizedBGs;

    private final boolean[] isInitialized;
    private float sizeOfPie;

    public PieHistogram(int noHists, int noBins){
        this.numBins = noBins;
        this.noHists = noHists;
        this.sizeOfBin = 256 / numBins;
        this.binShift = (int) (8 - log(numBins) / log(2));
        this.sizeOfPie = 360 / noHists;
        this.noHists = noHists;

        this.alphaF = 0.1f;
        this.alphaB = 0.2f;

//        this.alphaF = 0.3f;
//        this.alphaB = 0.4f;


        this.notNormalizedFGs = new int[noHists][numBins * numBins * numBins];
        this.notNormalizedBGs = new int[noHists][numBins * numBins * numBins];

        this.normalizedFGs = new float[noHists][numBins * numBins * numBins];
        this.normalizedBGs = new float[noHists][numBins * numBins * numBins];

        this.totalFs = new int[noHists];
        this.totalBs = new int[noHists];


        this.isInitialized = new boolean[noHists];

        reset();
    }

    public void update(List<int[]> colorImages, List<byte[]> maskImages, List<float[]> quaternions){
        for (int i = 0; i < colorImages.size(); i++){
            int[] colorImage = colorImages.get(i);
            byte[] maskImage = maskImages.get(i);
            float[] quaternion = quaternions.get(i);
            long start = System.currentTimeMillis();
            update(colorImage, maskImage, quaternion);
            long end = System.currentTimeMillis();
            Log.e("Image", String.valueOf(end - start));
        }
    }


    public void update(int[] colorImage, byte[] maskImage, float[] quaternion) {

        float[] eulerAngle = MyMath.quaternion2Euler(quaternion);
        float yaw = eulerAngle[1];
        int histId = (int) (yaw / this.sizeOfPie);

        buildHistogram(colorImage, maskImage, histId);

        normalizeHistogram(histId);

    }

    private void normalizeHistogram(int histId){
        float sumF = totalFs[histId] != 0 ? (float) (1.0 / (float) totalFs[histId]) : 0;
        float sumB = totalBs[histId] != 0 ? (float) (1.0 / (float) totalBs[histId]) : 0;
        for (int colorIdx = 0; colorIdx < numBins * numBins * numBins; colorIdx++) {

            if (this.isInitialized[histId] == true) {
                normalizedFGs[histId][colorIdx] = normalizedFGs[histId][colorIdx] * alphaF
                        + (float) notNormalizedFGs[histId][colorIdx] * sumF * (1 - alphaF);

                normalizedBGs[histId][colorIdx] = normalizedBGs[histId][colorIdx] * alphaB
                        + (float) notNormalizedBGs[histId][colorIdx] * sumB * (1 - alphaB);
            }
            else{
                normalizedFGs[histId][colorIdx] = (float) notNormalizedFGs[histId][colorIdx] * sumF;

                normalizedBGs[histId][colorIdx] = (float) notNormalizedBGs[histId][colorIdx] * sumB;
            }
        }
        if (this.isInitialized[histId] == false) {
            this.isInitialized[histId] = true;
        }
    }

    public void reset() {
        for(int[] row : notNormalizedFGs)
            Arrays.fill(row, 0);
        for(int[] row : notNormalizedBGs)
            Arrays.fill(row, 0);

        for(float[] row : normalizedFGs)
            Arrays.fill(row, 0);
        for(float[] row : normalizedBGs)
            Arrays.fill(row, 0);

        Arrays.fill(isInitialized, false);

        Arrays.fill(totalFs, 0);
        Arrays.fill(totalBs, 0);
    }

    private void buildHistogram(int[] colorImage, byte[] maskImage, int histId){
        totalFs[histId] = 0;
        totalBs[histId] = 0;

        Arrays.fill(notNormalizedFGs[histId], 0);
        Arrays.fill(notNormalizedBGs[histId], 0);

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
                totalFs[histId] += 1;
                notNormalizedFGs[histId][colorIdx] += 1;
            }
            else {
                totalBs[histId] += 1;
                notNormalizedBGs[histId][colorIdx] += 1;
            }
        }
    }

    public void InitPieHistograms(List<int[]> colorImages, List<byte[]> maskImages, List<float[]> quaternions){
        int redIdx, greenIdx, blueIdx;
        int color, mask;
        int pixelIdx, colorIdx;

        for (int i = 0; i < colorImages.size(); i++){
            int[] colorImage = colorImages.get(i);
            byte[] maskImage = maskImages.get(i);

            float[] quaternion = quaternions.get(i);
            float[] eulerAngle = MyMath.quaternion2Euler(quaternion);
            float yaw = eulerAngle[1];
            int histId = (int) (yaw / this.sizeOfPie);

            isInitialized[histId] = true;

            // Counting
            for (pixelIdx = 0; pixelIdx < colorImage.length; pixelIdx++) {
                // get color and mask infomation
                color = colorImage[pixelIdx]; // ARGB
                mask = maskImage[pixelIdx];

                // idx in histogram
                redIdx = ((color &   0x00FF0000) >> 16) >> binShift;
                greenIdx = ((color & 0x0000FF00) >> 8) >> binShift;
                blueIdx = ((color &  0x000000FF)) >> binShift;

                colorIdx = (redIdx * numBins + greenIdx) * numBins + blueIdx;
                if (mask != 0) {
                    totalFs[histId] += 1;
                    notNormalizedFGs[histId][colorIdx] += 1;
                } else {
                    totalBs[histId] += 1;
                    notNormalizedBGs[histId][colorIdx] += 1;
                }
            }
        }

        for(int i = 0; i < noHists; i++) {
            // Normalizing
            float sumF = totalFs[i] != 0 ? (float) (1.0 / (float) totalFs[i]) : 0;
            float sumB = totalBs[i] != 0 ? (float) (1.0 / (float) totalBs[i]) : 0;
            for (colorIdx = 0; colorIdx < numBins * numBins * numBins; colorIdx++) {
                normalizedFGs[i][colorIdx] = (float) notNormalizedFGs[i][colorIdx] * sumF;
                normalizedBGs[i][colorIdx] = (float) notNormalizedBGs[i][colorIdx] * sumB;
            }
        }
    }


    public float[] getForegroundHistogram(float[] quaternion) {

        float[] eulerAngle = MyMath.quaternion2Euler(quaternion);
        float yaw = eulerAngle[1];
        int histId = (int) (yaw / this.sizeOfPie);

        return normalizedFGs[histId];
    }
    public float[] getBackgroundHistogram(float[] quaternion) {
        float[] eulerAngle = MyMath.quaternion2Euler(quaternion);
        float yaw = eulerAngle[1];
        int histId = (int) (yaw / this.sizeOfPie);

        return normalizedBGs[histId];
    }
    public int getNumBins() { return numBins; }
    public int getSizeOfBin() { return sizeOfBin; }
    public int getBinShift() {return binShift; }
}
