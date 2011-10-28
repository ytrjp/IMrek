package com.tectria.imrek.fragments;

import java.util.Vector;

import com.tectria.imrek.R;
import com.tectria.imrek.R.layout;
import com.tectria.imrek.util.IMrekConversationManager;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ChannelFragment extends Fragment {
    
	Context context;
	View layout;
	String topic;
	ListView convo;
	Vector<String> messages;
	IMrekConversationManager cmanager;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		context = getActivity().getApplicationContext();
		Bundle args = this.getArguments();
		topic = args.getString("topic");
		Vector<String> v = IMrekConversationManager.getInstance(context).getChannelUpdate(topic);
		layout = inflater.inflate(R.layout.f_channel, container, false);
		convo = (ListView)layout.findViewById(R.id.conversation);
		ListAdapter adapter = convo.getAdapter();
		if (adapter.isEmpty()) {
			for (String s : v) {
				
			}
		}
		//Add appropriate handlers, etc here (to communicate with IMrekConversations)
		
		//Get Managers
		cmanager = IMrekConversationManager.getInstance(getActivity().getBaseContext());
		
		//Get Messages
		messages = cmanager.openChannelMessages(topic);
		
		//Add appropriate handlers, etc here (to communicate with IMrekChannels)
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		
		layout = inflater.inflate(R.layout.f_channel, container, false);
		
		TextView text = (TextView)layout.findViewById(R.id.text);
		
		StringBuilder builder = new StringBuilder();
		for(int i=0;i<messages.size();i++) {
			builder.append(messages.get(i));
		}
		text.setText(builder.toString());
		
		return layout;
	}
}