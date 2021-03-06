/*
    Copyright 2014 Alexander Wang
    
    This file is part of Ogame on Android.

    Ogame on Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Ogame on Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Ogame on Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.wikaba.ogapp;

import java.util.ArrayList;

import com.wikaba.ogapp.utils.AccountCredentials;
import com.wikaba.ogapp.utils.DatabaseManager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class NoAccountFragment extends Fragment
		implements OnClickListener,
			LoaderManager.LoaderCallbacks<ArrayList<AccountCredentials>>,
			AdapterView.OnItemClickListener {
	
	private static final int ALL_ACCS_LOADER_ID = 0;
	
	private Spinner uniSpinner;
	private EditText usernameField;
	private EditText passwdField;
	private Button loginButton;
	private CheckBox pwCheckBox;
	private HomeActivity act;
	private ListView existingAccs;
	private ArrayList<AccountCredentials> allAccounts;
	
	public NoAccountFragment() {
	}
	
	@Override
	public void onAttach(Activity act) {
		super.onAttach(act);
		this.act = (HomeActivity)act;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_no_acc, parent, false);
		uniSpinner = (Spinner)root.findViewById(R.id.uniSelect);
		usernameField = (EditText)root.findViewById(R.id.username);
		passwdField = (EditText)root.findViewById(R.id.password);
		loginButton = (Button)root.findViewById(R.id.login);
		existingAccs = (ListView)root.findViewById(R.id.existingAccList);
		pwCheckBox = (CheckBox)root.findViewById(R.id.pw_checkbox);
		
		String[] uniNames = getResources().getStringArray(R.array.universe_names);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(act, android.R.layout.simple_list_item_1, uniNames);
		uniSpinner.setAdapter(adapter);
		
		getLoaderManager().initLoader(ALL_ACCS_LOADER_ID, null, this);
		
		loginButton.setOnClickListener(this);
		
		registerForContextMenu(existingAccs);
		
		pwCheckBox.setOnClickListener(this);
		
		return root;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = act.getMenuInflater();
		inflater.inflate(R.menu.accounts, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if(itemId == R.id.remove) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			int rowPosition = info.position;
			AccountCredentials creds = allAccounts.get(rowPosition);
			
			DatabaseManager dbmanager = new DatabaseManager(act);
			dbmanager.removeAccount(creds.universe, creds.username);
			dbmanager.close();
			allAccounts.remove(rowPosition);
			AccountAdapter adapter = (AccountAdapter)existingAccs.getAdapter();
			adapter.notifyDataSetChanged();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch(id) {
		case R.id.login:
			String username = usernameField.getText().toString();
			String passwd = passwdField.getText().toString();
			View selectedView = uniSpinner.getSelectedView();
			if(selectedView == null) {
				Toast.makeText(act, "Please select a valid universe.", Toast.LENGTH_SHORT).show();
				return;
			}
			
			TextView selectedText = (TextView)selectedView;
			String universe = selectedText.getText().toString();
			
			act.addAccount(universe, username, passwd);
			break;
		case R.id.pw_checkbox:
			int inputType = (pwCheckBox.isChecked()) ?
					(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
					: (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			passwdField.setInputType(inputType);
			break;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		AccountCredentials cred = allAccounts.get(position);
		act.accountRowId = cred.id;
		act.goToOverview();
	}
	
	@Override
	public Loader<ArrayList<AccountCredentials>> onCreateLoader(int id, Bundle args) {
		return new LoadAccountsLoader(act);
	}
	
	@Override
	public void onLoadFinished(Loader<ArrayList<AccountCredentials>> loader, ArrayList<AccountCredentials> data) {
		AccountAdapter adapter = new AccountAdapter(act, data);
		existingAccs.setAdapter(adapter);
		existingAccs.setOnItemClickListener(this);
		allAccounts = data;
	}
	
	@Override
	public void onLoaderReset(Loader<ArrayList<AccountCredentials>> loader) {
	}
	
	private static class LoadAccountsLoader extends AsyncTaskLoader<ArrayList<AccountCredentials>> {
		private ArrayList<AccountCredentials> oldData;
		
		private DatabaseManager dbmanager;
		/**
		 * Initialize a LoadAccountsTask object that will be
		 * calling DatabaseManager methods through the parameter
		 * passed in
		 * @param dbman - the DatabaseManager object to use to 
		 */
		public LoadAccountsLoader(Context ctx) {
			super(ctx);
			dbmanager = new DatabaseManager(ctx);
			oldData = null;
		}
		
		@Override
		protected void onStartLoading() {
			if(oldData != null)
				deliverResult(oldData);
			
			this.forceLoad();
		}
		
		@Override
		protected void onStopLoading() {
		}
		
		@Override
		protected void onReset() {
		}
		
		@Override
		public void deliverResult(ArrayList<AccountCredentials> newData) {
			oldData = newData;
			super.deliverResult(newData);
		}
		
		@Override
		public ArrayList<AccountCredentials> loadInBackground() {
			ArrayList<AccountCredentials> allAccs = dbmanager.getAllAccounts();
			return allAccs;
		}
	}
	
	public static class AccountAdapter extends BaseAdapter {
		private ArrayList<AccountCredentials> accs;
		private Context ctx;
		
		public AccountAdapter(Context cont, ArrayList<AccountCredentials> accs) {
			this.accs = accs;
			ctx = cont;
		}

		@Override
		public int getCount() {
			return accs.size();
		}

		@Override
		public Object getItem(int position) {
			return accs.get(position);
		}

		@Override
		public long getItemId(int position) {
			return -1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView v = null;
			if(convertView == null) {
				LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = (TextView)inflater.inflate(R.layout.account_text_view, parent, false);
				
			}
			else {
				v = (TextView)convertView;
			}
			
			StringBuilder strb = new StringBuilder();
			AccountCredentials creds = accs.get(position);
			strb.append(creds.username)
			.append(" in ")
			.append(creds.universe);
			
			v.setText(strb.toString());
			
			return v;
		}
	}
}
