package org.ray.upnp.ssdp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

public class SSDPSocket {

	private static final String TAG = SSDPSocket.class.getName();

	SocketAddress mSSDPMulticastGroup;
	MulticastSocket mLocalSocket;

	NetworkInterface mNetIf;

	public SSDPSocket() throws IOException {
		InetAddress localInAddress = InetAddress.getLocalHost();
		Log.d(TAG, "Local address: " + localInAddress.getHostAddress());

		mSSDPMulticastGroup = new InetSocketAddress(SSDP.ADDRESS, SSDP.PORT);
		mLocalSocket = new MulticastSocket();

		mNetIf = NetworkInterface.getByInetAddress(localInAddress);
		mLocalSocket.joinGroup(mSSDPMulticastGroup, mNetIf);
	}

	/** Used to send SSDP packet */
	public void send(String data) throws IOException {
		DatagramPacket dp = new DatagramPacket(data.getBytes(), data.length(), mSSDPMulticastGroup);

		mLocalSocket.send(dp);

	}

	/** Used to receive SSDP packet */
	public DatagramPacket receive() throws IOException {
		byte[] buf = new byte[1024];
		DatagramPacket dp = new DatagramPacket(buf, buf.length);

		mLocalSocket.receive(dp);
		return dp;
	}

	/** Close the socket */
	public void close() {
		if (mLocalSocket != null) {
			try {
				mLocalSocket.leaveGroup(mSSDPMulticastGroup, mNetIf);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mLocalSocket.close();
		}
	}

	public static void send( final JSONObject json) {

		// Thread which send a SSDP Notify msg
		new Thread(new Runnable() {

			@Override
			public void run() {
				Log.d(TAG, "json sent : " + json);
				sendSsdpNotifyMsg(json);
			}
		}).start();
	}

	/**
	 * Function called to send the SSDP notify msg in JSON format accroding to the user interaction
	 * 
	 * @param notifyMsgJson
	 */
	private static void sendSsdpNotifyMsg(JSONObject notifyMsgJson) {
		SSDPSocket currentSock = null;
		try {
			currentSock = new SSDPSocket();
			if (notifyMsgJson != null) {
				SSDPNotifyMsg infoMsg = new SSDPNotifyMsg(notifyMsgJson.toString(), "device:intentreceiver");
				if (infoMsg != null) {
					Log.d(TAG, "Sending notification : " + infoMsg.toString());
					currentSock.send(infoMsg.toString());
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (currentSock != null) {
				currentSock.close();
				currentSock = null;
			}
		}
	}

}
