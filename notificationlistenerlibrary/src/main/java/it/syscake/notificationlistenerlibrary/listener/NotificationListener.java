package it.syscake.notificationlistenerlibrary.listener;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;

import it.syscake.notificationlistenerlibrary.SharedPrefManager;
import it.syscake.notificationlistenerlibrary.http.NotificationRestClient;
import it.syscake.notificationlistenerlibrary.model.Message;
import it.syscake.notificationlistenerlibrary.model.MqttNotification;
import it.syscake.notificationlistenerlibrary.utils.CryptoUtils;
import it.systemslab.cryptomodule.DHKEInstance;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static it.syscake.notificationlistenerlibrary.model.MqttNotification.Action.Removed;

public class NotificationListener extends NotificationListenerService implements Callback, PhoneCallListener.PhoneCallInterface {

    private final static String TAG = NotificationListener.class.getSimpleName();
    private final LocalBinder binder = new LocalBinder();
    private final NotificationRestClient client = new NotificationRestClient();
    private boolean bound = false;
    private final HashSet<Integer> set = new HashSet<>();
    private final SharedPrefManager sharedPrefManager = SharedPrefManager.getInstance(this);
    private TelephonyManager manager;
    private final PhoneCallListener phoneCallListener = new PhoneCallListener();

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        Log.d(TAG, "onFailure: call: " + e.toString());
        e.printStackTrace();
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
        Log.d(TAG, "onResponse: body: " + Objects.requireNonNull(response.body()).string());

    }

    @Override
    public void outgoingCallStarted() {
        Log.d(TAG, "outgoingCallStarted");
        client.sendOutgoingCallNotification(this, this);
    }

    @Override
    public void disconnected() {
        Log.d(TAG, "disconnected");
        client.sendDisconnectedCallNotification(this, this);
    }

    @Override
    public void ingoingCallStarted() {
        Log.d(TAG, "ingoingCallStarted");
        client.sendIngoingCallNotification(this, this);
    }

    public class LocalBinder extends Binder {
        public NotificationListener getService() {
            return NotificationListener.this;
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
        manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (manager != null) {
            phoneCallListener.setListener(this);
            manager.listen(phoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
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

    private String encrypt(String clearText) {
        String encrypted = DHKEInstance.getInstance().encryptMessage(
            CryptoUtils.decryptData(SharedPrefManager.getInstance(this).getKey(), this),
            clearText
        );

        if (encrypted == null)
            encrypted = "";

        return encrypted;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        Notification notification = sbn.getNotification();
        Log.d(TAG, "onNotificationPosted: id: " + sbn.getId());
        if (notification != null) {
            Bundle extras = notification.extras;
            // interesting extras
            // sbn.getPackageName()
            // android.title
            // android.text
            if (extras != null) {
                MqttNotification.Action action;

                if (set.add(sbn.getId())) {
                    action = MqttNotification.Action.Posted;
                } else {
                    action = MqttNotification.Action.Update;
                }

                Log.d(TAG, "onNotificationPosted: active: " + set.size());
                String packageName = sbn.getPackageName();
                if (!sharedPrefManager.getPackageEnabled(packageName)) {
                    Log.d(TAG, "onNotificationPosted: avoiding " + packageName);
                } else {
                    try {
                        ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                        packageName = applicationInfo.loadLabel(getPackageManager()).toString();
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.d(TAG, "onNotificationPosted: e: " + e.toString());
                        e.printStackTrace();
                    }
                    packageName = encrypt(packageName);

                    ///
                    for (String k : extras.keySet())
                       Log.d(TAG, "onNotificationPosted: " + k + ": " + extras.get(k));

                    String title = encrypt(getStringFromExtras(extras, "android.title"));
                    String text = encrypt(getStringFromExtras(extras, "android.text"));

                    SharedPrefManager instance = SharedPrefManager.getInstance(this);
                    Message m = new Message(
                        instance.getAlias(),
                        new MqttNotification(title, text, packageName, sbn.getId(), action)
                    );

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

        set.remove(sbn.getId());
        Log.d(TAG, "onNotificationRemoved: id: " + sbn.getId() + "; active: " + set.size());
        client.sendNotificationToWatch(
            new Message(
                SharedPrefManager.getInstance(this).getAlias(),
                new MqttNotification(sbn.getId(), Removed)
            ),
            this
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destroyed");
        if (manager != null) {
            manager.listen(phoneCallListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    public void publicMethod(){
        Log.d(TAG, "publicMethod: this is a public method");
    }

    public boolean checkNotificationListenerPermission() {
        Log.d(TAG, "checking notification permissions");
        ComponentName cn = new ComponentName(this, NotificationListener.class);
        String flat = Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    public void requestRebind() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(TAG, "requesting rebind");
            requestRebind(new ComponentName(this, NotificationListener.class));
        }
    }

    public boolean isBound() {
        return bound;
    }

    public NotificationListener() {
        super();
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