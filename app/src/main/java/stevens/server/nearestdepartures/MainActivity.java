package stevens.server.nearestdepartures;

import static android.content.Intent.ACTION_USER_PRESENT;

import static stevens.server.nearestdepartures.NearestDeparturesWidget.ACTION_REFRESH_DATA;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.room.Room;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.color.DynamicColors;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.xml.datatype.Duration;

public class MainActivity extends Activity {
    private ScheduledExecutorService worker_thread;
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

        //schedule widget updates
        Intent updateIntent = new Intent(this,NearestDeparturesWidget.class);
        updateIntent.setAction(ACTION_REFRESH_DATA); //click departures board to refresh
        AlarmManager alarm_manager = this.getSystemService(AlarmManager.class);
        alarm_manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,5000,30000, PendingIntent.getBroadcast(this,1, updateIntent,PendingIntent.FLAG_IMMUTABLE));
        //dummy request required here so that it does not trigger widget update when there is no work left
        //https://issuetracker.google.com/issues/115575872
        WorkRequest dummyRequest = new PeriodicWorkRequest.Builder(DummyWorker.class,java.time.Duration.ofSeconds(60))
                .build();
        WorkManager
                .getInstance(getApplicationContext())
                .enqueue(dummyRequest);

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
        ArrayList<String> permissionsToRequest = new ArrayList<String>();
        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity","requesting fine location access");
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }else {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity","requesting background location access");
                permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }
        }
        if (this.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity","requesting notification permissions");
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!permissionsToRequest.isEmpty()) {
            String[] permissionsToRequestArray = permissionsToRequest.toArray(new String[] {});
            this.requestPermissions(permissionsToRequestArray, 106);
        }

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

class DummyWorker extends Worker {
    public DummyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        return Result.success();
    }
}
