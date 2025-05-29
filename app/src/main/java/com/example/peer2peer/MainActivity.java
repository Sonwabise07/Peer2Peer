package com.example.peer2peer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView; // <<--- Added import for TextView

public class MainActivity extends AppCompatActivity {

    CardView tutorCard;
    CardView tuteeCard;
    TextView adminLoginText; // <<--- Added variable for Admin Login TextView

    // Define constants for roles
    public static final String EXTRA_USER_ROLE = "USER_ROLE";
    public static final String ROLE_TUTOR = "Tutor";
    public static final String ROLE_TUTEE = "Tutee";
    // No constant needed for Admin role here, as we launch a specific activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tutorCard = findViewById(R.id.card_tutor);
        tuteeCard = findViewById(R.id.card_tutee);
        adminLoginText = findViewById(R.id.text_admin_login); // <<--- Find the new TextView by ID

        tutorCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to LoginActivity, passing "Tutor" role
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.putExtra(EXTRA_USER_ROLE, ROLE_TUTOR);
                startActivity(intent);
            }
        });

        tuteeCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to LoginActivity, passing "Tutee" role
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.putExtra(EXTRA_USER_ROLE, ROLE_TUTEE);
                startActivity(intent);
            }
        });

        // **** ADDED OnClickListener for Admin Login TextView ****
        adminLoginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to AdminLoginActivity (We will create this next)
                Intent intent = new Intent(MainActivity.this, AdminLoginActivity.class);
                // No need to pass role for admin login
                startActivity(intent);
            }
        });
        // **** END Added Listener ****
    }
}