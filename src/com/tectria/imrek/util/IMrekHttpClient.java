package com.tectria.imrek.util;

import com.loopj.android.http.*;

public class IMrekHttpClient {
	
	private static final String base_url = "http://broker.wilhall.com/stable/idlookup.php";
	private static AsyncHttpClient client = new AsyncHttpClient();
	
	private static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
	    client.get(url, params, responseHandler);
	}
	
	private static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
	    client.post(url, params, responseHandler);
	}
	
	public static void register(String username, String password, AsyncHttpResponseHandler handler) {
		RequestParams params = new RequestParams();
		params.put("username", username);
		params.put("password", password);
		params.put("action", "0");
		post(base_url, params, handler);
	}
	
	public static void verify(String username, String password, AsyncHttpResponseHandler handler) {
		RequestParams params = new RequestParams();
		params.put("username", username);
		params.put("password", password);
		params.put("action", "1");
		post(base_url, params, handler);
	}
}