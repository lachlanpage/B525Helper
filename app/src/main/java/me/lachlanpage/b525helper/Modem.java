package me.lachlanpage.b525helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

// Modem class is responsible for handling and retrieving ALL information from Huawei Modem
public class Modem implements Runnable {

    // API END POINTS
    private String MODEM_IP = "192.168.8.1";
    private String HOMEPAGE = "http://" + MODEM_IP + "/html/home.html";
    private String LOGIN = "http://" + MODEM_IP + "/api/user/login";
    private String TRAFFIC_STATS_URL = "http://" + MODEM_IP + "/api/monitoring/traffic-statistics";
    private String AUTHENTICATION_LOGIN = "http://"+ MODEM_IP + "/api/user/authentication_login";
    private String SIGNAL_URL = "http://" + MODEM_IP + "/api/device/signal";

    // Common header used throughout SCRAM process
    private String REQUEST_TOKEN = "__RequestVerificationToken";

    private String ADMIN_PASSWORD = "password";

    private final String PREF_PASSWORD_NAME = "adminPassword";
    private SharedPreferences mSettings;



    private String mLoggedInCookie;
    private Boolean mIsLoggedIn;

    // TRAFFIC STATS
    private int currentConnectionTime;
    private Float currentUpload;
    private Float currentDownload;

    private Float currentDownloadRate;
    private Float currentUploadRate;

    private Float totalUpload;
    private Float totalDownload;
    private int totalConnectionTime;

    private int cellID;
    private int rsrq;
    private int rsrp;
    private int rssi ;
    private int sinr ;
    private int band;
    private int uploadBandwidth;
    private int downloadBandwidth;

    public void getSignalStats() {
        try {
            Connection.Response res = Jsoup.connect(SIGNAL_URL).cookie("SessionID", mLoggedInCookie).execute();
            // stats related to signal device information
            Document signalDocument = res.parse();

            cellID = Integer.parseInt(signalDocument.selectFirst("cell_id").text());
            rsrq = Integer.parseInt(signalDocument.selectFirst("rsrq").text().replace("dB", ""));
            rsrp = Integer.parseInt(signalDocument.selectFirst("rsrp").text().replace("dBm", ""));
            rssi = Integer.parseInt(signalDocument.selectFirst("rssi").text().replace("dBm", ""));
            sinr = Integer.parseInt(signalDocument.selectFirst("sinr").text().replace("dB", ""));
            band = Integer.parseInt(signalDocument.selectFirst("band").text());
            uploadBandwidth = Integer.parseInt(signalDocument.selectFirst("ulbandwidth").text().replace("MHz", ""));
            downloadBandwidth = Integer.parseInt(signalDocument.selectFirst("dlbandwidth").text().replace("MHz", ""));

            //System.out.println(signal_doc.toString());
            System.out.println("cellID: " + cellID);
            System.out.println("rsrq: " + rsrq);
            System.out.println("rsrp: " + rsrp);
            System.out.println("rssi: " + rssi);
            System.out.println("sinr: " + sinr);
            System.out.println("band: " + band);
            System.out.println("ub: " + uploadBandwidth);
            System.out.println("db: " + downloadBandwidth);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // get stats from TRAFFIC_STATS_URL endpoint
    public void getTrafficStats() {
        try {
            Document trafficDocument = Jsoup.connect(TRAFFIC_STATS_URL).get();

            currentConnectionTime = Integer.parseInt(trafficDocument.selectFirst("currentconnecttime").text());

            currentUpload = Float.parseFloat(trafficDocument.selectFirst("currentupload").text());
            currentDownload = Float.parseFloat(trafficDocument.selectFirst("currentdownload").text());
            currentDownloadRate = Float.parseFloat(trafficDocument.selectFirst("currentdownloadrate").text());
            currentUploadRate = Float.parseFloat(trafficDocument.selectFirst("currentuploadrate").text());

            totalUpload = Float.parseFloat(trafficDocument.selectFirst("totalupload").text());
            totalDownload = Float.parseFloat(trafficDocument.selectFirst("totaldownload").text());
            totalConnectionTime = Integer.parseInt(trafficDocument.selectFirst("totalconnecttime").text());

        } catch(Exception e) { e.printStackTrace(); }
    }

    // attempt to login to router using SCRAM based authentication
    private void login() {
        try {
            Connection.Response res = Jsoup.connect(HOMEPAGE).execute();

            String sessionID = res.cookie("SessionID");

            // Begin SCRAM authentication
            UtilityCrypto crypto = new UtilityCrypto();

            String clientNonce = crypto.generateClientNonce();

            // B525 uses last 32 bits of server token
            String serverToken = Jsoup.connect("http://192.168.8.1/api/webserver/token").get().selectFirst("token").text();
            serverToken = serverToken.substring(serverToken.length() - 32, serverToken.length());

            String scramBodyRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><username>admin</username><firstnonce>" + clientNonce + "</firstnonce><mode>1</mode></request>";
            res = Jsoup.connect("http://192.168.8.1/api/user/challenge_login").requestBody(scramBodyRequest).header("Content-type", "text/html").header(REQUEST_TOKEN, serverToken).cookie("SessionID", sessionID).method(Connection.Method.POST).execute();

            String verificationToken = res.header(REQUEST_TOKEN);
            Document authDocument = res.parse();
            String serverNonce = authDocument.selectFirst("servernonce").text();
            String salt = authDocument.selectFirst("salt").text();
            int iterations = Integer.parseInt(authDocument.selectFirst("iterations").text());

            byte[] proof = crypto.getClientProof(clientNonce, serverNonce, ADMIN_PASSWORD, salt, iterations);

            String loginBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><clientproof>" + crypto.toHex(proof) + "</clientproof><finalnonce>" + serverNonce + "</finalnonce></request>";
            res = Jsoup.connect(AUTHENTICATION_LOGIN).requestBody(loginBody).header("Content-type", "application/x-www-form-urlencoded; charset=UTF-8").header(REQUEST_TOKEN, verificationToken).cookie("SessionID", sessionID).method(Connection.Method.POST).execute();

            mLoggedInCookie = res.cookie("SessionID");
            mIsLoggedIn = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        getTrafficStats();

        if(!mIsLoggedIn){
            login();
            getSignalStats();
        }

        else {
            getSignalStats();
        }


    }

    public Modem(Context context) {
        mIsLoggedIn = false;

        mSettings = PreferenceManager.getDefaultSharedPreferences(context);

        ADMIN_PASSWORD = mSettings.getString(PREF_PASSWORD_NAME, "password");
    }
}
