package gdrive.cli.config

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.json.JsonParser
import com.google.api.client.json.jackson.JacksonFactory
import gdrive.cli.Constants
import org.apache.log4j.Logger
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONWriter

import static gdrive.cli.Constants.*;

/**
 * A class to hold the configuration options that gdrive-cli uses to synchronize with the server.
 *
 * Created by brad on 6/18/14.
 */
class GDriveConfig {

    static Logger logger = Logger.getLogger(GDriveConfig);

    public GDriveConfig(){
        logger.debug("Parsing grive state file (if exists)...")
        gdriveCliFile = new File(GDRIVE_CLI_FILE);
        if( gdriveCliFile.exists() )
            gdriveCliJson = new JSONObject(gdriveCliFile.text);
        else
            gdriveCliJson = new JSONObject();

        logger.debug("Parsing grive state file (if exists)...")
        griveStateFile = new File(GRIVE_STATE_FILE);
        if( griveStateFile.exists() )
            griveStateJson = new JSONObject(griveStateFile.text);

        logger.debug("Reading client secrets...");
        String clientSecretsText = System.getResourceAsStream(CLIENTSECRETS_LOCATION).text;
        logger.debug("Successfully read: \n[$clientSecretsText]");
        if( clientSecretsText == null || clientSecretsText.trim().length() == 0 )
            throw new NullPointerException("Failure to load client secrets file from classpath: @|red $CLIENTSECRETS_LOCATION|@")
        JacksonFactory factory = new JacksonFactory();
        JsonParser parser = factory.createJsonParser(clientSecretsText);
        clientSecrets = parser.parseAndClose(GoogleClientSecrets.class);
        logger.debug("Client secrets: $clientSecrets")

    }//end GDriveConfig()


    public File gdriveCliFile = null;
    public JSONObject gdriveCliJson = null;

    public File griveStateFile = null;
    public JSONObject griveStateJson = null;

    public GoogleClientSecrets clientSecrets = null;

    public Calendar getLastSyncDate() {
        try {
            return toTime(gdriveCliJson?.getLong(LAST_SYNC_KEY));
        }catch(JSONException jsone){
            if( jsone.message.matches("JSONObject.*not found.") )
                return toTime(0l);
            throw jsone;
        }
    }

    public String getRefreshToken() {
        try {
            return gdriveCliJson?.getString(REFRESH_TOKEN_KEY);
        }catch(JSONException jsone){
            if( jsone.message.equals("JSONObject[\"refresh_token\"] not found.") )
                return null;
            throw jsone;
        }
    }//end getRefreshToken()

    public void saveGDriveCliJson() {
        logger.debug("Saving GDriveCliJSON...")
        FileWriter writer = new FileWriter(gdriveCliFile, false);
        writer.write(gdriveCliJson.toString(2));
        writer.flush();
        writer.close();
    }




    private Calendar toTime( long millisSinceEpoch ){
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millisSinceEpoch);
        return c;
    }



}//end GDriveConfig()
