package it.systemslab.systemslabnotificationlistener.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

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

    public MqttNotification() {
    }

    public MqttNotification(String title, String text, String packageName, int id, Action action) {
        this.title = title;
        this.text = text;
        this.packageName = packageName;
        this.id = id;
        this.action = action;
    }

    public MqttNotification(int id, Action action) {
        this.id = id;
        this.action = action;
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

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
