package fr.cmoatoto.multishare.sender;

import org.json.JSONException;
import org.json.JSONObject;
import org.ray.upnp.ssdp.SSDPSocket;

import android.os.AsyncTask;
import android.util.Log;

public class BroadcastTask extends AsyncTask<String, Object, Object> {

	private static String TAG = BroadcastTask.class.getName();

	public static String SEND_OBJECT = TAG + ".sendObject";
	public static String SEND_TEXT = TAG + ".sendText";
	public static String SEND_KEYBOARD_KEY = TAG + ".sendKeyboardKey";

	@Override
	protected Object doInBackground(String... args) {

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

		JSONObject jsonPostObject = new JSONObject();
		try {
			jsonPostObject.put("value", valueToPost);
			jsonPostObject.put("mime", mimeType);
			if (extension != null) {
				jsonPostObject.put("extension", extension);
			}
			Log.d(TAG, "SSDP:SEND : " + jsonPostObject.toString());
		} catch (JSONException e) {
			Log.d(TAG, Log.getStackTraceString(e));
		}
		SSDPSocket.send(jsonPostObject);
		return null;
	}

	@Override
	protected void onCancelled(Object result) {
		Log.d(TAG, "canceled");
	}

	@Override
	protected void onPostExecute(Object result) {
		super.onPostExecute(result);
		Log.d(TAG, "finished");
	}
}
