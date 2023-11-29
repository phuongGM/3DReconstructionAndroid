package com.google.ar.core.examples.java.Reconstruction;

import static com.google.ar.core.examples.java.Reconstruction.Settings.NO_THREADS;

import android.graphics.Bitmap;
import android.graphics.Color;

import android.opengl.Matrix;
import android.util.Log;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.log;
import static java.lang.Math.min;

import com.google.ar.core.examples.java.Utils.Algorithm;
import com.google.ar.core.examples.java.Utils.MyMath;


public class ShapeOptimization {
    public static void updateProbabilityBaseOnHistogram(
            Histogram histogram,
            final int[] colorImage,
            final byte[] maskImage,
            final float[] MVP,
            int width, int height)
    {

        final float listFGVoxels[] = new float[NO_THREADS];
        final float listBGVoxels[] = new float[NO_THREADS];

        float[] lbf = Volume.getInstance().getLBF();
        final float[] rtn = Volume.getInstance().getRTN();

        final int Nx = Volume.getInstance().getNx();
        final int Ny = Volume.getInstance().getNy();
        final int Nz = Volume.getInstance().getNz();

        final float stepX = (rtn[0] - lbf[0]) / (float)Volume.getInstance().getNx();
        final float stepY = (rtn[1] - lbf[1]) / (float)Volume.getInstance().getNy();
        final float stepZ = (rtn[2] - lbf[2]) / (float)Volume.getInstance().getNz();

        final float[] probData = Volume.getInstance().getProb();

        final float[] FGHistogram = histogram.getForegroundHistogram();
        final float[] BGHistogram = histogram.getBackgroundHistogram();
        final int numBins = histogram.getNumBins();
        final int binShift = histogram.getBinShift();

//        final float w = 250;
        final float w = 250;

        final int range = Nx * Ny * Nz / NO_THREADS;
        ExecutorService es = Executors.newCachedThreadPool();
        for (int threadId = 0; threadId < NO_THREADS; threadId++){
            final int finalThreadId = threadId;
            es.execute(new Runnable() {
                int vStart = finalThreadId * range;
                @Override
                public void run() {
                    int vEnd = vStart + range;
                    if (finalThreadId + 1 == NO_THREADS){
                        vEnd = Nz * Nx * Ny;
                    }

                    for (int voxelIdx = vStart; voxelIdx < vEnd; voxelIdx++) {
                        //                        int voxelIdx = ((iz * Nx + ix) * Ny) + iy;
                        int iz = voxelIdx / (Nx * Ny);
                        int ix = (voxelIdx - iz * (Nx * Ny)) / Ny;
                        int iy = voxelIdx - iz * (Nx * Ny) - ix * Ny;

                        float[] point = new float[]{
                                lbf[0] + ix * stepX + stepX / 2,
                                lbf[1] + iy * stepY + stepY / 2,
                                lbf[2] + iz * stepZ + stepZ / 2,
                                1.0f
                        };
                        float[] p = new float[4];

                        Matrix.multiplyMV(p, 0, MVP, 0, point, 0);
                        if (p[3] == 0)
                            continue;

                        float[] p3f = new float[]{p[0] / p[3], p[1] / p[3], p[2] / p[3]};
                        float iPcRf = 0;
                        float iPcRb = 0;
                        if (p3f[0] < -1 ||
                                p3f[0] > 1 ||
                                p3f[1] < -1 ||
                                p3f[1] > 1 ||
                                p3f[2] < -1 ||
                                p3f[2] > 1) {

                        }
                        else {
                            int p3fX = (int) ((p3f[0] + 1) / 2 * width);

                            if (p3fX == width) p3fX -= 1;

                            int p3fY = (int) ((p3f[1] + 1) / 2 * height);
                            p3fY = height - p3fY;
                            if (p3fY == height) p3fY -= 1;

                            int pixelIdx = p3fY * width + p3fX;

                            int color = colorImage[pixelIdx]; // ARGB
                            // idx in histogram
                            int redIdx = ((color & 0x00FF0000) >> 16) >> binShift;
                            int greenIdx = ((color & 0x0000FF00) >> 8) >> binShift;
                            int blueIdx = ((color & 0x000000FF)) >> binShift;

                            int colorIdx = (redIdx * numBins + greenIdx) * numBins + blueIdx;

                            iPcRf = FGHistogram[colorIdx];
                            iPcRb = BGHistogram[colorIdx];
                            iPcRf += 1e-10;
                            iPcRb += 1e-10;

                            if (Volume.getInstance().isFirstInit()) {
                                probData[2 * voxelIdx] = (float) log(iPcRf);
                                probData[2 * voxelIdx + 1] = (float) log(1 - iPcRb);
                            }
                            else {
                                probData[2 * voxelIdx] = (float) ((w * probData[2 * voxelIdx] + 1 * log(iPcRf)) / (w + 1));
                                probData[2 * voxelIdx + 1] = (float) ((w * probData[2 * voxelIdx + 1] + 1 * log(1 - iPcRb)) / (w + 1));
                            }
                        }

                        if (iPcRf > iPcRb) listFGVoxels[finalThreadId] += 1;
                        else listBGVoxels[finalThreadId] += 1;

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

        float avgBGVoxels = 0, avgFGVoxels = 0;

        for (int threadId = 0; threadId < NO_THREADS; threadId++){
            avgFGVoxels += listFGVoxels[threadId];
            avgBGVoxels += listBGVoxels[threadId];
        }
        avgFGVoxels /= (Nx * Ny * Nz);
        avgBGVoxels /= (Nx * Ny * Nz);


        float avgFGPixels = 0, avgBGPixels = 0;
        for (int i = 0; i < maskImage.length; i++){
            if (maskImage[i] != 0) avgFGPixels += 1;
        }
        avgFGPixels /= maskImage.length;
        avgBGPixels = 1 - avgFGPixels;
        Log.v("avgFGPixels", String.valueOf(avgFGPixels));

        if (Volume.getInstance().isFirstInit()) {
            Volume.getInstance().setAVG_FG_Pixels(avgFGPixels);
            Volume.getInstance().setAVG_BG_Pixels(avgBGPixels);

            Volume.getInstance().setAVG_FG_Voxels(avgFGVoxels);
            Volume.getInstance().setAVG_BG_Voxels(avgBGVoxels);
        }
        else{
            avgFGPixels = (Volume.getInstance().getAVG_FG_Pixels() * w +  avgFGPixels) / (w + 1);
            avgBGPixels = (Volume.getInstance().getAVG_BG_Pixels() * w +  avgBGPixels) / (w + 1);

            Volume.getInstance().setAVG_FG_Pixels(avgFGPixels);
            Volume.getInstance().setAVG_BG_Pixels(avgBGPixels);

            avgFGVoxels = (Volume.getInstance().getAVG_FG_Voxels() * w +  avgFGVoxels) / (w + 1);
            avgBGVoxels = (Volume.getInstance().getAVG_BG_Voxels() * w +  avgBGVoxels) / (w + 1);

            Volume.getInstance().setAVG_FG_Voxels(avgFGVoxels);
            Volume.getInstance().setAVG_BG_Voxels(avgBGVoxels);
        }
        Log.v("avg_FGVoxels", String.valueOf(avgFGVoxels));
        Log.v("avg_BGVoxels", String.valueOf(avgBGVoxels));

        if (Volume.getInstance().isFirstInit()) Volume.getInstance().setFirstInit(false);

    }

    public static void updateProbabilityBaseOnGaussian(
            Histogram histogram,
            final int[] colorImage,
            final byte[] maskImage,
            final float[] MVP,
            int width, int height)
    {


        final float listFGVoxels[] = new float[NO_THREADS];
        final float listBGVoxels[] = new float[NO_THREADS];

        float[] lbf = Volume.getInstance().getLBF();
        final float[] rtn = Volume.getInstance().getRTN();

        final int Nx = Volume.getInstance().getNx();
        final int Ny = Volume.getInstance().getNy();
        final int Nz = Volume.getInstance().getNz();

        final float stepX = (rtn[0] - lbf[0]) / (float)Volume.getInstance().getNx();
        final float stepY = (rtn[1] - lbf[1]) / (float)Volume.getInstance().getNy();
        final float stepZ = (rtn[2] - lbf[2]) / (float)Volume.getInstance().getNz();

        final float[] probData = Volume.getInstance().getProb();

        final GaussianDistribution FGDist = Volume.getInstance().getLkhDist().getFGDist();
        final GaussianDistribution BGDist = Volume.getInstance().getLkhDist().getBGDist();


        final float w = 250;
        final int range = Nx * Ny * Nz / NO_THREADS;
        ExecutorService es = Executors.newCachedThreadPool();
        for (int threadId = 0; threadId < NO_THREADS; threadId++){
            final int finalThreadId = threadId;
            es.execute(new Runnable() {
                int vStart = finalThreadId * range;
                @Override
                public void run() {
                    int vEnd = vStart + range;
                    if (finalThreadId + 1 == NO_THREADS){
                        vEnd = Nz * Nx * Ny;
                    }

                    for (int voxelIdx = vStart; voxelIdx < vEnd; voxelIdx++) {
                        //                        int voxelIdx = ((iz * Nx + ix) * Ny) + iy;
                        int iz = voxelIdx / (Nx * Ny);
                        int ix = (voxelIdx - iz * (Nx * Ny)) / Ny;
                        int iy = voxelIdx - iz * (Nx * Ny) - ix * Ny;

                        float[] point = new float[]{
                                lbf[0] + ix * stepX + stepX / 2,
                                lbf[1] + iy * stepY + stepY / 2,
                                lbf[2] + iz * stepZ + stepZ / 2,
                                1.0f
                        };
                        float[] p = new float[4];

                        Matrix.multiplyMV(p, 0, MVP, 0, point, 0);
                        if (p[3] == 0)
                            continue;

                        float[] p3f = new float[]{p[0] / p[3], p[1] / p[3], p[2] / p[3]};
                        float iPcRf = 0;
                        float iPcRb = 0;
                        if (p3f[0] < -1 ||
                                p3f[0] > 1 ||
                                p3f[1] < -1 ||
                                p3f[1] > 1 ||
                                p3f[2] < -1 ||
                                p3f[2] > 1) {

                        }
                        else {
                            int p3fX = (int) ((p3f[0] + 1) / 2 * width);

                            if (p3fX == width) p3fX -= 1;

                            int p3fY = (int) ((p3f[1] + 1) / 2 * height);
                            p3fY = height - p3fY;
                            if (p3fY == height) p3fY -= 1;

                            int pixelIdx = p3fY * width + p3fX;

                            int color = colorImage[pixelIdx]; // ARGB
                            //                        array[pixelIdx] = (byte) 255;

                            // idx in histogram

                            float[] colorVec = new float[3];
                            colorVec[0] = ((color & 0x00FF0000) >> 16);
                            colorVec[1] = ((color & 0x0000FF00) >> 8);
                            colorVec[2] = ((color & 0x000000FF));

                            iPcRf = FGDist.getProbability(colorVec);
                            iPcRb = BGDist.getProbability(colorVec);
                        }

                        iPcRf += 1e-10;
                        iPcRb += 1e-10;

                        if (Volume.getInstance().isFirstInit()) {
                            probData[2 * voxelIdx] = (float) log(iPcRf);
                            probData[2 * voxelIdx + 1] = (float) log(1 - iPcRb);
                        }
                        else {
                            probData[2 * voxelIdx] = (float) ((w * probData[2 * voxelIdx] + 1 * log(iPcRf)) / (w + 1));
                            probData[2 * voxelIdx + 1] = (float) ((w * probData[2 * voxelIdx + 1] + 1 * log(1 - iPcRb)) / (w + 1));
                        }

                        if (iPcRf > iPcRb) listFGVoxels[finalThreadId] += 1;
                        else listBGVoxels[finalThreadId] += 1;

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

        float avgBGVoxels = 0, avgFGVoxels = 0;

        for (int threadId = 0; threadId < NO_THREADS; threadId++){
            avgFGVoxels += listFGVoxels[threadId];
            avgBGVoxels += listBGVoxels[threadId];
        }
        avgFGVoxels /= (Nx * Ny * Nz);
        avgBGVoxels /= (Nx * Ny * Nz);


        float avgFGPixels = 0, avgBGPixels = 0;
        for (int i = 0; i < maskImage.length; i++){
            if (maskImage[i] != 0) avgFGPixels += 1;
        }
        avgFGPixels /= maskImage.length;
        avgBGPixels = 1 - avgFGPixels;
        Log.v("avgFGPixels", String.valueOf(avgFGPixels));

        if (Volume.getInstance().isFirstInit()) {
            Volume.getInstance().setAVG_FG_Pixels(avgFGPixels);
            Volume.getInstance().setAVG_BG_Pixels(avgBGPixels);

            Volume.getInstance().setAVG_FG_Voxels(avgFGVoxels);
            Volume.getInstance().setAVG_BG_Voxels(avgBGVoxels);
        }
        else{
            avgFGPixels = (Volume.getInstance().getAVG_FG_Pixels() * w +  avgFGPixels) / (w + 1);
            avgBGPixels = (Volume.getInstance().getAVG_BG_Pixels() * w +  avgBGPixels) / (w + 1);

            Volume.getInstance().setAVG_FG_Pixels(avgFGPixels);
            Volume.getInstance().setAVG_BG_Pixels(avgBGPixels);

            avgFGVoxels = (Volume.getInstance().getAVG_FG_Voxels() * w +  avgFGVoxels) / (w + 1);
            avgBGVoxels = (Volume.getInstance().getAVG_BG_Voxels() * w +  avgBGVoxels) / (w + 1);

            Volume.getInstance().setAVG_FG_Voxels(avgFGVoxels);
            Volume.getInstance().setAVG_BG_Voxels(avgBGVoxels);
        }
        Log.v("avg_FGVoxels", String.valueOf(avgFGVoxels));
        Log.v("avg_BGVoxels", String.valueOf(avgBGVoxels));

        if (Volume.getInstance().isFirstInit()) Volume.getInstance().setFirstInit(false);

    }


    public static void updateProbabilityBaseOnHistogram(
            Histogram histogram,
            final List<int[]> colorImages,
            final List<byte[]> maskImages,
            final List<float[]> MVPs,
            int width, int height,
            float w)
    {
        final float listFGVoxels[] = new float[NO_THREADS];
        final float listBGVoxels[] = new float[NO_THREADS];

        final float[] lbf = Volume.getInstance().getLBF();
        final float[] rtn = Volume.getInstance().getRTN();

        final int Nx = Volume.getInstance().getNx();
        final int Ny = Volume.getInstance().getNy();
        final int Nz = Volume.getInstance().getNz();

        final float stepX = (rtn[0] - lbf[0]) / (float)Volume.getInstance().getNx();
        final float stepY = (rtn[1] - lbf[1]) / (float)Volume.getInstance().getNy();
        final float stepZ = (rtn[2] - lbf[2]) / (float)Volume.getInstance().getNz();


        final float[] probData = Volume.getInstance().getProb();

        final float[] FGHistogram = histogram.getForegroundHistogram();
        final float[] BGHistogram = histogram.getBackgroundHistogram();
        final int numBins = histogram.getNumBins();
        final int binShift = histogram.getBinShift();

        // ------------------- Calculate Prob in and out of Voxels ---------------------

        final int range = Nx * Ny * Nz / NO_THREADS;
        float[] logProb = new float[probData.length];

        for (int i = 0; i < colorImages.size(); i++) {
            final int[] colorImage = colorImages.get(i);
            final float[] MVP = MVPs.get(i);

            ExecutorService es = Executors.newCachedThreadPool();
            for (int threadId = 0; threadId < NO_THREADS; threadId++) {
                final int finalThreadId = threadId;
                es.execute(new Runnable() {
                    int vStart = finalThreadId * range;

                    @Override
                    public void run() {
                        int vEnd = vStart + range;
                        if (finalThreadId + 1 == NO_THREADS) {
                            vEnd = Nz * Nx * Ny;
                        }

                        for (int voxelIdx = vStart; voxelIdx < vEnd; voxelIdx++) {
//                        int voxelIdx = ((iz * Nx + ix) * Ny) + iy;
                            int iz = voxelIdx / (Nx * Ny);
                            int ix = (voxelIdx -  iz * Nx * Ny) / Ny;
                            int iy = voxelIdx - iz * (Nx * Ny) - ix * Ny;

                            float[] point = new float[]{
                                    lbf[0] + ix * stepX + stepX / 2,
                                    lbf[1] + iy * stepY + stepY / 2,
                                    lbf[2] + iz * stepZ + stepZ / 2,
                                    1.0f};
                            float[] p = new float[4];

                            Matrix.multiplyMV(p, 0, MVP, 0, point, 0);
                            if (p[3] == 0)
                                continue;

                            float[] p3f = new float[]{p[0] / p[3], p[1] / p[3], p[2] / p[3]};

                            float iPcRf = 0;
                            float iPcRb = 0;

                            if (p3f[0] < -1 ||
                                p3f[0] > 1 ||
                                p3f[1] < -1 ||
                                p3f[1] > 1 ||
                                p3f[2] < -1 ||
                                p3f[2] > 1)
                            {

                            }
                            else {
                                int p3fX = (int) ((p3f[0] + 1) / 2 * width);
                                if (p3fX == width) p3fX -= 1;
                                int p3fY = (int) ((p3f[1] + 1) / 2 * height);
                                p3fY = height - p3fY;
                                if (p3fY == height) p3fY -= 1;

                                int pixelIdx = p3fY * width + p3fX;

                                int color = colorImage[pixelIdx]; // ARGB

                                // idx in histogram
                                int redIdx = ((color & 0x00FF0000) >> 16) >> binShift;
                                int greenIdx = ((color & 0x0000FF00) >> 8) >> binShift;
                                int blueIdx = ((color & 0x000000FF)) >> binShift;

                                int colorIdx = (redIdx * numBins + greenIdx) * numBins + blueIdx;


                                iPcRf += FGHistogram[colorIdx];
                                iPcRb += BGHistogram[colorIdx];


                            }

                            iPcRf += 1e-10f;
                            iPcRb += 1e-10f;

//                            probData[2 * voxelIdx] += (float) log(iPcRf);
//                            probData[2 * voxelIdx + 1] += (float) log(1 - iPcRb);


                            logProb[2 * voxelIdx] += (float) log(iPcRf);
                            logProb[2 * voxelIdx + 1] += (float) log(1 - iPcRb);


                            if (iPcRf > iPcRb) listFGVoxels[finalThreadId] += 1;
                            else listBGVoxels[finalThreadId] += 1;
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




        for (int voxelIdx = 0; voxelIdx < Nx * Ny * Nz; voxelIdx++) {
            logProb[2 * voxelIdx] /= colorImages.size();
            logProb[2 * voxelIdx + 1] /= colorImages.size();

            if (Volume.getInstance().isFirstInit()) {
                probData[2 * voxelIdx] = logProb[2 * voxelIdx];
                probData[2 * voxelIdx + 1] = logProb[2 * voxelIdx + 1];
            }
            else {
                probData[2 * voxelIdx] =  (w * probData[2 * voxelIdx] + logProb[2 * voxelIdx]) / (w + 1);
                probData[2 * voxelIdx + 1] = (w * probData[2 * voxelIdx + 1] + logProb[2 * voxelIdx + 1]) / (w + 1);
            }
        }

        // ------------------- Calculate AVG Foreground Background Voxels ---------------------

        float avgBGVoxels = 0, avgFGVoxels = 0;
        for (int threadId = 0; threadId < NO_THREADS; threadId++) {
            avgFGVoxels += listFGVoxels[threadId];
            avgBGVoxels += listBGVoxels[threadId];
        }
        avgFGVoxels /= (Nx * Ny * Nz * colorImages.size());
        avgBGVoxels /= (Nx * Ny * Nz * colorImages.size());


        // ------------------- Calculate AVG Foreground Background Pixels ---------------------

        final float[] listFGPixels = new float[NO_THREADS];
        final float[] listBGPixels = new float[NO_THREADS];
        Arrays.fill(listFGPixels, 0);
        Arrays.fill(listBGPixels, 0);

        final int rangeImage = maskImages.get(0).length / NO_THREADS;
        for (int i = 0; i < maskImages.size(); i++) {
            final byte[] maskImage = maskImages.get(i);

            ExecutorService es = Executors.newCachedThreadPool();
            for (int threadId = 0; threadId < NO_THREADS; threadId++) {
                final int finalThreadId = threadId;
                es.execute(new Runnable() {
                    int vStart = finalThreadId * rangeImage;

                    @Override
                    public void run() {
                        int vEnd = vStart + rangeImage;
                        if (finalThreadId + 1 == NO_THREADS) {
                            vEnd = maskImage.length;
                        }

                        for (int pixelIdx = vStart; pixelIdx < vEnd; pixelIdx++) {
                            if (maskImage[pixelIdx] != 0)
                                listFGPixels[finalThreadId] += 1;
                            else
                                listBGPixels[finalThreadId] += 1;
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

        float avgFGPixels = 0, avgBGPixels = 0;
        for (int threadId = 0; threadId < NO_THREADS; threadId++) {
            avgFGPixels += listFGPixels[threadId];
            avgBGPixels += listBGPixels[threadId];
        }
        avgFGPixels /= ( maskImages.size() * maskImages.get(0).length);
        avgBGPixels /= ( maskImages.size() * maskImages.get(0).length);

        // ------------------- Setting ---------------------
        if (Volume.getInstance().isFirstInit()) {
            Volume.getInstance().setAVG_FG_Pixels(avgFGPixels);
            Volume.getInstance().setAVG_BG_Pixels(avgBGPixels);

            Volume.getInstance().setAVG_FG_Voxels(avgFGVoxels);
            Volume.getInstance().setAVG_BG_Voxels(avgBGVoxels);
        }
        else{
            avgFGPixels = (Volume.getInstance().getAVG_FG_Pixels() * w +  avgFGPixels) / (w + 1);
            avgBGPixels = (Volume.getInstance().getAVG_BG_Pixels() * w +  avgBGPixels) / (w + 1);

            Volume.getInstance().setAVG_FG_Pixels(avgFGPixels);
            Volume.getInstance().setAVG_BG_Pixels(avgBGPixels);

            avgFGVoxels = (Volume.getInstance().getAVG_FG_Voxels() * w +  avgFGVoxels) / (w + 1);
            avgBGVoxels = (Volume.getInstance().getAVG_BG_Voxels() * w +  avgBGVoxels) / (w + 1);

            Volume.getInstance().setAVG_FG_Voxels(avgFGVoxels);
            Volume.getInstance().setAVG_BG_Voxels(avgBGVoxels);
        }



    }




    public static Bitmap arrayByte2Bitmap(byte[] array, int width, int height){
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        int alpha = 1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++){
                int id = y * width + x;
                int red = array[id];
                int green = array[id];
                int blue = array[id];
                bmp.setPixel(x, y, Color.argb(alpha, red, green, blue));

            }
        }
        return bmp;
    }


    public static void updateProbabilityBaseOnMulipleHistograms(
            MultipleHistogram multipleHistogram,
            final List<int[]> colorImages,
            final List<byte[]> maskImages,
            final List<float[]> MVPs,
            final List<float[]> quaternions,
            int width, int height,
            float w)
    {
        final float listFGVoxels[] = new float[NO_THREADS];
        final float listBGVoxels[] = new float[NO_THREADS];

        final float[] lbf = Volume.getInstance().getLBF();
        final float[] rtn = Volume.getInstance().getRTN();

        final int Nx = Volume.getInstance().getNx();
        final int Ny = Volume.getInstance().getNy();
        final int Nz = Volume.getInstance().getNz();

        final float stepX = (rtn[0] - lbf[0]) / (float)Volume.getInstance().getNx();
        final float stepY = (rtn[1] - lbf[1]) / (float)Volume.getInstance().getNy();
        final float stepZ = (rtn[2] - lbf[2]) / (float)Volume.getInstance().getNz();


        final float[] probData = Volume.getInstance().getProb();


        final int numBins = multipleHistogram.getNumBins();
        final int binShift = multipleHistogram.getBinShift();

        // ------------------- Calculate Prob in and out of Voxels ---------------------

        final int range = Nx * Ny * Nz / NO_THREADS;
        float[] logProb = new float[probData.length];

        for (int i = 0; i < colorImages.size(); i++) {
            final int[] colorImage = colorImages.get(i);
            final float[] MVP = MVPs.get(i);

            final float[] FGHistogram = multipleHistogram.getForegroundHistogram(quaternions.get(i));
            final float[] BGHistogram = multipleHistogram.getBackgroundHistogram(quaternions.get(i));

            ExecutorService es = Executors.newCachedThreadPool();
            for (int threadId = 0; threadId < NO_THREADS; threadId++) {
                final int finalThreadId = threadId;
                es.execute(new Runnable() {
                    int vStart = finalThreadId * range;

                    @Override
                    public void run() {
                        int vEnd = vStart + range;
                        if (finalThreadId + 1 == NO_THREADS) {
                            vEnd = Nz * Nx * Ny;
                        }

                        for (int voxelIdx = vStart; voxelIdx < vEnd; voxelIdx++) {
//                        int voxelIdx = ((iz * Nx + ix) * Ny) + iy;
                            int iz = voxelIdx / (Nx * Ny);
                            int ix = (voxelIdx -  iz * Nx * Ny) / Ny;
                            int iy = voxelIdx - iz * (Nx * Ny) - ix * Ny;

                            float[] point = new float[]{lbf[0] + ix * stepX, lbf[1] + iy * stepY, lbf[2] + iz * stepZ, 1.0f};
                            float[] p = new float[4];

                            Matrix.multiplyMV(p, 0, MVP, 0, point, 0);
                            if (p[3] == 0)
                                continue;

                            float[] p3f = new float[]{p[0] / p[3], p[1] / p[3], p[2] / p[3]};

                            float iPcRf = 0;
                            float iPcRb = 0;

                            if (p3f[0] < -1 ||
                                    p3f[0] > 1 ||
                                    p3f[1] < -1 ||
                                    p3f[1] > 1 ||
                                    p3f[2] < -1 ||
                                    p3f[2] > 1)
                            {

                            }
                            else {
                                int p3fX = (int) ((p3f[0] + 1) / 2 * width);
                                if (p3fX == width) p3fX -= 1;
                                int p3fY = (int) ((p3f[1] + 1) / 2 * height);
                                p3fY = height - p3fY;
                                if (p3fY == height) p3fY -= 1;

                                int pixelIdx = p3fY * width + p3fX;

                                int color = colorImage[pixelIdx]; // ARGB

                                // idx in histogram
                                int redIdx = ((color & 0x00FF0000) >> 16) >> binShift;
                                int greenIdx = ((color & 0x0000FF00) >> 8) >> binShift;
                                int blueIdx = ((color & 0x000000FF)) >> binShift;

                                int colorIdx = (redIdx * numBins + greenIdx) * numBins + blueIdx;

                                iPcRf = FGHistogram[colorIdx];
                                iPcRb = BGHistogram[colorIdx];


                            }
                            iPcRf += 1e-10f;
                            iPcRb += 1e-10f;

//                            probData[2 * voxelIdx] += (float) log(iPcRf);
//                            probData[2 * voxelIdx + 1] += (float) log(1 - iPcRb);


                            logProb[2 * voxelIdx] += (float) log(iPcRf);
                            logProb[2 * voxelIdx + 1] += (float) log(1 - iPcRb);


                            if (iPcRf > iPcRb) listFGVoxels[finalThreadId] += 1;
                            else listBGVoxels[finalThreadId] += 1;
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




        for (int voxelIdx = 0; voxelIdx < Nx * Ny * Nz; voxelIdx++) {
            logProb[2 * voxelIdx] /= colorImages.size();
            logProb[2 * voxelIdx + 1] /= colorImages.size();

            if (Volume.getInstance().isFirstInit()) {
                probData[2 * voxelIdx] = logProb[2 * voxelIdx];
                probData[2 * voxelIdx + 1] = logProb[2 * voxelIdx + 1];
            }
            else {
                probData[2 * voxelIdx] =  (w * probData[2 * voxelIdx] + logProb[2 * voxelIdx]) / (w + 1);
                probData[2 * voxelIdx + 1] = (w * probData[2 * voxelIdx + 1] + logProb[2 * voxelIdx + 1]) / (w + 1);
            }
        }

        // ------------------- Calculate AVG Foreground Background Voxels ---------------------

        float avgBGVoxels = 0, avgFGVoxels = 0;
        for (int threadId = 0; threadId < NO_THREADS; threadId++) {
            avgFGVoxels += listFGVoxels[threadId];
            avgBGVoxels += listBGVoxels[threadId];
        }
        avgFGVoxels /= (Nx * Ny * Nz * colorImages.size());
        avgBGVoxels /= (Nx * Ny * Nz * colorImages.size());


        // ------------------- Calculate AVG Foreground Background Pixels ---------------------

        final float[] listFGPixels = new float[NO_THREADS];
        final float[] listBGPixels = new float[NO_THREADS];
        Arrays.fill(listFGPixels, 0);
        Arrays.fill(listBGPixels, 0);

        final int rangeImage = maskImages.get(0).length / NO_THREADS;
        for (int i = 0; i < maskImages.size(); i++) {
            final byte[] maskImage = maskImages.get(i);

            ExecutorService es = Executors.newCachedThreadPool();
            for (int threadId = 0; threadId < NO_THREADS; threadId++) {
                final int finalThreadId = threadId;
                es.execute(new Runnable() {
                    int vStart = finalThreadId * rangeImage;

                    @Override
                    public void run() {
                        int vEnd = vStart + rangeImage;
                        if (finalThreadId + 1 == NO_THREADS) {
                            vEnd = maskImage.length;
                        }

                        for (int pixelIdx = vStart; pixelIdx < vEnd; pixelIdx++) {
                            if (maskImage[pixelIdx] != 0)
                                listFGPixels[finalThreadId] += 1;
                            else
                                listBGPixels[finalThreadId] += 1;
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

        float avgFGPixels = 0, avgBGPixels = 0;
        for (int threadId = 0; threadId < NO_THREADS; threadId++) {
            avgFGPixels += listFGPixels[threadId];
            avgBGPixels += listBGPixels[threadId];
        }
        avgFGPixels /= ( maskImages.size() * maskImages.get(0).length);
        avgBGPixels /= ( maskImages.size() * maskImages.get(0).length);

        // ------------------- Setting ---------------------
        if (Volume.getInstance().isFirstInit()) {
            Volume.getInstance().setAVG_FG_Pixels(avgFGPixels);
            Volume.getInstance().setAVG_BG_Pixels(avgBGPixels);

            Volume.getInstance().setAVG_FG_Voxels(avgFGVoxels);
            Volume.getInstance().setAVG_BG_Voxels(avgBGVoxels);
        }
        else{
            avgFGPixels = (Volume.getInstance().getAVG_FG_Pixels() * w +  avgFGPixels) / (w + 1);
            avgBGPixels = (Volume.getInstance().getAVG_BG_Pixels() * w +  avgBGPixels) / (w + 1);

            Volume.getInstance().setAVG_FG_Pixels(avgFGPixels);
            Volume.getInstance().setAVG_BG_Pixels(avgBGPixels);

            avgFGVoxels = (Volume.getInstance().getAVG_FG_Voxels() * w +  avgFGVoxels) / (w + 1);
            avgBGVoxels = (Volume.getInstance().getAVG_BG_Voxels() * w +  avgBGVoxels) / (w + 1);

            Volume.getInstance().setAVG_FG_Voxels(avgFGVoxels);
            Volume.getInstance().setAVG_BG_Voxels(avgBGVoxels);
        }

//
//        if (Volume.getInstance().isFirstInit() == true)
//            Volume.getInstance().setFirstInit(false);

    }



    public static void updateProbabilityBaseOnPieHistograms(
            PieHistogram multipleHistogram,
            final List<int[]> colorImages,
            final List<byte[]> maskImages,
            final List<float[]> MVPs,
            final List<float[]> quaternions,
            int width, int height,
            float w)
    {
        final float listFGVoxels[] = new float[NO_THREADS];
        final float listBGVoxels[] = new float[NO_THREADS];

        final float[] lbf = Volume.getInstance().getLBF();
        final float[] rtn = Volume.getInstance().getRTN();

        final int Nx = Volume.getInstance().getNx();
        final int Ny = Volume.getInstance().getNy();
        final int Nz = Volume.getInstance().getNz();

        final float stepX = (rtn[0] - lbf[0]) / (float)Volume.getInstance().getNx();
        final float stepY = (rtn[1] - lbf[1]) / (float)Volume.getInstance().getNy();
        final float stepZ = (rtn[2] - lbf[2]) / (float)Volume.getInstance().getNz();


        final float[] probData = Volume.getInstance().getProb();


        final int numBins = multipleHistogram.getNumBins();
        final int binShift = multipleHistogram.getBinShift();

        // ------------------- Calculate Prob in and out of Voxels ---------------------

        final int range = Nx * Ny * Nz / NO_THREADS;
        float[] logProb = new float[probData.length];

        for (int i = 0; i < colorImages.size(); i++) {
            final int[] colorImage = colorImages.get(i);
            final float[] MVP = MVPs.get(i);

            final float[] FGHistogram = multipleHistogram.getForegroundHistogram(quaternions.get(i));
            final float[] BGHistogram = multipleHistogram.getBackgroundHistogram(quaternions.get(i));

            ExecutorService es = Executors.newCachedThreadPool();
            for (int threadId = 0; threadId < NO_THREADS; threadId++) {
                final int finalThreadId = threadId;
                es.execute(new Runnable() {
                    int vStart = finalThreadId * range;

                    @Override
                    public void run() {
                        int vEnd = vStart + range;
                        if (finalThreadId + 1 == NO_THREADS) {
                            vEnd = Nz * Nx * Ny;
                        }

                        for (int voxelIdx = vStart; voxelIdx < vEnd; voxelIdx++) {
//                        int voxelIdx = ((iz * Nx + ix) * Ny) + iy;
                            int iz = voxelIdx / (Nx * Ny);
                            int ix = (voxelIdx -  iz * Nx * Ny) / Ny;
                            int iy = voxelIdx - iz * (Nx * Ny) - ix * Ny;

                            float[] point = new float[]{
                                    lbf[0] + ix * stepX + 0.5f * stepX,
                                    lbf[1] + iy * stepY + 0.5f * stepY,
                                    lbf[2] + iz * stepZ + 0.5f * stepZ,
                                    1.0f};
                            float[] p = new float[4];

                            Matrix.multiplyMV(p, 0, MVP, 0, point, 0);
                            if (p[3] == 0)
                                continue;

                            float[] p3f = new float[]{p[0] / p[3], p[1] / p[3], p[2] / p[3]};

                            float iPcRf = 0;
                            float iPcRb = 0;

                            if (p3f[0] < -1 ||
                                    p3f[0] > 1 ||
                                    p3f[1] < -1 ||
                                    p3f[1] > 1 ||
                                    p3f[2] < -1 ||
                                    p3f[2] > 1)
                            {

                            }
                            else {
                                int p3fX = (int) ((p3f[0] + 1) / 2 * width);
                                if (p3fX == width) p3fX -= 1;
                                int p3fY = (int) ((p3f[1] + 1) / 2 * height);
                                p3fY = height - p3fY;
                                if (p3fY == height) p3fY -= 1;
//
//                                float[] color = Algorithm.interpolation(colorImage, p3fX, p3fY, width, height);
////
//                                // idx in histogram
//                                int redIdx = (int)color[0] >> binShift;
//                                int greenIdx = (int)color[1] >> binShift;
//                                int blueIdx = (int)color[2] >> binShift;
//
//                                int colorIdx = (redIdx * numBins + greenIdx) * numBins + blueIdx;
//
//                                iPcRf = FGHistogram[colorIdx];
//                                iPcRb = BGHistogram[colorIdx];

                                float[] iPc = Algorithm.interpolation(
                                        colorImage, p3fX, p3fY, width, height, FGHistogram, BGHistogram, binShift, numBins);
                                iPcRf = iPc[0];
                                iPcRb = iPc[1];

                            }
                            iPcRf += 1e-10f;
                            iPcRb += 1e-10f;

//                            probData[2 * voxelIdx] += (float) log(iPcRf);
//                            probData[2 * voxelIdx + 1] += (float) log(1 - iPcRb);


                            logProb[2 * voxelIdx] += (float) log(iPcRf);
                            logProb[2 * voxelIdx + 1] += (float) log(1 - iPcRb);


                            if (iPcRf > iPcRb) listFGVoxels[finalThreadId] += 1;
                            else listBGVoxels[finalThreadId] += 1;
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




        for (int voxelIdx = 0; voxelIdx < Nx * Ny * Nz; voxelIdx++) {
            logProb[2 * voxelIdx] /= colorImages.size();
            logProb[2 * voxelIdx + 1] /= colorImages.size();

            if (Volume.getInstance().isFirstInit()) {
                probData[2 * voxelIdx] = logProb[2 * voxelIdx];
                probData[2 * voxelIdx + 1] = logProb[2 * voxelIdx + 1];
            }
            else {
                probData[2 * voxelIdx] =  (w * probData[2 * voxelIdx] + logProb[2 * voxelIdx]) / (w + 1);
                probData[2 * voxelIdx + 1] = (w * probData[2 * voxelIdx + 1] + logProb[2 * voxelIdx + 1]) / (w + 1);
            }
        }

        // ------------------- Calculate AVG Foreground Background Voxels ---------------------

        float avgBGVoxels = 0, avgFGVoxels = 0;
        for (int threadId = 0; threadId < NO_THREADS; threadId++) {
            avgFGVoxels += listFGVoxels[threadId];
            avgBGVoxels += listBGVoxels[threadId];
        }
        avgFGVoxels /= (Nx * Ny * Nz * colorImages.size());
        avgBGVoxels /= (Nx * Ny * Nz * colorImages.size());


        // ------------------- Calculate AVG Foreground Background Pixels ---------------------

        final float[] listFGPixels = new float[NO_THREADS];
        final float[] listBGPixels = new float[NO_THREADS];
        Arrays.fill(listFGPixels, 0);
        Arrays.fill(listBGPixels, 0);

        final int rangeImage = maskImages.get(0).length / NO_THREADS;
        for (int i = 0; i < maskImages.size(); i++) {
            final byte[] maskImage = maskImages.get(i);

            ExecutorService es = Executors.newCachedThreadPool();
            for (int threadId = 0; threadId < NO_THREADS; threadId++) {
                final int finalThreadId = threadId;
                es.execute(new Runnable() {
                    int vStart = finalThreadId * rangeImage;

                    @Override
                    public void run() {
                        int vEnd = vStart + rangeImage;
                        if (finalThreadId + 1 == NO_THREADS) {
                            vEnd = maskImage.length;
                        }

                        for (int pixelIdx = vStart; pixelIdx < vEnd; pixelIdx++) {
                            if (maskImage[pixelIdx] != 0)
                                listFGPixels[finalThreadId] += 1;
                            else
                                listBGPixels[finalThreadId] += 1;
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

        float avgFGPixels = 0, avgBGPixels = 0;
        for (int threadId = 0; threadId < NO_THREADS; threadId++) {
            avgFGPixels += listFGPixels[threadId];
            avgBGPixels += listBGPixels[threadId];
        }
        avgFGPixels /= ( maskImages.size() * maskImages.get(0).length);
        avgBGPixels /= ( maskImages.size() * maskImages.get(0).length);

        // ------------------- Setting ---------------------
        if (Volume.getInstance().isFirstInit()) {
            Volume.getInstance().setAVG_FG_Pixels(avgFGPixels);
            Volume.getInstance().setAVG_BG_Pixels(avgBGPixels);

            Volume.getInstance().setAVG_FG_Voxels(avgFGVoxels);
            Volume.getInstance().setAVG_BG_Voxels(avgBGVoxels);
        }
        else{
            avgFGPixels = (Volume.getInstance().getAVG_FG_Pixels() * w +  avgFGPixels) / (w + 1);
            avgBGPixels = (Volume.getInstance().getAVG_BG_Pixels() * w +  avgBGPixels) / (w + 1);

            Volume.getInstance().setAVG_FG_Pixels(avgFGPixels);
            Volume.getInstance().setAVG_BG_Pixels(avgBGPixels);

            avgFGVoxels = (Volume.getInstance().getAVG_FG_Voxels() * w +  avgFGVoxels) / (w + 1);
            avgBGVoxels = (Volume.getInstance().getAVG_BG_Voxels() * w +  avgBGVoxels) / (w + 1);

            Volume.getInstance().setAVG_FG_Voxels(avgFGVoxels);
            Volume.getInstance().setAVG_BG_Voxels(avgBGVoxels);
        }

//
//        if (Volume.getInstance().isFirstInit() == true)
//            Volume.getInstance().setFirstInit(false);

    }


    public static void shapeEstimate(){

    }
}
