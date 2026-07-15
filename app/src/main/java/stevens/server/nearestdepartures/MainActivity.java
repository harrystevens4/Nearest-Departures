package stevens.server.nearestdepartures;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import com.google.android.material.color.DynamicColors;

import org.json.JSONException;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle saved_instance_state){
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
        //super call
        super.onCreate(saved_instance_state);
    }
}
