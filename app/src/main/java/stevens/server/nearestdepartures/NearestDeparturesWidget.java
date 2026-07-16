package stevens.server.nearestdepartures;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.content.Context.MODE_PRIVATE;
import static android.content.Intent.ACTION_USER_PRESENT;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Implementation of App Widget functionality.
 */
public class NearestDeparturesWidget extends AppWidgetProvider {

    private static final ExecutorService update_loop = Executors.newSingleThreadExecutor();;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.nearest_departures_widget);

        //setup on click actions
        Intent open_app_intent = new Intent(context,MainActivity.class);
        PendingIntent open_app_pending_intent = PendingIntent.getActivity(context,0,open_app_intent,FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.nearest_departures_widget_layout,open_app_pending_intent);
        Intent update_intent = new Intent(context,NearestDeparturesWidget.class);
        update_intent.setAction("REFRESH_DATA"); //click station name to refresh
        views.setOnClickPendingIntent(R.id.nearest_departures_widget_station_name,PendingIntent.getBroadcast(context,0,update_intent,FLAG_IMMUTABLE));

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
            Future<?> future = update_loop.submit(new Runnable() {
               @Override
               public void run() {
                   RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.nearest_departures_widget);
                   try {
                       try {
                           InetAddress.getByName("api1.raildata.org.uk");
                       } catch (UnknownHostException ignored) {
                           Log.e("NearestDeparturesWidget", "Could not lookup hostname for LDBWS api");
                           return;
                       }
                       //instantiate the national rail api
                       SharedPreferences shared_preferences = context.getSharedPreferences("api_keys", MODE_PRIVATE);
                       String api_key = shared_preferences.getString("LDBWS", "");
                       NationalRailAPI national_rail_api = new NationalRailAPI(api_key, null);
                       NationalRailAPI.Departures station_departures = national_rail_api.getDeparturesFor("BFR");
                       NationalRailAPI.Departures.TrainService[] services = station_departures.getDepartures();
                       //populate departure board
                       StringBuilder departure_board_text = new StringBuilder();
                       for (NationalRailAPI.Departures.TrainService service : services) {
                           Log.d("MainActivity", service.getDepartureTime() + " - " + service.getDestinationName());
                           //add each departure
                           String departure_time_string;
                           LocalTime departure_time = service.getDepartureTime();
                           if (departure_time == null) {
                               departure_time_string = "Cancelled";
                           } else {
                               departure_time_string = departure_time.toString();
                           }
                           departure_board_text.append(departure_time_string).append("  ").append(service.getDestinationName()).append("\n");
                       }
                       //set the station name
                       views.setTextViewText(R.id.nearest_departures_widget_station_name, station_departures.getStationName());
                       //update the widget
                       views.setTextViewText(R.id.nearest_departures_widget_departures_board, departure_board_text.toString());
                       AppWidgetManager app_widget_manager = AppWidgetManager.getInstance(context);
                       //just update all of them rather than re requesting for each one
                       int[] app_widget_ids = app_widget_manager.getAppWidgetIds(new ComponentName(context,NearestDeparturesWidget.class));
                       app_widget_manager.partiallyUpdateAppWidget(app_widget_ids,views);
                       Log.d("NearestDeparturesWidget", "updated widget with latest departures");
                   } catch (IOException | JSONException e) {
                       Log.e("NearestDeparturesWidget", "error fetching new departures: " + e);
                       //throw new RuntimeException(e);
                   }
               }
           });
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