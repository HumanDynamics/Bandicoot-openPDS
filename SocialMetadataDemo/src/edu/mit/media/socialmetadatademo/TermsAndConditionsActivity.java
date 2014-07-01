package edu.mit.media.socialmetadatademo;

import android.os.Bundle;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import edu.mit.media.bandicootopenpdsdemo.R;

public class TermsAndConditionsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_terms_and_conditions);
		setResult(RESULT_CANCELED);
		TextView tv = ((TextView) findViewById(R.id.share_button_title));
		tv.setMovementMethod(new ScrollingMovementMethod());
		findViewById(R.id.terms_agree_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(AnonymousLoginActivity.AGREED_TO_TERMS_RESULT_CODE);
				finish();
			}
		});
	}
}
