package com.apical.factorytest;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.view.View;
import android.util.AttributeSet;
import android.util.Log;

import java.io.*;
import java.util.*;

public class DeviceView extends View {
    private final static String TAG = "DeviceView";

    private Context        mContext;
    private SensorManager  mSensorManager;
    private StorageManager mStorageManager;
    private int            mExtSdDet;
    private int            mUsbOtgDet;
    private int            mUHostDet;
    private int            mEarphoneDet;
    private String         mBatteryStatus;
    private boolean        mBatteryResult;
    private float          mX, mY, mZ;

    public DeviceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext       = context;
        mSensorManager = (SensorManager ) context.getSystemService(Context.SENSOR_SERVICE );
        mStorageManager= (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        mX = mY = mZ   = Float.MIN_VALUE;

        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter1.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter1.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter1.addAction("android.hardware.usb.action.USB_STATE");
        filter1.addAction(Intent.ACTION_HEADSET_PLUG);
        mContext.registerReceiver(mBatteryReceiver, filter1);

        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(Intent.ACTION_MEDIA_EJECT  );
        filter2.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter2.addDataScheme("file");
        mContext.registerReceiver(mMediaChangeReceiver, filter2);

        mSensorManager.registerListener(mGSensorListener,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void onDestroy() {
        mContext.unregisterReceiver(mBatteryReceiver);
        mContext.unregisterReceiver(mMediaChangeReceiver);
        mSensorManager.unregisterListener(mGSensorListener);
    }

    @Override
    public String toString() {
        boolean pass = false;
        String  str = "Device test\r\n"
                    + "-----------\r\n";
        pass = mExtSdDet    == 3;
        str += "extsd   : " + (pass ? "PASS  " : "NG    ") + mExtSdDet    + "\r\n";

/*
        pass = mUsbOtgDet   == 3;
        str += "usbotg  : " + (pass ? "PASS  " : "NG    ") + mUsbOtgDet   + "\r\n";
*/
        pass = mUHostDet   == 3;
        str += "uhost   : " + (pass ? "PASS  " : "NG    ") + mUHostDet    + "\r\n";

        pass = mEarphoneDet == 3;
        str += "hpdet   : " + (pass ? "PASS  " : "NG    ") + mEarphoneDet + "\r\n";

        pass = mX != Float.MIN_VALUE || mY != Float.MIN_VALUE || mZ != Float.MIN_VALUE;
        str += "gsensor : " + (pass ? "PASS  " : "NG    ") + String.format("%2.1f, %2.1f, %2.1f", mX, mY, mZ) + "\r\n";

        pass = mBatteryResult;
        str += "battery : " + (pass ? "PASS  " : "NG    ") + mBatteryStatus + "\r\n";

        /*
        pass = Build.SERIAL.startsWith("T54");
        str += "uuid    : " + (pass ? "PASS  " : "NG    ") + Build.SERIAL + "\r\n";
        */

        pass = getMemorySize() > 512 * 1024 * 1024;
        str += "ddr     : " + (pass ? "PASS  " : "NG    ") + String.format("%.1f", getMemorySize() / 1024 / 1024f) + " MB\r\n";

        pass = getFlashSize() > 8 * 1024 * 1024 * 1024;
        str += "flash   : " + (pass ? "PASS  " : "NG    ") + String.format("%.1f", getFlashSize() / 1024 / 1024 / 1024f) + " GB\n\n";
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
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));

        boolean result = true;

        // draw title
        String title = mContext.getString(R.string.device_test);
        canvas.drawText(title, 2, 16, paint);

        paint.setTextSize(15);
        String sdtest = mContext.getString(R.string.extsd_test);
        if (mExtSdDet == 3) {
            paint.setColor(Color.rgb(0, 255, 0));
            sdtest += " " + mContext.getString(R.string.test_pass);
        } else if (mExtSdDet == 1) {
            paint.setColor(Color.rgb(255, 255, 0));
            sdtest += " " + mContext.getString(R.string.extsd_eject);
            result = false;
        } else {
            paint.setColor(Color.rgb(255, 0, 0));
            sdtest += " " + mContext.getString(R.string.extsd_insert);
            result = false;
        }
        canvas.drawText(sdtest, 2, 25 + 25 * 1, paint);

/*
        String otgtest = mContext.getString(R.string.usbotg_test);
        if (mUsbOtgDet == 3) {
            paint.setColor(Color.rgb(0, 255, 0));
            otgtest += " " + mContext.getString(R.string.test_pass);
        } else if (mUsbOtgDet == 1) {
            paint.setColor(Color.rgb(255, 255, 0));
            otgtest += " " + mContext.getString(R.string.otg_eject);
            result = false;
        } else {
            paint.setColor(Color.rgb(255, 0, 0));
            otgtest += " " + mContext.getString(R.string.otg_insert);
            result = false;
        }
        canvas.drawText(otgtest, 2, 25 + 25 * 2, paint);
*/
        String uhosttest = mContext.getString(R.string.uhost_test);
        if (mUHostDet == 3) {
            paint.setColor(Color.rgb(0, 255, 0));
            uhosttest += " " + mContext.getString(R.string.test_pass);
        } else if (mUHostDet == 1) {
            paint.setColor(Color.rgb(255, 255, 0));
            uhosttest += " " + mContext.getString(R.string.uhost_eject);
            result = false;
        } else {
            paint.setColor(Color.rgb(255, 0, 0));
            uhosttest += " " + mContext.getString(R.string.uhost_insert);
            result = false;
        }
        canvas.drawText(uhosttest, 2, 25 + 25 * 2, paint);

        String ephtest = mContext.getString(R.string.ephdet_test);
        if (mEarphoneDet == 3) {
            paint.setColor(Color.rgb(0, 255, 0));
            ephtest += " " + mContext.getString(R.string.test_pass);
        } else if (mEarphoneDet == 1) {
            paint.setColor(Color.rgb(255, 255, 0));
            ephtest += " " + mContext.getString(R.string.eph_eject);
            result = false;
        } else {
            paint.setColor(Color.rgb(255, 0, 0));
            ephtest += " " + mContext.getString(R.string.eph_insert);
            result = false;
        }
        canvas.drawText(ephtest, 2, 25 + 25 * 3, paint);

        String gsensortest = mContext.getString(R.string.gsensor_test);
        if (mX == Float.MIN_VALUE && mY == Float.MIN_VALUE && mZ == Float.MIN_VALUE) {
            paint.setColor(Color.rgb(255, 0, 0));
            gsensortest += " NG";
            result = false;
        } else {
            paint.setColor(Color.rgb(0, 255, 0));
            gsensortest += String.format(" %2.1f, %2.1f, %2.1f", mX, mY, mZ);
        }
        canvas.drawText(gsensortest, 2, 25 + 25 * 4, paint);

        String battest = mContext.getString(R.string.battery_test) + " " + mBatteryStatus;
        if (mBatteryResult) {
            paint.setColor(Color.rgb(0, 255, 0));
        } else {
            paint.setColor(Color.rgb(255, 255, 0));
//          result = false;
        }
        canvas.drawText(battest, 2, 25 + 25 * 5, paint);

        /*
        String uuidtest = mContext.getString(R.string.uuid_test) + " " + Build.SERIAL;
        if (Build.SERIAL.startsWith("T54")) {
            paint.setColor(Color.rgb(0, 255, 0));
        } else {
            paint.setColor(Color.rgb(255, 0, 0));
            result = false;
        }
        canvas.drawText(uuidtest, 2, 25 + 25 * 6, paint);
        */

        String ddrtest = mContext.getString(R.string.ddr_test) + " " + String.format("%.1f", getMemorySize() / 1024 / 1024f) + " MB";
        if (getMemorySize() > 512 * 1024 * 1024) {
            paint.setColor(Color.rgb(0, 255, 0));
        } else {
            paint.setColor(Color.rgb(255, 0, 0));
            result = false;
        }
        canvas.drawText(ddrtest, 2, 25 + 25 * 6, paint);

        String flashtest = mContext.getString(R.string.flash_test) + " " + String.format("%.1f", getFlashSize() / 1024 / 1024 / 1024f) + " GB";
        if (getFlashSize() > 8 * 1024 * 1024 * 1024) {
            paint.setColor(Color.rgb(0, 255, 0));
        } else {
            paint.setColor(Color.rgb(255, 0, 0));
            result = false;
        }
        canvas.drawText(flashtest, 2, 25 + 25 * 7, paint);

        String wifimac = ((WifiManager)mContext.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getMacAddress().toLowerCase();
        String btmac   = BluetoothAdapter.getDefaultAdapter().getAddress().toLowerCase();
        boolean wifimacpass = wifimac.startsWith("90:f4:c1:1a");
        boolean btmacpass   = wifimac.startsWith("90:f4:c1:1a");
        String wifimactest = mContext.getString(R.string.wifimac_test) + " " + (wifimacpass ? "PASS  " : "NG   ");
        if (wifimacpass) {
            paint.setColor(Color.rgb(0, 255, 0));
        } else {
            paint.setColor(Color.rgb(255, 0, 0));
            result = false;
        }
        canvas.drawText(wifimactest, 2, 25 + 25 * 8, paint);

        String btmactest = mContext.getString(R.string.btmac_test) + " " + (btmacpass ? "PASS  " : "NG   ");
        if (btmacpass) {
            paint.setColor(Color.rgb(0, 255, 0));
        } else {
            paint.setColor(Color.rgb(255, 0, 0));
            result = false;
        }
        canvas.drawText(btmactest, 2, 25 + 25 * 9, paint);

        paint.setColor(Color.rgb(255, 255, 0));
        paint.setTextSize(16);
        canvas.drawText(mContext.getString(R.string.test_standard), 2, 25 + 25 * 11, paint);
        paint.setTextSize(15);
        canvas.drawText(mContext.getString(R.string.standard1), 2, 25 + 25 * 12, paint);
        canvas.drawText(mContext.getString(R.string.standard2) + WifiView.TEST_PASS_STD, 2, 25 + 25 * 13, paint);
        canvas.drawText(mContext.getString(R.string.standard3) + BtView  .TEST_PASS_STD, 2, 25 + 25 * 14, paint);
        canvas.drawText(mContext.getString(R.string.version), 2, 25 + 25 * 15, paint);

        if (result) setBackgroundColor(0x3300ff00);
    }

    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int     status      = intent.getIntExtra("status", 0);
                int     health      = intent.getIntExtra("health", 0);
                boolean present     = intent.getBooleanExtra("present", false);
                int     level       = intent.getIntExtra("level", 0);
                int     scale       = intent.getIntExtra("scale", 0);
                int     icon_small  = intent.getIntExtra("icon-small", 0);
                int     plugged     = intent.getIntExtra("plugged", 0);
                int     voltage     = intent.getIntExtra("voltage", 0);
                int     temperature = intent.getIntExtra("temperature", 0);
                String  technology  = intent.getStringExtra("technology");

                String statusString = "";
                switch (status) {
                case BatteryManager.BATTERY_STATUS_UNKNOWN:
                    statusString = "unknown";
                    break;
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    statusString = "charging";
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    statusString = "discharging";
                    break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    statusString = "not charging";
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                    statusString = "full";
                    break;
                }

                String acString = "none";
                switch (plugged) {
                case BatteryManager.BATTERY_PLUGGED_AC:
                    acString = "ac";
                    break;
                case BatteryManager.BATTERY_PLUGGED_USB:
                    acString = "usb";
                    break;
                case 3:
                    acString = "usb-charger";
                    break;
                }

                if (present) {
                    mBatteryStatus = statusString + " "
                      + String.valueOf(level) + "% "
                      + String.valueOf(temperature/10.0) + "'C";
                } else {
                    mBatteryStatus = "not present";
                }
                mBatteryResult = temperature <= 550;
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                mUHostDet |= (1 << 0);
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                mUHostDet |= (1 << 1);
            } else if (action.equals("android.hardware.usb.action.USB_STATE")) {
                if (intent.getExtras().getBoolean("connected")) {
                    mUsbOtgDet |= (1 << 0);
                } else {
                    mUsbOtgDet |= (1 << 1);
                }
            } else if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                if (intent.getIntExtra("state", 0) == 1) {
                    mEarphoneDet |= (1 << 0);
                } else {
                    mEarphoneDet |= (1 << 1);
                }
            }
            invalidate();
        }
    };

    private BroadcastReceiver mMediaChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final Uri uri = intent.getData();
            String   path = uri.getPath();

            if (  action.equals(Intent.ACTION_MEDIA_EJECT)
               || action.equals(Intent.ACTION_MEDIA_UNMOUNTED) ) {
                Log.i(TAG, "Intent.ACTION_MEDIA_EJECT path = " + path);
                if (path.contains("extsd")) {
                    mExtSdDet |= (1 << 1);
                } else if (path.contains("usbhost")) {
                    mUHostDet |= (1 << 1);
                }
                invalidate();
            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                Log.i(TAG, "Intent.ACTION_MEDIA_MOUNTED = " + path);
                if (path.contains("extsd")) {
                    mExtSdDet |= (1 << 0);
                } else if (path.contains("usbhost")) {
                    mUHostDet |= (1 << 0);
                }
                invalidate();
            }
        }
    };

    private SensorEventListener mGSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                mX = sensorEvent.values[0];
                mY = sensorEvent.values[1];
                mZ = sensorEvent.values[2];
                invalidate();
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private long getMemorySize() {
        String   str   = null;
        String[] strs  = null;
        long     total = 0;
        try {
            FileReader     fr = new FileReader("/proc/meminfo");
            BufferedReader br = new BufferedReader(fr, 8192);
            str   = br.readLine();
            strs  = str.split("\\s+");
            total = Integer.valueOf(strs[1]).intValue() * 1024;
            br.close();
            fr.close();
        } catch (IOException e) {}

        return total;
    }

    private long getFlashSize() {
        StorageVolume[] vollist = mStorageManager.getVolumeList();
        StorageVolume   volume  = vollist[0];
        final StatFs stat       = new StatFs(volume != null ? volume.getPath() : Environment.getDataDirectory().getPath());
        final long   blocksize  = stat.getBlockSize();
        final long   blocknum   = stat.getBlockCount();
        final long   total      = blocknum * blocksize;
        return total;
    }
};
