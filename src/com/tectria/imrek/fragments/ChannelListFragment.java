package com.tectria.imrek.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.tectria.imrek.IMrekChannels;
import com.tectria.imrek.R;
import com.tectria.imrek.util.IMrekConversationManager;

public class ChannelListFragment extends ListFragment {
	
	Context context;
	View layout;
	
	//Data for the channel list
	final String[] from = new String[] {"channel", "lastm"};
	final int[] to = new int[] { R.id.channel, R.id.lastm };
	ArrayList<HashMap<String, String>> items;
	SimpleAdapter adapter;
	//Reusable HashMap
    HashMap<String, String> map;
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		context = getActivity().getApplicationContext();
		items = IMrekConversationManager.getInstance(getActivity().getBaseContext()).getChannelsLastMessages();
        
        //TODO: Add appropriate handlers, etc (to communicate with IMrekConversations)
	}
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		registerForContextMenu(getListView());
		
		layout = inflater.inflate(R.layout.f_channel_list, container, false);
		
        //Get channels
        items = IMrekConversationManager.getInstance(getActivity().getBaseContext()).getChannelsLastMessages();
		
		//Create the adapter
        adapter = new SimpleAdapter(context, items, R.layout.item_channel_list, from, to);
        this.setListAdapter(adapter);
        adapter.notifyDataSetChanged();
		
		return layout;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
	  super.onCreateContextMenu(menu, v, menuInfo);
	  MenuInflater inflater = getActivity().getMenuInflater();
	  inflater.inflate(R.menu.channel_item, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		if(item.getItemId() == R.id.close) {
			IMrekConversationManager.getInstance(getActivity().getBaseContext()).removeChannel(items.get(info.position).get("channel"));
			items.remove(info.position);
			adapter.notifyDataSetChanged();
		} else {
			return super.onContextItemSelected(item);
		}
		return true;
	}
	
	@Override
	public void onListItemClick (ListView l, View v, int position, long id) {
		Intent intent = new Intent(context, IMrekChannels.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		intent.putExtra("index", position);
		startActivity(intent);
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