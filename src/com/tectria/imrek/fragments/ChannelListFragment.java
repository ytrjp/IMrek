package com.tectria.imrek.fragments;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.tectria.imrek.IMrekChannels;
import com.tectria.imrek.R;
import com.tectria.imrek.util.IMrekConversationManager;
import com.tectria.imrek.util.IMrekHttpClient;

public class ChannelListFragment extends ListFragment {
	
	Context context;
	View layout;
	ImageButton newchannel;
	AlertDialog.Builder dialog;
	View dialogview;
	
	//Data for the channel list
	final String[] from = new String[] {"channel", "message"};
	final int[] to = new int[] { R.id.channel, R.id.lastm };
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
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
		
		layout = inflater.inflate(R.layout.f_channel_list, container, false);
		
        //Get channels
        items = IMrekConversationManager.getInstance(getActivity().getBaseContext()).getChannelsLastMessages();
		
		//Create the adapter
        adapter = new SimpleAdapter(context, items, R.layout.item_channel_list, from, to);
        this.setListAdapter(adapter);
        adapter.notifyDataSetChanged();
		
        newchannel = (ImageButton)layout.findViewById(R.id.newchannel);
        
        newchannel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialogview = inflater.inflate(R.layout.dialog_newchannel, null);
		    	dialog = new AlertDialog.Builder(container.getContext());
		    	dialog.setTitle("Create Channel");
		    	dialog.setView(dialogview);
		    	
		    	final EditText channelname = (EditText)dialogview.findViewById(R.id.channelname);
		    	
		    	dialog.setPositiveButton("Create", new DialogInterface.OnClickListener() {
		    		@Override
					public void onClick(final DialogInterface dialog, int id) {
		    			
		    			boolean exists = false;
		    			for(HashMap<String, String> map : items) {
		    				if(map.get("channel") == channelname.getText().toString()) {
		    					exists = true;
		    					break;
		    				}
		    			}
		    			
		    			if(channelname.getText().toString() != "" && !exists) {
		    				IMrekConversationManager.getInstance(getActivity().getBaseContext()).addChannel(channelname.getText().toString());
		    				HashMap<String, String> map = new HashMap<String, String>();
		    				map.put("channel", channelname.getText().toString());
		    				map.put("message", "");
		    				items.add(map);
		    				adapter.notifyDataSetChanged();
		    			}
		    			dialog.dismiss();
		           }
		        });
		    	dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		    		@Override
					public void onClick(final DialogInterface dialog, int id) {
		    			dialog.dismiss();
		           }
		        });
		    	dialog.show();
			}
        });
        
		return layout;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		registerForContextMenu(getListView());
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	  super.onCreateContextMenu(menu, v, menuInfo);
	  AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	  menu.setHeaderTitle(items.get(info.position).get("channel"));
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
	
	public void addChannel(String channel, String message) {
		map = new HashMap<String, String>();
		map.put("channel", channel);
		map.put("message", message);
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