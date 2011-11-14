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
	private IMrekPreferenceManager prefs;
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
	
	ImageButton newchannel;
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
        
        //Get our preference manager
        prefs = IMrekPreferenceManager.getInstance(this);
        inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        context = this;
        svcReceiverRegistered = false;
        
        if(prefs.getCrashedLastClose()) {
        	sendMessage(IMrekMqttService.MSG_STOP, "Logging Out", null, null);
    		prefs.setLoggedIn(false);
    		prefs.clearSavedUser();
    		Intent intent = new Intent(getBaseContext(), SplashScreenLogin.class);
    		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
			finish();
        }
        
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
            		sendMessage(IMrekMqttService.MSG_STOP, "Logging Out", null, null);
            		prefs.setLoggedIn(false);
            		prefs.clearSavedUser();
            		Intent intent = new Intent(getBaseContext(), SplashScreenLogin.class);
            		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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
            		sendMessage(IMrekMqttService.MSG_STOP, "Logging Out", null, null);
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
        
        //Get channels
        items = IMrekConversationManager.getInstance(getBaseContext()).getChannelsLastMessages();
		
		//Create the adapter
        adapter = new SimpleAdapter(getBaseContext(), items, R.layout.item_channel_list, from, to);
        setListAdapter(adapter);
        registerForContextMenu(getListView());
        
        adapter.notifyDataSetChanged();
		
        newchannel = (ImageButton)findViewById(R.id.newchannel);
        
        newchannel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(prefs.getIsConnected()) {
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
			    				IMrekConversationManager.getInstance(getBaseContext()).addChannel(channelname.getText().toString());
			    				HashMap<String, String> map = new HashMap<String, String>();
			    				map.put("channel", channelname.getText().toString());
			    				map.put("message", "");
			    				items.add(map);
			    				adapter.notifyDataSetChanged();
			    				sendMessage(IMrekMqttService.MQTT_SUBSCRIBE, channelname.getText().toString(), null, null);
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
			    	
			    	channelname.setOnKeyListener(new OnKeyListener() {
						@Override
						public boolean onKey(View v, int keyCode, KeyEvent event) {
							if((event.getAction() == KeyEvent.ACTION_DOWN) && ((keyCode == KeyEvent.KEYCODE_ENTER) || (keyCode == KeyEvent.KEYCODE_TAB)
									|| (keyCode == KeyEvent.KEYCODE_SPACE))) {
					        	return true;
					        }
					        return false;
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
    		        		// TODO: load friends list
    		        		break;
    		        	case IMrekMqttService.MQTT_CONNECTION_LOST:
    		        		setDisconnected();
    		        		sendMessage(IMrekMqttService.MSG_CONNECT, prefs.getUsername(), prefs.getPassword(), null);
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
    		        		sendMessage(IMrekMqttService.MSG_RECONNECT, u, t, null);
    		        		break;
    		        	case IMrekMqttService.MQTT_PUBLISH_ARRIVED:
    		        		IMrekConversationManager.getInstance(getBaseContext()).newMessageReceived(bundle.getString("arg1"), bundle.getString("arg2"));
    		        		break;
    		        	case IMrekMqttService.MQTT_PUBLISH_SENT:
    		        		// TODO: Check if we get an MQTT_PUBLISH_ARRIVED for our own messages
    		        		break;
    		        	case IMrekMqttService.MQTT_SUBSCRIBE_SENT:
    		        		IMrekConversationManager.getInstance(getBaseContext()).addChannel(bundle.getString("arg1"));
    		        		break;
    		        	case IMrekMqttService.MQTT_UNSUBSCRIBE_SENT:
    		        		IMrekConversationManager.getInstance(getBaseContext()).removeChannel(bundle.getString("arg1"));
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
    		        		sendMessage(IMrekMqttService.MQTT_SEND_KEEPALIVE, "keepalive", null, null);
    		        		break;
    		        	case IMrekMqttService.MQTT_KEEPALIVE_FAILED:
    		        		sendMessage(IMrekMqttService.MSG_PING, "ping", null, null);
    		        		// set timeout to kill service? Don't kill if received ping
    		        		break;
    		        	case IMrekMqttService.SERVICE_READY:
    		        		sendMessage(IMrekMqttService.MSG_CONNECT, prefs.getUsername(), prefs.getToken(), null);
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
	    	adapter.notifyDataSetChanged();
	    	paused = false;
    	}
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	paused = true;
    	if (svcReceiverRegistered) {
    		unregisterReceiver(svcReceiver);
    		svcReceiverRegistered = false;
    	}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	if(!isFinishing()) {
    		//We're crashing D:
    		prefs.setCrashedLastClose(true);
    		//Try and tell the service
    		sendMessage(IMrekMqttService.MSG_STOP, "Crashing", null, null);
    	}
    }
    
	@Override
	public void onListItemClick (ListView l, View v, int position, long id) {
		Intent intent = new Intent(getBaseContext(), IMrekChannels.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		intent.putExtra("index", position);
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
			startActivity(prefIntent);
		}
		else if(mi.getItemId() == R.id.logout) {
			prefs.setLoggedIn(false);
			prefs.setAutoLogin(false);
			sendMessage(IMrekMqttService.MSG_DISCONNECT, "Quitting", null, null);
			Intent intent = new Intent(getBaseContext(), SplashScreenLogin.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
			finish();
		}
		else if(mi.getItemId() == R.id.reconnect) {
			sendMessage(IMrekMqttService.MSG_RECONNECT, prefs.getUsername(), prefs.getPassword(), null);
		}
		else if(mi.getItemId() == R.id.quit) {
			quitDialog = new AlertDialog.Builder(this);
			quitDialog.setMessage("Are you sure you want to quit? This will close IMrek and disconnect you from the server.");
			quitDialog.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sendMessage(IMrekMqttService.MSG_STOP, "Quitting", null, null);
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