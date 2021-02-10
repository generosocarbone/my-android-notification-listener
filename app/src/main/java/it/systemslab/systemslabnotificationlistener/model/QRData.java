package it.systemslab.systemslabnotificationlistener.model;

import com.google.gson.Gson;

public class QRData {
    private final String key;
    private final String alias;

    public QRData(String key, String alias) {
        this.key = key;
        this.alias = alias;
    }

    public String getKey() {
        return key;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this, QRData.class);
    }
}
