package me.lachlanpage.b525helper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

// Modem class is responsible for handling and retrieving ALL information from Huawei Modem
public class Modem {

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


    // TRAFFIC STATS
    private int currentConnectionTime;
    private Float currentUpload;
    private Float currentDownload;

    private Float currentDownloadRate;
    private Float currentUploadRate;

    private Float totalUpload;
    private Float totalDownload;
    private int totalConnectionTime;


    // get fields from TRAFFIC_STATS_URL endpoint
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

    public Modem() {
        //this.isLoggedIn = false;
    }
}
