package com.kiosk.webviewlauncher;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "KioskPrefs";
    private static final String PREF_URL = "web_url";
    private static final String PREF_PASSWORD_HASH = "password_hash";
    private static final String DEFAULT_URL = "https://www.nikolaindustry.com";
    private static final String DEFAULT_PASSWORD = "12345";

    private EditText passwordEditText;
    private Button verifyButton;
    private LinearLayout urlConfigContainer;
    private EditText urlEditText;
    private Button saveButton;
    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private Button changePasswordButton;
    private boolean isAuthenticated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        passwordEditText = findViewById(R.id.password_edit_text);
        verifyButton = findViewById(R.id.verify_button);
        urlConfigContainer = findViewById(R.id.url_config_container);
        urlEditText = findViewById(R.id.url_edit_text);
        saveButton = findViewById(R.id.save_button);
        newPasswordEditText = findViewById(R.id.new_password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        changePasswordButton = findViewById(R.id.change_password_button);

        // Initialize default password if not set
        initializeDefaultPassword();

        // Hide URL configuration initially
        urlConfigContainer.setVisibility(View.GONE);

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPassword();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUrl();
            }
        });

        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });
    }

    private void initializeDefaultPassword() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String storedHash = prefs.getString(PREF_PASSWORD_HASH, null);
        
        // If no password is set, initialize with default password
        if (storedHash == null) {
            String defaultHash = hashPassword(DEFAULT_PASSWORD);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_PASSWORD_HASH, defaultHash);
            editor.apply();
        }
    }

    private void verifyPassword() {
        String enteredPassword = passwordEditText.getText().toString().trim();
        
        if (enteredPassword.isEmpty()) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String storedHash = prefs.getString(PREF_PASSWORD_HASH, null);
        String enteredHash = hashPassword(enteredPassword);

        if (storedHash != null && storedHash.equals(enteredHash)) {
            // Password correct
            isAuthenticated = true;
            passwordEditText.setVisibility(View.GONE);
            verifyButton.setVisibility(View.GONE);
            urlConfigContainer.setVisibility(View.VISIBLE);
            loadCurrentUrl();
            Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show();
        } else {
            // Password incorrect
            isAuthenticated = false;
            passwordEditText.setText("");
            Toast.makeText(this, "Incorrect password. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadCurrentUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentUrl = prefs.getString(PREF_URL, DEFAULT_URL);
        urlEditText.setText(currentUrl);
    }

    private void saveUrl() {
        if (!isAuthenticated) {
            Toast.makeText(this, "Please authenticate first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String url = urlEditText.getText().toString().trim();
        
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_URL, url);
        editor.apply();

        Toast.makeText(this, "URL saved! Returning to app...", 
                Toast.LENGTH_SHORT).show();
        finish();
    }

    private void changePassword() {
        if (!isAuthenticated) {
            Toast.makeText(this, "Please authenticate first", Toast.LENGTH_SHORT).show();
            return;
        }

        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Validation
        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 4) {
            Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_LONG).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match. Please try again.", Toast.LENGTH_LONG).show();
            confirmPasswordEditText.setText("");
            return;
        }

        // Save new password
        String newPasswordHash = hashPassword(newPassword);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_PASSWORD_HASH, newPasswordHash);
        editor.apply();

        // Clear password fields
        newPasswordEditText.setText("");
        confirmPasswordEditText.setText("");

        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
