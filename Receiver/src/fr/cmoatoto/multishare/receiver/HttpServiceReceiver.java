package fr.cmoatoto.multishare.receiver;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdServiceInfo;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

public class HttpServiceReceiver extends Service implements RegistrationListener {

	private static HttpServiceReceiver httpService = null;

	private static String TAG = HttpServiceReceiver.class.getName();

	private static NanoHTTPDReceiver server;

	NsdManager mNsdManager;

	public static NanoHTTPDReceiver getHttpServer() {
		return server;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {
		httpService = this;
		Log.d(TAG, "My Service Created");
		try {
			server = new NanoHTTPDReceiver( //
					0, //
					new File("/"), //
					getApplicationContext() //
			);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("TAG", "using port " + server.getLocalPort());
		NsdServiceInfo nsi = new NsdServiceInfo();
		nsi.setPort(server.getLocalPort());
		nsi.setServiceName("MultiShare");
		nsi.setServiceType("_http._tcp.");
		mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
		mNsdManager.registerService(nsi, NsdManager.PROTOCOL_DNS_SD, this);
	}

	@Override
	public void onDestroy() {
		mNsdManager.unregisterService(this);
		httpService = null;

		Log.d("HttpService", "My Service Stopped");
		startHttpServiceIfNeeded(this);
	}

	@Override
	public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
		Log.e(TAG, "onUnregistrationFailed");
	}

	@Override
	public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
		Log.d(TAG, "onServiceUnregistered");
	}

	@Override
	public void onServiceRegistered(NsdServiceInfo serviceInfo) {
		Log.d(TAG, "onServiceRegistered");
	}

	@Override
	public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
		Log.e(TAG, "onRegistrationFailed");
	}

	public static void show(final Context c, String value, final String mime, final String extension) {
		// clean our last tmp files to avoid not enough space error
		File fileDir = new File(Environment.getExternalStorageDirectory() + "/download");
		if (fileDir.exists() && fileDir.isDirectory()) {
			String[] files = fileDir.list();
			if (files.length > 0) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].startsWith("tmp.")) {
						Log.d("HttpService", "removing " + Environment.getExternalStorageDirectory() + "/download/" + files[i]);
						boolean deleted = new File(Environment.getExternalStorageDirectory() + "/download/" + files[i]).delete();
						Log.d("HttpSercice", deleted ? "Success !" : "Fail !");
					}
				}
			}
		}

		// launch the file as a typical intent
		if (c != null && mime != null) {
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setDataAndType(Uri.parse(value), mime);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			// Don't stop video on end (if it is a vidéo ;) )
			i.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, false);
			if (c.getPackageManager().queryIntentActivities(i, 0).isEmpty()) {
				Log.w("HttpSercice", "NO ACTIVITY FOUND FOR : " + mime);
				if (value.startsWith("http")) {
					DownloadFile downloadFile = new DownloadFile(c, value, extension, new DownloadFileListener() {

						@Override
						public void onDownloadFinish() {
							show(c, "file://" + Environment.getExternalStorageDirectory() + "/download/tmp" + (extension != null ? ("." + extension) : ""), mime, null);
						}
					});
					downloadFile.start();
				}
				return;
			}
			c.startActivity(i);
			Log.w("HttpSercice", "Show : " + mime + ", value = " + value);
		}
	}

	public static void startHttpServiceIfNeeded(Context c) {
		if (httpService == null) {
			c.startService(new Intent(c, HttpServiceReceiver.class));
		}
	}

	private static class DownloadFile extends Thread {

		public DownloadFile(final Context c, final String urlString, final String extension, final DownloadFileListener listener) {
			super(new Runnable() {

				@Override
				public void run() {

					try {
						URL url = new URL(urlString);
						URLConnection connection = url.openConnection();
						connection.connect();
						// this will be useful so that you can show a typical
						// 0-100% progress bar
						int fileLength = connection.getContentLength();

						// download the file
						InputStream input = new BufferedInputStream(url.openStream());
						OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory() + "/download/tmp" + (extension != null ? ("." + extension) : ""));

						byte data[] = new byte[1024];
						long total = 0;
						int count;
						while ((count = input.read(data)) != -1) {
							total += count;
							// publishing the progress....
							output.write(data, 0, count);
						}

						output.flush();
						output.close();
						input.close();

						if (listener != null) {
							listener.onDownloadFinish();
						}
					} catch (Exception e) {
						Log.e("HttpService", Log.getStackTraceString(e));

					}
				}
			});
		}
	}

	private static abstract class DownloadFileListener {
		public abstract void onDownloadFinish();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
