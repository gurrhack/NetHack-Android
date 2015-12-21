package com.tbd.NetHack.Hearse;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.Toast;

import com.tbd.NetHack.Log;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class communicates with the Hearse server and provides all Hearse functionality.
 * @author Ranbato
 */
public class Hearse implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String CLIENT_NAME = "Gurr Android Nethack";
	private static final String CLIENT_VERSION = "3.6.0";
	private static final String CLIENT_ID = CLIENT_NAME + " " + CLIENT_VERSION;
	private static final String HEARSE_CRC = getStringMD5(CLIENT_ID);
	private static final String HOST = "hearse.krollmark.com";
	private static final String BASE_URL = "http://hearse.krollmark.com/bones.dll?act=";
	// hearse commands
	private static final String NEW_USER = "newuser";
	private static final String UPLOAD = "upload";
	private static final String DOWNLOAD = "download";
	private static final String BONES_CHECK = "bonescheck";
	private static final String UPDATE_USER = "changeuserinfo";
	private static final String HEADER_TOKEN = "X_USERTOKEN";
	private static final String HEADER_EMAIL = "X_USEREMAIL";
	private static final String HEADER_NICK = "X_USERNICK";
	private static final String HEADER_HEARSE_CRC = "X_HEARSECRC";
	//    private static final String HEADER_VERSION = "X_VER";
//    # 1 = incarnation = major, minor, patchlevel, editlevel
//    # 2 = feature set
//    # 3 = entity count
//    # 4 = struct sizes = flag, obj, monst, you
//    private static final int HEADER_VERSION_COUNT = 4;
	private static final String HEADER_BONES_CRC = "X_BONESCRC";
	private static final String HEADER_VERSIONCRC = "X_VERSIONCRC";
	private static final String HEADER_FILE_NAME = "X_FILENAME";
	private static final String HEADER_USER_LEVELS = "X_USERLEVELS";
	private static final String HEADER_NETHACKVER = "X_NETHACKVER";
	private static final String HEADER_FORCEDOWNLOAD = "X_FORCEDOWNLOAD";
	/**
	 * Not implemented
	 */
	private static final String HEADER_FORCE_UPDATE = "X_FORCEUPDATE";
	/**
	 * Not implemented.
	 */
	private static final String HEADER_MATCHBONES = "X_MATCHBONES";
	private static final String HEADER_WANTS_INFO = "X_GIVEINFO";
	private static final String HEADER_MOTD = "X_MOTD";
	private static final String HEADER_CLIENT = "X_CLIENTID";
	private static final String HEADER_HEARSE = "X-HEARSE";
	private static final String HEADER_ERROR = "X_ERROR";
	private static final String F_ERROR_FATAL = "FATAL";
	private static final String F_ERROR_INFO = "INFO";
	/**
	 * bon<dungeon code><0 | role code>.<level boneid | level number>
	 * XXX case_tolerant if appropriate
	 */
	private static final Pattern PATTERN = Pattern.compile("^bon[A-Z](0|(Arc|Bar|Cav|Hea|Kni|Mon|Pri|Rog|Ran|Sam|Tou|Val|Wiz))\\.([A-Z]|\\d+)\\z", Pattern.CASE_INSENSITIVE);
	private static final String TAG = "MD5";
	private static final String PREFS_HEARSE_ID = "hearseID";
	private static final String PREFS_HEARSE_MAIL = "hearseMail";
	private static final String PREFS_HEARSE_NAME = "hearseName";
	private static final String PREFS_HEARSE_KEEP_UPLOADED = "hearseKeepUploaded";
	private static final String PREFS_HEARSE_ENABLE = "hearseEnable";
	private static final String PREFS_HEARSE_UPDATE_USER = "hearseUpdateUser";
	private static final String PREFS_HEARSE_LAST_UPLOAD = "hearseLastUpload";
	private final Activity context;
	private final SharedPreferences prefs;
	private final String dataDirString;
	private final String userNick;
	private final String userEmail;
	private String userToken;
	private final boolean keepUploaded;
	private long lastUpload;
	private final HttpClient httpClient;

	/**
	 * Creates a new instance of Hearse
	 *
	 * @param context The {@link android.app.Activity} application for access to {@link Activity}, through which it can
	 *            create {@link Toast} notifications, etc.
	 * @param prefs SharedPreferences
	 * @param path nethack datadir
	 */
	public Hearse(Activity context, SharedPreferences prefs, String path) {

		this.context = context;
		dataDirString = path;
		this.prefs = prefs;

		userToken = prefs.getString(PREFS_HEARSE_ID, "");
		userEmail = prefs.getString(PREFS_HEARSE_MAIL, "");
		userNick = prefs.getString(PREFS_HEARSE_NAME, "");
		keepUploaded = prefs.getBoolean(PREFS_HEARSE_KEEP_UPLOADED, false);
		lastUpload = prefs.getLong(PREFS_HEARSE_LAST_UPLOAD, 0);
		httpClient = new DefaultHttpClient();

		prefs.registerOnSharedPreferenceChangeListener(this);

		if(prefs.getBoolean(PREFS_HEARSE_ENABLE, false)) {
			hearseThread.start();
		}
	}

	private static boolean checkMD5(String md5, File updateFile) {
		if (TextUtils.isEmpty(md5) || updateFile == null) {
			return false;
		}

		String calculatedDigest = getFileMD5(updateFile);
		if (calculatedDigest == null) {
			return false;
		}
		return calculatedDigest.equalsIgnoreCase(md5);
	}

	private static String getByteMD5(byte[] bytesOfMessage) {

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			Log.print(e.toString());
			return null;
		}
		byte[] md5sum = md.digest(bytesOfMessage);
		BigInteger bigInt = new BigInteger(1, md5sum);
		String output = bigInt.toString(16);
		// Fill to 32 chars
		output = String.format("%32s", output).replace(' ', '0');
		return output;
	}

	private static String getStringMD5(String input) {
		byte[] bytesOfMessage;
		try {
			bytesOfMessage = input.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.print(e.toString());
			return null;
		}

		return getByteMD5(bytesOfMessage);
	}

	private static String getFileMD5(File updateFile) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}

		BufferedInputStream is;
		try {
			is = new BufferedInputStream(new FileInputStream(updateFile));
		} catch (FileNotFoundException e) {
			Log.print("Exception while getting FileInputStream" + e);
			return null;
		}

		byte[] buffer = new byte[8192];
		int read;
		try {
			while ((read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			String output = bigInt.toString(16);
			// Fill to 32 chars
			output = String.format("%32s", output).replace(' ', '0');
			return output;
		} catch (IOException e) {
			throw new RuntimeException("Unable to process file for MD5", e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				Log.print(TAG + " Exception on closing MD5 input stream " + e);
			}
		}
	}

	/**
	 * Collects all of the files that match bones file name pattern.
	 *
	 * @return array of all bones files
	 */
	private File[] enumerateBones() {

		File dataDir = new File(dataDirString);
		File[] bones = dataDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return isValidBonesFileName(filename);
			}
		});
		return bones == null ? new File[0] : bones;
	}

	/**
	 * The main method.  Registers new user and uploads and downloads bones files.
	 * Originally called drive() when single threaded. :)
	 *
	 */
	private Thread hearseThread = new Thread() {
		@Override
		public void run() {

			if(isHearseReachable()) {

				boolean newUser = false;
				if(userToken.length() == 0) {
					if(userEmail.length() > 0) {
						userToken = createNewUser();
						newUser = true;
					} else {
						showEmailRequired();
					}
				} else {
					Log.print("using existing token " + userToken);

					// Check if userNick information has changed and update.
					if(prefs.getBoolean(PREFS_HEARSE_UPDATE_USER, false)) {
						if(userEmail.length() > 0) {
							changeUserInfo();
						} else {
							showEmailRequired();
						}
					}
				}

				if(prefs.contains(PREFS_HEARSE_UPDATE_USER))
					prefs.edit().remove(PREFS_HEARSE_UPDATE_USER).commit();

				if(userToken.length() > 0) {
					int nUp = uploadBones();
					int nDown = 0;
					if(nUp > 0) {
						nDown = downloadBones();
					}
					Log.print("Hearse uploaded " + nUp + ", downloaded " + nDown);

					if(nUp > 0 || nDown > 0) {
						updateLastUpload();
					}
				}
			} else {
				Log.print("Hearse not reachable");
			}

		}
	};

	private void updateLastUpload() {

		for(File bones : enumerateBones()) {
			long lastModified = bones.lastModified();
			if(lastModified > lastUpload)
				lastUpload = lastModified;
		}
		prefs.edit().putLong(PREFS_HEARSE_LAST_UPLOAD, lastUpload).commit();
	}

	private void showEmailRequired() {
		showToast("Hearse requires an Email address to register");
	}

	private void showToast(final String message) {
		context.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.print(message);
				Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
			}
		});
	}

	private String createNewUser() {
		List<Header> headerList = new ArrayList<Header>();


		headerList.add(new BasicHeader(HEADER_TOKEN, userEmail));

		headerList.add(new BasicHeader(HEADER_NICK, userNick));

		HttpResponse resp = doGet(BASE_URL, NEW_USER, headerList);

		if (resp.getFirstHeader(HEADER_HEARSE) == null) {
			consumeContent(resp);
			return "";
		}
		if (resp.getFirstHeader(HEADER_ERROR) != null) {

			HttpEntity e = resp.getEntity();
			BufferedReader in;
			try {
				in = new BufferedReader(new InputStreamReader(e.getContent()));

				String line;
				while ((line = in.readLine()) != null) {
					Log.print(line); //@todo output this to screen
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			consumeContent(resp);
			return "";

		}

		Header tokenHeader = resp.getFirstHeader(HEADER_TOKEN);
		SharedPreferences.Editor ed = prefs.edit();
		ed.putString(PREFS_HEARSE_ID, tokenHeader.getValue());
		ed.commit();
		consumeContent(resp);
		return tokenHeader.getValue();
	}

	private int downloadBones() {

		int nDownloaded = 0;
		String hackver = prefs.getString(HEADER_NETHACKVER, "54"); // 54 is us, 26 is Windows 3.4.3

		StringBuilder builder = new StringBuilder();
		for(File bones : enumerateBones()) {
			builder.append(bones.getName());
			builder.append(',');
		}
		String existingBonesSet = builder.toString();

		while (true) {

			List<Header> headerList = new ArrayList<Header>();

			headerList.add(new BasicHeader(HEADER_TOKEN, userToken));
			if(existingBonesSet.length() > 0)
				headerList.add(new BasicHeader(HEADER_USER_LEVELS, existingBonesSet));
			//isEmpty requires API 9
			if (!"".equals(hackver)) {
				headerList.add(new BasicHeader(HEADER_NETHACKVER, hackver));
			}

			HttpResponse resp = doGet(BASE_URL, DOWNLOAD, headerList);

			if (resp.getFirstHeader(HEADER_HEARSE) == null) {
				consumeContent(resp);
				return 0;
			}
			Header header = resp.getFirstHeader(HEADER_ERROR);
			if (header != null) {

				if (header.getValue().equals(F_ERROR_INFO)) {
					// This is a warning so pretend we succeeded.
					HttpEntity e = resp.getEntity();
					BufferedReader in;
					try {
						in = new BufferedReader(new InputStreamReader(e.getContent()));

						String line;
						while ((line = in.readLine()) != null) {
							Log.print(line); //@todo output this to screen

						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} else {
					HttpEntity e = resp.getEntity();
					BufferedReader in;
					try {
						in = new BufferedReader(new InputStreamReader(e.getContent()));

						String line;
						while ((line = in.readLine()) != null) {
							Log.print(line); //@todo output this to screen

						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}

				}
				consumeContent(resp);
				break;
			} else {

				Header fileName = resp.getFirstHeader(HEADER_FILE_NAME);
				Header md5 = resp.getFirstHeader(HEADER_BONES_CRC);

				File bonesFile = new File(dataDirString, fileName.getValue());
				// For thread safety, don't download as real name.  Nethack might try to load it before complete
				File tmpBonesFile = new File(dataDirString, bonesFile.getName() + ".tmp");

				BufferedOutputStream out = null;
				InputStream in = null;
				try {
					tmpBonesFile.createNewFile();
					in = resp.getEntity().getContent();
					out = new BufferedOutputStream(new FileOutputStream(tmpBonesFile));
					int c;
					while ((c = in.read()) != -1) {
						out.write(c);
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					// I miss try-with-resources :)
					if (out != null) {
						try {
							out.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

				String md5Check = getFileMD5(tmpBonesFile);
				if (md5Check.equals(md5.getValue())) {
					tmpBonesFile.renameTo(bonesFile);
					Log.print("Downloaded " + bonesFile.getName());
					existingBonesSet = existingBonesSet + bonesFile.getName() + ",";
					nDownloaded++;
				} else {
					//arg
					Log.print("Bad bones downloaded");
					tmpBonesFile.delete();
				}

				consumeContent(resp);

			}
		}

		return nDownloaded;
	}

	private int uploadBones() {
		List<File> newBones = new ArrayList<File>(5);

		// Add all bones files that have been modified since the last upload
		for (File bonesFile : enumerateBones()) {
			long lastModified = bonesFile.lastModified();
			if(lastModified > lastUpload) {
				newBones.add(bonesFile);
			}
		}

		return uploadBonesFiles(newBones);
	}

	private int uploadBonesFiles(List<File> files) {

		int nUploaded = 0;
		String currentFileName;

		SharedPreferences.Editor ed = prefs.edit();

		for (int i = 0; i < files.size(); i++) {

			currentFileName = files.get(i).getName();

			List<Header> headerList = new ArrayList<Header>();

			headerList.add(new BasicHeader(HEADER_TOKEN, userToken));
			headerList.add(new BasicHeader(HEADER_FILE_NAME, currentFileName));
			if (i == 0) {
				headerList.add(new BasicHeader(HEADER_WANTS_INFO, "Y"));
			}

			NHFileInfo info = loadFile(files.get(i));

//            headerList.add(new BasicHeader(HEADER_VERSION + 1, info.get1()));
//            headerList.add(new BasicHeader(HEADER_VERSION + 2, info.get2()));
//            headerList.add(new BasicHeader(HEADER_VERSION + 3, info.get3()));
//            headerList.add(new BasicHeader(HEADER_VERSION + 4, info.get4()));
			headerList.add(new BasicHeader(HEADER_VERSIONCRC, getStringMD5(info.get1() + "," + info.get2() + "," + info.get3() + "," + info.get4())));

			headerList.add(new BasicHeader(HEADER_BONES_CRC, info.md5));

			HttpResponse resp = doPost(BASE_URL, UPLOAD, headerList, info.data);

			if (resp.getFirstHeader(HEADER_HEARSE) == null) {
				consumeContent(resp);
				return 0;
			}

			Header header = resp.getFirstHeader(HEADER_ERROR);
			if (header != null) {

				if (header.getValue().equals(F_ERROR_INFO)) {
					// This is a warning so pretend we succeeded.
					nUploaded++;

					if (!keepUploaded) {
						files.get(i).delete();
					}
					HttpEntity entity = resp.getEntity();
					BufferedReader in;
					try {
						in = new BufferedReader(new InputStreamReader(entity.getContent()));

						String line;
						while ((line = in.readLine()) != null) {
							Log.print(line); //@todo output this to screen

						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} else {
					HttpEntity e = resp.getEntity();
					BufferedReader in;
					try {
						in = new BufferedReader(new InputStreamReader(e.getContent()));

						String line;
						while ((line = in.readLine()) != null) {
							Log.print(line); //@todo output this to screen

						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}

				}
			} else {
				// Save the version for requests. Will help prevent bad bones.
				header = resp.getFirstHeader(HEADER_NETHACKVER);
				if (header != null) {
					ed.putString(HEADER_NETHACKVER, header.getValue());
				}
				Log.print("Uploaded " + currentFileName);
				nUploaded++;

				if (!keepUploaded) {
					files.get(i).delete();
				}

				header = resp.getFirstHeader(HEADER_MOTD);
				if (header != null) {
					Log.print(header.getName() + ":" + header.getValue()); //@todo output this to screen
				}

			}
			consumeContent(resp);
		}

		ed.commit();

		return nUploaded;
	}

	private NHFileInfo loadFile(File file) {
		long datasize = file.length();
		byte[] data = new byte[(int) datasize];
		BufferedInputStream in = null;
		NHFileInfo results = new NHFileInfo();
		try {
			in = new BufferedInputStream(new FileInputStream(file));

			in.read(data);

			results.data = data;

			ByteBuffer buf = ByteBuffer.wrap(data);
			// realized don't want to do this as it masks platform differences
//            buf.order(ByteOrder.LITTLE_ENDIAN);
			results.incarnation = ((long)buf.getInt()) & 0xffffffffL;
			results.feature_set = ((long)buf.getInt()) & 0xffffffffL;
			results.entity_count = ((long)buf.getInt()) & 0xffffffffL;
			results.struct_sizes = ((long)buf.getInt()) & 0xffffffffL;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		results.md5 = getByteMD5(data);

		return results;
	}

	private HttpResponse doGet(String baseUrl, String action, List<Header> headers) {
		HttpGet httpGet = new HttpGet(baseUrl + action);

		httpGet.setHeaders(headers.toArray(new Header[headers.size()]));
		httpGet.addHeader(HEADER_HEARSE_CRC, HEARSE_CRC);
		httpGet.addHeader(HEADER_CLIENT, CLIENT_ID);


		//making GET request.
		HttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
			// write response to log
			Log.print("Http Get Response:" + response.toString());
		} catch (ClientProtocolException e) {
			// Log exception
			e.printStackTrace();
		} catch (IOException e) {
			// Log exception
			e.printStackTrace();
		}

		return response;
	}

	private HttpResponse doPost(String baseUrl, String action, List<Header> headers, byte[] data) {

		HttpPost httpPost = new HttpPost(baseUrl + action);


		httpPost.setHeaders(headers.toArray(new Header[headers.size()]));
		httpPost.addHeader(HEADER_HEARSE_CRC, HEARSE_CRC);
		httpPost.addHeader(HEADER_CLIENT, CLIENT_ID);
		if (data != null) {
			ByteArrayEntity entity = new ByteArrayEntity(data);
			httpPost.setEntity(entity);
		}

		//making POST request.
		HttpResponse response = null;
		try {
			response = httpClient.execute(httpPost);
			// write response to log
			Log.print("Http Post Response:" + response.toString());
		} catch (ClientProtocolException e) {
			// Log exception
			e.printStackTrace();
		} catch (IOException e) {
			// Log exception
			e.printStackTrace();
		}

		return response;

	}

	private boolean isValidBonesFileName(String name) {
		Matcher matcher = PATTERN.matcher(name);

		boolean result = matcher.matches();
		Log.print(name + " is bones:" + result);
		return result;
	}

	private boolean isHearseReachable() {
		boolean reachable = false;
		try {
			reachable = InetAddress.getByName(HOST).isReachable(3000);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return reachable;
	}


	private void changeUserInfo() {
		Log.print("Hearse updating user info: " + userNick + ", " + userEmail);

		List<Header> headerList = new ArrayList<Header>();

		headerList.add(new BasicHeader(HEADER_EMAIL, userEmail));

		headerList.add(new BasicHeader(HEADER_NICK, userNick));

		headerList.add(new BasicHeader(HEADER_TOKEN, userToken));

		HttpResponse resp = doGet(BASE_URL, UPDATE_USER, headerList);

		if (resp.getFirstHeader(HEADER_HEARSE) != null && resp.getFirstHeader(HEADER_ERROR) != null) {

			HttpEntity e = resp.getEntity();
			BufferedReader in;
			try {
				in = new BufferedReader(new InputStreamReader(e.getContent()));

				String line;
				while ((line = in.readLine()) != null) {
					Log.print(line); //@todo output this to screen
				}

			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		consumeContent(resp);
	}

	private void consumeContent(HttpResponse resp)
	{
		try {
			HttpEntity entity = resp.getEntity();
			if(entity != null)
				entity.consumeContent();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to allow this class to listen for email or nickname changes and send them to Hearse
	 * @param sharedPreferences Preferences
	 * @param key changed key
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// Java 7 switch would be nice
		boolean enabled = sharedPreferences.getBoolean(PREFS_HEARSE_ENABLE, false);
		boolean enableChange = PREFS_HEARSE_ENABLE.equals(key);
		boolean emailChange = PREFS_HEARSE_MAIL.equals(key);
		boolean nickChange = PREFS_HEARSE_NAME.equals(key);

		if (emailChange || nickChange) {
			sharedPreferences.edit().putBoolean(PREFS_HEARSE_UPDATE_USER, true).commit();
		}

		if(enabled && (emailChange || enableChange)) {
			String mail = sharedPreferences.getString(PREFS_HEARSE_MAIL,"");
			if(mail.length() == 0) {
				showEmailRequired();
			}
		}
	}

	private class NHFileInfo {
		public byte[] data;
		String md5;
		long incarnation;    /* actual version number */
		long feature_set;    /* bitmask of config settings */
		long entity_count;   /* # of monsters and objects */
		long struct_sizes;   /* size of key structs */

		public String get1() {
			return String.valueOf(incarnation);
		}

		public String get2() {
			return String.valueOf(feature_set);
		}

		public String get3() {
			return String.valueOf(entity_count);
		}

		public String get4() {
			return String.valueOf(struct_sizes);
		}
	}

}

