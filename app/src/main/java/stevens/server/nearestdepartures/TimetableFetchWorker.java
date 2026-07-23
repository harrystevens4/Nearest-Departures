package stevens.server.nearestdepartures;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.getBroadcast;
import static android.content.Context.MODE_PRIVATE;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.List;

public class TimetableFetchWorker extends Worker {
    private final Context context;
    public TimetableFetchWorker(@NonNull Context context, @NonNull WorkerParameters parameters){
        super(context,parameters);
        this.context = context;
    }
    @NonNull
    @Override
    public Result doWork(){
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.nearest_departures_widget);
        try {
            try {
                InetAddress.getByName("api1.raildata.org.uk");
            } catch (UnknownHostException ignored) {
                Log.e("NearestDeparturesWidget", "Could not lookup hostname for LDBWS api");
                return Result.failure();
            }
            //grab context
            MainApplication appContext = (MainApplication) context.getApplicationContext();
            //find the closest stations
            List<String> closestStations = appContext.getStationsCrsByDistance();
            //shared preferences for ui state so if the user has selected the 3rd closest station it will stay selected
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("interface_state",MODE_PRIVATE);
            int stationSelection = sharedPreferences.getInt("currentClosestStationSelection",0);
            if (stationSelection >= closestStations.toArray().length){
                //wrap back around
                sharedPreferences
                        .edit()
                        .putInt("currentClosestStationSelection",0)
                        .apply();
                stationSelection = 0;
            }
            if (closestStations.size() == 0) throw new RuntimeException("no stations nearby");
            String selectedStation = closestStations.get(stationSelection); //if there are multiple stations next to each other the user can cycle through them
            //instantiate the national rail api
            SharedPreferences shared_preferences = context.getSharedPreferences("api_keys", MODE_PRIVATE);
            String api_key = shared_preferences.getString("LDBWS", "");
            NationalRailAPI national_rail_api = new NationalRailAPI(api_key, null);
            NationalRailAPI.Departures station_departures = national_rail_api.getDeparturesFor(selectedStation);
            NationalRailAPI.Departures.TrainService[] services = station_departures.getDepartures();
            //prepare ListView for population
            RemoteViews.RemoteCollectionItems.Builder timetableListViewBuilder = new RemoteViews.RemoteCollectionItems.Builder();
            //populate departure board
            String departureBoardTitle = station_departures.getStationName();
            int id = 0;
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
                RemoteViews listItemView = new RemoteViews(context.getPackageName(),R.layout.nearest_departures_widget_list_view_item);
                listItemView.setTextViewText(R.id.nearestDeparturesDestinationName, service.getDestinationName());
                listItemView.setTextViewText(R.id.nearestDeparturesDepartureTime, departure_time_string);
                Intent fillInIntent = new Intent();
                listItemView.setOnClickFillInIntent(R.id.nearestDeparturesListItemLayout,fillInIntent);
                timetableListViewBuilder.addItem(id,listItemView);
                id++;
            }
            //set the station name
            views.setTextViewText(R.id.nearestDeparturesStationName, departureBoardTitle);
            //update the widget
            views.setRemoteAdapter(R.id.nearestDeparturesListView,timetableListViewBuilder.build());
            Log.d("NearestDeparturesWidget", "updated widget with latest departures");
        } catch (IOException | JSONException | RuntimeException e) {
            Log.e("NearestDeparturesWidget", "error fetching new departures: " + e);
            //show error on widget
            views.setTextViewText(R.id.nearestDeparturesStationName, "No departures available");
            //add a prompt to refresh
            RemoteViews refreshTextView = new RemoteViews(context.getPackageName(),R.layout.nearest_departures_widget_list_view_item);
            //set refresh intent
            Intent fillInIntent = new Intent();
            refreshTextView.setOnClickFillInIntent(R.id.nearestDeparturesDestinationName,fillInIntent);
            refreshTextView.setTextViewText(R.id.nearestDeparturesDestinationName,"Click to refresh");
            RemoteViews.RemoteCollectionItems listViewItems = new RemoteViews.RemoteCollectionItems.Builder()
                    .addItem(0,refreshTextView)
                    .build();
            views.setRemoteAdapter(R.id.nearestDeparturesListView,listViewItems);
            Log.e("NearestDeparturesWidget", "updating widget to reflect errors");
        } finally {
            //set onclick for listview items
            Intent updateIntent = new Intent(context,NearestDeparturesWidget.class);
            updateIntent.setAction("REFRESH_DATA");
            views.setPendingIntentTemplate(R.id.nearestDeparturesListView,getBroadcast(context,0,updateIntent,FLAG_IMMUTABLE));
            //app widget update
            AppWidgetManager app_widget_manager = AppWidgetManager.getInstance(context);
            int[] app_widget_ids = app_widget_manager.getAppWidgetIds(new ComponentName(context, NearestDeparturesWidget.class));
            app_widget_manager.partiallyUpdateAppWidget(app_widget_ids, views);
        }
        return Result.success();
    }
}
