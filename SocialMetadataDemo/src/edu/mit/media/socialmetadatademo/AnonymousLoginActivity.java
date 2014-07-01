package edu.mit.media.socialmetadatademo;

import java.util.Random;

import edu.mit.media.bandicootopenpdsdemo.R;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.openpds.client.PersonalDataStore;
import edu.mit.media.openpds.client.PreferencesWrapper;
import edu.mit.media.openpds.client.RegistryClient;
import edu.mit.media.openpds.client.UserInfoTask;
import edu.mit.media.openpds.client.UserLoginTask;
import edu.mit.media.openpds.client.UserRegistrationTask;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class AnonymousLoginActivity extends Activity {

	private RegistryClient mRegistryClient;

	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";
	public static final int AGREED_TO_TERMS_RESULT_CODE=17;

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserRegistrationTask mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	private String mEmail;
	private String mPassword;
	private String mPasswordConfirmation;
	private String mName;
	private String mPin;

	// UI references.
	private TextView mPinView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;
	
	protected SharedPreferences mPrefs;
	protected FunfManager mFunfManager = null;
	
	protected ServiceConnection mConnection = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_anonymous_login);
		mRegistryClient = new RegistryClient(getString(R.string.registry_url), getString(R.string.client_key), getString(R.string.client_secret), "funf_write", getString(R.string.client_basic_auth));		
		
		SharedPreferences prefs = this.getSharedPreferences("default", MODE_WORLD_READABLE);
		if (prefs.contains("pin")) {
			mPin = prefs.getString("pin", "");
		} else {
			Random random = new Random();
			Integer pin = random.nextInt(90000) + 10000;
			// Not checking uniqueness against the server - the odds of collisions in this small of a group is very small
			mPin = pin.toString();
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("pin", pin.toString());
			editor.commit();
		}
		
		mPinView = (TextView) findViewById(R.id.pinTextView);
		mPinView.setText(mPin);
		
		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		mConnection = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mFunfManager = ((FunfManager.LocalBinder) service).getManager();
				mFunfManager.runPipelineAction("upload");
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				mFunfManager = null;			
			}
		};	
		
		
		findViewById(R.id.uploadDataButton).setOnClickListener(
			new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Intent funfIntent = new Intent(AnonymousLoginActivity.this, FunfManager.class);
					if (mFunfManager == null) {
						AnonymousLoginActivity.this.bindService(funfIntent, mConnection, Context.BIND_AUTO_CREATE);
					} else {
						mFunfManager.runPipelineAction("upload");
					}					
				}
		});
		
		try {
			PersonalDataStore pds = new PersonalDataStore(this);
		} catch (Exception ex) {
			validateAndShowTerms();			
		}
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mConnection != null) {
			if (mFunfManager != null) {
				unbindService(mConnection);	
				Intent intent = new Intent(this, mFunfManager.getClass());
				stopService(intent);
				mFunfManager = null;
			}
			mConnection = null;
		}

	}
	
	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void validateAndShowTerms() {
		// Store values at the time of the login attempt.
		
		mName = "";
		mEmail = mPin + "@anonymous.com";
		mPassword = "aaaa";
		mPasswordConfirmation = "aaaa"; 

		boolean cancel = false;
		View focusView = null;

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			Intent termsIntent = new Intent(this, TermsAndConditionsActivity.class);
			startActivityForResult(termsIntent, 0);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == AGREED_TO_TERMS_RESULT_CODE) {
			if (mAuthTask != null) {
				return;
			}
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			mAuthTask = new UserRegistrationTask(this, new PreferencesWrapper(this), mRegistryClient) {
				protected void onPostExecute(String token) {
					super.onPostExecute(token);
					mAuthTask = null;
					showProgress(false);

					Intent funfIntent = new Intent(AnonymousLoginActivity.this, FunfManager.class);
					AnonymousLoginActivity.this.bindService(funfIntent, mConnection, Context.BIND_AUTO_CREATE);
				}
			};
			mAuthTask.execute(mName, mEmail, mPassword, mPin);
		}
	}
	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
}
