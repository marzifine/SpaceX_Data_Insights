package client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class APIClient {
    /**
     * The method provides information about event by its id
     * @param id - event id
     * @return String for JSONObject
     * @throws IOException
     */
    public static String getEvent(String id) throws IOException {
        return getData("https://api.spacexdata.com/v5/launches/" + id);
    }

    /**
     * The method provides information about past events
     * @return String of all past events for JSONArray
     * @throws IOException
     */
    public static String getPastEvents() throws IOException {
        return getData("https://api.spacexdata.com/v5/launches/past");
    }

    /**
     * The method provides history facts about SpaceX
     * @return String of history fact for JSONArray
     * @throws IOException
     */
    public static String getFacts() throws IOException {
        return getData("https://api.spacexdata.com/v4/history");
    }

    /**
     * The method provides information about an upcoming event
     * @return String of event information for JSONArray
     * @throws IOException
     */
    public static String getUpcomingEvent() throws IOException {
        return getData("https://api.spacexdata.com/v5/launches/upcoming");
    }

    /**
     * The method provides information about rocket by its id
     * @param id - rocket id
     * @return String of rocket information for JSONObject
     * @throws IOException
     */
    public static String getRocketInfo(String id) throws IOException {
        return getData("https://api.spacexdata.com/v4/rockets/" + id);
    }

    /**
     * The method provides information about crew member by id
     * @param id - crew member id
     * @return String of members information for JSONObject
     * @throws IOException
     */
    public static String getCrewInfo(String id) throws IOException {
        return getData("https://api.spacexdata.com/v4/crew/" + id);
    }

    /**
     * The method provides information about launchpad by its id
     * @param id - launchpad id
     * @return String of launchpad information for JSONObject
     * @throws IOException
     */
    public static String getLaunchpadInfo(String id) throws IOException {
        return getData("https://api.spacexdata.com/v4/launchpads/" + id);
    }

    /**
     * The method sends connects with provided url, sends GET request
     * and collects all the provided data to a String
     * @param url - needed url
     * @return String of information for JSONObject/JSONArray
     * @throws IOException
     */
    private static String getData(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            StringBuilder response = new StringBuilder();
            Scanner scanner = new Scanner(connection.getInputStream());
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
                response.append("\n");
            }
            scanner.close();

            return response.toString();
        }

        // an error happened
        System.out.println("Sorry, an error occurred while connecting. Please try again later.");
        return null;
    }
}
