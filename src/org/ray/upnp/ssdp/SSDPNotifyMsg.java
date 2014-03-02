package org.ray.upnp.ssdp;

public class SSDPNotifyMsg {

	static final String HOST = "Host:" + SSDP.ADDRESS + ":" + SSDP.PORT;
	static final String MAN = "Man:\"ssdp:discover\"";

	String mLocation; /* Search target */
	String mNT;

	public SSDPNotifyMsg(String location, String ssdp_nt) {
		mLocation = location;
		mNT = ssdp_nt;
	}

	@Override
	public String toString() {
		StringBuilder content = new StringBuilder();

		content.append(SSDP.SL_NOTIFY).append(SSDP.NEWLINE);
		content.append(HOST).append(SSDP.NEWLINE);
		content.append(SSDP.NT + ":" + mNT).append(SSDP.NEWLINE);
		content.append(SSDP.LOCATION + ":" + mLocation).append(SSDP.NEWLINE);
		content.append(SSDP.NTS + ":" + SSDP.NTS_ALIVE).append(SSDP.NEWLINE);
		content.append(SSDP.NEWLINE);

		return content.toString();
	}
}
