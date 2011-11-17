package com.tectria.imrek.util;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.tectria.imrek.IMrekChannels;
import com.tectria.imrek.IMrekMain;


public class IMrekNotificationManager {

	private static IMrekNotificationManager instance = null;
	private NotificationManager notifyMan;
	private Context context;
	
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
	
	
	public void notifyNewMessage(String chan, int channel_id) {
		
		if (IMrekPreferenceManager.getInstance(context).getOpenChannel() != chan) {
			this.showNotification(channel_id, chan, channel_id, "New Message", "You have new messages", IMrekChannels.class);
		}
	}
	
	public void removeNotif(int channel_id) {
		notifyMan.cancel(channel_id);
	}
	
	@SuppressWarnings("rawtypes")
	public Notification getNotificationObject(String title, String text, Class intentClass) {
		Notification n = new Notification();
		n.defaults |= Notification.DEFAULT_SOUND;
		n.flags |= Notification.FLAG_AUTO_CANCEL;
		n.icon = com.tectria.imrek.R.drawable.icon;
		n.when = System.currentTimeMillis();
		
		Intent intent = new Intent(context, intentClass);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
		n.setLatestEventInfo(context, title, text, pi);
		
		return n;
	}
	
	@SuppressWarnings("rawtypes")
	private void showNotification(int notifId, String channel, int channel_id, String title, String text, Class intentClass) {
		Notification n = new Notification();
		n.flags |= Notification.DEFAULT_SOUND;
		n.flags |= Notification.FLAG_AUTO_CANCEL;
		n.icon = com.tectria.imrek.R.drawable.icon;
		n.when = System.currentTimeMillis();
		
		Intent i = new Intent(context, intentClass);
		i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		i.putExtra("channel_id", notifId);
		
		PendingIntent pi = PendingIntent.getActivity(context, 0,  i, 0);
		n.setLatestEventInfo(context, title, text, pi);
		
		notifyMan.notify(notifId, n);
	}
}

