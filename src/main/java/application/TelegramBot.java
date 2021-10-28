package application;

import client.APIClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
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
        if (update.hasMessage()) {
            Long id = update.getMessage().getFrom().getId();
            if (!users.containsKey(id))
                users.put(id, new User(id));
            User user = users.get(id);
            if (update.getMessage().isCommand()) {
                String command = update.getMessage().getText();
                switch (command) {
                    case START_COMMAND -> handleStart(update, user);
                    case UPCOMING_EVENT_COMMAND -> handleUpcomingEvent(update, user);
                    case PAST_EVENTS_COMMAND -> handlePastEvents(update, user);
                    case FACT_COMMAND -> handleFact(update, user);
                    default -> {
                        SendMessage message = new SendMessage();
                        message.setChatId(String.valueOf(update.getMessage().getChatId()));
                        message.setText("\uD83D\uDE2E Sorry I don't know this command... Try another one");
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
                }
            }
        } else if (update.hasCallbackQuery()) {
            User user = users.get(update.getCallbackQuery().getMessage().getFrom());
            if (!user.getAction().equals("")) {
                if (user.getAction().contains("upcoming:")) handleUpcomingEvent(update, user);
                else if (user.getAction().contains("past:")) handlePastEvents(update, user);
            }
        }
    }

    private void handleFact(Update update, User user) {
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
            message.setText("\uD83D\uDE2C Sorry, something went wrong. Please try again later.");
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
                getNextFivePastEvents(update, user, messageText, past);
                user.setAction("past:ask");
            } else if (user.getAction().equals("past:ask")) {
                switch (update.getCallbackQuery().getData()) {
                    case "YES":
                        String messageText = "";
                        JSONArray past = user.getPastEvents();
                        getNextFivePastEvents(update, user, messageText, past);
                    case "NO":
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
                        user.setAction("past:ask:getNumber");
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
                        Date date = new Date(timeStamp * 1000);
                        messageText += "The launch was on " + date + " with ";
                        getLaunchDetails(update, user, messageText, event);
                        user.setAction("past:ask:getNumber:choosing");
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
            message.setText("\uD83D\uDE2C Sorry, something went wrong. Please try again later.");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void getLaunchDetails(Update update, User user, String messageText, JSONObject event)
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
        askAboutDetails(update, messageText);
    }

    private void getNextFivePastEvents(Update update, User user, String messageText, JSONArray past) {
        int count = user.getPastEventId();
        for (int i = past.length() - user.getPastEventId(); i >= 0; i--) {
            JSONObject event = past.getJSONObject(i);
            Long timeStamp = event.getLong("date_unix");
            Date date = new Date(timeStamp * 1000);
            messageText += count + ". " + "The launch was on " + date + " with ";
            messageText = getCrewAmountString(messageText, event);
            if (count == past.length()) {
                messageText += "Those are all the events I could find.";
                user.setPastEventId(count);
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
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();

                InlineKeyboardButton yes = new InlineKeyboardButton();
                yes.setText("YES");
                yes.setCallbackData("YES");

                InlineKeyboardButton no = new InlineKeyboardButton();
                no.setText("NO");
                no.setCallbackData("NO");
                rowInline.add(yes);
                rowInline.add(no);
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
//                row.add("YES");
//                row.add("NO");
                list.add(row);
                keyboardMarkup.setKeyboard(list);
                message.setReplyMarkup(keyboardMarkup);
                message.setReplyMarkup(markupInline);
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
            messageText += "The description to this event is:\n" + event.getString("details");
        }
        return messageText;
    }

    private void getEventData(Update update, User user) {
        try {
            JSONObject event = user.getEvent();
            JSONArray crew = event.getJSONArray("crew");
            int crewAmount = crew.length();

            String messageText = "";
            switch (update.getCallbackQuery().getData()) {
                case "ROCKET":
                    String rocketId = "";
                    if (event.has("rocket") && event.get("rocket") instanceof String)
                        rocketId = event.getString("rocket");
                    if (rocketId.equals("")) {
                        messageText += "Sorry, information about the rocket is missing.\n" +
                                "Please try again later \uD83E\uDD7A";
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
                                    + new URL(member.getString("image") + "\n");

                        if (member.has("wikipedia") && member.get("wikipedia") instanceof String)
                            messageText += "Here is a link to the wikipedia page: "
                                    + new URL(member.getString("wikipedia") + "\n");
                    }
                    break;
                case "LAUNCHPAD":
                    String launchpadId = "";
                    if (event.has("launchpad") && event.get("launchpad") instanceof String)
                        launchpadId = event.getString("launchpad");
                    if (launchpadId.equals("")) {
                        messageText += "Sorry, information about the launchpad is missing.\n" +
                                "Please try again later \uD83E\uDD7A";
                        break;
                    }
                    String launchpadInfo = APIClient.getLaunchpadInfo(launchpadId);
                    JSONObject launchpad = new JSONObject(launchpadInfo);
                    String nameLaunchpad = launchpad.getString("full_name");
                    int success = launchpad.getJSONArray("launches").length();
                    messageText += nameLaunchpad + " has already " + success + " succeeded launches!";
                    break;
            }
            askAboutDetails(update, messageText);
        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(update.getMessage().getChatId()));
            message.setText("\uD83D\uDE2C Sorry, something went wrong. Please try again later.");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void askAboutDetails(Update update, String messageText) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(update.getMessage().getChatId()));
        message.setText(messageText);
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> list = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton rocket = new InlineKeyboardButton();
        rocket.setText("ROCKET");
        rocket.setCallbackData("ROCKET");

        InlineKeyboardButton crew = new InlineKeyboardButton();
        crew.setText("CREW");
        crew.setCallbackData("CREW");

        InlineKeyboardButton launchpad = new InlineKeyboardButton();
        launchpad.setText("LAUNCHPAD");
        launchpad.setCallbackData("LAUNCHPAD");

        rowInline.add(rocket);
        rowInline.add(crew);
        rowInline.add(launchpad);

        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);

//        row.add("ROCKET");
//        row.add("CREW");
//        row.add("LAUNCHPAD");
        row.add("EXIT");
        list.add(row);

        keyboardMarkup.setKeyboard(list);
        message.setReplyMarkup(keyboardMarkup);
        message.setReplyMarkup(markupInline);
        try {
            execute(message); // Call method to send the message
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleUpcomingEvent(Update update, User user) {
        try {
            if (user.getAction().equals("")) {
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
                    message.setChatId(String.valueOf(update.getMessage().getChatId()));
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
                getLaunchDetails(update, user, messageText, nextEvent);
                user.setAction("upcoming:choosing");
            } else if (user.getAction().equals("upcoming:choosing"))
                getEventData(update, user);
        } catch (IOException error) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(update.getMessage().getChatId()));
            message.setText("\uD83D\uDE2C Sorry, something went wrong. Please try again later.");
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
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
//            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
//            keyboardMarkup.setResizeKeyboard(true);
//            keyboardMarkup.setOneTimeKeyboard(true);
//            List<KeyboardRow> list = new ArrayList<>();
//            KeyboardRow row = new KeyboardRow();
//            row.add("EXIT");
//            list.add(row);
//            keyboardMarkup.setKeyboard(list);
//            message.setReplyMarkup(keyboardMarkup);
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}
