package com.tectria.imrek;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.*;

public class IMrekActivity extends TabActivity {
	
	private String deviceID;
	private boolean started;
	private SharedPreferences prefs;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        prefs = getSharedPreferences(PushService.TAG, MODE_PRIVATE);
        started = prefs.getBoolean(PushService.PREF_STARTED, false);
        deviceID = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        
        if(!started) {
        	PushService.actionStart(getApplicationContext());
        }
        
        Resources res = getResources();
        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;
        Intent intent;

        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, FriendsListActivity.class);

        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost.newTabSpec("artists").setIndicator("Artists",
                          res.getDrawable(R.drawable.icon))
                      .setContent(intent);
        tabHost.addTab(spec);
        
        intent = new Intent().setClass(this, ConversationListActivity.class);
        spec = tabHost.newTabSpec("albums").setIndicator("Albums",
                          res.getDrawable(R.drawable.icon))
                      .setContent(intent);
        tabHost.addTab(spec);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
        started = prefs.getBoolean(PushService.PREF_STARTED, false);
        
        if(!started) {
        	PushService.actionStart(getApplicationContext());
        }
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem mi) {
		switch(mi.getItemId()) {
		case R.id.preferences:
			Intent prefIntent = new Intent(getBaseContext(), PreferencesActivity.class);
			startActivity(prefIntent);
			break;
		case R.id.quit:
			PushService.actionStop(getApplicationContext());
			finish();
			break;
		}
		return true;
	}
}