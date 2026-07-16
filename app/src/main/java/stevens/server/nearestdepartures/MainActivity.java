package stevens.server.nearestdepartures;

import static android.content.Intent.ACTION_USER_PRESENT;
import static android.database.sqlite.SQLiteDatabase.OPEN_READONLY;

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
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.google.android.material.color.DynamicColors;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private ScheduledExecutorService worker_thread;
    public StationInfoDatabase station_database;
    @Override
    public void onCreate(Bundle saved_instance_state){
        //setup worker thread for later use
        this.worker_thread = Executors.newSingleThreadScheduledExecutor();

        //open stations database
        Log.d("MainActivity","opening stations database");
        //TODO implement this
        //SQLiteDatabase.openDatabase(db_path,new SQLiteDatabase.OpenParams.Builder().addOpenFlags(OPEN_READONLY).build());
        this.station_database = Room.databaseBuilder(getApplicationContext(),StationInfoDatabase.class,"station-info-db")
                .createFromAsset("stations.sqlite3")
                .build();

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

        //schedule a location update
        this.worker_thread.submit(new Runnable() {
            @Override
            public void run() {
                update_location();
            }
        });

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

    public void update_location(){
        Log.d("NearestDeparturesWidget","fetching database info...");
        //get station info
        List<String> station_info = this.station_database.stationInfoDao().getAllStationsCrs();
        Log.d("NearestDeparturesWidget",""+station_info);
    }
}
