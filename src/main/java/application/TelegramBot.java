package application;

import client.APIClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TelegramBot extends TelegramLongPollingBot {


    private final static String START_COMMAND = "/start";
    private final static String UPCOMING_EVENT_COMMAND = "/upcoming";
    private final static String PAST_EVENTS_COMMAND = "/past";
    private final static String FACT_COMMAND = "/fact";
    private static Map<Long, User> users;
    private static String GREETING_MESSAGE;
    private final Set<String> commands = new HashSet<>();

    public TelegramBot() {
        super();
        try {
            users = new HashMap<>();
            GREETING_MESSAGE = Files.readString(Path.of("src", "resources", "greetings.txt"));
            commands.addAll(Files.readAllLines(Path.of("src",  "resources", "commands-bot")));
        } catch (IOException ignored) {
        }
    }

    @Override
    public String getBotUsername() {
        return "SpaceXBot";
    }

    /**
     * The method checks before initial run that the bot has a valid token;
     * if not - asks to enter it and saves to the api.txt file
     * @return String of Bot Token
     */
    @Override
    public String getBotToken() {
        int TOKEN_LENGTH = 46;
        try {
            String token = Files.readString(Path.of("src", "resources", "api.txt"));
            if (token.length() != TOKEN_LENGTH || !token.contains(":")) {
                throw new IOException("Wrong token");
            }
            return token;
        } catch (IOException e) {
            Scanner sc = new Scanner(System.in);
            System.out.println("Please enter your bot token:");
            String token = sc.nextLine();
            while (token.length() != TOKEN_LENGTH || !token.contains(":")) {
                System.out.println("Please enter right bot token:");
                token = sc.nextLine();
            }
            try {
                Files.writeString(Path.of("src", "resources", "api.txt"), token);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            System.out.println("If you want to change it, open \"src\", \"resources\", \"api.txt\"");
            return token;
        }
    }

    /**
     * The method notifies if there was an action (message or query callback) made towards bot;
     * checks users id and chats id; handles command or query callback
     * @param update - Update towards bot
     */
    @Override
    public void onUpdateReceived(Update update) {
        final Long currentUserId = update.hasCallbackQuery() ?
                update.getCallbackQuery().getFrom().getId() : update.getMessage().getFrom().getId();
        final Integer chatId = Math.toIntExact(update.hasCallbackQuery() ?
                update.getCallbackQuery().getMessage().getChatId() : update.getMessage().getChatId());
        if (update.hasMessage()) {
            if (!users.containsKey(currentUserId))
                users.put(currentUserId, new User(currentUserId));
            User user = users.get(currentUserId);
            if (update.getMessage().isCommand() && commands.contains(update.getMessage().getText())) {
                String command = update.getMessage().getText();
                switch (command) {
                    case START_COMMAND -> handleStart(chatId);
                    case UPCOMING_EVENT_COMMAND -> handleUpcomingEvent(user, chatId, false, null);
                    case PAST_EVENTS_COMMAND -> handlePastEvents(update, user, chatId, false, null);
                    case FACT_COMMAND -> handleFact(chatId);
                    default -> {
                        SendMessage message = new SendMessage();
                        message.setChatId(String.valueOf(chatId));
                        message.setText("Sorry I don't know this command... Try another one \uD83E\uDD7A");
                        try {
                            execute(message); // Call method to send the message
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            if (!users.containsKey(currentUserId))
                users.put(currentUserId, new User(currentUserId));
            User user = users.get(currentUserId);
            //query in format action:description:id[:id...]
            //callbackQuery[0] - action(upcoming/past/...)
            //callbackQuery[1] - description(event/rocket/launchpad/...)
            //callbackQuery[2] - id of objects(for past - use it's count)
            String[] callbackQuery = update.getCallbackQuery().getData().split(":");
            if (callbackQuery[0].equalsIgnoreCase("upcoming")) {
                handleUpcomingEvent(user, chatId, true, callbackQuery);
            } else if (callbackQuery[0].equalsIgnoreCase("past"))
                handlePastEvents(update, user, chatId, true, callbackQuery);
        }
    }

    /**
     * The method handles a random fact:
     * redirects request to APIClient and gets a random fact from the provided array
     * @param chatId - current chatId
     */
    private void handleFact(Integer chatId) {
        try {
            JSONArray facts = new JSONArray(APIClient.getFacts());
            int id = new Random().nextInt(facts.length());
            JSONObject fact = facts.getJSONObject(id);
            String title = fact.getString("title");
            long timeStamp = fact.getLong("event_date_unix");
            Date date = new Date(timeStamp * 1000);
            StringBuilder messageText = new StringBuilder();
            messageText.append(title).append(" on ").append(date);
            if (fact.has("details") && fact.get("details") instanceof String)
                messageText.append("\n").append(fact.getString("details"));
            if (fact.has("links") && fact.get("links") instanceof JSONObject) {
                JSONObject links = (fact.getJSONObject("links"));
                if (links.has("article")) {
                    if (links.get("article") instanceof String)
                        messageText.append("\nTo read more click here: ").append(new URL(links.getString("article"))).append("\n");
                    else if (links.get("article") instanceof JSONArray) {
                        JSONArray articles = links.getJSONArray("article");
                        messageText.append("\nTo read more click here: ");
                        for (int i = 0; i < articles.length(); i++) {
                            messageText.append(articles.getString(i)).append("\n");
                        }
                    }
                }
            }
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(messageText.toString());
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Sorry, something went wrong. Please try again later \uD83E\uDD7A");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The method handles past events:
     * redirects a request to APIClient, shows a user every 5 events starting from the
     * closest to current date; it's possible to get more information about every event
     * @param update - Update towards bot
     * @param user - current User
     * @param chatId - current chatId
     * @param callback - if there was a callback (the button was pressed)
     * @param callbackQuery - array of callback information
     */
    private void handlePastEvents(Update update, User user, Integer chatId, boolean callback, String[] callbackQuery) {
        try {
            if (!callback) {
                JSONArray past = new JSONArray(APIClient.getPastEvents());
                user.setPastEvents(past);
                getNextFivePastEvents(user, past, chatId);
            }
            if (callbackQuery[1].equalsIgnoreCase("event")) {
                String messageText = "";
                int id = Integer.parseInt(update.getCallbackQuery().getData()
                        .substring(update.getCallbackQuery().getData().lastIndexOf(":") + 1));
                JSONObject event = user.getPastEvents().getJSONObject(user.getPastEvents().length() - id);
                user.setEvent(event);
                long timeStamp = event.getLong("date_unix");
                Date date = new Date(timeStamp * 1000);
                messageText += "The launch was on " + date + " with ";
                getLaunchDetails(user, messageText, event, chatId, "past:");
            } else {
                switch (callbackQuery[1]) {
                    case "next" -> {
                        JSONArray past = user.getPastEvents();
                        getNextFivePastEvents(user, past, chatId);
                    }
                    case "reset" -> {
                        user.setPastEventId(1);
                        SendMessage message = new SendMessage();
                        message.setChatId(String.valueOf(chatId));
                        message.setText("Next time you select past events they will start from the beginning \uD83D\uDE0A");
                        try {
                            execute(message); // Call method to send the message
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                    default -> getEventData(chatId, callbackQuery);
                }
            }

        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Sorry, something went wrong. Please try again later \uD83E\uDD7A");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The method gives more details about specific launch:
     * crew, provided description, links to webcast, wikipedia, article;
     * asks if user wants to get more information about rocket/crew/launchpad
     * @param user - current User
     * @param messageText - text for message to send to user
     * @param event - current event to get details about
     * @param chatId - current chatId
     * @param callbackAction - previous callback action
     * @throws MalformedURLException - is thrown if the url isn't valid
     */
    private void getLaunchDetails(User user, String messageText, JSONObject event, Integer chatId, String callbackAction)
            throws MalformedURLException {
        messageText = getCrewAmountString(messageText, event);

        if (event.has("details") && event.get("details") instanceof String) {
            messageText += event.getString("details") + "\n";
        }

        if (event.has("links") && event.get("links") instanceof JSONObject) {
            JSONObject links = event.getJSONObject("links");
            if (links.has("webcast") && links.get("webcast") instanceof String)
                messageText += "Here is the link to webcast: " + new URL(links.getString("webcast")) + "\n";

            if (links.has("wikipedia") && links.get("wikipedia") instanceof String)
                messageText += "Here is the link to wikipedia page: " + new URL(links.getString("wikipedia")) + "\n";

            if (links.has("article") && links.get("article") instanceof String)
                messageText += "Here is the link to an article: " + new URL(links.getString("article")) + "\n";
        }
        user.setEvent(event);
        askAboutDetails(user, messageText, chatId, callbackAction);
    }

    /**
     * The method shows next 5 past events as buttons to be pressed,
     * also shows NEXT (for next 5 events) button and RESET button to
     * get past events from the very beginning
     * @param user - current User
     * @param past - array of all past events
     * @param chatId - current chatId
     */
    private void getNextFivePastEvents(User user, JSONArray past, Integer chatId) {
        int count = user.getPastEventId();
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (int i = past.length() - user.getPastEventId(); i >= 0; i--) {
            JSONObject event = past.getJSONObject(i);
            long timeStamp = event.getLong("date_unix");
            Date date = new Date(timeStamp * 1000);
            String messageText = date + " with ";
            messageText = getCrewAmountString(messageText, event);

            //add new event as a button
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton pastEvent = new InlineKeyboardButton();
            pastEvent.setText(messageText);
            pastEvent.setCallbackData("past:event:" + count);
            rowInline.add(pastEvent);
            rowsInline.add(rowInline);

            if (count == past.length()) {
                user.setPastEventId(count);
                break;
            }
            if (count % 5 == 0) {
                user.setPastEventId(count + 1);
                break;
            }
            count++;
        }
        //add next button
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton next = new InlineKeyboardButton();
        next.setText("NEXT");
        next.setCallbackData("past:next");
        rowInline.add(next);
        rowsInline.add(rowInline);

        //add reset button
        List<InlineKeyboardButton> nextRowInline = new ArrayList<>();
        InlineKeyboardButton reset = new InlineKeyboardButton();
        reset.setText("RESET");
        reset.setCallbackData("past:reset");
        nextRowInline.add(reset);
        rowsInline.add(nextRowInline);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Choose event you want to learn more about \uD83E\uDD13");
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        try {
            execute(message); // Call method to send the message
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        user.setPastEventId(count + 1);
    }

    /**
     * The method completes a text for message with the right text
     * depending on amount of crew members
     * @param messageText - text to complete
     * @param event - current event
     * @return completed String
     */
    private String getCrewAmountString(String messageText, JSONObject event) {
        JSONArray crew = event.getJSONArray("crew");
        int crewAmount = crew.length();
        if (crewAmount == 0)
            messageText += "no people on board.\n";
        else if (crewAmount == 1)
            messageText += "1 person on board.\n";
        else
            messageText += crewAmount + " people on board.\n";
        return messageText;
    }

    /**
     * The method gives more information about rocket/crew/launchpad
     * @param chatId - current chatId
     * @param callbackQuery - array of callback information
     */
    private void getEventData(Integer chatId, String[] callbackQuery) {
        try {
            StringBuilder messageText = new StringBuilder();
            switch (callbackQuery[1]) {
                case "rocket" -> {
                    if (callbackQuery.length < 3) {
                        messageText.append("Sorry, information about the rocket is missing.\n" + "Please try again later \uD83E\uDD7A");
                        break;
                    }
                    String rocketInfo = APIClient.getRocketInfo(callbackQuery[2]);
                    JSONObject rocket = new JSONObject(rocketInfo);
                    String nameRocket = rocket.getString("name");
                    String type = rocket.getString("type");
                    String firstFlight = rocket.getString("first_flight");
                    messageText.append("The name of the ").append(type).append(" is ").append(nameRocket).append("\n").append("and its first flight was on ").append(firstFlight).append(".\n");
                    if (rocket.has("description") && rocket.get("description") instanceof String) {
                        String description = rocket.getString("description");
                        messageText.append("Provided description is:\n").append(description).append("\n");
                    }
                    if (rocket.has("flickr_images") && rocket.get("flickr_images") instanceof JSONArray) {
                        String flickrImage = ((JSONArray) rocket.get("flickr_images")).getString(0);
                        messageText.append("Here is a link to an image: ").append(new URL(flickrImage)).append("\n");
                    }
                }
                case "crew" -> {
                    JSONObject event = new JSONObject(APIClient.getEvent(callbackQuery[2]));
                    JSONArray crew = event.getJSONArray("crew");
                    int crewAmount = crew.length();
                    if (crewAmount == 0) {
                        messageText.append("Oops, it seems like there is no information about the crew.\n" + "Please try another time \uD83E\uDD7A");
                        break;
                    }
                    messageText.append("Meet the crew!");
                    messageText.append("\uD83E\uDDD1\u200D\uD83D\uDE80".repeat(Math.max(0, crew.length())));
                    messageText.append("\n\n");
                    for (int i = 0; i < crew.length(); i++) {
                        String memberId = crew.getJSONObject(i).getString("crew");
                        JSONObject member = new JSONObject(APIClient.getCrewInfo(memberId));
                        String nameCrew = member.getString("name");
                        String role = crew.getJSONObject(i).getString("role");
                        messageText.append("\uD83E\uDDD1\u200D\uD83D\uDE80 ").append(nameCrew).append(" is the ").append(role).append(".\n");

                        if (member.has("image") && member.get("image") instanceof String)
                            messageText.append("Here is a link to the photo: ").append(new URL(member.getString("image"))).append("\n");

                        if (member.has("wikipedia") && member.get("wikipedia") instanceof String)
                            messageText.append("Here is a link to the wikipedia page: ").append(new URL(member.getString("wikipedia"))).append("\n");
                    }
                }
                case "launchpad" -> {
                    if (callbackQuery.length < 3) {
                        messageText.append("Sorry, information about the launchpad is missing.\n" + "Please try again later \uD83E\uDD7A");
                        break;
                    }
                    messageText.append("This is a launchpad for the selected flight! \uD83D\uDCA5 \n");
                    String launchpadId = callbackQuery[2];
                    String launchpadInfo = APIClient.getLaunchpadInfo(launchpadId);
                    JSONObject launchpad = new JSONObject(launchpadInfo);
                    String nameLaunchpad = launchpad.getString("full_name");
                    int success = launchpad.getJSONArray("launches").length();
                    messageText.append(nameLaunchpad).append(" has already ").append(success).append(" succeeded launches!");
                }
            }
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(messageText.toString());
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Sorry, something went wrong. Please try again later \uD83E\uDD7A");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The method asks user if there is need to get more information about rocket/crew/launchpad
     * as buttons
     * @param user - current User
     * @param messageText - text to send to user
     * @param chatId - current chatId
     * @param callbackAction - last callback action
     */
    private void askAboutDetails(User user, String messageText, Integer chatId, String callbackAction) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(messageText);
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        String rocketId = "";
        if (user.getEvent().has("rocket") && user.getEvent().get("rocket") instanceof String)
            rocketId = user.getEvent().getString("rocket");
        InlineKeyboardButton rocket = new InlineKeyboardButton();
        rocket.setText("\uD83D\uDE80 ROCKET \uD83D\uDE80");
        rocket.setCallbackData(callbackAction + "rocket:" + rocketId);


        InlineKeyboardButton crew = new InlineKeyboardButton();
        crew.setText("\uD83D\uDC69\u200D\uD83D\uDE80 CREW \uD83D\uDC69\u200D\uD83D\uDE80");
        //id for crew is the id of event, cause of large data size
        crew.setCallbackData(callbackAction + "crew:" + user.getEvent().getString("id"));

        List<InlineKeyboardButton> secondRowInline = new ArrayList<>();
        String launchpadId = "";
        if (user.getEvent().has("launchpad") && user.getEvent().get("launchpad") instanceof String)
            launchpadId = user.getEvent().getString("launchpad");
        InlineKeyboardButton launchpad = new InlineKeyboardButton();
        launchpad.setText("\uD83D\uDCA5 LAUNCHPAD \uD83D\uDCA5");
        launchpad.setCallbackData(callbackAction + "launchpad:" + launchpadId);

        rowInline.add(rocket);
        rowInline.add(crew);
        secondRowInline.add(launchpad);

        rowsInline.add(rowInline);
        rowsInline.add(secondRowInline);

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        try {
            execute(message); // Call method to send the message
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * The method handles an upcoming event:
     * redirects request to APIClient, gets the closest to the current date event and
     * gets more details about it
     * @param user - current User
     * @param chatId - current chatId
     * @param callback - if there was a callback (the button was pressed)
     * @param callbackQuery - array of callback information
     */
    private void handleUpcomingEvent(User user, Integer chatId, boolean callback, String[] callbackQuery) {
        try {
            if (!callback) {
                String messageText = "";
                JSONArray jsonArray = new JSONArray(Objects.requireNonNull(APIClient.getUpcomingEvent()));
                JSONObject nextEvent = null;
                for (int i = 0; i < jsonArray.length(); i++) {
                    long timeStamp = jsonArray.getJSONObject(i).getLong("date_unix");
                    Date date = new Date(timeStamp * 1000);
                    Date now = Calendar.getInstance().getTime();
                    //check if the event happens after the current time
                    if (date.after(now)) {
                        nextEvent = jsonArray.getJSONObject(i);
                        break;
                    }
                }
                if (nextEvent == null) {
                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(chatId));
                    message.setText("Sorry, I could not find next event. Please try again later \uD83E\uDD7A");
                    try {
                        execute(message); // Call method to send the message
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                long timeStamp = nextEvent.getLong("date_unix");
                Date date = new Date(timeStamp * 1000);
                messageText += "Next launch is on " + date + " with ";
                //gets details and sends the message
                getLaunchDetails(user, messageText, nextEvent, chatId, "upcoming:");
            } else if (callbackQuery[0].equalsIgnoreCase("upcoming"))
                getEventData(chatId, callbackQuery);
        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Sorry, something went wrong. Please try again later \uD83E\uDD7A");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The method handles start:
     * sends a greetings message
     * @param chatId - current chatId
     */
    private void handleStart(Integer chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(GREETING_MESSAGE);
        try {
            execute(message); // Call method to send the message
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
