package com.autovol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

public class ResultsFragmentActivity extends Activity implements
		ActionBar.OnNavigationListener {

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_results_fragment);

		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
		// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<ClassifyType>(actionBar.getThemedContext(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, ClassifyType.values()), this);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.results_action_delete) {
			for (ClassifyType type : ClassifyType.values()) {
				File f = getFileStreamPath(ClassifyService.getFileName(type.toString()));
				if (f.exists()) {
					f.delete();
				}
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.results, menu);
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		// When the given dropdown item is selected, show its contents in the
		// container view.
		getFragmentManager()
				.beginTransaction()
				.replace(R.id.container,
						ResultsFragment.newInstance(ClassifyType.values()[position].toString())).commit();
		return true;
	}
	
	public static class ResultsFragment extends ListFragment {
		
		private String type;;
		private ArrayAdapter<String> adapter;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			type = getArguments().getString(TYPE);
			
			adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
			setListAdapter(adapter);
		}
		
		private final static String TYPE = "type";
		
		@Override
		public void onResume() {
			super.onResume();
			loadData();
			getListView().setSelection(adapter.getCount() - 1);
		}
		
		private void loadData() {
			BufferedReader reader;
			try {
				String filename = ClassifyService.getFileName(type);
				reader = new BufferedReader(
						new FileReader(getActivity().getFileStreamPath(filename)));
				
				adapter.clear();
				String line;
				while ((line = reader.readLine()) != null) {
					adapter.add(line);
				}
				adapter.notifyDataSetChanged();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static ResultsFragment newInstance(String type) {
			ResultsFragment fragment = new ResultsFragment();
			Bundle args = new Bundle();
			args.putString(TYPE,type);
			fragment.setArguments(args);
			return fragment;
		}
	}

}
