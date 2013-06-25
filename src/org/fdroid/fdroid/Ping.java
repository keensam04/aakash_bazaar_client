/*package org.fdroid.fdroid;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;

public class Ping {
	
	ContentValues values = new ContentValues();
	private Context mContext;

	public void ping() {
		String localserver = null;
		InetAddress in;
		in = null;
		try {
			System.out.print("address resolved!");
			localserver = "10.105.15.225";
			in = InetAddress.getByName(localserver);

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			System.out.println("something went wrong while resolving address!");
			e.printStackTrace();
		}
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();

		StrictMode.setThreadPolicy(policy);
		try {

			if (in.isReachable(5000)) {
				System.out.println(" Local server is accessible! ");
				values.put("address",
	                    mContext.getString(R.string.default_repo_address));
	            values.put("pubkey",
	                    mContext.getString(R.string.default_repo_pubkey));
			} else {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View vi = li.inflate(R.layout.about, null);

				Builder p = new AlertDialog.Builder(this).setView(view);
				final AlertDialog alrt2 = p.create();
				alrt2.setIcon(R.drawable.icon);
				alrt2.setTitle(getString(R.string.about_title));
				alrt2.setButton(DialogInterface.BUTTON_NEUTRAL,
						getString(R.string.about_website),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								System.out
										.println(" This is it!!!!!!!!!!!!!!!!!!!!! ");
								values.put(
										"address",
										mContext.getString(R.string.default_repo_address));
								values.put(
										"pubkey",
										mContext.getString(R.string.default_repo_pubkey));
							}
						});
				alrt2.setButton(DialogInterface.BUTTON_NEGATIVE,
						getString(R.string.ok),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
							}
						});
				alrt2.show();

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			info.setText(e.toString());
		}

	}

}
*/