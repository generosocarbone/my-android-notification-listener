package it.systemslab.systemslabnotificationlistener.mqtt;


public class MqttEnum {
    public enum CommandType {
        Sync,
        Alarm
    }

    public enum CommandDanger {
        Flooding,
        GasDetected,
        NoiseDetected,
        MaximumCapacity,
        GoToCabin,
        BackToCabin,
        EvacuationOffices,
        GenericAlert,
        BackToOffices,
        Ring,
        Infortunio,
        stopLavori,
        ProhibitedZone,
    }

}
