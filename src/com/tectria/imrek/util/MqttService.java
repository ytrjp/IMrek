package com.tectria.imrek.util;


import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class MqttService extends Service {
	
	//Messenger Service
    ArrayList<Messenger> clients = new ArrayList<Messenger>();
    int cmd = 0;
    String response;

    //msg.what commands
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_COMMAND = 3;
    public static final int MSG_RESPONSE = 4;
    
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
    
    public static final int MQTT_KEEPALIVE_FAILED = 16; //A keepalive failed to send. We may want to reconnect.
    public static final int MQTT_CONNECT_FAILED = 17; //A connect failed. Reconnect?
    public static final int MQTT_PUBLISH_FAILED = 18; //A publish failed
    public static final int MQTT_SUBSCRIBE_FAILED = 19; //A subscribe failed.
    //This message is sent when a method of MQTTConnection that requires the network
    // is called while the connection is disconnected. This might be a good time to issue a reconnect
    public static final int MQTT_NO_CONNECTION = 20;
    
    public static final int MSG_PING = 21;
    public static final int MSG_KEEPALIVE = 22;
    //Our Managers
    SharedPreferences prefMan;
	ConnectivityManager connMan;
	//NotificationManager notifMan;
	
	//Service
	public long startTime;
	public boolean isStarted;
	
	//MQTT
	private MQTTConnection mqtt;
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
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger msgr = new Messenger(new IncomingHandler());
    
    /**
     * Return interface for the msgr when a client binds to the service
     */
    @Override
    public IBinder onBind(Intent intent) {
        return msgr.getBinder();
    }
    
    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    clients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    clients.remove(msg.replyTo);
                    break;
                case MSG_COMMAND:
                	//What kind of command?
                	//Determine, and set our response accordingly
                    cmd = msg.arg1;
                    response = null;
                    Bundle bundle = msg.getData();
                    switch(cmd){
	                	case MSG_CONNECT:
	                		connect(bundle.getString("data1"), bundle.getString("data2"));
	            			break;
	                	case MSG_DISCONNECT:
	                		disconnect();
	            			break;
	                	case MSG_RECONNECT:
	                		reconnect(bundle.getString("data1"), bundle.getString("data2"));
	            			break;
	                	case MSG_STOP:
	                		stop();
	                		break;
	            	}
                    //If we need to respond
                    if(response != null) {
                    	//Send the message to every client (We should only have one, but we need to handle dead connections)
                        for(int i=clients.size()-1; i>=0; i--) {
                        	//Try to send the message
                            try {
                            	Message message = Message.obtain(null, MSG_RESPONSE, cmd, 0, null);
                            	Bundle rbundle = new Bundle();
                	            rbundle.putString("data1", response);
                	            msg.setData(rbundle);
                                clients.get(i).send(message);
                            } catch (RemoteException e) {
                                clients.remove(i); //Client is dead, remove it
                            }
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
        
        private void sendMessage(int command, String data1) {
        	//Send the message to every client (We should only have one, but we need to handle dead connections)
            for(int i=clients.size()-1; i>=0; i--) {
            	//Try to send the message
                try {
                	Message message = Message.obtain(null, MSG_RESPONSE, cmd, 0, null);
                	Bundle bundle = new Bundle();
    	            bundle.putString("data1", data1);
    	            message.setData(bundle);
                    clients.get(i).send(message);
                } catch (RemoteException e) {
                    clients.remove(i); //Client is dead, remove it
                }
            }
		}
		
        private void sendMessage(int command, String data1, String data2) {
        	//Send the message to every client (We should only have one, but we need to handle dead connections)
            for(int i=clients.size()-1; i>=0; i--) {
            	//Try to send the message
                try {
                	Message message = Message.obtain(null, MSG_RESPONSE, cmd, 0, null);
                	Bundle bundle = new Bundle();
    	            bundle.putString("data1", data1);
    	            bundle.putString("data2", data2);
    	            message.setData(bundle);
                    clients.get(i).send(message);
                } catch (RemoteException e) {
                    clients.remove(i); //Client is dead, remove it
                }
            }
		}
		
        private void sendMessage(int command, String data1, String data2, String data3) {
        	//Send the message to every client (We should only have one, but we need to handle dead connections)
            for(int i=clients.size()-1; i>=0; i--) {
            	//Try to send the message
                try {
                	Message message = Message.obtain(null, MSG_RESPONSE, cmd, 0, null);
                	Bundle bundle = new Bundle();
    	            bundle.putString("data1", data1);
    	            bundle.putString("data2", data2);
    	            bundle.putString("data3", data3);
    	            message.setData(bundle);
                    clients.get(i).send(message);
                } catch (RemoteException e) {
                    clients.remove(i); //Client is dead, remove it
                }
            }
		}
    }
    
    private void sendMessage(int command, String data1) {
    	//Send the message to every client (We should only have one, but we need to handle dead connections)
        for(int i=clients.size()-1; i>=0; i--) {
        	//Try to send the message
            try {
            	Message message = Message.obtain(null, MSG_RESPONSE, cmd, 0, null);
            	Bundle bundle = new Bundle();
	            bundle.putString("data1", data1);
                clients.get(i).send(message);
            } catch (RemoteException e) {
                clients.remove(i); //Client is dead, remove it
            }
        }
	}
	
    private void sendMessage(int command, String data1, String data2) {
    	//Send the message to every client (We should only have one, but we need to handle dead connections)
        for(int i=clients.size()-1; i>=0; i--) {
        	//Try to send the message
            try {
            	Message message = Message.obtain(null, MSG_RESPONSE, cmd, 0, null);
            	Bundle bundle = new Bundle();
	            bundle.putString("data1", data1);
	            bundle.putString("data2", data2);
                clients.get(i).send(message);
            } catch (RemoteException e) {
                clients.remove(i); //Client is dead, remove it
            }
        }
	}
	
    private void sendMessage(int command, String data1, String data2, String data3) {
    	//Send the message to every client (We should only have one, but we need to handle dead connections)
        for(int i=clients.size()-1; i>=0; i--) {
        	//Try to send the message
            try {
            	Message message = Message.obtain(null, MSG_RESPONSE, cmd, 0, null);
            	Bundle bundle = new Bundle();
	            bundle.putString("data1", data1);
	            bundle.putString("data2", data2);
	            bundle.putString("data3", data3);
                clients.get(i).send(message);
            } catch (RemoteException e) {
                clients.remove(i); //Client is dead, remove it
            }
        }
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
    	boolean sent = false;
    	//Send the message to every client, and try to get a valid connection
        for(int i=clients.size()-1; i>=0; i--) {
        	//Try to send the message
            try {
                clients.get(i).send(Message.obtain(null, MSG_RECONNECT_CREDENTIALS, cmd, 0, null));
                sent = true;
            } catch (RemoteException e) {
                clients.remove(i); //Client is dead, remove it
            }
        }
        //If our message isn't sent, then all clients are dead and the service is on it's own.
        if(!sent) {
        	//Try and validate the last login token. If this fails in any way, fall back on a fresh login with old credentials if possible
            IMrekHttpClient.reconnect(prefMan.getString("last_user", ""), prefMan.getString("last_token", ""), prefMan.getString("deviceid", ""), new AsyncHttpResponseHandler() {
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
                    		connect(prefMan.getString("last_user", ""), prefMan.getString("last_token", ""));
                    		return;
                    	}
                    } catch(JSONException e) {
                        tryFreshLogin();
                    }
                }
            });
        }
    }
    
    private void tryFreshLogin() {
    	//If autologin is set, we can try and get a valid user/pass from the preferences
    	if(prefMan.getBoolean("autologin", false)) {
    		if(validCred(prefMan.getString("user", ""), prefMan.getString("pass", ""))) {
    			IMrekHttpClient.login(prefMan.getString("user", ""), prefMan.getString("pass", ""), prefMan.getString("deviceid", ""), new AsyncHttpResponseHandler() {
        			
        			@Override
                    public void onSuccess(String strdata) {
                        try {
                        	JSONObject data = new JSONObject(strdata);
                        	//We have a valid user/pass combo. Cool.
                        	if(data.getInt("status") == 0) {
                        		//Get the token
                        		prefMan.edit().putString("token", data.getJSONObject("data").getString("token")).commit();
                        		setLoggedIn();
                        		connect(prefMan.getString("user", ""), prefMan.getString("token", ""));
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
			mqtt.sendKeepAlive();
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
     * Set shared preference 'loggedin' to true
     */
    public void setLoggedIn() {
    	prefMan.edit().putBoolean("loggedin", true).commit();
    }
    
    /**
     * Set shared preference 'loggedin' to false
     */
    public void setLoggedOut() {
    	prefMan.edit().putBoolean("loggedin", false).commit();
    }
    
    /**
     * Checks preferences to see if the service was previously started
     */
	private boolean wasStarted() {
 		return prefMan.getBoolean("started", false);
	}

	/**
     * Sets whether or not the service has been started in the preferences
     */
	private void setStarted(boolean started) {
 		prefMan.edit().putBoolean("started", started).commit();		
 		isStarted = started;
	}
	
	/**
	 * This method does any necessary cleanup if the service has crashed
	 */
	private void handleCrashedService() {
		//If it was started before, it must have crashed (oops)
		if(wasStarted()) {
			//We're started now, to set started to true
			setStarted(true);
			stopKeepAlives(); 
			//Try and send a message to get new login credentials
			getCredentialsForReconnect();
		} else {
			//We're started now, to set started to true
			setStarted(true);
		}
	}
	private void handleDisconnect() {
		//Handle MQTT disconnecting
	}
	
	// Check if we are online
	private boolean isNetworkAvailable() {
		NetworkInfo info = connMan.getActiveNetworkInfo();
		if(info == null) {
			return false;
		}
		return info.isConnected();
	}

	// Schedule application level keep-alives using the AlarmManager
	private void startKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, MqttService.class);
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
		i.setClass(this, MqttService.class);
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
			prefMan.edit().putString("last_user", user).putString("last_token", token).commit();
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
			/*
			 * TODO: Add safe reconnect stuff
			 */
			connect(user, token);
		}
	}

    @Override
    public void onCreate() {
    	//Get out managers
    	prefMan = PreferenceManager.getDefaultSharedPreferences(this);
    	connMan = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
    	
    	//Instantiate MQTTConnection
    	mqtt = new MQTTConnection(MQTT_HOST);
    	
    	handleCrashedService();
    }

    @Override
	public void onDestroy() {
		//Stop service if started
		if (prefMan.getBoolean("started", false) == true) {
			stop();
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
		}
		
		public void connect(String user, String pass) {
        	this.clientid = user;
        	this.user = user;
        	this.pass = pass;
        	
        	try {
        		this.client = MqttClient.createMqttClient(this.connSpec, MQTT_PERSISTENCE);
				client.connect(clientid, MQTT_CLEAN_START, MQTT_KEEP_ALIVE, user, pass);
			} catch (Exception e) {
				sendMessage(MQTT_CONNECT_FAILED, clientid, user, pass);
				disconnect();
				return;
			}
        	
        	client.registerSimpleHandler(this);
			
			//Subscribe to a topic identical to our deviceid
			//This will be where we recieve "commands"
			subscribeToTopic(clientid);
	
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
				client.disconnect();
			} catch (MqttPersistenceException e) {
				//Oops
			}
			sendMessage(MQTT_DISCONNECTED, this.clientid, this.user, this.pass);
		}
		
		/*
		 * Send a request to the message broker to be sent messages published with 
		 *  the specified topic name. Wildcards are allowed.	
		 */
		public void subscribeToTopic(String topicName) {
			if ((client == null) || (client.isConnected() == false)) {
				//We don't have a connection.
				sendMessage(MQTT_NO_CONNECTION, topicName);
			} else {									
				String[] topics = { topicName };
				try {
					client.subscribe(topics, MQTT_QUALITIES_OF_SERVICE);
				} catch (MqttException e) {
					sendMessage(MQTT_SUBSCRIBE_FAILED, topicName);
				}
				sendMessage(MQTT_SUBSCRIBE_SENT, topicName);
			}
		}	
		
		/*
		 * Sends a message to the message broker, requesting that it be published
		 *  to the specified topic.
		 */
		public void publishToTopic(String topicName, String message) {		
			if ((client == null) || (client.isConnected() == false)) {
				//We don't have a connection.
				sendMessage(MQTT_NO_CONNECTION, topicName, message);
			} else {
				try {
					client.publish(topicName, message.getBytes(), MQTT_QUALITY_OF_SERVICE, MQTT_RETAINED_PUBLISH);
					sendMessage(MQTT_PUBLISH_SENT, topicName, message);
				} catch (Exception e) {
					sendMessage(MQTT_PUBLISH_FAILED, topicName, message);
				}
			}
		}		
		
		/*
		 * Called if the application loses it's connection to the message broker.
		 */
		@Override
		public void connectionLost() throws Exception {
			sendMessage(MQTT_CONNECTION_LOST, "Connection Lost");
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
			sendMessage(MQTT_PUBLISH_ARRIVED, topicName, new String(payload));
		}   
		
		public void sendKeepAlive() {
			try {
				client.ping();
				publishToTopic(mqtt.clientid + "/keepalive", mqtt.clientid);
			} catch (MqttException e) {
				sendMessage(MQTT_KEEPALIVE_FAILED, clientid, user, pass);
			}
		}		
	}
}