package com.crazydude.androidhwstreaminglibrary;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import net.butterflytv.rtmp_client.RTMPMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by kartavtsev.s on 12.04.2016.
 */
public class MediaCodecManager implements Camera.PreviewCallback, SurfaceHolder.Callback {

    public static final String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 25;
    private static final int IFRAME_INTERVAL = 5;

    private int mWidth = 640; // ширина видео
    private int mHeight = 480; // высота видео
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private MediaCodec mMediaCodec;
    private boolean mIsStarted = false;
    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private MediaFormat mVideoFormat;
    private int mCurrentFrame;
    private RTMPMuxer mMuxer;

    public MediaCodecManager(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
        mSurfaceView.getHolder().addCallback(this);
        mMuxer = new RTMPMuxer();
        mMuxer.open("rtmp://ulc.network:1935/videochat/test");
    }

    private static long computePresentationTime(int frameIndex) {
        return 132 + frameIndex * 1000000 / FRAME_RATE;
    }

    private static int computeLivePresentationTime(int frameIndex) {
        return frameIndex * (1000 / FRAME_RATE);
    }

    public void release() {
        mCamera.lock();
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        mMuxer.close();
        mSurfaceView = null;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mIsStarted) {
            encodeFrame(data);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        init();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        release();
    }

    private void encodeFrame(byte[] data) {
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);

        if (inputBufferIndex >= 0) {
            byte[] tempBuffer = new byte[data.length];
            NV21toI420SemiPlanar(data, tempBuffer, mWidth, mHeight);
            mInputBuffers[inputBufferIndex].put(tempBuffer);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length, computePresentationTime(mCurrentFrame), 0);
        }

        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, -1);

        if (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mOutputBuffers[outputBufferIndex];
            byte[] tempBuffer = new byte[mBufferInfo.size];
            outputBuffer.get(tempBuffer);
            mMuxer.writeVideo(tempBuffer, 0, tempBuffer.length, computeLivePresentationTime(mCurrentFrame));
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            mOutputBuffers = mMediaCodec.getOutputBuffers();
        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            mVideoFormat = mMediaCodec.getOutputFormat();
        }

        mCurrentFrame++;
    }

    private void init() {
        try {
            initCamera();
            initCodec();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initCodec() throws IOException {
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mVideoFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        mVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        mVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mWidth * mHeight * FRAME_RATE);
        mVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        mMediaCodec.configure(mVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        mIsStarted = true;

        mInputBuffers = mMediaCodec.getInputBuffers();
        mOutputBuffers = mMediaCodec.getOutputBuffers();
    }

    private void initCamera() throws IOException {
        mCamera = Camera.open();
        mCamera.startPreview();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mWidth, mHeight);
        parameters.setPreviewFormat(ImageFormat.NV21);
        mCamera.setParameters(parameters);
        mCamera.setPreviewDisplay(mSurfaceView.getHolder());
        mCamera.setPreviewCallback(this);
    }

    private void NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes, int width,
                                      int height) {
        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
        for (int i = width * height; i < nv21bytes.length; i += 2) {
            i420bytes[i] = nv21bytes[i + 1];
            i420bytes[i + 1] = nv21bytes[i];
        }
    }
}
