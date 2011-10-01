package com.tectria.imrek;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.*;

public class IMrekActivity extends TabActivity {
	
	private String deviceID;
	private boolean started;
	private SharedPreferences prefs;
	private Editor editor;
	private TextView status;
	private ImageView statusicon;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
        
        status = (TextView)findViewById(R.id.status);
        statusicon = (ImageView)findViewById(R.id.statusicon);
        setDisconnected();
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        started = prefs.getBoolean(IMrekPushService.PREF_STARTED, false);
        deviceID = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        boolean test = prefs.getBoolean("checkbox_preference", false);
        
        //if(!started) {
        	/*editor = getSharedPreferences(IMrekPushService.TAG, MODE_PRIVATE).edit();
        	editor.putString(IMrekPushService.PREF_DEVICE_ID, deviceID);
	    	editor.commit();
        	IMrekPushService.actionStart(getApplicationContext());*/
        //}
        
        Resources res = getResources();
        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;
        Intent intent;

        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, FriendsListActivity.class);

        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost.newTabSpec("friendslist").setIndicator("Friends List",
                          res.getDrawable(R.drawable.friends_icons))
                      .setContent(intent);
        tabHost.addTab(spec);
        
        intent = new Intent().setClass(this, ConversationListActivity.class);
        spec = tabHost.newTabSpec("conversationlist").setIndicator("Conversation List",
                          res.getDrawable(R.drawable.list_icons))
                      .setContent(intent);
        tabHost.addTab(spec);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	started = prefs.getBoolean(IMrekPushService.PREF_STARTED, false);
        
        //if(!started) {
        	/*editor = getSharedPreferences(IMrekPushService.TAG, MODE_PRIVATE).edit();
        	editor.putString(IMrekPushService.PREF_DEVICE_ID, deviceID);
	    	editor.commit();
        	IMrekPushService.actionStart(getApplicationContext());*/
        //}
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
			IMrekPushService.actionStop(getApplicationContext());
			finish();
			break;
		}
		return true;
	}
    
    public void setConnected() {
    	status.setTextColor(getResources().getColor(R.color.connectedColor));
    	status.setText("Connected");
    	statusicon.setImageResource(R.drawable.icon_connected);
    }
	
	public void setDisconnected() {
		status.setTextColor(getResources().getColor(R.color.disconnectedColor));
		status.setText("Disconnected");
    	statusicon.setImageResource(R.drawable.icon_disconnected);
	}
}