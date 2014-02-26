package fr.cmoatoto.multishare.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

public class AndroidUtils {

	public static void showKeyboard(Activity a) {
		((InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
	}

	public static void hideKeyboard(Activity a) {
		((InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(a.getWindow().getDecorView().getWindowToken(), 0);
	}

	public static int dpToPx(Context context, int dp) {
		return (int) ((dp * context.getResources().getDisplayMetrics().density) + 0.5);
	}

	public static int pxToDp(Context context, int px) {
		return (int) ((px / context.getResources().getDisplayMetrics().density) + 0.5);
	}

	/**
	 * Return the filename from a uri.
	 */
	public static String getFilename(Context c, Uri uri) {
		try {
			String scheme = uri.getScheme();
			if (scheme.equals("file")) {
				return uri.getLastPathSegment();
			} else if (scheme.equals("content")) {
				String[] proj = { MediaStore.Files.FileColumns.DISPLAY_NAME };
				Cursor cursor = c.getContentResolver().query(uri, proj, null, null, null);
				if (cursor != null && cursor.getCount() != 0) {
					int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
					cursor.moveToFirst();
					return cursor.getString(columnIndex);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getPath(Context context, Uri uri) {
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			String[] projection = { "_data" };
			Cursor cursor;

			try {
				cursor = context.getContentResolver().query(uri, projection, null, null, null);
				int column_index = cursor.getColumnIndex("_data");
				if (column_index != -1 && cursor.moveToFirst()) {
					String path = cursor.getString(column_index);
					if (path == null) {
						path = getNewTemporaryFilePath(context, uri);
					}
					return path;
				} else {
					return getNewTemporaryFilePath(context, uri);
				}
			} catch (Exception e) {
				return getNewTemporaryFilePath(context, uri);
			}
		} else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	public static String getNewTemporaryFilePath(Context context, Uri uri) {
		File file = getFile(context, uri, true);
		return file == null ? null : file.getPath();
	}

	public static File getFile(Context context, Uri uri, boolean forceCreation) {

		if (!forceCreation && "file".equalsIgnoreCase(uri.getScheme())) {
			return new File(uri.getPath());
		}

		File file = null;

		try {
			File root = context.getFilesDir();
			if (root == null) {
				throw new Exception("data dir not found");
			}
			file = new File(root, getFilename(context, uri));
			file.delete();
			InputStream is = context.getContentResolver().openInputStream(uri);
			OutputStream os = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int cnt = is.read(buf);
			while (cnt > 0) {
				os.write(buf, 0, cnt);
				cnt = is.read(buf);
			}
			os.close();
			is.close();
			file.deleteOnExit();
		} catch (Exception e) {
			Log.e("OpenFile", e.getMessage(), e);
		}
		return file;
	}

	public static String extension(String filename) {
		if (filename == null || filename.length() == 0) {
			return "";
		}
		// Ensure the last dot is after the last file separator
		int lastSep = filename.lastIndexOf(File.separatorChar);
		int lastDot;
		if (lastSep < 0) {
			lastDot = filename.lastIndexOf('.');
		} else {
			lastDot = filename.substring(lastSep + 1).lastIndexOf('.');
			if (lastDot >= 0) {
				lastDot += lastSep + 1;
			}
		}

		if (lastDot >= 0 && lastDot > lastSep) {
			return filename.substring(lastDot + 1).toLowerCase();
		}

		return "";
	}

	public static String getIPAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (useIPv4) {
							if (isIPv4)
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 port suffix
								return delim < 0 ? sAddr : sAddr.substring(0, delim);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
		} // for now eat exceptions
		return "";
	}
}
