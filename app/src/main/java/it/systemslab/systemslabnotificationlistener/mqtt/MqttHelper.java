package it.systemslab.systemslabnotificationlistener.mqtt;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class MqttHelper implements IMqttActionListener, MqttCallbackExtended {

    private MqttAndroidClient mqttAndroidClient;
    private static final String TAG = "MqttHelper";
    private static MqttHelper instance;
    private Cloud2DeviceMessageCallback cloud2DeviceMessageCallback;
    private MqttHelperCallback mqttHelperCallback;
    private final Context context;
    private final AtomicInteger coda = new AtomicInteger(0);
    private final String alias = "JSKXZBCS";

    private MqttHelper(Context context, boolean local) {
        this.context = context;

        String uri = String.format(Locale.ITALIAN, "ws://stream.lifesensor.cloud:9001");

        String clientId = MqttAsyncClient.generateClientId();
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(
                String.format(
                        "%s/BeamWatch/%s",
                        context.getFilesDir().getAbsolutePath(),
                        clientId
                )
        );
        mqttAndroidClient = new MqttAndroidClient(context, uri, clientId, dataStore);
        mqttAndroidClient.setCallback(this);
        connect();
    }

    public static MqttHelper getInstance(Context context, boolean local) {
        if (instance == null) {
            Log.d(TAG, "getInstance: creating new istance");
            instance = new MqttHelper(context, local);
        }
        return instance;
    }

    public void closeConnection() {
        Log.d(TAG, "closeConnection");
        mqttAndroidClient.setCallback(null);
        this.cloud2DeviceMessageCallback = null;
        this.mqttHelperCallback = null;
        mqttAndroidClient.close();
        mqttAndroidClient = null;
        instance = null;
    }

    public void setHelperCallback(MqttHelperCallback callback) {
        this.mqttHelperCallback = callback;
    }

    public void setCloud2DeviceMessageCallback(Cloud2DeviceMessageCallback cloud2DeviceMessageCallback) {
        this.cloud2DeviceMessageCallback = cloud2DeviceMessageCallback;
    }

    public int getCoda() {
        return coda.get();
    }

    private MqttConnectOptions connectOptions;

    private MqttConnectOptions buildMqttConnectOptions() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        // https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html#setConnectionTimeout-int-
        /*
        * Sets the connection timeout value.
        * This value, measured in seconds, defines the maximum time interval the client will wait for the network connection to the MQTT server to be established.
        * The default timeout is 30 seconds.
        * A value of 0 disables timeout processing meaning the client will wait until the network connection is made successfully or fails.
        * */
        mqttConnectOptions.setConnectionTimeout((int) (2.5 * 60));
        /// https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html#setKeepAliveInterval-int-
        /*
        * Sets the "keep alive" interval.
        * This value, measured in seconds, defines the maximum time interval between messages sent or received.
        * It enables the client to detect if the server is no longer available, without having to wait for the TCP/IP timeout.
        * The client will ensure that at least one message travels across the network within each keep alive period.
        * In the absence of a data-related message during the time period, the client sends a very small "ping" message, which the server will acknowledge.
        * A value of 0 disables keepalive processing in the client.
        * */
        mqttConnectOptions.setKeepAliveInterval(5 * 60);
        mqttConnectOptions.setUserName(alias);
        mqttConnectOptions.setPassword("$2a$12$vSiU7exEEw3bB0arm2lbs.NUHi354IRjhe7BuVt..ZVOXQlbabRtC".toCharArray());
        return mqttConnectOptions;
    }

    public void connect() {
        
        if (connectOptions == null)
            connectOptions = buildMqttConnectOptions();

        try {
            mqttAndroidClient.connect(connectOptions, "CONNECT", this);

        } catch (MqttException ex) {
            Log.d(TAG, "connect: mqtt exception: " + ex.toString());
            ex.printStackTrace();
        }
    }

    public void publishTrace(final String strMessage) {
        Log.d(TAG, "publishTrace: message: " + strMessage);
        publish(strMessage, String.format("notification/%s", alias));
    }

    public void publish(final String strMessage, String topic) {
        if (!mqttAndroidClient.isConnected()) {
            Log.d(TAG, "publish: not connected to broker. Message will be sent asap...");
        }

        MqttMessage message = new MqttMessage(strMessage.getBytes());
        message.setQos(1);
        message.setRetained(false);

        try {
            int i = coda.incrementAndGet();
            Log.d(TAG, "publish: coda: " + i);
            mqttAndroidClient.publish(topic, message, "PUBLISH", this);

        } catch (Exception e) {
            Log.d(TAG, "publish: exception 3: " + e.toString());
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return mqttAndroidClient.isConnected();
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        Object userContext = asyncActionToken.getUserContext();
        if (userContext.equals("PUBLISH")) {
            int i = coda.decrementAndGet();
            Log.d(TAG, "onSuccess: message published: " + asyncActionToken.getResponse() + "; coda: " + i);
        } else if (userContext.equals("CONNECT")) {
            Log.d(TAG, "onSuccess: connected");
            DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
            disconnectedBufferOptions.setBufferEnabled(true);
            disconnectedBufferOptions.setBufferSize(100);
            disconnectedBufferOptions.setPersistBuffer(false);
            disconnectedBufferOptions.setDeleteOldestMessages(false);
            mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);

            if (mqttHelperCallback != null)
                mqttHelperCallback.onConnectionSuccess();

        } else if (userContext.equals("SUBSCRIBE")) {
            Log.d(TAG, "onSuccess: MQTT Subscribed!");
        } else if (userContext.equals("SUBSCRIBE-NOTIFICATION")) {
            Log.d(TAG, "onSuccess: MQTT Subscribed SUBSCRIBE-NOTIFICATION!");
        }
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        Object userContext = asyncActionToken.getUserContext();
        if (userContext.equals("PUBLISH")) {
            Log.d(TAG, "publish: onFailure: message push failure: " + exception + "; " + asyncActionToken);
        } else if (userContext.equals("CONNECT")) {
            if (mqttHelperCallback != null)
                mqttHelperCallback.onConnectionFailure();
            Log.d(TAG, String.format("connect: onFailure: MQTT Failed to connect to broker: %s", exception.toString()));
            exception.printStackTrace();
        } else if (userContext.equals("SUBSCRIBE")) {
            Log.d(TAG, "subscribe: onFailure: MQTT Subscribe failed!");
        } else if (userContext.equals("SUBSCRIBE-NOTIFICATION")) {
            Log.d(TAG, "subscribe: onFailure: MQTT SUBSCRIBE-NOTIFICATION failed!");
        }

        if (exception != null)
            exception.printStackTrace();
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.d(TAG, "connectionLost: Connection to broker lost: " + cause);
        if (cause != null) {
            cause.printStackTrace();
        }

        if (mqttHelperCallback != null)
            mqttHelperCallback.onConnectionLost();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        MqttCloudToDeviceMessage c2dMessage = new Gson().fromJson(message.toString(), MqttCloudToDeviceMessage.class);
        Log.d(TAG, "messageArrived: parsed: " + message.toString());
        if (cloud2DeviceMessageCallback != null) {
            cloud2DeviceMessageCallback.newMessage(c2dMessage);
        } else {
            Log.e(TAG, "messageArrived: message not handled");
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG, "deliveryComplete: " + token.getResponse());
    }


    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        Log.d(TAG, "connected complete. Is it a reconnect? " + reconnect);

        if (mqttHelperCallback != null)
            mqttHelperCallback.onConnectionSuccess();
    }
}