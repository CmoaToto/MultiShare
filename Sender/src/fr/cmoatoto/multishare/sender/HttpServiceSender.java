package fr.cmoatoto.multishare.sender;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.webkit.MimeTypeMap;
import fr.cmoatoto.multishare.sender.DiscoverHelper.CandidateHostFoundListener;
import fr.cmoatoto.multishare.utils.AndroidUtils;

public class HttpServiceSender extends Service {

	private static String TAG = HttpServiceSender.class.getName();

	public static String TYPE_KEY = TAG + ".typeKey";

	private static NanoHTTPDSender server;
	private String mFormatedIpAddress;

	NsdManager mNsdManager;

	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	// Handler that receives messages from the thread
	private static class ServiceHandler extends Handler {
		private String destination = null;
		private Object lock = new Object();

		public ServiceHandler(Looper looper, Context c) {
			super(looper);
			new DiscoverHelper(c, new CandidateHostFoundListener() {

				@Override
				public void onHostFound(String destination) {
					synchronized (lock) {
						ServiceHandler.this.destination = destination;
						lock.notify();
					}
				}
			});
		}

		@Override
		public void handleMessage(Message msg) {
			synchronized (lock) {
				if (destination == null) {
					Log.d(TAG, "Waiting for discovery to end");
					try {
						lock.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Log.d(TAG, "Sending message " + msg.obj.toString() + "to " + destination + "/intent");
				try {
					HttpClient client = new DefaultHttpClient();
					HttpPost post = new HttpPost(destination + "/intent");
					List<NameValuePair> entities = new ArrayList<NameValuePair>();
					entities.add(new BasicNameValuePair("intent", msg.obj.toString()));
					post.setEntity(new UrlEncodedFormEntity(entities));
					client.execute(post);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

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

			Message msg = mServiceHandler.obtainMessage();
			String txt = intent.getStringExtra(Intent.EXTRA_TEXT);

			if (txt != null) {
				Log.d(TAG, "Text loaded : " + txt);
				JSONObject jsonPostObject = new JSONObject();
				try {
					jsonPostObject.put("value", txt);
					jsonPostObject.put("mime", "text/plain");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				msg.obj = jsonPostObject;
				mServiceHandler.sendMessage(msg);
				// BroadcastTask task = new BroadcastTask(getApplicationContext());
				// task.execute(BroadcastTask.SEND_TEXT, txt, type);
			}

		} else if (intent.hasExtra(Intent.EXTRA_STREAM)) {

			Uri streamUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
			File file = AndroidUtils.getFile(this, streamUri, false);
			Log.d(TAG, "Stream loaded : " + file.getPath());

			try {
				if (server != null) {
					server.stop();
				}
				server = new NanoHTTPDSender(0, null, this);

				Message msg = mServiceHandler.obtainMessage();
				URL ext = new URL(new URL(server.getLocalhost()), server.addFile(Uri.fromFile(file)));
				Log.d(TAG, "External URL : " + ext.toExternalForm());

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
				JSONObject jsonPostObject = new JSONObject();
				try {
					jsonPostObject.put("value", ext.toExternalForm());
					jsonPostObject.put("mime", mimetype != null ? mimetype : type);
					jsonPostObject.put("extension", extension);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				msg.obj = jsonPostObject;
				mServiceHandler.sendMessage(msg);
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

		mFormatedIpAddress = "http://" + AndroidUtils.getIPAddress(true) + ":XX/";

		Log.d(TAG, "HttpService Started On " + mFormatedIpAddress);

		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block. We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper, getApplicationContext());

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
