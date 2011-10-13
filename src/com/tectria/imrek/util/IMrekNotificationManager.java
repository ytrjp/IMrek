package com.tectria.imrek.util;

import java.util.ArrayList;

import com.tectria.imrek.IMrekActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;


public class IMrekNotificationManager {

	private static IMrekNotificationManager instance = null;
	private NotificationManager notifyMan;
	private Context context;
	private ArrayList<String> channelsToNotify;
	
	
	protected IMrekNotificationManager(Context ctx) {
		context = ctx;
		notifyMan = (NotificationManager)ctx.getSystemService(Service.NOTIFICATION_SERVICE);
	}
	
	public static IMrekNotificationManager getInstance(Context ctx) {
		if (instance == null) {
			instance = new IMrekNotificationManager(ctx);
		}
		return instance; 
	}
	
	public void addChannelToNotify(String chan) {
		channelsToNotify.add(chan);
	}
	
	public void addChannelsToNotify(ArrayList<String> chans) {
		channelsToNotify.addAll(chans);
	}
	
	public void notifyNewMessage(String chan) {
		int id = channelsToNotify.indexOf(chan);
		if (id > -1) {
			this.showNotification(id, "New Message", "You have new messages in " + chan, IMrekActivity.class);
		}
	}
	
	public void removeNotifsForChan(String chan) {
		notifyMan.cancel(channelsToNotify.indexOf(chan));
	}
	
	public Notification getNotificationObject(String title, String text, Class intentClass) {
		Notification n = new Notification();
		
		n.flags |= Notification.DEFAULT_ALL;
		n.icon = com.tectria.imrek.R.drawable.icon;
		n.when = System.currentTimeMillis();
		
		PendingIntent pi = PendingIntent.getActivity(context, 0,  new Intent(context, intentClass), 0);
		n.setLatestEventInfo(context, title, text, pi);
		
		return n;
	}
	
	
	private void showNotification(int notifId, String title, String text, Class intentClass) {
		Notification n = new Notification();
		n.flags |= Notification.DEFAULT_ALL;
		n.icon = com.tectria.imrek.R.drawable.icon;
		n.when = System.currentTimeMillis();
		
		PendingIntent pi = PendingIntent.getActivity(context, 0,  new Intent(context, intentClass), 0);
		n.setLatestEventInfo(context, title, text, pi);
		
		notifyMan.notify(notifId, n);
	}
}
