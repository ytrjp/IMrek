package com.tectria.imrek;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

import com.tectria.imrek.util.IMrekMqttService;
import com.tectria.imrek.util.IMrekPreferenceManager;

public class IMrekMain extends TabActivity {
	
	//Tab Manager + Tabs
	Resources res;
	TabHost tabHost;
	TabHost.TabSpec friend_spec;
	TabHost.TabSpec convo_spec;
	Intent tabintent;
	
	//PreferenceManager + Preferences
	private IMrekPreferenceManager prefs;
	
	//Views
	private TextView status;
	private ImageView statusicon;
	
	//Dialogs
	private AlertDialog.Builder quitDialog;
	
	//Misc
	private Bundle extras;
	private String user;
	private String pass;
	
	Messenger mService = null;
	boolean isBound;

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
            try {
				mService.send(msg);
				sendMessage(IMrekMqttService.MSG_CONNECT, prefs.getUsername(), prefs.getToken());
			} catch (RemoteException e) {
				//The service crashed if we got here
	        	//But it should be restored by the system.
	        	//Until it is, we should just disconnect here and reset some preferences.
				// TODO: stuff
				setUIDisconnected();
			}
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
		            		// TODO: Reconnect topics
		            		// TODO: load friends list
		            		break;
		            	case IMrekMqttService.MQTT_CONNECTION_LOST:
		            		setUIDisconnected();
		            		// TODO: Clear friends / conversation list
		            		break;
		            	case IMrekMqttService.MQTT_DISCONNECTED:
		            		setUIDisconnected();
		            		// TODO: Clear friends / conversation list
		            		break;
		            	case IMrekMqttService.MSG_RECONNECT_CREDENTIALS:
		            		//Call a reconnect with the most recent, most-probably-valid credentials we can.
		            		//Call a reconnect with the most recent, most-probably-valid credentials we can.
		            		String u = prefs.getUsername();
		            		String t = prefs.getToken();
		            		if(u == "") {
		            			u = prefs.getLastUser();
		            		}
		            		if(t == "") {
		            			t = prefs.getLastToken();
		            		}
		            		sendMessage(IMrekMqttService.MSG_RECONNECT, u, t);
		            		break;
		            	case IMrekMqttService.MQTT_PUBLISH_ARRIVED:
		            		// TODO: Call conversation manager functions
		            		break;
		            	case IMrekMqttService.MQTT_PUBLISH_SENT:
		            		// TODO: Call conversation manager functions
		            		break;
		            	case IMrekMqttService.MQTT_SUBSCRIBE_SENT:
		            		// TODO: Call conversation manager functions
		            		break;
		            	case IMrekMqttService.MQTT_PUBLISH_FAILED:
		            		// TODO: Call conversation manager functions
		            		// TODO: Retry
		            		break;
		            	case IMrekMqttService.MQTT_SUBSCRIBE_FAILED:
		            		// TODO: Call conversation manager functions
		            		// TODO: Retry
		            		break;
		            	case IMrekMqttService.MQTT_CONNECT_FAILED:
		            		// TODO: Call conversation manager functions
		            		// TODO: Retry
		            		break;
		            	case IMrekMqttService.MSG_PING:
		            		sendMessage(IMrekMqttService.MQTT_SEND_KEEPALIVE, "keepalive");
		            		break;
		            	case IMrekMqttService.MQTT_KEEPALIVE_FAILED:
		            		sendMessage(IMrekMqttService.MSG_PING, "ping");
		            		// set timeout to kill service? Don't kill if received ping
		            		break;
	            	}
	                break;
	            default:
	                super.handleMessage(msg);
	        }
	    }
	    
	    @SuppressWarnings("unused")
		private void sendMessage(int command, String data1) {
			try {
	            Message msg = Message.obtain(null, IMrekMqttService.MSG_COMMAND, command, 0, null);
	            Bundle bundle = new Bundle();
	            bundle.putString("data1", data1);
	            msg.setData(bundle);
	            mService.send(msg);
	        } catch (RemoteException e) {
	            //The service crashed if we got here
	        	//But it should be restored by the system.
	        	//Until it is, we should just disconnect here and reset some preferences.
	        	handleCrashedService();
	        }
		}
		
		private void sendMessage(int command, String data1, String data2) {
			try {
	            Message msg = Message.obtain(null, IMrekMqttService.MSG_COMMAND, command, 0, null);
	            Bundle bundle = new Bundle();
	            bundle.putString("data1", data1);
	            bundle.putString("data2", data2);
	            msg.setData(bundle);
	            mService.send(msg);
	        } catch (RemoteException e) {
	            //The service crashed if we got here
	        	//But it should be restored by the system.
	        	//Until it is, we should just disconnect here and reset some preferences.
	        	handleCrashedService();
	        }
		}
		
	    @SuppressWarnings("unused")
		private void sendMessage(int command, String data1, String data2, String data3) {
			try {
	            Message msg = Message.obtain(null, IMrekMqttService.MSG_COMMAND, command, 0, null);
	            Bundle bundle = new Bundle();
	            bundle.putString("data1", data1);
	            bundle.putString("data2", data2);
	            bundle.putString("data3", data3);
	            msg.setData(bundle);
	            mService.send(msg);
	        } catch (RemoteException e) {
	            //The service crashed if we got here
	        	//But it should be restored by the system.
	        	//Until it is, we should just disconnect here and reset some preferences.
	        	handleCrashedService();
	        }
		}
	}
	
	private void sendMessage(int command, String data1) {
		try {
            Message msg = Message.obtain(null, IMrekMqttService.MSG_COMMAND, command, 0, null);
            Bundle bundle = new Bundle();
            bundle.putString("data1", data1);
            msg.setData(bundle);
            mService.send(msg);
        } catch (RemoteException e) {
            //The service crashed if we got here
        	//But it should be restored by the system.
        	//Until it is, we should just disconnect here and reset some preferences.
        	handleCrashedService();
        }
	}
	
	private void sendMessage(int command, String data1, String data2) {
		try {
            Message msg = Message.obtain(null, IMrekMqttService.MSG_COMMAND, command, 0, null);
            Bundle bundle = new Bundle();
            bundle.putString("data1", data1);
            bundle.putString("data2", data2);
            msg.setData(bundle);
            mService.send(msg);
        } catch (RemoteException e) {
            //The service crashed if we got here
        	//But it should be restored by the system.
        	//Until it is, we should just disconnect here and reset some preferences.
        	handleCrashedService();
        }
	}
	
	@SuppressWarnings("unused")
	private void sendMessage(int command, String data1, String data2, String data3) {
		try {
            Message msg = Message.obtain(null, IMrekMqttService.MSG_COMMAND, command, 0, null);
            Bundle bundle = new Bundle();
            bundle.putString("data1", data1);
            bundle.putString("data2", data2);
            bundle.putString("data3", data3);
            msg.setData(bundle);
            mService.send(msg);
        } catch (RemoteException e) {
            //The service crashed if we got here
        	//But it should be restored by the system.
        	//Until it is, we should just disconnect here and reset some preferences.
        	handleCrashedService();
        }
	}
	
	public void handleCrashedService() {
		//Set everything as disconnected
		setDisconnected();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Request window feature for custom title
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        
        //Actually set a custom title using our XML
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
        
        //Get our preference manager
        prefs = IMrekPreferenceManager.getInstance(this);
        
      //If we aren't logged in,
        if(!prefs.getLoggedIn()) {
        	//Credentials aren't yet verified
        	prefs.setVerified(false);
        	//Grab the intent extras
        	extras = this.getIntent().getExtras();
        	//If we're being passed a user/pass combo
            if(this.getIntent().hasExtra("user") && this.getIntent().hasExtra("pass")) {
            	//Set our current user/pass to the combo from the bundle
            	user = extras.getString("user");
            	pass = extras.getString("pass");
            	//If user/pass is blank and/or less than the required length
            	if((user.equals("") || pass.equals("")) || (user.length() < 5 || pass.length() < 6)) {
            		//If we get here, then somehow we were passed an invalid user/pass combo from the login activity
            		//Disconnect and log out, clear the user/pass in the preferences, and return to the splash/login activity
            		sendMessage(IMrekMqttService.MSG_STOP, "Logging Out");
            		prefs.setLoggedIn(false);
            		prefs.clearSavedUser();
            		Intent intent = new Intent(getBaseContext(), SplashScreenLogin.class);
        			startActivity(intent);
        			finish();
            	} else {
            		prefs.setVerified(true);
            	}
            //If there's no user/pass in the bundle
            } else {
            	//Default to the user/pass in the preferences
            	user = prefs.getUsername();
            	pass = prefs.getPassword();
            	//If user/pass is blank and/or less than the required length
            	if((user.equals("") || pass.equals("")) || (user.length() < 5 || pass.length() < 6)) {
            		//If we get here, we somehow have an invalid user or password in the preferences.
            		//Disconnect and log out, clear the user/pass in the preferences, and return to the splash/login activity
            		sendMessage(IMrekMqttService.MSG_STOP, "Logging Out");
            		prefs.setLoggedIn(false);
            		prefs.clearSavedUser();
            		Intent intent = new Intent(getBaseContext(), SplashScreenLogin.class);
        			startActivity(intent);
        			finish();
            	} else {
            		prefs.setVerified(true);
            	}
            }
        }
        
        //Views
        status = (TextView)findViewById(R.id.status);
        statusicon = (ImageView)findViewById(R.id.statusicon);
        
        //Start the UI off as disconnected
        setUIDisconnected();
        
        //Let the service know it doesn't have to reconnect
        //The service won't reconnect if it doesn't think it was previously started
        prefs.setWasStarted(false);
        
        doBindService();
        
        //Set up tabs
        //Do this last because if for whatever reason we need to fall back to the splash/login,
        //this isn't needed
        res = getResources();
    	tabHost = getTabHost();
		
		tabintent = new Intent().setClass(this, FriendsListTab.class);
		friend_spec = tabHost.newTabSpec("friendslist").setIndicator("Friends List", res.getDrawable(R.drawable.friends_icons)).setContent(tabintent);
		tabHost.addTab(friend_spec);
		
		tabintent = new Intent().setClass(this, ConversationListTab.class);
		convo_spec = tabHost.newTabSpec("conversationlist").setIndicator("Conversation List", res.getDrawable(R.drawable.list_icons)).setContent(tabintent);
		tabHost.addTab(convo_spec);
		
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem mi) {
		switch(mi.getItemId()) {
			case R.id.preferences:
				Intent prefIntent = new Intent(getBaseContext(), PreferenceScreen.class);
				startActivity(prefIntent);
				break;
			case R.id.logout:
				prefs.setLoggedIn(false);
				prefs.setAutoLogin(false);
				Intent intent = new Intent(getBaseContext(), SplashScreenLogin.class);
				startActivity(intent);
				finish();
				break;
			case R.id.reconnect:
				sendMessage(IMrekMqttService.MSG_RECONNECT, "Reconnect");
				break;
			case R.id.quit:
				quitDialog = new AlertDialog.Builder(this);
				quitDialog.setMessage("Are you sure you want to quit? This will close IMrek and disconnect you from the server.");
				quitDialog.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						sendMessage(IMrekMqttService.MSG_STOP, "Quitting");
						setDisconnected();
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
				break;
		}
		return true;
	}
    
	public void setConnected() {
		prefs.setIsConnected(true);
    	setUIConnected();
	}
	
	public void setDisconnected() {
		prefs.setIsConnected(false);
    	setUIDisconnected();
	}
    
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
}