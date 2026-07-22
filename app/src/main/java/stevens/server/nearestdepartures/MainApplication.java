package stevens.server.nearestdepartures;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainApplication extends Application {
    private final StateManager state = new StateManager(this);
    private final ArrayList<String> currentLocationsCrs = new ArrayList<String>();
    private Location lastLocation;
    private long lastLocationUpdateTimeMs;
    @Override public void onCreate(){
        super.onCreate();
        //dummy request required here so that it does not trigger widget update when there is no work left
        //https://issuetracker.google.com/issues/115575872
        //lets just update the location or smth
        WorkRequest locationUpdateRequest = new PeriodicWorkRequest.Builder(LocationUpdateWorker.class, Duration.ofMinutes(15))
                .build();
        Context applicationContext = this.getApplicationContext();
        WorkManager workManager = WorkManager.getInstance(applicationContext);
        workManager.enqueue(locationUpdateRequest);
    }
    public List<String> getStationsCrsByDistance() {
        //initialise if not already
        if (currentLocationsCrs.isEmpty() && lastLocation == null){
            updateLocation(); //no nearby stations could also result in currentLocationCrs being null so check lastLocation has been set
        } else if (System.currentTimeMillis() - lastLocationUpdateTimeMs > 120000){
            Log.d("MainApplication",(System.currentTimeMillis() - lastLocation.getTime())+"ms since last location update, refreshing...");
            updateLocation(); //update if we havent in a while
        }
        return currentLocationsCrs;
    }
    public void updateLocation(){
        try {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                Log.e("MainApplication","user has not opted into location services");
                return;
            }
            //fetch location
            Log.d("MainApplication","fetching location...");
            FusedLocationProviderClient location_services = LocationServices.getFusedLocationProviderClient(this);
            CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                    .setMaxUpdateAgeMillis(300000) //location no older than 5 minutes
                    .build();
            Task<Location> lastLocationTask = location_services.getCurrentLocation(currentLocationRequest,(CancellationToken)null);
            lastLocation = Tasks.await(lastLocationTask);
            Log.d("MainApplication","location: "+ lastLocation.getLatitude()+" "+ lastLocation.getLongitude());
            //sort stations by distance
            List<StationInfo> stationList = state.getStationInfoList();
            stationList.sort(new Comparator<StationInfo>() {
                @Override
                public int compare(StationInfo station1, StationInfo station2) {
                    Location station1Location = new Location((String)null);
                    station1Location.setLatitude(station1.latitude);
                    station1Location.setLongitude(station1.longitude);
                    Location station2Location = new Location((String)null);
                    station2Location.setLatitude(station2.latitude);
                    station2Location.setLongitude(station2.longitude);
                    float distance1 = lastLocation.distanceTo(station1Location);
                    float distance2 = lastLocation.distanceTo(station2Location);
                    return Float.compare(distance1,distance2);
                }
            });
            currentLocationsCrs.clear();
            for (StationInfo stationInfo : stationList){
                //maximum of 3 stations
                if (currentLocationsCrs.size() >= 3) break;
                //maximum distance
                Location stationLocation = new Location((String)null);
                stationLocation.setLatitude(stationInfo.latitude);
                stationLocation.setLongitude(stationInfo.longitude);
                //500m range
                float distanceToStation = lastLocation.distanceTo(stationLocation);
                if (distanceToStation > 1000) continue;
                Log.d("MainApplication","found nearby station: "+stationInfo.station_name+" "+distanceToStation+"m");
                //only store the crs
                currentLocationsCrs.add(stationInfo.crs);
            }
            lastLocationUpdateTimeMs = System.currentTimeMillis();
            Log.d("MainApplication","closest station: "+currentLocationsCrs.get(0));
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

class LocationUpdateWorker extends Worker {
    private final Context context;
    public LocationUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        MainApplication mainApplication = (MainApplication)context.getApplicationContext();
        mainApplication.updateLocation();
        return Result.success();
    }
}