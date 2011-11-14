package com.tectria.imrek.fragments;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.tectria.imrek.IMrekChannels;
import com.tectria.imrek.IMrekMqttService;
import com.tectria.imrek.R;
import com.tectria.imrek.util.IMrekConversationManager;
import com.tectria.imrek.util.IMrekPreferenceManager;

public class ChannelFragment extends ListFragment {
    
	//Misc
	Context context;
	InputMethodManager imm;
	
	//List Adapter Stuff
	public String topic;
	ArrayList<HashMap<String, String>> items;
	final String[] to = new String[]{"name", "message"};
	final int[] from = new int[]{R.id.name, R.id.message};
	SimpleAdapter adapter;
    HashMap<String, String> map;
    
    //Views
    View layout;
    TextView channel;
    ViewGroup viewContainer; //Allows us to access the main activity's window token, so save it
    Button sendbutton;
	EditText sendtext;
	ImageButton clearmessages;
	ImageButton closechannel;
	
    public void setConnected() {
    	sendbutton.setEnabled(true);
    }
	
    public void setDisconnected() {
    	sendbutton.setEnabled(false);
    }
    
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
		
		layout = inflater.inflate(R.layout.f_channel, container, false);
		//Get Views
		channel = (TextView)layout.findViewById(R.id.channel);
		sendbutton = (Button)layout.findViewById(R.id.sendbutton);
		sendtext = (EditText)layout.findViewById(R.id.sendtext);
		closechannel = (ImageButton)layout.findViewById(R.id.closechannel);
        clearmessages = (ImageButton)layout.findViewById(R.id.clearmessages);
        
        closechannel.setOnClickListener(((IMrekChannels)getActivity()).cclistener);
        clearmessages.setOnClickListener(((IMrekChannels)getActivity()).cmlistener);
		
		//Set Some Handlers
		sendbutton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(sendtext.getText().toString() != "") {
					//TODO: adapt for new messaging protocol
					((IMrekChannels)getActivity()).sendMessage(IMrekMqttService.MQTT_PUBLISH, topic, IMrekPreferenceManager.getInstance(context).getUsername()+":"+sendtext.getText().toString(), null);
					sendtext.setText("");
				}
			}
		});
		
		sendtext.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
		        	//TODO: adapt for new messaging protocol
					((IMrekChannels)getActivity()).sendMessage(IMrekMqttService.MQTT_PUBLISH, topic, IMrekPreferenceManager.getInstance(context).getUsername()+":"+sendtext.getText().toString(), null);
		        	sendtext.setText("");
		        	imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		        	return true;
		        }
		        return false;
			}
		});
		
		//Set the channel name in the layout
		channel.setText(topic);
		
		//if there is no list adapter
		if(this.getListAdapter() == null) {
	        
	        //Get last 25 messages
	        items = IMrekConversationManager.getInstance(context).openChannelMessages(topic);
			
			//Create the adapter
			adapter = new SimpleAdapter(context, items, R.layout.item_message, to, from);
	        this.setListAdapter(adapter);
			
		//if we have a list adapter
		} else {
			//Fetch a channel update
			ArrayList<HashMap<String, String>> v = IMrekConversationManager.getInstance(context).getChannelUpdate(topic);
			
			//Loop through new messages and add them to items
			for(HashMap<String, String> map : v) {
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
	
	public void publishMessage(String channel, String message) {
		String[] m = message.split(":", 2);
		HashMap<String, String> h = new HashMap<String, String>();
		h.put(m[0],m[1]);
		items.add(h);
		IMrekConversationManager.getInstance(context).newMessageReceived(channel, message);
	}
}