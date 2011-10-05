package com.tectria.imrek;

import android.app.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class ConversationListActivity extends Activity {
	
	ListView list = null;
	String[] test = {"1", "2"};
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friendslist);
        
        list = (ListView)findViewById(R.id.friendslist);
        
        list.setAdapter(new ArrayAdapter<String>(this, R.layout.friends_list_item, R.id.friend_name, test));
        
        list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Toast.makeText(getApplicationContext(), test[position], Toast.LENGTH_SHORT).show();
			}
        });
    }
}