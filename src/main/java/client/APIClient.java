package client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.*;

import org.json.JSONArray;
import org.mortbay.util.IO;

public class APIClient {
    private static int attempts = 0;

    public static int getAttempts() {
        return attempts;
    }

    public static void setAttempts(int attempts) {
        APIClient.attempts = attempts;
    }

    public static void addAttempts(int attempts) {
        APIClient.attempts += attempts;
    }

    public static int getSuccessfulLaunches() {
        return successfulLaunches;
    }

    public static void setSuccessfulLaunches(int successfulLaunches) {
        APIClient.successfulLaunches = successfulLaunches;
    }

    public static void addSuccessfulLaunches(int successfulLaunches) {
        APIClient.successfulLaunches += successfulLaunches;
    }

    private static int successfulLaunches= 0;
    public static String getLaunchpadData() throws IOException {

        //get  launchpad data: how many pads are active and how many successful launches were there
        HttpURLConnection connection = (HttpURLConnection) new URL("https://api.spacexdata.com/v4/launchpads").openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if(responseCode == 200){
            String response = "";
            Scanner scanner = new Scanner(connection.getInputStream());
            while(scanner.hasNextLine()){
                response += scanner.nextLine();
                response += "\n";
            }
            scanner.close();

            return response;
        }

        // an error happened
        return null;
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
        if(responseCode == 200){
            String response = "";
            Scanner scanner = new Scanner(connection.getInputStream());
            while(scanner.hasNextLine()){
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
