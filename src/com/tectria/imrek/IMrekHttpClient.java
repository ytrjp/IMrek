package com.tectria.imrek;

import com.loopj.android.http.*;

public class IMrekHttpClient {
	
	private String deviceID;
	private String base_url;
	private AsyncHttpClient client;
	
	public IMrekHttpClient(String deviceID) {
		this.deviceID = deviceID;
		this.client = new AsyncHttpClient();
		this.base_url = "http://broker.wilhall.com/";
	}
	
	private void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
	    client.get(url, params, responseHandler);
	}
	
	public void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
	    client.get(url, params, responseHandler);
	}
}