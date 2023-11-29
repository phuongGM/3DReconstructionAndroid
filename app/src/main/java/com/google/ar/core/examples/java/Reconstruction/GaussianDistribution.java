package com.google.ar.core.examples.java.Reconstruction;


import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.pow;

import com.google.ar.core.examples.java.Utils.MyMath;

public class GaussianDistribution {
    private float[] mean;
    private float[] covMat;
    private float[] invCovMat;
    private float normalizedConst;

    public GaussianDistribution() {
        normalizedConst = 0;
    }

    public void update(float[] mean, float[] covMat, float alpha) {
        // Fuse
        if (this.normalizedConst == 0) {
            this.mean = mean;
            this.covMat = covMat;
        }
	    else {
            for(int i = 0; i < mean.length; i++) {
                this.mean[i] = alpha * this.mean[i] + (1 - alpha) * mean[i];
            }

            for(int i = 0; i < covMat.length; i++) {
                this.covMat[i] = alpha * this.covMat[i] + (1 - alpha) * covMat[i];
            }
        }
        this.invCovMat = MyMath.InvertMatrix3(this.covMat);
        this.normalizedConst = (float) (pow(2 * PI, -1.5) * pow(MyMath.Determinant(this.covMat), -0.5));
    }

    float getProbability(float[] color) {
        float[] tmp = MyMath.subtract(mean, color);
        float b = MyMath.dotProduct(tmp, MyMath.mulMatxVec(invCovMat, tmp, 3, 3, false));
        return (float) (normalizedConst * exp(-0.5 * b));
    }

}
