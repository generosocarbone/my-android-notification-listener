package eu.beamdigital.lifesensorworkers;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.myLooper())
                .postDelayed(() -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }, 500);
    }
}
