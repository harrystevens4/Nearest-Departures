package stevens.server.nearestdepartures;

import android.content.Context;

import androidx.annotation.NonNull;

public class NationalRailApi {
    public static class Departures {

    }
    private final Context context;
    private final String api_key;
    public NationalRailApi(Context context, @NonNull String api_key, @NonNull String crs){
        this.context = context;
        this.api_key = api_key;
    }
    public Departures getDeparturesFor(String crs){
        return null;
    }
}
