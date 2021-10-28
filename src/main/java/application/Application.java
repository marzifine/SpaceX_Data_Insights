package application;

import client.APIClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Application extends TelegramLongPollingBot {
    private static Scanner scanner;
    private static String action = "";
    private static Map<Integer, User> users = new HashMap<>();
    private final Set<String> commands = new HashSet<>();

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

    public Application() {
        super();
//        commands.addAll()
    }

//    public static void main(String[] args) throws IOException {
//        scanner = new Scanner(System.in);
//
//        System.out.println(GREETING_MESSAGE);
//        handleCommand();
//        scanner.close();
//    }

//    private static void handleCommand() {
//        while (!action.equals("EXIT")) {
//            System.out.println("Now you can type in the next command");
//            switch (scanner.nextLine()) {
//                case UPCOMING_EVENT_COMMAND: action = "upcoming";
//                    handleUpcomingEvent();
//                    break;
//                case PAST_EVENTS_COMMAND: action = "past";
//                    handlePastEvents();
//                    break;
//                case FACT_COMMAND: action = "fact";
//                    handleFact();
//                    break;
//                case EXIT_COMMAND: action = "EXIT";
//                    break;
//                case HELP_COMMAND: action = "help";
//                    System.out.println(HELP_MESSAGE);
//            }
//        }
//    }
//    private static void handleCommand(String command) {
//        if (!action.equals("EXIT")) {
//            switch (command) {
//                case UPCOMING_EVENT_COMMAND: action = "upcoming";
//                    handleUpcomingEvent();
//                    break;
//                case PAST_EVENTS_COMMAND: action = "past";
//                    handlePastEvents();
//                    break;
//                case FACT_COMMAND: action = "fact";
//                    handleFact();
//                    break;
//                case EXIT_COMMAND: action = "EXIT";
//                    break;
//                case HELP_COMMAND: action = "help";
//                    System.out.println(HELP_MESSAGE);
//            }
//        }
//    }

    private void handleFact(Update update, User user) {
        try {
            JSONArray facts = new JSONArray(APIClient.getFacts());
            int id = new Random().nextInt(facts.length());
            JSONObject fact = facts.getJSONObject(id);
            String title = fact.getString("title");
            Long timeStamp = fact.getLong("event_date_unix");
            Date date = new Date(timeStamp*1000);
            String messageText = "";
            messageText += title + " on " + date;
            if (fact.has("details") && fact.get("details") instanceof String)
                messageText += "\n" + fact.getString("details");
            if (fact.has("links") && fact.get("links") instanceof JSONObject) {
                JSONObject links = (fact.getJSONObject("links"));
                if (links.has("article")) {
                    if (links.get("article") instanceof String)
                        messageText += "\nTo read more click here: " + new URL(links.getString("article"));
                    else if (links.get("article") instanceof JSONArray) {
                        JSONArray articles = links.getJSONArray("article");
                        messageText += ("\nTo read more click here: ");
                        for (int i = 0; i < articles.length(); i++) {
                            messageText += articles.getString(i) + "\n";
                        }
                    }
                }
            }
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(update.getMessage().getChatId()));
            message.setText(messageText);
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            user.setAction("");
        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(update.getMessage().getChatId()));
            message.setText("Sorry, something went wrong. Please try again later.");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void handlePastEvents(Update update, User user) {
        try {
            if (user.getAction().equals("")) {
                String messageText = "";
                JSONArray past = new JSONArray(APIClient.getPastEvents());
                user.setPastEvents(past);
                int count = user.getPastEventId();
                user.setPastEventId(count);
                for (int i = past.length() - 1; i >= 0; i--) {
                    JSONObject event = past.getJSONObject(i);
                    Long timeStamp = event.getLong("date_unix");
                    Date date = new Date(timeStamp * 1000);
                    System.out.print(count + ". " + "The launch was on " + date + " with ");
                    JSONArray crew = event.getJSONArray("crew");
                    int crewAmount = crew.length();
                    if (crewAmount == 0)
                        messageText += "no people ob board.\n";
                    else if (crewAmount == 1)
                        messageText += "1 person on board.\n";
                    else
                        messageText += crewAmount + " people on board.\n";
                    if (event.has("details") && event.get("details") instanceof String) {
                        System.out.println("The description to this event is:\n" + event.getString("details"));
                    }
                    if (count % 5 == 0) {
                        messageText += "Do you want to get another 5 events?";
                        SendMessage message = new SendMessage();
                        message.setChatId(String.valueOf(update.getMessage().getChatId()));
                        message.setText(messageText);

                        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                        keyboardMarkup.setResizeKeyboard(true);
                        keyboardMarkup.setOneTimeKeyboard(true);
                        List<KeyboardRow> list = new ArrayList<>();
                        KeyboardRow row = new KeyboardRow();
                        row.add("YES");
                        row.add("NO");
                        list.add(row);
                        keyboardMarkup.setKeyboard(list);
                        message.setReplyMarkup(keyboardMarkup);
                        try {
                            execute(message); // Call method to send the message
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                        user.setPastEventId(count + 1);
                        break;
                    }
                    count++;
                }
                user.setAction(user.getAction() + ":ask");
            } else if (user.getAction().equals("past:ask")) {
                switch (update.getMessage().getText()) {
                    case "YES" :
                        String messageText = "";
                        JSONArray past = user.getPastEvents();
                        int count = user.getPastEventId();
                        for (int i = past.length() - user.getPastEventId(); i >= 0; i--) {
                            JSONObject event = past.getJSONObject(i);
                            Long timeStamp = event.getLong("date_unix");
                            Date date = new Date(timeStamp * 1000);
                            messageText += count + ". " + "The launch was on " + date + " with ";
                            JSONArray crew = event.getJSONArray("crew");
                            int crewAmount = crew.length();
                            if (crewAmount == 0)
                                messageText += "no people ob board.\n";
                            else if (crewAmount == 1)
                                messageText += "1 person on board.\n";
                            else
                                messageText += crewAmount + " people on board.\n";
                            if (event.has("details") && event.get("details") instanceof String) {
                                messageText += "The description to this event is:\n" + event.getString("details");
                            }
                            //TODO: handle the last event
                            if (count % 5 == 0) {
                                messageText += "Do you want to get another 5 events?";
                                SendMessage message = new SendMessage();
                                message.setChatId(String.valueOf(update.getMessage().getChatId()));
                                message.setText(messageText);

                                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                                keyboardMarkup.setResizeKeyboard(true);
                                keyboardMarkup.setOneTimeKeyboard(true);
                                List<KeyboardRow> list = new ArrayList<>();
                                KeyboardRow row = new KeyboardRow();
                                row.add("YES");
                                row.add("NO");
                                list.add(row);
                                keyboardMarkup.setKeyboard(list);
                                message.setReplyMarkup(keyboardMarkup);
                                try {
                                    execute(message); // Call method to send the message
                                } catch (TelegramApiException e) {
                                    e.printStackTrace();
                                }
                                //TODO: count + 1 ?
                                user.setPastEventId(count + 1);
                                break;
                            }
                            count++;
                        }
                    case "NO" :
                        SendMessage message = new SendMessage();
                        message.setChatId(String.valueOf(update.getMessage().getChatId()));
                        message.setText("Please enter a number of event you want to learn more about.");

                        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                        keyboardMarkup.setResizeKeyboard(true);
                        keyboardMarkup.setOneTimeKeyboard(true);
                        List<KeyboardRow> list = new ArrayList<>();
                        KeyboardRow row = new KeyboardRow();
                        row.add("EXIT");
                        list.add(row);
                        keyboardMarkup.setKeyboard(list);
                        message.setReplyMarkup(keyboardMarkup);
                        try {
                            execute(message); // Call method to send the message
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                        user.setAction(user.getAction() + ":getNumber");
                }
            } else if (user.getAction().equals("past:ask:getNumber")) {
                String messageText = "";
                if (update.getMessage().getText().matches("\\d+")) {
                    int id = Integer.parseInt(update.getMessage().getText());
                    if (id > user.getPastEventId() || id < 1) {
                        messageText += "Please enter the right number.";
                    } else {
                        JSONObject event = user.getPastEvents().getJSONObject(user.getPastEvents().length() - id);
                        Long timeStamp = event.getLong("date_unix");
                        Date date = new Date(timeStamp*1000);
                        messageText += "The launch was on " + date + " with ";
                        JSONArray crew = event.getJSONArray("crew");
                        int crewAmount = crew.length();
                        if (crewAmount == 0)
                            messageText += "no people ob board.\n";
                        else if (crewAmount == 1)
                            messageText += "1 person on board.\n";
                        else
                            messageText += crewAmount + " people on board.\n";
                        if (event.has("details") && event.get("details") instanceof String) {
                            messageText += "The description to this event is:\n" + event.getString("details");
                        }

                        if (event.has("links") && event.get("links") instanceof JSONObject) {
                            JSONObject links = event.getJSONObject("links");
                            if (links.has("webcast") && links.get("webcast") instanceof String)
                                messageText += "Here is the link to webcast: " + new URL(links.getString("webcast"));

                            if (links.has("wikipedia") && links.get("wikipedia") instanceof String)
                                messageText += "Here is the link to wikipedia page: " + new URL(links.getString("wikipedia"));

                            if (links.has("article") && links.get("article") instanceof String)
                                messageText += "Here is the link to an article: " + new URL(links.getString("article"));
                        }
                        user.setEvent(event);
                        SendMessage message = new SendMessage();
                        message.setChatId(String.valueOf(update.getMessage().getChatId()));
                        message.setText(messageText);
                        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                        keyboardMarkup.setResizeKeyboard(true);
                        keyboardMarkup.setOneTimeKeyboard(true);
                        List<KeyboardRow> list = new ArrayList<>();
                        KeyboardRow row = new KeyboardRow();
                        row.add("ROCKET");
                        row.add("CREW");
                        row.add("LAUNCHPAD");
                        row.add("EXIT");
                        list.add(row);
                        keyboardMarkup.setKeyboard(list);
                        message.setReplyMarkup(keyboardMarkup);
                        try {
                            execute(message); // Call method to send the message
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                        user.setAction(user.getAction() + ":choosing");
                        return;
                    }
            } else {
                    messageText += "Please enter the right number.";
                }
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(update.getMessage().getChatId()));
                message.setText(messageText);
                try {
                    execute(message); // Call method to send the message
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (user.getAction().contains("choosing"))
                    getEventData(update, user);
        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(update.getMessage().getChatId()));
            message.setText("Sorry, something went wrong. Please try again later.");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

//    private void printCrewAmount(int crewAmount) {
//        if (crewAmount == 0)
//            System.out.println("no people ob board.");
//        else if (crewAmount == 1)
//            System.out.println("1 person on board.");
//        else
//            System.out.println(crewAmount + " people on board.");
//    }

    private void getEventData(Update update, User user) {
        try {
            if (update.getMessage().getText().equals("ROCKET")) {

            }
            JSONObject event = user.getEvent();
            JSONArray crew = event.getJSONArray("crew");
            int crewAmount = crew.length();

            String messageText = "";
            switch (update.getMessage().getText()) {
                case "ROCKET":
                    String rocketId = "";
                    if (event.has("rocket") && event.get("rocket") instanceof String)
                        rocketId = event.getString("rocket");
                    if (rocketId.equals("")) {
                        messageText += "Sorry, information about the rocket is missing.\n" +
                                "Please try again later.";
                        break;
                    }
                    String rocketInfo = APIClient.getRocketInfo(rocketId);
                    JSONObject rocket = new JSONObject(rocketInfo);
                    String nameRocket = rocket.getString("name");
                    String type = rocket.getString("type");
                    String firstFlight = rocket.getString("first_flight");
                    messageText += "The name of the " + type + " is " + nameRocket + "\n" +
                            "and its first flight was on " + firstFlight + ".";
                    if (rocket.has("description") && rocket.get("description") instanceof String) {
                        String description = rocket.getString("description");
                        messageText += "Provided description is:\n" + description;
                    }
                    if (rocket.has("flickr_images") && rocket.get("flickr_images") instanceof JSONArray) {
                        String flickrImage = ((JSONArray) rocket.get("flickr_images")).getString(0);
                        messageText += "Here is a link to an image: " + new URL(flickrImage);
                    }
                    break;
                case "CREW":
                    if (crewAmount == 0) {
                        messageText += "Oops, it seems like there is no information about the crew.\nPlease try another time.";
                        break;
                    }
                    for (int i = 0; i < crew.length(); i++) {
                        String memberId = crew.getJSONObject(i).getString("crew");
                        JSONObject member = new JSONObject(APIClient.getCrewInfo(memberId));
                        String nameCrew = member.getString("name");
                        String role = crew.getJSONObject(i).getString("role");
                        messageText += nameCrew + " is the " + role + ".\n";

                        if (member.has("image") && member.get("image") instanceof String)
                            messageText += "Here is a link to the photo: " + new URL(member.getString("image") + "\n");

                        if (member.has("wikipedia") && member.get("wikipedia") instanceof String)
                            messageText += "Here is a link to the wikipedia page: " + new URL(member.getString("wikipedia") + "\n");
                    }
                    break;
                case "LAUNCHPAD":
                    String launchpadId = "";
                    if (event.has("launchpad") && event.get("launchpad") instanceof String)
                        launchpadId = event.getString("launchpad");
                    if (launchpadId.equals("")) {
                        messageText += "Sorry, information about the launchpad is missing.\n" +
                                "Please try again later.";
                        break;
                    }
                    String launchpadInfo = APIClient.getLaunchpadInfo(launchpadId);
                    JSONObject launchpad = new JSONObject(launchpadInfo);
                    String nameLaunchpad = launchpad.getString("full_name");
                    int success = launchpad.getJSONArray("launches").length();
                    messageText += nameLaunchpad + " has already " + success + " succeeded launches!";
                    break;
            }
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(update.getMessage().getChatId()));
            message.setText(messageText);
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            keyboardMarkup.setOneTimeKeyboard(true);
            List<KeyboardRow> list = new ArrayList<>();
            KeyboardRow row = new KeyboardRow();
            row.add("ROCKET");
            row.add("CREW");
            row.add("LAUNCHPAD");
            row.add("EXIT");
            list.add(row);
            keyboardMarkup.setKeyboard(list);
            message.setReplyMarkup(keyboardMarkup);
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
//            }
        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(update.getMessage().getChatId()));
            message.setText("Sorry, something went wrong. Please try again later.");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The method searches for upcoming events at SpaceX and
     * gives information
     * @param update
     * @param user
     */
    private void handleUpcomingEvent(Update update, User user) {
        try {
            if (user.getAction().equals("")) {
                JSONArray jsonArray = new JSONArray(Objects.requireNonNull(APIClient.getUpcomingEvent()));
                JSONObject nextEvent = null;
                for (int i = 0; i < jsonArray.length(); i++) {
                    Long timeStamp = jsonArray.getJSONObject(i).getLong("date_unix");
                    Date date = new Date(timeStamp * 1000);
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
                Date date = new Date(timeStamp * 1000);
                String messageText = "";
                messageText += "Next launch is on " + date + " with ";
                JSONArray crew = nextEvent.getJSONArray("crew");
                int crewAmount = crew.length();
                if (crewAmount == 0)
                    messageText += "no people ob board.\n";
                else if (crewAmount == 1)
                    messageText += "1 person on board.\n";
                else
                    messageText += crewAmount + " people on board.\n";
//                printCrewAmount(crewAmount);

                if (nextEvent.has("details") && nextEvent.get("details") instanceof String) {
                    messageText += "The description to this event is:\n" + nextEvent.getString("details");
                }

                if (nextEvent.has("links") && nextEvent.get("links") instanceof JSONObject) {
                    JSONObject links = nextEvent.getJSONObject("links");
                    if (links.has("webcast") && links.get("webcast") instanceof String)
                        messageText += "Here is the link to webcast: " + new URL(links.getString("webcast"));

                    if (links.has("wikipedia") && links.get("wikipedia") instanceof String)
                        messageText += "Here is the link to wikipedia page: " + new URL(links.getString("wikipedia"));

                    if (links.has("article") && links.get("article") instanceof String)
                        messageText += "Here is the link to an article: " + new URL(links.getString("article"));
                }
                user.setEvent(nextEvent);
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(update.getMessage().getChatId()));
                message.setText(messageText);
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                keyboardMarkup.setOneTimeKeyboard(true);
                List<KeyboardRow> list = new ArrayList<>();
                KeyboardRow row = new KeyboardRow();
                row.add("ROCKET");
                row.add("CREW");
                row.add("LAUNCHPAD");
                row.add("EXIT");
                list.add(row);
                keyboardMarkup.setKeyboard(list);
                message.setReplyMarkup(keyboardMarkup);
                try {
                    execute(message); // Call method to send the message
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                user.setAction(user.getAction() + ":choosing");
            } else if (user.getAction().equals("upcoming:choosing"))
                getEventData(update, user);
        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(update.getMessage().getChatId()));
            message.setText("Sorry, something went wrong. Please try again later.");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return null;
    }

    @Override
    public String getBotToken() {
        return null;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Integer id = Math.toIntExact(update.getMessage().getFrom().getId());
            if (!users.containsKey(id))
                users.put(id, new User(id));
            User user = users.get(id);
            if (update.getMessage().isCommand()) {
                String command = update.getMessage().getText();
                switch (command) {
                    case "/start" -> handleStart(update, user);
                    case "/upcoming" -> handleUpcomingEvent(update, user);
                    case "/past" -> handlePastEvents(update, user);
                    case "/fact" -> handleFact(update, user);
//                    case "/help" -> handleHelp(update, user);
                    default -> {
                        SendMessage message = new SendMessage();
                        message.setChatId(String.valueOf(update.getMessage().getChatId()));
                        message.setText("Sorry I don't know this command... Try another one");
                        try {
                            execute(message); // Call method to send the message
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                if (update.getMessage().getText().equals("EXIT")) {
                    user.setAction("");
                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(update.getMessage().getChatId()));
                    message.setText("Action was successfully aborted!");
                    ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
                    remove.setRemoveKeyboard(true);
                    message.setReplyMarkup(remove);
                    try {
                        execute(message); // Call method to send the message
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                } else if (!user.getAction().equals("")) {
                    if (user.getAction().contains("upcoming:")) handleUpcomingEvent(update, user);
                    else if (user.getAction().contains("past:")) handlePastEvents(update, user);
                    else if (user.getAction().contains("fact:")) handleFact(update, user);
                }
            }
        }
    }

    private void handleStart(Update update, User user) {
        if (user.getAction().equals("")) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(update.getMessage().getChatId()));
            message.setText(GREETING_MESSAGE);
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            keyboardMarkup.setOneTimeKeyboard(true);
            List<KeyboardRow> list = new ArrayList<>();
            KeyboardRow row = new KeyboardRow();
            row.add("EXIT");
            list.add(row);
            keyboardMarkup.setKeyboard(list);
            message.setReplyMarkup(keyboardMarkup);
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}
