package application;

import client.APIClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class TelegramBot extends TelegramLongPollingBot {
    private String botToken = "2044808909:AAH70a1hUq172F7dfsYQVWrzCHgL1dbYNaE";
    private String botUsername = "SpaceX_Data_Insights_Bot";
    private Map<Long, User> users;

    TelegramBot() {
        super();
        this.users = new HashMap<>();
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Long id = update.getMessage().getFrom().getId();
            User user = users.containsKey(id) ? users.get(id) : new User(id);
            users.put(id, new User(id));
            if (update.getMessage().isCommand()) {
                String command = update.getMessage().getText();
                switch (command) {
                    case "/start" -> handleStart(update, user);
                    case "/upcoming" -> handleUpcomingEvent(update, user);
                }
            }
        }
    }

    private void handleStart(Update update, User user) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(update.getMessage().getChatId()));
//        message.setText(Application.HELP_MESSAGE);
        try {
            execute(message); // Call method to send the message
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleUpcomingEvent(Update update, User user) {
    }
//
}
