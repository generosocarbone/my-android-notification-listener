package it.systemslab.systemslabnotificationlistener.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class Message {
    private String alias;
    private Notification notification;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
