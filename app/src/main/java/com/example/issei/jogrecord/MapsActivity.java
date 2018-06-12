package com.example.issei.jogrecord;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Address;
import android.location.Location;
//import android.location.LocationListener;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
//import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.BaseGmsClient;

import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;



import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener,
        LoaderManager.LoaderCallbacks<Address> { //                                               a1
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private static final int ADDRESSLOADER_ID = 0;
    //INTERVAL:500,FASTESTINTERVAL:16 で綺麗な線が描けた
    private static final int INTERVAL = 500;
    private static final int FASTESTINTERVAL = 16;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(INTERVAL) //位置情報の更新間隔をミリ秒で設定                                 a2
            .setFastestInterval(FASTESTINTERVAL)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //位置情報取得要求の優先順位
    private FusedLocationProviderApi mFusedLocationProviderApi = LocationServices.FusedLocationApi;
    private List<LatLng> mRunList = new ArrayList<LatLng>();
    private WifiManager mWifi;
    private boolean mWifiOff = false;
    private long mStartTimeMillis;
    private double mMeter = 0.0;
    private double mElapsedTime = 0.0;
    private double mSpeed = 0.0;
    private DatabaseHelper mDbHelper;
    private boolean mStart = false;
    private boolean mFirst = false;
    private boolean mStop = false;
    private boolean mAsked = false;
    private Chronometer mChronometer;

    @Override
    protected void onSaveInstanceState(Bundle outState) {       //                                a3
        super.onSaveInstanceState(outState);
        //メンバー変数が初期化されることへの対処
        outState.putBoolean("ASKED", mAsked);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) { //                         a4
        super.onRestoreInstanceState(savedInstanceState);
        mAsked = savedInstanceState.getBoolean("ASKED");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        //画面をスリープにしない
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mGoogleApiClient = new GoogleApiClient.Builder(this) //                           a5
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); //                                      a6

        mDbHelper = new DatabaseHelper(this);    //                                       a7
        ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButton);
        tb.setChecked(false);   //OFFへ変更

        //ToggleのCheckが変更したタイミングで呼び出されるリスナー
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //トグルキーが変更された際に呼び出される
                if (isChecked) {  //                                                              a8
                    startChronometer();
                    mStart = true;
                    mFirst = true;
                    mStop = false;
                    mMeter = 0.0;
                    mRunList.clear();
                } else {
                    stopChronometer();
                    mStop = true;
                    calcSpeed();
                    saveConfirm();
                    mStart = false;
                    Log.v("aaaa","mstart = false");
                }
            }
        });
    }

    private void startChronometer() {
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        //電源ON時からの経過時間の値をベースに
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.start();
        mStartTimeMillis = System.currentTimeMillis();
    }

    private void stopChronometer() {
        mChronometer.stop();
        //ミリ秒
        mElapsedTime = SystemClock.elapsedRealtime() - mChronometer.getBase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mAsked) {  //                                                                        a9
            wifiConfirm();
            mAsked = !mAsked;
        }
        mGoogleApiClient.connect();  //                                                          a10
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latlng) {  //                                      a11
                Intent intent = new Intent(MapsActivity.this, JogView.class);
                startActivity(intent);
            }
        });

        //DangerousなPermissionはリクエストして許可をもらわないと使えない
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) { //                                         a12
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                //一度拒否された時、Rationale（論理的根拠）を説明して、再度許可ダイアログを出すようにする
                new android.app.AlertDialog.Builder(this)
                        .setTitle("許可が必要です")
                        .setMessage("移動に合わせて地図を動かすためには、ACCESS_FINE_LOCATIONを許可してください")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //OK button pressed
                                requestAccessFineLocation();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showToast("GPS機能が使えないので、地図は動きません");
                            }
                        })
                        .show();
            } else {
                //まだ許可を求める前の時、許可を求めるダイアログを表示します。
                requestAccessFineLocation();
            }
        }
    }

    private void requestAccessFineLocation() {  //                                              a13
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {  //                                                                    a14
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                //ユーザーが許可したとき
                //許可が必要な機能を改めて実行する
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    //ユーザーが許可しなかったとき
                    //許可されなかったため機能が実行できないことを表示する
                    showToast("GPS機能が使えないので、地図は動きません");
                    //以下は、java.lang.RuntimeExceptionになる
                    //mMap.setMyLocationEnabled(true);
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                //ユーザーが許可したとき
                //許可が必要な機能を改めて実行する
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //
                } else {
                    //ユーザーが許可しなかったとき
                    //許可されなかったため機能が実行できないことを表示する
                    showToast("外部へのファイルの保存が許可されなかったので、記録できません");
                }
                return;
            }
        }
    }

    private void wifiConfirm() {
        mWifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        if (mWifi.isWifiEnabled()) {
            wifiConfirmDialog();
        }
    }

    private void wifiConfirmDialog() {
        DialogFragment newFragment = WifiConfirmDialogFragment.newInstance(
                R.string.wifi_confirm_dialog_title, R.string.wifi_confirm_dialog_message);
        newFragment.show(getFragmentManager(), "dialog");
    }

    public void wifiOff() {
        mWifi.setWifiEnabled(false);
        mWifiOff = true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, REQUEST, this); //    a15
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v("aaaa","onLocationChanged");
        //stop後は動かさない
        if (mStop) {
            return;
        }
        CameraPosition cameraPos = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(19)
                .bearing(0).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPos)); //                 a16

        //マーカー設定
        mMap.clear();
        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions options = new MarkerOptions();
        options.position(latlng);
        //ランチャーアイコン
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher);
        options.icon(icon);
        mMap.addMarker(options);  //                                                             a17

        if (mStart) {
            if (mFirst) {
                Bundle args = new Bundle();
                args.putDouble("lat", location.getLatitude());
                args.putDouble("lon", location.getLongitude());

                getLoaderManager().restartLoader(ADDRESSLOADER_ID, args, this); //      a18
                mFirst = !mFirst;
                Log.v("aaaa","mFirst = " + mFirst);
            } else {
                //移動線を描画
                drawTrace(latlng);
                //走行距離を累積
                sumDistance();
            }
        }
    }

    private void drawTrace(LatLng latlng) {  //                                                  a19
        mRunList.add(latlng);
        if (mRunList.size() > 2) {
            PolylineOptions polyOptions = new PolylineOptions();
            for (LatLng polyLatLng : mRunList) {
                polyOptions.add(polyLatLng);
            }
            polyOptions.color(Color.BLUE);
            polyOptions.width(3);
            polyOptions.geodesic(false);
            mMap.addPolyline(polyOptions);
        }
    }

    private void sumDistance() {    //                                                           a20
        if (mRunList.size() < 2) {
            return;
        }
        mMeter = 0;
        float[] results = new float[3];
        int i = 1;
        while (i < mRunList.size()) {
            results[0] = 0;
            Location.distanceBetween(mRunList.get(i - 1).latitude, mRunList.get(i - 1).longitude, mRunList.get(i).latitude, mRunList.get(i).longitude, results);
            mMeter += results[0];
            i++;
        }
        //distanceBetweenの距離はメートル単位
        double disMeter = mMeter / 1000;
        TextView disText = (TextView) findViewById(R.id.disText);
        disText.setText(String.format("%.2f" + " km", disMeter));
    }

    private void calcSpeed() {  //                                                               a21
        sumDistance();
        mSpeed = (mMeter / 1000) / (mElapsedTime / 1000) * 60 * 60;

    }

    private void saveConfirm() {  //                                                             a22
        //DangerousなPermissionはリクエストして許可をもらわないと使えない
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                //一度拒否されたとき、Rationale（理論的根拠）を説明して、再度許可ダイアログを出すようにする
                new android.app.AlertDialog.Builder(this)
                        .setTitle("許可が必要です")
                        .setMessage("ジョギングの記録を保存するためには、WRITE_EXTERNAL_STORAGEを許可してください")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //ok button pressed
                                requestWriteExternalStorage();
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showToast("外部ファイルへの保存が許可されなかったので、記録できません");
                            }
                        })
                        .show();
            } else {
                //まだ許可を求める前のとき、許可を求めるダイアログを表示します。
                requestWriteExternalStorage();
            }
        } else {
            saveConfirmDialog();
        }
    }
    private void requestWriteExternalStorage(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
    }
    private void saveConfirmDialog(){     //                                                     a23
        Log.v("aaaa","saveConfirmDialog");
        String message = "時間：";
        TextView disText = (TextView) findViewById(R.id.disText);

        message = message + mChronometer.getText().toString() + " " +
                "距離" + disText.getText() + "\n" +
                "時速" + String.format("%.2f" + " km", mSpeed);
        Log.v("aaaa","message = " + message);

        DialogFragment newFragment = SaveConfirmDialogFragment.newInstance(
                R.string.save_confirm_dialog_title, message);
        Log.v("aaaa","newFragment = " + newFragment);

        newFragment.show(getFragmentManager(),"dialog");
        Log.v("aaaa","到達");
    }

    @Override
    protected void onPause(){   //                                                               a24
        super.onPause();
        if(mGoogleApiClient.isConnected()){
            stopLocationUpdates();
        }

        mGoogleApiClient.disconnect();
    }
    @Override
    protected void onStop(){  //                                                                 a25
        super.onStop();
        Log.v("aaaa","onStop");
        //自プログラムがオフした場合はWIFIをオンにする処理
        if(mWifiOff){
            mWifi.setWifiEnabled(true);
            Log.v("aaaa","onStop2");
        }
    }
    protected void stopLocationUpdates(){  //                                                    a26
        mFusedLocationProviderApi.removeLocationUpdates(mGoogleApiClient, this);
    }
    @Override
    public void onConnectionSuspended(int cause){
        //Do nothing
    }
    @Override
    public void onConnectionFailed(ConnectionResult result){
        //Do nothing
    }

    @Override
    public Loader<Address> onCreateLoader(int id, Bundle args){ //                               a27
        double lat = args.getDouble("lat");
        double lon = args.getDouble("lon");
        Log.v("aaaa","lat = " + lat + "\n" + "lon = " + lon);
        return new AddressTaskLoader(this, lat,lon);
    }

    @Override
    public void onLoadFinished(Loader<Address> loader, Address result){   //                     a28
        Log.v("aaaa","onLoadFinished2");
        if(result != null){
            Log.v("aaaa","result = " + result);
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < result.getMaxAddressLineIndex() + 1; i++){
                String item = result.getAddressLine(i);
                Log.v("aaaa","item = " + item);
                if(item == null){
                    Log.v("aaaa","item2 = " + item);
                    break;
                }
                sb.append(item);
            }
            TextView address = (TextView) findViewById(R.id.address);
            Log.v("aaaa","address = " + address);

            address.setText(sb.toString());
        }
    }

    @Override
    public void onLoaderReset(Loader<Address> loader){
    }

    public void saveJogViaCTP(){    //                                                           a29
        Log.v("aaaa","saveJogViaCTP");
        String strDate = new SimpleDateFormat("yyyy/MM/dd").format(mStartTimeMillis);
        Log.v("aaaa","strDate = " + strDate);
        TextView txtAddress = (TextView)findViewById(R.id.address);
        Log.v("aaaa","txtAddress = " + txtAddress);

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DATE, strDate);
        values.put(DatabaseHelper.COLUMN_ELAPSEDTIME,mChronometer.getText().toString());
        values.put(DatabaseHelper.COLUMN_DISTANCE, mMeter);
        values.put(DatabaseHelper.COLUMN_SPEED, mSpeed);
        values.put(DatabaseHelper.COLUMN_ADDRESS, txtAddress.getText().toString());
        Uri uri = getContentResolver().insert(JogRecordContentProvider.CONTENT_URI, values);
        showToast("データを保存しました");
        Log.v("aaaa","uri = " + uri);
    }

    public void saveJog() {   //                                                                 a30
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String strDate = new SimpleDateFormat("yyyy/MM/dd").format(mStartTimeMillis);

        TextView txtAddress = (TextView)findViewById(R.id.address);

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DATE,strDate);
        values.put(DatabaseHelper.COLUMN_ELAPSEDTIME,mChronometer.getText().toString());
        values.put(DatabaseHelper.COLUMN_DISTANCE, mMeter);
        values.put(DatabaseHelper.COLUMN_SPEED, mSpeed);
        values.put(DatabaseHelper.COLUMN_ADDRESS, txtAddress.getText().toString());
        try {
            db.insert(DatabaseHelper.TABLE_JOGRECORD, null, values);
        } catch (Exception e) {
            showToast("データの保存に失敗しました");
        } finally {
            db.close();
        }
    }
    private void showToast(String msg){
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }
}

