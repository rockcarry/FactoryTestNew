package com.apical.factorytest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class mainActivity extends Activity {
    private final static String TAG = "mainActivity";

    private GpsView      mGpsView;
    private WifiView     mWifiView;
    private BtView       mBtView;
    private DeviceView   mDevView;
    private WaveformView mWaveform;
    private TextView     mTxtHome;
    private TextView     mTxtPower;
    private TextView     mTxtVolDec;
    private TextView     mTxtVolInc;
    private View         mKeyTest;
    private View         mMicTest;
    private Button       mCamTest;
    private Button       mSpkTest;
    private Button       mEphTest;
    private Button       mBklTest;
    private Button       mLedTest;
    private Button       mSaveGps;
    private Button       mSaveReport;
    private Button       mBtnExit;
    private int          mResultHome;
    private int          mResultPower;
    private int          mResultVolDec;
    private int          mResultVolInc;
    private boolean      mCamPass;
    private boolean      mSpkPass;
    private boolean      mEphPass;
    private boolean      mBklPass;
    private boolean      mLedPass;
    private MediaPlayer  mPlayer;
    private AudioManager mAudioMan;
    private SurfaceView  mPreview;
    private Camera       mCamera;
    private int          mCamIdx;

    @Override
    public String toString() {
        String str = "Button test\r\n"
                   + "-----------\r\n"
                   + "home : " + (mResultHome == 3 ? "PASS " : "NG   ") + mResultHome   + "\r\n"
                   + "power: " + (mResultHome == 3 ? "PASS " : "NG   ") + mResultPower  + "\r\n"
                   + "vol- : " + (mResultHome == 3 ? "PASS " : "NG   ") + mResultVolDec + "\r\n"
                   + "vol+ : " + (mResultHome == 3 ? "PASS " : "NG   ") + mResultVolInc + "\r\n\r\n"
                   + "Other test (need subjective judgment)\r\n"
                   + "-------------------------------------\r\n";
        str += "camera test   : " + (mCamPass ? "PASS" : "NG") + "\r\n";
        str += "speaker test  : " + (mSpkPass ? "PASS" : "NG") + "\r\n";
        str += "earphone test : " + (mEphPass ? "PASS" : "NG") + "\r\n";
        str += "backlight test: " + (mBklPass ? "PASS" : "NG") + "\r\n";
//      str += "chargeled test: " + (mLedPass ? "PASS" : "NG") + "\r\n";
        return str;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mGpsView   = (GpsView     )findViewById(R.id.gps_view       );
        mWifiView  = (WifiView    )findViewById(R.id.wifi_view      );
        mBtView    = (BtView      )findViewById(R.id.bt_view        );
        mDevView   = (DeviceView  )findViewById(R.id.device_view    );
        mWaveform  = (WaveformView)findViewById(R.id.wave_view      );
        mTxtHome   = (TextView    )findViewById(R.id.txt_key_home   );
        mTxtPower  = (TextView    )findViewById(R.id.txt_key_power  );
        mTxtVolDec = (TextView    )findViewById(R.id.txt_key_voldec );
        mTxtVolInc = (TextView    )findViewById(R.id.txt_key_volinc );
        mKeyTest   = (View        )findViewById(R.id.key_test       );
        mMicTest   = (View        )findViewById(R.id.mic_test       );
        mCamTest   = (Button      )findViewById(R.id.btn_cam_test   );
        mSpkTest   = (Button      )findViewById(R.id.btn_spk_test   );
        mEphTest   = (Button      )findViewById(R.id.btn_hp_test    );
        mBklTest   = (Button      )findViewById(R.id.btn_bkl_test   );
        mLedTest   = (Button      )findViewById(R.id.btn_led_test   );
        mSaveGps   = (Button      )findViewById(R.id.btn_save_gps   );
        mSaveReport= (Button      )findViewById(R.id.btn_save_report);
        mBtnExit   = (Button      )findViewById(R.id.btn_exit       );

        mBtView    .setOnClickListener(mOnClickListener);
        mCamTest   .setOnClickListener(mOnClickListener);
        mSpkTest   .setOnClickListener(mOnClickListener);
        mEphTest   .setOnClickListener(mOnClickListener);
        mBklTest   .setOnClickListener(mOnClickListener);
        mLedTest   .setOnClickListener(mOnClickListener);
        mSaveGps   .setOnClickListener(mOnClickListener);
        mSaveReport.setOnClickListener(mOnClickListener);
        mBtnExit   .setOnClickListener(mOnClickListener);

        // create a player for music playing
        mPlayer = MediaPlayer.create(this, R.raw.music);
        mPlayer.setLooping(true);

        // get AudioManager
        mAudioMan = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // set default screen backlight
        setScreenBrightness(128);

        // start test view refresh
        mHandler.sendEmptyMessageDelayed(MSG_REFREH, 1000);

        try { mCamera  = Camera.open(1); } catch (Exception e) { e.printStackTrace(); }
        mPreview = (SurfaceView)findViewById(R.id.camera_view);
        mPreview.getHolder().addCallback(mPreviewSurfaceHolderCallback);
        mPreview.setOnClickListener(mOnClickListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // stop test view refresh
        mHandler.removeMessages(MSG_REFREH);

        mGpsView .onDestroy();
        mWifiView.onDestroy();
        mBtView  .onDestroy();
        mDevView .onDestroy();
        mWaveform.onDestroy();
        mPlayer.release();

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideSystemUI(true);

        Settings.System.putInt(getContentResolver(), Settings.System.SHOW_TOUCHES    , 1);
        Settings.System.putInt(getContentResolver(), Settings.System.POINTER_LOCATION, 1);

        mGpsView .onResume();
        mWifiView.onResume();
        mBtView  .onResume();
        mDevView .onResume();
        mWaveform.onResume();

        // enter hardware button test mode
        if (mResultHome != 3 || mResultPower != 3 || mResultVolDec != 3 || mResultVolInc != 3) {
            sendBroadcast(new Intent("com.apical.testhwbutton.enable"));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        hideSystemUI(false);

        Settings.System.putInt(getContentResolver(), Settings.System.SHOW_TOUCHES    , 0);
        Settings.System.putInt(getContentResolver(), Settings.System.POINTER_LOCATION, 0);

        mGpsView .onPause();
        mWifiView.onPause();
        mBtView  .onPause();
        mDevView .onPause();
        mWaveform.onPause();

        // disable hardware button test mode
        sendBroadcast(new Intent("com.apical.testhwbutton.disable"));
    }

    private void doCamTest() {
        View view = getLayoutInflater().inflate(R.layout.dialog4, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(R.string.cam_test_title);
        builder.setView(view);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.findViewById(R.id.radio_front).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                selectCamera(0);
            }
        });
        dialog.findViewById(R.id.radio_back).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                selectCamera(1);
            }
        });
        dialog.findViewById(R.id.radio_avin).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                selectCamera(2);
            }
        });
        dialog.findViewById(R.id.radio_ng).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mCamTest.setBackgroundColor(Color.RED);
                mCamTest.setTextColor(Color.YELLOW);
                mCamPass = false;
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.radio_pass).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mCamTest.setBackgroundColor(Color.GREEN);
                mCamTest.setTextColor(Color.BLACK);
                mCamPass = true;
                dialog.dismiss();
            }
        });
    }

    private void doSpkTest() {
        // set default volume
        int defvol = mAudioMan.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2;
        mAudioMan.setStreamVolume(AudioManager.STREAM_MUSIC, defvol, 0);

        // play music
        mPlayer.seekTo(20000);
        mPlayer.start();

        View view = getLayoutInflater().inflate(R.layout.dialog1, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(R.string.spk_test_title);
        builder.setView(view);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.findViewById(R.id.radio_ng).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mSpkTest.setBackgroundColor(Color.RED);
                mSpkTest.setTextColor(Color.YELLOW);
                mSpkPass = false;
                mPlayer.pause();
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.radio_pass).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mSpkTest.setBackgroundColor(Color.GREEN);
                mSpkTest.setTextColor(Color.BLACK);
                mSpkPass = true;
                mPlayer.pause();
                dialog.dismiss();
            }
        });
    }

    private void doEphTest() {
        // set default volume
        int defvol = mAudioMan.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2;
        mAudioMan.setStreamVolume(AudioManager.STREAM_MUSIC, defvol, 0);

        // play music
        mPlayer.seekTo(20000);
        mPlayer.start();

        View view = getLayoutInflater().inflate(R.layout.dialog1, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(R.string.eph_test_title);
        builder.setView(view);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.findViewById(R.id.radio_ng).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mEphTest.setBackgroundColor(Color.RED);
                mEphTest.setTextColor(Color.YELLOW);
                mEphPass = false;
                mPlayer.pause();
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.radio_pass).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mEphTest.setBackgroundColor(Color.GREEN);
                mEphTest.setTextColor(Color.BLACK);
                mEphPass = true;
                mPlayer.pause();
                dialog.dismiss();
            }
        });
    }

    private void doBklTest() {
        View view = getLayoutInflater().inflate(R.layout.dialog2, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(R.string.bkl_test_max);
        builder.setView(view);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.findViewById(R.id.radio_max).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mHandler.removeMessages(MSG_BLK_MAX);
                mHandler.removeMessages(MSG_BLK_MIN);
                setScreenBrightness(0);
                mHandler.sendEmptyMessage(MSG_BLK_MAX);
            }
        });
        dialog.findViewById(R.id.radio_min).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mHandler.removeMessages(MSG_BLK_MAX);
                mHandler.removeMessages(MSG_BLK_MIN);
                setScreenBrightness(255);
                mHandler.sendEmptyMessage(MSG_BLK_MIN);
            }
        });
        dialog.findViewById(R.id.radio_ng).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mHandler.removeMessages(MSG_BLK_MAX);
                mHandler.removeMessages(MSG_BLK_MIN);
                setScreenBrightness(127);
                mBklTest.setBackgroundColor(Color.RED);
                mBklTest.setTextColor(Color.YELLOW);
                mBklPass = false;
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.radio_pass).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mHandler.removeMessages(MSG_BLK_MAX);
                mHandler.removeMessages(MSG_BLK_MIN);
                setScreenBrightness(127);
                mBklTest.setBackgroundColor(Color.GREEN);
                mBklTest.setTextColor(Color.BLACK);
                mBklPass = true;
                dialog.dismiss();
            }
        });
    }

    private void doLedTest() {
        writeFile("/dev/apical", "led_test 1");

        View view = getLayoutInflater().inflate(R.layout.dialog3, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(R.string.led_test_title);
        builder.setView(view);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.findViewById(R.id.radio_red).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                writeFile("/dev/apical", "led_color 100");
            }
        });
        dialog.findViewById(R.id.radio_green).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                writeFile("/dev/apical", "led_color 010");
            }
        });
        dialog.findViewById(R.id.radio_orange).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                writeFile("/dev/apical", "led_color 001");
            }
        });
        dialog.findViewById(R.id.radio_ng).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mLedTest.setBackgroundColor(Color.RED);
                mLedTest.setTextColor(Color.YELLOW);
                dialog.dismiss();
                writeFile("/dev/apical", "led_test 0");
                mLedPass = false;
            }
        });
        dialog.findViewById(R.id.radio_pass).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mLedTest.setBackgroundColor(Color.GREEN);
                mLedTest.setTextColor(Color.BLACK);
                dialog.dismiss();
                writeFile("/dev/apical", "led_test 0");
                mLedPass = true;
            }
        });
    }

    private String genTestReport() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String title = "+-------------------------------+\r\n"
                     + " test report for tndt80b device  \r\n"
                     + "+-------------------------------+\r\n"
//                   + "report generate time: " + df.format(date) + "\r\n"
//                   + "device serial number: " + Build.SERIAL + "\r\n"
                     + "\r\n";
        String report = title + mGpsView + mWifiView + mBtView + mDevView + mWaveform + this.toString() + "\r\n\r\n\r\n\r\n\r\n";
        return report;
    }

    private void doSaveGps() {
        String gps = mGpsView.toString();
        gps = gps.substring(0, gps.indexOf("test result:"));
        boolean ret = writeFile("/sdcard/gpsreport.txt", gps);
        if (ret) {
            Toast.makeText(this, R.string.save_gps_done, Toast.LENGTH_LONG).show();
        }
    }

    private void doSaveReport() {
//      boolean ret1 = takeScreenShot("/sdcard/testreport.png");
        boolean ret2 = writeFile("/sdcard/testreport.txt", genTestReport());
//      if (ret1 && ret2) {
        if (ret2) {
            Toast.makeText(this, R.string.save_report_done, Toast.LENGTH_LONG).show();
        }
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.bt_view:
                mBtView.rescan();
                break;
            case R.id.btn_cam_test:
                doCamTest();
                break;
            case R.id.btn_spk_test:
                doSpkTest();
                break;
            case R.id.btn_hp_test:
                doEphTest();
                break;
            case R.id.btn_bkl_test:
                doBklTest();
                break;
            case R.id.btn_led_test:
                doLedTest();
                break;
            case R.id.btn_save_gps:
                doSaveGps();
                break;
            case R.id.btn_save_report:
                doSaveReport();
                break;
            case R.id.btn_exit:
                finish();
                break;
            case R.id.camera_view:
                mCamIdx++; mCamIdx %= 3;
                selectCamera(mCamIdx);
                break;
            }
        }
    };

    private void updateKeyColor(View view, int result) {
        switch (result) {
        case 0:  view.setBackgroundColor(Color.RED   ); break;
        case 3:  view.setBackgroundColor(Color.GREEN ); break;
        default: view.setBackgroundColor(Color.YELLOW); break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mResultHome == 3 && mResultPower == 3 && mResultVolDec == 3 && mResultVolInc == 3) {
            return super.onKeyDown(keyCode, event);
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_HOME:
            mResultHome   |= (1 << 0);
            updateKeyColor(mTxtHome, mResultHome);
            break;

        case KeyEvent.KEYCODE_POWER:
            mResultPower  |= (1 << 0);
            updateKeyColor(mTxtPower, mResultPower);
            break;

        case KeyEvent.KEYCODE_VOLUME_DOWN:
            mResultVolDec |= (1 << 0);
            updateKeyColor(mTxtVolDec, mResultVolDec);
            break;

        case KeyEvent.KEYCODE_VOLUME_UP:
            mResultVolInc |= (1 << 0);
            updateKeyColor(mTxtVolInc, mResultVolInc);
            break;
        }

        if (mResultHome == 3 && mResultPower == 3 && mResultVolDec == 3 && mResultVolInc == 3) {
            sendBroadcast(new Intent("com.apical.testhwbutton.disable"));
            mKeyTest.setBackgroundColor(0x3300ff00);
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mResultHome == 3 && mResultPower == 3 && mResultVolDec == 3 && mResultVolInc == 3) {
            return super.onKeyUp(keyCode, event);
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_HOME:
            mResultHome |= (1 << 1);
            updateKeyColor(mTxtHome, mResultHome);
            break;

        case KeyEvent.KEYCODE_POWER:
            mResultPower |= (1 << 1);
            updateKeyColor(mTxtPower, mResultPower);
            break;

        case KeyEvent.KEYCODE_VOLUME_DOWN:
            mResultVolDec |= (1 << 1);
            updateKeyColor(mTxtVolDec, mResultVolDec);
            break;

        case KeyEvent.KEYCODE_VOLUME_UP:
            mResultVolInc |= (1 << 1);
            updateKeyColor(mTxtVolInc, mResultVolInc);
            break;
        }

        if (mResultHome == 3 && mResultPower == 3 && mResultVolDec == 3 && mResultVolInc == 3) {
            sendBroadcast(new Intent("com.apical.testhwbutton.disable"));
            mKeyTest.setBackgroundColor(0x3300ff00);
        }
        return true;
    }

    private static final int MSG_REFREH  = 1;
    private static final int MSG_BLK_MAX = 2;
    private static final int MSG_BLK_MIN = 3;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_REFREH: {
                    mHandler.sendEmptyMessageDelayed(MSG_REFREH, 200);
                    mGpsView .invalidate();
                    mWifiView.invalidate();
                    mBtView  .invalidate();
                    if (mWaveform.isPass()) {
                        mMicTest.setBackgroundColor(0x3300ff00);
                    }
                    mSaveReport.setEnabled(!genTestReport().contains("NG"));
                }
                break;
            case MSG_BLK_MAX: {
                    int bkl = getScreenBrightness();
                    bkl += 10;
                    bkl  = bkl < 255 ? bkl : 255;
                    setScreenBrightness(bkl);
                    if (bkl != 255) mHandler.sendEmptyMessageDelayed(MSG_BLK_MAX, 33);
                }
                break;
            case MSG_BLK_MIN: {
                    int bkl = getScreenBrightness();
                    bkl -= 10;
                    bkl  = bkl > 0 ? bkl : 0;
                    setScreenBrightness(bkl);
                    if (bkl != 0  ) mHandler.sendEmptyMessageDelayed(MSG_BLK_MIN, 33);
                }
                break;
            }
        }
    };

    private void hideSystemUI(boolean b) {
        /*
        getWindow().getDecorView().setSystemUiVisibility(
                  View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        */
        if (b) {
            sendBroadcast(new Intent("com.apical.systemui.strict.enable" ));
        } else {
            sendBroadcast(new Intent("com.apical.systemui.strict.disable"));
        }
    }

    private int getScreenBrightness() {
        int value = 0;
        try {
            value = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception e) {}
        return value;
    }

    private void setScreenBrightness(int value) {
        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = value / 255f;
        getWindow().setAttributes(params);
    }

    private boolean writeFile(String file, String text) {
        boolean ret = true;
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, false);
            writer.write(text);
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) { e.printStackTrace(); ret = false; }
        }
        return ret;
    }

    private boolean takeScreenShot(String path) {
        boolean ret = true;
        View view = mGpsView.getRootView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();

        Bitmap bitmap = view.getDrawingCache();
        if (bitmap != null) {
            try {
                FileOutputStream out = new FileOutputStream(path);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch(Exception e) { e.printStackTrace(); ret = false; }
        }

        return ret;
    }

    private static void setAvinSwitchType(boolean type) {
        String  path = "/dev/apical";
        File    file = new File(path);
        String  str  = null;
        boolean flag = false;

        if (!file.exists()) return;

        try {
            FileOutputStream os = new FileOutputStream(file);
            String avin = "avin " + (type ? 1 : 0);
            os.write(avin.getBytes());
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selectCamera(int idx) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
        try {
            switch (idx) {
            case 0: setAvinSwitchType(false); mCamera  = Camera.open(1); break;
            case 1: setAvinSwitchType(false); mCamera  = Camera.open(0); break;
            case 2: setAvinSwitchType(true ); mCamera  = Camera.open(0); break;
            }
            mCamera.setPreviewDisplay(mPreview.getHolder());
            mCamera.startPreview();
        } catch (Exception e) { e.printStackTrace(); }
        mCamIdx = idx;
    }

    private SurfaceHolder.Callback mPreviewSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");
            if (mCamera != null) {
                try {
                    mCamera.setPreviewDisplay(mPreview.getHolder());
                    mCamera.startPreview();
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");
            if (mCamera != null) {
                mCamera.stopPreview();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            Log.d(TAG, "surfaceChanged");
        }
    };
}

