package com.apical.factorytest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.util.AttributeSet;
import android.util.Log;

public class WaveformView extends View {
    private static final String TAG = "AudioTestActivity";
    private static final int SAMPLE_RATE_RECORD   = 44100;

    private AudioRecord    mRecorder;
    private int            mMinBufSizeR;
    private RecordThread   mRecThread;
    private short[]        mAudioData;
    private int            mMax = Integer.MIN_VALUE;
    private int            mMin = Integer.MAX_VALUE;
    private boolean        mPass= false;

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMinBufSizeR = AudioRecord.getMinBufferSize(SAMPLE_RATE_RECORD,
                            AudioFormat.CHANNEL_CONFIGURATION_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
        mRecorder    = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_RECORD,
                            AudioFormat.CHANNEL_CONFIGURATION_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            mMinBufSizeR * 2);
    }

    public void onResume() {
        mRecThread = new RecordThread();
        mRecThread.start();
    }

    public void onPause() {
        mRecThread.stopRecord();
        mRecThread = null;
    }

    public void onDestroy() {
        mRecorder.stop();
        mRecorder.release();
    }

    public boolean isPass() {
        return mPass;
    }

    @Override
    public String toString() {
        String str = "Mic test\r\n"
                   + "--------\r\n"
                   + "test result: " + (mPass ? "PASS" : "NG")
                   + "\r\n\r\n";
        return str;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);

        if (mAudioData == null) {
            return;
        }

        Path  path  = new Path ();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.rgb(0, 255, 0));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);

        int w = canvas.getWidth();
        int h = canvas.getHeight();
        int n = mAudioData.length; // mAudioData.length; // 512;

        int delta = n / w > 1 ? n / w : 1;
        int px, py, i;
        px = 0;
        py = (int)(h * (mAudioData[0] + 0x7fff) / 0x10000);
        path.moveTo(px, py);
        for (i=delta; i<n; i+=delta) {
            px = w * i / n;
            py = (int)(h * (mAudioData[i] + 0x7fff) / 0x10000);
            path.lineTo(px, py);
            mMax = mMax > mAudioData[i] ? mMax : mAudioData[i];
            mMin = mMin < mAudioData[i] ? mMin : mAudioData[i];
        }
        canvas.drawPath(path, paint);

        // update mPass
        mPass = mMax > 0x3fff && mMin < -0x3fff;
    }

    public void setAudioData(short[] data) {
        mAudioData = data;
    }

    private static final int MSG_REFREH  = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_REFREH:
                invalidate();
                break;
            }
        }
    };

    class RecordThread extends Thread
    {
        private boolean isRecording = false;

        @Override
        public void run() {
            isRecording = true;
            mRecorder.startRecording();

            while (isRecording) {
                int sampnum = mMinBufSizeR / 2;
                int offset  = 0;
                short[] buf = new short[sampnum];
                while (offset != sampnum) {
                    offset += mRecorder.read(buf, offset, sampnum);
//                  Log.d(TAG, "read record data offset = " + offset);
                }
                WaveformView.this.setAudioData(buf);
                WaveformView.this.mHandler.sendEmptyMessage(MSG_REFREH);
            }

            mRecorder.stop();
        }

        public void stopRecord() {
            isRecording = false;
        }
    }
}
