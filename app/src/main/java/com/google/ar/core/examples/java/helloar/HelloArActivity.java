/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;


import android.content.DialogInterface;
import android.content.res.Resources;

import android.media.Image;
import android.opengl.GLES30;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Config.InstantPlacementMode;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.Reconstruction.CMF;
import com.google.ar.core.examples.java.Reconstruction.Histogram;
import com.google.ar.core.examples.java.Reconstruction.MeshReconstructionEngine;
import com.google.ar.core.examples.java.Reconstruction.PieHistogram;
import com.google.ar.core.examples.java.Reconstruction.ReconstructedMesh;
import com.google.ar.core.examples.java.Reconstruction.ShapeOptimization;
import com.google.ar.core.examples.java.Reconstruction.Volume;
import com.google.ar.core.examples.java.Utils.IO;
import com.google.ar.core.examples.java.Utils.MyMath;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DepthSettings;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.InstantPlacementSettings;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TapHelper;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;
import com.google.ar.core.examples.java.common.samplerender.Framebuffer;
import com.google.ar.core.examples.java.common.samplerender.Mesh;
import com.google.ar.core.examples.java.common.samplerender.SampleRender;
import com.google.ar.core.examples.java.common.samplerender.Shader;
import com.google.ar.core.examples.java.common.samplerender.Texture;
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.PlaneRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.SpecularCubemapFilter;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3D model.
 */
public class HelloArActivity extends AppCompatActivity implements SampleRender.Renderer {

    private static final String TAG = HelloArActivity.class.getSimpleName();

    private static final String HISTOGRAM_TAG = Histogram.class.getSimpleName();
    private static final String CMF_TAG = CMF.class.getSimpleName();
    private static final String SHAPE_OPTIMIZE_TAG = ShapeOptimization.class.getSimpleName();


    private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";
    private static final String WAITING_FOR_TAP_MESSAGE = "Tap on a surface to place an object.";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100f;

    private static final int CUBEMAP_RESOLUTION = 16;
    private static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;

    //-----------------------------
    private Button initButton;
    private Button runButton;
    private Button stopButton;
    private Button saveButton;



    private boolean trigger = false;
    private int width, height;
    private int[] colorImage;
    byte[] maskImage;
    MyRunnable threadReconstruction;
    Histogram histogram;

    //  final int Nx = 40, Ny = 40, Nz = 40;
    int Nx, Ny, Nz;
    SeekBar seekBarSize;
    TextView textViewSize;
    float minSz = 0.1f;
    float maxSz = 1f;
    float sz = 0.4f;

    // Left Bottom Far Coordinate
    final float[] LBF = new float[]{-sz / 2, 0, -sz / 2};
    // Right Top Near Coordinate
    final float[] RTN = new float[]{sz / 2, sz, sz / 2};

    int countingUpdate = 0;
    int numKeyFrames = 8;
    int countingKeyFrames = 0;
    float[] cameraTranslation;
    float[] cameraQuaternion;

    SeekBar seekBarWeight;
    TextView textViewWeight;
    float weight = 58;
    float minWeight = 5;
    float maxWeight = 300;

    SeekBar seekBarRes;
    TextView textViewRes;
    int res = 40; // Resolution

    Switch renderSwith;
    boolean isVoxelRendering = false;

    Switch histogramSwith;
    boolean isMultipleHistograms = false;
    //    MultipleHistogram multipleHistogram;
    PieHistogram multipleHistogram;
    int noHists = 10;


    //-----------------------------

    private boolean installRequested;

    private Session session;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper displayRotationHelper;
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
    private TapHelper tapHelper;
    private SampleRender render;

    private PlaneRenderer planeRenderer;
    private BackgroundRenderer backgroundRenderer;
    private Framebuffer virtualSceneFramebuffer;
    private boolean hasSetTextureNames = false;

    private final DepthSettings depthSettings = new DepthSettings();
    private boolean[] depthSettingsMenuDialogCheckboxes = new boolean[2];

    private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
    private boolean[] instantPlacementSettingsMenuDialogCheckboxes = new boolean[1];
    // Assumed distance from the device camera to the surface on which user will try to place objects.
    // This value affects the apparent scale of objects while the tracking method of the
    // Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
    // Values in the [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower
    // values for AR experiences where users are expected to place objects on surfaces close to the
    // camera. Use larger values for experiences where the user will likely be standing and trying to
    // place an object on the ground or floor in front of them.
    private static final float APPROXIMATE_DISTANCE_METERS = 2.0f;

    // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
    // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
    private long lastPointCloudTimestamp = 0;

    // Virtual object (ARCore pawn)
    private Mesh virtualObjectMesh;
    private Shader virtualObjectShader;
    private final ArrayList<Anchor> anchors = new ArrayList<>();

    // Environmental HDR
    private Texture dfgTexture;
    private SpecularCubemapFilter cubemapFilter;

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16]; // view x model
    private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model
    private final float[] sphericalHarmonicsCoefficients = new float[9 * 3];
    private final float[] viewInverseMatrix = new float[16];
    private final float[] worldLightDirection = {0.0f, 0.0f, 0.0f, 0.0f};
    private final float[] viewLightDirection = new float[4]; // view x world light direction

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        // Set up touch listener.
        tapHelper = new TapHelper(/*context=*/ this);
        surfaceView.setOnTouchListener(tapHelper);

        // Set up renderer.
        render = new SampleRender(surfaceView, this, getAssets());

        installRequested = false;

        depthSettings.onCreate(this);
        instantPlacementSettings.onCreate(this);


        // --------------------------------------------------------
        Nx = res;
        Ny = res;
        Nz = res;

        seekBarWeight = findViewById(R.id.seekBarWeight);
        textViewWeight = findViewById(R.id.textViewWeight);
        seekBarWeight.setProgress((int) ((weight - minWeight) / (maxWeight - minWeight) * 100));
        textViewWeight.setText("Weight: " + weight + "/ " + maxWeight);
        seekBarWeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                weight = (float) i / 100 * (maxWeight - minWeight) + minWeight;
                textViewWeight.setText("Weight: " + weight + "/ " + maxWeight);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBarSize = findViewById(R.id.seekBarSize);
        textViewSize = findViewById(R.id.textViewSize);
        seekBarSize.setProgress((int) ((sz - minSz) / (maxSz - minSz) * 100));
        textViewSize.setText("Size: " + sz);
        seekBarSize.setEnabled(false);
        seekBarSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sz = (float) i / 100 * (maxSz - minSz) + minSz;

                Volume.getInstance().setSize(sz);

                if (isVoxelRendering){
                    MeshReconstructionEngine.voxelize();
                }
                else {
                    MeshReconstructionEngine.marchingCube();
                }

                enableTransfer();

                textViewSize.setText("Size: " + sz);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        seekBarRes = findViewById(R.id.seekBarRes);
        textViewRes = findViewById(R.id.textViewRes);
        seekBarRes.setProgress(res);
        textViewRes.setText("Resolution: " + res + " x " + res + " x " + res);
        seekBarRes.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                res = i;
                Nx = res;
                Ny = res;
                Nz = res;
                textViewRes.setText("Resolution: " + res + " x " + res + " x " + res);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        renderSwith = findViewById(R.id.switchRendering);
        renderSwith.setChecked(isVoxelRendering);
        renderSwith.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isVoxelRendering = b;
            }
        });

        histogramSwith = findViewById(R.id.histogramSwith);
        histogramSwith.setChecked(isMultipleHistograms);
        histogramSwith.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isMultipleHistograms = b;
            }
        });

        histogram = new Histogram(32);
//        multipleHistogram = new MultipleHistogram(10, 32);
        multipleHistogram = new PieHistogram(noHists, 32);


        threadReconstruction = new MyRunnable() {
            @Override
            public void run() {
                int updateHist = 3;
                countingUpdate = 1;
                int iters = 1;

                List<int[]> listColorImages = new ArrayList<>();
                List<byte[]> listMaskImages = new ArrayList<>();
                List<float[]> listPoses = new ArrayList<>();
                List<float[]> listQuaternions = new ArrayList<>();


                Volume.getInstance().setFirstInit(true);
                while (running.get()) {
                    if (colorImage == null | maskImage == null) continue;
                    if (countingUpdate % updateHist != 0) continue;

                    listColorImages.add(colorImage.clone());
                    listMaskImages.add(maskImage.clone());
                    listPoses.add(modelViewProjectionMatrix.clone());
                    listQuaternions.add(cameraQuaternion.clone());

                    if (isMultipleHistograms == false) {
                        if (Volume.getInstance().isFirstInit()) {
                            if (listColorImages.size() == 10) {

                                histogram.update(listColorImages, listMaskImages);
                                ShapeOptimization.updateProbabilityBaseOnHistogram(
                                        histogram,
                                        listColorImages,
                                        listMaskImages,
                                        listPoses,
                                        width, height,
                                        weight
                                );
                                CMF.solve(0.1f,
                                        0.11f,
                                        0.35f,
                                        1e-5f,
                                        iters,
                                        Volume.getInstance().getPu());


                                if (isVoxelRendering) {
                                    MeshReconstructionEngine.voxelize();
                                }
                                else {
                                    MeshReconstructionEngine.marchingCube();
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

                                histogram.update(listColorImages, listMaskImages);

                                ShapeOptimization.updateProbabilityBaseOnHistogram(
                                        histogram,
                                        listColorImages,
                                        listMaskImages,
                                        listPoses,
                                        width, height,
                                        weight
                                );

                                CMF.solve(0.1f,
                                        0.11f,
                                        0.35f,
                                        1e-5f,
                                        iters,
                                        Volume.getInstance().getPu());

                                if (isVoxelRendering) {
                                    MeshReconstructionEngine.voxelize();
                                } else {
                                    MeshReconstructionEngine.marchingCube();
                                }

                                enableTransfer();
                                listColorImages.clear();
                                listMaskImages.clear();
                                listPoses.clear();
                            }
                        }
                    }
                    else {
                        if (Volume.getInstance().isFirstInit()) {
                            if (listColorImages.size() == 10) {
//                                multipleHistogram.InitMultipleHistograms(listColorImages, listMaskImages, listQuaternions);
                                multipleHistogram.InitPieHistograms(listColorImages, listMaskImages, listQuaternions);
                                ShapeOptimization.updateProbabilityBaseOnPieHistograms(
                                        multipleHistogram,
                                        listColorImages,
                                        listMaskImages,
                                        listPoses,
                                        listQuaternions,
                                        width, height,
                                        weight
                                );
                                CMF.solve(0.1f,
                                        0.11f,
                                        0.35f,
                                        1e-5f,
                                        iters,
                                        Volume.getInstance().getPu());


                                if (isVoxelRendering) {
                                    MeshReconstructionEngine.voxelize();
                                } else {
                                    MeshReconstructionEngine.marchingCube();
                                }

                                enableTransfer();
                                Volume.getInstance().setFirstInit(false);
                                listColorImages.clear();
                                listMaskImages.clear();
                                listPoses.clear();
                                listQuaternions.clear();
                            }
                        }
                        else {
                            if (listColorImages.size() == 5) {
                                multipleHistogram.update(listColorImages, listMaskImages, listQuaternions);

                                ShapeOptimization.updateProbabilityBaseOnPieHistograms(
                                        multipleHistogram,
                                        listColorImages,
                                        listMaskImages,
                                        listPoses,
                                        listQuaternions,
                                        width, height,
                                        weight
                                );

                                CMF.solve(0.1f,
                                        0.11f,
                                        0.35f,
                                        1e-5f,
                                        1,
                                        Volume.getInstance().getPu());

                                if (isVoxelRendering) {
                                    MeshReconstructionEngine.voxelize();
                                } else {
                                    MeshReconstructionEngine.marchingCube();
                                }

                                enableTransfer();
                                listColorImages.clear();
                                listMaskImages.clear();
                                listPoses.clear();
                                listQuaternions.clear();
                            }
                        }
                    }
                }
            }
        };

        ScheduledExecutorService serviceReconstruction = Executors.newSingleThreadScheduledExecutor();
        serviceReconstruction.scheduleAtFixedRate(threadReconstruction, 5, 5, TimeUnit.NANOSECONDS);


        initButton = findViewById(R.id.init_button);
        runButton = findViewById(R.id.run_button);
        stopButton = findViewById(R.id.stop_button);

        initButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.e("IMAGE W", String.valueOf(width));
//                Log.e("IMAGE H", String.valueOf(height));
                seekBarSize.setEnabled(true);
                seekBarRes.setEnabled(false);
                histogramSwith.setEnabled(false);
                Volume.getInstance().initVolumeSize(Nx, Ny, Nz);
                Volume.getInstance().initPu();

                Volume.getInstance().setSize(sz);

                if (isVoxelRendering){
                    MeshReconstructionEngine.voxelize();
                }
                else {
                    MeshReconstructionEngine.marchingCube();
                }

                enableTransfer();
            }
        });

        runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                seekBarSize.setEnabled(false);
                initButton.setEnabled(false);
                enableTransfer();

                Toast.makeText(getApplication(), "Run", Toast.LENGTH_SHORT)
                        .show();
                threadReconstruction.runMe();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplication(), "Stop", Toast.LENGTH_SHORT)
                        .show();
                threadReconstruction.stopMe();
            }
        });

        saveButton = findViewById(R.id.save_button);


        saveButton.setOnClickListener(new View.OnClickListener() {
            String modelName = "";

            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(HelloArActivity.this);
                builder.setTitle("Model Name");

                // Set up the input
                final EditText input = new EditText(HelloArActivity.this);

                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        modelName = input.getText().toString();

                        // Save model 3D
                        IO io = new IO(HelloArActivity.this);
                        File outputObj = io.getCustomFile(modelName + ".obj");
                        ReconstructedMesh.getInstance().saveToOBJ(outputObj);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });
    }


    @Override
    protected void onDestroy() {
        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            session.close();
            session = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                // Create the session.
                session = new Session(/* context= */ this);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            configureSession();
            // To record a live camera session for later playback, call
            // `session.startRecording(recorderConfig)` at anytime. To playback a previously recorded AR
            // session instead of using the live camera feed, call
            // `session.setPlaybackDataset(playbackDatasetPath)` before calling `session.resume()`. To
            // learn more about recording and playback, see:
            // https://developers.google.com/ar/develop/java/recording-and-playback
            session.resume();
        } catch (CameraNotAvailableException e) {
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
            session = null;
            return;
        }

        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            // Use toast instead of snackbar here since the activity will exit.
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onSurfaceCreated(SampleRender render) {
        // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
        // an IOException.
        try {
            planeRenderer = new PlaneRenderer(render);
            backgroundRenderer = new BackgroundRenderer(render);
            virtualSceneFramebuffer = new Framebuffer(render, /*width=*/ 1, /*height=*/ 1);


            virtualObjectMesh = Mesh.createFromAsset(render, "models/pawn.obj");

            virtualObjectShader =
                    Shader.createFromAssets(
                            render, "shaders/volume_phong.vert", "shaders/volume_phong.frag", /*defines=*/ null);

            float[] lightPos = new float[]{100f, 100.0f, 100.0f};
            virtualObjectShader.setVec3("light.position", lightPos);

            // light properties
            // note that all light colors are set at full intensity
            virtualObjectShader.setVec3("light.ambient", new float[]{1, 1, 1});
            virtualObjectShader.setVec3("light.diffuse", new float[]{1, 1, 1});
//            virtualObjectShader.setVec3("light.specular", new float[]{1, 1, 1});

            // material properties
            virtualObjectShader.setVec3("material.ambient", new float[]{1.0f/2, 0.5f/2, 0.3f/2});
            virtualObjectShader.setVec3("material.diffuse", new float[]{0.1f, 0.1f, 0.1f});
//            virtualObjectShader.setVec3("material.specular", new float[]{0.2f, 0.2f, 0.2f});
//            virtualObjectShader.setFloat("material.shininess", 1.2f);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read a required asset file", e);
            messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
        }
    }

    @Override
    public void onSurfaceChanged(SampleRender render, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        virtualSceneFramebuffer.resize(width, height);
        this.width = width;
        this.height = height;

        virtualObjectMesh.setSize(width, height);
    }

    @Override
    public void onDrawFrame(SampleRender render) {
        if (session == null) {
            return;
        }

        transferBufferToRenderer(render);

        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
            session.setCameraTextureNames(
                    new int[]{backgroundRenderer.getCameraColorTexture().getTextureId()});
            hasSetTextureNames = true;
        }


        // -- Update per-frame state

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        Frame frame;
        try {
            frame = session.update();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available during onDrawFrame", e);
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
            return;
        }
        Camera camera = frame.getCamera();



        // Update BackgroundRenderer state to match the depth settings.
        try {
            backgroundRenderer.setUseDepthVisualization(
                    render, depthSettings.depthColorVisualizationEnabled());
            backgroundRenderer.setUseOcclusion(render, depthSettings.useDepthForOcclusion());
        } catch (IOException e) {
            Log.e(TAG, "Failed to read a required asset file", e);
            messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
            return;
        }
        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundRenderer.updateDisplayGeometry(frame);

        if (camera.getTrackingState() == TrackingState.TRACKING
                && (depthSettings.useDepthForOcclusion()
                || depthSettings.depthColorVisualizationEnabled())) {
            try (Image depthImage = frame.acquireDepthImage()) {
                backgroundRenderer.updateCameraDepthTexture(depthImage);
            } catch (NotYetAvailableException e) {
                // This normally means that depth data is not available yet. This is normal so we will not
                // spam the logcat with this.
            }
        }


        // Handle one tap per frame.
        handleTap(frame, camera);

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

        // Show a message based on whether tracking has failed, if planes are detected, and if the user
        // has placed any objects.
        String message = null;
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
                message = SEARCHING_PLANE_MESSAGE;
            } else {
                message = TrackingStateHelper.getTrackingFailureReasonString(camera);
            }
        } else if (hasTrackingPlane()) {
            if (anchors.isEmpty()) {
                message = WAITING_FOR_TAP_MESSAGE;
            }
        } else {
            message = SEARCHING_PLANE_MESSAGE;
        }
        if (message == null) {
            messageSnackbarHelper.hide(this);
        } else {
            messageSnackbarHelper.showMessage(this, message);
        }

        // -- Draw background

        if (frame.getTimestamp() != 0) {
            // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
            // drawing possible leftover data from previous sessions if the texture is reused.
            backgroundRenderer.setSize(width, height);
            backgroundRenderer.drawBackground(render);


            // Download Color Image (Camera Image) immediately after rendering
            colorImage = backgroundRenderer.downloadColorImage();

        }

        // If not tracking, don't draw 3D objects.
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            return;
        }

        // -- Draw non-occluded virtual objects (planes, point cloud)

        // Get projection matrix.
        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);

        // Get camera matrix and draw.
        camera.getViewMatrix(viewMatrix, 0);

//    camera.getDisplayOrientedPose().getRotationQuaternion();

        // -- Draw occluded virtual objects

        // Update lighting parameters in the shader

        // Visualize anchors created by touch.
        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);
        for (Anchor anchor : anchors) {
            if (anchor.getTrackingState() != TrackingState.TRACKING) {
                continue;
            }

            // Get the current pose of an Anchor in world space. The Anchor pose is updated
            // during calls to session.update() as ARCore refines its estimate of the world.
            anchor.getPose().toMatrix(modelMatrix, 0);

            // Calculate model/view/projection matrices
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);


            // Update shader properties and draw
            virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
            virtualObjectShader.setMat4("u_ModelView", modelViewMatrix);
//            camera.getPose().getTranslation(viewPos, 0);
//
//            virtualObjectShader.setVec3("viewPos", viewPos);
//      virtualObjectShader.setVec3("camera.viewPos", new float [] {10, 10, 10});

            render.draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer);

            maskImage = virtualObjectMesh.downloadMaskImage();

            break;
        }


        // Compose the virtual scene with the background.
        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR);

        countingKeyFrames++;
        if (countingUpdate > 0 &
                countingKeyFrames % numKeyFrames == 0) {
            countingUpdate++;
        }

        cameraQuaternion = camera.getPose().getRotationQuaternion();
        cameraTranslation = camera.getPose().getTranslation();
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private void handleTap(Frame frame, Camera camera) {
        MotionEvent tap = tapHelper.poll();
        if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
            List<HitResult> hitResultList;
            if (instantPlacementSettings.isInstantPlacementEnabled()) {
                hitResultList =
                        frame.hitTestInstantPlacement(tap.getX(), tap.getY(), APPROXIMATE_DISTANCE_METERS);
            } else {
                hitResultList = frame.hitTest(tap);
            }
            for (HitResult hit : hitResultList) {
                // If any plane, Oriented Point, or Instant Placement Point was hit, create an anchor.
                Trackable trackable = hit.getTrackable();
                // If a plane was hit, check that it was hit inside the plane polygon.
                // DepthPoints are only returned if Config.DepthMode is set to AUTOMATIC.
                if ((trackable instanceof Plane
                        && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                        && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                        || (trackable instanceof Point
                        && ((Point) trackable).getOrientationMode()
                        == OrientationMode.ESTIMATED_SURFACE_NORMAL)
                        || (trackable instanceof InstantPlacementPoint)
                        || (trackable instanceof DepthPoint)) {
                    // Cap the number of objects created. This avoids overloading both the
                    // rendering system and ARCore.
                    if (anchors.size() >= 20) {
                        anchors.get(0).detach();
                        anchors.remove(0);
                    }

                    // Adding an Anchor tells ARCore that it should track this position in
                    // space. This anchor is created on the Plane to place the 3D model
                    // in the correct position relative both to the world and to the plane.
                    anchors.add(hit.createAnchor());
                    // For devices that support the Depth API, shows a dialog to suggest enabling
                    // depth-based occlusion. This dialog needs to be spawned on the UI thread.
                    this.runOnUiThread(this::showOcclusionDialogIfNeeded);

                    // Hits are sorted by depth. Consider only closest hit on a plane, Oriented Point, or
                    // Instant Placement Point.
                    break;
                }
            }
        }
    }

    /**
     * Shows a pop-up dialog on the first call, determining whether the user wants to enable
     * depth-based occlusion. The result of this dialog can be retrieved with useDepthForOcclusion().
     */
    private void showOcclusionDialogIfNeeded() {
        boolean isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
        if (!depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
            return; // Don't need to show dialog.
        }

        // Asks the user whether they want to use depth-based occlusion.
        new AlertDialog.Builder(this)
                .setTitle(R.string.options_title_with_depth)
                .setMessage(R.string.depth_use_explanation)
                .setPositiveButton(
                        R.string.button_text_enable_depth,
                        (DialogInterface dialog, int which) -> {
                            depthSettings.setUseDepthForOcclusion(true);
                        })
                .setNegativeButton(
                        R.string.button_text_disable_depth,
                        (DialogInterface dialog, int which) -> {
                            depthSettings.setUseDepthForOcclusion(false);
                        })
                .show();
    }

    private void launchInstantPlacementSettingsMenuDialog() {
        resetSettingsMenuDialogCheckboxes();
        Resources resources = getResources();
        new AlertDialog.Builder(this)
                .setTitle(R.string.options_title_instant_placement)
                .setMultiChoiceItems(
                        resources.getStringArray(R.array.instant_placement_options_array),
                        instantPlacementSettingsMenuDialogCheckboxes,
                        (DialogInterface dialog, int which, boolean isChecked) ->
                                instantPlacementSettingsMenuDialogCheckboxes[which] = isChecked)
                .setPositiveButton(
                        R.string.done,
                        (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
                .setNegativeButton(
                        android.R.string.cancel,
                        (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
                .show();
    }

    /**
     * Shows checkboxes to the user to facilitate toggling of depth-based effects.
     */
    private void launchDepthSettingsMenuDialog() {
        // Retrieves the current settings to show in the checkboxes.
        resetSettingsMenuDialogCheckboxes();

        // Shows the dialog to the user.
        Resources resources = getResources();
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            // With depth support, the user can select visualization options.
            new AlertDialog.Builder(this)
                    .setTitle(R.string.options_title_with_depth)
                    .setMultiChoiceItems(
                            resources.getStringArray(R.array.depth_options_array),
                            depthSettingsMenuDialogCheckboxes,
                            (DialogInterface dialog, int which, boolean isChecked) ->
                                    depthSettingsMenuDialogCheckboxes[which] = isChecked)
                    .setPositiveButton(
                            R.string.done,
                            (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
                    .setNegativeButton(
                            android.R.string.cancel,
                            (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
                    .show();
        } else {
            // Without depth support, no settings are available.
            new AlertDialog.Builder(this)
                    .setTitle(R.string.options_title_without_depth)
                    .setPositiveButton(
                            R.string.done,
                            (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
                    .show();
        }
    }

    private void applySettingsMenuDialogCheckboxes() {
        depthSettings.setUseDepthForOcclusion(depthSettingsMenuDialogCheckboxes[0]);
        depthSettings.setDepthColorVisualizationEnabled(depthSettingsMenuDialogCheckboxes[1]);
        instantPlacementSettings.setInstantPlacementEnabled(
                instantPlacementSettingsMenuDialogCheckboxes[0]);
        configureSession();
    }

    private void resetSettingsMenuDialogCheckboxes() {
        depthSettingsMenuDialogCheckboxes[0] = depthSettings.useDepthForOcclusion();
        depthSettingsMenuDialogCheckboxes[1] = depthSettings.depthColorVisualizationEnabled();
        instantPlacementSettingsMenuDialogCheckboxes[0] =
                instantPlacementSettings.isInstantPlacementEnabled();
    }

    /**
     * Checks if we detected at least one plane.
     */
    private boolean hasTrackingPlane() {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
    }

    /**
     * Configures the session with feature settings.
     */
    private void configureSession() {
        Config config = session.getConfig();
        config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        } else {
            config.setDepthMode(Config.DepthMode.DISABLED);
        }
        if (instantPlacementSettings.isInstantPlacementEnabled()) {
            config.setInstantPlacementMode(InstantPlacementMode.LOCAL_Y_UP);
        } else {
            config.setInstantPlacementMode(InstantPlacementMode.DISABLED);
        }
        session.configure(config);


//        CameraConfigFilter filter = new CameraConfigFilter(session);
//        filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));

        CameraConfigFilter filter = new CameraConfigFilter(session);
        List<CameraConfig> cameraConfigList = session.getSupportedCameraConfigs(filter);

        for (CameraConfig cconfig : cameraConfigList) {
            Log.d(TAG, "camera : " + cconfig.getCameraId() + "\n" +
                    cconfig.getImageSize() + "\n" +
                    cconfig.getTextureSize() + "\n" +
                    cconfig.getFpsRange());
        }

//        // Use element 0 from the list of returned camera configs. This is because
//        // it contains the camera config that best matches the specified filter
//        // settings.
//        session.setCameraConfig(cameraConfigList.get(2));
//        session.setCameraConfig(cameraConfigList.get(3));

    }

    void transferBufferToRenderer(SampleRender render) {
        if (trigger == true) trigger = false;
        else return;
        ReconstructedMesh.getInstance().transferToModel(render, virtualObjectMesh);
    }

    void enableTransfer() {
        trigger = true;
    }



}
