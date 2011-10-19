package com.tectria.imrek.fragments;

import com.tectria.imrek.R;
import com.tectria.imrek.R.id;
import com.tectria.imrek.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class FriendsListFragment extends Fragment {
	
	ListView list = null;
	String[] test = {"1", "2"};
    
    /** (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (container == null) {
            //Tab isnt seen so doesnt need to be created
            return null;
        }
		LinearLayout llayout = (LinearLayout)inflater.inflate(R.layout.f_friends_list, container, false);
		list = (ListView)llayout.findViewById(R.id.friendslist);
        
        list.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.item_friends_list, R.id.name, test));
        
        list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Toast.makeText(getActivity().getApplicationContext(), test[position], Toast.LENGTH_SHORT).show();
			}
        });
		return llayout;
	}
}