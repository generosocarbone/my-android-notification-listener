package it.syscake.notificationlistenerlibrary.http;

import android.content.Context;
import android.telephony.TelephonyManager;

import java.util.concurrent.TimeUnit;

import it.syscake.notificationlistenerlibrary.BuildConfig;
import it.syscake.notificationlistenerlibrary.SharedPrefManager;
import it.syscake.notificationlistenerlibrary.model.Message;
import it.syscake.notificationlistenerlibrary.model.MqttNotification;
import it.syscake.notificationlistenerlibrary.utils.CryptoUtils;
import it.systemslab.cryptomodule.DHKEInstance;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static it.syscake.notificationlistenerlibrary.model.MqttNotification.Action.CallEnded;
import static it.syscake.notificationlistenerlibrary.model.MqttNotification.Action.CallRinging;
import static it.syscake.notificationlistenerlibrary.model.MqttNotification.Action.CallStarted;
import static it.syscake.notificationlistenerlibrary.model.MqttNotification.Action.Confirm;

public class NotificationRestClient {

    private final static String TAG = NotificationRestClient.class.getSimpleName();
    private final static String BASE_URL = BuildConfig.LIFESENSOR_BE;
    private final static String POST_NOTIFICATION = BASE_URL + "/api/v1/notification";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    public void sendNotificationToWatch(Message message, Callback callback) {

        RequestBody body = RequestBody.create(
                message.toString(),
                JSON);

        Request request = new Request.Builder()
                .url(POST_NOTIFICATION)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    private static String encrypt(Context context, SharedPrefManager instance, String message) {
        DHKEInstance dhkeInstance = CryptoUtils.getInstance();
        return dhkeInstance.encryptMessage(CryptoUtils.decryptData(instance.getKey(), context),message);
    }

    synchronized public static void confirmNotification(Context context, Callback callback) {
        SharedPrefManager instance = SharedPrefManager.getInstance(context);
        if (instance.getAlias() != null) {

            NotificationRestClient client = new NotificationRestClient();
            client.sendNotificationToWatch(
                new Message(
                    instance.getAlias(),
                    new MqttNotification(
                        encrypt(context, instance, "action"),
                        encrypt(context, instance, "eu.beamdigital.beamwatch.confirm"),
                        encrypt(context, instance, "eu.beamdigital.beamwatch"),
                        0,
                        Confirm
                    )
                ),
                callback
            );
        }
    }

    public void sendOutgoingCallNotification(Context context, Callback callback) {
        sendCallNotification(context, callback, CallStarted, TelephonyManager.CALL_STATE_OFFHOOK);
    }

    public void sendDisconnectedCallNotification(Context context, Callback callback) {
        sendCallNotification(context, callback, CallEnded, TelephonyManager.CALL_STATE_IDLE);
    }

    public void sendIngoingCallNotification(Context context, Callback callback) {
        sendCallNotification(context, callback, CallRinging, TelephonyManager.CALL_STATE_RINGING);
    }

    public void sendCallNotification(Context context, Callback callback, MqttNotification.Action action, int state) {
        SharedPrefManager instance = SharedPrefManager.getInstance(context);
        if (instance.getAlias() != null) {

            NotificationRestClient client = new NotificationRestClient();
            client.sendNotificationToWatch(
                    new Message(instance.getAlias(), new MqttNotification(state, action )),
                    callback
            );
        }
    }
}
