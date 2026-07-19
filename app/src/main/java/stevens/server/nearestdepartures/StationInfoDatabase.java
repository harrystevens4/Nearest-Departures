package stevens.server.nearestdepartures;

import androidx.lifecycle.ViewModel;
import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {StationInfo.class},version = 1,exportSchema = false)
public abstract class StationInfoDatabase extends RoomDatabase implements AutoCloseable {
    public abstract StationInfoDao stationInfoDao();
}

