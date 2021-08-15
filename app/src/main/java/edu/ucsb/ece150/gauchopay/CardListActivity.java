package edu.ucsb.ece150.gauchopay;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class CardListActivity extends AppCompatActivity {

    public static final String APP_NAME = "GauchoPay";
    public static final String CARD_INFO = "CardInfo" ;
    public static final String ALL_CARDS = "AllCards";
    public static final String IS_CARD_LIST = "CardList" ;

    private static final int RC_HANDLE_INTERNET_PERMISSION = 2;

    private ArrayList<String> cardArray;
    private ArrayAdapter adapter;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    private ListView cardList;
    private Handler handler = new Handler();
    private Timer timer = new Timer();
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // Launch the asynchronous process to grab the web API
                    new ReadWebServer(getApplicationContext()).execute("");
                }
            });
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_list);

        // Ensure that we have Internet permissions
        int internetPermissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        if(internetPermissionGranted != PackageManager.PERMISSION_GRANTED) {
            final String[] permission = new String[] {Manifest.permission.INTERNET};
            ActivityCompat.requestPermissions(this, permission, RC_HANDLE_INTERNET_PERMISSION);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //do we have previously saved data?
        if(savedInstanceState!= null){
            // previous saved data
            cardArray = savedInstanceState.getStringArrayList(IS_CARD_LIST);
        }else{
            // no saved data
            sharedPreferences = getSharedPreferences(APP_NAME, MODE_PRIVATE);
            String json = sharedPreferences.getString(ALL_CARDS,"");
            if(json.length()!=0){
                // convert back to card array
                Gson gson = new Gson();
                cardArray = gson.fromJson(json, new TypeToken<ArrayList<String>>() {}.getType());

            }else{
                // initialise
                cardArray = new ArrayList<>();
            }

        }


        cardList = findViewById(R.id.cardList);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cardArray);
        cardList.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent toAddCardActivity = new Intent(getApplicationContext(), AddCardActivity.class);
                startActivity(toAddCardActivity);
            }
        });

        cardList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final int posID = (int) id;

                // If "lastAmount > 0" the last API call is a valid request (that the user must
                // respond to.
                if (ReadWebServer.getLastAmount() != 0) {

                    final String cardNumber =  cardArray.get(posID);

                    AlertDialog.Builder adb=new AlertDialog.Builder(CardListActivity.this);
                    adb.setTitle("Select card");
                    adb.setMessage("Are you sure you want to select this card" );
                    adb.setNegativeButton("Cancel", null);
                    adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                        @Override

                        public void onClick(DialogInterface dialog, int which) {
                            WriteWebServer writeWebServer = new WriteWebServer(getApplicationContext(),cardNumber);
                            writeWebServer.execute();
                        }});
                    adb.show();

                    // [TODO] Send the card information back to the web API. Reference the
                    // WriteWebServer constructor to know what information must be passed.
                    // Get the card number from the cardArray based on the position in the array.

                    // Reset the stored information from the last API call
                    ReadWebServer.resetLastAmount();
                }
            }
        });

        //delete cards

        cardList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                AlertDialog.Builder adb=new AlertDialog.Builder(CardListActivity.this);
                adb.setTitle("Delete?");
                adb.setMessage("Are you sure you want to delete this card" );
                final int positionToRemove = position;
                adb.setNegativeButton("Cancel", null);
                adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    @Override

                    public void onClick(DialogInterface dialog, int which) {
                        cardArray.remove(positionToRemove);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(CardListActivity.this, "Card deleted", Toast.LENGTH_SHORT).show();
                    }});
                adb.show();
                return true;
            }

        });

        // Start the timer to poll the webserver every 5000 ms
        timer.schedule(task, 0, 5000);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(IS_CARD_LIST,cardArray);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // [TODO] This is a placeholder. Modify the card information in the cardArray ArrayList
        // accordingly.

        sharedPreferences = getSharedPreferences(APP_NAME, MODE_PRIVATE);
        String cardNumber = sharedPreferences.getString(CARD_INFO,"");
        if(cardNumber.length()!=0){
            cardArray.add(cardNumber);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Card added", Toast.LENGTH_SHORT).show();

        }
        sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.clear();
        sharedPreferencesEditor.commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences = getSharedPreferences(APP_NAME,MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        Gson gson = new Gson();

        String json = gson.toJson(cardArray);
        sharedPreferencesEditor.putString(ALL_CARDS, json);
        sharedPreferencesEditor.apply();

    }
}
