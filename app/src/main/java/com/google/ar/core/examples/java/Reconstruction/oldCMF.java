//package com.google.ar.core.examples.java.Reconstruction;
//
//import android.util.Log;
//
//import java.util.Arrays;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//import static java.lang.Math.abs;
//import static java.lang.Math.exp;
//import static java.lang.Math.min;
//import static java.lang.Math.pow;
//import static java.lang.Math.sqrt;
//
//public class CMF {
//    /**
//     *
//     * @param probData Bound of sink flows
//     * @param alpha Penalty parameters
//     * @param Nx Number of voxels in x axis
//     * @param Ny Number of voxels in y axis
//     * @param Nz Number of voxels in z axis
//     * @param steps Steps for the step-size of projected-gradient of p
//     * @param cc Step-size of Augmented Larange Multiplier
//     * @param fError error criterion
//     * @param numIters the maximum iteration number
//     * @param numThreads Number of running threads
//     * @param pu output results
//     */
//    public static void solve(
//            final float[] probData,
//            final float avgFGPixels, final float avgBGPixels,
//            final float avgFGVoxels, final float avgBGVoxels,
//            float alpha,
//            final int Nx, final int Ny, final int Nz,
//            final float steps,
//            final float cc, float fError,
//            float numIters, final int numThreads,
//            final boolean isFirstInit,
//            final float[] pu){
//
//
//        /* Inputs */
//
//        /*
//         *pfVecParameters Setting
//         * [0] : number of columns
//         * [1] : number of rows
//         * [2] : number of heights
//         * [3] : number of labels
//         * [4] : the maximum iteration number
//         * [5] : error criterion
//         * [6] : cc for the step-size of ALM
//         * [7] : steps for the step-size of projected-gradient of p
//         */
//
//        /* Memory allocation */
//        final float[] Ct = new float [Nx * Ny * Nz * 2];
//        final float[] bx = new float[(Nx + 1) * Ny * Nz];
//        final float[] by = new float[Nx * (Ny + 1) * Nz];
//        final float[] bz = new float[Nx * Ny * (Nz + 1)];
//        final float[] divp = new float[Nx * Ny * Nz];
//        final float[] ps = new float[Nx * Ny * Nz];
//        final float[] pt = new float[Nx * Ny * Nz];;
//        final float[] gk = new float[Nx * Ny * Nz];
//        final float[] pts = new float[Nx * Ny * Nz];
//        final float[] penalty = new float[Nx * Ny * Nz];
//        Arrays.fill(penalty, alpha);
//        final float listErrorIter[] = new float[numThreads];
//        final float beta = 1f;
//
//
//        final int range = Nz * Nx * Ny / numThreads;
//
//        ExecutorService es = Executors.newCachedThreadPool();
//
//        for (int threadId = 0; threadId < numThreads; threadId++){
//            final int finalThreadId = threadId;
//            final float finalAvgFGPixels = avgFGPixels;
//            final float finalAvgBGPixels = avgBGPixels;
//            final float finalAvgFGVoxels = avgFGVoxels;
//            final float finalAvgBGVoxels = avgBGVoxels;
//            es.execute(new Runnable() {
//                int vStart = finalThreadId * range;
//                @Override
//                public void run() {
//                    int vEnd = vStart + range;
//                    if (finalThreadId + 1 == numThreads){
//                        vEnd = Nz * Nx * Ny;
//                    }
//
//                    for (int voxelIdx = vStart; voxelIdx < vEnd; voxelIdx++){
//                        float x = probData[2 * voxelIdx];
//                        float y = probData[2 * voxelIdx + 1];
//                        float iPcRf = (float) exp(x);
//                        float iPcRb = (float) (1 - exp(y));
//                        float iPi = (finalAvgFGPixels / finalAvgFGVoxels) * iPcRf / (iPcRf * finalAvgFGPixels + iPcRb * finalAvgBGPixels);
//                        float iPo = (finalAvgBGPixels / finalAvgBGVoxels) * iPcRb / (iPcRf * finalAvgFGPixels + iPcRb * finalAvgBGPixels);
//
//                        Ct[2 * voxelIdx] = iPi;
//                        Ct[2 * voxelIdx + 1] = iPo;
//                    }
//                }
//            });
//        }
//        es.shutdown();
//        try {
//            boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
////        int k = 12;
////        Log.v("CMF_Pi", String.valueOf(Ct[2 * k]));
////        Log.v("CMF_Po", String.valueOf(Ct[2 * k + 1]));
////        Log.d("CMF_", "=======================");
//
//
//        es = Executors.newCachedThreadPool();
//        for (int threadId = 0; threadId < numThreads; threadId++){
//            final int finalThreadId = threadId;
//            es.execute(new Runnable() {
//                int vStart = finalThreadId * range;
//                @Override
//                public void run() {
//                    int vEnd = vStart + range;
//                    if (finalThreadId + 1 == numThreads){
//                        vEnd = Nz * Nx * Ny;
//                    }
//                    for (int voxelIdx = vStart; voxelIdx < vEnd; voxelIdx++){
//                        float ui_prev = beta * pu[voxelIdx] + beta - 1f;
//                        float uo_prev = beta * (1f - pu[voxelIdx]) + beta - 1f;
//
//                        float pi, po;
//                        if (isFirstInit){
//                            pi = Ct[2 * voxelIdx];
//                            po = Ct[2 * voxelIdx + 1];
//                        }
//                        else {
//                            pi = Ct[2 * voxelIdx] * ui_prev;
//                            po = Ct[2 * voxelIdx + 1] * uo_prev;
//                        }
//
//                        Ct[2 * voxelIdx] = pi;
//                        Ct[2 * voxelIdx + 1] = po;
//                        if (pi > po) {
////                            pu[voxelIdx] = pi - po;
//                            pu[voxelIdx] = 1;
//                            ps[voxelIdx] = po;
//                            pt[voxelIdx] = po;
//                        }
//                        else {
//                            pu[voxelIdx] = 0;
//                            ps[voxelIdx] = pi;
//                            pt[voxelIdx] = pi;
//                        }
//                    }
//                }
//            });
//        }
//        es.shutdown();
//        try {
//            boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        /*  Main iterations */
//        int iNI = 0;
//
//        while (iNI < numIters) {
//            es = Executors.newCachedThreadPool();
//            for (int threadId = 0; threadId < numThreads; threadId++){
//                final int finalThreadId = threadId;
//                es.execute(new Runnable() {
//                    int vStart = finalThreadId * range;
//                    @Override
//                    public void run() {
//                        int vEnd = vStart + range;
//                        if (finalThreadId + 1 == numThreads){
//                            vEnd = Nz * Nx * Ny;
//                        }
//                        for (int voxelIdx = vStart; voxelIdx < vEnd; voxelIdx++){
//                            pts[voxelIdx] = divp[voxelIdx] - (ps[voxelIdx] - pt[voxelIdx] + pu[voxelIdx] / cc);
//                        }
//                    }
//                });
//            }
//            es.shutdown();
//            try {
//                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//
//            final int rangeX = Nz * (Nx - 1) * Ny / numThreads;
//            es = Executors.newCachedThreadPool();
//            for (int threadId = 0; threadId < numThreads; threadId++){
//                final int finalThreadId = threadId;
//                es.execute(new Runnable() {
//                    int vStart = finalThreadId * rangeX;
//                    @Override
//                    public void run() {
//                        int vEnd = vStart + rangeX;
//                        if (finalThreadId + 1 == numThreads){
//                            vEnd = Nz * (Nx - 1) * Ny;
//                        }
//                        for (int id1 = vStart; id1 < vEnd; id1++){
//                            int id0 = id1 + Ny;
//                            bx[id0] += steps * (pts[id0] - pts[id1]);                        }
//                    }
//                });
//            }
//            es.shutdown();
//            try {
//                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            final int rangeY = Nz * Nx * (Ny - 1) / numThreads;
//            es = Executors.newCachedThreadPool();
//            for (int threadId = 0; threadId < numThreads; threadId++){
//                final int finalThreadId = threadId;
//                es.execute(new Runnable() {
//                    int vStart = finalThreadId * rangeY;
//                    @Override
//                    public void run() {
//                        int vEnd = vStart + rangeY;
//                        if (finalThreadId + 1 == numThreads){
//                            vEnd = Nz * Nx * (Ny - 1);
//                        }
//                        for (int id1 = vStart; id1 < vEnd; id1++){
//                            int id0 = id1 + 1;
//                            by[id0] += steps * (pts[id0] - pts[id1]);
//                        }
//                    }
//                });
//            }
//            es.shutdown();
//            try {
//                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            final int rangeZ = (Nz - 1) * Nx * Ny / numThreads;
//            es = Executors.newCachedThreadPool();
//            for (int threadId = 0; threadId < numThreads; threadId++){
//                final int finalThreadId = threadId;
//                es.execute(new Runnable() {
//                    int vStart = finalThreadId * rangeZ;
//                    @Override
//                    public void run() {
//                        int vEnd = vStart + rangeZ;
//                        if (finalThreadId + 1 == numThreads){
//                            vEnd = (Nz - 1) * Nx * Ny;
//                        }
//                        for (int id1 = vStart; id1 < vEnd; id1++){
//                            int id0 = id1 + Nx * Ny;
//                            bz[id0] += steps * (pts[id0] - pts[id1]);                        }
//                    }
//                });
//            }
//            es.shutdown();
//            try {
//                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            es = Executors.newCachedThreadPool();
//            for (int threadId = 0; threadId < numThreads; threadId++){
//                final int finalThreadId = threadId;
//                es.execute(new Runnable() {
//                    int vStart = finalThreadId * range;
//                    @Override
//                    public void run() {
//                        int vEnd = vStart + range;
//                        if (finalThreadId + 1 == numThreads){
//                            vEnd = Nz * Nx * Ny;
//                        }
//                        for (int id = vStart; id < vEnd; id++){
//                            int id_x = id + Ny;
//                            int id_y = id + 1;
//                            int id_z = id + Nx * Ny;
//
//
//                            float fpt = (float) sqrt(
//                                    (pow(bx[id], 2) + pow(bx[id_x], 2) +
//                                            pow(by[id], 2) + pow(by[id_y], 2) +
//                                            pow(bz[id], 2) + pow(bz[id_z], 2)) * 0.5f);
//
//                            if (fpt > penalty[id])
//                                gk[id] = penalty[id] / fpt;
//                            else
//                                gk[id] = 1;
//                        }
//                    }
//                });
//            }
//            es.shutdown();
//            try {
//                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            es = Executors.newCachedThreadPool();
//            for (int threadId = 0; threadId < numThreads; threadId++){
//                final int finalThreadId = threadId;
//                es.execute(new Runnable() {
//                    int vStart = finalThreadId * rangeX;
//                    @Override
//                    public void run() {
//                        int vEnd = vStart + rangeX;
//                        if (finalThreadId + 1 == numThreads) {
//                            vEnd = Nz * (Nx - 1) * Ny;
//                        }
//                        for (int id1 = vStart; id1 < vEnd; id1++) {
//                            int id0 = id1 + Ny;
//                            bx[id0] = (gk[id0] + gk[id1]) * 0.5f * bx[id0];
//                        }
//                    }
//                });
//            }
//            es.shutdown();
//            try {
//                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            es = Executors.newCachedThreadPool();
//            for (int threadId = 0; threadId < numThreads; threadId++){
//                final int finalThreadId = threadId;
//                es.execute(new Runnable() {
//                    int vStart = finalThreadId * rangeY;
//                    @Override
//                    public void run() {
//                        int vEnd = vStart + rangeY;
//                        if (finalThreadId + 1 == numThreads) {
//                            vEnd = Nz * Nx * (Ny - 1);
//                        }
//                        for (int id1 = vStart; id1 < vEnd; id1++) {
//                            int id0 = id1 + 1;
//                            by[id0] = (gk[id0] + gk[id1]) * 0.5f * by[id0];
//                        }
//                    }
//                });
//            }
//            es.shutdown();
//            try {
//                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            es = Executors.newCachedThreadPool();
//            for (int threadId = 0; threadId < numThreads; threadId++){
//                final int finalThreadId = threadId;
//                es.execute(new Runnable() {
//                    int vStart = finalThreadId * rangeZ;
//                    @Override
//                    public void run() {
//                        int vEnd = vStart + rangeZ;
//                        if (finalThreadId + 1 == numThreads) {
//                            vEnd = (Nz - 1) * Nx * Ny;
//                        }
//                        for (int id1 = vStart; id1 < vEnd; id1++) {
//                            int id0 = id1 + Nx * Ny;
//                            bz[id0] = (gk[id0] + gk[id1]) * 0.5f * bz[id0];
//                        }
//                    }
//                });
//            }
//            es.shutdown();
//            try {
//                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//
//            es = Executors.newCachedThreadPool();
//            for (int threadId = 0; threadId < numThreads; threadId++){
//                final int finalThreadId = threadId;
//                es.execute(new Runnable() {
//                    int vStart = finalThreadId * range;
//                    @Override
//                    public void run() {
//                        int vEnd = vStart + range;
//                        if (finalThreadId + 1 == numThreads){
//                            vEnd = Nz * Nx * Ny;
//                        }
//                        for (int id = vStart; id < vEnd; id++){
//                            int id_x = id + Ny;
//                            int id_y = id + 1;
//                            int id_z = id + Nx * Ny;
//
////                        /* update the divergence field div(x,id)  */
//                            float c = bx[id_x] - bx[id] +
//                                    by[id_y] - by[id] +
//                                    bz[id_z] - bz[id];
//
//                            float a = divp[id] + pt[id] - pu[id] / cc + 1 / cc;
//                            float b = -divp[id] + ps[id] + pu[id] / cc;
//                            divp[id] = c;
//                            ps[id] = min(a, Ct[2 * id]);
//                            pt[id] = min(b, Ct[2 * id + 1]);
//
//                        }
//                    }
//                });
//            }
//            es.shutdown();
//            try {
//                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//
//            es = Executors.newCachedThreadPool();
//            Arrays.fill(listErrorIter, 0);
//            for (int threadId = 0; threadId < numThreads; threadId++){
//                final int finalThreadId = threadId;
//                es.execute(new Runnable() {
//                    int vStart = finalThreadId * range;
//                    @Override
//                    public void run() {
//                        int vEnd = vStart + range;
//                        if (finalThreadId + 1 == numThreads){
//                            vEnd = Nz * Nx * Ny;
//                        }
//                        for (int id = vStart; id < vEnd; id++){
//                            float error = cc * (divp[id] - ps[id] + pt[id]);
//
//                            pu[id] -= error;
//                            listErrorIter[finalThreadId] = listErrorIter[finalThreadId] + abs(error);
//                        }
//                    }
//                });
//            }
//            es.shutdown();
//            try {
//                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            float errorIter = 0;
//            for (int i = 0; i < numThreads; i++){
//                errorIter += listErrorIter[i];
//            }
//            float cvg = errorIter / (Nx * Ny * Nz);
//
//            if (cvg <= fError)
//                break;
//
//            iNI++;
//        }
//        Log.v("CMF", String.valueOf(iNI));
//
//    }
//}


//--------------------

/*

        threadReconstruction = new Runnable() {
            @Override
            public void run() {
                int updateHist = 3;
                 countingUpdate = 1;

                List<int[]> listColorImages = new ArrayList<>();
                List<byte[]> listMaskImages = new ArrayList<>();
                List<float[]> listPoses = new ArrayList<>();
                List<float[]> listQuaternions = new ArrayList<>();


                Volume.getInstance().setFirstInit(true);
                while (true) {
                    if (colorImage == null | maskImage == null) continue;
                    if (countingUpdate % updateHist != 0) continue;

                    listColorImages.add(colorImage.clone());
                    listMaskImages.add(maskImage.clone());
                    listPoses.add(modelViewProjectionMatrix.clone());

                    if (isMultipleHistograms == false) {
                        if (Volume.getInstance().isFirstInit()) {
                            if (listColorImages.size() == 10) {
                                isLock = true;
                                histogram.update(listColorImages, listMaskImages);
                                ShapeOptimization.updateProbabilityBaseOnHistogram(
                                        histogram,
                                        listColorImages,
                                        listMaskImages,
                                        listPoses,
                                        width, height,
                                        weight
                                );
                                CMF.solve(
                                        Volume.getInstance().getProb(),
                                        Volume.getInstance().getAVG_FG_Pixels(), Volume.getInstance().getAVG_BG_Pixels(),
                                        Volume.getInstance().getAVG_FG_Voxels(), Volume.getInstance().getAVG_BG_Voxels(),
                                        Nx, Ny, Nz,
                                        0.1f,
                                        0.11f,
                                        0.35f,
                                        1e-5f,
                                        1,
                                        4,
                                        Volume.getInstance().getPu());


                                if (isVoxelRendering) {
                                    MeshReconstructionEngine.voxelize(
                                            Volume.getInstance().getPu(),
                                            LBF, RTN,
                                            Nx, Ny, Nz);
                                }
                                else {
                                    MeshReconstructionEngine.marchingCube(
                                            Volume.getInstance().getPu(),
                                            LBF, RTN,
                                            Nx, Ny, Nz);
                                }

                                enableTransfer();
                                Volume.getInstance().setFirstInit(false);
                                listColorImages.clear();
                                listMaskImages.clear();
                                listPoses.clear();
                                Log.v("COUNTING", String.valueOf(colors - masks));
                                colors = 0;
                                masks = 0;
                                isLock = false;
                            }
                        }
                        else {
                            if (listColorImages.size() == 5) {
                                isLock = true;
                                histogram.update(listColorImages, listMaskImages);

                                ShapeOptimization.updateProbabilityBaseOnHistogram(
                                        histogram,
                                        listColorImages,
                                        listMaskImages,
                                        listPoses,
                                        width, height,
                                        weight
                                );

                                CMF.solve(
                                        Volume.getInstance().getProb(),
                                        Volume.getInstance().getAVG_FG_Pixels(), Volume.getInstance().getAVG_BG_Pixels(),
                                        Volume.getInstance().getAVG_FG_Voxels(), Volume.getInstance().getAVG_BG_Voxels(),
                                        Nx, Ny, Nz,
                                        0.1f,
                                        0.11f,
                                        0.35f,
                                        1e-5f,
                                        1,
                                        4,
                                        Volume.getInstance().getPu());

                                if (isVoxelRendering) {
                                    MeshReconstructionEngine.voxelize(
                                            Volume.getInstance().getPu(),
                                            LBF, RTN,
                                            Nx, Ny, Nz);
                                } else {
                                    MeshReconstructionEngine.marchingCube(
                                            Volume.getInstance().getPu(),
                                            LBF, RTN,
                                            Nx, Ny, Nz);
                                }

                                enableTransfer();
                                listColorImages.clear();
                                listMaskImages.clear();
                                listPoses.clear();

                                Log.v("COUNTING", String.valueOf(colors - masks));
                                colors = 0;
                                masks = 0;
                                isLock = false;
                            }
                        }
                    }
                    else {
                        if (Volume.getInstance().isFirstInit()) {
                            if (listColorImages.size() == 10) {
                                multipleHistogram.InitMultipleHistograms(listColorImages, listMaskImages, listQuaternions);
                                ShapeOptimization.updateProbabilityBaseOnMulipleHistograms(
                                        multipleHistogram,
                                        listColorImages,
                                        listMaskImages,
                                        listPoses,
                                        listQuaternions,
                                        width, height,
                                        weight
                                );
                                CMF.solve(
                                        Volume.getInstance().getProb(),
                                        Volume.getInstance().getAVG_FG_Pixels(), Volume.getInstance().getAVG_BG_Pixels(),
                                        Volume.getInstance().getAVG_FG_Voxels(), Volume.getInstance().getAVG_BG_Voxels(),
                                        Nx, Ny, Nz,
                                        0.1f,
                                        0.11f,
                                        0.35f,
                                        1e-5f,
                                        1,
                                        4,
                                        Volume.getInstance().getPu());


                                if (isVoxelRendering) {
                                    MeshReconstructionEngine.voxelize(
                                            Volume.getInstance().getPu(),
                                            LBF, RTN,
                                            Nx, Ny, Nz);
                                } else {
                                    MeshReconstructionEngine.marchingCube(
                                            Volume.getInstance().getPu(),
                                            LBF, RTN,
                                            Nx, Ny, Nz);
                                }

                                enableTransfer();
                                Volume.getInstance().setFirstInit(false);
                                listColorImages.clear();
                                listMaskImages.clear();
                                listPoses.clear();
                            }
                        }
                        else {
                            if (listColorImages.size() == 5) {
                                multipleHistogram.update(listColorImages, listMaskImages, listQuaternions);

                                ShapeOptimization.updateProbabilityBaseOnMulipleHistograms(
                                        multipleHistogram,
                                        listColorImages,
                                        listMaskImages,
                                        listPoses,
                                        listQuaternions,
                                        width, height,
                                        weight
                                );

                                CMF.solve(
                                        Volume.getInstance().getProb(),
                                        Volume.getInstance().getAVG_FG_Pixels(), Volume.getInstance().getAVG_BG_Pixels(),
                                        Volume.getInstance().getAVG_FG_Voxels(), Volume.getInstance().getAVG_BG_Voxels(),
                                        Nx, Ny, Nz,
                                        0.1f,
                                        0.11f,
                                        0.35f,
                                        1e-5f,
                                        1,
                                        4,
                                        Volume.getInstance().getPu());

                                if (isVoxelRendering) {
                                    MeshReconstructionEngine.voxelize(
                                            Volume.getInstance().getPu(),
                                            LBF, RTN,
                                            Nx, Ny, Nz);
                                } else {
                                    MeshReconstructionEngine.marchingCube(
                                            Volume.getInstance().getPu(),
                                            LBF, RTN,
                                            Nx, Ny, Nz);
                                }

                                enableTransfer();
                                listColorImages.clear();
                                listMaskImages.clear();
                                listPoses.clear();
                            }
                        }
                    }
                }
            }
        };

 */