package it.systemslab.systemslabnotificationlistener.mqtt;

import com.google.gson.annotations.SerializedName;

public class MqttCloudToDeviceAdditionalData {

    private MqttEnum.CommandDanger danger;
    private Integer area;
    @SerializedName("sub_zone")
    private String collectionPoint;
    private String alias;
    @SerializedName("time_spent")
    private Integer timeSpent;

    public MqttEnum.CommandDanger getDanger() {
        return danger;
    }

    public void setDanger(MqttEnum.CommandDanger danger) {
        this.danger = danger;
    }

    public Integer getArea() {
        return area;
    }

    public void setArea(Integer area) {
        this.area = area;
    }

    public String getCollectionPoint() {
        return collectionPoint;
    }

    public void setCollectionPoint(String collectionPoint) {
        this.collectionPoint = collectionPoint;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Integer getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(Integer timeSpent) {
        this.timeSpent = timeSpent;
    }
}
