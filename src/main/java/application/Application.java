package application;

import client.APIClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Application {
    private static Scanner scanner;
    private static String action = "";

    private final static String START_COMMAND = "/start";
    private final static String UPCOMING_EVENT_COMMAND = "/upcoming";
    private final static String PAST_EVENTS_COMMAND = "/past";
    private final static String FACT_COMMAND = "/fact";
    private final static String EXIT_COMMAND = "/exit";
    private final static String HELP_COMMAND = "/help";
    private final static String HELP_MESSAGE = "Use the following commands to get new information:\n" +
                                                UPCOMING_EVENT_COMMAND + " - get an upcoming event\n" +
                                                PAST_EVENTS_COMMAND + " - get last 10 events\n" +
                                                FACT_COMMAND + " - get a random fact about the SpaceX company\n" +
                                                HELP_COMMAND + " - get this message\n" +
                                                EXIT_COMMAND + " - exit bot";
    private static final String GREETING_MESSAGE =
            "Welcome to SpaceX data insights!\n" +
            "The bot can provide you insights about upcoming flights, information on past events, \n" +
            "facts, history and some other information. \n" +
            "To start please enter " + START_COMMAND +".";

    public static void main(String[] args) throws IOException {
        scanner = new Scanner(System.in);

        System.out.println(GREETING_MESSAGE);
        if (scanner.nextLine().equalsIgnoreCase(START_COMMAND)) {
            action = "start";
            start();
        }

        scanner.close();
    }

    public static void start() {
        System.out.println(HELP_MESSAGE);
        handleCommand();
    }

    private static void handleCommand() {
        while (scanner.hasNextLine() && !action.equals("EXIT")) {
            switch (scanner.nextLine()) {
                case UPCOMING_EVENT_COMMAND: action = "upcoming";
                    handleUpcomingEvent();
                    break;
                case PAST_EVENTS_COMMAND: action = "past";
                    handlePastEvents();
                    break;
                case FACT_COMMAND: action = "fact";
                    handleFact();
                    break;
                case EXIT_COMMAND: action = "EXIT";
                    return;
//                    handleExit();
//                    break;
                case HELP_COMMAND: action = "help";
                    System.out.println(HELP_MESSAGE);
            }
        }
    }

    private static void handleExit() {

    }

    private static void handleFact() {
        try {
            JSONArray facts = new JSONArray(APIClient.getFacts());
            int id = new Random().nextInt(facts.length());
            JSONObject fact = facts.getJSONObject(id);
            String title = fact.getString("title");
            Long timeStamp = fact.getLong("event_date_unix");
            Date date = new Date(timeStamp*1000);
            System.out.println(title + " on " + date);
            if (fact.has("details") && fact.get("details") instanceof String)
                System.out.println(fact.getString("details"));
            if (fact.has("links") && fact.get("links") instanceof JSONObject) {
                if (((JSONObject) fact.get("links")).has("article")) {
                    if (((JSONObject) fact.get("links")).get("article") instanceof String)
                        System.out.println("To read more click here: " + new URL(((JSONObject) fact.get("links")).getString("article")));
                    else if (((JSONObject) fact.get("links")).get("article") instanceof JSONArray) {
                        JSONArray articles = (JSONArray) ((JSONObject) fact.get("links")).get("article");
                        System.out.print("To learn more click here: ");
                        for (int i = 0; i < articles.length(); i++) {
                            System.out.println(articles.getString(i));
                        }
                    }
                }
            }
            action = "";
            System.out.println("Now you can type in the next command");
        } catch (IOException e) {
            System.out.println("Sorry, something went wrong. Please try again later.");
        }

    }

    private static void handlePastEvents() {

    }

    private static void handleUpcomingEvent() {
        try {
            JSONArray jsonArray = new JSONArray(Objects.requireNonNull(APIClient.getUpcomingEvent()));
            JSONObject nextEvent = null;
            for (int i = 0; i < jsonArray.length(); i++) {
                Long timeStamp = jsonArray.getJSONObject(i).getLong("date_unix");
                Date date = new Date(timeStamp*1000);
                Date now = Calendar.getInstance().getTime();
                //check if the event happens after the current time
                if (date.after(now)) {
                    nextEvent = jsonArray.getJSONObject(i);
                    break;
                }
            }
            if (nextEvent == null) {
                System.out.println("Sorry, I could not find next event. Please try again later.");
                return;
            }
            Long timeStamp = nextEvent.getLong("date_unix");
            Date date = new Date(timeStamp*1000);

            int crewAmount = nextEvent.getJSONArray("crew").length();

            System.out.println("Next upcoming event launches on " + date + "with " + crewAmount + " people on board.");
            if (nextEvent.has("details") && nextEvent.get("details") instanceof String) {
                System.out.println("The description to this event is:\n" + nextEvent.getString("details"));
            }

            if (nextEvent.has("webcast") && nextEvent.get("webcast") instanceof String) {
                System.out.println("Here is the link to webcast: " + new URL(nextEvent.getString("webcast")));
            }

            if (nextEvent.has("wikipedia") && nextEvent.get("wikipedia") instanceof String) {
                System.out.println("Here is the link to wikipedia page: " + new URL(nextEvent.getString("wikipedia")));
            }

            System.out.println("Do you want to get more information about this launch?\n" +
                    "Type YES if you do.");
            if (scanner.nextLine().equalsIgnoreCase("yes")) {
                action += ":choosing";
                System.out.println("If you want to get more information about the rocket please type in \"rocket\".");
                if (crewAmount > 0)
                    System.out.println("If you want to get more information about the crew please enter \"crew\".");
                System.out.println("If you want to abort this section type EXIT");
            } else {
                action = "";
                System.out.println("Now you can type in the next command");
                return;
            }
            while (scanner.hasNext() && action.equals("upcoming:choosing")) {
                switch (scanner.nextLine()) {
                    case "rocket" :
                        String rocketId = "";
                        if (nextEvent.has("rocket") && nextEvent.get("rocket") instanceof String)
                            rocketId = nextEvent.getString("rocket");
                        if (rocketId.equals("")) System.out.println("Sorry, information about the rocket is missing.\n" +
                                "Please try again later.");
                        else {
                            String rocketInfo = APIClient.getRocketInfo(rocketId);
                            JSONObject rocket = new JSONObject(rocketInfo);
                            String name = rocket.getString("name");
                            String type = rocket.getString("type");
                            String firstFlight = rocket.getString("first_flight");
                            System.out.println("The name of the " + type + " is " + name + "\n" +
                                    "and its first flight was on " + firstFlight + ".");
                            if (rocket.has("description") && rocket.get("description") instanceof String) {
                                String description = rocket.getString("description");
                                System.out.println("Provided description is:\n" + description);
                            }
                            if (rocket.has("flickr_images") && rocket.get("flickr_images") instanceof JSONArray) {
                                String flickrImage = ((JSONArray) rocket.get("flickr_images")).getString(0);
                                System.out.println("Here is a link to an image: " + new URL(flickrImage));
                            }
                        }
                        break;
                    case "crew" :
                        JSONArray crew = nextEvent.getJSONArray("crew");
                        for (int i = 0; i < crew.length(); i++) {
                            String memberId = crew.getJSONObject(i).getString("crew");
                            JSONObject member = new JSONObject(APIClient.getCrewInfo(memberId));
                            String name = member.getString("name");
                            String role = crew.getJSONObject(i).getString("role");
                            System.out.println(name + " is the " + role + ".");

                            if (member.has("image") && member.get("image") instanceof String)
                                System.out.println("Here is a link to the photo: " + new URL(member.getString("image")));

                            if (member.has("wikipedia") && member.get("wikipedia") instanceof String)
                                System.out.println("Here is a link to the wikipedia page: " + new URL(member.getString("wikipedia")));
                        }
                        break;
                    case "EXIT" :
                        action = "";
                        System.out.println("Now you can type in the next command");
                }
            }
        } catch (IOException error) {
            System.out.println("Sorry, something went wrong. Please try again later.");
        }
    }
}
