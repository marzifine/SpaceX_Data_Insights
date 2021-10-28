package application;

import client.APIClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
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
    private static Map<Long, User> users = new HashMap<Long, User>();
    private static String GREETING_MESSAGE;
    private final Set<String> commands = new HashSet<>();

    public TelegramBot() {
        super();
        try {
            GREETING_MESSAGE = Files.readString(Path.of("src", "main", "java", "resources", "greetings.txt"));
            commands.addAll(Files.readAllLines(Path.of("src", "main", "java", "resources", "commands-bot")));
        } catch (IOException ignored) {
        }
    }

    @Override
    public String getBotUsername() {
        return "SpaceXBot";
    }

    @Override
    public String getBotToken() {
        return "2044808909:AAH70a1hUq172F7dfsYQVWrzCHgL1dbYNaE";
    }

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
                user.setAction("");
                String command = update.getMessage().getText();
                switch (command) {
                    case START_COMMAND -> handleStart(chatId);
                    case UPCOMING_EVENT_COMMAND -> handleUpcomingEvent(update, user, chatId, false, null);
                    case PAST_EVENTS_COMMAND -> handlePastEvents(update, user, chatId, false, null);
                    case FACT_COMMAND -> handleFact(update, user, chatId);
                    default -> {
                        SendMessage message = new SendMessage();
                        message.setChatId(String.valueOf(chatId));
                        message.setText("\uD83D\uDE2E Sorry I don't know this command... Try another one");
                        try {
                            execute(message); // Call method to send the message
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
//                if (update.getMessage().getText().equals("EXIT")) {
//                    user.setAction("");
//                    SendMessage message = new SendMessage();
//                    message.setChatId(String.valueOf(chatId));
//                    message.setText("Action was successfully aborted!");
//                    ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
//                    remove.setRemoveKeyboard(true);
//                    message.setReplyMarkup(remove);
//                    try {
//                        execute(message); // Call method to send the message
//                    } catch (TelegramApiException e) {
//                        e.printStackTrace();
//                    }
//                } else if (!user.getAction().equals("")) {
//                    if (user.getAction().contains("upcoming:")) handleUpcomingEvent(update, user, chatId);
//                    else if (user.getAction().contains("past:")) handlePastEvents(update, user, chatId);
//                }
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
                handleUpcomingEvent(update, user, chatId, true, callbackQuery);
            } else if (callbackQuery[0].equalsIgnoreCase("past"))
                handlePastEvents(update, user, chatId, true, callbackQuery);
        }
    }

    private void handleFact(Update update, User user, Integer chatId) {
        try {
            JSONArray facts = new JSONArray(APIClient.getFacts());
            int id = new Random().nextInt(facts.length());
            JSONObject fact = facts.getJSONObject(id);
            String title = fact.getString("title");
            Long timeStamp = fact.getLong("event_date_unix");
            Date date = new Date(timeStamp * 1000);
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
            message.setChatId(String.valueOf(chatId));
            message.setText(messageText);
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            user.setAction("");
        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("\uD83D\uDE2C Sorry, something went wrong. Please try again later.");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void handlePastEvents(Update update, User user, Integer chatId, boolean callback, String[] callbackQuery) {
        try {
            if (!callback) {
                JSONArray past = new JSONArray(APIClient.getPastEvents());
                user.setPastEvents(past);
                getNextFivePastEvents(user, past, chatId);

            }
//                if (user.getAction().contains("choosing")) {
//                    getEventData(update, user, chatId);
//                } else
            if (callbackQuery[1].equalsIgnoreCase("event")) {
                String messageText = "";
                int id = Integer.parseInt(update.getCallbackQuery().getData()
                        .substring(update.getCallbackQuery().getData().lastIndexOf(":") + 1));
                JSONObject event = user.getPastEvents().getJSONObject(user.getPastEvents().length() - id);
                user.setEvent(event);
                Long timeStamp = event.getLong("date_unix");
                Date date = new Date(timeStamp * 1000);
                messageText += "The launch was on " + date + " with ";
                getLaunchDetails(user, messageText, event, chatId, "past:");
//                user.setAction("past:ask:choosing");
            } else {
                switch (callbackQuery[1]) {
                    case "next":
                        JSONArray past = user.getPastEvents();
                        getNextFivePastEvents(user, past, chatId);
                        break;
                    case "exit":
                        user.setPastEventId(1);
//                        user.setAction("");
                        break;
                    default:
                        getEventData(user, chatId, callbackQuery, "past");
                }
            }

        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("\uD83D\uDE2C Sorry, something went wrong. Please try again later.");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void getLaunchDetails(User user, String messageText, JSONObject event, Integer chatId, String callbackAction)
            throws MalformedURLException {
        messageText = getCrewAmountString(messageText, event);

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
        askAboutDetails(user, messageText, chatId, callbackAction);
    }

    private void getNextFivePastEvents(User user, JSONArray past, Integer chatId) {
        int count = user.getPastEventId();
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (int i = past.length() - user.getPastEventId(); i >= 0; i--) {
            JSONObject event = past.getJSONObject(i);
            Long timeStamp = event.getLong("date_unix");
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

        //add exit button
        List<InlineKeyboardButton> nextRowInline = new ArrayList<>();
        InlineKeyboardButton exit = new InlineKeyboardButton();
        exit.setText("EXIT");
        exit.setCallbackData("past:exit");
        nextRowInline.add(exit);
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

    private String getCrewAmountString(String messageText, JSONObject event) {
        JSONArray crew = event.getJSONArray("crew");
        int crewAmount = crew.length();
        if (crewAmount == 0)
            messageText += "no people ob board.\n";
        else if (crewAmount == 1)
            messageText += "1 person on board.\n";
        else
            messageText += crewAmount + " people on board.\n";
        if (event.has("details") && event.get("details") instanceof String) {
            messageText += "The description to this event is:\n" + event.getString("details") + "\n";
        }
        return messageText;
    }

    private void getEventData(User user, Integer chatId, String[] callbackQuery, String callbackAction) {
        try {
            String messageText = "";
            switch (callbackQuery[1]) {
                case "rocket":
                    if (callbackQuery.length < 3) {
                        messageText += "Sorry, information about the rocket is missing.\n" +
                                "Please try again later \uD83E\uDD7A";
                        break;
                    }
                    String rocketInfo = APIClient.getRocketInfo(callbackQuery[2]);
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
                //TODO get event by id
                case "crew":
                    JSONObject event = new JSONObject(APIClient.getEvent(callbackQuery[2]));
                    JSONArray crew = event.getJSONArray("crew");
                    int crewAmount = crew.length();
                    if (crewAmount == 0) {
                        messageText += "Oops, it seems like there is no information about the crew.\n" +
                                "Please try another time \uD83E\uDD7A";
                        break;
                    }
                    for (int i = 0; i < crew.length(); i++) {
                        String memberId = crew.getJSONObject(i).getString("crew");
                        JSONObject member = new JSONObject(APIClient.getCrewInfo(memberId));
                        String nameCrew = member.getString("name");
                        String role = crew.getJSONObject(i).getString("role");
                        messageText += nameCrew + " is the " + role + ".\n";

                        if (member.has("image") && member.get("image") instanceof String)
                            messageText += "Here is a link to the photo: "
                                    + new URL(member.getString("image")) + "\n";

                        if (member.has("wikipedia") && member.get("wikipedia") instanceof String)
                            messageText += "Here is a link to the wikipedia page: "
                                    + new URL(member.getString("wikipedia")) + "\n";
                    }
                    break;
                case "launchpad":
                    if (callbackQuery.length < 3) {
                        messageText += "Sorry, information about the launchpad is missing.\n" +
                                "Please try again later \uD83E\uDD7A";
                        break;
                    }
                    String launchpadId = callbackQuery[2];
                    String launchpadInfo = APIClient.getLaunchpadInfo(launchpadId);
                    JSONObject launchpad = new JSONObject(launchpadInfo);
                    String nameLaunchpad = launchpad.getString("full_name");
                    int success = launchpad.getJSONArray("launches").length();
                    messageText += nameLaunchpad + " has already " + success + " succeeded launches!";
                    break;
            }
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(messageText);
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
//            askAboutDetails(user, messageText, chatId, callbackAction);
        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("\uD83D\uDE2C Sorry, something went wrong. Please try again later.");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void askAboutDetails(User user, String messageText, Integer chatId, String callbackAction) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(messageText);
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> list = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        String rocketId = "";
        if (user.getEvent().has("rocket") && user.getEvent().get("rocket") instanceof String)
            rocketId = user.getEvent().getString("rocket");
        if (rocketId.equals("")) {
            messageText += "Sorry, information about the rocket is missing.\n" +
                    "Please try again later \uD83E\uDD7A";
        }
        InlineKeyboardButton rocket = new InlineKeyboardButton();
        rocket.setText("ROCKET");
        rocket.setCallbackData(callbackAction + "rocket:" + rocketId);


        InlineKeyboardButton crew = new InlineKeyboardButton();
        crew.setText("CREW");
        //id for crew is the id of event, cause of large data size
        crew.setCallbackData(callbackAction + "crew:" + user.getEvent().getString("id"));

        String launchpadId = "";
        if (user.getEvent().has("launchpad") && user.getEvent().get("launchpad") instanceof String)
            launchpadId = user.getEvent().getString("launchpad");
        if (launchpadId.equals("")) {
            messageText += "Sorry, information about the launchpad is missing.\n" +
                    "Please try again later \uD83E\uDD7A";
        }
        InlineKeyboardButton launchpad = new InlineKeyboardButton();
        launchpad.setText("LAUNCHPAD");
        launchpad.setCallbackData(callbackAction + "launchpad:" + launchpadId);

        rowInline.add(rocket);
        rowInline.add(crew);
        rowInline.add(launchpad);

        rowsInline.add(rowInline);

        List<InlineKeyboardButton> secondRowInline = new ArrayList<>();
        InlineKeyboardButton exit = new InlineKeyboardButton();
        exit.setText("EXIT");
        exit.setCallbackData("EXIT");

        secondRowInline.add(exit);
        rowsInline.add(secondRowInline);
        markupInline.setKeyboard(rowsInline);

        keyboardMarkup.setKeyboard(list);
        message.setReplyMarkup(keyboardMarkup);
        message.setReplyMarkup(markupInline);
        try {
            execute(message); // Call method to send the message
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleUpcomingEvent(Update update, User user, Integer chatId, boolean callback, String[] callbackQuery) {
        try {
            if (!callback) {
                String messageText = "";
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
                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(chatId));
                    message.setText("\uD83D\uDE14 Sorry, I could not find next event. Please try again later.");
                    try {
                        execute(message); // Call method to send the message
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                Long timeStamp = nextEvent.getLong("date_unix");
                Date date = new Date(timeStamp * 1000);
                messageText += "Next launch is on " + date + " with ";
                //gets details and sends the message
                getLaunchDetails(user, messageText, nextEvent, chatId, "upcoming:");
                user.setAction("upcoming:choosing");
            } else if (callbackQuery[0].equalsIgnoreCase("upcoming"))
                getEventData(user, chatId, callbackQuery, "upcoming");
        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("\uD83D\uDE2C Sorry, something went wrong. Please try again later.");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

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
