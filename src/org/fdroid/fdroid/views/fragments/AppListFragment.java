package org.fdroid.fdroid.views.fragments;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import org.fdroid.fdroid.*;
import org.fdroid.fdroid.views.AppListView;

public class AppListFragment extends Fragment implements AdapterView.OnItemClickListener {

    private FDroid parent;
    public static Context c;
    String app_id;

    protected AppListAdapter getAppListAdapter() {
		return null;
	}

    @Override
	public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            parent = (FDroid)activity;
            c = getActivity();
        } catch (ClassCastException e) {
            // I know fragments are meant to be activity agnostic, but I can't
            // think of a better way to share the one application list between
            // all three app list fragments.
            throw new RuntimeException(
                "AppListFragment can only be attached to FDroid activity. " +
                "Here it was attached to a " + activity.getClass() );
        }
    }

    public AppListManager getAppListManager() {
        return parent.getManager();
    }

    protected AppListView createPlainAppList() {
        AppListView view = new AppListView(getActivity());
        ListView list = createAppListView();
        view.addView(
                list,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        view.setAppList(list);
        return view;
    }

    protected ListView createAppListView() {
        ListView list = new ListView(getActivity());
        list.setFastScrollEnabled(true);
        list.setOnItemClickListener(this);
        list.setAdapter(getAppListAdapter());
        return list;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final DB.App app = (DB.App)getAppListAdapter().getItem(position);
        
        downloadScreenshots(app.id);
        
        app_id = app.id;
        Intent intent = new Intent(getActivity(), AppDetails.class);
		System.out.println("app id"+app_id);
		intent.putExtra("appid",app_id);
		startActivity(intent);
//        File my_file = new File(Environment.getExternalStorageDirectory()+"/my_file");
//        fileExists(my_file);
    }
    
    private void fileExists(File my_file) {
    	if(!my_file.exists()){
    		fileExists(my_file);
    	}
    	else{
    		my_file.delete();
    		
    	}
		
	}
    
    /*
     * download screenshots and store in /sdcard/fdroid and write database
     */
    
	public void downloadScreenshots(String appid) {
    	File fdroid = new File(Environment.getExternalStorageDirectory() + "/fdroid");
		if (!fdroid.exists()) {
			System.out.println("fdroid dir NOT exists!");
			fdroid.mkdir();	
			System.out.println("folder CREATED");
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
		
		new ParseUrl(c).execute(appid.toString());
    }
		
}
