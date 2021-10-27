package application;

public class User {
    private String action;
    private final int id;

    public int getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public User(int id) {
        action = "";
        this.id = id;
    }
}
