package stevens.server.nearestdepartures;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {StationInfo.class},version = 1)
public abstract class StationInfoDatabase extends RoomDatabase {
    public abstract StationInfoDao stationInfoDao();
}
