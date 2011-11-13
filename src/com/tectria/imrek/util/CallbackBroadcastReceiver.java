package com.tectria.imrek.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class CallbackBroadcastReceiver extends BroadcastReceiver {
	
	public abstract void gotBroadcast(Context context, Intent intent);
	
	@Override
    public void onReceive(Context context, Intent intent) {
        gotBroadcast(context, intent);
    }
}
