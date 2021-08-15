package edu.ucsb.ece150.gauchopay;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.core.content.ContextCompat.startActivity;


public class WriteWebServer extends AsyncTask<String, String, String> {
    private static final String myUserID = "9926445"; // [TODO] Fill in your ID. Your PERM number is ideal since it is a unique code that only you have access to.

    private URL urlObject;
    private String cardNumber;


    private WeakReference<Context> callingContext;

    WriteWebServer(Context context, String cardNumber) {
        this.callingContext = new WeakReference<>(context);
        this.cardNumber = cardNumber;
    }

    @Override
    protected String doInBackground(String... uri) {
        Log.d("Response", "Running");
        String responseString = null;
        try {
            String requestURL = "http://android.bryanparmenter.com/payment_send.php?id=" + myUserID + "&card=" + cardNumber;
            urlObject = new URL(requestURL);
        } catch(Exception e) {
            e.printStackTrace();
            responseString = "FAILED";
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) urlObject.openConnection();
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                // If this log is printed, then something went wrong with your call
                Log.d("Response from Send", "FAILED");
            }
        } catch(Exception e) {
            e.printStackTrace();
            responseString = "FAILED";
        } finally {
            connection.disconnect();
        }

        return responseString;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        Toast.makeText(callingContext.get(), "Transaction complete", Toast.LENGTH_SHORT).show();

        // [TODO] Let the user know that the transaction is complete. We don't need to do anything
        // with the result since we are only SENDING information to the web server.

    }
}
