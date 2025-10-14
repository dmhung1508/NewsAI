package com.example.newsai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.*;

import java.util.Arrays;
import android.widget.TextView;
import android.widget.EditText;
import android.text.TextUtils;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 1;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private CallbackManager fbCallbackManager;
    private EditText edtEmail, edtPassword;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(this);
        }

        setContentView(R.layout.activity_login);
        com.facebook.FacebookSdk.sdkInitialize(getApplicationContext());
        com.facebook.appevents.AppEventsLogger.activateApp(getApplication());
        mAuth = FirebaseAuth.getInstance();
        

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        

        findViewById(R.id.btnLogin).setOnClickListener(v -> loginWithEmail());
        

        TextView signbtn = findViewById(R.id.tvSignup);
        signbtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
        

        findViewById(R.id.close_btn).setOnClickListener(v -> finish());
        

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        findViewById(R.id.btnGoogle).setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });


        fbCallbackManager = CallbackManager.Factory.create();
        findViewById(R.id.btnFacebook).setOnClickListener(v -> {
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
            LoginManager.getInstance().registerCallback(fbCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    handleFacebookAccessToken(loginResult.getAccessToken());
                }
                @Override
                public void onCancel() {
                    Toast.makeText(LoginActivity.this, "Facebook login canceled", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onError(FacebookException error) {
                    Toast.makeText(LoginActivity.this, "Facebook login error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fbCallbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    Toast.makeText(this, "Google Sign-In thất bại", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In thất bại", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                goToMain();
            } else {
                Toast.makeText(this, "Google login failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                goToMain();
            } else {
                Toast.makeText(this, "Facebook login failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginWithEmail() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        
        // Validate
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }
        
        // Show loading
        Toast.makeText(this, "Đang đăng nhập...", Toast.LENGTH_SHORT).show();
        
        // Đăng nhập Firebase
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    
                    // Check if email is verified
                    if (user != null && !user.isEmailVerified()) {
                        Toast.makeText(this, 
                            "Email chưa được xác thực!\nVui lòng kiểm tra email và xác thực tài khoản.", 
                            Toast.LENGTH_LONG).show();
                        
                        // Optionally: Resend verification email
                        showResendVerificationOption(user);
                        
                        // Sign out user
                        mAuth.signOut();
                        return;
                    }
                    
                    Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                    goToMain();
                } else {
                    String errorMsg = "Đăng nhập thất bại";
                    if (task.getException() != null) {
                        String exception = task.getException().getMessage();
                        if (exception != null) {
                            if (exception.contains("no user record")) {
                                errorMsg = "Tài khoản không tồn tại";
                            } else if (exception.contains("password is invalid") || exception.contains("wrong-password")) {
                                errorMsg = "Mật khẩu không đúng";
                            } else if (exception.contains("too-many-requests")) {
                                errorMsg = "Quá nhiều lần thử. Vui lòng thử lại sau";
                            }
                        }
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void showResendVerificationOption(FirebaseUser user) {
        // Show dialog to ask if user wants to resend verification email
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Email chưa xác thực")
            .setMessage("Bạn có muốn gửi lại email xác thực không?")
            .setPositiveButton("Gửi lại", (dialog, which) -> {
                user.sendEmailVerification().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Email xác thực đã được gửi!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Không thể gửi email. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Đóng", null)
            .show();
    }

    private void goToMain() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String nameOrEmail = (user.getEmail() != null ? user.getEmail() : user.getDisplayName());
            Toast.makeText(this, "Xin chào: " + nameOrEmail, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}
