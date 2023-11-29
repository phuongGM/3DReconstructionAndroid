package com.google.ar.core.examples.java.Reconstruction;


import java.util.Arrays;

public class Volume {
    private float[] mLBF; // left bottom near
    private float[] mRTN; // right top far

    private float mSize;

    private int mNx, mNy, mNz; // Number of voxels along x, y, z axis

    private float[] mProbData; // 3D volume Nz x Nx x Ny containing prob of voxel in FG and in BG
    private float[] mPu;

    private boolean isFirstInit;

    float mAVG_FG_Pixels, mAVG_BG_Pixels;
    float mAVG_FG_Voxels, mAVG_BG_Voxels;

    int mWidth, mHeight;

    private LikelihoodDistribution lkhDist;

    private static final Volume instance = new Volume();

    // ---------------------------------------------
    public static Volume getInstance() {
        return instance;
    }

    public Volume(){
        mNx = mNy = mNz = 0;
        lkhDist = new LikelihoodDistribution();
    }

    public void initVolumeSize(int Nx, int Ny, int Nz){
        mNx = Nx;
        mNy = Ny;
        mNz = Nz;

        mLBF = new float[3];
        mRTN = new float[3];

        mProbData = new float[Nz * Nx * Ny * 2];
        Arrays.fill(mProbData, 0);
        mPu = new float[Nz * Nx * Ny];
        Arrays.fill(mPu, 0);

        isFirstInit = true;
    }

    public void setSize(float size) {
        mSize = size;

        mLBF[0] = -mSize / 2;
        mLBF[1] = 0;
        mLBF[2] = -mSize / 2;
        // Right Top Near Coordinate
        mRTN[0] = mSize / 2;
        mRTN[1] = mSize;
        mRTN[2] = mSize / 2;
    }

    public void initPu(){
//        for (int idZ = 1 * mNz / 3; idZ < 2 * mNz / 3; idZ++) {
//            for (int idX = 1 * mNx / 3; idX < 2 * mNx / 3; idX++) {
//                for (int idY = 0; idY < 1 * mNy / 3; idY++)
//                {
//                    int voxelIdx = (idZ * mNx + idX) * mNy + idY;
//                    mPu[voxelIdx] = 1f;
//                }
//            }
//        }

        for (int idZ = 0; idZ < mNz; idZ++) {
            for (int idX = 0; idX < mNx; idX++) {
                for (int idY = 0; idY < mNy; idY++)
                {
                    int voxelIdx = (idZ * mNx + idX) * mNy + idY;
                    mPu[voxelIdx] = 1f;
                }
            }
        }

    }

    public float[] getLBF() {
        return mLBF;
    }

    public float[] getRTN() {
        return mRTN;
    }

    public int getNx() {
        return mNx;
    }

    public int getNy() {
        return mNy;
    }

    public int getNz() {
        return mNz;
    }

    public float[] getProb() {
        return mProbData;
    }

    public float[] getPu() {
        return mPu;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public LikelihoodDistribution getLkhDist() {
        return lkhDist;
    }

    public float getAVG_FG_Pixels() {
        return mAVG_FG_Pixels;
    }

    public float getAVG_BG_Pixels() {
        return mAVG_BG_Pixels;
    }

    public void setAVG_FG_Pixels(float mAVG_FG_Pixels) {
        this.mAVG_FG_Pixels = mAVG_FG_Pixels;
    }

    public void setAVG_BG_Pixels(float mAVG_BG_Pixels) {
        this.mAVG_BG_Pixels = mAVG_BG_Pixels;
    }

    public boolean isFirstInit() {
        return isFirstInit;
    }

    public void setFirstInit(boolean firstInit) {
        isFirstInit = firstInit;
    }

    public float getAVG_FG_Voxels() {
        return mAVG_FG_Voxels;
    }

    public void setAVG_FG_Voxels(float mAVG_FG_Voxels) {
        this.mAVG_FG_Voxels = mAVG_FG_Voxels;
    }

    public float getAVG_BG_Voxels() {
        return mAVG_BG_Voxels;
    }

    public void setAVG_BG_Voxels(float mAVG_BG_Voxels) {
        this.mAVG_BG_Voxels = mAVG_BG_Voxels;
    }

}
