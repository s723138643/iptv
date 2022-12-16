package com.orion.player.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VideoGLSurfaceView extends GLSurfaceView {
    private Surface surface;
    private final List<Callback> callbacks = new ArrayList<>();

    public VideoGLSurfaceView(Context context) {
        this(context, null);
    }

    public VideoGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        setRenderer(new Render());
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    protected void onSurfaceTextureCreated(SurfaceTexture surfaceTexture) {
        post(() -> {
            surface = new Surface(surfaceTexture);
            notifySurfaceCreated(surface);
        });
    }

    protected void notifySurfaceCreated(Surface surface) {
        for (Callback callback : callbacks) {
            callback.surfaceCreated(surface);
        }
    }

    public Surface getSurface() {
        return surface;
    }

    public void addCallback(Callback callback) {
        callbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        callbacks.remove(callback);
    }

    private class Render implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
        protected int mTextureID;
        protected SurfaceTexture mSurface;
        protected boolean updateSurface = false;

        protected int programHandle;
        protected int aPosition;
        protected int aTextureCoordinates;
        protected int uMVPMatrix;
        protected int uSTMatrix;
        protected float[] mMVPMatrix = new float[16];
        protected float[] mSTMatrix = new float[16];

        protected final FloatBuffer pointer;
        protected float[] VERTEX_DATA = {
                -1.0f, -1.0f, 0, 0, 0,
                1.0f, -1.0f, 0, 1.0f, 0f,
                -1.0f, 1.0f, 0, 0, 1.0f,
                1.0f, 1.0f, 0, 1.0f, 1.0f,
        };

        protected final String vertexShader = "uniform mat4 u_MVPMatrix;\n" +
                "uniform mat4 u_STMatrix;\n" +
                "attribute vec4 a_Position;\n" +
                "attribute vec4 a_TextureCoordinates;\n" +
                "varying vec2 v_TextureCoordinates;\n" +
                "void main() {\n" +
                "  gl_Position = u_MVPMatrix * a_Position;\n" +
                "  v_TextureCoordinates = (u_STMatrix * a_TextureCoordinates).xy;\n" +
                "}";

        protected final String fragmentShader = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 v_TextureCoordinates;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(sTexture, v_TextureCoordinates);\n" +
                "}";

        {
            pointer = ByteBuffer.allocateDirect(VERTEX_DATA.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(VERTEX_DATA);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            programHandle = buildProgram(vertexShader, fragmentShader);
            aPosition = GLES20.glGetAttribLocation(programHandle, "a_Position");
            aTextureCoordinates = GLES20.glGetAttribLocation(programHandle, "a_TextureCoordinates");
            uMVPMatrix = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
            uSTMatrix = GLES20.glGetUniformLocation(programHandle, "u_STMatrix");

            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            mTextureID = textures[0];

            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            mSurface = new SurfaceTexture(mTextureID);
            mSurface.setOnFrameAvailableListener(this);
            onSurfaceTextureCreated(mSurface);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            Matrix.setIdentityM(mMVPMatrix, 0);
            Log.w("VideoGLSurfaceView", String.format("surface changed: %dx%d", width, height));
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            synchronized (this) {
                if (updateSurface) {
                    mSurface.updateTexImage();
                    mSurface.getTransformMatrix(mSTMatrix);
                    updateSurface = false;
                }
            }
            GLES20.glClearColor(0f, 0f, 0f, 1f);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(programHandle);
            GLES20.glUniformMatrix4fv(uMVPMatrix, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(uSTMatrix, 1, false, mSTMatrix, 0);
            setPointer(0, aPosition, 3, (3+2)*4);
            setPointer(3, aTextureCoordinates, 2, (3+2)*4);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glFinish();
        }

        @Override
        public void onFrameAvailable(SurfaceTexture unused) {
            synchronized (this) {
                updateSurface = true;
            }
            queueEvent(VideoGLSurfaceView.this::requestRender);
        }

        protected int compileShader(int type, String code){
            final int shaderObjectId = GLES20.glCreateShader(type);
            if (shaderObjectId == 0){
                return 0;
            }
            GLES20.glShaderSource(shaderObjectId, code);
            GLES20.glCompileShader(shaderObjectId);
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderObjectId, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0){
                GLES20.glDeleteShader(shaderObjectId);
                return 0;
            }
            return shaderObjectId;
        }

        protected int linkProgram(int vertexShaderId, int fragmentShaderId){
            final int programObjectId = GLES20.glCreateProgram();
            if(programObjectId == 0){
                return 0;
            }
            GLES20.glAttachShader(programObjectId, vertexShaderId);
            GLES20.glAttachShader(programObjectId, fragmentShaderId);
            GLES20.glLinkProgram(programObjectId);
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programObjectId, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0){
                GLES20.glDeleteProgram(programObjectId);
                return 0;
            }
            return programObjectId;
        }

        protected int buildProgram(String vertexShader, String fragmentShader){
            int vertexShaderId = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
            int fragmentShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
            return linkProgram(vertexShaderId, fragmentShaderId);
        }

        protected void setPointer(int offset, int location, int count, int stride) {
            pointer.position(offset);
            GLES20.glVertexAttribPointer(location, count, GLES20.GL_FLOAT, false, stride, pointer);
            GLES20.glEnableVertexAttribArray(location);
            pointer.position(0);
        }
    }

    public interface Callback {
        void surfaceCreated(Surface surface);
    }
}
