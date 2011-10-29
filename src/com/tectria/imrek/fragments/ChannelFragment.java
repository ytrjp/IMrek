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
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.tectria.imrek.R;
import com.tectria.imrek.util.IMrekConversationManager;

public class ChannelFragment extends ListFragment {
    
	//Misc
	Context context;
	IMrekConversationManager cmanager;
	InputMethodManager imm;
	
	//List Adapter Stuff
	String topic;
	ArrayList<HashMap<String, String>> items;
	final String[] to = new String[]{"message"};
	final int[] from = new int[]{R.id.message};
	SimpleAdapter adapter;
    HashMap<String, String> map;
    
    //Views
    View layout;
    TextView channel;
    ViewGroup viewContainer; //Allows us to access the main activity's window token, so save it
	
	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		context = getActivity().getApplicationContext();
		
		Bundle args = this.getArguments();
		topic = args.getString("topic");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		//Save the container view so we can access the window token
		viewContainer = container;
		//get the input method manager service
		imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		cmanager = IMrekConversationManager.getInstance(context);
		
		layout = inflater.inflate(R.layout.f_channel, container, false);
		
		//Get Views
		channel = (TextView)layout.findViewById(R.id.channel);
		
		//Set the channel name in the layout
		channel.setText(topic);
		
		//if there is no list adapter
		if(this.getListAdapter() == null) {
	        
	        //Get last 25 messages
	        Vector<String> v = cmanager.openChannelMessages(topic);
	        
	        //Put the messages in a hashmap
			items = new ArrayList<HashMap<String, String>>();
			for(int i = 0;i < v.size(); i++) {
	        	map = new HashMap<String, String>();
	        	map.put("message", v.get(i));
	        	items.add(map);
	        }
			
			//Create the adapter
			adapter = new SimpleAdapter(context, items, R.layout.item_message, to, from);
	        this.setListAdapter(adapter);
			
		//if we have a list adapter
		} else {
			//Fetch a channel update
			Vector<String> v = IMrekConversationManager.getInstance(context).getChannelUpdate(topic);
			
			//Loop through new messages and add them to items
			for(String s : v) {
				map = new HashMap<String, String>();
				map.put("message", s);
				items.add(map);
			}
			
			//Notify the adapter that we added messages
			adapter.notifyDataSetChanged();
		}
		
		//Add appropriate handlers, etc here (to communicate with IMrekConversations)
		
		return layout;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		//Hide the soft keyboard
		imm.hideSoftInputFromWindow(viewContainer.getWindowToken(), 0);
	}
}