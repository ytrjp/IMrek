package com.tectria.imrek;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import com.tectria.imrek.fragments.ChannelFragment;
import com.tectria.imrek.util.ChannelPagerAdapter;
import com.tectria.imrek.util.IMrekPreferenceManager;

 public class IMrekChannels extends FragmentActivity {   
	
	IMrekPreferenceManager prefs;
	private ChannelPagerAdapter pageradapter;
	Vector<String> channels;
	List<Fragment> fragments;
	
	//Bundle object to be reused
	Bundle bundle;

	/**
	 * Initialize the fragments to be paged
	 */
	private void initializePaging() {
		//Get a list of channels
		//For now use this test list
		channels = new Vector<String>(Arrays.asList("one", "two", "three"));
		
		fragments = new Vector<Fragment>();
		for(int i=0;i<channels.size();i++) {
			bundle = new Bundle();
			bundle.putString("topic", channels.get(i));
			fragments.add(Fragment.instantiate(this, ChannelFragment.class.getName(), bundle));
		}
		pageradapter  = new ChannelPagerAdapter(super.getSupportFragmentManager(), fragments);
		ViewPager pager = (ViewPager)super.findViewById(R.id.viewpager);
		pager.setAdapter(pageradapter);
	}
	
	public void addConvo(String name) {
		channels.add(name);
		bundle = new Bundle();
		bundle.putString("topic", name);
		fragments.add(Fragment.instantiate(this, ChannelFragment.class.getName(), bundle));
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Request window feature for custom title
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.channels);
        
        //Actually set a custom title using our XML
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
        
        //Get our preference manager
        prefs = IMrekPreferenceManager.getInstance(this);
        
        initializePaging();
    }
 
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem mi) {
		switch(mi.getItemId()) {
			case R.id.preferences:
				Intent prefIntent = new Intent(getBaseContext(), PreferenceScreen.class);
				startActivity(prefIntent);
				break;
			case R.id.logout:
				break;
			case R.id.reconnect:
				break;
			case R.id.quit:
				break;
		}
		return true;
	}
}