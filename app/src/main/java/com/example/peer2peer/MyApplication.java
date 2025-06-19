package com.example.peer2peer;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull; 


import com.stripe.android.PaymentConfiguration;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication_StripeInit";

    @Override
    public void onCreate() {
        super.onCreate();

        String stripePublishableKey = ""; 

        try {
           
            stripePublishableKey = getResources().getString(R.string.stripe_publishable_key);
           
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL ERROR: Failed to read stripe_publishable_key from resources. " +
                    "Ensure secrets.xml exists in res/values and contains the key.", e);
          
            return;
        }


       
        if (stripePublishableKey == null || stripePublishableKey.isEmpty()) {
            Log.e(TAG, "CRITICAL ERROR: Stripe Publishable Key is missing or empty in resources. " +
                    "Ensure secrets.xml contains 'stripe_publishable_key'.");
            return; 
        }

       
        if (stripePublishableKey.equals("pk_test_YOUR_ACTUAL_PUBLISHABLE_KEY")) {
            Log.w(TAG, "*** WARNING: Stripe SDK might be using a placeholder publishable key! " +
                    "Ensure the actual key is set correctly in secrets.xml. ***");
            
        }
      


        
        try {
            PaymentConfiguration.init(
                    getApplicationContext(),
                    stripePublishableKey
            );
            Log.i(TAG, "Stripe SDK Initialized Successfully using key from resources.");
        } catch (Exception e) {
            Log.e(TAG, "FATAL: Failed to initialize Stripe SDK. Payments will likely fail.", e);
        }
    }
}
