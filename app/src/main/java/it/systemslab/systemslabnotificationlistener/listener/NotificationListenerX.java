package it.systemslab.systemslabnotificationlistener.listener;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import it.systemslab.cryptomodule.DHKEInstance;
import it.systemslab.systemslabnotificationlistener.NotificationRestClient;
import it.systemslab.systemslabnotificationlistener.SharedPrefManager;
import it.systemslab.systemslabnotificationlistener.model.Message;
import it.systemslab.systemslabnotificationlistener.model.MqttNotification;
import it.systemslab.systemslabnotificationlistener.utils.CryptoUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class NotificationListenerX extends NotificationListenerService implements Callback {

    private final static String TAG = NotificationListenerX.class.getSimpleName();
    private final LocalBinder binder = new LocalBinder();
    private final NotificationRestClient client = new NotificationRestClient();
    private boolean bound = false;

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        Log.d(TAG, "onFailure: call: " + e.toString());
        e.printStackTrace();
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
        Log.d(TAG, "onResponse: on response");
    }

    public class LocalBinder extends Binder {
        public NotificationListenerX getService() {
            return NotificationListenerX.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onBind: action: " + action);
        if(SERVICE_INTERFACE.equals(action)){
            bound = true;
            return super.onBind(intent);
        } else {
            return binder;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "service started");
        requestRebind();
        return START_STICKY;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "onListenerConnected: ");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "onListenerDisconnected: ");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Notification notification = sbn.getNotification();
        if (notification != null) {
            Bundle extras = notification.extras;
            // interesting extras
            // sbn.getPackageName()
            // android.title
            // android.text
            if(extras != null) {
                String packageName = sbn.getPackageName();
                if (packageName.equals("com.android.systemui")) {
                    Log.d(TAG, "onNotificationPosted: avoiding " + packageName);
                } else {
                    packageName = DHKEInstance.getInstance().encryptMessage(
                            CryptoUtils.decryptData(SharedPrefManager.getInstance(this).getKey(), this),
                            sbn.getPackageName()
                    );

                    for (String k : extras.keySet())
                        Log.d(TAG, "onNotificationPosted: " + k + ": " + extras.get(k));

                    if (packageName == null)
                        packageName = "";

                    String title = getStringFromExtras(extras, "android.title");
                    title = DHKEInstance.getInstance().encryptMessage(
                            CryptoUtils.decryptData(SharedPrefManager.getInstance(this).getKey(), this),
                            title
                    );

                    String text = getStringFromExtras(extras, "android.text");
                    text = DHKEInstance.getInstance().encryptMessage(
                            CryptoUtils.decryptData(SharedPrefManager.getInstance(this).getKey(), this),
                            text
                    );

                    Log.d(TAG, String.format("new notification from %s:\n%s\n%s", packageName, title, text));

                    SharedPrefManager instance = SharedPrefManager.getInstance(this);
                    Message m = new Message(
                            instance.getAlias(),
                            new MqttNotification(title, text, packageName)
                    );

                    Log.d(TAG, "onNotificationPosted: " + m.toString());
                    client.sendNotificationToWatch(m, this);
                }
            } else {
                Log.d(TAG, "onNotificationPosted: no extras");
            }
        } else {
            Log.d(TAG, "onNotificationPosted: no notification");
        }
    }

    private String getStringFromExtras(Bundle extras, String key) {
        String extraString = "";
        try {
            if(extras.get(key) instanceof String) {
                extraString = extras.getString(key);
            } else if (extras.get(key) instanceof SpannableString) {
                SpannableString ss = (SpannableString) extras.get(key);
                extraString = ss.toString();
            } else {
                extraString = "";
            }
        } catch (Exception e) {
            Log.d(TAG, "getStringFromExtras: error: " + e.toString());
            e.printStackTrace();
        }

        return  extraString;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        Log.d(TAG, "onNotificationRemoved: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destroyed");
    }

    /// public method
    public void publicMethod(){
        Log.d(TAG, "publicMethod: this is a public method");
    }

    public boolean checkNotificationListenerPermission() {
        Log.d(TAG, "checking notification permissions");
        ComponentName cn = new ComponentName(this, NotificationListenerX.class);
        String flat = Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    public void requestRebind() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(TAG, "requesting rebind");
            requestRebind(new ComponentName(this, NotificationListenerX.class));
        }
    }

    public boolean isBound() {
        return bound;
    }

    public NotificationListenerX() {
        super();
    }

    private void tmpNotification(String title, String text, String packageName) {
        SharedPrefManager instance = SharedPrefManager.getInstance(this);
        if (instance.getAlias() != null) {
            DHKEInstance dhkeInstance = DHKEInstance.getInstance();
            NotificationRestClient client = new NotificationRestClient();
            client.sendNotificationToWatch(
                    new Message(
                            instance.getAlias(),
                            new MqttNotification(
                                    dhkeInstance.encryptMessage(CryptoUtils.decryptData(instance.getKey(), this), title),
                                    dhkeInstance.encryptMessage(CryptoUtils.decryptData(instance.getKey(), this), text),
                                    dhkeInstance.encryptMessage(CryptoUtils.decryptData(instance.getKey(), this), packageName)
                            )
                    ),
                    this
            );

            Log.d(TAG, "tmpNotification: getKey: " + instance.getKey());
            Log.d(TAG, "tmpNotification: decryptData: " +CryptoUtils.decryptData(instance.getKey(), this));
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "attachBaseContext: ");
    }

    @Override
    public void onListenerHintsChanged(int hints) {
        super.onListenerHintsChanged(hints);
        Log.d(TAG, "onListenerHintsChanged: " + hints);
    }

    @Override
    public void onSilentStatusBarIconsVisibilityChanged(boolean hideSilentStatusIcons) {
        super.onSilentStatusBarIconsVisibilityChanged(hideSilentStatusIcons);
        Log.d(TAG, "onSilentStatusBarIconsVisibilityChanged: " + hideSilentStatusIcons);
    }

    @Override
    public void onNotificationChannelModified(String pkg, UserHandle user, NotificationChannel channel, int modificationType) {
        super.onNotificationChannelModified(pkg, user, channel, modificationType);
        Log.d(TAG, "onNotificationChannelModified: ");
    }

    @Override
    public void onNotificationChannelGroupModified(String pkg, UserHandle user, NotificationChannelGroup group, int modificationType) {
        super.onNotificationChannelGroupModified(pkg, user, group, modificationType);
        Log.d(TAG, "onNotificationChannelGroupModified: ");
    }

    @Override
    public void onInterruptionFilterChanged(int interruptionFilter) {
        super.onInterruptionFilterChanged(interruptionFilter);
        Log.d(TAG, "onInterruptionFilterChanged: ");
    }

    @Override
    public StatusBarNotification[] getActiveNotifications() {
        Log.d(TAG, "getActiveNotifications: ");
        return super.getActiveNotifications();
    }

    @Override
    public StatusBarNotification[] getActiveNotifications(String[] keys) {
        Log.d(TAG, "getActiveNotifications: ");
        return super.getActiveNotifications(keys);
    }

    @Override
    public RankingMap getCurrentRanking() {
        Log.d(TAG, "getCurrentRanking: ");
        return super.getCurrentRanking();
    }
}