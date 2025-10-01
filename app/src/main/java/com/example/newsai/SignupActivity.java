package com.example.newsai;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.*;

import java.util.Arrays;

public class SignupActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 1;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private CallbackManager fbCallbackManager;
    private EditText edtName, edtEmail, edtPassword, edtConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(this);
        }
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirm = findViewById(R.id.edtConfirm);

        findViewById(R.id.btnRegister).setOnClickListener(v -> registerWithEmail());
        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        // Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        findViewById(R.id.btnGoogle).setOnClickListener(v ->
                startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN));

        // Facebook
        com.facebook.FacebookSdk.sdkInitialize(getApplicationContext());
        fbCallbackManager = CallbackManager.Factory.create();
        findViewById(R.id.btnFacebook).setOnClickListener(v ->
                LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile")));
        LoginManager.getInstance().registerCallback(fbCallbackManager,
                new com.facebook.FacebookCallback<LoginResult>() {
                    @Override public void onSuccess(LoginResult result) {
                        handleFacebookAccessToken(result.getAccessToken());
                    }
                    @Override public void onCancel() {
                        Toast.makeText(SignupActivity.this, "Facebook hủy", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onError(com.facebook.FacebookException error) {
                        Toast.makeText(SignupActivity.this, "Facebook lỗi", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerWithEmail() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();
        String confirm = edtConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
            TextUtils.isEmpty(pass) || !pass.equals(confirm) || pass.length() < 6) {
            Toast.makeText(this, "Vui lòng nhập đúng thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    user.updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build());
                }
                Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                goToMain();
            } else {
                Toast.makeText(this, "Đăng ký thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fbCallbackManager.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google đăng ký thất bại", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        mAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) goToMain();
                    else Toast.makeText(this, "Google đăng ký thất bại", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        mAuth.signInWithCredential(FacebookAuthProvider.getCredential(token.getToken()))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) goToMain();
                    else Toast.makeText(this, "Facebook đăng ký thất bại", Toast.LENGTH_SHORT).show();
                });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
