package fr.cmoatoto.multishare.sender;

import java.util.Set;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.net.nsd.NsdManager.ResolveListener;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

/**
 * SharingActivity only works as Intent getter, then it send the computed intent to a sender service and finish itself
 */
public class ShareActivity extends Activity {

	private static String TAG = ShareActivity.class.getName();
	private NsdManager mNsdManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set blank activity view
		setContentView(new View(this));
		getWindow().setBackgroundDrawableResource(android.R.color.transparent);

		mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
		mNsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, new DiscoveryListener() {

			@Override
			public void onStopDiscoveryFailed(String serviceType, int errorCode) {
				Log.e(TAG, "onStopDiscoveryFailed");
			}

			@Override
			public void onStartDiscoveryFailed(String serviceType, int errorCode) {
				Log.e(TAG, "onStartDiscoveryFailed");
			}

			@Override
			public void onServiceLost(NsdServiceInfo serviceInfo) {
				Log.e(TAG, "onServiceLost");
			}

			@Override
			public void onServiceFound(NsdServiceInfo serviceInfo) {
				Log.w(TAG, "onServiceFound " + serviceInfo.getServiceName() + " / " + serviceInfo.getHost() + " / " + serviceInfo.getPort());
				if ("MultiShare".equals(serviceInfo.getServiceName())) {
					mNsdManager.resolveService(serviceInfo, new ResolveListener() {

						@Override
						public void onServiceResolved(NsdServiceInfo serviceInfo) {
							Log.w(TAG, "SERVICE AT " + serviceInfo.getHost() + " / " + serviceInfo.getPort());
							sendData();
						}

						@Override
						public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
							// TODO Auto-generated method stub

						}
					});
				}
			}

			@Override
			public void onDiscoveryStopped(String serviceType) {
				Log.d(TAG, "onDiscoveryStopped");
			}

			@Override
			public void onDiscoveryStarted(String serviceType) {
				Log.d(TAG, "onDiscoveryStarted");
			}
		});
	}

	private void sendData() {
		Log.d(TAG, "ShareActivity called");

		// Intent
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		Log.d(TAG, "Load type : " + type);
		Log.d(TAG, "Load action : " + action);

		Intent sendServiceIntent = new Intent(this, HttpServiceSender.class);
		if (Intent.ACTION_SEND.equals(action) && type != null) {
			sendServiceIntent.putExtra(HttpServiceSender.TYPE_KEY, type);

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
				// handleSendMultipleImages(intent); // Handle multiple images
				// being sent
			}

		} else {
			// TODO Handle other intents, such as being started from the home
			// screen
		}
		finish();
	}

}
