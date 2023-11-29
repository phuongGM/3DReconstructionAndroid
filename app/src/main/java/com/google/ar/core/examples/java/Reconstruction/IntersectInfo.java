package com.google.ar.core.examples.java.Reconstruction;

public class IntersectInfo {
    // 0 - 255
    public int signConfig;

    // If it exists, vertex on edge i is stored at position i.
    // For edge numbering and location see numberings.png.
    public float[][] edgeVertIndices = new float[12][3];
}
