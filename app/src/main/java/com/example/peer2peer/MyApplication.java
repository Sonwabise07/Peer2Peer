package com.example.peer2peer;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull; // Or import javax.annotation.Nonnull; if using that


import com.stripe.android.PaymentConfiguration;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication_StripeInit";

    @Override
    public void onCreate() {
        super.onCreate();

        String stripePublishableKey = ""; // Initialize to empty

        try {
            // --- Fetch the key from string resources ---
            stripePublishableKey = getResources().getString(R.string.stripe_publishable_key);
            // --- End Fetch ---
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL ERROR: Failed to read stripe_publishable_key from resources. " +
                    "Ensure secrets.xml exists in res/values and contains the key.", e);
            // Stop initialization if resource access fails
            return;
        }


        // --- Validation Checks ---
        if (stripePublishableKey == null || stripePublishableKey.isEmpty()) {
            Log.e(TAG, "CRITICAL ERROR: Stripe Publishable Key is missing or empty in resources. " +
                    "Ensure secrets.xml contains 'stripe_publishable_key'.");
            return; // Stop initialization here if key is missing/empty
        }

        // Check if it's still the placeholder value (Update placeholder if needed)
        if (stripePublishableKey.equals("pk_test_YOUR_ACTUAL_PUBLISHABLE_KEY")) {
            Log.w(TAG, "*** WARNING: Stripe SDK might be using a placeholder publishable key! " +
                    "Ensure the actual key is set correctly in secrets.xml. ***");
            // Decide if you want to stop initialization for placeholders
            // return; // Uncomment to prevent init with placeholder key
        }
        // --- End Validation ---


        // --- Initialize Stripe SDK ---
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
