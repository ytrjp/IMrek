package com.tectria.imrek;

import org.json.*;

import com.loopj.android.http.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.content.DialogInterface.OnCancelListener;

public class IMrekSplashLoginActivity extends Activity {
	
	private SharedPreferences prefs;
	private Editor editor;
	private AlertDialog.Builder dialog;
	private ProgressDialog progressdialog;
	private LayoutInflater inflater;
	private View dialogview;
	
	private EditText username;
	private EditText password;
	private CheckBox rememberme;
	private CheckBox autologin;
	private EditText confirm;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.splash);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	
    	makeProgressDialog();
    	prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	if(prefs.getBoolean("autologin", false) || prefs.getBoolean("loggedin", false)) {
    		startMain();
    	} else {
    		makeHelloDialog();
    	}
    }
    
    @Override
    public void onRestart() {
    	super.onRestart();
    	startMain();
    }
    
    @Override
    public void onBackPressed() {
       return;
    }
    
    public void startMain() {
    	Intent intent = new Intent(getBaseContext(), IMrekActivity.class);
    	startActivity(intent);
    }
    
    public void startMain(String user, String pass) {
    	Intent intent = new Intent(getBaseContext(), IMrekActivity.class);
    	intent.putExtra("user", user);
    	intent.putExtra("pass", pass);
    	startActivity(intent);
    }
    
    public void makeProgressDialog() {
    	progressdialog = ProgressDialog.show(this, "", "Loading. Please wait...", true);
    }
    
    public void makeHelloDialog() {
    	if(progressdialog.isShowing()) {
    		progressdialog.dismiss();
    	}
    	
    	dialog = new AlertDialog.Builder(this);
    	
    	dialog.setPositiveButton("Register", new DialogInterface.OnClickListener() {
    		@Override
			public void onClick(final DialogInterface dialog, int id) {
    			dialog.dismiss();
    			makeRegistrationDialog();
           }
       })
       .setNegativeButton("Login", new DialogInterface.OnClickListener() {
    	   	@Override
			public void onClick(DialogInterface dialog, int id) {
    	   		dialog.dismiss();
    	   		makeLoginDialog();
    	   	}
       });
    	dialog.show();
    }
    
    public void makeRegistrationDialog() {
    	if(progressdialog.isShowing()) {
    		progressdialog.dismiss();
    	}
    	dialogview = inflater.inflate(R.layout.registration_dialog, null);
    	dialog = new AlertDialog.Builder(this);
    	dialog.setView(dialogview);
    	
    	username = (EditText)dialogview.findViewById(R.id.username);
    	password = (EditText)dialogview.findViewById(R.id.password);
    	confirm = (EditText)dialogview.findViewById(R.id.confirm);
    	rememberme = (CheckBox)dialogview.findViewById(R.id.rememberme);
    	autologin = (CheckBox)dialogview.findViewById(R.id.autologin);
    	
    	rememberme.setChecked(prefs.getBoolean("rememberme", false));
    	autologin.setChecked(prefs.getBoolean("autologin", false));
    	
    	if(prefs.getBoolean("rememberme", false)) {
    		username.setText(prefs.getString("username", ""));
    	}
    	
    	dialog.setPositiveButton("Register", new DialogInterface.OnClickListener() {
    		@Override
			public void onClick(final DialogInterface dialog, int id) {
    			
    			dialog.dismiss();
    			makeProgressDialog();
    			
    			final String user = username.getText().toString();
    			final String pass = password.getText().toString();
    			
    			if(!pass.equals(confirm.getText().toString())) {
    				makeRegistrationDialog();
    				Toast toast = Toast.makeText(getApplicationContext(), "Passwords do not match", Toast.LENGTH_LONG);
					toast.show();
					return;
    			}
    			
    			if(pass.length() < 6) {
    				makeRegistrationDialog();
    				Toast toast = Toast.makeText(getApplicationContext(), "Password must be at least 6 characters", Toast.LENGTH_LONG);
					toast.show();
					return;
    			}
    			
    			if(user.length() < 6) {
    				makeRegistrationDialog();
    				Toast toast = Toast.makeText(getApplicationContext(), "Username must be at least 6 characters", Toast.LENGTH_LONG);
					toast.show();
					return;
    			}
    			
    			
    			
    			AsyncHttpClient http = new AsyncHttpClient();
    			RequestParams params = new RequestParams();
    			params.put("username", user);
    			params.put("password", pass);
    			params.put("action", "0");
    			
    			http.post("http://broker.wilhall.com/idlookup.php", params, new AsyncHttpResponseHandler() {
    	            @Override
    	            public void onFailure(Throwable error) {
    					makeRegistrationDialog();
    					Toast toast = Toast.makeText(getApplicationContext(), "An error occured contacting the server. Please try again.", Toast.LENGTH_LONG);
    					toast.show();
    	            }
    				
    				@Override
    	            public void onSuccess(String strdata) {
    	                try {
    	                	JSONObject data = new JSONObject(strdata);
    	                	if(data.getInt("error") == 1) {
    	                		makeRegistrationDialog();
    	    					Toast toast = Toast.makeText(getApplicationContext(), "Error: " + data.getString("message"), Toast.LENGTH_LONG);
    	    					toast.show();
    	    					return;
    	                	}
    	                	editor = prefs.edit();
    	    				editor.putBoolean("rememberme", rememberme.isChecked());
    	    				editor.putBoolean("autologin", autologin.isChecked());
    	    				editor.putBoolean("loggedin", true);
    	    				if(autologin.isChecked()) {
    	    					editor.putString("username", user);
    	    					editor.putString("password", pass);
    	    				} else if(rememberme.isChecked()) {
    	    					editor.putString("username", username.getText().toString());
    	    				}
    	    				editor.commit();
    	    				if(autologin.isChecked()) {
    	    					startMain();
    	    				} else {
    	    					startMain(user, pass);
    	    				}
    	                    
    	                } catch(JSONException e) {
    	                    dialog.dismiss();
    	                    makeRegistrationDialog();
    	                    Toast toast = Toast.makeText(getApplicationContext(), "Unknown error occured", Toast.LENGTH_LONG);
    	    				toast.show();
    	                }
    	            }
    	        });
           }
       });
    	
    	dialog.setTitle("Registration");
    	dialog.setCancelable(false);
    	dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				dialog.cancel();
				makeRegistrationDialog();
			}
    	
    	});
    	dialog.show();
    }
    
    public void makeLoginDialog() {
    	if(progressdialog.isShowing()) {
    		progressdialog.dismiss();
    	}
    	dialogview = inflater.inflate(R.layout.login_dialog, null);
    	dialog = new AlertDialog.Builder(this);
    	dialog.setView(dialogview);
    	
    	username = (EditText)dialogview.findViewById(R.id.username);
    	password = (EditText)dialogview.findViewById(R.id.password);
    	rememberme = (CheckBox)dialogview.findViewById(R.id.rememberme);
    	autologin = (CheckBox)dialogview.findViewById(R.id.autologin);
    	
    	rememberme.setChecked(prefs.getBoolean("rememberme", false));
    	autologin.setChecked(prefs.getBoolean("autologin", false));
    	
    	if(prefs.getBoolean("rememberme", false)) {
    		username.setText(prefs.getString("username", ""));
    	}
    	
    	dialog.setPositiveButton("Login", new DialogInterface.OnClickListener() {
    		@Override
			public void onClick(final DialogInterface dialog, int id) {
    			
    			dialog.dismiss();
    			makeProgressDialog();
    			
    			final String user = username.getText().toString();
    			final String pass = password.getText().toString();
    			
    			AsyncHttpClient http = new AsyncHttpClient();
    			RequestParams params = new RequestParams();
    			params.put("username", user);
    			params.put("password", pass);
    			params.put("action", "1");
    			
    			http.post("http://broker.wilhall.com/idlookup.php", params, new AsyncHttpResponseHandler() {
    	            @Override
    	            public void onFailure(Throwable error) {
    					makeLoginDialog();
    					Toast toast = Toast.makeText(getApplicationContext(), "An error occured contacting the server. Please try again.", Toast.LENGTH_LONG);
    					toast.show();
    	            }
    				
    				@Override
    	            public void onSuccess(String strdata) {
    	                try {
    	                	JSONObject data = new JSONObject(strdata);
    	                	if(data.getInt("error") == 1) {
    	                		makeLoginDialog();
    	    					Toast toast = Toast.makeText(getApplicationContext(), "Error: " + data.getString("message"), Toast.LENGTH_LONG);
    	    					toast.show();
    	                	}
    	                	editor = prefs.edit();
    	    				editor.putBoolean("rememberme", rememberme.isChecked());
    	    				editor.putBoolean("autologin", autologin.isChecked());
    	    				if(autologin.isChecked()) {
    	    					editor.putString("username", user);
    	    					editor.putString("password", pass);
    	    				} else if(rememberme.isChecked()) {
    	    					editor.putString("username", username.getText().toString());
    	    				}
    	    				editor.commit();
    	    				if(autologin.isChecked()) {
    	    					startMain();
    	    				} else {
    	    					startMain(user, pass);
    	    				}
    	                    
    	                } catch(JSONException e) {
    	                    dialog.dismiss();
    	                    makeLoginDialog();
    	                    Toast toast = Toast.makeText(getApplicationContext(), "Unknown error occured", Toast.LENGTH_LONG);
    	    				toast.show();
    	                }
    	            }
    	        });
           }
       });
    	
    	dialog.setTitle("Login");
    	dialog.setCancelable(false);
    	dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				dialog.cancel();
				makeLoginDialog();
			}
    	
    	});
    	dialog.show();
    }
}
