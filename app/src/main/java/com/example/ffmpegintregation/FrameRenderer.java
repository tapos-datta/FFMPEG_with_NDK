package com.example.ffmpegintregation;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import com.example.ffmpegintregation.Utils.EglUtil;
import com.example.ffmpegintregation.egl.GlFrameBufferObjectRenderer;
import com.example.ffmpegintregation.egl.GlFramebufferObject;
import com.example.ffmpegintregation.egl.GlPreviewFilter;
import com.example.ffmpegintregation.egl.GlSurfaceTexture;
import com.example.ffmpegintregation.filter.GlFilter;

import javax.microedition.khronos.egl.EGLConfig;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_MAX_TEXTURE_SIZE;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glViewport;

public class FrameRenderer extends GlFrameBufferObjectRenderer implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = FrameRenderer.class.getSimpleName();

    private GlSurfaceTexture previewTexture;
    private boolean updateSurface = false;

    private int texName;

    private float[] MVPMatrix = new float[16];
    private float[] ProjMatrix = new float[16];
    private float[] MMatrix = new float[16];
    private float[] VMatrix = new float[16];
    private float[] STMatrix = new float[16];


    private GlFramebufferObject filterFramebufferObject;
    private GlPreviewFilter previewFilter;
    private Surface surface = null;

    private GlFilter glFilter;
    private boolean isNewFilter;
    private final GLSurfaceView glPreview;

    private float aspectRatio = 1f;

    private long updateTexImageCompare = 0;
    private long updateTexImageCounter = 0;

    FrameRenderer(GLSurfaceView glPreview) {
        super();
        Matrix.setIdentityM(STMatrix, 0);
        this.glPreview = glPreview;
    }

    void setGlFilter(final GlFilter filter) {
        glPreview.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (glFilter != null) {
                    glFilter.release();
//                    if (glFilter instanceof GlLookUpTableFilter) {
//                        ((GlLookUpTableFilter) glFilter).releaseLutBitmap();
//                    }
                    glFilter = null;
                }
                glFilter = filter;
                isNewFilter = true;
                glPreview.requestRender();
            }
        });
    }

    @Override
    public void onSurfaceCreated(final EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        final int[] args = new int[1];

        GLES20.glGenTextures(args.length, args, 0);
        texName = args[0];


        previewTexture = new GlSurfaceTexture(texName);
        previewTexture.setOnFrameAvailableListener(this);


        GLES20.glBindTexture(previewTexture.getTextureTarget(), texName);
        // GL_TEXTURE_EXTERNAL_OES
        EglUtil.setupSampler(previewTexture.getTextureTarget(), GL_LINEAR, GL_LINEAR);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        filterFramebufferObject = new GlFramebufferObject();
        // GL_TEXTURE_EXTERNAL_OES
        previewFilter = new GlPreviewFilter(previewTexture.getTextureTarget());
        previewFilter.setup();

        surface = new Surface(previewTexture.getSurfaceTexture());

        Matrix.setLookAtM(VMatrix, 0,
                0.0f, 0.0f, 5.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        );

        synchronized (this) {
            updateSurface = false;
        }

        if (glFilter != null) {
            isNewFilter = true;
        }

        GLES20.glGetIntegerv(GL_MAX_TEXTURE_SIZE, args, 0);

    }

    @Override
    public void onSurfaceChanged(final int width, final int height) {
        Log.d(TAG, "onSurfaceChanged width = " + width + "  height = " + height);
        filterFramebufferObject.setup(width, height);
        previewFilter.setFrameSize(width, height);
        if (glFilter != null) {
            glFilter.setFrameSize(width, height);
        }
        aspectRatio = (float) width / height;
        Matrix.frustumM(ProjMatrix, 0, -aspectRatio, aspectRatio, -1, 1, 5, 7);
        Matrix.setIdentityM(MMatrix, 0);
    }

    @Override
    public void onDrawFrame(final GlFramebufferObject fbo) {

        synchronized (this) {
            if (updateTexImageCompare != updateTexImageCounter) {
                // loop and call updateTexImage() for each time the onFrameAvailable() method was called below.
                while (updateTexImageCompare != updateTexImageCounter) {

                    previewTexture.updateTexImage();
                    previewTexture.getTransformMatrix(STMatrix);
                    updateTexImageCompare++;  // increment the compare value until it's the same as _updateTexImageCounter
                }
            }
        }

        if (isNewFilter) {
            if (glFilter != null) {
                glFilter.setup();
                glFilter.setFrameSize(fbo.getWidth(), fbo.getHeight());
            }
            isNewFilter = false;
        }

        if (glFilter != null) {
            filterFramebufferObject.enable();
            glViewport(0, 0, filterFramebufferObject.getWidth(), filterFramebufferObject.getHeight());
        }

        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        Matrix.multiplyMM(MVPMatrix, 0, VMatrix, 0, MMatrix, 0);
        Matrix.multiplyMM(MVPMatrix, 0, ProjMatrix, 0, MVPMatrix, 0);

        previewFilter.draw(texName, MVPMatrix, STMatrix, aspectRatio);

        if (glFilter != null) {
            fbo.enable();
            GLES20.glClear(GL_COLOR_BUFFER_BIT);
            glFilter.draw(filterFramebufferObject.getTexName(), fbo);
        }
    }

    @Override
    public synchronized void onFrameAvailable(final SurfaceTexture previewTexture) {
        updateTexImageCounter++;
       // glPreview.requestRender();
    }

    void release() {
        if (glFilter != null) {
            glFilter.release();
        }
        if (previewTexture != null) {
            previewTexture.release();
        }
    }

    public void setViewaspect(float viewaspect,boolean isSwitched){
       // this.aspectRatio = viewaspect;

//        if(isSwitched) {
//            Matrix.frustumM(ProjMatrix, 0, -1, 1, - viewaspect,  viewaspect, 5, 7);
//        }else{
//            Matrix.frustumM(ProjMatrix, 0,   -1 - viewaspect ,  1 + viewaspect , -1, 1, 5, 7);
//        }
//        Matrix.setIdentityM(MMatrix, 0);

    }

    public Surface getSurface() {
        return surface;
    }
}
