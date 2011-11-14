package com.tectria.imrek;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.tectria.imrek.util.IMrekHttpClient;
import com.tectria.imrek.util.IMrekPreferenceManager;

public class SplashScreenLogin extends Activity {
	
	//Managers
	private IMrekPreferenceManager prefs;
	private LayoutInflater inflater;
	
	//Dialogs
	private AlertDialog.Builder dialog;
	private ProgressDialog progressdialog;
	private View dialogview;
	
	//Views
	private EditText username;
	private EditText password;
	private CheckBox rememberme;
	private CheckBox autologin;
	private EditText confirm;
	
	private boolean authed;
	private boolean atsplash;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.splash);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	
    	makeProgressDialog();
    	prefs = IMrekPreferenceManager.getInstance(this);
    	
    	if(prefs.getCrashedLastClose()) {
    		prefs.clearSavedUser();
    		prefs.setCrashedLastClose(false);
    	}
    	
    	//Get our deviceid
    	inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	if(prefs.getAutoLogin() || prefs.getLoggedIn()) {
    		startMain();
    	} else {
    		makeHelloDialog();
    	}
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	try {
    		progressdialog.dismiss();
    	} catch(Exception e) {
    		//Nothing
    	}
    }
    
    @Override
    public void onRestart() {
    	super.onRestart();
    	startMain();
    }
    
    @Override
    public void onBackPressed() {
    	if(!atsplash) {
	    	Intent intent = new Intent(getBaseContext(), SplashScreenLogin.class);
	    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	    	startActivity(intent);
	    	finish();
    	} else {
    		finish();
    	}
    }
    
    @Override
    public void onUserLeaveHint() {
    	if(!authed) {
    		finish();
    	}
    }
    
    public void startMain() {
    	Intent intent = new Intent(getBaseContext(), IMrekMain.class);
    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    	startActivity(intent);
    }
    
    public void startMain(String user, String pass) {
    	Intent intent = new Intent(getBaseContext(), IMrekMain.class);
    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    	intent.putExtra("user", user);
    	intent.putExtra("pass", pass);
    	startActivity(intent);
    }
    
    public void makeProgressDialog() {
    	atsplash = false;
    	progressdialog = ProgressDialog.show(this, "", "Loading. Please wait...", true);
    	progressdialog.getWindow().setGravity(Gravity.BOTTOM);
    }
    
    public void makeHelloDialog() {
    	atsplash = true;
    	try {
	    	if(progressdialog.isShowing()) {
	    		progressdialog.dismiss();
	    	}
    	} catch(Exception e) {
    		//continue
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
       }).setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				dialog.dismiss();
				finish();
			}  
       });
    	AlertDialog dialogC = dialog.create();
    	dialogC.getWindow().setGravity(Gravity.BOTTOM);
    	dialogC.show();
    }
    
    public void makeRegistrationDialog() {
    	atsplash = false;
    	if(progressdialog.isShowing()) {
    		progressdialog.dismiss();
    	}
    	dialogview = inflater.inflate(R.layout.dialog_registration, null);
    	dialog = new AlertDialog.Builder(this);
    	dialog.setTitle("Registration");
    	dialog.setView(dialogview);
    	
    	username = (EditText)dialogview.findViewById(R.id.username);
    	password = (EditText)dialogview.findViewById(R.id.password);
    	confirm = (EditText)dialogview.findViewById(R.id.confirm);
    	rememberme = (CheckBox)dialogview.findViewById(R.id.rememberme);
    	autologin = (CheckBox)dialogview.findViewById(R.id.autologin);
    	
    	rememberme.setChecked(prefs.getRememberMe());
    	autologin.setChecked(prefs.getAutoLogin());
    	
    	if(prefs.getRememberMe()) {
    		username.setText(prefs.getUsername());
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
    			
    			if(user.length() < 5) {
    				makeRegistrationDialog();
    				Toast toast = Toast.makeText(getApplicationContext(), "Username must be at least 6 characters", Toast.LENGTH_LONG);
					toast.show();
					return;
    			}
    			
    			IMrekHttpClient.register(user, pass, prefs.getDeviceId(), new AsyncHttpResponseHandler() {
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
    	                	if(data.getInt("status") == 1) {
    	                		makeRegistrationDialog();
    	    					Toast toast = Toast.makeText(getApplicationContext(), "Error: " + data.getString("message"), Toast.LENGTH_LONG);
    	    					toast.show();
    	    					return;
    	                	}
    	                	authed = true;
    	                	prefs.setRememberMe(rememberme.isChecked());
    	                	prefs.setAutoLogin(autologin.isChecked());
    	                	prefs.setUsername(username.getText().toString());
    	                	prefs.setPassword(password.getText().toString());
    	                	prefs.setToken(data.getJSONObject("data").getString("token"));
    	                	prefs.setLastUser(username.getText().toString());
    	                	prefs.setLastToken(data.getJSONObject("data").getString("token"));
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
    	
    	dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				dialog.dismiss();
				makeHelloDialog();
			}
    	
    	});
    	dialog.show();
    }
    
    public void makeLoginDialog() {
    	atsplash = false;
    	if(progressdialog.isShowing()) {
    		progressdialog.dismiss();
    	}
    	dialogview = inflater.inflate(R.layout.dialog_login, null);
    	dialog = new AlertDialog.Builder(this);
    	dialog.setTitle("Login");
    	dialog.setView(dialogview);
    	
    	username = (EditText)dialogview.findViewById(R.id.username);
    	password = (EditText)dialogview.findViewById(R.id.password);
    	rememberme = (CheckBox)dialogview.findViewById(R.id.rememberme);
    	autologin = (CheckBox)dialogview.findViewById(R.id.autologin);
    	
    	rememberme.setChecked(prefs.getRememberMe());
    	autologin.setChecked(prefs.getAutoLogin());
    	
    	if(prefs.getRememberMe()) {
    		username.setText(prefs.getUsername());
    	}
    	
    	dialog.setPositiveButton("Login", new DialogInterface.OnClickListener() {
    		@Override
			public void onClick(final DialogInterface dialog, int id) {
    			
    			dialog.dismiss();
    			makeProgressDialog();
    			
    			final String user = username.getText().toString();
    			final String pass = password.getText().toString();
    			
    			IMrekHttpClient.login(user, pass, prefs.getDeviceId(), new AsyncHttpResponseHandler() {
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
    	                	if(data.getInt("status") == 1) { 
    	                		makeLoginDialog();
    	    					Toast toast = Toast.makeText(getApplicationContext(), "Error: " + data.getString("message"), Toast.LENGTH_LONG);
    	    					toast.show();
    	    					return;
    	                	}
    	                	authed = true;
    	                	prefs.setRememberMe(rememberme.isChecked());
    	                	prefs.setAutoLogin(autologin.isChecked());
    	                	prefs.setUsername(username.getText().toString());
    	                	prefs.setPassword(password.getText().toString());
    	                	prefs.setToken(data.getJSONObject("data").getString("token"));
    	                	prefs.setLastUser(username.getText().toString());
    	                	prefs.setLastToken(data.getJSONObject("data").getString("token"));
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
    	
    	dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				dialog.dismiss();
				makeHelloDialog();
			}
    	
    	});
    	dialog.show();
    }
}
