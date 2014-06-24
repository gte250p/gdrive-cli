package gdrive.cli

import com.google.api.services.drive.DriveRequest

/**
 * Manages execution of google remote server requests.  Performs an exponential backoff approach to handling errors.
 */
class ExecutionManager {


    public static Object execute(DriveRequest request){
        // TODO Later we need to handle errors, perform exponential backoff, etc.
        return request.execute();
    }


}
