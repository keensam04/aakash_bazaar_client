/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2009  Roberto Jacinto, roberto.jacinto@caixamagica.pt
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import java.io.File;
import java.security.MessageDigest;
import java.util.Formatter;

import android.support.v4.view.MenuItemCompat;

import org.fdroid.fdroid.DB.DBHelper;
import org.fdroid.fdroid.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.*;
import org.fdroid.fdroid.compat.TabManager;
import org.fdroid.fdroid.views.AppListFragmentPageAdapter;

public class FDroid extends FragmentActivity {

	public static final int REQUEST_APPDETAILS = 0;
	public static final int REQUEST_MANAGEREPOS = 1;
	public static final int REQUEST_PREFS = 2;
	public static final int REQUEST_UPLOADAPK = 3;

	public static final String EXTRA_TAB_UPDATE = "extraTab";

	private static final int UPDATE_REPO = Menu.FIRST;
	private static final int MANAGE_REPO = Menu.FIRST + 1;
	private static final int UPLOAD_APK = Menu.FIRST + 2;
	private static final int PREFERENCES = Menu.FIRST + 3;
	private static final int ABOUT = Menu.FIRST + 4;
	private static final int SEARCH = Menu.FIRST + 5;

	private ProgressDialog pd;

	
	private ViewPager viewPager;

	private AppListManager manager = null;

	private TabManager tabManager = null;

	public AppListManager getManager() {
		return manager;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		manager = new AppListManager(this);
		setContentView(R.layout.fdroid);
		createViews();
		getTabManager().createTabs();

		//revert database to default
		String update_local_query = "update fdroid_repo set 'inuse'=1 where id=1;";
		String update_main_query = "update fdroid_repo set 'inuse'=0 where id=2;";
		
		DBHelper db = new DBHelper(FDroid.this);
		SQLiteDatabase d = db.getWritableDatabase();
		d.execSQL(update_local_query);
		d.execSQL(update_main_query);
		System.out
				.println("reverting database to default ");
		db.close();
		
		// Must be done *after* createViews, because it will involve a
		// callback to update the tab label for the "update" tab. This
		// will fail unless the tabs have actually been created.
		repopulateViews();

		Intent i = getIntent();
		if (i.hasExtra("uri")) {
			Intent call = new Intent(this, ManageRepo.class);
			call.putExtra("uri", i.getStringExtra("uri"));
			startActivityForResult(call, REQUEST_MANAGEREPOS);
		} else if (i.hasExtra(EXTRA_TAB_UPDATE)) {
			boolean showUpdateTab = i.getBooleanExtra(EXTRA_TAB_UPDATE, false);
			if (showUpdateTab) {
				getTabManager().selectTab(2);
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	protected void repopulateViews() {
		manager.repopulateLists();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, UPDATE_REPO, 1, R.string.menu_update_repo).setIcon(
				android.R.drawable.ic_menu_rotate);
		menu.add(Menu.NONE, MANAGE_REPO, 2, R.string.menu_manage).setIcon(
				android.R.drawable.ic_menu_agenda);
		menu.add(Menu.NONE, UPLOAD_APK, 4, R.string.menu_upload_apk).setIcon(
				android.R.drawable.ic_menu_upload);
		MenuItem search = menu.add(Menu.NONE, SEARCH, 3, R.string.menu_search)
				.setIcon(android.R.drawable.ic_menu_search);
		menu.add(Menu.NONE, PREFERENCES, 4, R.string.menu_preferences).setIcon(
				android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, ABOUT, 5, R.string.menu_about).setIcon(
				android.R.drawable.ic_menu_help);
		MenuItemCompat.setShowAsAction(search,
				MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case UPDATE_REPO:
			updateRepos();
			return true;

		case MANAGE_REPO:
			Intent i = new Intent(this, ManageRepo.class);
			startActivityForResult(i, REQUEST_MANAGEREPOS);
			return true;

		case UPLOAD_APK:
			LayoutInflater liapk = LayoutInflater.from(this);
			View viewapk = liapk.inflate(R.layout.uploadapk, null);

			WebView uploadApk = (WebView) viewapk
					.findViewById(R.id.wvUploadApk);

			uploadApk.getSettings().setJavaScriptEnabled(true);
			uploadApk.getSettings().setLoadWithOverviewMode(true);
			uploadApk.getSettings().setUseWideViewPort(true);

			uploadApk.setWebViewClient(new ourViewClient());

			/*
			 * address should be that of the login page for filling up the
			 * credentials of the developer who wants to host his apk on the
			 * akash bazaar repo
			 */
			try {
				uploadApk.loadUrl("http://www.dropbox.com");
			} catch (Exception e) {
				e.printStackTrace();
			}

			Builder p1 = new AlertDialog.Builder(this).setView(viewapk);
			final AlertDialog alrts = p1.create();
			alrts.setIcon(R.drawable.ic_menu_upload);
			alrts.setTitle(getString(R.string.upload_apk_title));
			alrts.show();
			return true;

		case PREFERENCES:
			Intent prefs = new Intent(getBaseContext(), Preferences.class);
			startActivityForResult(prefs, REQUEST_PREFS);
			return true;

		case SEARCH:
			onSearchRequested();
			return true;

		case ABOUT:
			LayoutInflater li = LayoutInflater.from(this);
			View view = li.inflate(R.layout.about, null);

			// Fill in the version...
			TextView tv = (TextView) view.findViewById(R.id.version);
			PackageManager pm = getPackageManager();
			try {
				PackageInfo pi = pm.getPackageInfo(getApplicationContext()
						.getPackageName(), 0);
				tv.setText(pi.versionName);
			} catch (Exception e) {
			}

			Builder p = new AlertDialog.Builder(this).setView(view);
			final AlertDialog alrt = p.create();
			alrt.setIcon(R.drawable.icon);
			alrt.setTitle(getString(R.string.about_title));
			alrt.setButton(DialogInterface.BUTTON_NEUTRAL,
					getString(R.string.about_website),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							Uri uri = Uri
									.parse("http://www.it.iitb.ac.in/aakash2/");
							startActivity(new Intent(Intent.ACTION_VIEW, uri));
						}
					});
			alrt.setButton(DialogInterface.BUTTON_NEGATIVE,
					getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
						}
					});
			alrt.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
		case REQUEST_APPDETAILS:
			break;
		case REQUEST_MANAGEREPOS:
			if (data.hasExtra("update")) {
				AlertDialog.Builder ask_alrt = new AlertDialog.Builder(this);
				ask_alrt.setTitle(getString(R.string.repo_update_title));
				ask_alrt.setIcon(android.R.drawable.ic_menu_rotate);
				ask_alrt.setMessage(getString(R.string.repo_alrt));
				ask_alrt.setPositiveButton(getString(R.string.yes),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								updateRepos();
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
			}
			break;
		case REQUEST_PREFS:
			// The automatic update settings may have changed, so reschedule (or
			// unschedule) the service accordingly. It's cheap, so no need to
			// check if the particular setting has actually been changed.
			UpdateService.schedule(getBaseContext());
			if (data != null
					&& (data.hasExtra("reset") || data.hasExtra("update"))) {
				updateRepos();
			} else {
				repopulateViews();
			}
			break;

		}
	}

	private void createViews() {
		viewPager = (ViewPager) findViewById(R.id.main_pager);
		AppListFragmentPageAdapter viewPageAdapter = new AppListFragmentPageAdapter(
				this);
		viewPager.setAdapter(viewPageAdapter);
		viewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						getTabManager().selectTab(position);
					}
				});
	}

	// For receiving results from the UpdateService when we've told it to
	// update in response to a user request.
	private class UpdateReceiver extends ResultReceiver {
		public UpdateReceiver(Handler handler) {
			super(handler);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			String message = resultData.getString(UpdateService.RESULT_MESSAGE);
			boolean finished = false;
			if (resultCode == UpdateService.STATUS_ERROR) {
				
				System.out.println("message"+message);
				if(message.contains("Update failed for "+getString(R.string.default_repo_address))){
					LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View view = li.inflate(R.layout.ping, null);
			
					TextView tvMainAddress = (TextView) view
							.findViewById(R.id.tvMainRepoAddress);
					TextView tvMainPubkey = (TextView) view
							.findViewById(R.id.tvMainRepoPubkey);
					tvMainAddress.setText("Repo Address: "
							+ getString(R.string.default_repo_address2));
					if (getString(R.string.default_repo_pubkey2) != null) {
						try {
							MessageDigest digest = MessageDigest
									.getInstance("SHA-1");
							digest.update(Hasher
									.unhex(getString(R.string.default_repo_pubkey2)));
							byte[] fingerprint = digest.digest();
							Formatter formatter = new Formatter(
									new StringBuilder());
							formatter.format("%02X", fingerprint[0]);
							for (int i = 1; i < fingerprint.length; i++) {
								formatter.format(i % 5 == 0 ? " %02X"
										: ":%02X", fingerprint[i]);
							}
							tvMainPubkey.setText("Fingerprint: "
									+ formatter.toString());
							formatter.close();
						} catch (Exception e) {
							Log.w("FDroid",
									"Unable to get certificate fingerprint.\n"
											+ Log.getStackTraceString(e));
						}
					}
			
					Builder p = new AlertDialog.Builder(FDroid.this).setView(view);
					final AlertDialog alrt = p.create();
					alrt.setIcon(R.drawable.icon);
					alrt.setTitle(getString(R.string.ping_title));
					alrt.setButton(DialogInterface.BUTTON_NEUTRAL,
							getString(R.string.ping_ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									String update_local_query = "update fdroid_repo set 'inuse'=0 where id=1;";
									String update_main_query = "update fdroid_repo set 'inuse'=1 where id=2;";
									
									DBHelper db = new DBHelper(FDroid.this);
									SQLiteDatabase d = db.getWritableDatabase();
									d.execSQL(update_local_query);
									d.execSQL(update_main_query);
									System.out
											.println(" Connecting to main repo ");
									db.close();
						
									updateRepos();
								}
							});
					alrt.setButton(DialogInterface.BUTTON_NEGATIVE,
							getString(R.string.ping_no),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							});
					alrt.show();
					
				}else{
					Toast.makeText(FDroid.this, message, Toast.LENGTH_LONG).show();
				}
				
				finished = true;
			} else if (resultCode == UpdateService.STATUS_COMPLETE) {
				repopulateViews();
				finished = true;
			} else if (resultCode == UpdateService.STATUS_INFO) {
				pd.setMessage(message);
			}

			if (finished && pd.isShowing())
				pd.dismiss();
		}
	}

	private UpdateReceiver mUpdateReceiver;

	/**
	 * The first time the app is run, we will have an empty app list. If this is
	 * the case, we will attempt to update with the default repo. However, if we
	 * have tried this at least once, then don't try to do it automatically
	 * again, because the repos or internet connection may be bad.
	 */
	public boolean updateEmptyRepos() {
		final String TRIED_EMPTY_UPDATE = "triedEmptyUpdate";
		boolean hasTriedEmptyUpdate = getPreferences(MODE_PRIVATE).getBoolean(
				TRIED_EMPTY_UPDATE, false);
		if (!hasTriedEmptyUpdate) {
			Log.d("FDroid",
					"Empty app list, and we haven't done an update yet. Forcing repo update.");
			getPreferences(MODE_PRIVATE).edit()
					.putBoolean(TRIED_EMPTY_UPDATE, true).commit();
			updateRepos();
			return true;
		} else {
			Log.d("FDroid",
					"Empty app list, but it looks like we've had an update previously. Will not force repo update.");
			return false;
		}
	}

	// Force a repo update now. A progress dialog is shown and the UpdateService
	// is told to do the update, which will result in the database changing. The
	// UpdateReceiver class should get told when this is finished.
	public void updateRepos() {

		pd = ProgressDialog.show(this, getString(R.string.process_wait_title),
				getString(R.string.process_update_msg), true, true);
		pd.setIcon(android.R.drawable.ic_dialog_info);
		pd.setCanceledOnTouchOutside(false);

		Intent intent = new Intent(this, UpdateService.class);
		mUpdateReceiver = new UpdateReceiver(new Handler());
		intent.putExtra("receiver", mUpdateReceiver);
		startService(intent);
	}

	private TabManager getTabManager() {
		if (tabManager == null) {
			tabManager = TabManager.create(this, viewPager);
		}
		return tabManager;
	}

	public void refreshUpdateTabLabel() {
		getTabManager().refreshTabLabel(TabManager.INDEX_CAN_UPDATE);
	}

}
