package fr.cmoatoto.multishare.sender;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.ResolveListener;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class DiscoverHelper implements NsdManager.DiscoveryListener {
	public static final String TAG = "DiscoverAndSend";

	private Context mContext;

	private NsdManager mNsdManager;

	private CandidateHostFoundListener mListener;

	public DiscoverHelper(Context c, CandidateHostFoundListener listener) {
		mContext = c;
		mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
		mNsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, this);
		mListener = listener;
	}

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
		Log.w(TAG, "onServiceFound " + serviceInfo.getServiceName());
		if (serviceInfo.getServiceName().startsWith("MultiShare")) {
			mNsdManager.resolveService(serviceInfo, new ResolveListener() {

				@Override
				public void onServiceResolved(NsdServiceInfo serviceInfo) {
					Log.w(TAG, "SERVICE AT " + serviceInfo.getHost() + " / " + serviceInfo.getPort());
					if (serviceInfo.getHost() == null) {
						return;
					}
					HttpClient client = new DefaultHttpClient();
					HttpGet get = new HttpGet("http:/" + serviceInfo.getHost() + ":" + serviceInfo.getPort() + "/test");
					try {
						client.execute(get);
						mListener.onHostFound("http:/" + serviceInfo.getHost() + ":" + serviceInfo.getPort());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
					Log.e(TAG, "onResolveFailed");
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

	public interface CandidateHostFoundListener {
		public void onHostFound(String destination);
	}

}
