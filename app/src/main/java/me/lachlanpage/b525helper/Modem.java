package me.lachlanpage.b525helper;

import android.content.Context;
import android.preference.PreferenceManager;
import android.widget.Toast;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Modem class is responsible for handling and retrieving ALL information from Huawei Modem
public class Modem implements Runnable {

    private final String ADMIN_PASSWORD;

    // API END POINTS
    private final String MODEM_IP = "192.168.8.1";
    private final String HOMEPAGE = "http://" + MODEM_IP + "/html/home.html";
    private final String TRAFFIC_STATS_URL = "http://" + MODEM_IP + "/api/monitoring/traffic-statistics";
    private final String AUTHENTICATION_LOGIN = "http://"+ MODEM_IP + "/api/user/authentication_login";
    private final String SIGNAL_URL = "http://" + MODEM_IP + "/api/device/signal";

    // Common header used throughout SCRAM process
    private final String REQUEST_TOKEN = "__RequestVerificationToken";

    private MainActivity mParentActivity;
    private String mLoggedInCookie;
    private Boolean mIsLoggedIn;

    private Map<String, ModemData> mDataMap;

    private DataConversion mDataConversion;

    private Context mContext;

    public void getSignalStats() {
        try {
            Connection.Response res = Jsoup.connect(SIGNAL_URL).cookie("SessionID", mLoggedInCookie).execute();
            // stats related to signal device information
            Document signalDocument = res.parse();

            int cellID = Integer.parseInt(signalDocument.selectFirst("cell_id").text());
            int rsrq = Integer.parseInt(signalDocument.selectFirst("rsrq").text().replace("dB", ""));
            int rsrp = Integer.parseInt(signalDocument.selectFirst("rsrp").text().replace("dBm", ""));
            int rssi = Integer.parseInt(signalDocument.selectFirst("rssi").text().replace("dBm", ""));
            int sinr = Integer.parseInt(signalDocument.selectFirst("sinr").text().replace("dB", ""));
            int band = Integer.parseInt(signalDocument.selectFirst("band").text());
            int uploadBandwidth = Integer.parseInt(signalDocument.selectFirst("ulbandwidth").text().replace("MHz", ""));
            int downloadBandwidth = Integer.parseInt(signalDocument.selectFirst("dlbandwidth").text().replace("MHz", ""));

            mDataMap.get("cell_id").setmDataKey(String.valueOf(cellID));
            mDataMap.get("rsrq").setmDataKey(String.valueOf(rsrq) + " dB");
            mDataMap.get("rsrp").setmDataKey(String.valueOf(rsrp) + " dBm");
            mDataMap.get("rssi").setmDataKey(String.valueOf(rssi) + " dBm");
            mDataMap.get("sinr").setmDataKey(String.valueOf(sinr) + " dB");
            mDataMap.get("band").setmDataKey(String.valueOf(band));
            mDataMap.get("ulbandwidth").setmDataKey(String.valueOf(uploadBandwidth) + " MHz");
            mDataMap.get("dlbandwidth").setmDataKey(String.valueOf(downloadBandwidth) + " MHz");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // iterate through hashmap of data and return the list
    public List<ModemData> getModemData() {

        List<ModemData> list = new ArrayList<>();

        for(Map.Entry<String, ModemData> entry: mDataMap.entrySet()) {
            list.add(entry.getValue());
        }

        return list;
    }

    // get stats from TRAFFIC_STATS_URL endpoint
    public void getTrafficStats() {
        try {
            Document trafficDocument = Jsoup.connect(TRAFFIC_STATS_URL).get();

            int currentConnectionTime = Integer.parseInt(trafficDocument.selectFirst("currentconnecttime").text());

            Float currentUpload = Float.parseFloat(trafficDocument.selectFirst("currentupload").text());
            Float currentDownload = Float.parseFloat(trafficDocument.selectFirst("currentdownload").text());
            Float currentDownloadRate = Float.parseFloat(trafficDocument.selectFirst("currentdownloadrate").text());
            Float currentUploadRate = Float.parseFloat(trafficDocument.selectFirst("currentuploadrate").text());

            Float totalUpload = Float.parseFloat(trafficDocument.selectFirst("totalupload").text());
            Float totalDownload = Float.parseFloat(trafficDocument.selectFirst("totaldownload").text());
            int totalConnectionTime = Integer.parseInt(trafficDocument.selectFirst("totalconnecttime").text());

            //System.out.println(signal_doc.toString());
            mDataMap.get("currentconnecttime").setmDataKey(mDataConversion.convertElapsedTime(currentConnectionTime));

            mDataMap.get("currentupload").setmDataKey(mDataConversion.convertBytesToGigabytes(currentUpload) + " Gb");
            mDataMap.get("currentdownload").setmDataKey(mDataConversion.convertBytesToGigabytes(currentDownload) + " Gb");

            mDataMap.get("currentdownloadrate").setmDataKey(mDataConversion.convertBytesToMegabits(currentDownloadRate) + " Mbit/s");
            mDataMap.get("currentuploadrate").setmDataKey(mDataConversion.convertBytesToMegabits(currentUploadRate) + " Mbit/s");

            mDataMap.get("totalupload").setmDataKey(mDataConversion.convertBytesToGigabytes(totalUpload) + " Gb");
            mDataMap.get("totaldownload").setmDataKey(mDataConversion.convertBytesToGigabytes(totalDownload) + " Gb");
            mDataMap.get("totalconnecttime").setmDataKey(mDataConversion.convertElapsedTime(totalConnectionTime));

        } catch(Exception e) { e.printStackTrace(); }
    }

    // attempt to login to router using SCRAM based authentication
    private void login() {
        try {
            Connection.Response res = Jsoup.connect(HOMEPAGE).execute();

            String sessionID = res.cookie("SessionID");

            if(sessionID == null)
            {
                Toast.makeText(mContext, "Error getting modem IP", Toast.LENGTH_LONG).show();
                throw new Error();
            }

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

            if(mLoggedInCookie == null)
            {
                doToastMessage("Error Logging In");
                throw new Error();
            }

            mIsLoggedIn = true;
            doToastMessage("Login Successful");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doToastMessage(final String text) {
        mParentActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
            }
        });
    }


    // called every 5 seconds interval to update listview
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

    public Modem(Context context, MainActivity act) {
        mIsLoggedIn = false;

        ADMIN_PASSWORD = PreferenceManager.getDefaultSharedPreferences(context).getString("adminPassword", "password");

        mDataConversion = new DataConversion();

        mDataMap = new LinkedHashMap<>();

        mContext = context;

        mParentActivity = act;

        Toast.makeText(mContext, "Logging In...", Toast.LENGTH_SHORT).show();

        // pre populate the hash map of Modem Data , this allows adapter to inflate view until data retrieved
        mDataMap.put("signalStats", new ModemData("Signal Stats:", ""));
        mDataMap.put("cell_id", new ModemData("Cell ID:", "0"));
        mDataMap.put("rsrq", new ModemData("RSRQ:", "0 dB"));
        mDataMap.put("rsrp", new ModemData("RSRP:", "0 dBm"));
        mDataMap.put("rssi", new ModemData("RSSI:", "0 dBm"));
        mDataMap.put("sinr", new ModemData("SINR: ", "0 dB"));
        mDataMap.put("band", new ModemData("Band: ", "0"));
        mDataMap.put("ulbandwidth", new ModemData("Upload Bandwidth:", "0 MHz"));
        mDataMap.put("dlbandwidth", new ModemData("Download Bandwidth:", "0 MHz"));

        mDataMap.put("trafficStats", new ModemData("Traffic Stats:", ""));
        mDataMap.put("currentdownloadrate", new ModemData("Current Download Rate:", "0"));
        mDataMap.put("currentuploadrate", new ModemData("Current Upload Rate:", "0"));

        mDataMap.put("usageStats", new ModemData("Usage Stats:", ""));
        mDataMap.put("currentupload", new ModemData("Current Upload:", "0"));
        mDataMap.put("currentdownload", new ModemData("Current Download:", "0"));
        mDataMap.put("currentconnecttime", new ModemData("Connection Time ", "0"));

        mDataMap.put("totalStats", new ModemData("Total Usage Stats:", ""));
        mDataMap.put("totalupload", new ModemData("Total Upload:", "0"));
        mDataMap.put("totaldownload", new ModemData("Total Download:", "0"));
        mDataMap.put("totalconnecttime", new ModemData("Total Connection Time:", "0"));
    }

}
