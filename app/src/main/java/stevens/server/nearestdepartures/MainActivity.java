package stevens.server.nearestdepartures;

import static android.content.Intent.ACTION_SCREEN_ON;
import static android.content.Intent.ACTION_USER_PRESENT;
import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import com.google.android.material.color.DynamicColors;

import org.json.JSONException;

import java.io.IOException;
import java.security.Security;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle saved_instance_state){
        //config
        Security.setProperty("networkaddress.cache.negative.ttl","0");
        Security.setProperty("networkaddress.cache.ttl","-1");
        //shared preferences
        SharedPreferences shared_preferences = this.getSharedPreferences("api_keys", MODE_PRIVATE);
        SharedPreferences.Editor shared_preferences_editor = shared_preferences.edit();
        String api_key = shared_preferences.getString("LDBWS","");
        //building ui
        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
        this.setContentView(R.layout.main_activity);
        EditText LDBWS_api_key_entry = this.findViewById(R.id.LBWS_api_key_entry);
        LDBWS_api_key_entry.setText(api_key);
        //callbacks
        LDBWS_api_key_entry.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                //update api_key when user changes it in the text entry
                String new_api_key = editable.toString();
                shared_preferences_editor.putString("LDBWS",new_api_key);
                shared_preferences_editor.apply();
            }
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        });
        if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity","requesting location access");
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},102);
        }

        //schedule widget updates
        AlarmManager alarm_manager = this.getSystemService(AlarmManager.class);
        Intent update_intent = new Intent(this,NearestDeparturesWidget.class);
        update_intent.setAction("REFRESH_DATA");
        this.sendBroadcast(update_intent);
        alarm_manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,0,30000, PendingIntent.getBroadcast(this,1,update_intent,PendingIntent.FLAG_IMMUTABLE));

        //screen on events
        this.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("NearestDeparturesWidget","Screen on event detected");
                Intent update_intent = new Intent(context,NearestDeparturesWidget.class);
                update_intent.setAction("REFRESH_DATA");
                context.sendBroadcast(update_intent);
            }
        },new IntentFilter(ACTION_USER_PRESENT));

        //super call
        super.onCreate(saved_instance_state);
    }
}
