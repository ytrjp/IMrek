package com.tectria.imrek.fragments;

import com.tectria.imrek.R;
import com.tectria.imrek.R.layout;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ChannelFragment extends Fragment {
    
	Context context;
	View layout;
	String topic;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		context = getActivity().getApplicationContext();
		Bundle args = this.getArguments();
		topic = args.getString("topic");
		
		layout = inflater.inflate(R.layout.f_channel, container, false);
		
		//Add appropriate handlers, etc here (to communicate with IMrekConversations)
		
		return layout;
	}
}