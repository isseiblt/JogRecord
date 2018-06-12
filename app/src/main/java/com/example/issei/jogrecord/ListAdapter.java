package com.example.issei.jogrecord;

import android.content.Context;
import android.database.Cursor;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class ListAdapter extends CursorAdapter {

    public ListAdapter(Context context, Cursor c, int flag) {
        super(context, c, flag);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {   //                         1
        Log.v("aaaa","bindView");
        //cursorからデータを取り出す
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
        String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE));
        String elapsedTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ELAPSEDTIME));
        double distance = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DISTANCE));
        double speed = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SPEED));
        String address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS));
        Log.v("aaaa","id = " + id);
        Log.v("aaaa","date" + date);
        Log.v("aaaa","elapsedTime" + elapsedTime);
        Log.v("aaaa","distance" + distance);
        Log.v("aaaa","speed" + speed);
        Log.v("aaaa","address" + address);

        TextView tv_id = (TextView) view.findViewById(R.id._id);
        TextView tv_date = (TextView) view.findViewById(R.id.date);
        TextView tv_elapsed_time = (TextView) view.findViewById(R.id.elapsed_time);
        TextView tv_distance = (TextView) view.findViewById(R.id.distance);
        TextView tv_speed = (TextView) view.findViewById(R.id.speed);
        TextView tv_place = (TextView) view.findViewById(R.id.address);

        tv_id.setText(String.valueOf(id));
        tv_date.setText(date);
        tv_elapsed_time.setText(elapsedTime);
        tv_distance.setText(String.format("%.2f" ,distance/1000)); //                              2
        tv_speed.setText(String.format("%.2f" ,speed));
        tv_place.setText(address);
    }
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {  //                 3
        Log.v("aaaa","newView");
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.row, null);
        return view;
    }
}
