package stevens.server.nearestdepartures.ui.theme;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.room.Room;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;

import stevens.server.nearestdepartures.StationInfo;
import stevens.server.nearestdepartures.StationInfoDao;
import stevens.server.nearestdepartures.StationInfoDatabase;

public class MainApplication extends Application {
    private final StateManager state = new StateManager(this);
    private String currentLocationCrs;
    private Location lastLocation;
    private long lastLocationUpdateTimeMs;
    public String getCurrentLocationCrs() {
        //initialise if not already
        if (currentLocationCrs == null && lastLocation == null){
            updateLocation(); //no nearby stations could also result in currentLocationCrs being null so check lastLocation has been set
        } else if (System.currentTimeMillis() - lastLocationUpdateTimeMs > 120000){
            Log.d("MainApplication",(System.currentTimeMillis() - lastLocation.getTime())+"ms since last location update, refreshing...");
            updateLocation(); //update if we havent in a while
        }
        return currentLocationCrs;
    }
    public void updateLocation(){
        try {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                Log.e("MainApplication","user has not opted into location services");
                return;
            }
            //TODO ask for location refresh here if location is very out of date
            //fetch location
            Log.d("MainApplication","fetching location...");
            FusedLocationProviderClient location_services = LocationServices.getFusedLocationProviderClient(this);
            Task<Location> lastLocationTask = location_services.getLastLocation();
            lastLocation = Tasks.await(lastLocationTask);
            Log.d("MainApplication","location: "+ lastLocation.getLatitude()+" "+ lastLocation.getLongitude());
            //find the closest station
            float minDistance = Float.POSITIVE_INFINITY;
            String closestStation = null;
            for (StationInfo station : state.getStationInfoList()){
                //calculate the distance to the station
                Location stationLocation = new Location((String)null);
                stationLocation.setLatitude(station.latitude);
                stationLocation.setLongitude(station.longitude);
                float distance = lastLocation.distanceTo(stationLocation);
                if (distance < minDistance){
                    closestStation = station.crs;
                    minDistance = distance;
                }
                currentLocationCrs = closestStation;
            }
            lastLocationUpdateTimeMs = System.currentTimeMillis();
            Log.d("MainApplication","closest station: "+closestStation+" "+minDistance+"m");
        } catch (Exception e){
            Log.e("MainApplication","location update failed: "+e);
        }
    }
}

class StateManager {
    private StationInfoDatabase stationInfoDatabase = null;
    private final Context context;
    private List<StationInfo> stationInfoList;
    public StateManager(Context context){
        //everything is lazy
        this.context = context;
    }
    public StationInfoDatabase getStationInfoDatabase() {
        //lazy initialisation (this is only called from a background thread)
        if (stationInfoDatabase == null) {
            //open stations database
            Log.d("MainApplication", "opening stations database");
            stationInfoDatabase = Room.databaseBuilder(context.getApplicationContext(), StationInfoDatabase.class, "station-info.db")
                    .createFromAsset("stations.sqlite3")
                    .fallbackToDestructiveMigration(true)
                    .build();
        }
        return stationInfoDatabase;
    }
    public List<StationInfo> getStationInfoList(){
        //we can cache this because the list of stations will not change like ever
        if (stationInfoList == null){
            Log.d("MainApplication", "fetching database info...");
            //get station info
            StationInfoDao stationInfoDao = this.getStationInfoDatabase().stationInfoDao();
            stationInfoList = stationInfoDao.getAllStations();
            Log.d("MainApplication", "database info retrieved");
        }
        return this.stationInfoList;
    }
}