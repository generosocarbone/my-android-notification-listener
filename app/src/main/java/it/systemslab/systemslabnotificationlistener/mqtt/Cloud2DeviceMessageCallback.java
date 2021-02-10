package it.systemslab.systemslabnotificationlistener.mqtt;

public interface Cloud2DeviceMessageCallback {
    void newMessage(MqttCloudToDeviceMessage newMessage);
}
