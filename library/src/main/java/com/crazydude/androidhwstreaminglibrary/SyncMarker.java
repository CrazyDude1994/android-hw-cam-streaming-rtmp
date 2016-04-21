package com.crazydude.androidhwstreaminglibrary;

/**
 * Created by kartavtsev.s on 21.04.2016.
 */
public class SyncMarker {

    private long mNanoTime;
    private long mFrameIndex;

    public SyncMarker(long nanoTime, long frameIndex) {
        this.mNanoTime = nanoTime;
        this.mFrameIndex = frameIndex;
    }
}
