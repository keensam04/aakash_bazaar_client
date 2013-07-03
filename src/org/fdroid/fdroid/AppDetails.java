/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.fdroid.fdroid;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import android.support.v4.view.MenuItemCompat;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.fdroid.fdroid.DB.DBHelper;
import org.fdroid.fdroid.DB.Repo;
import org.fdroid.fdroid.compat.MenuManager;
import org.xml.sax.XMLReader;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class AppDetails extends ListActivity {

	private static final int REQUEST_INSTALL = 0;
	private static final int REQUEST_UNINSTALL = 1;

	private class ApkListAdapter extends BaseAdapter {

		private List<DB.Apk> items;

		public ApkListAdapter(Context context, List<DB.Apk> items) {
			this.items = (items != null ? items : new ArrayList<DB.Apk>());
		}

		public void addItem(DB.Apk apk) {
			items.add(apk);
		}

		public List<DB.Apk> getItems() {
			return items;
		}

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public Object getItem(int position) {
			return items.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			java.text.DateFormat df = DateFormat.getDateFormat(mctx);

			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.apklistitem, null);
			}
			DB.Apk apk = items.get(position);
			TextView version = (TextView) v.findViewById(R.id.version);
			boolean iscurrent = apk.vercode == app_currentvercode;
			version.setText(getString(R.string.version) + " " + apk.version
					+ (iscurrent ? "*" : ""));

			// TODO: This will show 'Installed' for all apks with the
			// same version code, which could be more than one if they
			// come from different repos or are source/binary from the
			// same one!
			TextView status = (TextView) v.findViewById(R.id.status);
			if (apk.vercode == app.installedVerCode)
				status.setText(getString(R.string.inst));
			else
				status.setText(getString(R.string.not_inst));

			TextView size = (TextView) v.findViewById(R.id.size);
			if (apk.detail_size == 0) {
				size.setText("");
			} else {
				size.setText(Utils.getFriendlySize(apk.detail_size));
			}
			TextView buildtype = (TextView) v.findViewById(R.id.buildtype);
			if (apk.srcname != null) {
				buildtype.setText("source");
			} else {
				buildtype.setText("bin");
			}
			TextView added = (TextView) v.findViewById(R.id.added);
			if (apk.added != null) {
				added.setVisibility(View.VISIBLE);
				added.setText(df.format(apk.added));
			} else {
				added.setVisibility(View.GONE);
			}

			// Disable it all if it isn't compatible...
			View[] views = { v, version, status, size, buildtype, added };
			for (View view : views) {
				view.setEnabled(apk.compatible);
			}

			return v;
		}
	}

	private static final int INSTALL = Menu.FIRST;
	private static final int UNINSTALL = Menu.FIRST + 1;
	private static final int WEBSITE = Menu.FIRST + 2;
	private static final int ISSUES = Menu.FIRST + 3;
	private static final int SOURCE = Menu.FIRST + 4;
	private static final int MARKET = Menu.FIRST + 5;
	private static final int DONATE = Menu.FIRST + 6;
	private static final int LAUNCH = Menu.FIRST + 7;
	private DB.App app;
	private int app_currentvercode;
	private DB.Apk curapk;
	private String appid;
	private PackageManager mPm;
	private DownloadHandler downloadHandler;
	private boolean stateRetained;
	LinearLayout screenshot_parent;
	LinearLayout headerView;
	LinearLayout Load_screenshot_parent;
	private Context mctx = this;
	View infoView;
	String repoAddress;
	private boolean pref_cacheDownloaded;
	private boolean pref_expert;
	private boolean resetRequired;

	// The signature of the installed version.
	private Signature mInstalledSignature;
	private String mInstalledSigID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.appdetails);
		repoAddress = UpdateService.repoAddress;
		System.out.println("ADDRESS"+repoAddress);
		Intent i = getIntent();
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
			System.out.println("appid"+appid);
		}


		// Set up the list...
		headerView = new LinearLayout(this);
		ListView lv = (ListView) findViewById(android.R.id.list);
		lv.addHeaderView(headerView);
		ApkListAdapter la = new ApkListAdapter(this, null);
		setListAdapter(la);

		mPm = getPackageManager();
		// Get the preferences we're going to use in this Activity...
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		pref_cacheDownloaded = prefs.getBoolean("cacheDownloaded", false);
		pref_expert = prefs.getBoolean("expert", false);
		AppDetails old = (AppDetails) getLastNonConfigurationInstance();
		if (old != null) {
			copyState(old);
		} else {
			resetRequired = true;
		}

		if (resetRequired) {
			reset();
			resetRequired = false;
		}
		resetViews();

		MenuManager.create(this).invalidateOptionsMenu();

		if (downloadHandler != null) {
			downloadHandler.startUpdates();
		}

	}

	public ArrayList<String> fetch_screenshot_list_from_database(String appid) {
		DBHelper d = new DBHelper(this);
		SQLiteDatabase db = d.getWritableDatabase();

		ArrayList<String> image_array = new ArrayList<String>();
		// query database table 'screenshots' for image names with package name 
		String get_image_name = "select screenshot from screenshots where pkg_id='"+appid+"';";
		// System.out.println(get_image_name);
		Cursor cursor = db.rawQuery(get_image_name, null);
		// System.out.println("OUTSIDE IF STATEMENT");
		if (cursor.moveToFirst()) {
			// System.out.println("GOT SOMETHING IN TABLE: screenshots");
			do {
				image_array.add(cursor.getString(0));
				// System.out.println("CURSOR_POSITION : "+cursor.getPosition());
			} while (cursor.moveToNext());	
		}
		db.close();	// safely close the database

		// System.out.println("SCREENSHOT_LIST is: "+image_array.size());
		return image_array;
	}


	public void fetch_screenshot_from_sdCard_and_display(String appid1, ArrayList<String> image_array) {
		// fetch screenshots from '/mnt/sdcard' and display
		screenshot_parent = (LinearLayout) infoView.findViewById(R.id.parentLinear);
		Load_screenshot_parent.setVisibility(View.GONE);
		// System.out.println("AFTER PROGRESSBAR");
		File image_path = new File(Environment.getExternalStorageDirectory()+"/fdroid/"+appid);
		if (image_path.exists() && image_path.getUsableSpace() != 0) {
			//Toast.makeText(AppDetails.this, "fetching new images", Toast.LENGTH_SHORT).show();
			for (int i = 0; i < image_array.size(); i++) {
				// Toast.makeText(this, "image found"+image_array.get(i).toString(), Toast.LENGTH_SHORT).show();
				// System.out.println("image name"+image_array.get(i).toString());

				//add screenshots to layout
				ImageView screenshot = new ImageView(AppDetails.this);
				Bitmap bm = BitmapFactory.decodeFile(image_path+"/"+image_array.get(i).toString());
				// screenshot.setBackgroundResource(Drawable.createFromPath(image_path+"/"+image_array.get(i).toString()));
				screenshot.setImageBitmap(bm);
				screenshot_parent.addView(screenshot,250,200);

				Space space = new Space(AppDetails.this);
				screenshot_parent.addView(space,5,LayoutParams.MATCH_PARENT);
			}

		}

	}

	private final boolean isInternetOn() {
		// check internet connection via wifi  
		ConnectivityManager connec =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		if( connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED ||
				connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTING ||
				connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTING ||
				connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED ) {
			//Toast.makeText(this, connectionType + ” connected”, Toast.LENGTH_SHORT).show();
			return true;
		}
		else if( connec.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED || 
				connec.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED  ) {
			//System.out.println(“Not Connected”);
			return false;
		}
		return false;
	}

	public void create_pkg_dir() {
        /*
         * create <package_name> dirrectory under /mnt/sdcard/fdroid
         */
		// System.out.println("APP ID IS"+appid);
		File fdroid = new File(Environment.getExternalStorageDirectory() + "/fdroid");
		if (!fdroid.exists()) {
		    // System.out.println("fdroid dir NOT exists!");
			fdroid.mkdir();	
			// System.out.println("folder CREATED");
			File package_dir = new File(Environment.getExternalStorageDirectory() + "/fdroid/" + appid);
			if (!package_dir.exists()){
				package_dir.mkdir();
			}
		}
		else {
			System.out.println("dir fdroid already exist");
			File package_dir = new File(Environment.getExternalStorageDirectory() + "/fdroid/" + appid);
			if (!package_dir.exists()){
				package_dir.mkdir();
			}
		}

	}

	public class ParseUrl extends AsyncTask<String, String, String>{
		/*
		 * get page from URL:/screenshots/<package_name>
		 */
		String result;
		String PKG_NAME;
		
		@Override
		protected String doInBackground(String... params) {

			try {
				PKG_NAME = params[0];
				System.out.println("pkg name"+PKG_NAME);
				HttpClient httpClient = new DefaultHttpClient();
				HttpContext localContext = new BasicHttpContext();
				HttpGet httpGet = new HttpGet(repoAddress+"/screenshots/"+PKG_NAME+"/");
				HttpResponse response = httpClient.execute(httpGet, localContext);

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(
								response.getEntity().getContent()
								)
						);

				String line = null;
				while ((line = reader.readLine()) != null){
					result += line + "\n";
				}

			} catch (Exception e) {
				System.out.println("error is "+ e.getMessage().toString());
			}
			return null;
		}

		@Override
		protected void onPostExecute(String unused) {
			// System.out.println("RESULT = "+result);
			
			// parse html page and extract image file name into an array called 'screenshots'
			String[] screenshots = StringUtils.substringsBetween(result, "alt=\"[IMG]\"></td><td><a href=\"", "\">");
			if (screenshots != null) {
			    String URL_REPO = repoAddress;
			    
			    // start downloading images in //mnt/sdcard/fdroid/PKG_NAME
			    for (int i = 0; i < screenshots.length; i++) {
				// System.out.println(screenshots[i]);
				new DownloadFileAsync().
				    execute(URL_REPO+"/screenshots/"+PKG_NAME+"/"+screenshots[i],screenshots[i],PKG_NAME);
			    }
			    
			}else{
			    System.out.println("NO SCREENSHOTS AVAILABLE");
			    add_no_preview_textview();
			}
			super.onPostExecute(result);
		}

		public void add_no_preview_textview() {
			Load_screenshot_parent.setVisibility(View.GONE);
			TextView tv = new TextView(AppDetails.this);
			tv.setGravity(Gravity.FILL_VERTICAL);
			tv.setText("No screenshots available");
			tv.setPadding(0, 20, 0, 20);
			screenshot_parent.addView(tv);
		}
	}

	public class DownloadFileAsync extends AsyncTask<String, String, String>{

		int count;
		String PKG_NAME;
		String IMAGE_NAME;

		@Override
		protected String doInBackground(String... Url) {
			URL url;
			try {
				System.out.println("get url");
				System.out.println(Url.length);
				url = new URL(Url[0]);
				PKG_NAME = Url[2];
				IMAGE_NAME = Url[1];
				for (int i = 0; i < Url.length; i++) {  
					System.out.println(Url[i]);  
				} 

				URLConnection conection = url.openConnection();
				conection.connect();
				// get file length
				int lenghtOfFile = conection.getContentLength();
				System.out.println("get lenght url");
				// input stream to read file - with 8k buffer
				InputStream input = new BufferedInputStream(url.openStream());
				System.out.println("get input");
				// Output stream to write file
				// Url[0] = download url
				// Url[1] = image file name
				// Url[2] = package name
				OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory()+"/fdroid/"+Url[2]+"/"+Url[1]);
				System.out.println("get op");
				byte data[] = new byte[1024];

				long total = 0;

				while ((count = input.read(data)) != -1) {
					total += count;
					// publishing the progress....
					// After this onProgressUpdate will be called
					publishProgress(""+(int)((total*100)/lenghtOfFile));
					// writing data to file
					output.write(data, 0, count);
				}

				// flushing output
				output.flush();
				System.out.println("flush");
				// closing streams
				output.close();
				input.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println("Error "+e.getMessage().toString());
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			DBHelper d = new DBHelper(AppDetails.this);
			SQLiteDatabase db = d.getWritableDatabase();

			String query = "select screenshot from screenshots where screenshot = '"+IMAGE_NAME+"';";
			Cursor cursor = db.rawQuery(query, null);

			if(cursor.getCount() == 0){
				System.out.println("IMAGE NOT EXISTS IN DATABASE");
				ContentValues values = new ContentValues();
				values = new ContentValues();
				values.put("pkg_id", PKG_NAME);
				values.put("screenshot", IMAGE_NAME);
				db.insert("screenshots", null, values);
			}
			db.close(); // safely close database

			// display image in layout instead of progress bar
			Load_screenshot_parent.setVisibility(View.GONE);
			// System.out.println("AFTER PROGRESSBAR");

			File image_path = new File(Environment.getExternalStorageDirectory()+"/fdroid/"+appid+"/"+IMAGE_NAME);
			if (image_path.exists() && image_path.getUsableSpace() != 0) {

				//add screenshots to layout
				ImageView screenshot = new ImageView(AppDetails.this);
				Bitmap bm = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+"/fdroid/"+appid+"/"+IMAGE_NAME);
				//screenshot.setBackgroundResource(Drawable.createFromPath(image_path+"/"+image_array.get(i).toString()));
				screenshot.setImageBitmap(bm);
				screenshot_parent.addView(screenshot,250,200);

				Space space = new Space(AppDetails.this);
				screenshot_parent.addView(space,5,LayoutParams.MATCH_PARENT);
			}
			super.onPostExecute(result);
		}

	}

	public void downloadScreenshots(String appid) {
		/*
		 * When apk in the list is clicked, check is any screenshot entry
		 * in available in the 'fdroid.db/screenshots' table.
		 * If yes:
		 *     - fetch relavant screeshots from /mnt/sdcard/fdroid/<pakage_name>
		 *     - display in layout
		 * else:
		 *     chk for internet connection
		 *          if yes:
		 *             - parse the URL and get screenshot names(as a list) 
		 *               and store in an array
		 *             - download each image in a list at /mnt/sdcard/fdroid/<pakage
		 *               _name>
		 *             - write entry in db
		 *             - display in layout
		 *         else:
		 *             - show "No screenshots available"
		 */

		// query db
		ArrayList<String> image_array = fetch_screenshot_list_from_database(appid);

		if(image_array.size() != 0){
			try {
				System.out.println("entries available in database");
				fetch_screenshot_from_sdCard_and_display(appid,image_array);
			} catch (Exception e) {
				System.out.println("EXCEPTION: "+e.getMessage().toString());
			}

		}else{
			System.out.println("no entries in database");
			// if internet is ON
			if (isInternetOn()) {
				System.out.println("INTERNET ON");
				create_pkg_dir();
				new ParseUrl().execute(appid.toString());
			}else{
				System.out.println("INTERNET OFF");
				Load_screenshot_parent.setVisibility(View.GONE);
				TextView tv = new TextView(AppDetails.this);
				tv.setText("No screenshots available");
				tv.setPadding(0, 20, 0, 20);
				screenshot_parent.addView(tv);
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		stateRetained = true;
		return this;
	}

	@Override
	protected void onDestroy() {
		if (downloadHandler != null) {
			if (!stateRetained)
				downloadHandler.cancel();
			downloadHandler.destroy();
		}
		super.onDestroy();
	}

	// Copy all relevant state from an old instance. This is used
	// in place of reset(), so it must initialize all fields
	// normally set there.
	private void copyState(AppDetails old) {
		if (old.downloadHandler != null)
			downloadHandler = new DownloadHandler(old.downloadHandler);
		app = old.app;
		app_currentvercode = old.app_currentvercode;
		mInstalledSignature = old.mInstalledSignature;
		mInstalledSigID = old.mInstalledSigID;
	}

	// Reset the display and list contents. Used when entering the
	// activity, and also when something has been
	// installed/uninstalled.
	private void reset() {

		Log.d("FDroid", "Getting application details for " + appid);
		app = null;
		List<DB.App> apps = ((FDroidApp) getApplication()).getApps();
		for (DB.App tapp : apps) {
			if (tapp.id.equals(appid)) {
				app = tapp;
				break;
			}
		}
		if (app == null) {
			finish();
			return;
		}

		// Make sure the app is populated.
		try {
			DB db = DB.getDB();
			db.populateDetails(app, 0);
		} catch (Exception ex) {
			Log.d("FDroid", "Failed to populate app - " + ex.getMessage());
		} finally {
			DB.releaseDB();
		}

		DB.Apk curver = app.getCurrentVersion();
		app_currentvercode = curver == null ? 0 : curver.vercode;

		// Get the signature of the installed package...
		mInstalledSignature = null;
		mInstalledSigID = null;
		if (app.installedVersion != null) {
			PackageManager pm = getBaseContext().getPackageManager();
			try {
				PackageInfo pi = pm.getPackageInfo(appid,
						PackageManager.GET_SIGNATURES);
				mInstalledSignature = pi.signatures[0];
				Hasher hash = new Hasher("MD5", mInstalledSignature
						.toCharsString().getBytes());
				mInstalledSigID = hash.getHash();
			} catch (NameNotFoundException e) {
				Log.d("FDroid", "Failed to get installed signature");
			} catch (NoSuchAlgorithmException e) {
				Log.d("FDroid", "Failed to calculate signature MD5 sum");
				mInstalledSignature = null;
			}
		}

	}

	private void resetViews() {

		// Repopulate the list...
		ApkListAdapter la = (ApkListAdapter) getListAdapter();
		la.items.clear();
		for (DB.Apk apk : app.apks)
			la.addItem(apk);
		la.notifyDataSetChanged();

		// Insert the 'infoView' (which contains the summary, various odds and
		// ends, and the description) into the appropriate place, if we're in
		// landscape mode. In portrait mode, we put it in the listview's
		// header..
		infoView = View.inflate(this, R.layout.appinfo, null);
		ScrollView landparent = (ScrollView) findViewById(R.id.landleft);
		headerView.removeAllViews();
		if (landparent != null) {
			landparent.removeAllViews();
			landparent.addView(infoView);
			Log.d("FDroid", "Setting landparent infoview");
		} else {
			headerView.addView(infoView);
			Log.d("FDroid", "Setting header infoview");
		}

		// Set the icon...
		ImageView iv = (ImageView) findViewById(R.id.icon);
		File icon = new File(DB.getIconsPath(), app.icon);
		if (icon.exists()) {
			iv.setImageDrawable(new BitmapDrawable(icon.getPath()));
		} else {
			iv.setImageResource(android.R.drawable.sym_def_app_icon);
		}

		// Set the title and other header details...
		TextView tv = (TextView) findViewById(R.id.title);
		tv.setText(app.name);
		//        tv = (TextView) findViewById(R.id.license);
		//        tv.setText(app.license);
		tv = (TextView) findViewById(R.id.status);
		if (app.installedVersion == null)
			tv.setText(getString(R.string.details_notinstalled));
		else
			tv.setText(String.format(getString(R.string.details_installed),
					app.installedVersion));

		// display 'Install' and 'Launch' button
		screenshot_parent = (LinearLayout) infoView.findViewById(R.id.parentLinear);
		Load_screenshot_parent = (LinearLayout) screenshot_parent.findViewById(R.id.load_screenshot_parent);

		final Button installOrLaunch = (Button) findViewById(R.id.bAppDetailInstall);
		Button uninstall = (Button) findViewById(R.id.bAppDetailUninstall);
		if (app.installedVersion == null)
			installOrLaunch.setText(" Install ");
		else {
			installOrLaunch.setText(" Launch  ");
			uninstall.setVisibility(View.VISIBLE);
		}

		installOrLaunch.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (installOrLaunch.getText().toString() == "Install") {
					curapk = app.getCurrentVersion();
					if (curapk != null)
						install();
				} else
					launchApk(app.id);

			}
		});
		uninstall.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				removeApk(app.id);
			}
		});

		//********************************************************
		tv = (TextView) infoView.findViewById(R.id.description);
		tv.setMovementMethod(LinkMovementMethod.getInstance());

		//Rating widget
		RatingBar rate = (RatingBar) findViewById(R.id.rateIt);
		rate.setNumStars(5);
		//        rate.setOnRatingBarChangeListener(listener);

		// Need this to add the unimplemented support for ordered and unordered
		// lists to Html.fromHtml().
		class HtmlTagHandler implements TagHandler {
			int listNum;

			@Override
			public void handleTag(boolean opening, String tag, Editable output,
					XMLReader reader) {
				if (opening && tag.equals("ul")) {
					listNum = -1;
				} else if (opening && tag.equals("ol")) {
					listNum = 1;
				} else if (tag.equals("li")) {
					if (opening) {
						if (listNum == -1) {
							output.append("\t•");
						} else {
							output.append("\t" + Integer.toString(listNum)
									+ ". ");
							listNum++;
						}
					} else {
						output.append('\n');
					}
				}
			}
		}
		tv.setText(Html.fromHtml(app.detail_description, null,
				new HtmlTagHandler()));

		tv = (TextView) infoView.findViewById(R.id.summary);
		tv.setText(app.summary);

		tv = (TextView) infoView.findViewById(R.id.appid);
		if (pref_expert) {
			tv.setVisibility(View.VISIBLE);
			tv.setText(app.id);
		} else {
			tv.setVisibility(View.GONE);
		}

		tv = (TextView) infoView.findViewById(R.id.signature);
		if (pref_expert && mInstalledSignature != null) {
			tv.setVisibility(View.VISIBLE);
			tv.setText("Signed: " + mInstalledSigID);
		} else {
			tv.setVisibility(View.GONE);
		}
		try {
			downloadScreenshots(appid);
		} catch (Exception e) {
			Toast.makeText(AppDetails.this,"An Error occured during fetching screenshots, Please check internet connection", Toast.LENGTH_LONG).show();
		}

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		curapk = app.apks.get(position - l.getHeaderViewsCount());
		if (app.installedVersion != null
				&& app.installedVersion.equals(curapk.version)) {
			removeApk(app.id);
		} else {
			install();
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		super.onCreateOptionsMenu(menu);
		menu.clear();
		if (app == null)
			return true;
		DB.Apk curver = app.getCurrentVersion();
		List<MenuItem> toShow = new ArrayList<MenuItem>(2);
		if (app.installedVersion != null && curver != null
				&& !app.installedVersion.equals(curver.version)) {
			toShow.add(menu.add(Menu.NONE, INSTALL, 0, R.string.menu_update).setIcon(
					R.drawable.ic_menu_refresh));
		}
		if (app.installedVersion == null && curver != null) {
			toShow.add(menu.add(Menu.NONE, INSTALL, 1, R.string.menu_install).setIcon(
					android.R.drawable.ic_menu_add));
		} else {
			toShow.add(menu.add( Menu.NONE, LAUNCH, 1, R.string.menu_launch ).setIcon(
					android.R.drawable.ic_media_play));
			menu.add(Menu.NONE, UNINSTALL, 1, R.string.menu_uninstall).setIcon(
					android.R.drawable.ic_menu_delete);
		}
		if (app.detail_webURL.length() > 0) {
			menu.add(Menu.NONE, WEBSITE, 2, R.string.menu_website).setIcon(
					android.R.drawable.ic_menu_view);
		}
		if (app.detail_trackerURL.length() > 0) {
			menu.add(Menu.NONE, ISSUES, 3, R.string.menu_issues).setIcon(
					android.R.drawable.ic_menu_view);
		}
		if (app.detail_sourceURL.length() > 0) {
			menu.add(Menu.NONE, SOURCE, 4, R.string.menu_source).setIcon(
					android.R.drawable.ic_menu_view);
		}
		menu.add(Menu.NONE, MARKET, 5, R.string.menu_market).setIcon(
				android.R.drawable.ic_menu_view);
		if (app.detail_donateURL != null) {
			menu.add(Menu.NONE, DONATE, 6, R.string.menu_donate).setIcon(
					android.R.drawable.ic_menu_view);
		}
		for (MenuItem item : toShow) {
			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case LAUNCH:
			launchApk(app.id);
			return true;

		case INSTALL:
			// Note that this handles updating as well as installing.
			curapk = app.getCurrentVersion();
			if (curapk != null)
				install();
			return true;

		case UNINSTALL:
			removeApk(app.id);
			return true;

		case WEBSITE:
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse(app.detail_webURL)));
			return true;

		case ISSUES:
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse(app.detail_trackerURL)));
			return true;

		case SOURCE:
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse(app.detail_sourceURL)));
			return true;

		case MARKET:
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("http://market.android.com/details?id=" + app.id)));
			return true;

		case DONATE:
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse(app.detail_donateURL)));
			return true;

		}
		return super.onOptionsItemSelected(item);
	}

	// Install the version of this app denoted by 'curapk'.
	private void install() {

		String ra = null;
		try {
			DB db = DB.getDB();
			DB.Repo repo = db.getRepo(curapk.repo);
			if (repo != null)
				ra = repo.address;
		} catch (Exception ex) {
			Log.d("FDroid", "Failed to get repo address - " + ex.getMessage());
		} finally {
			DB.releaseDB();
		}
		if (ra == null)
			return;
		final String repoaddress = ra;

		if (!curapk.compatible) {
			AlertDialog.Builder ask_alrt = new AlertDialog.Builder(this);
			ask_alrt.setMessage(getString(R.string.installIncompatible));
			ask_alrt.setPositiveButton(getString(R.string.yes),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog,
						int whichButton) {
					downloadHandler = new DownloadHandler(curapk,
							repoaddress);
				}
			});
			ask_alrt.setNegativeButton(getString(R.string.no),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog,
						int whichButton) {
					return;
				}
			});
			AlertDialog alert = ask_alrt.create();
			alert.show();
			return;
		}
		if (mInstalledSigID != null && curapk.sig != null
				&& !curapk.sig.equals(mInstalledSigID)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.SignatureMismatch).setPositiveButton(
					getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
			return;
		}
		downloadHandler = new DownloadHandler(curapk, repoaddress);
	}

	private void removeApk(String id) {
		PackageInfo pkginfo;
		try {
			pkginfo = mPm.getPackageInfo(id, 0);
		} catch (NameNotFoundException e) {
			Log.d("FDroid", "Couldn't find package " + id + " to uninstall.");
			return;
		}
		Uri uri = Uri.fromParts("package", pkginfo.packageName, null);
		Intent intent = new Intent(Intent.ACTION_DELETE, uri);
		startActivityForResult(intent, REQUEST_UNINSTALL);
		((FDroidApp) getApplication()).invalidateApps();

	}

	private void installApk(File file) {
		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse("file://" + file.getPath()),
				"application/vnd.android.package-archive");
		startActivityForResult(intent, REQUEST_INSTALL);
		((FDroidApp) getApplication()).invalidateApps();
	}

	private void launchApk(String id) {
		Intent intent = mPm.getLaunchIntentForPackage(id);
		startActivity(intent);
	}

	private ProgressDialog createProgressDialog(String file, int p, int max) {
		final ProgressDialog pd = new ProgressDialog(this);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setMessage(getString(R.string.download_server) + ":\n " + file);
		pd.setMax(max);
		pd.setProgress(p);
		pd.setCancelable(true);
		pd.setCanceledOnTouchOutside(false);
		pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				downloadHandler.cancel();
			}
		});
		pd.setButton(DialogInterface.BUTTON_NEUTRAL,
				getString(R.string.cancel),
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				pd.cancel();
			}
		});
		pd.show();
		return pd;
	}

	// Handler used to update the progress dialog while downloading.
	private class DownloadHandler extends Handler {
		private Downloader download;
		private ProgressDialog pd;
		private boolean updating;
		private File localFile;

		public DownloadHandler(DB.Apk apk, String repoaddress) {
			download = new Downloader(apk, repoaddress);
			download.start();
			startUpdates();
		}

		public DownloadHandler(DownloadHandler oldHandler) {
			if (oldHandler != null) {
				download = oldHandler.download;
				localFile = oldHandler.localFile;
			}
			startUpdates();
		}

		public boolean updateProgress() {
			boolean finished = false;
			switch (download.getStatus()) {
			case RUNNING:
				if (pd == null) {
					pd = createProgressDialog(download.remoteFile(),
							download.getProgress(), download.getMax());
				} else {
					pd.setProgress(download.getProgress());
				}
				break;
			case ERROR:
				if (pd != null)
					pd.dismiss();
				String text;
				if (download.getErrorType() == Downloader.Error.CORRUPT)
					text = getString(R.string.corrupt_download);
				else
					text = download.getErrorMessage();
				Toast.makeText(AppDetails.this, text, Toast.LENGTH_LONG).show();
				finished = true;
				break;
			case DONE:
				if (pd != null)
					pd.dismiss();
				installApk(localFile = download.localFile());
				finished = true;
				break;
			case CANCELLED:
				Toast.makeText(AppDetails.this,
						getString(R.string.download_cancelled),
						Toast.LENGTH_SHORT).show();
				finished = true;
				break;
			default:
				break;
			}
			return finished;
		}

		public void startUpdates() {
			if (!updating) {
				updating = true;
				sendEmptyMessage(0);
			}
		}

		public void stopUpdates() {
			updating = false;
			removeMessages(0);
		}

		public void cancel() {
			if (download != null)
				download.interrupt();
		}

		public void cleanUp() {
			if (localFile == null) {
				Log.w("FDroid", "No APK to clean up!");
				return;
			}
			// If we're not meant to be caching, delete
			// the apk file we just installed (or maybe
			// the user cancelled the install - doesn't
			// matter) from the SD card...
			if (!pref_cacheDownloaded) {
			    Log.d("FDroid", "Cleaning up: " + localFile.getPath());
			    localFile.delete();
			    localFile = null;
			}
		}

		public void destroy() {
			// The dialog can't be dismissed when it's not displayed,
			// so do it when the activity is being destroyed.
			if (pd != null) {
				pd.dismiss();
				pd = null;
			}
			// Cancel any scheduled updates so that we don't
			// accidentally recreate the progress dialog.
			stopUpdates();
		}

		// Repeatedly run updateProgress() until it's finished.
		@Override
		public void handleMessage(Message msg) {
			if (download == null)
				return;
			boolean finished = updateProgress();
			if (finished)
				download = null;
			else
				sendMessageDelayed(obtainMessage(), 50);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_INSTALL:
			if (downloadHandler != null) {
				downloadHandler.cleanUp();
				downloadHandler = null;
			}
			resetRequired = true;
			break;
		case REQUEST_UNINSTALL:
			resetRequired = true;
			break;
		}
	}

}