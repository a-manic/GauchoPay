package edu.ucsb.ece150.gauchopay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import static androidx.core.content.ContextCompat.getSystemService;

@RequiresApi(api = Build.VERSION_CODES.O)
public class ReadWebServer extends AsyncTask<String, String, String> {

    private static final String myUserID = "9926445"; // [TODO] Fill in your ID. Your PERM number is ideal since it is a unique code that only you have access to.
    private static final String requestURL = "http://android.bryanparmenter.com/payment_listen.php?id=" + myUserID;

    private URL urlObject;
    private static int lastAmount;

    private static final String CHANNEL_ID = "Notifications";
    private static final String CHANNEL_NAME = "Notifications";
    private static final String CHANNEL_DESC = "Notifications";

    private WeakReference<Context> callingContext;


    ReadWebServer(Context context) {
        this.callingContext = new WeakReference<>(context);
    }



    @Override
    protected String doInBackground(String... uri) {
        String responseString = null;
        try {
            urlObject = new URL(requestURL);
        }
        catch(Exception e) {
            e.printStackTrace();
            responseString = "FAILED";
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) urlObject.openConnection();
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                // If this log is printed, then something went wrong with your call
                Log.d("Response from Send", "FAILED");
            } else {
                // Parse the input into a string and then read it
                return readFullyAsString(connection.getInputStream());
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

        // The server responds with an integer that is encoded as a string
        int resultInt = Integer.parseInt(result);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(CHANNEL_DESC);
        NotificationManager manager = getSystemService(callingContext.get(), NotificationManager.class);
        manager.createNotificationChannel(channel);

        // Behavior on a valid request
        if(resultInt != 0) {
            lastAmount = resultInt; // Do not modify this!

            // [TODO] A response was received from the server. Notify the user to select a card
            // for sending to the web server. (Maybe a Toast)



            Intent resultIntent = new Intent(callingContext.get(), CardListActivity.class);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(callingContext.get(), 0, resultIntent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(callingContext.get(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle("Select a card")
                    .setContentText("Please select a card to proceed with the transaction of $"+lastAmount)
                    .setContentIntent(resultPendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(callingContext.get());
            notificationManagerCompat.notify(1,builder.build());

        }else{
            manager.cancel(1);
        }
    }

    private String readFullyAsString(InputStream inputStream) throws IOException {
        return readFully(inputStream).toString("UTF-8");
    }

    private ByteArrayOutputStream readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while((length = inputStream.read(buffer)) != -1) {
            stream.write(buffer, 0, length);
        }

        return stream;
    }

    // Do not modify this! This helps keep track of the current API call. Since only one request
    // is made at a time, this value will stay the same until the pending transaction is cleared.
    static int getLastAmount() {
        return lastAmount;
    }

    static void resetLastAmount() {
        lastAmount = 0; // Do not modify this!


    }


}
