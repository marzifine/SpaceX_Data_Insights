package client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mortbay.util.IO;

public class APIClient {
    public static String getEvent(String id) throws IOException {
        return getData("https://api.spacexdata.com/v5/launches/" + id);
    }

    public static String getPastEvents() throws IOException {
        return getData("https://api.spacexdata.com/v5/launches/past");
    }

    public static String getFacts() throws IOException {
        return getData("https://api.spacexdata.com/v4/history");
    }

    public static String getUpcomingEvent() throws IOException {
        return getData("https://api.spacexdata.com/v5/launches/upcoming");
    }

    public static String getRocketInfo(String id) throws IOException {
        return getData("https://api.spacexdata.com/v4/rockets/" + id);
    }

    public static String getCrewInfo(String id) throws IOException {
        return getData("https://api.spacexdata.com/v4/crew/" + id);
    }

    public static String getLaunchpadInfo(String id) throws IOException {
        return getData("https://api.spacexdata.com/v4/launchpads/" + id);
    }

    private static String getData(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            String response = "";
            Scanner scanner = new Scanner(connection.getInputStream());
            while (scanner.hasNextLine()) {
                response += scanner.nextLine();
                response += "\n";
            }
            scanner.close();

            return response;
        }

        // an error happened
        System.out.println("Sorry, an error occurred while connecting. Please try again later.");
        return null;
    }
}
