package com.tectria.imrek;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ConversationListActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView name = new TextView(this);
        name.setText("Conversation List");
        setContentView(name);
    }
}