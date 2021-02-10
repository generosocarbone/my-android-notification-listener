package it.systemslab.systemslabnotificationlistener.mqtt;

public interface MqttHelperCallback {
    void onConnectionLost();

    void onConnectionSuccess();

    void onConnectionFailure();
}
