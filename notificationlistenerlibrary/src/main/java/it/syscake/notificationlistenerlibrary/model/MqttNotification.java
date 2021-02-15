package it.syscake.notificationlistenerlibrary.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.Calendar;

public class MqttNotification {

    public enum Action {
        Confirm,
        Posted,
        Update,
        Removed,
    }

    private String title;
    private String text;
    private String packageName;
    private int id;
    private Action action;
    private long timestamp;

    public MqttNotification() {
    }

    public MqttNotification(String title, String text, String packageName, int id, Action action) {
        this.title = title;
        this.text = text;
        this.packageName = packageName;
        this.id = id;
        this.action = action;
        this.timestamp = Calendar.getInstance().getTimeInMillis();
    }

    public MqttNotification(int id, Action action) {
        this.id = id;
        this.action = action;
        this.timestamp = Calendar.getInstance().getTimeInMillis();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
