package stevens.server.nearestdepartures;

import static android.content.Intent.ACTION_USER_PRESENT;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.color.DynamicColors;

import org.json.JSONException;

import java.io.IOException;
import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private ScheduledExecutorService worker_thread;
    private AtomicBoolean currently_updating_station_database = new AtomicBoolean(false);
    @Override
    public void onCreate(Bundle saved_instance_state){
        //setup worker thread for later use
        this.worker_thread = Executors.newSingleThreadScheduledExecutor();

        //notification channel
        NotificationManager notification_manager = this.getSystemService(NotificationManager.class);
        NotificationChannel notification_channel = new NotificationChannel("info",    "information", NotificationManager.IMPORTANCE_HIGH);
        notification_channel.enableVibration(true);
        notification_channel.enableLights(true);
        notification_manager.createNotificationChannel(notification_channel);

        //config
        Security.setProperty("networkaddress.cache.negative.ttl","0");
        Security.setProperty("networkaddress.cache.ttl","-1");

        //shared preferences
        SharedPreferences shared_preferences = this.getSharedPreferences("api_keys", MODE_PRIVATE);
        SharedPreferences.Editor shared_preferences_editor = shared_preferences.edit();
        String LDBWS_api_key = shared_preferences.getString("LDBWS","");
        String knowledgebase_api_key = shared_preferences.getString("knowledgebase","");

        //building ui
        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
        this.setContentView(R.layout.main_activity);
        EditText LDBWS_api_key_entry = this.findViewById(R.id.LBWS_api_key_entry);
        EditText knowledgebase_api_key_entry = this.findViewById(R.id.knowledgebase_api_key_entry);
        Button update_station_database_button = this.findViewById(R.id.update_station_database_button);
        LDBWS_api_key_entry.setText(LDBWS_api_key);
        knowledgebase_api_key_entry.setText(knowledgebase_api_key);

        //callbacks
        LDBWS_api_key_entry.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                //update LDBWS_api_key when user changes it in the text entry
                String new_api_key = editable.toString();
                shared_preferences_editor.putString("LDBWS",new_api_key);
                shared_preferences_editor.apply();
            }
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        });
        knowledgebase_api_key_entry.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                //update LDBWS_api_key when user changes it in the text entry
                String new_api_key = editable.toString();
                shared_preferences_editor.putString("knowledgebase",new_api_key);
                shared_preferences_editor.apply();
            }
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        });
        update_station_database_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                worker_thread.submit(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("MainActivity","updating station database");
                        if (currently_updating_station_database.getAndSet(true)){
                            Log.d("MainActivity","aborting update: already updating");
                            return;
                        }
                        show_notification("station database","updating...");
                        //instantiate the national rail api
                        SharedPreferences shared_preferences = getSharedPreferences("api_keys", MODE_PRIVATE);
                        String api_key = shared_preferences.getString("knowledgebase", "");
                        NationalRailAPI national_rail_api = new NationalRailAPI(null, knowledgebase_api_key);
                        //fetch stations
                        try {
                            NationalRailAPI.Stations stations = national_rail_api.getAllStations();
                            Log.d("MainActivity","station database update complete");
                            show_notification("station database","update complete");
                        } catch (IOException | JSONException e) {
                            Log.e("MainActivity","Error updating station database: "+e);
                            show_notification("station database","update failed: "+e);
                        }
                    }
                });
            }
        });


        //request permissions
        if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity","requesting location access");
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},102);
        }
        if (this.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity","requesting notification permissions");
            this.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},103);
        }

        //schedule widget updates
        AlarmManager alarm_manager = this.getSystemService(AlarmManager.class);
        Intent update_intent = new Intent(this,NearestDeparturesWidget.class);
        update_intent.setAction("REFRESH_DATA");
        this.sendBroadcast(update_intent);
        alarm_manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,5000,30000, PendingIntent.getBroadcast(this,1,update_intent,PendingIntent.FLAG_IMMUTABLE));

        //screen on events
        this.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //refresh data when user unlocks phone
                Log.d("NearestDeparturesWidget","Screen on event detected");
                Intent update_intent = new Intent(context,NearestDeparturesWidget.class);
                update_intent.setAction("REFRESH_DATA");
                context.sendBroadcast(update_intent);
            }
        },new IntentFilter(ACTION_USER_PRESENT));

        //super call
        super.onCreate(saved_instance_state);
    }
    public void show_notification(String title, String body){
        Notification notification = new Notification.Builder(this,"info")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .build();
        NotificationManager notification_manager = this.getSystemService(NotificationManager.class);
        notification_manager.notify(0,notification);
    }
}
