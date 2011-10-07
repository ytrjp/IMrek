package com.tectria.imrek;

import com.tectria.imrek.util.IMrekPushService;

import android.app.TabActivity;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.view.*;
import android.widget.*;

public class IMrekActivity extends TabActivity {
	
	//Tab Manager + Tabs
	Resources res;
	TabHost tabHost;
	TabHost.TabSpec spec;
	Intent tabintent;
	
	//PreferenceManager + Preferences
	private SharedPreferences prefs;
	private Editor editor;
	private boolean started;
	
	//Views
	private TextView status;
	private ImageView statusicon;
	
	//Misc
	private Bundle extras;
	private boolean quitting = false;
	private String deviceid;
	private String user;
	private String pass;
	private String token;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Request window feature for custom title
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        
        //Actually set a custom title using our XML
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
        
        //Get our preference manager
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        //Get our deviceid
        deviceid = prefs.getString("deviceid", getDeviceID());
        token = prefs.getString("token", null);
        
        //Grab some views, used to change UI status to connected/disconnected
        status = (TextView)findViewById(R.id.status);
        statusicon = (ImageView)findViewById(R.id.statusicon);
        
        //We start off with the UI status set to disconnected
        setUIDisconnected();
        
        //If we aren't logged in,
        if(!prefs.getBoolean("loggedin", false)) {
        	//Grab the intent extras
        	extras = this.getIntent().getExtras();
        	//If we're being passed a user/pass combo
            if(extras.containsKey("user") && extras.containsKey("pass")) {
            	//Set our current user/pass to the combo from the bundle
            	user = extras.getString("user");
            	pass = extras.getString("pass");
            	//If user/pass is blank and/or less than the required length
            	if((user.equals("") || pass.equals("")) || (user.length() < 5 || pass.length() < 6)) {
            		//If we get here, then somehow we were passed an invalid user/pass combo from the login activity
            		//Disconnect and log out, clear the user/pass in the preferences, and return to the splash/login activity
            		disconnect();
            		setLoggedOut();
            		clearSavedUser();
            		Intent intent = new Intent(getBaseContext(), IMrekSplashLoginActivity.class);
        			startActivity(intent);
            	}
            //If there's no user/pass in the bundle
            } else {
            	//Default to the user/pass in the preferences
            	user = prefs.getString("user", "");
            	pass = prefs.getString("pass", "");
            	//If user/pass is blank and/or less than the required length
            	if((user.equals("") || pass.equals("")) || (user.length() < 5 || pass.length() < 6)) {
            		//If we get here, we somehow have an invalid user or password in the preferences.
            		//Disconnect and log out, clear the user/pass in the preferences, and return to the splash/login activity
            		disconnect();
            		setLoggedOut();
            		clearSavedUser();
            		Intent intent = new Intent(getBaseContext(), IMrekSplashLoginActivity.class);
        			startActivity(intent);
            	}
            }
        }
        
        //Check if service is started
        started = prefs.getBoolean(IMrekPushService.PREF_STARTED, false);
        
        //Start the service
        connect(user, pass);
        
        //Set up tabs
        //Do this last because if for whatever reason we need to fall back to the splash/login,
        //this isn't needed
        res = getResources();
    	tabHost = getTabHost();
		
		tabintent = new Intent().setClass(this, FriendsListActivity.class);
		spec = tabHost.newTabSpec("friendslist").setIndicator("Friends List", res.getDrawable(R.drawable.friends_icons)).setContent(tabintent);
		tabHost.addTab(spec);
		
		tabintent = new Intent().setClass(this, ConversationListActivity.class);
		spec = tabHost.newTabSpec("conversationlist").setIndicator("Conversation List", res.getDrawable(R.drawable.list_icons)).setContent(tabintent);
		tabHost.addTab(spec);
    }
    
    /*
     * Make sure the service is started when we resume
     */
    @Override
    public void onResume() {
    	super.onResume();
        connect(user, pass);
    }
    
    /*
     * Make sure service is started, as long as we aren't quitting via the menu
     */
    @Override
    public void onStop() {
    	super.onStop();
    	if(!quitting) {
    		connect(user, pass);
    	}
    }
    
    /*
     * Make sure service is started, as long as we aren't quitting via the menu
     */
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	if(!quitting) {
    		connect(user, pass);
    	}
    }
    
    /*
     * Make sure the service is started after we restart
     */
    @Override
    public void onRestart() {
    	super.onRestart();
        connect(user, pass);
    }
    
    /*
     * Prevent the use of the back button within the tab manager activity.
     */
/*    @Override
    public void onBackPressed() {
    	return;
    }*/
    
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
		case R.id.restart:
			disconnect();
			setLoggedOut();
			connect(user, pass);
			setLoggedIn();
			break;
		case R.id.quit:
			quitting = true;
			disconnect();
			setLoggedOut();
			finish();
			break;
		}
		return true;
	}
    
    /*
     * Get deviceid and save it to preferences
     */
    public String getDeviceID() {
    	String id = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
    	editor = prefs.edit();
    	editor.putString("deviceid", id);
    	editor.commit();
    	return id;
    }
    
    /*
     * Clear saved user/pass in the preferences
     * Also clear last_user and last_pass, used by the push service for post-mortem recovery
     */
    public void clearSavedUser() {
    	editor = prefs.edit();
    	editor.remove("user");
    	editor.remove("pass");
    	editor.remove("last_user");
    	editor.remove("last_pass");
    	editor.commit();
    }
    
    /*
     * Set shared preference 'loggedin' to true
     */
    public void setLoggedIn() {
    	editor = prefs.edit();
    	editor.putBoolean("loggedin", true);
    	editor.commit();
    }
    
    /*
     * Set shared preference 'loggedin' to false
     */
    public void setLoggedOut() {
    	editor = prefs.edit();
    	editor.putBoolean("loggedin", false);
    	editor.commit();
    }
    
    /*
     * Start the MQTT push service if it isn't already started, and update the UI accordingly
     */
    public void connect(String mqtt_user, String mqtt_pass) {
    	//Check is service is started
    	started = prefs.getBoolean(IMrekPushService.PREF_STARTED, false);
    	//Actually start the service
		if(!started) {
			IMrekPushService.actionStart(getApplicationContext(), mqtt_user, mqtt_pass);
		}
		//Update UI to reflect service status
		setUIConnected();
    }
    
    public void setUIConnected() {
    	status.setTextColor(getResources().getColor(R.color.disconnectedColor));
		status.setText("Disconnected");
    	statusicon.setImageResource(R.drawable.icon_disconnected);
    }
	
    /*
     * Stop the MQTT push service if it isn't already stopped, and update the UI accordingly
     */
	public void disconnect() {
		//Check is service is started
    	started = prefs.getBoolean(IMrekPushService.PREF_STARTED, false);
		//Actually stop the service
		if(started) {
			IMrekPushService.actionStop(getApplicationContext());
		}
		//Update UI to reflect service status
		setUIDisconnected();
	}
	
	public void setUIDisconnected() {
		status.setTextColor(getResources().getColor(R.color.disconnectedColor));
		status.setText("Disconnected");
    	statusicon.setImageResource(R.drawable.icon_disconnected);
	}
}