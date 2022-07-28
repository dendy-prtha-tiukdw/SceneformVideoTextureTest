package com.example.sceneformvideotexturetest;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;


import com.example.sceneformvideotexturetest.databinding.ActivityAugmentedUiBinding;

import com.google.ar.core.Anchor;

import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;

import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import com.google.ar.sceneform.ux.TransformableNode;

public class AugmentedImageRenderable extends AppCompatActivity {

    private ActivityAugmentedUiBinding binding;
    private ArFragment arFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAugmentedUiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            Log.d("setOnTapArPlaneListener", "setOnTapArPlaneListener: pressed");
            if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                return;
            }

            Anchor anchor = hitResult.createAnchor();
            placeObject(arFragment, anchor);
        });
    }

    private void placeObject(ArFragment fragment, Anchor anchor) {
        ViewRenderable.builder()
                .setView(fragment.getContext(), R.layout.renderable_view)
                .build()
                .thenAccept(renderable -> {
                    renderable.setShadowCaster(false);
                    renderable.setShadowCaster(false);

                    WebView webView = renderable.getView().findViewById(R.id.webview);
                    webView.getSettings().setJavaScriptEnabled(true);
                    String myYouTubeVideoUrl = "https://www.youtube.com/embed/THMV-qWlMIU";
                    String dataUrl =
                            "<html>" +
                                    "<body>" +
                                    "<h2>Video From YouTube</h2>" +
                                    "<br>" +
                                    "<iframe width=\"560\" height=\"315\" src=\""+myYouTubeVideoUrl+"\" frameborder=\"0\" allowfullscreen/>" +
                                    "</body>" +
                                    "</html>";
                    webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
                    webView.getSettings().setLoadWithOverviewMode(true);
                    webView.getSettings().setUseWideViewPort(true);
//                    webView.loadData(dataUrl, "text/html", "utf-8");
                    webView.loadUrl("https://www.ukdw.ac.id/akademik/fakultas-teknologi-informasi/");
                    renderable.getView().findViewById(R.id.info_button).setOnClickListener(view -> {
                        Log.d("info_button", "info_button: pressed");
                    });
                    addControlsToScene(fragment, anchor, renderable);

                })
                .exceptionally(throwable -> {
                    Log.e("ViewRenderable.builder()", "onCreate: ", throwable);
                    return null;
                });
    }

    private void addControlsToScene(ArFragment fragment, Anchor anchor, ViewRenderable renderable) {
        AnchorNode trackingAnchorNode = new AnchorNode(anchor);
        trackingAnchorNode.setParent(fragment.getArSceneView().getScene());

        TransformableNode transformableNode = new TransformableNode(fragment.getTransformationSystem());
        transformableNode.setRenderable(renderable);
        transformableNode.setParent(trackingAnchorNode);

    }

}