package com.tectria.imrek;

import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.tectria.imrek.fragments.ChannelFragment;
import com.tectria.imrek.util.ChannelPagerAdapter;
import com.tectria.imrek.util.IMrekConversationManager;
import com.tectria.imrek.util.IMrekMqttService;
import com.tectria.imrek.util.IMrekPreferenceManager;

public class IMrekChannels extends FragmentActivity {   
	
	IMrekPreferenceManager prefs;
	private ChannelPagerAdapter pageradapter;
	ViewPager pager;
	Vector<String> channels;
	List<Fragment> fragments;
	int index;
	
	//Bundle object to be reused
	Bundle bundle;
	
	//Some Views
	TextView status;
	ImageView statusicon;
	ImageButton closechannel;
	ImageButton clearmessages;
	
	//Dialogs
	private AlertDialog.Builder quitDialog;
	private ChannelsServiceReceiver svcReceiver;
	private boolean svcReceiverRegistered;
	public void setUIConnected() {
    	status.setTextColor(getResources().getColor(R.color.connectedColor));
		status.setText("Connected");
    	statusicon.setImageResource(R.drawable.icon_connected);
    }
    
    public void setUIDisconnected() {
		status.setTextColor(getResources().getColor(R.color.disconnectedColor));
		status.setText("Disconnected");
    	statusicon.setImageResource(R.drawable.icon_disconnected);
	}
    
    public OnClickListener cclistener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int cur = pager.getCurrentItem();
			if(cur - 1 >= 0) {
				pager.setCurrentItem(cur - 1);
				IMrekConversationManager.getInstance(getBaseContext()).removeChannel(channels.get(cur));
				channels.remove(cur);
				fragments.remove(cur);
				pageradapter.notifyDataSetChanged();
			} else if (cur + 1 < fragments.size()) {
				pager.setCurrentItem(cur + 1);
				channels.remove(cur);
				fragments.remove(cur);
				pageradapter.notifyDataSetChanged();
			} else {
				IMrekConversationManager.getInstance(getBaseContext()).removeChannel(channels.get(cur));
				finish();
			}
		}
    };
    
    public OnClickListener cmlistener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			IMrekConversationManager.getInstance(getBaseContext()).clearChat(channels.get(pager.getCurrentItem()));
		}
    };
	    
	/**
	 * Initialize the fragments to be paged
	 */
	private void initializePaging() {
		//Get a list of channels
		//For now use this test list
		channels = IMrekConversationManager.getInstance(getBaseContext()).getChannelList();
		 
		fragments = new Vector<Fragment>();
		for(int i=0;i<channels.size();i++) {
			bundle = new Bundle();
			bundle.putString("topic", channels.get(i));
			fragments.add(Fragment.instantiate(getBaseContext(), ChannelFragment.class.getName(), bundle));
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
        
        clearmessages.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				IMrekConversationManager.getInstance(getBaseContext()).clearChat(channels.get(pager.getCurrentItem()));
			}
        });
        //Get our preference manager
        prefs = IMrekPreferenceManager.getInstance(getBaseContext());
        
        initializePaging();
        setUIDisconnected();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if (!svcReceiverRegistered) {
    		registerReceiver(svcReceiver, new IntentFilter("com.tectria.imrek.MESSAGE"));
    		svcReceiverRegistered = true;
    	}
    	Vector<String> newchannels = IMrekConversationManager.getInstance(getBaseContext()).getChannelList();
    	for(String chan : newchannels) {
    		if(!channels.contains(chan)) {
    			channels.add(chan);
    			bundle = new Bundle();
    			bundle.putString("topic", chan);
    			fragments.add(Fragment.instantiate(getBaseContext(), ChannelFragment.class.getName(), bundle));
    		}
    	} 
    	for(String chan : channels) {
    		if(!newchannels.contains(chan)) {
    			fragments.remove(channels.indexOf(chan));
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
    	if (svcReceiverRegistered) {
    		unregisterReceiver(svcReceiver);
    		svcReceiverRegistered = false;
    	}
    }
    
 
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem mi) {
		if(mi.getItemId() == R.id.preferences) {
			Intent prefIntent = new Intent(getBaseContext(), PreferenceScreen.class);
			startActivity(prefIntent);
		}
		else if(mi.getItemId() == R.id.logout) {
			prefs.setLoggedIn(false);
			prefs.setAutoLogin(false);
			Intent intent = new Intent(getBaseContext(), SplashScreenLogin.class);
			startActivity(intent);
			finish();
		}
		else if(mi.getItemId() == R.id.reconnect) {
			//TODO: Adapt for new message protocol
			//sendMessage(IMrekMqttService.MSG_RECONNECT, "Reconnect");
		}
		else if(mi.getItemId() == R.id.quit) {
			quitDialog = new AlertDialog.Builder(this);
			quitDialog.setMessage("Are you sure you want to quit? This will close IMrek and disconnect you from the server.");
			quitDialog.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//TODO: Adapt for new message protocol
					//sendMessage(IMrekMqttService.MSG_STOP, "Quitting");
					for(int i=0;i<fragments.size();i++) {
            			((ChannelFragment) fragments.get(i)).setDisconnected();
            		}
					setUIDisconnected();
					prefs.setLoggedIn(false);
					finish();
				}
			}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			quitDialog.show();	
		}
		return true;
	}
    
    private void sendMessage(int message_type, String arg1, String arg2, String arg3) {
		Intent i = new Intent(getApplicationContext(), IMrekMqttService.class);
		Bundle b = new Bundle();
		b.putInt("message_type", message_type);
		b.putString("arg1", arg1);
		b.putString("arg2", arg2);
		b.putString("arg3", arg3);
		i.putExtras(b);
		startService(i);
	}
    
    public static class ChannelsServiceReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle b = intent.getExtras();
			switch (b.getInt("msgtype")) {
			
			}
		}
    	
    }
    
}