package com.example.issei.jogrecord;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;

public class JogView extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>{  //    1
    private static final int CURSORLOADER_ID = 0;
    //private ListAdapter mAdapter;
    private CursorAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view);
        Log.v("aaaa","onCreate.jogview");

        Button btnView = (Button) this.findViewById(R.id.btnRet);
        btnView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                finish();
            }
        });

        mAdapter = new com.example.issei.jogrecord.ListAdapter(this, null, 0);
        setListAdapter(mAdapter);

        Log.v("aaaa","getLoaderManager");
        getLoaderManager().initLoader(CURSORLOADER_ID, null, this); //               2

    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v("aaaa","onCreateLoader");
        return new CursorLoader(this,        //                                            3
                JogRecordContentProvider.CONTENT_URI, null, null, null, "_id DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.v("aaaa","onLoadFinished");
        mAdapter.swapCursor(cursor);            //                                                 4
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.v("aaaa","onLoaderReset");
        mAdapter.swapCursor(null);
    }
}
