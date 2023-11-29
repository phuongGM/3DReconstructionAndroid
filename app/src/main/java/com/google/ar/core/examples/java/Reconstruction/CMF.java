package com.google.ar.core.examples.java.Reconstruction;

import static com.google.ar.core.examples.java.Reconstruction.Settings.NO_THREADS;

import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class CMF {
    /**
     * @param alpha Penalty parameters
     * @param steps Steps for the step-size of projected-gradient of p
     * @param cc Step-size of Augmented Larange Multiplier
     * @param fError error criterion
     * @param numIters the maximum iteration number
     * @param volumePu output results
     */
    public static void solve(
            float alpha,
            final float steps,
            final float cc, float fError,
            float numIters,
            final float[] volumePu)
    {
        // Bound of sink flows
        final float[] probData = Volume.getInstance().getProb();
        final float avgFGPixels = Volume.getInstance().getAVG_FG_Pixels();
        final float avgBGPixels = Volume.getInstance().getAVG_BG_Pixels();
        final float avgFGVoxels = Volume.getInstance().getAVG_FG_Voxels();
        final float avgBGVoxels = Volume.getInstance().getAVG_BG_Voxels();
        final int Nx = Volume.getInstance().getNx();
        final int Ny = Volume.getInstance().getNy();
        final int Nz = Volume.getInstance().getNz();

        /* Inputs */

        // number of labels
        int noLabels = 2;

        /* Memory allocation */
        final float[][] arrCt = new float[noLabels][Nx * Ny * Nz];

        final float[] ps = new float[Nx * Ny * Nz];
        Arrays.fill(ps, 0);

        final float[][] arrPt = new float[noLabels][Nx * Ny * Nz];
        for(float[] row: arrPt) Arrays.fill(row, 0);

        final float[][] arrPu= new float[noLabels][Nx * Ny * Nz];
        for(float[] row: arrPu) Arrays.fill(row, 0);

        final float[][] arrDivp = new float[noLabels][Nx * Ny * Nz];
        for(float[] row: arrDivp) Arrays.fill(row, 0);

        final float[][] arrBx = new float[noLabels][(Nx + 1) * Ny * Nz];
        for(float[] row: arrBx) Arrays.fill(row, 0);

        final float[][] arrBy = new float[noLabels][Nx * (Ny + 1) * Nz];
        for(float[] row: arrBy) Arrays.fill(row, 0);

        final float[][] arrBz = new float[noLabels][Nx * Ny * (Nz + 1)];
        for(float[] row: arrBz) Arrays.fill(row, 0);


        final float[] gk = new float[Nx * Ny * Nz];
        final float[] pts = new float[Nx * Ny * Nz];

        final float[] penalty = new float[Nx * Ny * Nz];
        Arrays.fill(penalty, alpha);
        final float[] pd = new float[Nx * Ny * Nz];

        final float[] listErrorIter = new float[NO_THREADS];

        final float beta = 0.75f;

        final int range = Nz * Nx * Ny / NO_THREADS;

        ExecutorService es = Executors.newCachedThreadPool();

        for (int threadId = 0; threadId < NO_THREADS; threadId++){

            final int finalThreadId = threadId;
            final float finalAvgFGPixels = avgFGPixels;
            final float finalAvgBGPixels = avgBGPixels;
            final float finalAvgFGVoxels = avgFGVoxels;
            final float finalAvgBGVoxels = avgBGVoxels;

            es.execute(new Runnable() {
                int vStart = finalThreadId * range;
                @Override
                public void run() {
                    int vEnd = vStart + range;
                    if (finalThreadId + 1 == NO_THREADS){
                        vEnd = Nz * Nx * Ny;
                    }

                    for (int voxelIdx = vStart; voxelIdx < vEnd; voxelIdx++){
                        float x = probData[2 * voxelIdx];
                        float y = probData[2 * voxelIdx + 1];
                        float iPcRf = (float) exp(x);
                        float iPcRb = (float) (1 - exp(y));
                        float iPi = (finalAvgFGPixels / finalAvgFGVoxels) * iPcRf / (iPcRf * finalAvgFGPixels + iPcRb * finalAvgBGPixels);
                        float iPo = (finalAvgBGPixels / finalAvgBGVoxels) * iPcRb / (iPcRf * finalAvgFGPixels + iPcRb * finalAvgBGPixels);

                        arrCt[0][voxelIdx] = iPi;
                        arrCt[1][voxelIdx] = iPo;
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

        es = Executors.newCachedThreadPool();
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

                        float pi, po;
                        pi = arrCt[0][voxelIdx];
                        po = arrCt[1][voxelIdx];

                        if (pi > po) {
                            arrPu[0][voxelIdx] = 1;
                            ps[voxelIdx] = pi;
                            arrPt[0][voxelIdx] = pi;
                            arrPt[1][voxelIdx] = pi;

                        } else {
                            arrPu[1][voxelIdx] = 1;
                            ps[voxelIdx] = po;
                            arrPt[0][voxelIdx] = po;
                            arrPt[1][voxelIdx] = po;
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


        /*  Main iterations */
        int iNI = 0;

        while (iNI < numIters) {
            Arrays.fill(pd, 0);

            // update the flow fields within each layer k=1...nlab
            for (int k = 0; k < noLabels; k++) {

                float[] Ct_k = arrCt[k];
                float[] divp_k = arrDivp[k];

                float[] pt_k = arrPt[k];
                float[] pu_k = arrPu[k];

                float[] bx_k = arrBx[k];
                float[] by_k = arrBy[k];
                float[] bz_k = arrBz[k];

                // update the spatial flow field p(x,i) = (bx(x,i), by(x,i), bz(x,i)):
                // the following steps are the gradient descent step with steps as the
                // step-size.

                es = Executors.newCachedThreadPool();
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
                                pts[voxelIdx] = divp_k[voxelIdx] - (ps[voxelIdx] - pt_k[voxelIdx] + pu_k[voxelIdx] / cc);
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

                // update the component bx(x,i)
                final int rangeX = Nz * (Nx - 1) * Ny / NO_THREADS;
                es = Executors.newCachedThreadPool();
                for (int threadId = 0; threadId < NO_THREADS; threadId++) {
                    final int finalThreadId = threadId;
                    es.execute(new Runnable() {
                        int vStart = finalThreadId * rangeX;

                        @Override
                        public void run() {
                            int vEnd = vStart + rangeX;
                            if (finalThreadId + 1 == NO_THREADS) {
                                vEnd = Nz * (Nx - 1) * Ny;
                            }
                            for (int id1 = vStart; id1 < vEnd; id1++) {
                                int id0 = id1 + Ny;
                                bx_k[id0] += steps * (pts[id0] - pts[id1]);
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

                // update the component by(x,i)
                final int rangeY = Nz * Nx * (Ny - 1) / NO_THREADS;
                es = Executors.newCachedThreadPool();
                for (int threadId = 0; threadId < NO_THREADS; threadId++) {
                    final int finalThreadId = threadId;
                    es.execute(new Runnable() {
                        int vStart = finalThreadId * rangeY;

                        @Override
                        public void run() {
                            int vEnd = vStart + rangeY;
                            if (finalThreadId + 1 == NO_THREADS) {
                                vEnd = Nz * Nx * (Ny - 1);
                            }
                            for (int id1 = vStart; id1 < vEnd; id1++) {
                                int id0 = id1 + 1;
                                by_k[id0] += steps * (pts[id0] - pts[id1]);
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

                // update the component bz(x,i)
                final int rangeZ = (Nz - 1) * Nx * Ny / NO_THREADS;
                es = Executors.newCachedThreadPool();
                for (int threadId = 0; threadId < NO_THREADS; threadId++) {
                    final int finalThreadId = threadId;
                    es.execute(new Runnable() {
                        int vStart = finalThreadId * rangeZ;

                        @Override
                        public void run() {
                            int vEnd = vStart + rangeZ;
                            if (finalThreadId + 1 == NO_THREADS) {
                                vEnd = (Nz - 1) * Nx * Ny;
                            }
                            for (int id1 = vStart; id1 < vEnd; id1++) {
                                int id0 = id1 + Nx * Ny;
                                bz_k[id0] += steps * (pts[id0] - pts[id1]);
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

                // the following steps give the projection to make |p(x,i)| <= alpha(x)
                es = Executors.newCachedThreadPool();
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
                            for (int id = vStart; id < vEnd; id++) {
                                int id_x = id + Ny;
                                int id_y = id + 1;
                                int id_z = id + Nx * Ny;


                                float fpt = (float) sqrt(
                                        (pow(bx_k[id], 2) + pow(bx_k[id_x], 2) +
                                                pow(by_k[id], 2) + pow(by_k[id_y], 2) +
                                                pow(bz_k[id], 2) + pow(bz_k[id_z], 2)) * 0.5f);

                                if (fpt > penalty[id])
                                    gk[id] = penalty[id] / fpt;
                                else
                                    gk[id] = 1;
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

                // update the component bx(x,i)
                es = Executors.newCachedThreadPool();
                for (int threadId = 0; threadId < NO_THREADS; threadId++) {
                    final int finalThreadId = threadId;
                    es.execute(new Runnable() {
                        int vStart = finalThreadId * rangeX;

                        @Override
                        public void run() {
                            int vEnd = vStart + rangeX;
                            if (finalThreadId + 1 == NO_THREADS) {
                                vEnd = Nz * (Nx - 1) * Ny;
                            }
                            for (int id1 = vStart; id1 < vEnd; id1++) {
                                int id0 = id1 + Ny;
                                bx_k[id0] = (gk[id0] + gk[id1]) * 0.5f * bx_k[id0];
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

                // update the component by(x,i)
                es = Executors.newCachedThreadPool();
                for (int threadId = 0; threadId < NO_THREADS; threadId++) {
                    final int finalThreadId = threadId;
                    es.execute(new Runnable() {
                        int vStart = finalThreadId * rangeY;

                        @Override
                        public void run() {
                            int vEnd = vStart + rangeY;
                            if (finalThreadId + 1 == NO_THREADS) {
                                vEnd = Nz * Nx * (Ny - 1);
                            }
                            for (int id1 = vStart; id1 < vEnd; id1++) {
                                int id0 = id1 + 1;
                                by_k[id0] = (gk[id0] + gk[id1]) * 0.5f * by_k[id0];
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

                // update the component bz(x,i)
                es = Executors.newCachedThreadPool();
                for (int threadId = 0; threadId < NO_THREADS; threadId++) {
                    final int finalThreadId = threadId;
                    es.execute(new Runnable() {
                        int vStart = finalThreadId * rangeZ;

                        @Override
                        public void run() {
                            int vEnd = vStart + rangeZ;
                            if (finalThreadId + 1 == NO_THREADS) {
                                vEnd = (Nz - 1) * Nx * Ny;
                            }
                            for (int id1 = vStart; id1 < vEnd; id1++) {
                                int id0 = id1 + Nx * Ny;
                                bz_k[id0] = (gk[id0] + gk[id1]) * 0.5f * bz_k[id0];
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


                // recompute the divergence field divp(x,i)
                es = Executors.newCachedThreadPool();
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
                            for (int id = vStart; id < vEnd; id++) {
                                int id_x = id + Ny;
                                int id_y = id + 1;
                                int id_z = id + Nx * Ny;

                                divp_k[id] = bx_k[id_x] - bx_k[id] +
                                            by_k[id_y] - by_k[id] +
                                            bz_k[id_z] - bz_k[id];
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


                // update the sink flow field pt(x,i)
                es = Executors.newCachedThreadPool();
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
                                float ud = -divp_k[voxelIdx] + ps[voxelIdx] + pu_k[voxelIdx] / cc;
                                pt_k[voxelIdx] = min(ud, Ct_k[voxelIdx]);
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

                // pd: the sum-up field for the computation of the source flow field  ps(x)
                es = Executors.newCachedThreadPool();
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
                                pd[voxelIdx] += (divp_k[voxelIdx] + pt_k[voxelIdx] - pu_k[voxelIdx] / cc);
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

            // updata the source flow ps
            es = Executors.newCachedThreadPool();
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
                            ps[voxelIdx] = pd[voxelIdx] / noLabels + 1 / (cc * noLabels);
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

            // update the multiplier u
            Arrays.fill(listErrorIter, 0);
            for (int k = 0; k < noLabels; k++) {

                float[] divp_k = arrDivp[k];
                float[] pt_k = arrPt[k];
                float[] pu_k = arrPu[k];

                es = Executors.newCachedThreadPool();
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
                                float error = cc * (divp_k[voxelIdx] - ps[voxelIdx] + pt_k[voxelIdx]);
                                pu_k[voxelIdx] -= error;
                                listErrorIter[finalThreadId] += abs(error);
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

            // evaluate the avarage error
            float errorIter = 0;
            for (int i = 0; i < NO_THREADS; i++){
                errorIter += listErrorIter[i];
            }
            float cvg = errorIter / (Nx * Ny * Nz);

            if (cvg <= fError)
                break;

            iNI++;
        }
        Log.v("CMF", String.valueOf(iNI));


        es = Executors.newCachedThreadPool();
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
                        if (arrPu[0][voxelIdx] > arrPu[1][voxelIdx])
//                            volumePu[voxelIdx] = 1;
                            volumePu[voxelIdx] = 1;
                        else
                            volumePu[voxelIdx] = 0;
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
}
