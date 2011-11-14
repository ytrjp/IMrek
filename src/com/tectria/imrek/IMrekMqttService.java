package com.tectria.imrek;


import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;

import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.tectria.imrek.fragments.ChannelFragment;
import com.tectria.imrek.util.CallbackBroadcastReceiver;
import com.tectria.imrek.util.IMrekHttpClient;
import com.tectria.imrek.util.IMrekPreferenceManager;

public class IMrekMqttService extends Service {

    //msg.what commands
    //public static final int MSG_REGISTER_CLIENT = 1;
    //public static final int MSG_UNREGISTER_CLIENT = 2;
    //public static final int MSG_COMMAND = 3;
    //public static final int MSG_RESPONSE = 4;
    
    //arg1
    public static final int MSG_STOP = 5; //Stop the service
    public static final int MSG_CONNECT = 6; //Connect to MQTT
    public static final int MSG_DISCONNECT = 7; //Disconnect from MQTT
    public static final int MSG_RECONNECT = 8; //Reconnect to MQTT
    public static final int MSG_RECONNECT_CREDENTIALS = 9; //Request for reconnect credentials
    
    public static final int MQTT_CONNECTED = 10; //Explicitly Connected
    public static final int MQTT_DISCONNECTED = 11; //Explicitly disconnected
    public static final int MQTT_CONNECTION_LOST = 12; //Connection lost unexpectedly
    public static final int MQTT_PUBLISH_ARRIVED = 13; //We recieved a message
    public static final int MQTT_PUBLISH_SENT = 14; //We sent a message
    public static final int MQTT_SUBSCRIBE_SENT = 15; //We sent a subscribe to a topic
    public static final int MQTT_UNSUBSCRIBE_SENT = 26; //We sent a unsubscribe to a topic
    
    public static final int MQTT_KEEPALIVE_FAILED = 16; //A keepalive failed to send. We may want to reconnect.
    public static final int MQTT_CONNECT_FAILED = 17; //A connect failed. Reconnect?
    public static final int MQTT_PUBLISH_FAILED = 18; //A publish failed
    public static final int MQTT_SUBSCRIBE_FAILED = 19; //A subscribe failed.
    public static final int MQTT_UNSUBSCRIBE_FAILED = 27; //A unsubscribe failed.
    //This message is sent when a method of MQTTConnection that requires the network
    // is called while the connection is disconnected. This might be a good time to issue a reconnect
    public static final int MQTT_NO_CONNECTION = 20;
    
    
    //These tell MQTT to do stuff
    public static final int MQTT_SUBSCRIBE = 21;
    public static final int MQTT_UNSUBSCRIBE = 22;
    public static final int MQTT_PUBLISH = 23;
    public static final int MQTT_SEND_KEEPALIVE = 24;
    
    //Ping the service
    public static final int MSG_PING = 25;
    
    public static final int SERVICE_READY = 28;
	
    public static final int SERVICE_CONNECT_CHECK = 29;
    public static final int SERVICE_CONNECT_TRUE = 30;
    public static final int SERVICE_CONNECT_FALSE = 31;
    
    //Our Managers
    IMrekPreferenceManager prefs;
	ConnectivityManager conn;
	//NotificationManager notifMan;
	
	//Service
	public long startTime;
	public boolean isStarted;
	
	//MQTT
	private MQTTConnection mqtt = null;
	private static final String MQTT_HOST = "69.164.216.146";
	private static int MQTT_BROKER_PORT_NUM = 1883;
	private static MqttPersistence MQTT_PERSISTENCE = null;
	private static boolean MQTT_CLEAN_START = true;
	private static short MQTT_KEEP_ALIVE = 60 * 15;
	private static int[] MQTT_QUALITIES_OF_SERVICE = { 0 } ;
	private static int MQTT_QUALITY_OF_SERVICE   = 0;
	private static boolean MQTT_RETAINED_PUBLISH = false;
	
	//Interval to send keepalives
	private static final long KEEP_ALIVE_INTERVAL = 1000 * 60 * 28;
	
	// Message Receiver
	static final String MESSAGE_RECEIVER_ACTION = "com.tectria.imrek.MESSAGE";
    
    private MainBroadcastReceiver svcReceiver;
	private boolean svcReceiverRegistered;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
    
    private boolean validCred(String user, String pass) {
    	if(user != null) {
    		if(!(user.length() > 4 && user.length() < 13)) {
    			return false;
    		}
    	} else {
    		return false;
    	}
    	if(pass != null) {
    		if(!(pass.length() > 6 && pass.length() < 13)) {
    			return false;
    		}
    	} else {
			return false;
		}
    	return true;
    }
    
    private boolean validTokenCred(String user, String token) {
    	if(user != null) {
    		if(!(user.length() > 4 && user.length() < 13)) {
    			return false;
    		}
    	} else {
    		return false;
    	}
    	if(token != null) {
    		if(token.length() != 12) {
    			return false;
    		}
    	} else {
			return false;
		}
    	return true;
    }
    
    private void getCredentialsForReconnect() {
    	if(validTokenCred(prefs.getLastUser(), prefs.getLastToken())) {
    		//Try and validate the last login token. If this fails in any way, fall back on a fresh login with old credentials if possible
            IMrekHttpClient.reconnect(prefs.getLastUser(), prefs.getLastToken(), prefs.getDeviceId(), new AsyncHttpResponseHandler() {
            	@Override
                public void onFailure(Throwable error) {
            		tryFreshLogin();
                }
    			
    			@Override
                public void onSuccess(String strdata) {
                    try {
                    	JSONObject data = new JSONObject(strdata);
                    	if(data.getInt("status") == 1) {
                    		tryFreshLogin();
        					return;
                    	} else {
                    		//We have a valid user/token combo. Cool.
                    		prefs.setLoggedIn(true);
                    		connect(prefs.getLastUser(), prefs.getLastToken());
                    		return;
                    	}
                    } catch(JSONException e) {
                        tryFreshLogin();
                    }
                }
            });
    	} else {
    		tryFreshLogin();
    	}
    }
    
    private void tryFreshLogin() {
    	//If autologin is set, we can try and get a valid user/pass from the preferences
    	if(prefs.getAutoLogin()) {
    		if(validCred(prefs.getUsername(), prefs.getPassword())) {
    			IMrekHttpClient.login(prefs.getUsername(), prefs.getPassword(), prefs.getDeviceId(), new AsyncHttpResponseHandler() {
        			
        			@Override
                    public void onSuccess(String strdata) {
                        try {
                        	JSONObject data = new JSONObject(strdata);
                        	//We have a valid user/pass combo. Cool.
                        	if(data.getInt("status") == 0) {
                        		//Get the token
                        		prefs.setToken(data.getJSONObject("data").getString("token"));
                        		prefs.setLoggedIn(true);
                        		connect(prefs.getUsername(), prefs.getToken());
                        		return;
                        	}
                        } catch(JSONException e) {
                            tryFreshLogin();
                        }
                    }
                });
    		}
    	}
    }
    
    @Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (intent.getAction().equals("_KEEPALIVE") == true) {
			mqtt.keepalive();
		}
	}
    
    /**
     * When the service is started, registers it as a STICKY service
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
	
	/**
	 * This method does any necessary cleanup if the service has crashed
	 */
	private void handleCrashedService() {
		//If it was started before, it must have crashed (oops)
		if(prefs.getWasStarted()) {
			//We're started now, to set started to true
			prefs.setWasStarted(true);
			isStarted = true;
			stopKeepAlives(); 
			//Try and send a message to get new login credentials
			getCredentialsForReconnect();
		} else {
			//We're started now, to set started to true
			prefs.setWasStarted(true);
			isStarted = true;
		}
	}
	
	private void handleDisconnect() {
		reconnect(prefs.getLastUser(), prefs.getLastToken());
	}
	
	// Check if we are online
	private boolean isNetworkAvailable() {
		NetworkInfo info = conn.getActiveNetworkInfo();
		if(info == null) {
			return false;
		}
		return info.isConnected();
	}

	// Schedule application level keep-alives using the AlarmManager
	private void startKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, IMrekMqttService.class);
		i.setAction("_KEEPALIVE");
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
		  System.currentTimeMillis() + KEEP_ALIVE_INTERVAL,
		  KEEP_ALIVE_INTERVAL, pi);
	}

	// Remove all scheduled keep alives
	private void stopKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, IMrekMqttService.class);
		i.setAction("_KEEPALIVE");
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}
	
	private void stop() {
		if(mqtt.client != null) {
			mqtt.disconnect();
		}
		stopSelf();
	}
	
	private void connect(String user, String token) {
		if(isStarted && mqtt.client == null) {
			//Update last_ preferences
			prefs.setLastUser(user);
			prefs.setLastToken(token);
			mqtt.connect(user, token);
		}
	}
	
	private void disconnect() {
		if(isStarted == true && mqtt.client != null) {
			mqtt.disconnect();
		}
	}
	
	private void reconnect(String user, String token) {
		if(isStarted == true && mqtt.client == null) {
			disconnect();
			connect(user, token);
		}
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
	
    @Override
    public void onCreate() {
    	
    	if (!svcReceiverRegistered) {
    		svcReceiver = new MainBroadcastReceiver() {

    			@Override
    			public void gotBroadcast(Context context, Intent intent) {
    				Bundle bundle = intent.getExtras();
    				switch(bundle.getInt("msgtype")) {
	    				case MSG_CONNECT:
	    	        		connect(bundle.getString("arg1"), bundle.getString("arg2"));
	    	    			break;
	    	        	case MSG_DISCONNECT:
	    	        		disconnect();
	    	    			break;
	    	        	case MSG_RECONNECT:
	    	        		reconnect(bundle.getString("arg1"), bundle.getString("arg2"));
	    	    			break;
	    	        	case MSG_STOP:
	    	        		stop();
	    	        		break;
	    	        	case MQTT_SUBSCRIBE:
	    	        		mqtt.subscribe(bundle.getString("arg1"));
	    	        		break;
	    	        	case MQTT_UNSUBSCRIBE:
	    	        		mqtt.unsubscribe(bundle.getString("arg1"));
	    	        		break;
	    	        	case MQTT_PUBLISH:
	    	        		mqtt.publish(bundle.getString("arg1"), bundle.getString("arg2"));
	    	        		break;
	    	        	case MQTT_SEND_KEEPALIVE:
	    	        		mqtt.keepalive();
	    	        		break;
//	    	        	case SERVICE_CONNECT_CHECK:
//	    	        		serviceConnectCheck();
//	    	        		break;
	    			}
    			}
    	    	
    	    };
    		registerReceiver(svcReceiver, new IntentFilter(MESSAGE_RECEIVER_ACTION));
    		svcReceiverRegistered = true;
    		sendMessage(IMrekMqttService.SERVICE_READY, null, null, null);
		}
    	
    	//Get our managers
    	prefs = IMrekPreferenceManager.getInstance(this);
    	conn = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
    	
    	//Instantiate MQTTConnection
    	if (serviceConnectCheck() == false) {
    		mqtt = new MQTTConnection(MQTT_HOST);
    	}

    	handleCrashedService();
    }

    @Override
	public void onDestroy() {
		//Stop service if started
		if (prefs.getWasStarted() == true) {
			stop();
		}	
	}
    
    private boolean serviceConnectCheck() {
    	if (mqtt != null) {
			if (mqtt.client != null) {
				if (mqtt.client.isConnected()) {
					sendMessage(SERVICE_CONNECT_TRUE, null, null, null);
					return true;
				} else {
					sendMessage(SERVICE_CONNECT_FALSE, null, null, null);
					return false;
				}
			} else {
				sendMessage(SERVICE_CONNECT_FALSE, null, null, null);
				return false;
			}
		} else {
			sendMessage(SERVICE_CONNECT_FALSE, null, null, null);
			return false;
		}
    }
    
	// This inner class is a wrapper on top of MQTT client.
	private class MQTTConnection implements MqttSimpleCallback {
		IMqttClient client = null;
		String connSpec;
		ArrayList<String> topics;
		String clientid;
		String user;
		String pass;
		
		// Creates a new connection given the broker address and initial topic
		public MQTTConnection(String brokerHostName) {
			// Create connection spec
	    	this.connSpec = "tcp://" + brokerHostName + "@" + MQTT_BROKER_PORT_NUM;
	    	this.topics = new ArrayList<String>();
		}
		
		/*
		 * Called if the application loses it's connection to the message broker.
		 */
		@Override
		public void connectionLost() throws Exception {
			sendMessage(MQTT_CONNECTION_LOST, "Connection Lost", null, null);
			stopKeepAlives();
			// null itself
			client = null;
			if(isNetworkAvailable() == true) {
				handleDisconnect();
			}
		}		
		
		/*
		 * Called when we receive a message from the message broker. 
		 */
		@Override
		public void publishArrived(String topicName, byte[] payload, int qos, boolean retained) {
			sendMessage(MQTT_PUBLISH_ARRIVED, topicName, new String(payload), null);
		}  
		
		public void connect(String user, String pass) {
        	this.clientid = user;
        	this.user = user;
        	this.pass = pass;
        	
        	try {
        		this.client = MqttClient.createMqttClient(this.connSpec, MQTT_PERSISTENCE);
				this.client.connect(clientid, MQTT_CLEAN_START, MQTT_KEEP_ALIVE, user, pass);
			} catch (Exception e) {
				sendMessage(MQTT_CONNECT_FAILED, clientid, user, pass);
				this.disconnect();
				return;
			}
        	
        	client.registerSimpleHandler(this);
			
			//Subscribe to a topic identical to our deviceid
			//This will be where we recieve "commands"
			//this.subscribe(clientid);
	
			//Save start time
			startTime = System.currentTimeMillis();
			
			//Start the keep-alives
			startKeepAlives();
			sendMessage(MQTT_CONNECTED, this.clientid, this.user, this.pass);
		}
		
		// Disconnect
		public void disconnect() {		
			stopKeepAlives();
			try {
				this.client.disconnect();
			} catch (MqttPersistenceException e) {
				//Oops
			}
			sendMessage(MQTT_DISCONNECTED, this.clientid, this.user, this.pass);
		}
		
		/*
		 * Send a request to the message broker to be sent messages published with 
		 *  the specified topic name. Wildcards are allowed.	
		 */
		public void subscribe(String topicName) {
			if ((this.client == null) || (this.client.isConnected() == false)) {
				//We don't have a connection.
				sendMessage(MQTT_NO_CONNECTION, topicName, null, null);
			} else {									
				String[] topics = { topicName };
				this.topics.add(topicName);
				try {
					this.client.subscribe(topics, MQTT_QUALITIES_OF_SERVICE);
				} catch (MqttException e) {
					sendMessage(MQTT_SUBSCRIBE_FAILED, topicName, null, null);
				}
				sendMessage(MQTT_SUBSCRIBE_SENT, topicName, null, null);
			}
		}
		
		/*
		 * Send a request to the message broker to be sent messages published with 
		 *  the specified topic name. Wildcards are allowed.	
		 */
		public void unsubscribe(String topicName) {
			if ((this.client == null) || (this.client.isConnected() == false)) {
				//We don't have a connection.
				sendMessage(MQTT_NO_CONNECTION, topicName, null, null);
			} else {									
				String[] topics = { topicName };
				if(this.topics.contains(topicName)) {
					this.topics.remove(topicName);
				}
				try {
					client.unsubscribe(topics);
				} catch (MqttException e) {
					sendMessage(MQTT_UNSUBSCRIBE_FAILED, topicName, null, null);
				}
				sendMessage(MQTT_UNSUBSCRIBE_SENT, topicName, null, null);
			}
		}	
		
		/*
		 * Sends a message to the message broker, requesting that it be published
		 *  to the specified topic.
		 */
		public void publish(String topicName, String message) {		
			if ((this.client == null) || (this.client.isConnected() == false)) {
				//We don't have a connection.
				sendMessage(MQTT_NO_CONNECTION, topicName, message, null);
			} else {
				try {
					this.client.publish(topicName, message.getBytes(), MQTT_QUALITY_OF_SERVICE, MQTT_RETAINED_PUBLISH);
					sendMessage(MQTT_PUBLISH_SENT, topicName, message, null);
				} catch (Exception e) {
					sendMessage(MQTT_PUBLISH_FAILED, topicName, message, null);
				}
			}
		}		
		
		public void keepalive() {
			try {
				client.ping();
				this.publish(mqtt.clientid + "/keepalive", mqtt.clientid);
			} catch (MqttException e) {
				sendMessage(MQTT_KEEPALIVE_FAILED, clientid, user, pass);
			}
		}		
	}
}