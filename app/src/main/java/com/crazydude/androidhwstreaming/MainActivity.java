package com.crazydude.androidhwstreaming;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;

import com.crazydude.androidhwstreaminglibrary.MediaCodecManager;

public class MainActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;
    private MediaCodecManager mMediaCodecManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mMediaCodecManager = new MediaCodecManager(mSurfaceView);
    }
}
