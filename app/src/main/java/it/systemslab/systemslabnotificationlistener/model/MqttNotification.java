package it.systemslab.systemslabnotificationlistener.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class MqttNotification {
    private String title;
    private String text;
    private String packageName;

    public MqttNotification() {
    }

    public MqttNotification(String title, String text, String packageName) {
        this.title = title;
        this.text = text;
        this.packageName = packageName;
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
