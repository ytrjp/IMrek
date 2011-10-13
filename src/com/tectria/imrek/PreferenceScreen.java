package com.tectria.imrek;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PreferenceScreen extends PreferenceActivity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	finish();
    }
}