package org.fdroid.fdroid;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.fdroid.fdroid.DB.DBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.widget.Toast;

public class ParseUrl extends AsyncTask<String, String, String>{
	Context context;
	String result;
	String PKG_NAME;
	public ParseUrl(AppDetails appDetails) {
		context = appDetails;
	}

	@Override
	protected String doInBackground(String... params) {
		// TODO Auto-generated method stub
		int count;

		try {
			PKG_NAME = params[0];
			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpGet httpGet = new HttpGet("http://www.it.iitb.ac.in/AakashApps/screenshots/"+PKG_NAME+"/");
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
			System.out.println("error is "+ e.getMessage());
		}
		return null;
	}

	@Override
	protected void onPostExecute(String unused) {
		// TODO Auto-generated method stub
		System.out.println("RESULT = "+result);
		//Toast.makeText(ParseUrl.this, "SACHIN"+result, Toast.LENGTH_SHORT).show();

		// parse html page and extract image file name into an array called screenshots
		String[] screenshots = StringUtils.substringsBetween(result, "alt=\"[IMG]\"></td><td><a href=\"", "\">");
		if (screenshots != null) {
			// create instance of database and insert values
			DBHelper d = new DBHelper(context);
			SQLiteDatabase db = d.getWritableDatabase();

			String URL_IIT = "http://www.it.iitb.ac.in/AakashApps/screenshots/";

			//start downloading images in /sdcard/fdroid/PKG_NAME
			for (int i = 0; i < screenshots.length; i++) {
				ContentValues values = new ContentValues();
				values = new ContentValues();
				values.put("pkg_id", PKG_NAME);
				values.put("screenshot", screenshots[i]);
				db.insert("screenshots", null, values);
				//System.out.println(URL_IIT+PKG_NAME+"/"+screenshots[i]);
				new DownloadFileAsync().
				execute(URL_IIT+PKG_NAME+"/"+screenshots[i],screenshots[i],PKG_NAME);
			}
			db.close();
		}



		super.onPostExecute(result);
	}
}
