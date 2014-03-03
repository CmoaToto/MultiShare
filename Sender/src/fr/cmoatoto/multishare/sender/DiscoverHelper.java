package fr.cmoatoto.multishare.sender;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class DiscoverHelper implements NsdManager.DiscoveryListener, NsdManager.ResolveListener {
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
		Log.w(TAG, "onServiceFound " + serviceInfo.getServiceName() + " / " + serviceInfo.getHost() + " / " + serviceInfo.getPort());
		if ("MultiShare".equals(serviceInfo.getServiceName())) {
			mNsdManager.resolveService(serviceInfo, this);
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

	@Override
	public void onServiceResolved(NsdServiceInfo serviceInfo) {
		Log.w(TAG, "SERVICE AT " + serviceInfo.getHost() + " / " + serviceInfo.getPort());
		if (serviceInfo.getHost() == null) {
			return;
		}
		if ("MultiShare".equals(serviceInfo.getServiceName())) {
			mListener.onHostFound("http:/" + serviceInfo.getHost() + ":" + serviceInfo.getPort());
		}
	}

	@Override
	public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
		// TODO Auto-generated method stub

	}

	public interface CandidateHostFoundListener {
		public void onHostFound(String destination);
	}

}
