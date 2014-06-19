package gdrive.cli

import org.apache.log4j.Logger
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

    @Override
    void handle() {
        // Read gdrive configuration file...
//        File configFile = new File("./.gdrive-config")
//        logger.info("Reading configuration file '@|cyan ${configFile.canonicalPath}|@'...")
//        if( !configFile.exists() )
//            errorAndDie("Could not find gdrive configuration file: @|red ${configFile.canonicalPath}|@\nIf this is the first time you have ran gdrive-cli, please use the @|cyan -a|@ option.")

        // begin synchronize process...

        logger.info("Synchronization completed successfully.")
    }

}
