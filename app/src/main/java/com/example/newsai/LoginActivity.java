package com.example.newsai;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        TextView signbtn= findViewById(R.id.tvSignup);
        signbtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView edtEmail = findViewById(R.id.edtEmail);
        TextView edtPassword = findViewById(R.id.edtPassword);
        btnLogin.setOnClickListener(v -> {
            String user = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            if (user.equals("admin") && password.equals("password")) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // đóng login
            } else {
                Toast.makeText(LoginActivity.this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
            }
        });

        }
    }






