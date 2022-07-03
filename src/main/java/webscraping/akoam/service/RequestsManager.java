package webscraping.akoam.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class RequestsManager {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:102.0) Gecko/20100101 Firefox/102.0";


    public static String sendGET(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setInstanceFollowRedirects(false);

        int responseCode = connection.getResponseCode();
        while (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            connection = (HttpURLConnection) new URL(connection.getHeaderField("Location")).openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            responseCode = connection.getResponseCode();
        }

        // workaround to prevent WE Quota redirections
        if (connection.getURL().getProtocol().equals("http")) {
            URL oldURL = connection.getURL();
            connection = (HttpURLConnection) new URL("https", oldURL.getHost(), oldURL.getPath()).openConnection();
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

}
