package org.fdroid.fdroid;

import android.app.Activity;
//import android.app.ListActivity;
//import android.content.Intent;
//import android.net.Uri;
import android.os.Bundle;
//import android.util.Log;
//import android.widget.LinearLayout;
//import android.widget.ListView;

public class AppDetailsOnClick extends Activity {

//	private String appid;
//	LinearLayout headerView;
	
    

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.applistitemonclick);
		
/*		Intent i = getIntent();
        appid = "";
        Uri data = getIntent().getData();
        if (data != null) {
            appid = data.getEncodedSchemeSpecificPart();
            Log.d("FDroid", "AppDetails launched from link, for '" + appid
                    + "'");
        } else if (!i.hasExtra("appid")) {
            Log.d("FDroid", "No application ID in AppDetails!?");
        } else {
            appid = i.getStringExtra("appid");
        }

		// Set up the list...
		headerView = new LinearLayout(this);
		ListView lv = (ListView) findViewById(android.R.id.list);
		lv.addHeaderView(headerView);
		AppListAdapter la = new AppListAdapter(this);
		setListAdapter(la);*/
	}
}
