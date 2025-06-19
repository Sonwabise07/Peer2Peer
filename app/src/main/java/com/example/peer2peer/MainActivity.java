package com.example.peer2peer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView; 

public class MainActivity extends AppCompatActivity {

    CardView tutorCard;
    CardView tuteeCard;
    TextView adminLoginText; 

  
    public static final String EXTRA_USER_ROLE = "USER_ROLE";
    public static final String ROLE_TUTOR = "Tutor";
    public static final String ROLE_TUTEE = "Tutee";
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tutorCard = findViewById(R.id.card_tutor);
        tuteeCard = findViewById(R.id.card_tutee);
        adminLoginText = findViewById(R.id.text_admin_login);

        tutorCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.putExtra(EXTRA_USER_ROLE, ROLE_TUTOR);
                startActivity(intent);
            }
        });

        tuteeCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.putExtra(EXTRA_USER_ROLE, ROLE_TUTEE);
                startActivity(intent);
            }
        });

        
        adminLoginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             
                Intent intent = new Intent(MainActivity.this, AdminLoginActivity.class);
               
                startActivity(intent);
            }
        });
      
    }
}