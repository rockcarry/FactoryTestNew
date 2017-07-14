package com.apical.factorytest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.view.View;
import android.util.AttributeSet;

import java.util.*;

public class WifiView extends View {
    public final static int TEST_PASS_STD = -65;
    private Context     mContext;
    private WifiManager mWifiManager;
    private WifiInfo    mWifiInfo;
    private String      mWifiMacAddr;
    private int         mCurMaxLevel;
    private int         mScanProgress;

    public WifiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager != null) {
            mWifiManager.setWifiEnabled(true);
            mWifiManager.startScan();
            mWifiInfo    = mWifiManager.getConnectionInfo();
            mWifiMacAddr = mWifiInfo.getMacAddress();
        }
        mCurMaxLevel = Integer.MIN_VALUE;
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void onDestroy() {
    }

    @Override
    public String toString() {
        boolean pass = false;
        String  str = "WiFi test\r\n"
                    + "---------\r\n";
        str += "mac: " + mWifiMacAddr + "\r\n";
        str += "highest signal level: " + mCurMaxLevel + "dBm\r\n";
        str += "test result: " + (mCurMaxLevel >= TEST_PASS_STD ? "PASS" : "NG");
        str += "\r\n\r\n";
        return str;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.rgb(255, 255, 0));
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1f);
        paint.setTextSize(15);
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));

        // get aplist
        List<ScanResult> aplist = mWifiManager.getScanResults();

        // update progress
        String t[] = { "|", "/", "-", "\\" };
        mScanProgress++; mScanProgress %= 4;

        // draw title
        String title = mContext.getString(R.string.txt_wifi_test) + " " + mWifiMacAddr;
        if (mCurMaxLevel == java.lang.Integer.MIN_VALUE) {
            paint.setColor(Color.rgb(255, 0, 0));
            title += " NG";
        } else if ( mCurMaxLevel >= -65) {
            paint.setColor(Color.rgb(0, 255, 0));
            title += " " + mCurMaxLevel;
        } else {
            paint.setColor(Color.rgb(255, 255, 0));
            title += " " + mCurMaxLevel;
        }
        title += " " + t[mScanProgress];
        canvas.drawText(title, 2, 15, paint);

        // draw aplist
        int x = 2;
        int y = 16;
        paint.setColor(Color.rgb(255, 255, 0));
        for (ScanResult sr : aplist) {
            if (mCurMaxLevel < sr.level) {
                mCurMaxLevel = sr.level;
            }

            x  = 16;
            y += 20;
            String apstr = String.format("%3ddBm - %s\n", sr.level, sr.SSID);
            canvas.drawText(apstr, x, y, paint);
        }

        int bgcolor;
        if (mCurMaxLevel == java.lang.Integer.MIN_VALUE) {
            bgcolor = Color.argb(0x33, 0xff, 0, 0);
        } else if ( mCurMaxLevel >= TEST_PASS_STD) {
            bgcolor = Color.argb(0x33, 0, 0xff, 0);
        } else {
            bgcolor = Color.argb(0x33, 0xff, 0xff, 0);
        }
        setBackgroundColor(bgcolor);
    }
};
