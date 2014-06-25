package gdrive.cli

import com.google.api.services.drive.DriveRequest
import org.apache.log4j.Logger

/**
 * Manages execution of google remote server requests.  Performs an exponential backoff approach to handling errors.
 */
class ExecutionManager {

    public static Logger logger = Logger.getLogger(ExecutionManager.class);

    public static Random RAND = new Random(System.currentTimeMillis());

    public static Object execute(DriveRequest request){
        def response = null;
        boolean requestSucceeded = false;
        int currentFailureCount = 0;
        while( !requestSucceeded ) {
            try {
                response = request.execute();
                requestSucceeded = true;
            } catch (Throwable t) {
                logger.error("Error sending request: "+t);
                currentFailureCount++;
                if( currentFailureCount > GDriveCliMain.CONFIG.getMaxRetries() ){
                    GDriveCliMain.errorAndDie("Retry count exceeds max retry param[@|red ${GDriveCliMain.CONFIG.getMaxRetries()}|@].  Refusing to execute remote google request.");
                }
                long timeoutSeconds = Math.max(Math.pow(2.0d, currentFailureCount), 240);
                Thread.sleep((timeoutSeconds * 1000.0d) + RAND.nextInt(5000)); // Will wait at most 4 minutes + 0-5 seconds.
            }
        }
        return response;
    }//end execute()


}//end ExecutionManager
