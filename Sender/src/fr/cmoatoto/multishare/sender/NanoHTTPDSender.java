package fr.cmoatoto.multishare.sender;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import fr.cmoatoto.multishare.utils.AndroidUtils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

/**
 * A simple, tiny, nicely embeddable HTTP 1.0 (partially 1.1) server in Java
 * 
 * <p>
 * NanoHTTPD version 1.27, Copyright &copy; 2001,2005-2013 Jarno Elonen (elonen@iki.fi, http://iki.fi/elonen/) and Copyright
 * &copy; 2010 Konstantinos Togias (info@ktogias.gr, http://ktogias.gr)
 * 
 * <p>
 * <b>Features + limitations: </b>
 * <ul>
 * 
 * <li>Only one Java file</li>
 * <li>Java 1.1 compatible</li>
 * <li>Released as open source, Modified BSD licence</li>
 * <li>No fixed config files, logging, authorization etc. (Implement yourself if you need them.)</li>
 * <li>Supports parameter parsing of GET and POST methods (+ rudimentary PUT support in 1.25)</li>
 * <li>Supports both dynamic content and file serving</li>
 * <li>Supports file upload (since version 1.2, 2010)</li>
 * <li>Supports partial content (streaming)</li>
 * <li>Supports ETags</li>
 * <li>Never caches anything</li>
 * <li>Doesn't limit bandwidth, request time or simultaneous connections</li>
 * <li>Default code serves files and shows all HTTP parameters and headers</li>
 * <li>File server supports directory listing, index.html and index.htm</li>
 * <li>File server supports partial content (streaming)</li>
 * <li>File server supports ETags</li>
 * <li>File server does the 301 redirection trick for directories without '/'</li>
 * <li>File server supports simple skipping for files (continue download)</li>
 * <li>File server serves also very long files without memory overhead</li>
 * <li>Contains a built-in list of most common mime types</li>
 * <li>All header names are converted lowercase so they don't vary between browsers/clients</li>
 * 
 * </ul>
 * 
 * <p>
 * <b>Ways to use: </b>
 * <ul>
 * 
 * <li>Run as a standalone app, serves files and shows requests</li>
 * <li>Subclass serve() and embed to your own program</li>
 * <li>Call serveFile() from serve() with your own base directory</li>
 * 
 * </ul>
 * 
 * See the end of the source file for distribution license (Modified BSD licence)
 */
public class NanoHTTPDSender {
	// ==================================================
	// API parts
	// ==================================================

	private static final String TAG = NanoHTTPDSender.class.getName();

	private Context mContext;

	public String getLocalhost() {
		return "http://remote_host:" + myServerSocket.getLocalPort() + "/";
	}

	private Hashtable<String, Uri> mCorrespondances = new Hashtable<String, Uri>();

	/**
	 * Override this to customize the server.
	 * <p>
	 * 
	 * (By default, this delegates to serveFile() and allows directory listing.)
	 * 
	 * @param uri
	 *            Percent-decoded URI without parameters, for example "/index.cgi"
	 * @param method
	 *            "GET", "POST" etc.
	 * @param parms
	 *            Parsed, percent decoded parameters from URI and, in case of POST, data.
	 * @param header
	 *            Header entries, percent decoded
	 * @return HTTP response, see class Response for details
	 */
	public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
		myOut.println(method + " '" + uri + "' ");

		Enumeration e = header.propertyNames();
		String range = null;
		while (e.hasMoreElements()) {
			String value = (String) e.nextElement();
			myOut.println("  HDR: '" + value + "' = '" + header.getProperty(value) + "'");
			if ((value.equals("range"))) {
				range = header.getProperty(value);
			}
		}
		e = parms.propertyNames();
		while (e.hasMoreElements()) {
			String value = (String) e.nextElement();
			myOut.println("  PRM: '" + value + "' = '" + parms.getProperty(value) + "'");
		}
		e = files.propertyNames();
		while (e.hasMoreElements()) {
			String value = (String) e.nextElement();
			myOut.println("  UPLOADED: '" + value + "' = '" + files.getProperty(value) + "'");
		}

		final StringBuilder buf = new StringBuilder();
		for (Entry<Object, Object> kv : header.entrySet())
			buf.append(kv.getKey() + " : " + kv.getValue() + "\n");

		// return new NanoHTTPD.Response(HTTP_OK, range, uri);
		return serveFile(uri, header);
	}

	/**
	 * HTTP response. Return one of these from serve().
	 */
	public class Response {
		/**
		 * Default constructor: response = HTTP_OK, data = mime = 'null'
		 */
		public Response() {
			this.status = HTTP_OK;
		}

		/**
		 * Basic constructor.
		 */
		public Response(String status, String mimeType, InputStream data) {
			this.status = status;
			this.mimeType = mimeType;
			this.data = data;
		}

		/**
		 * Convenience method that makes an InputStream out of given text.
		 */
		public Response(String status, String range, String txt) {
			this.status = status;
			this.range = range;
			try {
				if (txt != null && mCorrespondances != null && mCorrespondances.containsKey(txt)) {
					String path = AndroidUtils.getPath(mContext, mCorrespondances.get(txt));
					File file = new File(path);
					this.data = new ByteArrayInputStream(read(file, range));
					myOut.println("Creating response with file " + path);
				}
			} catch (java.io.UnsupportedEncodingException uee) {
				uee.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public byte[] read(File file, String range) throws IOException {

			InputStream ios = null;
			ByteArrayOutputStream ous = null;
			int start = 0, end = -1;
			if (range != null && range.startsWith("bytes=")) {
				range = range.replace("bytes=", "");
				start = range.split("-")[0].length() > 0 ? Integer.valueOf(range.split("-")[0]) : 0;
				end = range.split("-").length > 1 ? Integer.valueOf(range.split("-")[1]) : -1;
				myOut.println("RANGE : starts : " + start + ", end = " + end);
			}

			try {
				byte[] buffer = new byte[4096];
				ous = new ByteArrayOutputStream();
				ios = new FileInputStream(file);
				if (start != -1) {
					ios.read(new byte[start]);
				}
				int read = 0;
				int totalRead = 0;
				while ((read = ios.read(buffer)) != -1) {
					ous.write(buffer, 0, read);
					totalRead += read;
				}
				myOut.println("Total Read : " + totalRead);
			} finally {
				try {
					if (ous != null)
						ous.close();
				} catch (IOException e) {
				}

				try {
					if (ios != null)
						ios.close();
				} catch (IOException e) {
				}
			}
			return ous.toByteArray();
		}

		/**
		 * Adds given line to the header.
		 */
		public void addHeader(String name, String value) {
			header.put(name, value);
		}

		/**
		 * HTTP status code after processing, e.g. "200 OK", HTTP_OK
		 */
		public String status;

		/**
		 * MIME type of content, e.g. "text/html"
		 */
		public String mimeType;

		/**
		 * RANGE bytes range of the answered file
		 */
		public String range;

		/**
		 * Data of the response, may be null.
		 */
		public InputStream data;

		/**
		 * Headers for the HTTP response. Use addHeader() to add lines.
		 */
		public Properties header = new Properties();
	}

	public String addFile(Context c, Uri uri) {
		String path = AndroidUtils.getFilename(c, uri);
		mCorrespondances.put("/" + path, uri);
		return path;
	}

	/**
	 * Some HTTP response status codes
	 */
	public static final String HTTP_OK = "200 OK", HTTP_PARTIALCONTENT = "206 Partial Content", HTTP_RANGE_NOT_SATISFIABLE = "416 Requested Range Not Satisfiable", HTTP_REDIRECT = "301 Moved Permanently", HTTP_NOTMODIFIED = "304 Not Modified",
			HTTP_FORBIDDEN = "403 Forbidden", HTTP_NOTFOUND = "404 Not Found", HTTP_BADREQUEST = "400 Bad Request", HTTP_INTERNALERROR = "500 Internal Server Error", HTTP_NOTIMPLEMENTED = "501 Not Implemented";

	/**
	 * Common mime types for dynamic content
	 */
	public static final String MIME_PLAINTEXT = "text/plain", MIME_HTML = "text/html", MIME_IMAGE = "image/*", MIME_VIDEO = "video/*", MIME_DEFAULT_BINARY = "application/octet-stream", MIME_XML = "text/xml";

	// ==================================================
	// Socket & server code
	// ==================================================

	/**
	 * Starts a HTTP server to given port.
	 * <p>
	 * Throws an IOException if the socket is already in use
	 */
	public NanoHTTPDSender(int port, File wwwroot, Context context) throws IOException {
		this.mContext = context;
		myTcpPort = port;
		this.myRootDir = wwwroot;
		myServerSocket = new ServerSocket(myTcpPort);
		myThread = new Thread(new Runnable() {
			@Override
			public void run() {
				myOut.println("nanoHttpD created. ready to run !");
				try {
					while (true) {
						myOut.println("nanoHttpD waits for an incoming request");
						new HTTPSession(myServerSocket.accept());
					}
				} catch (IOException ioe) {
				}
			}
		});
		myThread.setDaemon(true);
		myThread.start();
	}

	/**
	 * Stops the server.
	 */
	public void stop() {
		try {
			myServerSocket.close();
			myOut.println("nanoHttpD closed !");
			myThread.join();
		} catch (IOException ioe) {
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Handles one session, i.e. parses the HTTP request and returns the response.
	 */
	private class HTTPSession implements Runnable {
		public HTTPSession(Socket s) {
			myOut.println("nanoHttpD received a new socket, HTTPSession starts !");
			mySocket = s;
			Thread t = new Thread(this);
			t.setDaemon(true);
			t.start();
		}

		@Override
		public void run() {
			try {
				InputStream is = mySocket.getInputStream();
				if (is == null)
					return;

				myOut.println("nanoHttpD HttpSession : inputStream not null. Decoding...");

				// Read the first 8192 bytes.
				// The full header should fit in here.
				// Apache's default header limit is 8KB.
				// Do NOT assume that a single read will get the entire header at once!
				final int bufsize = 8192;
				byte[] buf = new byte[bufsize];
				int splitbyte = 0;
				int rlen = 0;
				{
					int read = is.read(buf, 0, bufsize);
					while (read > 0) {
						rlen += read;
						splitbyte = findHeaderEnd(buf, rlen);
						if (splitbyte > 0)
							break;
						read = is.read(buf, rlen, bufsize - rlen);
					}
				}

				// Create a BufferedReader for parsing the header.
				ByteArrayInputStream hbis = new ByteArrayInputStream(buf, 0, rlen);
				BufferedReader hin = new BufferedReader(new InputStreamReader(hbis));
				Properties pre = new Properties();
				Properties parms = new Properties();
				Properties header = new Properties();
				Properties files = new Properties();

				// Decode the header into parms and header java properties
				decodeHeader(hin, pre, parms, header);
				String method = pre.getProperty("method");
				String uri = pre.getProperty("uri");
				myOut.println("nanoHttpD HttpSession : method = " + method + ", uri = " + uri);

				long size = 0x7FFFFFFFFFFFFFFFl;
				String contentLength = header.getProperty("content-length");
				if (contentLength != null) {
					try {
						size = Integer.parseInt(contentLength);
					} catch (NumberFormatException ex) {
					}
				}

				// Write the part of body already read to ByteArrayOutputStream f
				ByteArrayOutputStream f = new ByteArrayOutputStream();
				if (splitbyte < rlen)
					f.write(buf, splitbyte, rlen - splitbyte);

				// While Firefox sends on the first read all the data fitting
				// our buffer, Chrome and Opera send only the headers even if
				// there is data for the body. We do some magic here to find
				// out whether we have already consumed part of body, if we
				// have reached the end of the data to be sent or we should
				// expect the first byte of the body at the next read.
				if (splitbyte < rlen)
					size -= rlen - splitbyte;
				else if (splitbyte == 0 || size == 0x7FFFFFFFFFFFFFFFl)
					size = 0;

				// Now read all the body and write it to f
				buf = new byte[512];
				while (rlen >= 0 && size > 0) {
					rlen = is.read(buf, 0, 512);
					size -= rlen;
					if (rlen > 0)
						f.write(buf, 0, rlen);
				}

				// Get the raw body as a byte []
				byte[] fbuf = f.toByteArray();

				// Create a BufferedReader for easily reading it as string.
				ByteArrayInputStream bin = new ByteArrayInputStream(fbuf);
				BufferedReader in = new BufferedReader(new InputStreamReader(bin));

				// If the method is POST, there may be parameters
				// in data section, too, read it:
				if (method != null && method.equalsIgnoreCase("POST")) {
					String contentType = "";
					String contentTypeHeader = header.getProperty("content-type");
					StringTokenizer st = null;
					if (contentTypeHeader != null) {
						st = new StringTokenizer(contentTypeHeader, "; ");
						if (st.hasMoreTokens()) {
							contentType = st.nextToken();
						}
					}

					if (contentType.equalsIgnoreCase("multipart/form-data")) {
						// Handle multipart/form-data
						if (!st.hasMoreTokens())
							sendError(HTTP_BADREQUEST, "BAD REQUEST: Content type is multipart/form-data but boundary missing. Usage: GET /example/file.html");
						String boundaryExp = st.nextToken();
						st = new StringTokenizer(boundaryExp, "=");
						if (st.countTokens() != 2)
							sendError(HTTP_BADREQUEST, "BAD REQUEST: Content type is multipart/form-data but boundary syntax error. Usage: GET /example/file.html");
						st.nextToken();
						String boundary = st.nextToken();

						decodeMultipartData(boundary, fbuf, in, parms, files);
					} else {
						// Handle application/x-www-form-urlencoded
						String postLine = "";
						char pbuf[] = new char[512];
						int read = in.read(pbuf);
						while (read >= 0 && !postLine.endsWith("\r\n")) {
							postLine += String.valueOf(pbuf, 0, read);
							read = in.read(pbuf);
						}
						postLine = postLine.trim();
						decodeParms(postLine, parms);
					}
				}

				if (method != null && method.equalsIgnoreCase("PUT"))
					files.put("content", saveTmpFile(fbuf, 0, f.size()));

				// Ok, now do the serve()
				Response r = serve(uri, method, header, parms, files);
				if (r == null)
					sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
				else
					sendResponse(r.status, r.mimeType, r.header, r.data);

				in.close();
				is.close();
			} catch (IOException ioe) {
				try {
					sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
				} catch (Throwable t) {
				}
			} catch (InterruptedException ie) {
				// Thrown by sendError, ignore and exit the thread.
			}

			myOut.println("nanoHttpD HttpSession end now.");
		}

		/**
		 * Decodes the sent headers and loads the data into java Properties' key - value pairs
		 **/
		private void decodeHeader(BufferedReader in, Properties pre, Properties parms, Properties header) throws InterruptedException {
			try {
				// Read the request line
				String inLine = in.readLine();
				if (inLine == null)
					return;
				StringTokenizer st = new StringTokenizer(inLine);
				if (!st.hasMoreTokens())
					sendError(HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");

				String method = st.nextToken();
				pre.put("method", method);

				if (!st.hasMoreTokens())
					sendError(HTTP_BADREQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");

				String uri = st.nextToken();

				// Decode parameters from the URI
				int qmi = uri.indexOf('?');
				if (qmi >= 0) {
					decodeParms(uri.substring(qmi + 1), parms);
					uri = decodePercent(uri.substring(0, qmi));
				} else
					uri = decodePercent(uri);

				// If there's another token, it's protocol version,
				// followed by HTTP headers. Ignore version but parse headers.
				// NOTE: this now forces header names lowercase since they are
				// case insensitive and vary by client.
				if (st.hasMoreTokens()) {
					String line = in.readLine();
					while (line != null && line.trim().length() > 0) {
						int p = line.indexOf(':');
						if (p >= 0)
							header.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
						line = in.readLine();
					}
				}

				pre.put("uri", uri);
			} catch (IOException ioe) {
				sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
			}
		}

		/**
		 * Decodes the Multipart Body data and put it into java Properties' key - value pairs.
		 **/
		private void decodeMultipartData(String boundary, byte[] fbuf, BufferedReader in, Properties parms, Properties files) throws InterruptedException {
			try {
				int[] bpositions = getBoundaryPositions(fbuf, boundary.getBytes());
				int boundarycount = 1;
				String mpline = in.readLine();
				while (mpline != null) {
					if (mpline.indexOf(boundary) == -1)
						sendError(HTTP_BADREQUEST, "BAD REQUEST: Content type is multipart/form-data but next chunk does not start with boundary. Usage: GET /example/file.html");
					boundarycount++;
					Properties item = new Properties();
					mpline = in.readLine();
					while (mpline != null && mpline.trim().length() > 0) {
						int p = mpline.indexOf(':');
						if (p != -1)
							item.put(mpline.substring(0, p).trim().toLowerCase(), mpline.substring(p + 1).trim());
						mpline = in.readLine();
					}
					if (mpline != null) {
						String contentDisposition = item.getProperty("content-disposition");
						if (contentDisposition == null) {
							sendError(HTTP_BADREQUEST, "BAD REQUEST: Content type is multipart/form-data but no content-disposition info found. Usage: GET /example/file.html");
						}
						StringTokenizer st = new StringTokenizer(contentDisposition, ";");
						Properties disposition = new Properties();
						while (st.hasMoreTokens()) {
							String token = st.nextToken().trim();
							int p = token.indexOf('=');
							if (p != -1)
								disposition.put(token.substring(0, p).trim().toLowerCase(), token.substring(p + 1).trim());
						}
						String pname = disposition.getProperty("name");
						pname = pname.substring(1, pname.length() - 1);

						String value = "";
						if (item.getProperty("content-type") == null) {
							while (mpline != null && mpline.indexOf(boundary) == -1) {
								mpline = in.readLine();
								if (mpline != null) {
									int d = mpline.indexOf(boundary);
									if (d == -1)
										value += mpline;
									else
										value += mpline.substring(0, d - 2);
								}
							}
						} else {
							if (boundarycount > bpositions.length)
								sendError(HTTP_INTERNALERROR, "Error processing request");
							int offset = stripMultipartHeaders(fbuf, bpositions[boundarycount - 2]);
							String path = saveTmpFile(fbuf, offset, bpositions[boundarycount - 1] - offset - 4);
							files.put(pname, path);
							value = disposition.getProperty("filename");
							value = value.substring(1, value.length() - 1);
							do {
								mpline = in.readLine();
							} while (mpline != null && mpline.indexOf(boundary) == -1);
						}
						parms.put(pname, value);
					}
				}
			} catch (IOException ioe) {
				sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
			}
		}

		/**
		 * Find byte index separating header from body. It must be the last byte of the first two sequential new lines.
		 **/
		private int findHeaderEnd(final byte[] buf, int rlen) {
			int splitbyte = 0;
			while (splitbyte + 3 < rlen) {
				if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n')
					return splitbyte + 4;
				splitbyte++;
			}
			return 0;
		}

		/**
		 * Find the byte positions where multipart boundaries start.
		 **/
		public int[] getBoundaryPositions(byte[] b, byte[] boundary) {
			int matchcount = 0;
			int matchbyte = -1;
			Vector matchbytes = new Vector();
			for (int i = 0; i < b.length; i++) {
				if (b[i] == boundary[matchcount]) {
					if (matchcount == 0)
						matchbyte = i;
					matchcount++;
					if (matchcount == boundary.length) {
						matchbytes.addElement(new Integer(matchbyte));
						matchcount = 0;
						matchbyte = -1;
					}
				} else {
					i -= matchcount;
					matchcount = 0;
					matchbyte = -1;
				}
			}
			int[] ret = new int[matchbytes.size()];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = ((Integer) matchbytes.elementAt(i)).intValue();
			}
			return ret;
		}

		/**
		 * Retrieves the content of a sent file and saves it to a temporary file. The full path to the saved file is
		 * returned.
		 **/
		private String saveTmpFile(byte[] b, int offset, int len) {
			String path = "";
			if (len > 0) {
				String tmpdir = System.getProperty("java.io.tmpdir");
				try {
					File temp = File.createTempFile("NanoHTTPD", "", new File(tmpdir));
					OutputStream fstream = new FileOutputStream(temp);
					fstream.write(b, offset, len);
					fstream.close();
					path = temp.getAbsolutePath();
				} catch (Exception e) { // Catch exception if any
					myErr.println("Error: " + e.getMessage());
				}
			}
			return path;
		}

		/**
		 * It returns the offset separating multipart file headers from the file's data.
		 **/
		private int stripMultipartHeaders(byte[] b, int offset) {
			int i = 0;
			for (i = offset; i < b.length; i++) {
				if (b[i] == '\r' && b[++i] == '\n' && b[++i] == '\r' && b[++i] == '\n')
					break;
			}
			return i + 1;
		}

		/**
		 * Decodes the percent encoding scheme. <br/>
		 * For example: "an+example%20string" -> "an example string"
		 */
		private String decodePercent(String str) throws InterruptedException {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				for (int i = 0; i < str.length(); i++) {
					char c = str.charAt(i);
					switch (c) {
					case '+':
						baos.write((int) ' ');
						break;
					case '%':
						baos.write(Integer.parseInt(str.substring(i + 1, i + 3), 16));
						i += 2;
						break;
					default:
						baos.write((int) c);
						break;
					}
				}

				return new String(baos.toByteArray(), "UTF-8");
			} catch (Exception e) {
				sendError(HTTP_BADREQUEST, "BAD REQUEST: Bad percent-encoding.");
				return null;
			}
		}

		/**
		 * Decodes parameters in percent-encoded URI-format ( e.g. "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them
		 * to given Properties. NOTE: this doesn't support multiple identical keys due to the simplicity of Properties -- if
		 * you need multiples, you might want to replace the Properties with a Hashtable of Vectors or such.
		 */
		private void decodeParms(String parms, Properties p) throws InterruptedException {
			if (parms == null)
				return;

			StringTokenizer st = new StringTokenizer(parms, "&");
			while (st.hasMoreTokens()) {
				String e = st.nextToken();
				int sep = e.indexOf('=');
				if (sep >= 0)
					p.put(decodePercent(e.substring(0, sep)).trim(), decodePercent(e.substring(sep + 1)));
				else
					p.put(decodePercent(e).trim(), "");
			}
		}

		/**
		 * Returns an error message as a HTTP response and throws InterruptedException to stop further request processing.
		 */
		private void sendError(String status, String msg) throws InterruptedException {
			sendResponse(status, MIME_PLAINTEXT, null, new ByteArrayInputStream(msg.getBytes()));
			throw new InterruptedException();
		}

		/**
		 * Sends given response to the socket.
		 */
		private void sendResponse(String status, String mime, Properties header, InputStream data) {
			try {
				if (status == null)
					throw new Error("sendResponse(): Status can't be null.");

				OutputStream out = mySocket.getOutputStream();
				PrintWriter pw = new PrintWriter(out);
				pw.print("HTTP/1.0 " + status + " \r\n");

				if (mime != null)
					pw.print("Content-Type: " + mime + "\r\n");

				if (header == null || header.getProperty("Date") == null)
					pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");

				if (header != null) {
					Enumeration e = header.keys();
					while (e.hasMoreElements()) {
						String key = (String) e.nextElement();
						String value = header.getProperty(key);
						pw.print(key + ": " + value + "\r\n");
					}
				}

				pw.print("\r\n");
				pw.flush();

				if (data != null) {
					int pending = data.available(); // This is to support partial sends, see serveFile()
					byte[] buff = new byte[theBufferSize];
					while (pending > 0) {
						int read = data.read(buff, 0, ((pending > theBufferSize) ? theBufferSize : pending));
						if (read <= 0)
							break;
						out.write(buff, 0, read);
						pending -= read;
					}
				}
				out.flush();
				out.close();
				if (data != null)
					data.close();
				myOut.println("Data sent.");
			} catch (IOException ioe) {
				// Couldn't write? No can do.
				try {
					mySocket.close();
				} catch (Throwable t) {
				}
			}
		}

		private Socket mySocket;
	}

	/**
	 * URL-encodes everything between "/"-characters. Encodes spaces as '%20' instead of '+'.
	 */
	private String encodeUri(String uri) {
		String newUri = "";
		StringTokenizer st = new StringTokenizer(uri, "/ ", true);
		while (st.hasMoreTokens()) {
			String tok = st.nextToken();
			if (tok.equals("/"))
				newUri += "/";
			else if (tok.equals(" "))
				newUri += "%20";
			else {
				newUri += URLEncoder.encode(tok);
				// For Java 1.4 you'll want to use this instead:
				// try { newUri += URLEncoder.encode( tok, "UTF-8" ); } catch ( java.io.UnsupportedEncodingException uee ) {}
			}
		}
		return newUri;
	}

	private int myTcpPort;
	private final ServerSocket myServerSocket;
	private Thread myThread;
	private File myRootDir;

	// ==================================================
	// File server code
	// ==================================================

	/**
	 * Serves file from homeDir and its' subdirectories (only). Uses only URI, ignores all headers and HTTP parameters.
	 */
	public Response serveFile(String uri, Properties header) {
		Response res = null;

		// Remove URL arguments
		uri = uri.trim().replace(File.separatorChar, '/');
		if (uri.indexOf('?') >= 0)
			uri = uri.substring(0, uri.indexOf('?'));

		// Prohibit getting out of current directory
		if (uri.startsWith("..") || uri.endsWith("..") || uri.indexOf("../") >= 0)
			res = new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT, "FORBIDDEN: Won't serve ../ for security reasons.");

		if (mCorrespondances != null && mCorrespondances.containsKey(uri)) {
			uri = AndroidUtils.getPath(mContext, mCorrespondances.get(uri));
			myOut.println("Creating response with file " + uri);
		}

		File f = new File(uri);
		if (res == null && !f.exists())
			res = new Response(HTTP_NOTFOUND, MIME_PLAINTEXT, "Error 404, file not found.");

		// List the directory, if necessary
		if (res == null && f.isDirectory()) {
			res = new Response(HTTP_NOTFOUND, MIME_PLAINTEXT, "Error 404, file not found.");
		}

		try {
			if (res == null) {
				// Get MIME type from file name extension, if possible
				String mime = null;
				int dot = f.getCanonicalPath().lastIndexOf('.');
				if (dot >= 0)
					mime = (String) theMimeTypes.get(f.getCanonicalPath().substring(dot + 1).toLowerCase());
				if (mime == null)
					mime = MIME_DEFAULT_BINARY;

				// Calculate etag
				String etag = Integer.toHexString((f.getAbsolutePath() + f.lastModified() + "" + f.length()).hashCode());

				// Support (simple) skipping:
				long startFrom = 0;
				long endAt = -1;
				String range = header.getProperty("range");
				if (range != null) {
					if (range.startsWith("bytes=")) {
						range = range.substring("bytes=".length());
						int minus = range.indexOf('-');
						try {
							if (minus > 0) {
								startFrom = Long.parseLong(range.substring(0, minus));
								endAt = Long.parseLong(range.substring(minus + 1));
							}
						} catch (NumberFormatException nfe) {
						}
					}
				}

				// Change return code and add Content-Range header when skipping is requested
				long fileLen = f.length();
				if (range != null && startFrom >= 0) {
					if (startFrom >= fileLen) {
						res = new Response(HTTP_RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "");
						res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
						if (mime.startsWith("application/"))
							res.addHeader("Content-Disposition", "attachment; filename=\"" + f.getName() + "\"");
						res.addHeader("ETag", etag);
					} else {
						if (endAt < 0)
							endAt = fileLen - 1;
						long newLen = endAt - startFrom + 1;
						if (newLen < 0)
							newLen = 0;

						final long dataLen = newLen;
						FileInputStream fis = new FileInputStream(f) {
							public int available() throws IOException {
								return (int) dataLen;
							}
						};
						fis.skip(startFrom);

						res = new Response(HTTP_PARTIALCONTENT, mime, fis);
						res.addHeader("Content-Length", "" + dataLen);
						res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
						if (mime.startsWith("application/"))
							res.addHeader("Content-Disposition", "attachment; filename=\"" + f.getName() + "\"");
						res.addHeader("ETag", etag);
					}
				} else {
					if (etag.equals(header.getProperty("if-none-match")))
						res = new Response(HTTP_NOTMODIFIED, mime, "");
					else {
						res = new Response(HTTP_OK, mime, new FileInputStream(f));
						res.addHeader("Content-Length", "" + fileLen);
						if (mime.startsWith("application/"))
							res.addHeader("Content-Disposition", "attachment; filename=\"" + f.getName() + "\"");
						res.addHeader("ETag", etag);
					}
				}
			}
		} catch (IOException ioe) {
			res = new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
		}

		res.addHeader("Accept-Ranges", "bytes"); // Announce that the file server accepts partial content requestes

		myOut.println("Send response with Status : '" + res.status + "', MimeType : '" + res.mimeType + "', and Header = '" + res.header + "'");
		return res;
	}

	/**
	 * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
	 */
	public static Hashtable theMimeTypes = new Hashtable();
	static {
		StringTokenizer st = new StringTokenizer("3gp		video/3gp " + "acx		application/internet-property-stream " + "ai			application/postscript " + "aif		audio/x-aiff " + "aifc		audio/x-aiff " + "aiff		audio/x-aiff " + "asf		video/x-ms-asf "
				+ "asr		video/x-ms-asf " + "asx		video/x-ms-asf " + "au			audio/basic " + "avi		video/x-msvideo " + "axs		application/olescript " + "bas		text/plain " + "bcpio		application/x-bcpio " + "bin		application/octet-stream "
				+ "bmp		image/bmp " + "c			text/plain " + "cat		application/vnd.ms-pkiseccat " + "cdf		application/x-cdf " + "cdf		application/x-netcdf " + "cer		application/x-x509-ca-cert " + "class		application/octet-stream "
				+ "clp		application/x-msclip " + "cmx		image/x-cmx " + "cod		image/cis-cod " + "cpio		application/x-cpio " + "crd		application/x-mscardfile " + "crl		application/pkix-crl " + "crt		application/x-x509-ca-cert "
				+ "csh		application/x-csh " + "css		text/css " + "dcr		application/x-director " + "der		application/x-x509-ca-cert " + "dir		application/x-director " + "dll		application/x-msdownload " + "dms		application/octet-stream "
				+ "doc		application/msword " + "docx		application/msword " + "dot		application/msword " + "dvi		application/x-dvi " + "dxr		application/x-director " + "eps		application/postscript " + "etx		text/x-setext " + "evy		application/envoy "
				+ "exe		application/octet-stream " + "fif		application/fractals " + "flr		x-world/x-vrml " + "gif		image/gif " + "gtar		application/x-gtar " + "gz			application/x-gzip " + "h			text/plain " + "hdf		application/x-hdf "
				+ "hlp		application/winhlp " + "hqx		application/mac-binhex40 " + "hta		application/hta " + "htc		text/x-component " + "htm		text/html " + "html		text/html " + "htt		text/webviewhtml " + "ico		image/x-icon " + "ief		image/ief "
				+ "iii		application/x-iphone " + "ins		application/x-internet-signup " + "isp		application/x-internet-signup " + "jfif		image/pipeg " + "jpe		image/jpeg " + "jpeg		image/jpeg " + "jpg		image/jpeg " + "js			application/x-javascript "
				+ "latex		application/x-latex " + "lha		application/octet-stream " + "lsf		video/x-la-asf " + "lsx		video/x-la-asf " + "lzh		application/octet-stream " + "m13		application/x-msmediaview " + "m14		application/x-msmediaview "
				+ "m3u		audio/x-mpegurl " + "man		application/x-troff-man " + "mdb		application/x-msaccess " + "me			application/x-troff-me " + "mht		message/rfc822 " + "mhtml		message/rfc822 " + "mid		audio/mid " + "mny		application/x-msmoney "
				+ "mov		video/quicktime " + "movie		video/x-sgi-movie " + "mp2		video/mpeg " + "mp3		audio/mpeg " + "mp4		video/mp4 " + "mpa		video/mpeg " + "mpe		video/mpeg " + "mpeg		video/mpeg " + "mpg		video/mpeg "
				+ "mpp		application/vnd.ms-project " + "mppx		application/vnd.ms-project " + "mpv2		video/mpeg " + "ms			application/x-troff-ms " + "msg		application/vnd.ms-outlook " + "mvb		application/x-msmediaview " + "nc			application/x-netcdf "
				+ "nws		message/rfc822 " + "oda		application/oda " + "p10		application/pkcs10 " + "p12		application/x-pkcs12 " + "p7b		application/x-pkcs7-certificates " + "p7c		application/x-pkcs7-mime " + "p7m		application/x-pkcs7-mime "
				+ "p7r		application/x-pkcs7-certreqresp " + "p7s		application/x-pkcs7-signature " + "pbm		image/x-portable-bitmap " + "pdf		application/pdf " + "pfx		application/x-pkcs12 " + "pgm		image/x-portable-graymap "
				+ "pko		application/ynd.ms-pkipko " + "pma		application/x-perfmon " + "pmc		application/x-perfmon " + "pml		application/x-perfmon " + "pmr		application/x-perfmon " + "pmw		application/x-perfmon " + "png		image/png "
				+ "pnm		image/x-portable-anymap " + "pot		application/vnd.ms-powerpoint " + "ppm		image/x-portable-pixmap " + "pps		application/vnd.ms-powerpoint " + "ppsx		application/vnd.ms-powerpoint " + "ppt		application/vnd.ms-powerpoint "
				+ "pptx		application/vnd.ms-powerpoint " + "prf		application/pics-rules " + "ps			application/postscript " + "pub		application/x-mspublisher " + "qt			video/quicktime " + "ra			audio/x-pn-realaudio " + "ram		audio/x-pn-realaudio "
				+ "ras		image/x-cmu-raster " + "rgb		image/x-rgb " + "rmi		audio/mid " + "roff		application/x-troff " + "rtf		application/rtf " + "rtx		text/richtext " + "scd		application/x-msschedule " + "sct		text/scriptlet "
				+ "setpay		application/set-payment-initiation " + "setreg		application/set-registration-initiation " + "sh			application/x-sh " + "shar		application/x-shar " + "sit		application/x-stuffit " + "snd		audio/basic "
				+ "spc		application/x-pkcs7-certificates " + "spl		application/futuresplash " + "src		application/x-wais-source " + "sst		application/vnd.ms-pkicertstore " + "stl		application/vnd.ms-pkistl " + "stm		text/html "
				+ "sv4cpio	application/x-sv4cpio " + "sv4crc		application/x-sv4crc " + "svg		image/svg+xml " + "swf		application/x-shockwave-flash " + "t			application/x-troff " + "tar		application/x-tar " + "tcl		application/x-tcl "
				+ "tex		application/x-tex " + "texi		application/x-texinfo " + "texinfo	application/x-texinfo " + "tgz		application/x-compressed " + "tif		image/tiff " + "tiff		image/tiff " + "tr			application/x-troff "
				+ "trm		application/x-msterminal " + "tsv		text/tab-separated-values " + "txt		text/plain " + "uls		text/iuls " + "ustar		application/x-ustar " + "vcf		text/x-vcard " + "vrml		x-world/x-vrml " + "wav		audio/x-wav "
				+ "wcm		application/vnd.ms-works " + "wdb		application/vnd.ms-works " + "wks		application/vnd.ms-works " + "wmf		application/x-msmetafile " + "wps		application/vnd.ms-works " + "wri		application/x-mswrite " + "wrl		x-world/x-vrml "
				+ "wrz		x-world/x-vrml " + "xaf		x-world/x-vrml " + "xbm		image/x-xbitmap " + "xla		application/vnd.ms-excel " + "xlc		application/vnd.ms-excel " + "xlm		application/vnd.ms-excel " + "xls		application/vnd.ms-excel "
				+ "xlsx		application/vnd.ms-excel " + "xlt		application/vnd.ms-excel " + "xlw		application/vnd.ms-excel " + "xml		text/plain " + "xof		x-world/x-vrml " + "xpm		image/x-xpixmap " + "xwd		image/x-xwindowdump "
				+ "z			application/x-compress " + "zip		application/zip");
		while (st.hasMoreTokens())
			theMimeTypes.put(st.nextToken(), st.nextToken());
	}

	private static int theBufferSize = 16 * 1024;

	// Change these if you want to log to somewhere else than stdout
	private static class MainOut {
		public void println(String txt) {
			Log.v(TAG, txt);
		}
	}

	protected static MainOut myOut = new MainOut();
	protected static PrintStream myErr = System.err;

	/**
	 * GMT date formatter
	 */
	private static java.text.SimpleDateFormat gmtFrmt;
	static {
		gmtFrmt = new java.text.SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	/**
	 * The distribution licence
	 */
	private static final String LICENCE = "Copyright (C) 2001,2005-2013 by Jarno Elonen <elonen@iki.fi>\n" + "and Copyright (C) 2010 by Konstantinos Togias <info@ktogias.gr>\n" + "\n"
			+ "Redistribution and use in source and binary forms, with or without\n" + "modification, are permitted provided that the following conditions\n" + "are met:\n" + "\n"
			+ "Redistributions of source code must retain the above copyright notice,\n" + "this list of conditions and the following disclaimer. Redistributions in\n" + "binary form must reproduce the above copyright notice, this list of\n"
			+ "conditions and the following disclaimer in the documentation and/or other\n" + "materials provided with the distribution. The name of the author may not\n"
			+ "be used to endorse or promote products derived from this software without\n" + "specific prior written permission. \n" + " \n" + "THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR\n"
			+ "IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES\n" + "OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.\n" + "IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,\n"
			+ "INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT\n" + "NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,\n" + "DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY\n"
			+ "THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n" + "(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n" + "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";
}
