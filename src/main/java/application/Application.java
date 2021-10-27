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
                                                UPCOMING_EVENT_COMMAND + " - get information about upcoming event\n" +
                                                PAST_EVENTS_COMMAND + " - get information about 10 last events\n" +
                                                FACT_COMMAND + " - get a random fact about the SpaceX company\n" +
                                                HELP_COMMAND + " - get this message\n" +
                                                EXIT_COMMAND + " - exit bot";
    private static final String GREETING_MESSAGE =
            "Welcome to SpaceX data insights!\n" +
            "The bot can provide you insights about upcoming flights, information on past events, \n" +
            "facts, history and some other information.";
//            "To start please enter " + START_COMMAND +".";

    public static void main(String[] args) throws IOException {
        scanner = new Scanner(System.in);

        System.out.println(GREETING_MESSAGE);
        handleCommand();
//        if (scanner.nextLine().equalsIgnoreCase(START_COMMAND)) {
//            action = "start";
//            start();
//        }
        scanner.close();
    }

//    public static void start() {
//        System.out.println(HELP_MESSAGE);
//        handleCommand();
//    }

    private static void handleCommand() {
        System.out.println("Now you can type in the next command");
        while (!action.equals("EXIT") && scanner.hasNextLine()) {
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
                    break;
                case HELP_COMMAND: action = "help";
                    System.out.println(HELP_MESSAGE);
            }
        }
    }
    private static void handleCommand(String command) {
        if (!action.equals("EXIT")) {
            switch (command) {
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
                    break;
                case HELP_COMMAND: action = "help";
                    System.out.println(HELP_MESSAGE);
            }
        }
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
                JSONObject links = (fact.getJSONObject("links"));
                if (links.has("article")) {
                    if (links.get("article") instanceof String)
                        System.out.println("To read more click here: " + new URL(links.getString("article")));
                    else if (links.get("article") instanceof JSONArray) {
                        JSONArray articles = links.getJSONArray("article");
                        System.out.print("To learn more click here: ");
                        for (int i = 0; i < articles.length(); i++) {
                            System.out.println(articles.getString(i));
                        }
                    }
                }
            }
            action = "";
        } catch (IOException error) {
            System.out.println("Sorry, something went wrong. Please try again later.");
        }

    }

    private static void handlePastEvents() {
        try {
            JSONArray past = new JSONArray(APIClient.getPastEvents());
            int count = 1;
            for (int i = past.length() - 1; i >= 0; i--) {
                JSONObject event = past.getJSONObject(i);
                Long timeStamp = event.getLong("date_unix");
                Date date = new Date(timeStamp*1000);
                System.out.print(count + ". " + "The launch was on " + date + " with ");
                printCrewAmount(event.getJSONArray("crew").length());
                if (event.has("details") && event.get("details") instanceof String) {
                    System.out.println("The description to this event is:\n" + event.getString("details"));
                }
                if (count % 5 == 0) {
                    System.out.println("Type in NEXT to get next 5 launches.");
                    if (!scanner.nextLine().equalsIgnoreCase("next")) {
                        break;
                    }
                }
                count++;
            }
            action += ":getNumber";
            while (action.startsWith("past:getNumber")) {
                System.out.println("Please type in the number of event you want to learn more about.\n" +
                        "If you want to exit this section please type in \"EXIT\"");
                String idLine = scanner.nextLine();
                if (idLine.matches("\\d+")) {
                    int id = Integer.parseInt(idLine);
                    if (id > count || id < 1) {
                        System.out.println("Please enter the right number.");
                        continue;
                    }

                    JSONObject event = past.getJSONObject(past.length() - id);
                    Long timeStamp = event.getLong("date_unix");
                    Date date = new Date(timeStamp*1000);
                    System.out.print("The launch was on " + date + " with ");
                    getEventData(event);
                } else if (idLine.equals("EXIT")) {
                    action = "";
                    System.out.println("Now you can type in the next command");
                }
            }

        } catch (IOException error) {
            System.out.println("Sorry, something went wrong. Please try again later.");
        }
    }

    private static void printCrewAmount(int crewAmount) {
        if (crewAmount == 0)
            System.out.println("no people ob board.");
        else if (crewAmount == 1)
            System.out.println("1 person on board.");
        else
            System.out.println(crewAmount + " people on board.");
    }

    private static void getEventData(JSONObject event) {
        try {
            JSONArray crew = event.getJSONArray("crew");
            int crewAmount = crew.length();
            printCrewAmount(crewAmount);

            if (event.has("details") && event.get("details") instanceof String) {
                System.out.println("The description to this event is:\n" + event.getString("details"));
            }

            if (event.has("links") && event.get("links") instanceof JSONObject) {
                JSONObject links = event.getJSONObject("links");
                if (links.has("webcast") && links.get("webcast") instanceof String)
                    System.out.println("Here is the link to webcast: " + new URL(links.getString("webcast")));

                if (links.has("wikipedia") && links.get("wikipedia") instanceof String)
                    System.out.println("Here is the link to wikipedia page: " + new URL(links.getString("wikipedia")));

                if (links.has("article") && links.get("article") instanceof String)
                    System.out.println("Here is the link to an article: " + new URL(links.getString("article")));
            }
            action += ":choosing";
            System.out.println("If you want to get more information about the rocket please type in \"rocket\".");
            System.out.println("If you want to get more information about the crew please enter \"crew\".");
            System.out.println("If you want to get more information about the launchpad please type in \"launchpad\"");
            System.out.println("If you want to abort this section type \"EXIT\"");
            while (action.contains(":choosing")) {
                String command = scanner.nextLine();
                if (command.startsWith("/")) handleCommand(command);
                switch (command) {
                    case "rocket":
                        String rocketId = "";
                        if (event.has("rocket") && event.get("rocket") instanceof String)
                            rocketId = event.getString("rocket");
                        if (rocketId.equals("")) {
                            System.out.println("Sorry, information about the rocket is missing.\n" +
                                    "Please try again later.");
                            break;
                        }
                        String rocketInfo = APIClient.getRocketInfo(rocketId);
                        JSONObject rocket = new JSONObject(rocketInfo);
                        String nameRocket = rocket.getString("name");
                        String type = rocket.getString("type");
                        String firstFlight = rocket.getString("first_flight");
                        System.out.println("The name of the " + type + " is " + nameRocket + "\n" +
                                "and its first flight was on " + firstFlight + ".");
                        if (rocket.has("description") && rocket.get("description") instanceof String) {
                            String description = rocket.getString("description");
                            System.out.println("Provided description is:\n" + description);
                        }
                        if (rocket.has("flickr_images") && rocket.get("flickr_images") instanceof JSONArray) {
                            String flickrImage = ((JSONArray) rocket.get("flickr_images")).getString(0);
                            System.out.println("Here is a link to an image: " + new URL(flickrImage));
                        }
                        break;
                    case "crew":
                        if (crewAmount == 0) {
                            System.out.println("Oops, it seems like there is no information about the crew. Please try another time.");
                            break;
                        }
                        for (int i = 0; i < crew.length(); i++) {
                            String memberId = crew.getJSONObject(i).getString("crew");
                            JSONObject member = new JSONObject(APIClient.getCrewInfo(memberId));
                            String nameCrew = member.getString("name");
                            String role = crew.getJSONObject(i).getString("role");
                            System.out.println(nameCrew + " is the " + role + ".");

                            if (member.has("image") && member.get("image") instanceof String)
                                System.out.println("Here is a link to the photo: " + new URL(member.getString("image")));

                            if (member.has("wikipedia") && member.get("wikipedia") instanceof String)
                                System.out.println("Here is a link to the wikipedia page: " + new URL(member.getString("wikipedia")));
                        }
                        break;
                    case "launchpad":
                        String launchpadId = "";
                        if (event.has("launchpad") && event.get("launchpad") instanceof String)
                            launchpadId = event.getString("launchpad");
                        if (launchpadId.equals("")) {
                            System.out.println("Sorry, information about the launchpad is missing.\n" +
                                    "Please try again later.");
                            break;
                        }
                        String launchpadInfo = APIClient.getLaunchpadInfo(launchpadId);
                        JSONObject launchpad = new JSONObject(launchpadInfo);
                        String nameLaunchpad = launchpad.getString("full_name");
                        int success = launchpad.getJSONArray("launches").length();
                        System.out.println(nameLaunchpad + " has already " + success + " succeeded launches!");
                        break;
                    case "EXIT":
                        action = action.substring(0, action.lastIndexOf(":"));
                }
            }
        } catch (IOException error) {
            System.out.println("Sorry, something went wrong. Please try again later.");
        }
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
            System.out.print("Next launch is on " + date + " with ");
            getEventData(nextEvent);
            action = "";
        } catch (IOException error) {
            System.out.println("Sorry, something went wrong. Please try again later.");
        }
    }
}
