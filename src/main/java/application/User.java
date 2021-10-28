package application;

import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;

public class User {
    private String action;
    private final long id;
    private JSONObject event;
    private int pastEventId = 1;
    private JSONArray pastEvents;

    public JSONArray getPastEvents() {
        return pastEvents;
    }

    public void setPastEvents(JSONArray pastEvents) {
        this.pastEvents = pastEvents;
    }

    public int getPastEventId() {
        return pastEventId;
    }

    public void setPastEventId(int pastEventId) {
        this.pastEventId = pastEventId;
    }

    public JSONObject getEvent() {
        return event;
    }

    public void setEvent(JSONObject event) {
        this.event = event;
    }

    public long getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public User(long id) {
        action = "";
        this.id = id;
    }
}