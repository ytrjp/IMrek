package com.tectria.imrek.util;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;

public class IMrekPreferenceManager {
	
	private static IMrekPreferenceManager instance = null;
	private SharedPreferences appSharedPrefs;
	private Editor prefsEditor;
	private Context context;
	
	protected IMrekPreferenceManager(Context ctx) {
		this.appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		this.prefsEditor = this.appSharedPrefs.edit();
		this.context = ctx;
	}
	
	public static IMrekPreferenceManager getInstance(Context ctx) {
		if (instance == null) {
			instance = new IMrekPreferenceManager(ctx);
		}
		return instance;
	}
	
	public synchronized String getUsername() {
		return appSharedPrefs.getString("user", "");
	}
	
	public synchronized void setUsername(String username) {
		prefsEditor.putString("user", username);
		prefsEditor.commit();
	}
	
	public synchronized boolean getCrashedLastClose() {
		return appSharedPrefs.getBoolean("crashedlast", true);
	}
	
	public synchronized void setCrashedLastClose(boolean crashed) {
		prefsEditor.putBoolean("crashedlast", crashed);
		prefsEditor.commit();
	}
	
	public synchronized String getPassword() {
		return appSharedPrefs.getString("pass", "");
	}
	
	public synchronized void setPassword(String password) {
		prefsEditor.putString("pass", password);
		prefsEditor.commit();
	}
	
	public synchronized int getQoS() {
		return Integer.parseInt(appSharedPrefs.getString("qos", "2"));
	}
	
	public synchronized String getToken() {
		return appSharedPrefs.getString("token", "");
	}
	
	public synchronized void setToken(String token) {
		prefsEditor.putString("token", token);
		prefsEditor.commit();
	}
	
	public synchronized boolean getWasStarted() {
		return appSharedPrefs.getBoolean("started", false);
	}
	
	public synchronized void setWasStarted(boolean started) {
		prefsEditor.putBoolean("started", started);
		prefsEditor.commit();
	}
	
	public synchronized boolean getLoggedIn() {
		return appSharedPrefs.getBoolean("loggedin", false);
	}
	
	public synchronized void setLoggedIn(boolean loggedin) {
		prefsEditor.putBoolean("loggedin", loggedin);
		prefsEditor.commit();
	}
	
	public synchronized boolean getIsConnected() {
		return appSharedPrefs.getBoolean("connected", false);
	}
	
	public synchronized void setIsConnected(boolean connected) {
		prefsEditor.putBoolean("connected", connected);
		prefsEditor.commit();
	}
	
	public synchronized boolean getAutoLogin() {
		return appSharedPrefs.getBoolean("autologin", false);
	}
	
	public synchronized void setAutoLogin(boolean autologin) {
		prefsEditor.putBoolean("autologin", autologin);
		prefsEditor.commit();
	}
	
	public synchronized boolean getVerified() {
		return appSharedPrefs.getBoolean("verified", false);
	}
	
	public synchronized void setVerified(boolean verified) {
		prefsEditor.putBoolean("verified", verified);
		prefsEditor.commit();
	}
	
	public synchronized boolean getRememberMe() {
		return appSharedPrefs.getBoolean("rememberme", false);
	}
	
	public synchronized void setRememberMe(boolean rememberme) {
		prefsEditor.putBoolean("rememberme", rememberme);
		prefsEditor.commit();
	}
	
	public synchronized String getLastUser() {
		return appSharedPrefs.getString("last_user", "");
	}
	
	public synchronized void setLastUser(String user) {
		prefsEditor.putString("last_user", user);
		prefsEditor.commit();
	}
	
	public synchronized String getLastToken() {
		return appSharedPrefs.getString("last_token", "");
	}
	
	public synchronized void setLastToken(String token) {
		prefsEditor.putString("last_token", token);
		prefsEditor.commit();
	}
	
	public synchronized String getDeviceId() {
		String id;
		if (appSharedPrefs.contains("deviceid")) {
			id = appSharedPrefs.getString("deviceid", "");
		} else {
			id = Secure.getString(this.context.getContentResolver(), Secure.ANDROID_ID);
			prefsEditor.putString("deviceid", id);
			prefsEditor.commit();
		}
		return id;
	}
	
	public synchronized void addNotifChannel(String channel_name) {
		prefsEditor.putBoolean("channel_" + channel_name, true);
		prefsEditor.commit();
	}
	
	public synchronized void removeNotifChannel(String channel_name) {
		prefsEditor.remove("channel_"+channel_name);
		prefsEditor.commit();
	}
	
	public synchronized boolean getNotifChannel(String channel_name) {
		return appSharedPrefs.getBoolean("channel_"+channel_name, false);
	}
	
	public synchronized void setOpenChannel(String channel_name) {
		prefsEditor.putString("open_channel", channel_name);
	}
	
	public synchronized String getOpenChannel() {
		return appSharedPrefs.getString("open_channel", "");
	}
	
	public synchronized void clearSavedUser() {
		prefsEditor.remove("user");
		prefsEditor.remove("pass");
		prefsEditor.remove("token");
		prefsEditor.putBoolean("rememberme", false);
		prefsEditor.putBoolean("autologin", false);
		prefsEditor.commit();
	}
	
}
