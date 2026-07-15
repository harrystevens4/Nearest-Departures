package stevens.server.nearestdepartures;

import static android.content.Context.MODE_PRIVATE;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of App Widget functionality.
 */
public class NearestDeparturesWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.nearest_departures_widget);

        //schedule updates
        SharedPreferences shared_preferences = context.getSharedPreferences("api_keys", MODE_PRIVATE);
        String api_key = shared_preferences.getString("LDBWS","");
        ExecutorService execution_queue = Executors.newSingleThreadExecutor();
        NationalRailAPI national_rail_api = new NationalRailAPI(api_key);
        execution_queue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //instantiate the national rail api
                    NationalRailAPI.Departures station_departures = national_rail_api.getDeparturesFor("CTF");
                    NationalRailAPI.Departures.TrainService[] services = station_departures.getDepartures();
                    //populate departure board
                    StringBuilder departure_board_text = new StringBuilder();
                    for (NationalRailAPI.Departures.TrainService service : services) {
                        Log.d("MainActivity", service.getDestinationName()+" "+service.getDepartureTime());
                        //add each departure
                        departure_board_text.append(service.getDestinationName()).append(" ").append(service.getDepartureTime()).append("\n");
                    }
                    //set the station name
                    views.setTextViewText(R.id.nearest_departures_widget_station_name,station_departures.getStationName());
                    //update the widget
                    views.setTextViewText(R.id.nearest_departures_widget_departures_board, departure_board_text.toString());
                    appWidgetManager.updateAppWidget(appWidgetId, views);
//                    Log.d("MainActivity",""+station_departures);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
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