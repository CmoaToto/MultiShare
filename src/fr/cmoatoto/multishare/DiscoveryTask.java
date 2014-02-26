package fr.cmoatoto.multishare;

import java.net.SocketTimeoutException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class DiscoveryTask extends AsyncTask<String, Object, Object> {
	
	private static String TAG = DiscoveryTask.class.getName();
	
	public static String SEND_OBJECT = TAG + ".sendObject";
	public static String SEND_TEXT = TAG + ".sendText";
	public static String SEND_KEYBOARD_KEY = TAG + ".sendKeyboardKey";

	private Context mContext;
//	private String response;
	
//	private static String ssdpLocation = null;
	
	public DiscoveryTask(Context c) {
		mContext = c;
	}

	@Override
	protected Object doInBackground(String... args) {
//		SSDPSocket sock = null;
		
		String valueToPost = null;
		String mimeType = null;
		String extension = null;

		if (args.length >= 3 && args[0].equals(SEND_OBJECT)) {
			valueToPost = args[1];
			mimeType = args[2];
			if (args.length > 2) {
				extension = args[3];
			}
		} else if (args.length >= 2 && args[0].equals(SEND_TEXT)) {
			valueToPost = args[1];
			mimeType = "text/plain";
		} else if (args.length >= 2 && args[0].equals(SEND_KEYBOARD_KEY)) {
			valueToPost = args[1];
			mimeType = "keyboard/key";
		}

		try {
			Log.d(TAG, "SSDPSocket : created");

//			while (!isCancelled()) {
//				if (ssdpLocation == null) {
//					Log.d(TAG, "SSDPSocket : waiting for device IP");
//					sock = new SSDPSocket();
//					DatagramPacket dp = sock.receive();
//					Log.d(TAG, "SSDPSocket : received");
//					if (SSDPParser.isSSDPNotifyMsg(dp)) {
//					Log.d(TAG, "SSDPSocket : isSSDPNotifyMsg : " + SSDPParser.getSsdpNt(dp));
//						String msgNt = SSDPParser.getSsdpNt(dp);
//						if (msgNt != null && msgNt.equals("device:intentreceiver")) {
//							ssdpLocation = SSDPParser.getSsdpLocation(dp);
//							Log.d(TAG, "SSDP:LOCATION : " + ssdpLocation);
//						}
//					}
//					sock.close();
//					Log.d(TAG, "SSDPSocket : received --> closed");
//				} else {
//					Log.d(TAG, "SSDPSocket : Re-use last device IP : " + ssdpLocation);
//				}

//				if (ssdpLocation != null) {
						// Launch post Request with
					JSONObject jsonPostObject = new JSONObject();
					try {
						jsonPostObject.put("value", valueToPost);
						jsonPostObject.put("mime", mimeType);
						if (extension != null) {
							jsonPostObject.put("extension", extension);
						}

//						DefaultHttpClient httpClient = new DefaultHttpClient();
//						HttpPost httpPost = new HttpPost(ssdpLocation);
//						StringEntity se = new StringEntity(jsonPostObject.toString());
						Log.d(TAG, "SSDP:SEND : " + jsonPostObject.toString());
//						httpPost.setEntity(se);
//						httpPost.setHeader("Accept", "application/json");
//						httpPost.setHeader("Content-type", "application/json");
//						ResponseHandler responseHandler = new BasicResponseHandler();
//						response = httpClient.execute(httpPost, responseHandler);
//						Log.d(TAG, "SSDP:RESPONSE : " + response);
					} catch (JSONException e) {
						Log.d(TAG, Log.getStackTraceString(e));
					}
					cancel(false);
//				}
//			}
//		} catch (SocketTimeoutException e) {
//			Log.d(TAG, "TimeOut. No IP received from Device.");
//			ssdpLocation = null;
//			return e;
//		} catch (IOException e) {
//			Log.d(TAG, Log.getStackTraceString(e));
//			ssdpLocation = null;
		} finally {
//			if (sock != null) {
//				sock.close();
//				sock = null;
//			}
		}
		return null;
	}

	@Override
	protected void onCancelled(Object result) {
		Log.d(TAG, "DiscoveryTask : canceled");
	}

	@Override
	protected void onPostExecute(Object result) {
		super.onPostExecute(result);
		if (result instanceof SocketTimeoutException) {
			Toast.makeText(mContext, "No Device found. Check your connection/network.", Toast.LENGTH_LONG).show();
		}
		Log.d(TAG, "DiscoveryTask : finished");
	}
}
