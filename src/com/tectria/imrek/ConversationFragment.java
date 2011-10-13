package com.tectria.imrek;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ConversationFragment extends Activity {
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView name = new TextView(this);
        name.setText("Conversation List");
        setContentView(name);
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	finish();
    }
}