package fr.cmoatoto.multishare.receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;

import org.ray.upnp.ssdp.SSDPParser;
import org.ray.upnp.ssdp.SSDPSocket;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class ReceiveTask extends AsyncTask<String, Object, Object> {

	private static String TAG = ReceiveTask.class.getName();

	private Context mContext;

	public ReceiveTask(Context c) {
		mContext = c;
	}

	@Override
	protected Object doInBackground(String... arg0) {
		SSDPSocket sock = null;

		try {
			Log.d(TAG, "SSDPSocketReceiver : created");

			while (!isCancelled()) {
				Log.d(TAG, "SSDPSocketReceiver : waiting for device share");
				sock = new SSDPSocket();
				DatagramPacket dp = sock.receive();
				Log.d(TAG, "SSDPSocketReceiver : received");
				if (SSDPParser.isSSDPNotifyMsg(dp)) {
					Log.d(TAG, "SSDPSocketReceiver : isSSDPNotifyMsg : " + SSDPParser.getSsdpNt(dp));
					String msgNt = SSDPParser.getSsdpNt(dp);
					if (msgNt != null && msgNt.equals("device")) {
						// Log.d(TAG, "SSDP:LOCATION : " + SSDPParser.getSsdpNt(dp));
					}
				}
				sock.close();
				Log.d(TAG, "SSDPSocket : received --> closed");

				cancel(false);
			}
		} catch (SocketTimeoutException e) {
			Log.d(TAG, "TimeOut. No IP received from STB.");
			return e;
		} catch (IOException e) {
			Log.d(TAG, Log.getStackTraceString(e));
		} finally {
			if (sock != null) {
				sock.close();
				sock = null;
			}
		}
		return null;
	}

	@Override
	protected void onCancelled(Object result) {
		Log.d(TAG, "StbDiscoveryTask : canceled");
	}

	@Override
	protected void onPostExecute(Object result) {
		super.onPostExecute(result);
		if (result instanceof SocketTimeoutException) {
			Toast.makeText(mContext, "No Set-Top Box found. Check your connection/network.", Toast.LENGTH_LONG).show();
		}
		Log.d(TAG, "StbDiscoveryTask : finished");
	}
}
