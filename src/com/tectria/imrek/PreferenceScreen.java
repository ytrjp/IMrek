package com.tectria.imrek;

import java.util.ArrayList;
import java.util.Vector;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;

public class PreferenceScreen extends PreferenceActivity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle extras = getIntent().getExtras();
        ArrayList<String> channels = (ArrayList<String>)extras.get("channels");
        
        addPreferencesFromResource(R.layout.preferences);
        
		PreferenceCategory cp = (PreferenceCategory)findPreference("chan_notif");
		
		if(channels.size() > 0) {
			for(String channel : channels) {
				CheckBoxPreference cb = new CheckBoxPreference(this);
				cb.setKey("channel_" + channel);
				cb.setDefaultValue(true);
				cb.setTitle(channel);
				cb.setSummaryOff("Notifications are never shown for this channel");
				cb.setSummaryOn("Notifications are always shown for this channel");
				cp.addPreference(cb);
			}
		}
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	finish();
    }
}