package gdrive.cli

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.ChildList
import com.google.api.services.drive.model.ChildReference
import com.google.api.services.drive.model.File as GoogleFile;
import com.google.api.services.drive.model.FileList
import org.apache.commons.io.FileUtils
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

        logger.debug("Caching all google remote files...")
        int filesCached = cacheGoogleDriveData(drive);

        logger.debug("Marking root directory...");
        GoogleFile root = getRootDirectory(drive);
        GDriveCliMain.CONFIG.h2db.executeUpdate("update GOOGLE_FILE set IS_ROOT = true where ID = ?", [root.getId()]);

        logger.info("Synchronization completed successfully.")
    }//end handle()


    //==================================================================================================================
    //  Private Helper Methods
    //==================================================================================================================
    private GoogleFile getRootDirectory(Drive drive){
        GoogleFile root = ExecutionManager.execute(drive.files().get("root"));
        logger.debug("Root file information: \n"+root.toPrettyString());
        return root;
    }//end getRootDirectory()


    private int cacheGoogleDriveData(Drive drive){
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
            listOp.setMaxResults(Constants.MAX_GOOGLE_FILES_AT_ONE_TIME);
            listOp.setQ(afterModTimeQuery);
            listOp.setPageToken(nextPageToken);
            FileList fileList = ExecutionManager.execute(listOp);
            filesChanged += fileList.getItems().size();

            for( GoogleFile file : fileList.getItems() ){
                cache(file);
            }

//            logger.debug("Writing file list page @|cyan $pageCounter|@ to disk...");
//            File currentFileListPage = new File(GDriveCliMain.CONFIG.driveCacheDir, "filelist.page.${formatPageNumber(pageCounter, 6)}")
//            currentFileListPage << fileList.toPrettyString();

            nextPageToken = fileList.getNextPageToken();
//            if (nextPageToken == null)
                break;
        }

        int count = -1;
        GDriveCliMain.CONFIG.h2db.eachRow("select count(*) as count from GOOGLE_FILE") { row ->
            count = row.count;
        }
        logger.info("Successfully cached @|cyan ${count}|@ files from google drive.")

        return filesChanged;
    }//end cacheGoogleDriveChanges()

    private void cache(GoogleFile file){
        // First, we delete the row if it exists in the database already...
        GDriveCliMain.CONFIG.h2db.executeUpdate("DELETE from GOOGLE_FILE where ID = ?", [file.getId()]);

        GDriveCliMain.CONFIG.h2db.executeUpdate(
                "INSERT INTO GOOGLE_FILE (ID, CREATE_DATE, ETAG, EXTENSION, SIZE, IS_FILE, IS_DIRECTORY, TRASHED, MD5CHECKSUM, MIME_TYPE, MODIFIED_DATE, SHARED, TITLE, VERSION, PARENT_ID, IS_ROOT, PROCESSED) " +
                        "VALUES (                 ?,  ?,           ?,    ?,         ?,    ?,       ?,            ?,       ?,           ?,         ?,             ?,      ?,     ?,       ?,         false,   false)",
                [file.getId(),
                 toDate(file.getCreatedDate().getValue()),
                 file.getEtag(),
                 file.getFileExtension(),
                 file.getFileSize(),
                 !file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder"),
                 file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder"),
                 file.getLabels().getTrashed(),
                 file.getMd5Checksum(),
                 file.getMimeType(),
                 toDate(file.getModifiedDate().getValue()),
                 file.getShared(),
                 file.getTitle(),
                 file.getVersion(),
                 file.getParents()?.isEmpty() ? null : file.getParents()?.get(0).getId()
                ]);
        logger.debug("Successfully inserted cached file[@|cyan ${file.getId()}|@:@|blue ${FileUtils.byteCountToDisplaySize(file.getFileSize() ?: 0l)}|@:@|green ${file.getTitle()}|@]...")

    }//end cache()

    private Date toDate( Long timestamp ){
        Calendar instance = Calendar.getInstance();
        instance.setTimeInMillis(timestamp);
        return instance.getTime();
    }


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
