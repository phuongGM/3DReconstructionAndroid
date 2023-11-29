package com.google.ar.core.examples.java.Reconstruction;


import com.google.ar.core.examples.java.Reconstruction.GaussianDistribution;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LikelihoodDistribution {
    private GaussianDistribution mFGDist;
    private GaussianDistribution mBGDist;
    final int numThreads = 8;
    float avgFGPixels, avgBGPixels;
    float alpha;

    public LikelihoodDistribution() {
        mFGDist = new GaussianDistribution();
        mBGDist = new GaussianDistribution();
        avgFGPixels = 0;
        avgBGPixels = 0;
        alpha = 0.2f;
    }

    public void update(final int[] colorImage, final byte[] maskImage) {
        // ------------------------FG-------------------------
        final float[][] listFGMeans = new float[numThreads][3];
        final float[][] listFGCovMats = new float[numThreads][9];
        final float[] listFGPixels = new float[numThreads];
        final float[] FGMean = new float[3];
        final float[] FGCovMat = new float[9];
        float FGPixels = 0;

//        Arrays.fill(listFGMeans, 0);
//        Arrays.fill(listFGCovMats, 0);
        for (float[] row: listFGMeans)
            Arrays.fill(row, 0);
        for (float[] row: listFGCovMats)
            Arrays.fill(row, 0);

        Arrays.fill(listFGPixels, 0);
        Arrays.fill(FGMean, 0);
        Arrays.fill(FGCovMat, 0);

        // ------------------------BG-------------------------
        final float[][] listBGMeans = new float[numThreads][3];
        final float[][] listBGCovMats = new float[numThreads][9];
        final float[] listBGPixels = new float[numThreads];
        final float[] BGMean = new float[3];
        final float[] BGCovMat = new float[9];
        float BGPixels = 0;

        for (float[] row: listBGMeans)
            Arrays.fill(row, 0);
        for (float[] row: listBGCovMats)
            Arrays.fill(row, 0);

        Arrays.fill(listBGPixels, 0);
        Arrays.fill(BGMean, 0);
        Arrays.fill(BGCovMat, 0);

        final int range = colorImage.length / numThreads;

        // ------------------ UPDATE MEANS ---------------------
        ExecutorService es = Executors.newCachedThreadPool();
        for (int threadId = 0; threadId < numThreads; threadId++) {
            final int finalThreadId = threadId;
            es.execute(new Runnable() {
                @Override
                public void run() {
                    final int vStart = finalThreadId * range;
                    int vEnd = range + vStart;
                    if (finalThreadId + 1 == numThreads) {
                        vEnd = colorImage.length;
                    }
                    for (int pixelIdx = vStart; pixelIdx < vEnd; pixelIdx++) {
                        int color = colorImage[pixelIdx]; // ARGB
                        byte mask = maskImage[pixelIdx];

                        if (mask != 0) {
                            listFGMeans[finalThreadId][0] += (color & 0x00FF0000) >> 16; // red
                            listFGMeans[finalThreadId][1] += (color & 0x0000FF00) >> 8; // green
                            listFGMeans[finalThreadId][2] += (color & 0x000000FF); // blue

                            listFGPixels[finalThreadId] += 1;
                        } else {
                            listBGMeans[finalThreadId][0] += (color & 0x00FF0000) >> 16; // red
                            listBGMeans[finalThreadId][1] += (color & 0x0000FF00) >> 8; // green
                            listBGMeans[finalThreadId][2] += (color & 0x000000FF); // blue

                            listBGPixels[finalThreadId] += 1;
                        }
                    }
                }
            });
        }
        es.shutdown();
        try {
            boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int threadId = 0; threadId < numThreads; threadId++) {
            // ------------FG-------------
            FGMean[0] += listFGMeans[threadId][0];
            FGMean[1] += listFGMeans[threadId][1];
            FGMean[2] += listFGMeans[threadId][2];

            FGPixels += listFGPixels[threadId];
            // ------------BG-------------
            BGMean[0] += listBGMeans[threadId][0];
            BGMean[1] += listBGMeans[threadId][1];
            BGMean[2] += listBGMeans[threadId][2];

            BGPixels += listBGPixels[threadId];
        }

        for (int i = 0; i < FGMean.length; i++){
            // ------------FG-------------
            FGMean[i] /= FGPixels;

            // ------------BG-------------
            BGMean[i] /= BGPixels;
        }


        // ---------- UPDATE COVARIANCE MATRICES ------------------------
        es = Executors.newCachedThreadPool();
        for (int threadId = 0; threadId < numThreads; threadId++) {
            final int finalThreadId = threadId;
            es.execute(new Runnable() {
                final int vStart = finalThreadId * range;
                @Override
                public void run() {
                    int vEnd = range + vStart;
                    if (finalThreadId + 1 == numThreads) {
                        vEnd = colorImage.length;
                    }
                    float[] tmp = new float[3];

                    for (int pixelIdx = vStart; pixelIdx < vEnd; pixelIdx++) {
                        int color = colorImage[pixelIdx]; // ARGB
                        byte mask = maskImage[pixelIdx];

                        if (mask != 0) {
                            tmp[0] = FGMean[0] - ((color & 0x00FF0000) >> 16); // red
                            tmp[1] = FGMean[1] - ((color & 0x0000FF00) >> 8); // green
                            tmp[2] = FGMean[2] - (color & 0x000000FF); // blue

                            for (int n = 0; n < 3; n++)
                            {
                                for (int m = 0; m < 3; m++)
                                {
                                    listFGCovMats[finalThreadId][n + m * 3] += tmp[n] *  tmp[m];
                                }
                            }

                        } else {
                            tmp[0] = BGMean[0] - ((color & 0x00FF0000) >> 16); // red
                            tmp[1] = BGMean[1] - ((color & 0x0000FF00) >> 8); // green
                            tmp[2] = BGMean[2] - (color & 0x000000FF); // blue

                            for (int n = 0; n < 3; n++)
                            {
                                for (int m = 0; m < 3; m++)
                                {
                                    listBGCovMats[finalThreadId][n + m * 3] += tmp[n] *  tmp[m];
                                }
                            }
                        }
                    }
                }
            });
        }
        es.shutdown();
        try {
            boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int threadId = 0; threadId < numThreads; threadId++) {
            // ------------FG-------------
            for(int i = 0; i < FGCovMat.length; i++) {
                FGCovMat[i] += listFGCovMats[threadId][i];
            }
            // ------------BG-------------
            for(int i = 0; i < BGCovMat.length; i++) {
                BGCovMat[i] += listBGCovMats[threadId][i];
            }
        }

        for (int i = 0; i < FGCovMat.length; i++){
            // ------------FG-------------
            FGCovMat[i] /= FGPixels;

            // ------------BG-------------
            BGCovMat[i] /= BGPixels;
        }

        mFGDist.update(FGMean, FGCovMat, alpha);
        mBGDist.update(BGMean, BGCovMat, alpha);
    }


    public void update(List<int[]> colorImages, List<byte[]> maskImages) {
        // ------------------------FG-------------------------
        final float[][] listFGMeans = new float[numThreads][3];
        final float[][] listFGCovMats = new float[numThreads][9];
        final float[] listFGPixels = new float[numThreads];
        final float[] FGMean = new float[3];
        final float[] FGCovMat = new float[9];
        float FGPixels = 0;

//        Arrays.fill(listFGMeans, 0);
//        Arrays.fill(listFGCovMats, 0);
        for (float[] row: listFGMeans)
            Arrays.fill(row, 0);
        for (float[] row: listFGCovMats)
            Arrays.fill(row, 0);

        Arrays.fill(listFGPixels, 0);
        Arrays.fill(FGMean, 0);
        Arrays.fill(FGCovMat, 0);

        // ------------------------BG-------------------------
        final float[][] listBGMeans = new float[numThreads][3];
        final float[][] listBGCovMats = new float[numThreads][9];
        final float[] listBGPixels = new float[numThreads];
        final float[] BGMean = new float[3];
        final float[] BGCovMat = new float[9];
        float BGPixels = 0;

        for (float[] row: listBGMeans)
            Arrays.fill(row, 0);
        for (float[] row: listBGCovMats)
            Arrays.fill(row, 0);

        Arrays.fill(listBGPixels, 0);
        Arrays.fill(BGMean, 0);
        Arrays.fill(BGCovMat, 0);

        final int range = colorImages.get(0).length / numThreads;
        ExecutorService es;

        for (int i = 0; i < colorImages.size(); i++) {

            final int[] colorImage = colorImages.get(i);
            final byte[] maskImage = maskImages.get(i);

            es = Executors.newCachedThreadPool();
            // ------------------ UPDATE MEANS ---------------------
            for (int threadId = 0; threadId < numThreads; threadId++) {
                final int finalThreadId = threadId;
                es.execute(new Runnable() {
                    @Override
                    public void run() {
                        final int vStart = finalThreadId * range;
                        int vEnd = range + vStart;
                        if (finalThreadId + 1 == numThreads) {
                            vEnd = colorImage.length;
                        }
                        for (int pixelIdx = vStart; pixelIdx < vEnd; pixelIdx++) {
                            int color = colorImage[pixelIdx]; // ARGB
                            byte mask = maskImage[pixelIdx];

                            if (mask != 0) {
                                listFGMeans[finalThreadId][0] += (color & 0x00FF0000) >> 16; // red
                                listFGMeans[finalThreadId][1] += (color & 0x0000FF00) >> 8; // green
                                listFGMeans[finalThreadId][2] += (color & 0x000000FF); // blue


//                                int red = (color & 0x00FF0000) >> 16; // red
//                                int green = (color & 0x0000FF00) >> 8; // green
//                                int blue = (color & 0x000000FF); // blue

//                                Log.v("COLOR_RED", String.valueOf(red));
//                                Log.v("COLOR_GREEN", String.valueOf(green));
//                                Log.v("COLOR_BLUE", String.valueOf(blue));
//                                Log.d("COLOR_", "=======================");

                                listFGPixels[finalThreadId] += 1;
                            } else {
                                listBGMeans[finalThreadId][0] += (color & 0x00FF0000) >> 16; // red
                                listBGMeans[finalThreadId][1] += (color & 0x0000FF00) >> 8; // green
                                listBGMeans[finalThreadId][2] += (color & 0x000000FF); // blue

                                listBGPixels[finalThreadId] += 1;
                            }
                        }
                    }
                });
            }
            es.shutdown();
            try {
                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int threadId = 0; threadId < numThreads; threadId++) {
            // ------------FG-------------
            FGMean[0] += listFGMeans[threadId][0];
            FGMean[1] += listFGMeans[threadId][1];
            FGMean[2] += listFGMeans[threadId][2];

            FGPixels += listFGPixels[threadId];
            // ------------BG-------------
            BGMean[0] += listBGMeans[threadId][0];
            BGMean[1] += listBGMeans[threadId][1];
            BGMean[2] += listBGMeans[threadId][2];

            BGPixels += listBGPixels[threadId];
        }

        for (int i = 0; i < FGMean.length; i++){
            // ------------FG-------------
            FGMean[i] /= FGPixels;

            // ------------BG-------------
            BGMean[i] /= BGPixels;
        }


        // ---------- UPDATE COVARIANCE MATRICES ------------------------

        for (int i = 0; i < colorImages.size(); i++) {

            final int[] colorImage = colorImages.get(i);
            final byte[] maskImage = maskImages.get(i);
            es = Executors.newCachedThreadPool();
            for (int threadId = 0; threadId < numThreads; threadId++) {
                final int finalThreadId = threadId;
                es.execute(new Runnable() {
                    final int vStart = finalThreadId * range;

                    @Override
                    public void run() {
                        int vEnd = range + vStart;
                        if (finalThreadId + 1 == numThreads) {
                            vEnd = colorImage.length;
                        }
                        float[] tmp = new float[3];

                        for (int pixelIdx = vStart; pixelIdx < vEnd; pixelIdx++) {
                            int color = colorImage[pixelIdx]; // ARGB
                            byte mask = maskImage[pixelIdx];

                            if (mask != 0) {
                                tmp[0] = FGMean[0] - ((color & 0x00FF0000) >> 16); // red
                                tmp[1] = FGMean[1] - ((color & 0x0000FF00) >> 8); // green
                                tmp[2] = FGMean[2] - (color & 0x000000FF); // blue

                                for (int n = 0; n < 3; n++) {
                                    for (int m = 0; m < 3; m++) {
                                        listFGCovMats[finalThreadId][n + m * 3] += tmp[n] * tmp[m];
                                    }
                                }

                            } else {
                                tmp[0] = BGMean[0] - ((color & 0x00FF0000) >> 16); // red
                                tmp[1] = BGMean[1] - ((color & 0x0000FF00) >> 8); // green
                                tmp[2] = BGMean[2] - (color & 0x000000FF); // blue

                                for (int n = 0; n < 3; n++) {
                                    for (int m = 0; m < 3; m++) {
                                        listBGCovMats[finalThreadId][n + m * 3] += tmp[n] * tmp[m];
                                    }
                                }
                            }
                        }
                    }
                });
            }
            es.shutdown();
            try {
                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int threadId = 0; threadId < numThreads; threadId++) {
            // ------------FG-------------
            for(int i = 0; i < FGCovMat.length; i++) {
                FGCovMat[i] += listFGCovMats[threadId][i];
            }
            // ------------BG-------------
            for(int i = 0; i < BGCovMat.length; i++) {
                BGCovMat[i] += listBGCovMats[threadId][i];
            }
        }

        for (int i = 0; i < FGCovMat.length; i++){
            // ------------FG-------------
            FGCovMat[i] /= FGPixels;

            // ------------BG-------------
            BGCovMat[i] /= BGPixels;
        }

        mFGDist.update(FGMean, FGCovMat, alpha);
        mBGDist.update(BGMean, BGCovMat, alpha);
    }

    public GaussianDistribution getFGDist() {
        return mFGDist;
    }

    public GaussianDistribution getBGDist() {
        return mBGDist;
    }
}
