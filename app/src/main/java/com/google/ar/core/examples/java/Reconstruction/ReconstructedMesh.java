package com.google.ar.core.examples.java.Reconstruction;

import com.google.ar.core.examples.java.Utils.IO;
import com.google.ar.core.examples.java.common.samplerender.Mesh;
import com.google.ar.core.examples.java.common.samplerender.SampleRender;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReconstructedMesh {
    public List<float[]> vertices;
    public List<int[]> triangles;
    public List<float[]> vertexNormals;
    private static final ReconstructedMesh instance = new ReconstructedMesh();

    ReconstructedMesh(){
        vertices = new ArrayList<>();
        triangles = new ArrayList<>();
        vertexNormals = new ArrayList<>();
    }

    public static ReconstructedMesh getInstance() {
        return instance;
    }

    public void reset(){
        vertices.clear();
        triangles.clear();
        vertexNormals.clear();
    }

    public void transferToModel(SampleRender render, Mesh object){
        float[] listVertices = new float[vertices.size() * 3];
        float[] listNormals = new float[vertexNormals.size() * 3];

//        float[] vertexNormals = new float[vertices.size() * 3];
        int[] listIndices = new int[triangles.size() * 3];

        for(int i = 0; i < vertices.size(); i++){
            float[] vertex = vertices.get(i);
            listVertices[3*i] = vertex[0];
            listVertices[3*i + 1] = vertex[1];
            listVertices[3*i + 2] = vertex[2];
        }

        for(int i = 0; i < vertexNormals.size(); i++){
            float[] normal = vertexNormals.get(i);
            listNormals[3 * i] = normal[0];
            listNormals[3 * i + 1] = normal[1];
            listNormals[3 * i + 2] = normal[2];
        }

        for(int i = 0; i < triangles.size(); i++){
            int[] tri = triangles.get(i);
            listIndices[3*i] = tri[0];
            listIndices[3*i + 1] = tri[1];
            listIndices[3*i + 2] = tri[2];
        }


        object.makeBuffer(render, listVertices, listNormals, listIndices);
    }

    public void transferToModelWithoutNormal(SampleRender render, Mesh object){
        float[] listVertices = new float[vertices.size() * 3];

//        float[] vertexNormals = new float[vertices.size() * 3];
        int[] listIndices = new int[triangles.size() * 3];

        for(int i = 0; i < vertices.size(); i++){
            float[] vertex = vertices.get(i);
            listVertices[3*i] = vertex[0];
            listVertices[3*i + 1] = vertex[1];
            listVertices[3*i + 2] = vertex[2];
        }

        for(int i = 0; i < triangles.size(); i++){
            int[] tri = triangles.get(i);
            listIndices[3*i] = tri[0];
            listIndices[3*i + 1] = tri[1];
            listIndices[3*i + 2] = tri[2];
        }


        object.makeBuffer(render, listVertices, listIndices);
    }

    public void saveToOBJ(File outputFile){

        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));

            // Write stats
            bos.write(
                    String.format("# %d vertices, %d triangles\n",
                            vertices.size(), triangles.size()).getBytes()
            );
            bos.write("\n".getBytes());

            // vertices
            for (float[] v : vertices)
            {
                bos.write(
                        String.format("v %f %f %f\n", v[0], v[1], v[2]).getBytes()
                );
            }


            // vertex normals
            bos.write("\n".getBytes());
            for (float[] vn : vertexNormals)
            {
                bos.write(
                        String.format("vn %f %f %f\n", vn[0], vn[1], vn[2]).getBytes()
                );
            }

            // triangles (1-based)
            bos.write("\n".getBytes());
            for (int[]t : triangles)
            {
                bos.write(
                        String.format("f %d//%d %d//%d %d//%d\n",
                                t[0] + 1, t[0] + 1,
                                t[1] + 1, t[1] + 1,
                                t[2] + 1, t[2] + 1
                        ).getBytes()
                );
            }

            bos.flush();
            bos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
