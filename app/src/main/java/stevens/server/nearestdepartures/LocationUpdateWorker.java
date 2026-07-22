package stevens.server.nearestdepartures;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class LocationUpdateWorker extends Worker {
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