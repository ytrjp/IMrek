package com.tectria.imrek;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import android.app.AlertDialog;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

import com.tectria.imrek.fragments.ChannelListFragment;
import com.tectria.imrek.fragments.FriendsListFragment;
import com.tectria.imrek.util.IMrekConversationManager;
import com.tectria.imrek.util.IMrekMqttService;
import com.tectria.imrek.util.IMrekPreferenceManager;
import com.tectria.imrek.util.TabPagerAdapter;

public class IMrekMain extends FragmentActivity implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
	
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
	    @Override
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

	    @Override
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
		            		setConnected();
		            		// TODO: Reconnect topics
		            		// TODO: load friends list
		            		break;
		            	case IMrekMqttService.MQTT_CONNECTION_LOST:
		            		setDisconnected();
		            		// TODO: Clear friends / conversation list
		            		break;
		            	case IMrekMqttService.MQTT_DISCONNECTED:
		            		setDisconnected();
		            		// TODO: Clear friends / conversation list
		            		break;
		            	case IMrekMqttService.MSG_RECONNECT_CREDENTIALS:
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
		            		IMrekConversationManager.getInstance(getBaseContext()).newMessageReceived(bundle.getString("data1"), bundle.getString("data2"));
		            		break;
		            	case IMrekMqttService.MQTT_PUBLISH_SENT:
		            		// TODO: Check if we get an MQTT_PUBLISH_ARRIVED for our own messages
		            		break;
		            	case IMrekMqttService.MQTT_SUBSCRIBE_SENT:
		            		IMrekConversationManager.getInstance(getBaseContext()).addChannel(bundle.getString("data1"));
		            		break;
		            	case IMrekMqttService.MQTT_UNSUBSCRIBE_SENT:
		            		IMrekConversationManager.getInstance(getBaseContext()).removeChannel(bundle.getString("data1"));
		            		break;
		            	case IMrekMqttService.MQTT_PUBLISH_FAILED:
		            		// TODO: Call conversation manager functions
		            		// TODO: Retry
		            		break;
		            	case IMrekMqttService.MQTT_SUBSCRIBE_FAILED:
		            		// TODO: Retry?
		            	case IMrekMqttService.MQTT_UNSUBSCRIBE_FAILED:
		            		// TODO: Retry?
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
        
        this.initializeTabHost(savedInstanceState);
		if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab")); //set the tab as per the saved state
        }
		// Intialise ViewPager
		this.intialiseViewPager();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	doUnbindService();
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
    
    /**
     * 
     * 
     * 
     * Lots of fragment shit
     * 
     * 
     * 
     * 
     * 
     */
	private TabHost mTabHost;
	private ViewPager mViewPager;
	private HashMap<String, TabInfo> mapTabInfo = new HashMap<String, IMrekMain.TabInfo>();
	private TabPagerAdapter mPagerAdapter;
	
	/**
	 *
	 * Maintains extrinsic info of a tab's construct
	 */
	private class TabInfo {
		 private String tag;
         private Class<?> clss;
         private Bundle args;
         private Fragment fragment;
         TabInfo(String tag, Class<?> clazz, Bundle args) {
        	 this.tag = tag;
        	 this.clss = clazz;
        	 this.args = args;
         }

	}
	/**
	 * A simple factory that returns dummy views to the Tabhost
	 */
	class TabFactory implements TabContentFactory {

		private final Context mContext;

	    /**
	     * @param context
	     */
	    public TabFactory(Context context) {
	        mContext = context;
	    }

	    /** (non-Javadoc)
	     * @see android.widget.TabHost.TabContentFactory#createTabContent(java.lang.String)
	     */
	    @Override
		public View createTabContent(String tag) {
	        View v = new View(mContext);
	        v.setMinimumWidth(0);
	        v.setMinimumHeight(0);
	        return v;
	    }

	}

	/** (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
	protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag()); //save the tab selected
        super.onSaveInstanceState(outState);
    }

    /**
     * Initialise ViewPager
     */
    private void intialiseViewPager() {

		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(Fragment.instantiate(this, ChannelListFragment.class.getName()));
		fragments.add(Fragment.instantiate(this, FriendsListFragment.class.getName()));
		//fragments.add(Fragment.instantiate(this, Tab3Fragment.class.getName()));
		this.mPagerAdapter  = new TabPagerAdapter(super.getSupportFragmentManager(), fragments);
		//
		this.mViewPager = (ViewPager)super.findViewById(R.id.viewpager);
		this.mViewPager.setAdapter(this.mPagerAdapter);
		this.mViewPager.setOnPageChangeListener(this);
    }

	/**
	 * Initialize the Tab Host
	 */
	private void initializeTabHost(Bundle args) {
		res = getResources();
		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        TabInfo tabInfo = null;
        IMrekMain.AddTab(this, this.mTabHost, this.mTabHost.newTabSpec("channel_list").setIndicator("Channels", res.getDrawable(R.drawable.icon_channel_list_tab)), ( tabInfo = new TabInfo("channel_list", ChannelListFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        IMrekMain.AddTab(this, this.mTabHost, this.mTabHost.newTabSpec("friends_list").setIndicator("Friends", res.getDrawable(R.drawable.icon_friends_list_tab)), ( tabInfo = new TabInfo("friends_list", FriendsListFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        //IMrekMain.AddTab(this, this.mTabHost, this.mTabHost.newTabSpec("Tab3").setIndicator("Tab 3"), ( tabInfo = new TabInfo("Tab3", Tab3Fragment.class, args)));
        //this.mapTabInfo.put(tabInfo.tag, tabInfo);
        // Default to first tab
        //this.onTabChanged("Tab1");
        //
        mTabHost.setOnTabChangedListener(this);
	}

	/**
	 * Add Tab content to the Tabhost
	 * @param activity
	 * @param tabHost
	 * @param tabSpec
	 * @param clss
	 * @param args
	 */
	private static void AddTab(IMrekMain activity, TabHost tabHost, TabHost.TabSpec tabSpec, TabInfo tabInfo) {
		// Attach a Tab view factory to the spec
		tabSpec.setContent(activity.new TabFactory(activity));
        tabHost.addTab(tabSpec);
	}

	/** (non-Javadoc)
	 * @see android.widget.TabHost.OnTabChangeListener#onTabChanged(java.lang.String)
	 */
	@Override
	public void onTabChanged(String tag) {
		//TabInfo newTab = this.mapTabInfo.get(tag);
		int pos = this.mTabHost.getCurrentTab();
		this.mViewPager.setCurrentItem(pos);
    }

	/* (non-Javadoc)
	 * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrolled(int, float, int)
	 */
	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageSelected(int)
	 */
	@Override
	public void onPageSelected(int position) {
		// TODO Auto-generated method stub
		this.mTabHost.setCurrentTab(position);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrollStateChanged(int)
	 */
	@Override
	public void onPageScrollStateChanged(int state) {
		// TODO Auto-generated method stub

	}
}