package fr.cmoatoto.multishare.sender;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.os.IBinder;
import android.util.Log;
import android.webkit.MimeTypeMap;
import fr.cmoatoto.multishare.utils.AndroidUtils;

public class HttpServiceSender extends Service {

	private static String TAG = HttpServiceSender.class.getName();

	public static String TYPE_KEY = TAG + ".typeKey";

	private static NanoHTTPDSender server;
	private String mFormatedIpAddress;

	NsdManager mNsdManager;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (intent == null) {
			stopSelf();
			return super.onStartCommand(intent, flags, startId);
		}
		String type = intent.getStringExtra(TYPE_KEY);
		if (type == null) {
			type = intent.getType();
		}
		if (type == null) {
			stopSelf();
			return super.onStartCommand(intent, flags, startId);
		}

		if (intent.hasExtra(Intent.EXTRA_TEXT)) {

			String txt = intent.getStringExtra(Intent.EXTRA_TEXT);

			if (txt != null) {
				Log.d(TAG, "Text loaded : " + txt);

				// launch other device discovering
				BroadcastTask task = new BroadcastTask();
				task.execute(BroadcastTask.SEND_TEXT, txt, type);
			}

		} else if (intent.hasExtra(Intent.EXTRA_STREAM)) {

			Uri streamUri = (Uri) intent
					.getParcelableExtra(Intent.EXTRA_STREAM);
			File file = AndroidUtils.getFile(this, streamUri, false);
			Log.d(TAG, "Stream loaded : " + file.getPath());

			try {
				if (server != null) {
					server.stop();
				}
				server = new NanoHTTPDSender(0, null, this);
				server.setFormatedIpAddress(mFormatedIpAddress);

				URL ext = new URL(new URL(server.getLocalhost()),
						server.addFile(Uri.fromFile(file)));
				Log.d(TAG, "External URL : " + ext.toExternalForm());

				String mimetype = null;
				String extension = "*";
				if (file.getName().contains(".")) {
					extension = AndroidUtils.extension(file.getName());
					mimetype = MimeTypeMap.getSingleton()
							.getMimeTypeFromExtension(extension);
				}
				if (mimetype == null) {
					if (type.startsWith("*/")) {
						mimetype = "application/" + extension;
					} else {
						mimetype = type.replace("/*", "/" + extension);
					}
				}

				// launch device discover
				BroadcastTask task = new BroadcastTask();
				task.execute(BroadcastTask.SEND_OBJECT, ext.toExternalForm(),
						mimetype != null ? mimetype : type,
						extension != null ? extension : null);
			} catch (MalformedURLException e) {
				Log.d(TAG, Log.getStackTraceString(e));
			} catch (IOException e) {
				Log.d(TAG, Log.getStackTraceString(e));
			}

		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {

		Log.d(TAG, "HttpService Created");

		mFormatedIpAddress = "http://" + AndroidUtils.getIPAddress(true)
				+ ":XX/";

		Log.d(TAG, "HttpService Started On " + mFormatedIpAddress);
	}

	public void onDestroy() {

		if (server != null)
			server.stop();

		Log.d(TAG, "HttpService Stopped");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
