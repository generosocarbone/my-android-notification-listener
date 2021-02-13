package it.syscake.notificationlistenerlibrary.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class Message {

    private String alias;
    private MqttNotification notification;

    public Message() {
    }

    public Message(String alias, MqttNotification notification) {
        this.alias = alias;
        this.notification = notification;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public MqttNotification getNotification() {
        return notification;
    }

    public void setNotification(MqttNotification notification) {
        this.notification = notification;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
