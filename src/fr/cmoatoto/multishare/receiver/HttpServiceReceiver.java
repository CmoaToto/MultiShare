package fr.cmoatoto.multishare.receiver;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;
import org.ray.upnp.ssdp.SSDPSocket;

import fr.cmoatoto.multishare.sender.BroadcastTask;
import fr.cmoatoto.multishare.sender.HttpServiceSender;
import fr.cmoatoto.multishare.utils.AndroidUtils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

public class HttpServiceReceiver extends Service {

	private static HttpServiceReceiver httpService = null;

	private static String TAG = HttpServiceReceiver.class.getName();

	private static final int PORT = 8765;

	private static NanoHTTPDReceiver server;
	private String mFormatedIpAddress;

	public static NanoHTTPDReceiver getHttpServer() {
		return server;
	}

	public String getFormatedAddress() {
		return mFormatedIpAddress;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {

		httpService = this;

		Log.d(TAG, "My Service Created");

		mFormatedIpAddress = "http://" + AndroidUtils.getIPAddress(true) + ":" + PORT + "/";

		Log.d("TAG", "start on " + mFormatedIpAddress);
		
		ReceiveTask task = new ReceiveTask(this);
		task.execute();

//		try {
//			// SSDPSocket.init(this);
//			server = new NanoHTTPDReceiver(PORT, null, this);
//			server.setFormatedIpAddress(mFormatedIpAddress);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	@Override
	public void onDestroy() {

		httpService = null;

		Log.d("HttpService", "My Service Stopped");

		startHttpServiceIfNeeded(this);
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
							show(c, "file://" + Environment.getExternalStorageDirectory() + "/download/tmp" + (extension != null ? ("." + extension) : ""),
									mime, null);
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
						// this will be useful so that you can show a typical 0-100% progress bar
						int fileLength = connection.getContentLength();

						// download the file
						InputStream input = new BufferedInputStream(url.openStream());
						OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory() + "/download/tmp"
								+ (extension != null ? ("." + extension) : ""));

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
