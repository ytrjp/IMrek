package com.tectria.imrek.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

import com.tectria.imrek.R;
import com.tectria.imrek.util.IMrekConversationManager;

public class FriendsListFragment extends ListFragment {
	
	Context context;
	View layout;
	
	//Data for the channel list
	Vector<String> channels;
	Vector<String> lastmessages;
	String[] from;
	int[] to;
	ArrayList<HashMap<String, String>> items;
	SimpleAdapter adapter;
	//Reusable HashMap
    HashMap<String, String> map;
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		context = getActivity().getApplicationContext();
		
		
        
        
        //TODO: Add appropriate handlers, etc (to communicate with IMrekConversations)
        
	}
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		layout = inflater.inflate(R.layout.f_channel_list, container, false);

        /*//Pass ArrayList to adapter
        adapter = new SimpleAdapter(context, items, R.layout.item_channel_list, from, to);
        this.setListAdapter(adapter);*/
		
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