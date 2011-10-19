package com.tectria.imrek.fragments;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

import com.tectria.imrek.R;

public class ChannelListFragment extends ListFragment {
	
	Context context;
	View layout;
	
	//Data for the channel list
	String[] channels;
	String[] lastmessages;
	ArrayList<HashMap<String, String>> items;
	SimpleAdapter adapter;
	//Reusable HashMap
    HashMap<String, String> map;
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		context = getActivity().getApplicationContext();
		
		layout = inflater.inflate(R.layout.f_channel_list, container, false);
		
		//Get these from the database
		channels = new String[]{"one", "two", "three"};
		lastmessages = new String[]{"lastone", "lasttwo", "lastthree"};
		
		//Item mapping
        String[] from = new String[] {"channel", "lastm"};
        int[] to = new int[] { R.id.channel, R.id.lastm };

        //ArrayList of HashMaps for the adapter
        items = new ArrayList<HashMap<String, String>>();
        
        for(int i = 0;i < channels.length; i++) {
        	map = new HashMap<String, String>();
        	map.put("channel", channels[i]);
        	map.put("lastm", lastmessages[i]);
        	items.add(map);
        }

        //Pass ArrayList to adapter
        adapter = new SimpleAdapter(context, items, R.layout.item_channel_list, from, to);
        this.setListAdapter(adapter);
        
		//TODO: Add appropriate handlers, etc (to communicate with IMrekConversations)
		
		return layout;
	}
	
	public void addChannel(String channel, String lastm) {
		map = new HashMap<String, String>();
		map.put("channel", channel);
		map.put("lastm", lastm);
		items.add(map);
		adapter.notifyDataSetChanged();
	}
	
	public void removeChannel(String channel) {
		for(int i=0;i<items.size();i++) {
			if(items.get(i).containsValue(channel)) {
				items.remove(i);
			}
		}
		adapter.notifyDataSetChanged();
	}
}