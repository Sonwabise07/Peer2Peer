package com.example.peer2peer; // Ensure this matches your package name

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Import the Toast class

import androidx.appcompat.app.AppCompatActivity;

public class LandingActivity extends AppCompatActivity {

    private static final long BUTTON_ANIMATION_DELAY = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        // Hide the action bar for a full-screen experience
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ImageView appLogo = findViewById(R.id.image_app_logo_landing);
        TextView textWelcome = findViewById(R.id.text_welcome_title);
        TextView textTagline = findViewById(R.id.text_welcome_tagline);
        Button getStartedButton = findViewById(R.id.button_get_started_landing);

        // Hide button initially to animate it in later
        getStartedButton.setVisibility(View.INVISIBLE);

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1000); // 1 second fade in

        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_button);
        slideUp.setDuration(700);


        // Start animations for logo and text
        if (appLogo != null) appLogo.startAnimation(fadeIn);
        textWelcome.startAnimation(fadeIn);
        textTagline.startAnimation(fadeIn);


        // Delay the button appearance for a staggered effect
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            getStartedButton.setVisibility(View.VISIBLE);
            getStartedButton.startAnimation(slideUp);
        }, BUTTON_ANIMATION_DELAY);


        getStartedButton.setOnClickListener(v -> {
            // Show a toast message for immediate user feedback
            Toast.makeText(LandingActivity.this, "Loading...", Toast.LENGTH_SHORT).show();

            // Navigate to the main activity
            Intent intent = new Intent(LandingActivity.this, MainActivity.class);
            startActivity(intent);

            // Finish this activity so the user can't return to it
            finish();
        });
    }
}