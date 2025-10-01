package com.example.newsai;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
//            startActivity(intent);
//            finish();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {

                startActivity(new Intent(this, MainActivity.class));
            } else {

                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
        },3000);
    }




}
