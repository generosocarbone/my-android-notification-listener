package it.systemslab.systemslabnotificationlistener.mqtt;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class MqttCloudToDeviceMessage {

    private MqttEnum.CommandType commandType;
    private MqttCloudToDeviceAdditionalData additionalData;

    public MqttEnum.CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(MqttEnum.CommandType commandType) {
        this.commandType = commandType;
    }

    public MqttCloudToDeviceAdditionalData getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(MqttCloudToDeviceAdditionalData additionalData) {
        this.additionalData = additionalData;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
