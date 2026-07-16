package stevens.server.nearestdepartures;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Query;

import java.util.List;

@Entity(tableName = "stations")
public class StationInfo {
    @PrimaryKey @NonNull
    public String crs;
    public String station_name;
    public double latitude;
    public double longitude;
}

