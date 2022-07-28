package com.example.sceneformvideotexturetest;

import static com.google.ar.core.AugmentedImage.TrackingMethod.*;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.example.sceneformvideotexturetest.databinding.ActivityAugmentedDefaultBinding;
import com.google.android.filament.Engine;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.EngineInstance;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.InstructionsController;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.VideoNode;
import com.google.android.filament.filamat.MaterialBuilder;
import com.google.android.filament.filamat.MaterialPackage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AugmentedImage extends AppCompatActivity implements
        FragmentOnAttachListener, BaseArFragment.OnSessionConfigurationListener {

    private ActivityAugmentedDefaultBinding binding;
    private MediaPlayer mediaPlayer;
    private ArFragment arFragment;
    private AugmentedImageDatabase database;
    private Renderable plainVideoModel;
    private Material plainVideoMaterial;
    private final List<CompletableFuture<Void>> futures = new ArrayList<>();

    private int mode = R.id.menuPlainVideo;
    private boolean rabbitDetected = false;
    private boolean matrixDetected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAugmentedDefaultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar, (v, insets) -> {
            ((ViewGroup.MarginLayoutParams) binding.toolbar.getLayoutParams()).topMargin = insets
                    .getInsets(WindowInsetsCompat.Type.systemBars())
                    .top;

            return WindowInsetsCompat.CONSUMED;
        });

        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }

        if (Sceneform.isSupported(this)) {
            // .glb models can be loaded at runtime when needed or when app starts
            // This method loads ModelRenderable when app starts
            loadMatrixModel();
            loadMatrixMaterial();
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.profil_ukdw);
        mediaPlayer.setLooping(true);
    }

    private void loadMatrixMaterial() {
        Engine filamentEngine = EngineInstance.getEngine().getFilamentEngine();

        MaterialBuilder.init();
        MaterialBuilder materialBuilder = new MaterialBuilder()
                .platform(MaterialBuilder.Platform.MOBILE)
                .name("External Video Material")
                .require(MaterialBuilder.VertexAttribute.UV0)
                .shading(MaterialBuilder.Shading.UNLIT)
                .doubleSided(true)
                .samplerParameter(MaterialBuilder.SamplerType.SAMPLER_EXTERNAL, MaterialBuilder.SamplerFormat.FLOAT, MaterialBuilder.ParameterPrecision.DEFAULT, "videoTexture")
                .optimization(MaterialBuilder.Optimization.NONE);

        MaterialPackage plainVideoMaterialPackage = materialBuilder
                .blending(MaterialBuilder.BlendingMode.OPAQUE)
                .material("void material(inout MaterialInputs material) {\n" +
                        "    prepareMaterial(material);\n" +
                        "    material.baseColor = texture(materialParams_videoTexture, getUV0()).rgba;\n" +
                        "}\n")
                .build(filamentEngine);
        if (plainVideoMaterialPackage.isValid()) {
            ByteBuffer buffer = plainVideoMaterialPackage.getBuffer();
            futures.add(Material.builder()
                    .setSource(buffer)
                    .build()
                    .thenAccept(material -> {
                        plainVideoMaterial = material;
                    })
                    .exceptionally(
                            throwable -> {
                                Toast.makeText(this, "Unable to load material", Toast.LENGTH_LONG).show();
                                return null;
                            }));
        }
        MaterialBuilder.shutdown();
    }

    private void loadMatrixModel() {
        futures.add(ModelRenderable.builder()
                .setSource(this, Uri.parse("models/Video.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(model -> {
                    //removing shadows for this Renderable
                    model.setShadowCaster(false);
                    model.setShadowReceiver(true);
                    plainVideoModel = model;
                })
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG).show();
                            return null;
                        }));
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        item.setChecked(!item.isChecked());
        this.mode = item.getItemId();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
//        mediaPlayer.start();

    }

    @Override
    protected void onStop() {
        super.onStop();
//        mediaPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }

        futures.forEach(future -> {
            if (future.isDone())
                future.cancel(true);
        });
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        // Disable plane detection
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);

        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime
        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)
        database = new AugmentedImageDatabase(session);

        Bitmap matrixImage = BitmapFactory.decodeResource(getResources(), R.drawable.matrix);
        Bitmap rabbitImage = BitmapFactory.decodeResource(getResources(), R.drawable.rabbit);

        // Every image has to have its own unique String identifier
        database.addImage("rabbit", rabbitImage);
        database.addImage("matrix", matrixImage);

        config.setAugmentedImageDatabase(database);

        // Check for image detection
        arFragment.setOnAugmentedImageUpdateListener(this::onAugmentedImageTrackingUpdate);
    }

    private void onAugmentedImageTrackingUpdate(com.google.ar.core.AugmentedImage augmentedImage) {
        // If there are both images already detected, for better CPU usage we do not need scan for them
        Log.i("Tracking status", "TrackingState: " + augmentedImage.getTrackingState());
        Log.i("Tracking status", "TrackingMethod: " + augmentedImage.getTrackingMethod());

//        if (rabbitDetected && matrixDetected) {
//            return;
//        }

        if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
            if (augmentedImage.getTrackingMethod() == FULL_TRACKING) {
                // Setting anchor to the center of Augmented Image
                AnchorNode anchorNode = new AnchorNode(augmentedImage.createAnchor(augmentedImage.getCenterPose()));

                // If matrix video haven't been placed yet and detected image has String identifier of "matrix"
                if (augmentedImage.getName().equals("matrix")) {
                    if (!matrixDetected) {
                        matrixDetected = true;
                        anchorNode.setWorldScale(new Vector3(augmentedImage.getExtentX(), 1f, augmentedImage.getExtentZ()));
                        arFragment.getArSceneView().getScene().addChild(anchorNode);

                        TransformableNode videoNode = new TransformableNode(arFragment.getTransformationSystem());
                        // For some reason it is shown upside down so this will rotate it correctly
                        videoNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 180f));
                        videoNode.setLocalRotation(Quaternion.axisAngle(new Vector3(1, 0f, 0), 180f));
                        anchorNode.addChild(videoNode);

                        // Setting texture
                        ExternalTexture externalTexture = new ExternalTexture();
                        RenderableInstance renderableInstance = videoNode.setRenderable(plainVideoModel);
                        renderableInstance.setMaterial(plainVideoMaterial);

                        // Setting MediaPLayer
                        renderableInstance.getMaterial().setExternalTexture("videoTexture", externalTexture);
                        mediaPlayer.setSurface(externalTexture.getSurface());
                    }

                    if (!mediaPlayer.isPlaying()) {
//                        Toast.makeText(this, "Matrix tag detected", Toast.LENGTH_LONG).show();

                        // AnchorNode placed to the detected tag and set it to the real size of the tag
                        // This will cause deformation if your AR tag has different aspect ratio than your video

                        mediaPlayer.start();
                    }
                }

                // If rabbit models haven't been placed yet and detected image has String identifier of "rabbit"
                // This is also example of models loading and placing at runtime
                if (!rabbitDetected && augmentedImage.getName().equals("rabbit")) {
                    rabbitDetected = true;
                    Toast.makeText(this, "Rabbit tag detected", Toast.LENGTH_LONG).show();

                    anchorNode.setWorldScale(new Vector3(3.5f, 3.5f, 3.5f));
                    arFragment.getArSceneView().getScene().addChild(anchorNode);
                    futures.add(ModelRenderable.builder()
                            .setSource(this, Uri.parse("models/Rabbit.glb"))
                            .setIsFilamentGltf(true)
                            .build()
                            .thenAccept(rabbitModel -> {
                                TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                                modelNode.setRenderable(rabbitModel);
                                anchorNode.addChild(modelNode);
                            })
                            .exceptionally(
                                    throwable -> {
                                        Toast.makeText(this, "Unable to load rabbit model", Toast.LENGTH_LONG).show();
                                        return null;
                                    }));
                }
            }
            if (augmentedImage.getTrackingMethod() == LAST_KNOWN_POSE) {
                if (matrixDetected && augmentedImage.getName().equals("matrix")) {
                    mediaPlayer.pause();
                }
            }
        }

        if (matrixDetected && rabbitDetected) {
            arFragment.getInstructionsController().setEnabled(
                    InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false);
        }

    }
}