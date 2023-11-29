package com.google.ar.core.examples.java.Reconstruction;

public class Cube {

    // Cube has 8 vertices. Each vertex can have positive or negative sign.
    // 2^8 = 256 possible configurations mapped to intersected edges in each case.
    // The 12 edges are numbered as 1, 2, 4, ..., 2048 and are stored as a 12-bit bitstring for each configuration.
	final int signConfigToIntersectedEdges[] = new int[]{
        0x0  , 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
                0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
                0x190, 0x99 , 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
                0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
                0x230, 0x339, 0x33 , 0x13a, 0x636, 0x73f, 0x435, 0x53c,
                0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
                0x3a0, 0x2a9, 0x1a3, 0xaa , 0x7a6, 0x6af, 0x5a5, 0x4ac,
                0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
                0x460, 0x569, 0x663, 0x76a, 0x66 , 0x16f, 0x265, 0x36c,
                0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
                0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0xff , 0x3f5, 0x2fc,
                0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
                0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x55 , 0x15c,
                0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
                0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0xcc ,
                0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
                0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
                0xcc , 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
                0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
                0x15c, 0x55 , 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
                0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
                0x2fc, 0x3f5, 0xff , 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
                0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
                0x36c, 0x265, 0x16f, 0x66 , 0x76a, 0x663, 0x569, 0x460,
                0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
                0x4ac, 0x5a5, 0x6af, 0x7a6, 0xaa , 0x1a3, 0x2a9, 0x3a0,
                0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
                0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x33 , 0x339, 0x230,
                0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
                0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x99 , 0x190,
                0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
                0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x0 };

    class Edge
    {
        public int edgeFlag; // flag: 1, 2, 4, ... 2048
        public int vert0; // 0-7
        public int vert1; // 0-7
        Edge(int edgeFlag, int vert0, int vert1){
            this.edgeFlag = edgeFlag;
            this.vert0 = vert0;
            this.vert1 = vert1;
        }
    };

	final Edge edges[] = new Edge[]
    {
        new Edge(1, 0, 1), // edge 0
        new Edge( 2, 1, 2 ), // edge 1
        new Edge( 4, 2, 3 ), // ...
        new Edge( 8, 3, 0 ),
        new Edge( 16, 4, 5 ),
        new Edge( 32, 5, 6 ),
        new Edge( 64, 6, 7 ),
        new Edge( 128, 7, 4 ),
        new Edge( 256, 0, 4 ),
        new Edge( 512, 1, 5 ),
        new Edge( 1024, 2, 6 ),
        new Edge( 2048, 3, 7 ) // edge 11
    };

    public float[][] listVertices = new float[8][3];
    private int[] vertexInfo = new int[8];

    public int SignConfig(){
        int edgeIndex = 0;

        for (int i = 0; i < 8; ++i)
        {
            if (vertexInfo[i] == 0)
            {
                edgeIndex |= 1 << i;
            }
            else {
                edgeIndex |= edgeIndex;
            }
        }

        return edgeIndex;
    }

    float[] getMidPoint(int i1, int i2){
        float x = (listVertices[i1][0] + listVertices[i2][0]) / 2;
        float y = (listVertices[i1][1] + listVertices[i2][1]) / 2;
        float z = (listVertices[i1][2] + listVertices[i2][2]) / 2;

        return new float[]{x, y, z};
    }

    Cube(float[] lbfPoint, float[] size, float[] halfSize, int[] vertexInfo){

        for (int i = 0; i < 8; i++) this.vertexInfo[i] = vertexInfo[i];

        listVertices[0][0] = lbfPoint[0] + halfSize[0];
        listVertices[0][1] = lbfPoint[1] + halfSize[1];
        listVertices[0][2] = lbfPoint[2] + halfSize[2];

        listVertices[1][0] = listVertices[0][0] + size[0];
        listVertices[1][1] = listVertices[0][1];
        listVertices[1][2] = listVertices[0][2];

        listVertices[2][0] = listVertices[0][0] + size[0];
        listVertices[2][1] = listVertices[0][1];
        listVertices[2][2] = listVertices[0][2] + size[2];

        listVertices[3][0] = listVertices[0][0];
        listVertices[3][1] = listVertices[0][1];
        listVertices[3][2] = listVertices[0][2] + size[2];

        listVertices[4][0] = listVertices[0][0];
        listVertices[4][1] = listVertices[0][1] + size[1];
        listVertices[4][2] = listVertices[0][2];

        listVertices[5][0] = listVertices[0][0] + size[0];
        listVertices[5][1] = listVertices[0][1] + size[1];
        listVertices[5][2] = listVertices[0][2];

        listVertices[6][0] = listVertices[0][0] + size[0];
        listVertices[6][1] = listVertices[0][1] + size[1];
        listVertices[6][2] = listVertices[0][2] + size[2];

        listVertices[7][0] = listVertices[0][0];
        listVertices[7][1] = listVertices[0][1] + size[1];
        listVertices[7][2] = listVertices[0][2] + size[2];
    }

    public IntersectInfo Intersect(){
        // idea:
        // from signs at 8 corners of cube a sign configuration (256 possible ones) is computed
        // this configuration can be used to index into a table that tells which of the 12 edges are intersected
        // find vertices adjacent to edges and interpolate cut vertex and store it in IntersectionInfo object

        IntersectInfo intersect = new IntersectInfo();
        intersect.signConfig = SignConfig();

        for (int e = 0; e < 12; ++e)
        {

            if ((signConfigToIntersectedEdges[intersect.signConfig] & edges[e].edgeFlag) != 0)
            {
                int v0 = edges[e].vert0;
                int v1 = edges[e].vert1;
                float[] vert = getMidPoint(v0, v1);
                intersect.edgeVertIndices[e][0] = vert[0];
                intersect.edgeVertIndices[e][1] = vert[1];
                intersect.edgeVertIndices[e][2] = vert[2];
            }
        }

        return intersect;
    }
}


;