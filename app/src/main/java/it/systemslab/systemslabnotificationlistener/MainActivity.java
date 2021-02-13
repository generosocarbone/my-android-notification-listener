package it.systemslab.systemslabnotificationlistener;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import it.syscake.notificationlistenerlibrary.SharedPrefManager;
import it.syscake.notificationlistenerlibrary.listener.NotificationListener;
import it.syscake.notificationlistenerlibrary.model.QRData;
import it.syscake.notificationlistenerlibrary.utils.CryptoUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static it.syscake.notificationlistenerlibrary.http.NotificationRestClient.confirmNotification;


@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity implements ServiceConnection, Callback {

    private final static String TAG = MainActivity.class.getSimpleName();
    private NotificationListener service;

    @ViewById
    RecyclerView packages_rv;

    @ViewById
    TextView alias;

    @AfterViews
    void av(){
        SharedPrefManager instance = SharedPrefManager.getInstance(this);
        alias.setText(String.format("Alias: %s", instance.getAlias()));
        startService();

        final PackageManager pm = getPackageManager();
        PackagesAdapter adapter = new PackagesAdapter(this);
        packages_rv.setLayoutManager(new LinearLayoutManager(this));
        packages_rv.setAdapter(adapter);
        adapter.setDataset(pm.getInstalledApplications(PackageManager.GET_META_DATA));
    }

    private boolean isSystemPackage(int flags) {
        return ((flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    @Click(R.id.associazione)
    void associa(){
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

                    confirmNotification(this, this);
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