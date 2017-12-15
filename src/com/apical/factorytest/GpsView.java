package com.apical.factorytest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.view.View;
import android.util.AttributeSet;
import android.util.Log;

import java.util.*;

public class GpsView extends View {
    private Context         mContext;
    private LocationManager mLocManager;
    private GpsStatus       mGpsStatus;
    private int             mProgress;
    private boolean         mGpsFixed;
    private List<Integer>   mSnrList;
    private int             mCurMax;

    public GpsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mSnrList = new ArrayList();
        mCurMax  = 0;

        mLocManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mLocManager.addGpsStatusListener(mGpsStatusListener);
        mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, mLocationListener);
    }

    public void onResume() {
    }

    public void onPause() {
    }

    protected void onDestroy() {
        mLocManager.removeUpdates(mLocationListener);
        mLocManager.removeGpsStatusListener(mGpsStatusListener);
    }

    @Override
    public String toString() {
        boolean pass = false;
        String  str = "GPS test\r\n"
                    + "--------\r\n";
        str += "highest satellite snr:";
        for (int i=0; i<mSnrList.size(); i++) {
            str += " " + mSnrList.get(i);
        }
        str += "\r\n";
        if (  mSnrList.size() >= 3
           && mSnrList.get(0) >= 45
           && mSnrList.get(1) >= 45
           && mSnrList.get(2) >= 43 ) {
            pass = true;
        }
        str += "test result: " + (pass ? "PASS" : "NG");
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

        int cw = getMeasuredWidth ();
        int ch = getMeasuredHeight();
        int sw = cw / 16;
        int sh = ch - 62;

        paint.setColor(Color.rgb(128, 128, 0));
        canvas.drawLine(20, ch - 20, cw - 20, ch - 20, paint);
        canvas.drawLine(20, ch - 20, 20, ch - 25, paint);
        canvas.drawLine(cw - 20, ch - 20, cw - 20, ch - 25, paint);
        for (int i=1; i<15; i++) {
            canvas.drawLine(20 + i * sw, ch - 20, 20 + i * sw, ch - 23, paint);
        }

        List<Integer> snrlist = new ArrayList();
        if (mGpsStatus != null) {
            Iterator<GpsSatellite> satellites = mGpsStatus.getSatellites().iterator();
            if (satellites != null) {
                int i = 0;
                while (satellites.hasNext() && i<15) {
                    GpsSatellite s = satellites.next();
                    int snr = (int)s.getSnr();
                    int prn = (int)s.getPrn();
                    if (mGpsFixed) {
                        if (snr < 10) {
                            paint.setColor(0xffff0000);
                        } else if (snr < 22) {
                            paint.setColor(0xffff8800);
                        } else if (snr < 38) {
                            paint.setColor(0xffffff00);
                        } else {
                            paint.setColor(0xff00ff00);
                        }
                    } else {
                        paint.setColor(0xff777777);
                    }
                    canvas.drawRect(20 + i * sw + 1, ch - 20 - 1 - snr * sh / 52, 20 + (i + 1) * sw - 1, ch - 20 - 1, paint);
                    String strprn = String.format("%02d", prn);
                    String strsnr = String.format("%d"  , snr);
                    paint.setColor(Color.rgb(188, 188, 0));
                    canvas.drawText(strprn, 20 + i * sw + (sw - paint.measureText(strprn)) / 2, ch - 20 + 16, paint);
                    canvas.drawText(strsnr, 20 + i * sw + (sw - paint.measureText(strsnr)) / 2, ch - 20 - 1 - snr * sh / 52 - 10, paint);
                    i++;
                    snrlist.add(snr);
                }
            }
        }

        Comparator cmp = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        };
        Collections.sort(snrlist, cmp);
        int max = 0;
        for (int i=0; i<snrlist.size() && i<6; i++) {
            max += snrlist.get(i);
        }
        if (mCurMax < max) {
            mCurMax  = max;
            mSnrList = snrlist;
        }

        int pass = 0;
        if (  mSnrList.size() >= 3
           && mSnrList.get(0) >= 45
           && mSnrList.get(1) >= 45
           && mSnrList.get(2) >= 43) {
             pass = 2;
        } else if (
              mSnrList.size() >= 2
           && mSnrList.get(0) >= 45
           && mSnrList.get(1) >= 42) {
             pass = 1;
        }

        // draw title
        String t[] = { "|", "/", "-", "\\" };
        mProgress++; mProgress %= 4;
        String title = mContext.getString(R.string.txt_gps_test) + " " + t[mProgress];
        for (int i=0; i<mSnrList.size(); i++) {
            title += " " + mSnrList.get(i);
        }
        if (mCurMax == 0) {
            paint.setColor(Color.rgb(255, 0, 0));
            title += " NG";
        } else if (pass == 1) {
            paint.setColor(Color.rgb(255, 255, 0));
        } else if (pass == 2) {
            paint.setColor(Color.rgb(0, 255, 0));
        } else {
            paint.setColor(Color.rgb(255, 255, 0));
        }
        canvas.drawText(title, 2, 15, paint);

        int bgcolor;
        if (mCurMax == 0) {
            bgcolor = Color.argb(0x33, 0xff, 0, 0);
        } else if (pass == 1) {
            bgcolor = Color.argb(0x55, 0x5f, 0x5f, 0xff);
        } else if (pass == 2) {
            bgcolor = Color.argb(0x33, 0, 0xff, 0);
        } else {
            bgcolor = Color.argb(0x33, 0xff, 0xff, 0);
        }
        setBackgroundColor(bgcolor);
    }

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mGpsFixed = true;
        }
        @Override
        public void onProviderDisabled(String provider) {
            mGpsFixed = false;
        }
        @Override
        public void onProviderEnabled(String provider) {
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
            case LocationProvider.AVAILABLE:
                break;
            case LocationProvider.OUT_OF_SERVICE:
                mGpsFixed = false;
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                mGpsFixed = false;
                break;
            }
        }
    };

    GpsStatus.Listener mGpsStatusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int arg0) {
            switch (arg0) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                mGpsStatus = mLocManager.getGpsStatus(null);
                break;

            case GpsStatus.GPS_EVENT_STARTED:
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                break;
            }
        }
    };
};
