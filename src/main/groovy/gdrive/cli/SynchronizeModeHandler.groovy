package gdrive.cli

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import org.apache.commons.lang.StringUtils
import org.apache.log4j.Logger

import java.text.SimpleDateFormat

import static gdrive.cli.GDriveCliMain.*;

/**
 * Created by brad on 6/18/14.
 */
class SynchronizeModeHandler implements SystemModeHandler {

    static Logger logger = Logger.getLogger(SynchronizeModeHandler)

    @Override
    String getModeSupported() {
        return SYNC_MODE;
    }

    /**
     * Web pages that are helpful:
     *  https://developers.google.com/drive/v2/reference/
     *  https://developers.google.com/resources/api-libraries/documentation/drive/v2/java/latest/
     *  https://developers.google.com/drive/web/search-parameters
     *
     *  http://code.google.com/p/google-api-java-client/
     *
     */


    @Override
    void handle() {
        logger.debug("Connecting to google drive...");
        Drive drive = buildDrive();

        File driveCache = new File(".", ".drive_cache");
        driveCache.mkdirs();

        int filesChangedInDrive = cacheGoogleDriveChanges(drive, driveCache);

        // TODO Finish synchronization

        logger.info("Synchronization completed successfully.")
    }//end handle()

    //==================================================================================================================
    //  Private Helper Methods
    //==================================================================================================================
    private int cacheGoogleDriveChanges(Drive drive, File driveCache){
        int filesChanged = 0;
        Calendar lastSyncDate = GDriveCliMain.CONFIG.getLastSyncDate(); // Should be in GMT with the server.
        TimeZone tz = TimeZone.getTimeZone("UTC");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        dateFormat.setTimeZone(tz);
        String lastSyncDateAsISO = dateFormat.format(lastSyncDate.getTime());

        String afterModTimeQuery = "modifiedDate > '$lastSyncDateAsISO'";

        logger.info("Reading google drive file state after last sync time: $lastSyncDateAsISO");
        String nextPageToken = null;
        int pageCounter = 0;
        while( true ) {
            pageCounter++;
            logger.debug("Listing files...");
            Drive.Files.List listOp = drive.files().list();
            listOp.setMaxResults(100);
            listOp.setQ(afterModTimeQuery);
            listOp.setPageToken(nextPageToken);
            FileList fileList = listOp.execute();
            filesChanged += fileList.getItems().size();

            logger.debug("Writing file list page @|cyan $pageCounter|@ to disk...");
            File currentFileListPage = new File(driveCache, "filelist.page.${formatPageNumber(pageCounter, 6)}")
            currentFileListPage << fileList.toPrettyString();

            nextPageToken = fileList.getNextPageToken();
            if (nextPageToken == null)
                break;
        }
        return filesChanged;
    }//end cacheGoogleDriveChanges()


    String formatPageNumber(int pageNum, int leadingZeros){
        return String.format("%0"+leadingZeros+"d", pageNum);
    }

    Drive buildDrive() {
        String refreshToken = GDriveCliMain.CONFIG.getRefreshToken();
        if( StringUtils.isNotEmpty(refreshToken) ) {
            logger.debug("A refreshToken: @|cyan $refreshToken|@ was found.  Using this to access drive...")
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setClientSecrets(GDriveCliMain.CONFIG.clientSecrets)
                    .setJsonFactory(new JacksonFactory())
                    .setTransport(new NetHttpTransport())
                    .build();
            credential.setRefreshToken(refreshToken);
            logger.debug("Calling refresh token...")
            credential.refreshToken();

            Drive service = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), credential).setApplicationName(Constants.APP_NAME).build();
            return service;
        }else{
            GDriveCliMain.errorAndDie("Unable to find refresh token.  You need to run -a to establish a refresh token first.  See --help for more information.")
        }
    }//end buildDrive()


}//end SynchronizeModeHandler()
