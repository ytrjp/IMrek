package com.tectria.imrek;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.TextView;

import com.tectria.imrek.util.IMrekConversationManager;
import com.tectria.imrek.util.IMrekPreferenceManager;

public class IMrekMain extends ListActivity {
	
	//Tab Manager + Tabs
	Resources res;
	TabHost tabHost;
	TabHost.TabSpec friend_spec;
	TabHost.TabSpec convo_spec;
	Intent tabintent;
	boolean paused;
	
	//PreferenceManager + Preferences
	InputMethodManager imm;
	
	//Views
	private TextView status;
	private ImageView statusicon;
	
	//Dialogs
	private AlertDialog.Builder quitDialog;
	
	//Misc
	private Bundle extras;
	private String user;
	private String pass;
	private MainBroadcastReceiver svcReceiver;
	private boolean svcReceiverRegistered;
	static final String MESSAGE_RECEIVER_ACTION = "com.tectria.imrek.MESSAGE";
	boolean serviceStarted = false;
	boolean switching;
	
	ImageButton newchannel;
	TextView userinfo;
	AlertDialog.Builder dialog;
	View dialogview;
	LayoutInflater inflater;
	Context context;
	
	//Data for the channel list
	final String[] from = new String[] {"channel", "message"};
	final int[] to = new int[] { R.id.channel, R.id.lastm };
	ArrayList<HashMap<String, String>> items;
	SimpleAdapter adapter;
	//Reusable HashMap
    HashMap<String, String> map;
	
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
        
        inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        context = this;
        svcReceiverRegistered = false;
        
        if(IMrekPreferenceManager.getInstance(getBaseContext()).getCrashedLastClose()) {
        	sendMessage(IMrekMqttService.MSG_STOP, "Logging Out", null, null);
    		IMrekPreferenceManager.getInstance(getBaseContext()).setLoggedIn(false);
    		IMrekPreferenceManager.getInstance(getBaseContext()).clearSavedUser();
    		Intent intent = new Intent(getBaseContext(), SplashScreenLogin.class);
    		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    		switching = true;
			startActivity(intent);
			finish();
        }
        
        //If we aren't logged in,
        if(!IMrekPreferenceManager.getInstance(getBaseContext()).getLoggedIn()) {
        	//Credentials aren't yet verified
        	IMrekPreferenceManager.getInstance(getBaseContext()).setVerified(false);
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
            		sendMessage(IMrekMqttService.MSG_STOP, "Logging Out", null, null);
            		IMrekPreferenceManager.getInstance(getBaseContext()).setLoggedIn(false);
            		IMrekPreferenceManager.getInstance(getBaseContext()).clearSavedUser();
            		Intent intent = new Intent(getBaseContext(), SplashScreenLogin.class);
            		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            		switching = true;
        			startActivity(intent);
        			finish();
            	} else {
            		IMrekPreferenceManager.getInstance(getBaseContext()).setVerified(true);
            	}
            //If there's no user/pass in the bundle
            } else {
            	//Default to the user/pass in the preferences
            	user = IMrekPreferenceManager.getInstance(getBaseContext()).getUsername();
            	pass = IMrekPreferenceManager.getInstance(getBaseContext()).getPassword();
            	//If user/pass is blank and/or less than the required length
            	if((user.equals("") || pass.equals("")) || (user.length() < 5 || pass.length() < 6)) {
            		//If we get here, we somehow have an invalid user or password in the preferences.
            		//Disconnect and log out, clear the user/pass in the preferences, and return to the splash/login activity
            		sendMessage(IMrekMqttService.MSG_STOP, "Logging Out", null, null);
            		IMrekPreferenceManager.getInstance(getBaseContext()).setLoggedIn(false);
            		IMrekPreferenceManager.getInstance(getBaseContext()).clearSavedUser();
            		Intent intent = new Intent(getBaseContext(), SplashScreenLogin.class);
            		switching = true;
        			startActivity(intent);
        			finish();
            	} else {
            		IMrekPreferenceManager.getInstance(getBaseContext()).setVerified(true);
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
        IMrekPreferenceManager.getInstance(getBaseContext()).setWasStarted(false);
        
        //Get channels
        items = IMrekConversationManager.getInstance(getBaseContext()).getChannelsLastMessages();
		
		//Create the adapter
        adapter = new SimpleAdapter(getBaseContext(), items, R.layout.item_channel_list, from, to);
        setListAdapter(adapter);
        registerForContextMenu(getListView());
        
        adapter.notifyDataSetChanged();
		
        newchannel = (ImageButton)findViewById(R.id.newchannel);
        userinfo = (TextView)findViewById(R.id.userinfo);
        
        userinfo.setText("Logged in as " + IMrekPreferenceManager.getInstance(getBaseContext()).getUsername());
        
        newchannel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(IMrekPreferenceManager.getInstance(getBaseContext()).getIsConnected()) {
					dialogview = inflater.inflate(R.layout.dialog_newchannel, null);
			    	dialog = new AlertDialog.Builder(context);
			    	dialog.setTitle("Join/Create Channel");
			    	dialog.setView(dialogview);
			    	
			    	final EditText channelname = (EditText)dialogview.findViewById(R.id.channelname);
			    	
			    	dialog.setPositiveButton("Join/Create", new DialogInterface.OnClickListener() {
			    		@Override
						public void onClick(final DialogInterface dialog, int id) {
			    			
			    			boolean exists = false;
			    			for(HashMap<String, String> map : items) {
			    				if(map.get("channel").equals(channelname.getText().toString())) {
			    					exists = true;
			    					break;
			    				}
			    			}
			    			
			    			if(channelname.getText().toString() != "" && !exists) {
			    				String channame = channelname.getText().toString().replace(" ", "").replace("	", "");
			    				IMrekConversationManager.getInstance(getBaseContext()).addChannel(channame);
			    				HashMap<String, String> map = new HashMap<String, String>();
			    				map.put("channel", channame);
			    				map.put("message", "");
			    				items.add(map);
			    				adapter.notifyDataSetChanged();
			    				sendMessage(IMrekMqttService.MQTT_SUBSCRIBE, channame, null, null);
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
				} else {
					disconnectedDialog("Disconnected", "You cannot create a new channel while disconnected.");
				}
			}
        });
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	sendMessage(IMrekMqttService.SERVICE_STOP_FOREGROUND, null, null, null);
    	if (!svcReceiverRegistered) {
    		svcReceiver = new MainBroadcastReceiver(){
    			@Override
    			public void gotBroadcast(Context context, Intent intent) {
    				Bundle bundle = intent.getExtras();
    				switch (bundle.getInt("msgtype")) {
    					case IMrekMqttService.MQTT_CONNECTED:
    		        		setConnected();
    		        		for(HashMap<String, String> item : items) {
    		        			sendMessage(IMrekMqttService.MQTT_SUBSCRIBE, item.get("channel"), null, null);
    		        		}
    		        		break;
    		        	case IMrekMqttService.MQTT_CONNECTION_LOST:
    		        		setDisconnected();
    		        		sendMessage(IMrekMqttService.MSG_CONNECT, IMrekPreferenceManager.getInstance(getBaseContext()).getUsername(), IMrekPreferenceManager.getInstance(getBaseContext()).getToken(), null);
    		        		break;
    		        	case IMrekMqttService.MQTT_DISCONNECTED:
    		        		setDisconnected();
    		        		break;
    		        	case IMrekMqttService.MSG_RECONNECT_CREDENTIALS:
    		        		//Call a reconnect with the most recent, most-probably-valid credentials we can.
    		        		String u = IMrekPreferenceManager.getInstance(getBaseContext()).getUsername();
    		        		String t = IMrekPreferenceManager.getInstance(getBaseContext()).getToken();
    		        		if(u == "") {
    		        			u = IMrekPreferenceManager.getInstance(getBaseContext()).getLastUser();
    		        		}
    		        		if(t == "") {
    		        			t = IMrekPreferenceManager.getInstance(getBaseContext()).getLastToken();
    		        		}
    		        		sendMessage(IMrekMqttService.MSG_RECONNECT, u, t, null);
    		        		break;
    		        	case IMrekMqttService.MQTT_PUBLISH_ARRIVED:
    		        		IMrekConversationManager.getInstance(getBaseContext()).newMessageReceived(bundle.getString("arg1"), bundle.getString("arg2"));
    		        		break;
    		        	case IMrekMqttService.MQTT_PUBLISH_SENT:
    		        		
    		        		break;
    		        	case IMrekMqttService.MQTT_SUBSCRIBE_SENT:
    		        		IMrekConversationManager.getInstance(getBaseContext()).addChannel(bundle.getString("arg1"));
    		        		break;
    		        	case IMrekMqttService.MQTT_UNSUBSCRIBE_SENT:
    		        		IMrekConversationManager.getInstance(getBaseContext()).removeChannel(bundle.getString("arg1"));
    		        		break;
    		        	case IMrekMqttService.MQTT_PUBLISH_FAILED:
    		        		
    		        		break;
    		        	case IMrekMqttService.MQTT_SUBSCRIBE_FAILED:
    		        		
    		        	case IMrekMqttService.MQTT_UNSUBSCRIBE_FAILED:
    		        		
    		        		break;
    		        	case IMrekMqttService.MQTT_CONNECT_FAILED:
    		        		
    		        		break;
    		        	case IMrekMqttService.MSG_PING:
    		        		sendMessage(IMrekMqttService.MQTT_SEND_KEEPALIVE, "keepalive", null, null);
    		        		break;
    		        	case IMrekMqttService.MQTT_KEEPALIVE_FAILED:
    		        		sendMessage(IMrekMqttService.MSG_PING, "ping", null, null);
    		        		// set timeout to kill service? Don't kill if received ping
    		        		break;
    		        	case IMrekMqttService.SERVICE_READY:
    		        		sendMessage(IMrekMqttService.MSG_CONNECT, IMrekPreferenceManager.getInstance(getBaseContext()).getUsername(), IMrekPreferenceManager.getInstance(getBaseContext()).getToken(), null);
    		        		break;
    				}
    			}
    	    };
    		registerReceiver(svcReceiver, new IntentFilter(MESSAGE_RECEIVER_ACTION));
    		svcReceiverRegistered = true;
    		if(!serviceStarted) {
    			serviceStarted = true;
    			Intent i = new Intent(context, IMrekMqttService.class);
    			startService(i);
    		}
    	}
    	
    	if(paused) {
    		items = IMrekConversationManager.getInstance(getBaseContext()).getChannelsLastMessages();
    		adapter = new SimpleAdapter(getBaseContext(), items, R.layout.item_channel_list, from, to);
            setListAdapter(adapter);
    		adapter.notifyDataSetChanged();
    		if(IMrekPreferenceManager.getInstance(getBaseContext()).getIsConnected()) {
    			setConnected();
    		}
	    	paused = false;
    	}
    }
    
    @Override
    public void onBackPressed() {
    	paused = true;
    	Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	paused = true;
    	if (svcReceiverRegistered) {
    		unregisterReceiver(svcReceiver);
    		svcReceiverRegistered = false;
    	}
    	if(!switching) {
    		sendMessage(IMrekMqttService.SERVICE_START_FOREGROUND, null, null, null);
    	} else {
    		switching = false;
    	}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	if(!isFinishing()) {
    		//We're crashing D:
    		IMrekPreferenceManager.getInstance(getBaseContext()).setCrashedLastClose(true);
    		//Try and tell the service
    		sendMessage(IMrekMqttService.MSG_STOP, "Crashing", null, null);
    	}
    }
    
	@Override
	public void onListItemClick (ListView l, View v, int position, long id) {
		Intent intent = new Intent(getBaseContext(), IMrekChannels.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		intent.putExtra("index", position);
		switching = true;
		startActivity(intent);
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
			prefIntent.putExtra("channels", IMrekConversationManager.getInstance(getBaseContext()).getChannelList());
			startActivity(prefIntent);
		}
		else if(mi.getItemId() == R.id.logout) {
			IMrekPreferenceManager.getInstance(getBaseContext()).setLoggedIn(false);
			IMrekPreferenceManager.getInstance(getBaseContext()).setAutoLogin(false);
			sendMessage(IMrekMqttService.MSG_DISCONNECT, "Quitting", null, null);
			Intent intent = new Intent(getBaseContext(), SplashScreenLogin.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
			finish();
		}
		else if(mi.getItemId() == R.id.reconnect) {
			sendMessage(IMrekMqttService.MSG_RECONNECT, IMrekPreferenceManager.getInstance(getBaseContext()).getUsername(), IMrekPreferenceManager.getInstance(getBaseContext()).getPassword(), null);
		}
		else if(mi.getItemId() == R.id.quit) {
			quitDialog = new AlertDialog.Builder(this);
			quitDialog.setTitle("Quit");
			quitDialog.setIcon(R.drawable.ic_confirm);
			quitDialog.setMessage("Are you sure you want to quit? This will close IMrek and disconnect you from the server.");
			quitDialog.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sendMessage(IMrekMqttService.MSG_STOP, "Quitting", null, null);
					setDisconnected();
					IMrekPreferenceManager.getInstance(getBaseContext()).setLoggedIn(false);
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
    
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	  super.onCreateContextMenu(menu, v, menuInfo);
	  AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	  menu.setHeaderTitle(items.get(info.position).get("channel"));
	  MenuInflater inflater = getMenuInflater();
	  inflater.inflate(R.menu.channel_item, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		if(item.getItemId() == R.id.close) {
			IMrekConversationManager.getInstance(getBaseContext()).removeChannel(items.get(info.position).get("channel"));
			sendMessage(IMrekMqttService.MQTT_UNSUBSCRIBE, items.get(info.position).get("channel"), null, null);
			items.remove(info.position);
			adapter.notifyDataSetChanged();
		} else {
			return super.onContextItemSelected(item);
		} 
		return true;
	}
    
	private void sendMessage(int msgtype, String arg1, String arg2, String arg3) {
		Intent i = new Intent(MESSAGE_RECEIVER_ACTION);
		Bundle b = new Bundle();
		b.putInt("msgtype", msgtype);
		b.putString("arg1", arg1);
		b.putString("arg2", arg2);
		b.putString("arg3", arg3);
		i.putExtras(b);
		sendBroadcast(i);
	}
	
	public final void disconnectedDialog(String title, String text) {
    	dialog = new AlertDialog.Builder(this);
        TextView dialogText = new TextView(this);
        dialogText.setText(text);
        dialogText.setPadding(10, 10, 10, 10);
        dialog.setView(dialogText);
        dialog.setTitle(title);
        dialog.create();
        dialog.show();
    }
	
	public void setConnected() {
		IMrekPreferenceManager.getInstance(getBaseContext()).setIsConnected(true);
    	setUIConnected();
	}
	
	public void setDisconnected() {
		IMrekPreferenceManager.getInstance(getBaseContext()).setIsConnected(false);
    	setUIDisconnected();
	}
    
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
}