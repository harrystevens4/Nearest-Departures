package stevens.server.nearestdepartures;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "stations",indices = {@Index(value = {"crs"})})
public class StationInfo {
    @PrimaryKey @NonNull
    public String crs;
    @ColumnInfo(name = "name") @NonNull
    public String station_name;
    public double latitude;
    public double longitude;
    @NonNull @Override
    public String toString(){
        return String.format("{crs = \"%s\", station_name = \"%s\", latitude = %f, longitude = %f}",crs,station_name,latitude,longitude);
    }
}

