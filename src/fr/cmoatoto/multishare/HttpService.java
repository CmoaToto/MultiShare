package fr.cmoatoto.multishare;

import java.io.File;
import java.net.URL;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.IBinder;
import android.util.Log;
import android.webkit.MimeTypeMap;
import fr.cmoatoto.multishare.utils.AndroidUtils;

public class HttpService extends Service {

	private static String TAG = HttpService.class.getName();

	public static String TYPE_KEY = TAG + ".typeKey";

	private static final int PORT = 8765;

	private MulticastLock mCastLock;
	// private static NanoHTTPD server;
	private String mFormatedIpAddress;

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
				DiscoveryTask task = new DiscoveryTask(this);
				task.execute(DiscoveryTask.SEND_TEXT, txt, type);
			}

		} else if (intent.hasExtra(Intent.EXTRA_STREAM)) {

			Uri streamUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
			File file = AndroidUtils.getFile(this, streamUri, false);
			Log.d(TAG, "Stream loaded : " + file.getPath());

//			try {
				// TODO : Fix this
//				if (server != null) {
//					server.stop();
//				}
//				server = new NanoHTTPD(PORT, null, this);
//				server.setFormatedIpAddress(mFormatedIpAddress);

				URL ext = null;//new URL(new URL(server.getLocalhost()), server.addFile(file));
				Log.d(TAG, "External URL : ");// + ext.toExternalForm());

				String mimetype = null;
				String extension = "*";
				if (file.getName().contains(".")) {
					extension = AndroidUtils.extension(file.getName());
					mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
				}
				if (mimetype == null) {
					if (type.startsWith("*/")) {
						mimetype = "application/" + extension;
					} else {
						mimetype = type.replace("/*", "/" + extension);
					}
				}

				// launch STB discover
				DiscoveryTask task = new DiscoveryTask(this);
				task.execute(DiscoveryTask.SEND_OBJECT, null/*ext.toExternalForm()*/, mimetype != null ? mimetype : type, extension != null ? extension : null);
//			} catch (MalformedURLException e) {
//				Log.d(TAG, Log.getStackTraceString(e));
//			} catch (IOException e) {
//				Log.d(TAG, Log.getStackTraceString(e));
//			}

		}

		mCastLock = ((WifiManager) getSystemService(WIFI_SERVICE)).createMulticastLock("SSDP");
		mCastLock.acquire();

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {

		Log.d(TAG, "HttpService Created");

		mFormatedIpAddress = "http://" + AndroidUtils.getIPAddress(true) + ":" + PORT + "/";

		Log.d(TAG, "HttpService Started On " + mFormatedIpAddress);
	}

	public void onDestroy() {

//		if (server != null)
//			server.stop();
		if (mCastLock != null)
			mCastLock.release();

		Log.d(TAG, "HttpService Stopped");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
