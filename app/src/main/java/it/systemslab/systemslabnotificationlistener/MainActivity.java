package it.systemslab.systemslabnotificationlistener;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private final static String TAG = MainActivity.class.getSimpleName();
    private NotificationListenerA service;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if(service.checkNotificationListenerPermission()) {
                service.requestRebind();
            } else {
                Toast.makeText(
                        this,
                        "Impossibile avviare il service",
                        Toast.LENGTH_SHORT
                ).show();

            }
        }
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, NotificationListenerA.class);
        startService(serviceIntent);
        bindService(serviceIntent, this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: binded");
        this.service = ((NotificationListenerA.LocalBinder) iBinder).getService();
        if(service!= null) {
            service.publicMethod();
            if(service.checkNotificationListenerPermission()) {
                service.requestRebind();
                StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
                if (activeNotifications != null)
                    Log.d(TAG, "active notifications: " + activeNotifications.length);
                else
                    Log.d(TAG, "no status bar notifications");

                activeNotifications = service.getActiveNotifications();
                if (activeNotifications != null)
                    Log.d(TAG, "service. active notifications: " + activeNotifications.length);
                else
                    Log.d(TAG, "service. no status bar notifications");
            } else {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivityForResult(intent, 0);
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "onServiceDisconnected: unbinded");
    }


}