package stevens.server.nearestdepartures;

public enum SharedPreferenceInfo {
    //todo convert all shared preferences to use this enum
    DEPARTURES_SORTING_METHOD ("configuration","departuresSortingMethod"),
    ;

    private String file;
    private String preferenceName;

    SharedPreferenceInfo(String file, String preferenceName) {
        this.file = file;
        this.preferenceName = preferenceName;
    }
    String getFile(){
        return this.file;
    }
    String getPreferenceName(){
        return this.preferenceName;
    }
}
