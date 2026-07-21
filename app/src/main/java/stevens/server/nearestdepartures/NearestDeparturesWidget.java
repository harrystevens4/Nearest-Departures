package stevens.server.nearestdepartures;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.content.Context.MODE_PRIVATE;
import static android.content.Intent.ACTION_USER_PRESENT;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import stevens.server.nearestdepartures.ui.theme.MainApplication;

/**
 * Implementation of App Widget functionality.
 */
public class NearestDeparturesWidget extends AppWidgetProvider {

    private final ExecutorService update_loop = Executors.newSingleThreadExecutor();
    private static boolean hasScheduledUpdates = false;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.nearest_departures_widget);

        //setup on click actions
        Intent open_app_intent = new Intent(context,MainActivity.class);
        PendingIntent open_app_pending_intent = PendingIntent.getActivity(context,0,open_app_intent,FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.nearest_departures_widget_layout,open_app_pending_intent);
        Intent updateIntent = new Intent(context,NearestDeparturesWidget.class);
        updateIntent.setAction("REFRESH_DATA"); //click station name to refresh
        views.setOnClickPendingIntent(R.id.nearest_departures_widget_station_name,PendingIntent.getBroadcast(context,0,updateIntent,FLAG_IMMUTABLE));

        //schedule widget updates
        AlarmManager alarm_manager = context.getSystemService(AlarmManager.class);
        alarm_manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,5000,30000, PendingIntent.getBroadcast(context,1,updateIntent,PendingIntent.FLAG_IMMUTABLE));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent){
        Log.d("NearestDeparturesWidget","onReceive("+intent.getAction()+")");
        if (Objects.equals(intent.getAction(),ACTION_USER_PRESENT)){
            Log.d("NearestDeparturesWidget","Screen on event detected");
        } else if (Objects.equals(intent.getAction(),"REFRESH_DATA")) {
            //schedule an update
            //TODO switch to WorkManager
            WorkRequest updateWidgetRequest = new OneTimeWorkRequest.Builder(TimetableFetchWorker.class)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build();
            WorkManager
                    .getInstance(context)
                    .enqueue(updateWidgetRequest);
        } else {
            super.onReceive(context, intent);
        }
    }
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            Log.d("NearestDeparturesWidget","updating widget "+appWidgetId);
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }
    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

