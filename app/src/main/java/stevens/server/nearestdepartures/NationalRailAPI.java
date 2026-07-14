package stevens.server.nearestdepartures;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class NationalRailAPI {
    public static class Departures {

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
        return null;
    }
}
