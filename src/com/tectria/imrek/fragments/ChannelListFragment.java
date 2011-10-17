package com.tectria.imrek.fragments;

import java.util.ArrayList;
import java.util.HashMap;

import com.tectria.imrek.R;
import com.tectria.imrek.R.id;
import com.tectria.imrek.R.layout;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class ChannelListFragment extends ListFragment {
	
	Context context;
	View layout;
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		context = getActivity().getApplicationContext();
		
		layout = inflater.inflate(R.layout.f_channel_list, container, false);
		
		
		/*//Item mapping
        String[] from = new String[] {"topic", "mcount", "lastm",};
        int[] to = new int[] { R.id.topic, R.id.mcount, R.id.lastm };

        //ArrayList of HashMaps for the adapter
        ArrayList<HashMap<String, String>> channels = new ArrayList<HashMap<String, String>>();
        //Reusable HashMap
        HashMap<String, String> map;
        
        for(int i = 0;i < someArray.length(); i++) {
        	map = new HashMap<String, String>();
        	map.put("topic", );
        	map.put("mcount", );
        	map.put("lastm", );
        	channels.add(map);
        }

        //Pass ArrayList to adapter
        SimpleAdapter adapter = new SimpleAdapter(context, channels, R.layout.item_channel_list, from, to);
        this.setListAdapter(adapter);*/
        
		//Add appropriate handlers, etc here (to communicate with IMrekConversations)
		
		return layout;
	}
}