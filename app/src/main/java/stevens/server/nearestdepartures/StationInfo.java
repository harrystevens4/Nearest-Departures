package stevens.server.nearestdepartures;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Query;

import java.util.List;

@Entity(tableName = "stations",indices = {@Index(value = {"crs"})})
public class StationInfo {
    @PrimaryKey @NonNull
    public String crs;
    @ColumnInfo(name = "name") @NonNull
    public String station_name;
    public double latitude;
    public double longitude;
}

