package com.apical.factorytest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.util.AttributeSet;

import java.util.*;

public class BtView extends View {
    public final static int TEST_PASS_STD = -65;

    private Context               mContext;
    private BluetoothAdapter      mBtAdapter;
    private String                mBtMacAddr;
    private String                mCurMaxName ;
    private int                   mCurMaxLevel;
    private int                   mScanProgress;
    private boolean               mScanFinish;
    private List<BluetoothDevice> mBtDevList;
    private List<Integer        > mBtLevelList;

    public BtView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext     = context;
        mBtDevList   = new ArrayList();
        mBtLevelList = new ArrayList();
        mCurMaxName  = "";
        mCurMaxLevel = java.lang.Integer.MIN_VALUE;
        mBtAdapter   = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter != null) {
            mScanFinish = false;
            mBtAdapter.enable();
            mHandler.sendEmptyMessage(MSG_CHECK_ENABLED);

        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mReceiver, filter);
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void onDestroy() {
        mContext.unregisterReceiver(mReceiver);
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
    }

    public void rescan() {
        if (mScanFinish) {
            mScanFinish = false;
            mBtDevList  .clear();
            mBtLevelList.clear();
            mBtAdapter  .startDiscovery();
        }
    }

    @Override
    public String toString() {
        boolean pass = false;
        String  str = "Bluetooth test\r\n"
                    + "--------------\r\n";
        str += "mac: " + mBtMacAddr + "\r\n";
        str += "highest signal level: " + (mCurMaxLevel == Integer.MIN_VALUE ? "no signal" : String.format("%ddBm", mCurMaxLevel)) + " " + mCurMaxName + "\r\n";
        str += "test result: " + (mCurMaxLevel >= TEST_PASS_STD ? "PASS" : "NG");
        str += "\r\n\r\n";
        return str;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1f);
        paint.setTextSize(15);
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));

        // update progress
        String t[] = { "|", "/", "-", "\\" };
        mScanProgress++; mScanProgress %= 4;

        // draw title
        String title = mContext.getString(R.string.txt_bt_test) + " " + mBtMacAddr;
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
        if (!mScanFinish) {
            title += " " + t[mScanProgress];
        }
        canvas.drawText(title, 2, 15, paint);

        // draw aplist
        int x = 2;
        int y = 16;
        paint.setColor(Color.rgb(255, 255, 0));
        for (int i=0; i<mBtDevList.size(); i++) {
            String name = mBtDevList.get(i).getName();
            if (mCurMaxLevel < mBtLevelList.get(i) && (name != null) && name.toLowerCase().startsWith("rm-bt")) {
                mCurMaxName = mBtDevList.get(i).getName();
                mCurMaxLevel = mBtLevelList.get(i);
            }

            x  = 16;
            y += 20;
            String devstr = String.format("%3ddBm - %s\n", mBtLevelList.get(i), name);
            canvas.drawText(devstr, x, y, paint);
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

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra  (BluetoothDevice.EXTRA_DEVICE);
                int             level  = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI  );
                mBtDevList  .add(device);
                mBtLevelList.add(level );
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                mScanFinish = true;
                rescan(); // auto rescan
            }
        }
    };

    private static final int MSG_CHECK_ENABLED = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_CHECK_ENABLED: {
                    if (mBtAdapter.isEnabled()) {
                        mBtAdapter.startDiscovery();
                        mBtMacAddr = mBtAdapter.getAddress();
                    } else {
                        mHandler.sendEmptyMessageDelayed(MSG_CHECK_ENABLED, 200);
                    }
                }
                break;
            }
        }
    };
};
