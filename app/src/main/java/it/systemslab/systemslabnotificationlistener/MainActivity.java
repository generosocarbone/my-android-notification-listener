package it.systemslab.systemslabnotificationlistener;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

import it.systemslab.cryptomodule.DHKEInstance;
import it.systemslab.systemslabnotificationlistener.listener.NotificationListener;
import it.systemslab.systemslabnotificationlistener.model.Message;
import it.systemslab.systemslabnotificationlistener.model.MqttNotification;
import it.systemslab.systemslabnotificationlistener.model.QRData;
import it.systemslab.systemslabnotificationlistener.utils.CryptoUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static it.systemslab.systemslabnotificationlistener.model.MqttNotification.Action.Confirm;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity implements ServiceConnection, Callback {

    private final static String TAG = MainActivity.class.getSimpleName();
    private NotificationListener service;

    @ViewById
    TextView alias;

    @AfterViews
    void av(){
        SharedPrefManager instance = SharedPrefManager.getInstance(this);
        alias.setText(String.format("Alias: %s", instance.getAlias()));
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

                    confirmNotification();
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void startService() {
        if(service == null) {
            Intent serviceIntent = new Intent(this, NotificationListener.class);
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
        this.service = ((NotificationListener.LocalBinder) iBinder).getService();
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

    private void confirmNotification() {
        SharedPrefManager instance = SharedPrefManager.getInstance(this);
        if (instance.getAlias() != null) {
            DHKEInstance dhkeInstance = DHKEInstance.getInstance();
            NotificationRestClient client = new NotificationRestClient();
            client.sendNotificationToWatch(
                new Message(
                    instance.getAlias(),
                    new MqttNotification(
                        dhkeInstance.encryptMessage(CryptoUtils.decryptData(instance.getKey(), this),"action"),
                        dhkeInstance.encryptMessage(CryptoUtils.decryptData(instance.getKey(), this),"eu.beamdigital.beamwatch.confirm"),
                        dhkeInstance.encryptMessage(CryptoUtils.decryptData(instance.getKey(), this), "eu.beamdigital.beamwatch"),
                        0,
                        Confirm
                    )
                ),
            this
            );
        }
    }

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        Log.d(TAG, "onFailure: " + e.toString());
        e.printStackTrace();
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
        Log.d(TAG, "onResponse: " + Objects.requireNonNull(response.body()).string());
    }
}