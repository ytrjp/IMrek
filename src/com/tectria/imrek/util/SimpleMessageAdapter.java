package com.tectria.imrek.util;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.widget.SimpleAdapter;

public class SimpleMessageAdapter extends SimpleAdapter {
	
	public SimpleMessageAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
	}

	public boolean areAllItemsEnabled() 
    { 
		return false; 
    }
	
    public boolean isEnabled(int position) 
    { 
    	return false; 
    } 
}
