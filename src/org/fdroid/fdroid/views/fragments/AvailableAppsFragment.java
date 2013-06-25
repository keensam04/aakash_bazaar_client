package org.fdroid.fdroid.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.fdroid.fdroid.AppListAdapter;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.AppListView;

public class AvailableAppsFragment extends AppListFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		AppListView view = new AppListView(getActivity());
		view.setOrientation(LinearLayout.HORIZONTAL);
		view.setWeightSum(1f);

		ListView lvCategories = new ListView(getActivity());

		// Giving it an ID lets the default save/restore state
		// functionality do its stuff.
		lvCategories.setId(R.id.categoryListView);
		lvCategories.setAdapter(getAppListManager().getCategoriesAdapter());
		lvCategories.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				String category = parent.getItemAtPosition(pos).toString();
				getAppListManager().setCurrentCategory(category);
			}
		});

		LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		p1.weight = 0.7f;

		view.addView(lvCategories, p1);

		ListView list = createAppListView();
		view.setAppList(list);

		LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		p2.weight = 0.3f;

		view.addView(list, p2);

		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	
	

	@Override
	protected AppListAdapter getAppListAdapter() {
		return getAppListManager().getAvailableAdapter();
	}
}
