package stevens.server.nearestdepartures;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.util.Objects;

public class NationalRailAPI {
    public static class Departures {
        public static class TrainService {
            private final JSONObject json_info;
            public TrainService(JSONObject json_info){
                this.json_info = json_info;
            }
            @NonNull
            @Override
            public String toString(){
                return this.json_info.toString();
            }
            public String getDestinationName(){
                try {
                    return this.json_info.getJSONArray("destination").getJSONObject(0).getString("locationName");
                } catch (JSONException e) {
                    return null;
                }
            }
            public LocalTime getDepartureTime(){
                try {
                    String std = this.json_info.getString("std"); //stated time of departure
                    String etd = this.json_info.getString("etd"); //estimated time of departure
                    String departure_time_string;
                    //if the train is late, etd = "09:18" std = "09:04"
                    //if on time, etd = "On time" std = "09:14"
                    //if canceled, etd = "Cancelled"
                    if (Objects.equals(etd,"On time")){
                        departure_time_string = std;
                    } else if (Objects.equals(etd,"Cancelled")){
                        return null;
                    } else departure_time_string = etd;
                    return LocalTime.parse(departure_time_string);
                } catch (JSONException e) {
                    return null;
                }
            }
        }
        private final JSONObject json_departures;
        public Departures(JSONObject json_departures){
            this.json_departures = json_departures;
        }
        public String getStationName(){
            try {
                return this.json_departures.getString("locationName");
            } catch (JSONException e) {
                return null;
            }
        }
        public String getStationCRS(){
            try {
                return this.json_departures.getString("locationName");
            } catch (JSONException e) {
                return null;
            }
        }
        public TrainService[] getDepartures(){
            try {
                JSONArray train_services = this.json_departures.getJSONArray("trainServices");
                TrainService[] services_array = new TrainService[train_services.length()];
                for (int i = 0; i < train_services.length(); i++) {
                    services_array[i] = new TrainService(train_services.getJSONObject(i));
                }
                return services_array;
            } catch (JSONException e) {
                return null;
            }
        }
    }
    private final String api_key;
    public NationalRailAPI(@NonNull String api_key){
        this.api_key = api_key;
    }
    public Departures getDeparturesFor(String crs) throws IOException, JSONException {
        URL request_url = new URL("https://api1.raildata.org.uk/1010-live-departure-board-dep1_2/LDBWS/api/20220120/GetDepartureBoard/"+crs);
        Log.d("NationalRailAPI","making request to "+request_url);
        HttpURLConnection connection =  (HttpURLConnection) request_url.openConnection();
        //request headers
        connection.setRequestProperty("x-apikey",this.api_key);
        connection.setDoInput(true);
        connection.connect();
        int response_code = connection.getResponseCode();
        Log.d("NationalRailAPI","request returned code "+ response_code);
        if (response_code < 200 || response_code >= 300){
            throw new IOException("request returned code "+ response_code);
        }
        //get body
        byte[] body = connection.getInputStream().readAllBytes();
        //parse json
        JSONObject json_response = new JSONObject(new String(body));
        Log.d("NationalRailAPI",json_response.toString());
        connection.disconnect();
        //convert to departures object
        return new Departures(json_response);
    }
}
