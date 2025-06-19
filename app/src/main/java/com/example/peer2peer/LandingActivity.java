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

import androidx.appcompat.app.AppCompatActivity;

public class LandingActivity extends AppCompatActivity {

    private static final long BUTTON_ANIMATION_DELAY = 1000; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

       
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ImageView appLogo = findViewById(R.id.image_app_logo_landing);
        TextView textWelcome = findViewById(R.id.text_welcome_title);
        TextView textTagline = findViewById(R.id.text_welcome_tagline);
        Button getStartedButton = findViewById(R.id.button_get_started_landing);

       
        getStartedButton.setVisibility(View.INVISIBLE);

      
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1000); // 1 second fade in

        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_button); 
        slideUp.setDuration(700);


       
        if (appLogo != null) appLogo.startAnimation(fadeIn);
        textWelcome.startAnimation(fadeIn);
        textTagline.startAnimation(fadeIn);


       
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            getStartedButton.setVisibility(View.VISIBLE);
            getStartedButton.startAnimation(slideUp);
        }, BUTTON_ANIMATION_DELAY);


        getStartedButton.setOnClickListener(v -> {
           
            Intent intent = new Intent(LandingActivity.this, MainActivity.class);
            startActivity(intent);
          
            finish();
        });
    }
}
