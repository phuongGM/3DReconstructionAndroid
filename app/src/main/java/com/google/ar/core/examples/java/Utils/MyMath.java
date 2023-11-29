package com.google.ar.core.examples.java.Utils;

import android.util.Log;

import java.util.Arrays;

import static java.lang.Math.PI;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class MyMath {
    private static String TAG = "MyMath";

    public static float[] InvertMatrix3(float[] mat){
        // computes the inverse of a matrix m
        float det = mat[0] * (mat[4] * mat[8] - mat[5] * mat[7]) -
                mat[3] * (mat[1] * mat[8] - mat[7] * mat[2]) +
                mat[6]* (mat[1] * mat[5] - mat[4] * mat[2]);

        float invdet = 1 / det;
        float[] dst = new float[mat.length];

        dst[0] = (mat[4] * mat[8] - mat[5] * mat[7]) * invdet;
        dst[3] = (mat[6]* mat[5] - mat[3] * mat[8]) * invdet;
        dst[6]= (mat[3] * mat[7] - mat[6]* mat[4]) * invdet;
        dst[1] = (mat[7] * mat[2] - mat[1] * mat[8]) * invdet;
        dst[4] = (mat[0] * mat[8] - mat[6]* mat[2]) * invdet;
        dst[7] = (mat[1] * mat[6]- mat[0] * mat[7]) * invdet;
        dst[2] = (mat[1] * mat[5] - mat[2] * mat[4]) * invdet;
        dst[5] = (mat[2] * mat[3] - mat[0] * mat[5]) * invdet;
        dst[8] = (mat[0] * mat[4] - mat[1] * mat[3]) * invdet;

        return dst;
    }

    public static float Determinant(float[] mat){
        float det = mat[0] * (mat[4] * mat[8] - mat[5] * mat[7]) -
                mat[3] * (mat[1] * mat[8] - mat[7] * mat[2]) +
                mat[6]* (mat[1] * mat[5] - mat[4] * mat[2]);
        return det;
    }

    public static float[] mulMatxVec(
            float[] matx,
            float[] vec,
            int rowsMatrix,
            int colsMatrix,
            boolean isRowOrder){
        float[] out = new float[rowsMatrix];
        Arrays.fill(out, 0);

        if (vec.length != colsMatrix) {
            Log.d(TAG, "fail mulMatxVec");
            return out;
        }

        if (isRowOrder){
        /*
          matrix = 0 1 2
                   3 4 5
                   6 7 8
         */
            for (int n = 0; n < rowsMatrix; n++){
                for (int m = 0; m < colsMatrix; m++){
                    out[n] += matx[n * colsMatrix + m] * vec[m];
                }
            }
        }
        else{
        /*
          matrix = 0 3 6
                   1 4 7
                   2 5 8
         */
            for (int n = 0; n < rowsMatrix; n++){
                for (int m = 0; m < colsMatrix; m++){
                    out[n] += matx[n + m * rowsMatrix] * vec[m];
                }
            }
        }
        return out;
    }

    public static float[] mulMVecMatx(
            float[] matx,
            float[] vec,
            int rowsMatrix,
            int colsMatrix,
            boolean isRowOrder){
        float[] out = new float[colsMatrix];
        Arrays.fill(out, 0);
        if (vec.length != rowsMatrix) {
            Log.d(TAG, "fail mulMVecMatx");
            return out;
        }
        if (isRowOrder){
        /*
          matrix = 0 1 2
                   3 4 5
                   6 7 8
         */
            for (int m = 0; m < colsMatrix; m++){
                for (int n = 0; n < rowsMatrix; n++){
                    out[m] += matx[n * colsMatrix + m] * vec[m];
                }
            }
        }
        else{
        /*
          matrix = 0 3 6
                   1 4 7
                   2 5 8
         */
            for (int m = 0; m < colsMatrix; m++){
                for (int n = 0; n < rowsMatrix; n++){
                    out[m] += matx[n + m * rowsMatrix] * vec[m];
                }
            }
        }
        return out;
    }

    public static float[] mulMatScalar(float[] matx, float alpha){
        float[] out = new float[matx.length];
        for (int i = 0; i < matx.length; i++){
            out[i] = matx[i] * alpha;
        }
        return out;
    }

    public static float dotProduct(float vectorA[], float vectorB[]) {
        float product = 0;

        // Loop for calculate cot product
        for (int i = 0; i < vectorA.length; i++)
            product = product + vectorA[i] * vectorB[i];
        return product;
    }

    public static float[] crossProduct(float vectorA[], float vectorB[]) {
        float output[] = new float[3];
        output[0] = vectorA[1] * vectorB[2]
                - vectorA[2] * vectorB[1];
        output[1] = vectorA[2] * vectorB[0]
                - vectorA[0] * vectorB[2];
        output[2] = vectorA[0] * vectorB[1]
                - vectorA[1] * vectorB[0];
        return output;
    }

    public static float[] subtract(float[] vectorA, float[] vectorB){
        float output[] = new float[vectorA.length];
        for (int i = 0; i < vectorA.length; i++) {
            output[i] = vectorA[i] - vectorB[i];
        }
        return output;
    }

    public static float[] add(float[] vectorA, float[] vectorB){
        float output[] = new float[vectorA.length];
        for (int i = 0; i < vectorA.length; i++) {
            output[i] = vectorA[i] + vectorB[i];
        }
        return output;
    }

    public static void normalize(float[] vector){
        float norm = (float) sqrt(vector[0] *vector[0] +
                vector[1] *vector[1] +
                vector[2] *vector[2]);
        vector[0] /= norm;
        vector[1] /= norm;
        vector[2] /= norm;
    }

    public static float cosineSimilarity(float[] vecA, float[] vecB){
        float lenA = 0;
        float lenB = 0;

        for (int i = 0; i < vecA.length; i++){
            lenA += vecA[i] * vecA[i];
            lenB += vecB[i] * vecB[i];
        }
        lenA = (float) Math.sqrt(lenA);
        lenB = (float) Math.sqrt(lenB);

        return dotProduct(vecA, vecB) / (lenA * lenB);
    }

    public static float[] slerp(float[] v0, float[] v1, float t) {
        // v0 and v1 should be unit length or else
        // something broken will happen.

        // Compute the cosine of the angle between the two vectors.
        float dot = MyMath.dotProduct(v0, v1);

        float DOT_THRESHOLD = 0.9995f;
        if (dot > DOT_THRESHOLD) {
            // If the inputs are too close for comfort, linearly interpolate
            // and normalize the result.

            float[] result = MyMath.add(v0, MyMath.mulMatScalar(MyMath.subtract(v1, v0), t));
            normalize(result);
            return result;
        }

        if (dot < -1) dot = -1;           // Robustness: Stay within domain of acos()
        if (dot > 1) dot = 1;           // Robustness: Stay within domain of acos()

        float theta_0 = (float) acos(dot);  // theta_0 = angle between input vectors
        float theta = theta_0*t;    // theta = angle between v0 and result

        float[] v2 = MyMath.subtract(v1, MyMath.mulMatScalar(v0, dot));
        normalize(v2);              // { v0, v2 } is now an orthonormal basis

        float[] v0_1 = MyMath.mulMatScalar(v0, (float) cos(theta));
        float[] v2_1 = MyMath.mulMatScalar(v2, (float) sin(theta));
        return add(v0_1, v2_1);
    }

    public static float[] quaternion2Euler(float[] q){

        double roll, pitch, yaw;

        // roll (x-axis rotation)
        double sinr_cosp = 2 * (q[3] * q[0] + q[1] * q[2]);
        double cosr_cosp = 1 - 2 * (q[0] * q[0] + q[1] * q[1]);
        roll = Math.toDegrees(Math.atan2(sinr_cosp, cosr_cosp));

        // pitch (y-axis rotation)
        double sinp = 2 * (q[3] * q[1] - q[2] * q[0]);
        if (Math.abs(sinp) >= 1)
            pitch = Math.toDegrees(Math.PI / 2 * sinp / Math.abs(sinp)); // use 90 degrees if out of range
        else
            pitch = Math.toDegrees(Math.asin(sinp));

        // yaw (z-axis rotation)
        double siny_cosp = 2 * (q[3] * q[2] + q[0] * q[1]);
        double cosy_cosp = 1 - 2 * (q[1] * q[1] + q[2] * q[2]);
        yaw = Math.toDegrees(Math.atan2(siny_cosp, cosy_cosp));

        roll += 180;
        yaw += 180;
        pitch += 180;

        return new float[]{(float) roll, (float) yaw, (float) pitch};
    }

    public static float extractThetaEuler(float[] vec){
        return (float) ((Math.atan2(vec[0], vec[2]) + PI) / PI * 180);
    }

}
