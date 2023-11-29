package com.google.ar.core.examples.java.Reconstruction;

public class MeshReconstructionEngine {
    /*
	  Y
	  |
	  |
	 4#---------#5
	 /		   / |
   7#---------#6 |
	|		  |  |1-------------X
	|    	  | /
   3#---------#2
   /
  /
 Z
*/
    public static void createVertexInfo(
            int[] info,
            float[] sdf,
            int ix, int iy, int iz,
            int Nx, int Ny, int Nz)
    {
//        float threshold = 0.0000001f;
        float threshold = 0.5f;

        int voxelIdx = (iz * Nx + ix) * Ny + iy;

        // Vetex 0
        info[0] = sdf[voxelIdx] > threshold ? 1 : 0;

        // Vertex 1
        if (ix == Nx - 1) {
            info[1] = 0;
        }
        else {
            voxelIdx = (iz * Nx + ix + 1) * Ny + iy;
            info[1] = sdf[voxelIdx] > threshold ? 1 : 0;
        }

        // Vertex 2
        if ((ix == Nx - 1) || (iz == Nz - 1)) {
            info[2] = 0;
        }
        else {
            voxelIdx = ((iz + 1) * Nx + ix + 1) * Ny + iy;
            info[2] = sdf[voxelIdx] > threshold ? 1 : 0;
        }

        // Vertex 3
        if (iz == Nz - 1) {
            info[3] = 0;
        }
        else {
            voxelIdx = ((iz + 1) * Nx + ix) * Ny + iy;
            info[3] = sdf[voxelIdx] > threshold ? 1 : 0;
        }

        // Vetex 4
        if (iy == Ny - 1) {
            info[4] = 0;
        }
        else {
            voxelIdx = (iz * Nx + ix) * Ny + iy + 1;
            info[4] = sdf[voxelIdx] > threshold ? 1 : 0;
        }

        // Vertex 5
        if ((iy == Ny - 1) || (ix == Nx - 1)) {
            info[5] = 0;
        }
        else {
            voxelIdx = (iz * Nx + ix + 1) * Ny + iy + 1;
            info[5] = sdf[voxelIdx] > threshold ? 1 : 0;
        }

        // Vertex 6
        if ((iy == Ny - 1) || (ix == Nx - 1) || (iz == Nz - 1)) {
            info[6] = 0;
        }
        else {
            voxelIdx = ((iz + 1) * Nx + ix + 1) * Ny + iy + 1;
            info[6] = sdf[voxelIdx] > threshold ? 1 : 0;
        }

        // Vertex 7
        if ((iy == Ny - 1) || (iz == Nz - 1)) {
            info[7] = 0;
        }
        else {
            voxelIdx = ((iz + 1) * Nx + ix) * Ny + iy + 1;
            info[7] = sdf[voxelIdx] > threshold ? 1 : 0;
        }
    }

    public static void marchingCube(){
        float[] sdf = Volume.getInstance().getPu();
        float[] lbf = Volume.getInstance().getLBF();
        float[] rtn = Volume.getInstance().getRTN();
        int Nx = Volume.getInstance().getNx();
        int Ny = Volume.getInstance().getNy();
        int Nz = Volume.getInstance().getNz();

        ReconstructedMesh.getInstance().reset();
        float stepX = (rtn[0] - lbf[0]) / (float)Nx;
        float stepY = (rtn[1] - lbf[1]) / (float)Ny;
        float stepZ = (rtn[2] - lbf[2]) / (float)Nz;

        float[] halfSize = new float[]{stepX * 0.5f, stepY * 0.5f, stepZ * 0.5f};
        float[] size = new float[] {stepX, stepY, stepZ};

        float x, y, z;

	/*
	    left bottom far has index 0.
	    right top near has index 6.
		 4#---------#5
		 /		   / |
	   7#---------#6 |
		|		  |  |1
		|    	  | /
	   3#---------#2
	*/

        for (int iz = 0; iz < Nz; iz++) for (int ix = 0; ix < Nx; ix++) for (int iy = 0; iy < Ny; iy++)
        {
            x = lbf[0] + ix * stepX + stepX * 0.5f;
            y = lbf[1] + iy * stepY + stepY * 0.5f;
            z = lbf[2] + iz * stepZ + stepZ * 0.5f;
            int[] vertexInfo = new int[8];

            float[] lbfPoint = new float[]{x, y, z};
            createVertexInfo(vertexInfo, sdf, ix, iy, iz, Nx, Ny, Nz);

            Cube cube = new Cube(lbfPoint, size, halfSize, vertexInfo);

            IntersectInfo intersect = cube.Intersect();
            Triangulate.Triangulate(intersect, ReconstructedMesh.getInstance());
        }
    }


        /*
            left bottom far has index 0.
            right top near has index 6.
             4#----------#5
             / | 	    / |
            7#---------#6 |
            |  0-------|--|1
            | /   	   | /
            3#---------#2
    */
    final static int CubeFaces[][] = new int[][] {
            new int[]{6, 7, 3},
            new int[]{6, 3, 2},

            new int[]{3, 0, 1},
            new int[]{3, 1, 2},

            new int[]{1, 5, 6},
            new int[]{1, 6, 2},

            new int[]{5, 4, 7},
            new int[]{5, 7, 6},

            new int[]{4, 5, 1},
            new int[]{4, 1, 0},

            new int[]{7, 4, 0},
            new int[]{7, 0, 3},

    };

    public static void voxelize() {
        float[] sdf = Volume.getInstance().getPu();
        float[] lbf = Volume.getInstance().getLBF();
        float[] rtn = Volume.getInstance().getRTN();
        int Nx = Volume.getInstance().getNx();
        int Ny = Volume.getInstance().getNy();
        int Nz = Volume.getInstance().getNz();

        ReconstructedMesh.getInstance().reset();
        float stepX = (rtn[0] - lbf[0]) / (float) Nx;
        float stepY = (rtn[1] - lbf[1]) / (float) Ny;
        float stepZ = (rtn[2] - lbf[2]) / (float) Nz;

        float x, y, z;

	/*
	    left bottom far has index 0.
        right top near has index 6.

          | Y
          |
		 4#---------#5
		 /|		   / |
	   7#---------#6 |
		| 0-------|--|1------ X
		|/    	  | /
	   3#---------#2
	   /
	  / Z

	*/

        int Nx1 = (Nx + 1);
        int Ny1 = (Ny + 1);
        int Nz1 = (Nz + 1);

        for (int iz = 0; iz < Nz1; iz++) {
            for (int ix = 0; ix < Nx1; ix++) {
                for (int iy = 0; iy < Ny1; iy++) {
                    x = lbf[0] + ix * stepX;
                    y = lbf[1] + iy * stepY;
                    z = lbf[2] + iz * stepZ;
                    ReconstructedMesh.getInstance().vertices.add(new float[]{x, y, z});
                    float[] normal = estimateNormal(sdf, ix, iy, iz, Nx, Ny, Nz);
//                    normalize(normal);
                    ReconstructedMesh.getInstance().vertexNormals.add(normal);

                }
            }
        }

        int[] listFaceIndices = new int[8];

        for (int iz = 0; iz < Nz; iz++) {
            for (int ix = 0; ix < Nx; ix++) {
                for (int iy = 0; iy < Ny; iy++) {
                    int voxelIdx = (iz * Nx + ix) * Ny + iy;

                    if (sdf[voxelIdx] == 0) continue;

                    listFaceIndices[0] = (iz * Nx1 + ix) * Ny1 + iy;
                    listFaceIndices[1] = (iz * Nx1 + ix + 1) * Ny1 + iy;
                    listFaceIndices[2] = ((iz + 1) * Nx1 + ix + 1) * Ny1 + iy;
                    listFaceIndices[3] = ((iz + 1) * Nx1 + ix) * Ny1 + iy;

                    listFaceIndices[4] = (iz * Nx1 + ix) * Ny1 + iy + 1;
                    listFaceIndices[5] = (iz * Nx1 + ix + 1) * Ny1 + iy + 1;
                    listFaceIndices[6] = ((iz + 1) * Nx1 + ix + 1) * Ny1 + iy + 1;
                    listFaceIndices[7] = ((iz + 1) * Nx1 + ix) * Ny1 + iy + 1;

                    for (int[] face : CubeFaces) {
                        int i0 = listFaceIndices[face[0]];
                        int i1 = listFaceIndices[face[1]];
                        int i2 = listFaceIndices[face[2]];
                        ReconstructedMesh.getInstance().triangles.add(new int[]{i0, i1, i2});

//                        float[] v0 = ReconstructMesh.getInstance().vertices.get(i0);
//                        float[] v1 = ReconstructMesh.getInstance().vertices.get(i1);
//                        float[] v2 = ReconstructMesh.getInstance().vertices.get(i2);
//
//                        float[] normal = crossProduct(subtract(v0 , v1), subtract(v0, v2));
//                        float[] normal = new float[]{1f, 0f, 0f};
//                        normalize(normal);

//                        ReconstructMesh.getInstance().vertexNormals.add(normal);
//                        ReconstructMesh.getInstance().vertexNormals.add(normal);
//                        ReconstructMesh.getInstance().vertexNormals.add(normal);

                    }
                }
            }
        }
    }

    private static float[] estimateNormal(
            final float[] sdf,
            int vertIdx, int vertIdy, int vertIdz,
            int Nx, int Ny, int Nz){
        int TLU, TRU, BRU, BLU;
        int TLD, TRD, BRD, BLD;
        if ((vertIdz - 1) >= 0 &
            (vertIdx - 1) >= 0 &
            (vertIdy - 1 + 1) < Ny)
            TLU = (vertIdz - 1) * Nx * Ny + (vertIdx - 1) * Ny + vertIdy;
        else
            TLU = -1;

        if ((vertIdz - 1) >= 0 &
            (vertIdx - 1 + 1) < Nx &
            (vertIdy - 1 + 1) < Ny)
            TRU = (vertIdz - 1) * Nx * Ny + vertIdx * Ny + vertIdy;
        else
            TRU = -1;

        if ((vertIdz - 1 + 1) < Nz &
            (vertIdx - 1) >= 0 &
            (vertIdy - 1 + 1) < Ny)
            BLU = vertIdz * Nx * Ny + (vertIdx - 1) * Ny + vertIdy;
        else
            BLU = -1;

        if ((vertIdz - 1 + 1) < Nz &
            (vertIdx - 1 + 1) < Nx &
            (vertIdy - 1 + 1) < Ny)
            BRU = vertIdz * Nx * Ny + vertIdx * Ny + vertIdy;
        else
            BRU = -1;


        if ((vertIdz - 1) >= 0 &
                (vertIdx - 1) >= 0 &
                (vertIdy - 1) >= 0)
            TLD = (vertIdz - 1) * Nx * Ny + (vertIdx - 1) * Ny + vertIdy - 1;
        else
            TLD = -1;

        if ((vertIdz - 1) >= 0 &
                (vertIdx - 1 + 1) < Nx &
                (vertIdy - 1) >= 0)
            TRD = (vertIdz - 1) * Nx * Ny + vertIdx * Ny + vertIdy - 1;
        else
            TRD = -1;

        if ((vertIdz - 1 + 1) < Nz &
                (vertIdx - 1) >= 0 &
                (vertIdy - 1) >= 0)
            BLD = vertIdz * Nx * Ny + (vertIdx - 1) * Ny + vertIdy - 1;
        else
            BLD = -1;

        if ((vertIdz - 1 + 1) < Nz &
                (vertIdx - 1 + 1) < Nx &
                (vertIdy - 1) >= 0)
            BRD = vertIdz * Nx * Ny + vertIdx * Ny + vertIdy - 1;
        else
            BRD = -1;

        float vecX = 0, vecY = 0, vecZ = 0;
        if (TLU != -1 && sdf[TLU] != 0){
            vecX += 1; vecZ += 1; vecY -= 1;
        }
        if (TRU != -1 && sdf[TRU] != 0){
            vecX -= 1; vecZ += 1; vecY -= 1;
        }
        if (BLU != -1 && sdf[BLU] != 0){
            vecX += 1; vecZ -= 1; vecY -= 1;
        }
        if (BRU != -1 && sdf[BRU] != 0){
            vecX -= 1; vecZ -= 1; vecY -= 1;
        }
        // ---
        if (TLD != -1 && sdf[TLD] != 0){
            vecX += 1; vecZ += 1; vecY += 1;
        }
        if (TRD != -1 && sdf[TRD] != 0){
            vecX -= 1; vecZ += 1; vecY += 1;
        }
        if (BLD != -1 && sdf[BLD] != 0){
            vecX += 1; vecZ -= 1; vecY += 1;
        }
        if (BRD != -1 && sdf[BRD] != 0){
            vecX -= 1; vecZ -= 1; vecY += 1;
        }
        return new float[]{vecX, vecY, vecZ};
    }
}
