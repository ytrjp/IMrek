package com.tectria.imrek.util;

import com.loopj.android.http.*;

public class IMrekHttpClient {
	
	private static final String base_url = "http://broker.wilhall.com/unstable/action.php";
	private static final String keepalive_url = "http://broker.wilhall.com:port/keepalive/";
	private static AsyncHttpClient client = new AsyncHttpClient();
	
	private static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
	    client.get(url, params, responseHandler);
	}
	
	private static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
	    client.post(url, params, responseHandler);
	}
	
	public static void register(String username, String password, String deviceid, AsyncHttpResponseHandler handler) {
		RequestParams params = new RequestParams();
		params.put("username", username);
		params.put("password", password);
		params.put("deviceid", deviceid);
		params.put("action", "0");
		post(base_url, params, handler);
	}
	
	public static void login(String username, String password, String deviceid, AsyncHttpResponseHandler handler) {
		RequestParams params = new RequestParams();
		params.put("username", username);
		params.put("password", password);
		params.put("deviceid", deviceid);
		params.put("action", "1");
		post(base_url, params, handler);
	}
	
	public static void reconnect(String username, String token, String deviceid, AsyncHttpResponseHandler handler) {
		RequestParams params = new RequestParams();
		params.put("username", username);
		params.put("token", token);
		params.put("deviceid", deviceid);
		params.put("action", "2");
		post(base_url, params, handler);
	}
	
	public static void keepalive(String username, String token) {
		RequestParams params = new RequestParams();
		params.put("username", username);
		params.put("password", token);
		params.put("action", "3");
		get(keepalive_url, params, null);
	}
}