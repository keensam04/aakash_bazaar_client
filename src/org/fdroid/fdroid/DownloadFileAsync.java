package org.fdroid.fdroid;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

public class DownloadFileAsync extends AsyncTask<String, String, String>{

	 int count;
	
	@Override
	protected String doInBackground(String... Url) {
		URL url;
		try {
			System.out.println("get url");
			url = new URL(Url[0]);
		
        URLConnection conection = url.openConnection();
        conection.connect();
        // getting file length
        int lenghtOfFile = conection.getContentLength();
        System.out.println("get lenght url");
        // input stream to read file - with 8k buffer
        InputStream input = new BufferedInputStream(url.openStream());
        System.out.println("get input");
        // Output stream to write file
        OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory()+"/demo.png");
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

}
