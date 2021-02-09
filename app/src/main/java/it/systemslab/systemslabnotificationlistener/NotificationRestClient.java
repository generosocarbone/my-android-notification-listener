package it.systemslab.systemslabnotificationlistener;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import it.systemslab.systemslabnotificationlistener.model.Message;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NotificationRestClient {

    private final static String TAG = NotificationRestClient.class.getSimpleName();
    private final static String BASE_URL = BuildConfig.LIFESENSOR_BE;
    private final static String POST_CONFIG = BASE_URL + "/api/v0/postNotification";
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
                .url(POST_CONFIG)
                .post(body)
                .build();
        Log.d(TAG, "sendNotificationToWatch: url: " + POST_CONFIG);
        client.newCall(request).enqueue(callback);
    }
}
