package com.autovol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

public class ResultsActivity extends ListActivity {
	
	ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_results);
		
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		setListAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.results, menu);
		return true;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		loadData();
		getListView().setSelection(adapter.getCount() - 1);
	}
	
	private void loadData() {
		BufferedReader reader;
		try {
			reader = new BufferedReader(
					new FileReader(getFileStreamPath(ClassifyService.FILE_NAME)));
			
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.results_action_delete) {
			File f = getFileStreamPath(ClassifyService.FILE_NAME);
			if (f.exists()) {
				f.delete();
				adapter.clear();
			}
			return true;
		} else if (id == R.id.results_action_refresh) {
			loadData();
			getListView().setSelection(adapter.getCount() - 1);
		}
		return super.onOptionsItemSelected(item);
	}

}
