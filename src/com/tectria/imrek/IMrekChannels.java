package com.tectria.imrek;

import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.tectria.imrek.fragments.ChannelFragment;
import com.tectria.imrek.util.ChannelPagerAdapter;
import com.tectria.imrek.util.IMrekConversationManager;
import com.tectria.imrek.util.IMrekPreferenceManager;

public class IMrekChannels extends FragmentActivity {   
	
	private ChannelPagerAdapter pageradapter;
	ViewPager pager;
	Vector<String> channels;
	List<Fragment> fragments;
	List<String> fids;  
	int index;
	boolean switching;
	
	//Bundle object to be reused
	Bundle bundle;
	
	//Some Views
	TextView status;
	ImageView statusicon;
	
	private ChannelsBroadcastReceiver svcReceiver;
	private boolean svcReceiverRegistered;
	static final String MESSAGE_RECEIVER_ACTION = "com.tectria.imrek.MESSAGE";
	
	public void setUIConnected() {
    	status.setTextColor(getResources().getColor(R.color.connectedColor));
		status.setText("Connected");
    	statusicon.setImageResource(R.drawable.ic_connected);
    }
    
    public void setUIDisconnected() {
		status.setTextColor(getResources().getColor(R.color.disconnectedColor));
		status.setText("Disconnected");
    	statusicon.setImageResource(R.drawable.ic_disconnected);
	}
    
    public void setConnected() {
    	setUIConnected();
    	for(Fragment fragment : fragments) {
    		((ChannelFragment)fragment).setConnected();
    	}
    }
    
    public void setDisconnected() {
    	setUIDisconnected();
    	for(Fragment fragment : fragments) {
    		((ChannelFragment)fragment).setDisconnected();
    	}
    }
    
    public OnClickListener cclistener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int cur = pager.getCurrentItem();
			if(cur - 1 >= 0) {
				pager.setCurrentItem(cur - 1);
				IMrekConversationManager.getInstance(getBaseContext()).removeChannel(channels.get(cur));
				channels = IMrekConversationManager.getInstance(getBaseContext()).getChannelList();
				fragments.remove(cur);
				pageradapter.notifyDataSetChanged();
			} else {
				IMrekConversationManager.getInstance(getBaseContext()).removeChannel(channels.get(cur));
				switching = true;
				finish();
			}
		}
    };
    
    public OnClickListener cmlistener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			IMrekConversationManager.getInstance(getBaseContext()).clearChat(channels.get(pager.getCurrentItem()));
			((ChannelFragment)fragments.get(pager.getCurrentItem())).clearMessages();
		}
    };
	    
	/**
	 * Initialize the fragments to be paged
	 */
	private void initializePaging() {
		//Get a list of channels
		//For now use this test list
		channels = IMrekConversationManager.getInstance(getBaseContext()).getChannelList();
		fids = new Vector<String>();
		fragments = new Vector<Fragment>();
		for(int i=0;i<channels.size();i++) {
			bundle = new Bundle();
			bundle.putString("topic", channels.get(i));
			fragments.add(Fragment.instantiate(getBaseContext(), ChannelFragment.class.getName(), bundle));
			fids.add(channels.get(i));
		}
		pageradapter  = new ChannelPagerAdapter(super.getSupportFragmentManager(), fragments);
		pager = (ViewPager)super.findViewById(R.id.viewpager);
		pager.setAdapter(pageradapter);
		
		pager.setCurrentItem(index);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.channels);
        
        Bundle extras = getIntent().getExtras();
        index = extras.getInt("index");
        
        //Actually set a custom title using our XML
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
        
        //Get views
        status = (TextView)findViewById(R.id.status);
        statusicon = (ImageView)findViewById(R.id.statusicon);
        
        initializePaging();
        if(IMrekPreferenceManager.getInstance(getBaseContext()).getIsConnected()) {
        	setConnected();
        } else {
        	setDisconnected();
        }
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if (!svcReceiverRegistered) {
    		svcReceiver = new ChannelsBroadcastReceiver() {

    			@Override
    			public void gotBroadcast(Context context, Intent intent) {
    				Bundle bundle = intent.getExtras();
    				switch(bundle.getInt("msgtype")) {
    		        	case IMrekMqttService.MQTT_CONNECTED:
    		        		setConnected();
    		        		for(int i=0;i<fragments.size();i++) {
    		        			((ChannelFragment) fragments.get(i)).setConnected();
    		        		}
    		        		break;
    		        	case IMrekMqttService.MQTT_CONNECTION_LOST:
    		        		setDisconnected();
    		        		for(int i=0;i<fragments.size();i++) {
    		        			((ChannelFragment) fragments.get(i)).setDisconnected();
    		        		}
    		        		break;
    		        	case IMrekMqttService.MQTT_DISCONNECTED:
    		        		setDisconnected();
    		        		for(int i=0;i<fragments.size();i++) {
    		        			((ChannelFragment) fragments.get(i)).setDisconnected();
    		        		}
    		        		break;
    		        	case IMrekMqttService.MQTT_PUBLISH_ARRIVED:
    		        		//Add message to appropriate conversation
    		        		((ChannelFragment)fragments.get(fids.indexOf(bundle.getString("arg1")))).publishMessage(bundle.getString("arg1"), bundle.getString("arg2"));
    		        		break;
    		        	case IMrekMqttService.MQTT_PUBLISH_SENT:
    		        		//Indicate message was sent?
    		        		break;
    		        	case IMrekMqttService.MQTT_SUBSCRIBE_SENT:
    		        		//Add a fragment for the channel
    		        		break;
    		        	case IMrekMqttService.MQTT_UNSUBSCRIBE_SENT:
    		        		//Remove fragment for the channel
    		        		break;
    		        	case IMrekMqttService.MQTT_PUBLISH_FAILED:
    		        		//Turn the failed message red or something?
    		        		break;
    		        	case IMrekMqttService.MQTT_SUBSCRIBE_FAILED:
    		        		//Remove fragment for channel
    		        	case IMrekMqttService.MQTT_UNSUBSCRIBE_FAILED:
    		        		//Don't try and re-add. Just fallback and make sure fragment is removed
    		        		break;
    		    	}
    			}
    	    	
    	    };
    		registerReceiver(svcReceiver, new IntentFilter(MESSAGE_RECEIVER_ACTION));
    		svcReceiverRegistered = true;
    	}
    	Vector<String> newchannels = IMrekConversationManager.getInstance(getBaseContext()).getChannelList();
    	for(String chan : newchannels) {
    		if(!channels.contains(chan)) {
    			channels.add(chan);
    			bundle = new Bundle();
    			bundle.putString("topic", chan);
    			fragments.add(Fragment.instantiate(getBaseContext(), ChannelFragment.class.getName(), bundle));
    			fids.add(chan);
    		}
    	} 
    	for(String chan : channels) {
    		if(!newchannels.contains(chan)) {
    			fragments.remove(channels.indexOf(chan));
    			fids.remove(chan);
    			channels.remove(chan);
    		}
    	}
    	pageradapter.notifyDataSetChanged();
		pager.refreshDrawableState();
		pager.setCurrentItem(index);
		
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if (svcReceiverRegistered) {
    		unregisterReceiver(svcReceiver);
    		svcReceiverRegistered = false;
    	}
    	if(!switching) {
    		sendMessage(IMrekMqttService.SERVICE_STOP_FOREGROUND, null, null, null);
    	} else {
    		switching = false;
    	}
    }
    
    @Override
    public void onBackPressed() {
    	switching = true;
    	finish();
    }
    
    public void sendMessage(int msgtype, String arg1, String arg2, String arg3) {
    	Intent i = new Intent(MESSAGE_RECEIVER_ACTION);
		Bundle b = new Bundle();
		b.putInt("msgtype", msgtype);
		b.putString("arg1", arg1);
		b.putString("arg2", arg2);
		b.putString("arg3", arg3);
		i.putExtras(b);
		sendBroadcast(i);
	}
}