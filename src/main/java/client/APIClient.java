package client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.json.JSONArray;

public class APIClient {
    private static int attempts = 0;
    private static int successfulLaunches= 0;
    public static void main(String[] args) throws IOException{
//        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to SpaceX data insights.");
        System.out.println("Here you will receive data regarding SpaceX launchpads:\n" +
                "how many launch attempts there were and how many of them succeeded.");
        JSONArray jsonArray = new JSONArray(Objects.requireNonNull(getLaunchpadData()));
        handleInfo(jsonArray);
        System.out.println("Attempts: " + attempts);
        System.out.println("Succeeded: " + successfulLaunches);

//        scanner.close();
    }

    private static String getLaunchpadData() throws IOException {

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

    private static void handleInfo(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.length(); i++) {
            attempts += jsonArray.getJSONObject(i).getInt("launch_attempts");
            successfulLaunches += jsonArray.getJSONObject(i).getInt("launch_successes");
        }
    }
}
