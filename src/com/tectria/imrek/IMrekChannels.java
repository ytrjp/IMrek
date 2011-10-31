package com.tectria.imrek;

import java.util.List;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.tectria.imrek.IMrekMain.IncomingHandler;
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
	
	//Dialogs
	private AlertDialog.Builder quitDialog;
	
	Messenger mService = null;
	boolean isBound;
	
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
    
	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger msgr = new Messenger(new IncomingHandler());

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	        mService = new Messenger(service);

	        // We want to monitor the service for as long as we are
	        // connected to it.
	        Message msg = Message.obtain(null, IMrekMqttService.MSG_REGISTER_CLIENT);
            msg.replyTo = msgr;
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mService = null;
	        // TODO: stuff
	        setUIDisconnected();
	    }
	};
	
	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because there is no reason to be able to let other
	    // applications replace our component.
	    bindService(new Intent(getBaseContext(), IMrekMqttService.class), mConnection, Context.BIND_AUTO_CREATE);
	    isBound = true;
	}

	void doUnbindService() {
	    if (isBound) {
	        // If we have received the service, and hence registered with
	        // it, then now is the time to unregister.
	        if (mService != null) {
	            try {
	                Message msg = Message.obtain(null, IMrekMqttService.MSG_UNREGISTER_CLIENT);
	                msg.replyTo = msgr;
	                mService.send(msg);
	            } catch (RemoteException e) {
	                // There is nothing special we need to do if the service
	                // has crashed.
	            }
	        }

	        // Detach our existing connection.
	        unbindService(mConnection);
	        isBound = false;
	    }
	}
	
	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
	    @Override
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	            case IMrekMqttService.MSG_RESPONSE:
	            	@SuppressWarnings("unused")
	            	Bundle bundle = msg.getData();
	            	int cmd = msg.arg1;
	            	switch(cmd) {
		            	case IMrekMqttService.MQTT_CONNECTED:
		            		setUIConnected();
		            		for(int i=0;i<fragments.size();i++) {
		            			((ChannelFragment) fragments.get(i)).setConnected();
		            		}
		            		break;
		            	case IMrekMqttService.MQTT_CONNECTION_LOST:
		            		setUIDisconnected();
		            		for(int i=0;i<fragments.size();i++) {
		            			((ChannelFragment) fragments.get(i)).setDisconnected();
		            		}
		            		break;
		            	case IMrekMqttService.MQTT_DISCONNECTED:
		            		setUIDisconnected();
		            		for(int i=0;i<fragments.size();i++) {
		            			((ChannelFragment) fragments.get(i)).setDisconnected();
		            		}
		            		break;
		            	case IMrekMqttService.MQTT_PUBLISH_ARRIVED:
		            		//Add message to appropriate conversation
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
	                break;
	            default:
	                super.handleMessage(msg);
	        }
	    }
	    @SuppressWarnings("unused")
		public void sendMessage(int command, String data1) {
			try {
	            Message msg = Message.obtain(null, IMrekMqttService.MSG_COMMAND, command, 0, null);
	            Bundle bundle = new Bundle();
	            bundle.putString("data1", data1);
	            msg.setData(bundle);
	            mService.send(msg);
	        } catch (RemoteException e) {
	        	//Nothing!
	        }
		}
		
	    public void sendMessage(int command, String data1, String data2) {
			try {
	            Message msg = Message.obtain(null, IMrekMqttService.MSG_COMMAND, command, 0, null);
	            Bundle bundle = new Bundle();
	            bundle.putString("data1", data1);
	            bundle.putString("data2", data2);
	            msg.setData(bundle);
	            mService.send(msg);
	        } catch (RemoteException e) {
	        	//Nothing!
	        }
		}
		
	    @SuppressWarnings("unused")
	    public void sendMessage(int command, String data1, String data2, String data3) {
			try {
	            Message msg = Message.obtain(null, IMrekMqttService.MSG_COMMAND, command, 0, null);
	            Bundle bundle = new Bundle();
	            bundle.putString("data1", data1);
	            bundle.putString("data2", data2);
	            bundle.putString("data3", data3);
	            msg.setData(bundle);
	            mService.send(msg);
	        } catch (RemoteException e) {
	        	//Nothing!
	        }
		}
	}
	
	public void sendMessage(int command, String data1) {
		try {
            Message msg = Message.obtain(null, IMrekMqttService.MSG_COMMAND, command, 0, null);
            Bundle bundle = new Bundle();
            bundle.putString("data1", data1);
            msg.setData(bundle);
            mService.send(msg);
        } catch (RemoteException e) {
            //Nothing!
        }
	}
	
	public void sendMessage(int command, String data1, String data2) {
		try {
            Message msg = Message.obtain(null, IMrekMqttService.MSG_COMMAND, command, 0, null);
            Bundle bundle = new Bundle();
            bundle.putString("data1", data1);
            bundle.putString("data2", data2);
            msg.setData(bundle);
            mService.send(msg);
        } catch (RemoteException e) {
        	//Nothing!
        }
	}
	
	@SuppressWarnings("unused")
	public void sendMessage(int command, String data1, String data2, String data3) {
		try {
            Message msg = Message.obtain(null, IMrekMqttService.MSG_COMMAND, command, 0, null);
            Bundle bundle = new Bundle();
            bundle.putString("data1", data1);
            bundle.putString("data2", data2);
            bundle.putString("data3", data3);
            msg.setData(bundle);
            mService.send(msg);
        } catch (RemoteException e) {
        	//Nothing!
        }
	}
	    
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
	
	public void addConvo(String name) {
		channels.add(name);
		bundle = new Bundle();
		bundle.putString("topic", name);
		fragments.add(Fragment.instantiate(this, ChannelFragment.class.getName(), bundle));
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
        
        //Get our preference manager
        prefs = IMrekPreferenceManager.getInstance(getBaseContext());
        
        doBindService();
        initializePaging();
        setUIDisconnected();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
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
			sendMessage(IMrekMqttService.MSG_RECONNECT, "Reconnect");
		}
		else if(mi.getItemId() == R.id.quit) {
			quitDialog = new AlertDialog.Builder(this);
			quitDialog.setMessage("Are you sure you want to quit? This will close IMrek and disconnect you from the server.");
			quitDialog.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sendMessage(IMrekMqttService.MSG_STOP, "Quitting");
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
}