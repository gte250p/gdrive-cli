package gdrive.cli

/**
 * Created by brad on 6/19/14.
 */
class Constants {

    public static final String APP_NAME = "GDrive CLI";

    public static final String GDRIVE_CLI_FILE = ".gdrive_cli";

    public static final String GRIVE_STATE_FILE = ".grive_state";

    public static final String CLIENTSECRETS_LOCATION = "/client-secrets.json";

    // Taken from: https://github.com/Grive/grive/blob/27817e835fe115ebbda5410ec904aa49a2ad01f1/grive/src/main.cc
    // public static String GRIVE_CLIENT_ID = "22314510474.apps.googleusercontent.com";
    // public static String GRIVE_CLIENT_SECRET = "bl4ufi89h-9MkFlypcI7R785";



    public static final String REFRESH_TOKEN_KEY = "refresh_token";
    public static final String LAST_SYNC_KEY = "last_sync_time"
    public static final String MAX_REQUEST_RETRY_KEY = "max_request_retries";


    public static final Integer MAX_GOOGLE_FILES_AT_ONE_TIME = 1000; // Supports values between 1-1000.  See https://developers.google.com/drive/v2/reference/files/list

}//end Constants()
