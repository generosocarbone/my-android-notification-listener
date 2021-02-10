package it.systemslab.systemslabnotificationlistener.listener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    public static final String TAG = BootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.getAction());
        Intent serviceIntent = new Intent(context, NotificationListenerXX.class);
        context.startService(serviceIntent);
    }
}
