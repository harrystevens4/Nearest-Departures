package stevens.server.nearestdepartures;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface StationInfoDao {
    @Query("SELECT * FROM stations")
    List<StationInfo> getAllStations();
    @Query("SELECT crs FROM stations LIMIT 10")
    List<String> getAllStationsCrs();
}
