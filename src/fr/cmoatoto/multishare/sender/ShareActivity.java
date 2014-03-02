package fr.cmoatoto.multishare.sender;

import java.util.Set;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;

/**
 *  SharingActivity only works as Intent getter, then it send the computed intent to a sender service and finish itself
 */
public class ShareActivity extends Activity {
	
	private static String TAG = ShareActivity.class.getName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set blank activity view
		setContentView(new View(this));
		getWindow().setBackgroundDrawableResource(android.R.color.transparent);

		Log.d(TAG, "ShareActivity called");

		// Intent
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		Log.d(TAG, "Load type : " + type);
		Log.d(TAG, "Load action : " + action);

		Intent sendServiceIntent = new Intent(this, HttpService.class);
		if (Intent.ACTION_SEND.equals(action) && type != null) {
			sendServiceIntent.putExtra(HttpService.TYPE_KEY, type);

			Log.d(TAG, "Load extras : ");
			Bundle extras = intent.getExtras();
			if (extras != null) {
				Set<String> extrasKeys = extras.keySet();
				for (String key : extrasKeys) {
					Log.d(TAG, "	- " + key + " = " + extras.get(key).toString());
				}
			}
			sendServiceIntent.putExtras(extras);
			startService(sendServiceIntent);

		} else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {

			if (type.startsWith("image/")) {
				// handleSendMultipleImages(intent); // Handle multiple images being sent
			}

		} else {
			// TODO Handle other intents, such as being started from the home screen
		}
		finish();
	}

}
