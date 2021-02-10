package it.systemslab.systemslabnotificationlistener;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import it.systemslab.cryptomodule.DHKEInstance;
import it.systemslab.systemslabnotificationlistener.listener.NotificationListenerXX;
import it.systemslab.systemslabnotificationlistener.model.Message;
import it.systemslab.systemslabnotificationlistener.model.MqttNotification;
import it.systemslab.systemslabnotificationlistener.model.QRData;
import it.systemslab.systemslabnotificationlistener.mqtt.MqttHelper;
import it.systemslab.systemslabnotificationlistener.mqtt.MqttHelperCallback;
import it.systemslab.systemslabnotificationlistener.utils.CryptoUtils;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity implements ServiceConnection, MqttHelperCallback {

    private final static String TAG = MainActivity.class.getSimpleName();
    private NotificationListenerXX service;

    @ViewById
    TextView alias;

    @AfterViews
    void av(){

        SharedPrefManager instance = SharedPrefManager.getInstance(this);
        alias.setText(String.format("Alias: %s", instance.getAlias()));
        MqttHelper mqttHelper = MqttHelper.getInstance(this, true);
        mqttHelper.setHelperCallback(this);
        mqttHelper.connect();
    }

    @Click(R.id.associazione)
    void associa(){
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 666) {
            if(service.checkNotificationListenerPermission()) {
                if (service.isBound()) {
                    Log.d(TAG, "service bound");
                } else {
                    service.requestRebind();
                }
            } else {
                Toast.makeText(
                        this,
                        "Impossibile avviare il service",
                        Toast.LENGTH_SHORT
                ).show();

            }
        } else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if(result != null) {
                if(result.getContents() == null) {
                    Toast.makeText(this, R.string.cancelled, Toast.LENGTH_LONG).show();
                } else {
                    Log.d(TAG, "onActivityResult: Scanned: " + result.getContents());
                    QRData qrdata = new Gson().fromJson(result.getContents(), QRData.class);
                    String contentAlias = qrdata.getAlias();
                    if(contentAlias == null || contentAlias.equals("") || qrdata.getKey() == null || qrdata.getKey().equals("")) {
                        Toast.makeText(this, R.string.conf_error, Toast.LENGTH_LONG).show();
                        return;
                    }

                    SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
                    prefManager.setAlias(contentAlias);
                    prefManager.setKey(CryptoUtils.encryptData(qrdata.getKey(), this));
                    alias.setText(String.format("Alias: %s", contentAlias));

                    tmpNotification();
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void startService() {
        if(service == null) {
            Intent serviceIntent = new Intent(this, NotificationListenerXX.class);
//            startService(serviceIntent);
            bindService(serviceIntent, this, BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: binded");
        this.service = ((NotificationListenerXX.LocalBinder) iBinder).getService();
        if(service != null) {
            service.publicMethod();
            if(service.checkNotificationListenerPermission()) {
                if (service.isBound()) {
                    Log.d(TAG, "service bound");
                    StatusBarNotification[] activeNotifications = service.getActiveNotifications();
                    if (activeNotifications != null)
                        Log.d(TAG, "service. active notifications: " + activeNotifications.length);
                    else
                        Log.d(TAG, "service. no status bar notifications");
                } else {
                    service.requestRebind();
                }
            } else {
                service.requestRebind();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivityForResult(intent, 666);
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "onServiceDisconnected: unbinded");
        service = null;
    }


    @Override
    public void onConnectionLost() {
        Log.d(TAG, "onConnectionLost: ");
    }

    @Override
    public void onConnectionSuccess() {
        Log.d(TAG, "onConnectionSuccess: ");
        tmpNotification();
    }

    private void tmpNotification() {
        SharedPrefManager instance = SharedPrefManager.getInstance(this);
        if (instance.getAlias() != null) {
            DHKEInstance dhkeInstance = DHKEInstance.getInstance();
            Log.d(TAG, "tmpNotification: getKey: " + instance.getKey());
            Log.d(TAG, "tmpNotification: decryptData: " +CryptoUtils.decryptData(instance.getKey(), this));
            MqttHelper.getInstance(this, true)
                    .publishTrace(
                            new Message(
                                    instance.getAlias(),
                                    new MqttNotification(
                                            dhkeInstance.encryptMessage(CryptoUtils.decryptData(instance.getKey(), this),"title"),
                                            dhkeInstance.encryptMessage(CryptoUtils.decryptData(instance.getKey(), this),"text"),
                                            dhkeInstance.encryptMessage(CryptoUtils.decryptData(instance.getKey(), this), getPackageName())
                                    )
                            ).toString()
                    );
        }
    }

    @Override
    public void onConnectionFailure() {
        Log.d(TAG, "onConnectionFailure: ");
    }
}