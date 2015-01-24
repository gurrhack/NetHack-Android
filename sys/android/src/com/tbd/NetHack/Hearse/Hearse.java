/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tbd.NetHack.Hearse;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.Toast;

import com.tbd.NetHack.Log;
import com.tbd.NetHack.NetHack;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class communicates with the Hearse server and provides all Hearse functionality.
 * @author Ranbato
 */
public class Hearse extends Thread implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String EXISTING_BONES_SET_PREFS = "existingBonesSet";
    private static final String CLIENT_NAME = "Gurr Android Nethack";
    private static final String CLIENT_VERSION = "1.0.0";
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
    private static final String HEADER_EMAIL = HEADER_TOKEN;//"X_USEREMAIL";
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
    private static final Pattern pattern = Pattern.compile("^bon[A-Z](0|(:?Arc|Bar|Cav|Hea|Kni|Mon|Pri|Rog|Ran|Sam|Tou|Val|Wiz))\\.([A-Z]|\\d+)\\z", Pattern.CASE_INSENSITIVE);
    private static final String TAG = "MD5";
    public static final String PREFS_HEARSE_ID = "hearseID";
    public static final String PREFS_HEARSE_MAIL = "hearseMail";
    public static final String PREFS_HEARSE_NAME = "hearseName";
    public static final String PREFS_HEARSE_KEEP_UPLOADED = "hearseKeepUploaded";
    public static final String PREFS_HEARSE_ENABLE = "hearseEnable";
    private final NetHack nethack;
    private final SharedPreferences prefs;
    private String dataDirString;
    private String userNick;
    private String userEmail;
    private String userToken;
    private boolean keepUploaded;
    private HttpClient httpClient;

    /**
     * List of existing bones.  Would be much cleaner with API 11 level as we could use StringSet directly.
     */
    private String existingBonesSet;

    /**
     * Creates a new instance of Hearse
     *
     * @param nh The {@link com.tbd.NetHack.NetHack} application for access to {@link Activity}, through which it can
     *            create {@link Toast} notifications, etc.  Also provides access to the
     *                location of the Nethack data directory
     * @param prefs SharedPreferences
     */
    public Hearse(NetHack nh,SharedPreferences prefs) {
        super();

        this.nethack = nh;
        dataDirString = nethack.getPreferences(Activity.MODE_PRIVATE).getString("datadir", "");
        this.prefs = prefs;

        userToken = prefs.getString(PREFS_HEARSE_ID, "");
        userEmail = prefs.getString(PREFS_HEARSE_MAIL, "");
        userNick = prefs.getString(PREFS_HEARSE_NAME, "");
        keepUploaded = prefs.getBoolean(PREFS_HEARSE_KEEP_UPLOADED, false);
        existingBonesSet = prefs.getString(EXISTING_BONES_SET_PREFS, "");
        httpClient = new DefaultHttpClient();

        //@todo register for pref change events so can force email entry
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

        /*
        // Sort them by age to facilitate processing
        Arrays.sort(bones, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                long lhsModified = lhs.lastModified();
                long rhsModified = rhs.lastModified();
                if (lhsModified < rhsModified) {
                    return -1;
                } else if (lhsModified > rhsModified) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
*/
        return bones == null ? new File[0] : bones;
    }

    /**
     * The main method.  Registers new user and uploads and downloads bones files.
     * Originally called drive() when single threaded. :)
     *
     */
    @Override
    public void run() {

        // Sleep 3 seconds to allow Nethack to finish starting up
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (isHearseReachable()) {

            boolean newUser = false;
            if (userToken.length() == 0) {
                if (userEmail.length() > 0) {
                    userToken = createNewUser();
                    newUser = true;
                } else {

                    showToast("Hearse requires an Email address to register");
                }
            } else {
                Log.print("using existing token " + userToken);
            }

            // @todo Check if userNick information has changed and update.

            if (userToken.length() > 0) {
                boolean up = uploadBones();
                if (up || newUser) {
                    downloadBones();
                }
            }
        }

    }

    private void showToast(final String message) {
        nethack.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.print(message);
                Toast.makeText(nethack, message, Toast.LENGTH_SHORT).show();
            }
        });


    }

    private String createNewUser() {
        List<Header> headerList = new ArrayList<Header>();


        headerList.add(new BasicHeader(HEADER_EMAIL, userEmail));

        headerList.add(new BasicHeader(HEADER_NICK, userNick));

        HttpResponse resp = doGet(BASE_URL, NEW_USER, headerList);

        if (resp.getFirstHeader(HEADER_HEARSE) == null) {
            return "";
        }
        if (resp.getFirstHeader(HEADER_ERROR) != null) {

            HttpEntity e = resp.getEntity();
            BufferedReader in;
            try {
                in = new BufferedReader(new InputStreamReader(e.getContent()));

                String line = null;
                while ((line = in.readLine()) != null) {
                    System.out.println(line); //@todo output this to screen
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } else {
            Header tokenHeader = resp.getFirstHeader(HEADER_TOKEN);
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(PREFS_HEARSE_ID, tokenHeader.getValue());
            ed.commit();
            return tokenHeader.getValue();
        }

        return "";
    }

    private void downloadBones() {

        String hackver = prefs.getString(HEADER_NETHACKVER, "54"); // 54 is us, 26 is Windows 3.4.3
        while (true) {

            List<Header> headerList = new ArrayList<Header>();

            headerList.add(new BasicHeader(HEADER_TOKEN, userToken));
            headerList.add(new BasicHeader(HEADER_USER_LEVELS, existingBonesSet));
            //isEmpty requires API 9
            if (!"".equals(hackver)) {
                headerList.add(new BasicHeader(HEADER_NETHACKVER, hackver));
            }


            HttpResponse resp = doGet(BASE_URL, DOWNLOAD, headerList);

            if (resp.getFirstHeader(HEADER_HEARSE) == null) {
                return;
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


                } else {
                    //arg
                    Log.print("Bad bones downloaded");
                    tmpBonesFile.delete();
                }


            }


        }

        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(EXISTING_BONES_SET_PREFS, existingBonesSet);
        ed.commit();


    }

    private boolean uploadBones() {
        File[] bones = enumerateBones();
        List<File> newBones = new ArrayList<File>(5);

        // Update bones list to account for deleted ones.
        String[] bonesList = existingBonesSet.split(",");

        //May be overkill as never more than 100 entries but...
        Set<String> bonesSet = new HashSet<String>(bonesList.length);
        Collections.addAll(bonesSet, bonesList);

        for (File bonesFile : bones) {
            if (!bonesSet.remove(bonesFile.getName())) {
                //new bones file to be uploaded
                newBones.add(bonesFile);
            }
        }

        // Now the ones left in the Set are actually the bones files used and deleted
        // so we need to take them out of the existingBonesSet
        // Normally shouldn't be more than a couple as we run this every game.
        // Changes will be saved in uploadBonesFiles
        for (String missing : bonesSet) {
            existingBonesSet = existingBonesSet.replace(missing + ",", "");
        }

        return uploadBonesFiles(newBones);
    }

    private boolean uploadBonesFiles(List<File> files) {

        boolean someUploaded = false;
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
                return false;
            }
            Header header = resp.getFirstHeader(HEADER_ERROR);
            if (header != null) {

                if (header.getValue().equals(F_ERROR_INFO)) {
                    // This is a warning so pretend we succeeded.
                    someUploaded = true;

                    if (keepUploaded) {

                        // Add it to the list if we are keeping it.
                        existingBonesSet = existingBonesSet + currentFileName + ",";

                    } else {
                        files.get(i).delete();
                    }
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
            } else {
                // Save the version for requests. Will help prevent bad bones.
                header = resp.getFirstHeader(HEADER_NETHACKVER);
                if (header != null) {
                    ed.putString(HEADER_NETHACKVER, header.getValue());
                }
                Log.print("Uploaded " + currentFileName);
                someUploaded = true;

                if (!keepUploaded) {
                    files.get(i).delete();

                } else {
                    // Add it to the list if we are keeping it.
                    existingBonesSet = existingBonesSet + currentFileName + ",";
                }

                header = resp.getFirstHeader(HEADER_MOTD);
                if (header != null) {
                    Log.print(header.getName() + ":" + header.getValue()); //@todo output this to screen
                }

            }


        }


        ed.putString(EXISTING_BONES_SET_PREFS, existingBonesSet);
        ed.commit();

        return someUploaded;
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
            results.incarnation = buf.getLong();
            results.feature_set = buf.getLong();
            results.entity_count = buf.getLong();
            results.struct_sizes = buf.getLong();

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
        Matcher matcher = pattern.matcher(name);

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


    private void changeUserInfo(String email, String nick){

        List<Header> headerList = new ArrayList<Header>();


        headerList.add(new BasicHeader(HEADER_EMAIL, email));

        headerList.add(new BasicHeader(HEADER_NICK, nick));

        headerList.add(new BasicHeader(HEADER_TOKEN,userToken));

        HttpResponse resp = doGet(BASE_URL, UPDATE_USER, headerList);

        if (resp.getFirstHeader(HEADER_HEARSE) == null) {
            return;
        }
        if (resp.getFirstHeader(HEADER_ERROR) != null) {

            HttpEntity e = resp.getEntity();
            BufferedReader in;
            try {
                in = new BufferedReader(new InputStreamReader(e.getContent()));

                String line = null;
                while ((line = in.readLine()) != null) {
                    System.out.println(line); //@todo output this to screen
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

return;

    }

    /**
     * Method to allow this class to listen for email or nickname changes and send them to Hearse
     * @param sharedPreferences Preferences
     * @param key changed key
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Java 7 switch would be nice
        if (PREFS_HEARSE_MAIL.equals(key) || PREFS_HEARSE_NAME.equals(key)) {
            if(sharedPreferences.contains(PREFS_HEARSE_ID)) {
                String mail = sharedPreferences.getString(PREFS_HEARSE_MAIL,"");
                String name = sharedPreferences.getString(PREFS_HEARSE_NAME,"");
                if(!"".equals(mail) || !"".equals(name)) {
                    changeUserInfo(mail, name);
                }
            }
        } else if ("hearseEnable".equals(key)) {
            //@todo prompt for hearseMail if empty
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

