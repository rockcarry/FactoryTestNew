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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
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
    private DeviceView   mDevView;
    private TextView     mTxtHome;
    private TextView     mTxtPower;
    private TextView     mTxtVolDec;
    private TextView     mTxtVolInc;
    private RadioButton  mBtnFm88;
    private RadioButton  mBtnFm90;
    private RadioButton  mBtnFm92;
    private RadioButton  mBtnFmOff;
    private Button       mMicTest;
    private Button       mSpkTest;
    private Button       mEphTest;
    private Button       mBklTest;
    private Button       mSaveGps;
    private Button       mSaveReport;
    private Button       mBtnExit;
    private int          mResultHome;
    private int          mResultPower;
    private int          mResultVolDec;
    private int          mResultVolInc;
    private boolean      mMicPass;
    private boolean      mSpkPass;
    private boolean      mEphPass;
    private boolean      mBklPass;
    private MediaPlayer  mPlayer;
    private AudioManager mAudioMan;
    private SurfaceView  mPreview;

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
        str += "mic test      : " + (mMicPass ? "PASS" : "NG") + "\r\n";
        str += "speaker test  : " + (mSpkPass ? "PASS" : "NG") + "\r\n";
        str += "earphone test : " + (mEphPass ? "PASS" : "NG") + "\r\n";
        str += "backlight test: " + (mBklPass ? "PASS" : "NG") + "\r\n";
        return str;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mGpsView   = (GpsView     )findViewById(R.id.gps_view       );
        mWifiView  = (WifiView    )findViewById(R.id.wifi_view      );
        mDevView   = (DeviceView  )findViewById(R.id.device_view    );
        mBtnFm88   = (RadioButton )findViewById(R.id.btn_fm_88mhz   );
        mBtnFm90   = (RadioButton )findViewById(R.id.btn_fm_90mhz   );
        mBtnFm92   = (RadioButton )findViewById(R.id.btn_fm_92mhz   );
        mBtnFmOff  = (RadioButton )findViewById(R.id.btn_fm_off     );
        mMicTest   = (Button      )findViewById(R.id.btn_mic_test   );
        mSpkTest   = (Button      )findViewById(R.id.btn_spk_test   );
        mEphTest   = (Button      )findViewById(R.id.btn_hp_test    );
        mBklTest   = (Button      )findViewById(R.id.btn_bkl_test   );
        mSaveGps   = (Button      )findViewById(R.id.btn_save_gps   );
        mSaveReport= (Button      )findViewById(R.id.btn_save_report);
        mBtnExit   = (Button      )findViewById(R.id.btn_exit       );

        mBtnFm88   .setOnClickListener(mOnClickListener);
        mBtnFm90   .setOnClickListener(mOnClickListener);
        mBtnFm92   .setOnClickListener(mOnClickListener);
        mBtnFmOff  .setOnClickListener(mOnClickListener);
        mMicTest   .setOnClickListener(mOnClickListener);
        mSpkTest   .setOnClickListener(mOnClickListener);
        mEphTest   .setOnClickListener(mOnClickListener);
        mBklTest   .setOnClickListener(mOnClickListener);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // stop test view refresh
        mHandler.removeMessages(MSG_REFREH);

        mGpsView .onDestroy();
        mWifiView.onDestroy();
        mDevView .onDestroy();
        mPlayer.release();
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideSystemUI(true);

        Settings.System.putInt(getContentResolver(), Settings.System.SHOW_TOUCHES    , 1);
        Settings.System.putInt(getContentResolver(), Settings.System.POINTER_LOCATION, 1);

        mGpsView .onResume();
        mWifiView.onResume();
        mDevView .onResume();

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
        mDevView .onPause();

        // disable hardware button test mode
        sendBroadcast(new Intent("com.apical.testhwbutton.disable"));
    }

    private void doMicTest() {
        View view = getLayoutInflater().inflate(R.layout.dialog5, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(R.string.mic_test_title);
        builder.setView(view);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.findViewById(R.id.radio_record).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                RecordTestStart();
            }
        });
        dialog.findViewById(R.id.radio_playback).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                RecordTestPlayback();
            }
        });
        dialog.findViewById(R.id.radio_ng).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mMicTest.setBackgroundColor(Color.RED);
                mMicTest.setTextColor(Color.YELLOW);
                mMicPass = false;
                dialog.dismiss();
                RecordTestStop();
            }
        });
        dialog.findViewById(R.id.radio_pass).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                mMicTest.setBackgroundColor(Color.GREEN);
                mMicTest.setTextColor(Color.BLACK);
                mMicPass = true;
                dialog.dismiss();
                RecordTestStop();
            }
        });
    }

    private void doSpkTest() {
        // set default volume
        int defvol = mAudioMan.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
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
        int defvol = mAudioMan.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
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

    private String genTestReport() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String title = "+-------------------------------+\r\n"
                     + " test report for a33-213 device  \r\n"
                     + "+-------------------------------+\r\n"
//                   + "report generate time: " + df.format(date) + "\r\n"
//                   + "device serial number: " + Build.SERIAL + "\r\n"
                     + "\r\n";
        String report = title + mGpsView + mWifiView + mDevView + this.toString() + "\r\n\r\n\r\n\r\n\r\n";
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
            case R.id.btn_fm_88mhz:
                setFmTxFreq(880);
                break;
            case R.id.btn_fm_90mhz:
                setFmTxFreq(900);
                break;
            case R.id.btn_fm_92mhz:
                setFmTxFreq(920);
                break;
            case R.id.btn_fm_off:
                setFmTxFreq(000);
                break;
            case R.id.btn_mic_test:
                doMicTest();
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
            case R.id.btn_save_gps:
                doSaveGps();
                break;
            case R.id.btn_save_report:
                doSaveReport();
                break;
            case R.id.btn_exit:
                finish();
                break;
            }
        }
    };

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

    private static final String TEST_RECORD_AUDIO_FILE = "/sdcard/factorytest.m4a";
    private MediaPlayer   mAudioPlayer;
    private MediaRecorder mAudioRecorder;
    private void RecordTestStart() {
        RecordTestStop();
        mAudioRecorder = new MediaRecorder();
        mAudioRecorder.setAudioSource (MediaRecorder.AudioSource.MIC);
        mAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mAudioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mAudioRecorder.setOutputFile  (TEST_RECORD_AUDIO_FILE);
        try {
            mAudioRecorder.prepare();
            mAudioRecorder.start ();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void RecordTestPlayback() {
        RecordTestStop();

        // set default volume
        int defvol = mAudioMan.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mAudioMan.setStreamVolume(AudioManager.STREAM_MUSIC, defvol, 0);

        mAudioPlayer = new MediaPlayer();
        try {
            mAudioPlayer.setDataSource(TEST_RECORD_AUDIO_FILE);
            mAudioPlayer.prepare();
            mAudioPlayer.start();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void RecordTestStop() {
        if (mAudioPlayer != null) {
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
        if (mAudioRecorder != null) {
            mAudioRecorder.release();
            mAudioRecorder = null;
        }
    }

    private void setFmTxFreq(int freq) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("fmtx_state_value", freq != 0);
        bundle.putInt    ("fmtx_freq_value" , freq);
        Intent intent = new Intent("android.fm.action.set_fmtx_state");
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }
}

