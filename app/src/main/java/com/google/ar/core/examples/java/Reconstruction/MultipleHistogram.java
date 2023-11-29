package com.google.ar.core.examples.java.Reconstruction;

import static java.lang.Math.log;

import android.util.Log;

import com.google.ar.core.examples.java.Utils.MyMath;

import java.util.Arrays;
import java.util.List;

public class MultipleHistogram {
    String TAG = "HISTOGRAM";
    private int numBins, sizeOfBin, binShift;
    private int totalF, totalB;
    private int noHists;

    private float alphaF, alphaB;

    private final int[] notNormalizedFG;
    private final int[] notNormalizedBG;

    private final float[][] normalizedFG;
    private final float[][] normalizedBG;

    private final float[][] listQuaternions;

    public MultipleHistogram(int noHists, int noBins){
        this.numBins = noBins;
        this.noHists = noHists;
        this.sizeOfBin = 256 / numBins;
        this.binShift = (int) (8 - log(numBins) / log(2));

        this.alphaF = 0.1f;
        this.alphaB = 0.2f;

//        this.alphaF = 0.3f;
//        this.alphaB = 0.4f;


        this.notNormalizedFG = new int[numBins * numBins * numBins];
        this.notNormalizedBG = new int[numBins * numBins * numBins];

        this.normalizedFG = new float[noHists][numBins * numBins * numBins];
        this.normalizedBG = new float[noHists][numBins * numBins * numBins];
        this.listQuaternions = new float[noHists][4];

        reset();
    }

    public void update(List<int[]> colorImages, List<byte[]> maskImages, List<float[]> quaternions){
        for (int i = 0; i < colorImages.size(); i++){
            int[] colorImage = colorImages.get(i);
            byte[] maskImage = maskImages.get(i);
            float[] quaternion = quaternions.get(i);
            update(colorImage, maskImage, quaternion);
        }
    }


    public void update(int[] colorImage, byte[] maskImage, float[] quaternion) {
        int histId = 0;
        float similarity;
        float maxSimilar = -1;
        for (int i = 0; i < noHists; i++){
            similarity = MyMath.cosineSimilarity(listQuaternions[i], quaternion);
            if (similarity > maxSimilar) histId = i;
        }

        buildHistogram(colorImage, maskImage);

        normalizeHistogram(histId);

        float[] updatedQuaternion = MyMath.slerp(listQuaternions[histId], quaternion, 0.1f);
        for (int i = 0; i < 4; i++)
            listQuaternions[histId][i] = updatedQuaternion[i];

    }

    private void normalizeHistogram(int histId){
        float sumF = totalF != 0 ? (float) (1.0 / (float) totalF) : 0;
        float sumB = totalB != 0 ? (float) (1.0 / (float) totalB) : 0;
        for (int colorIdx = 0; colorIdx < numBins * numBins * numBins; colorIdx++) {
            normalizedFG[histId][colorIdx] = normalizedFG[histId][colorIdx] * alphaF
                    + (float)notNormalizedFG[colorIdx] * sumF * (1 - alphaF);

            normalizedBG[histId][colorIdx] = normalizedBG[histId][colorIdx] * alphaB
                    + (float)notNormalizedBG[colorIdx] * sumB * (1 - alphaB);

        }
    }

    public void reset() {
        Arrays.fill(notNormalizedFG, 0);
        Arrays.fill(notNormalizedBG, 0);
        for(float[] row : normalizedFG)
            Arrays.fill(row, 0);
        for(float[] row : normalizedBG)
            Arrays.fill(row, 0);
        for(float[] row : listQuaternions)
            Arrays.fill(row, 0);

        totalB = 0; totalF = 0;
    }

    private void buildHistogram(int[] colorImage, byte[] maskImage){

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

    public void InitMultipleHistograms(List<int[]> colorImages, List<byte[]> maskImages, List<float[]> quaternions){
        int redIdx, greenIdx, blueIdx;
        int color, mask;
        int pixelIdx, colorIdx;

        for (int i = 0; i < noHists; i++){
            int[] colorImage = colorImages.get(i);
            byte[] maskImage = maskImages.get(i);
            float[] quaternion = quaternions.get(i);
            for (int k = 0; k < 4; k++) listQuaternions[i][k] = quaternion[k];

            totalB = 0; totalF = 0;
            Arrays.fill(notNormalizedFG, 0);
            Arrays.fill(notNormalizedBG, 0);

            // Counting
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

            // Normalizing
            float sumF = totalF != 0 ? (float) (1.0 / (float) totalF) : 0;
            float sumB = totalB != 0 ? (float) (1.0 / (float) totalB) : 0;
            for (colorIdx = 0; colorIdx < numBins * numBins * numBins; colorIdx++) {
                normalizedFG[i][colorIdx] = (float)notNormalizedFG[colorIdx] * sumF;
                normalizedBG[i][colorIdx] = (float)notNormalizedBG[colorIdx] * sumB;
            }
        }
    }


    public float[] getForegroundHistogram(float[] quaternion) {
        int histId = 0;
        float similarity;
        float maxSimilar = -1;
        for (int i = 0; i < noHists; i++){
            similarity = MyMath.cosineSimilarity(listQuaternions[i], quaternion);
            if (similarity > maxSimilar) histId = i;
        }
        return normalizedFG[histId];
    }
    public float[] getBackgroundHistogram(float[] quaternion) {
        int histId = 0;
        float similarity;
        float maxSimilar = -1;
        for (int i = 0; i < noHists; i++){
            similarity = MyMath.cosineSimilarity(listQuaternions[i], quaternion);
            if (similarity > maxSimilar) histId = i;
        }
        return normalizedBG[histId];
    }
    public int getNumBins() { return numBins; }
    public int getSizeOfBin() { return sizeOfBin; }
    public int getBinShift() {return binShift; }
}
