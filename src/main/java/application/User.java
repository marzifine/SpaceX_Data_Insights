package application;

import org.json.JSONArray;
import org.json.JSONObject;

public class User {
    /**
     * User has id, last researched event,
     * an id of last visible past event and an array of past events
     */
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

    public User(long id) {
        this.id = id;
    }
}
