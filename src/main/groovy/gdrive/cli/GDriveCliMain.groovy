package gdrive.cli

import gdrive.cli.config.GDriveConfig
import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator

/**
 * Entry point of the command line application.
 * <br/><br/>
 * Created by brad on 6/18/14.
 */
class GDriveCliMain {

    static Logger logger = Logger.getLogger(GDriveCliMain)

    static final String SYNC_MODE = "SYNCHRONIZE"
    static final String AUTH_MODE = "AUTHORIZE"

    static String SYSTEM_MODE = SYNC_MODE;
    public static GDriveConfig CONFIG = null;
    public static GDriveConfig getConfiguration(){
        return CONFIG;
    }


    /**
     * Method first called by the program on startup.
     * <br/><br/>
     * @param args
     */
    public static void main(String[] args){
        long start = System.currentTimeMillis();
        File lockFile = new File(".gdrive_cli.lock");
        if( lockFile.exists() )
            errorAndDie("Existing lock file '.gdrive_cli.lock', refusing to sync.  Please remove this file and try again.")
        lockFile << "Start=${System.currentTimeMillis()}\n"
        parseArgs(args);

        logger.debug("Reading gdrive configuration...");
        CONFIG = new GDriveConfig();

        SystemModeHandler handler = null;
        try {
            handler = resolveModeHandler();
        }catch(Throwable t){
            errorAndDie("Error while resolving mode handler for mode: ${SYSTEM_MODE}", t);
        }
        if( !handler )
            errorAndDie("Unable to handle mode: @|red ${SYSTEM_MODE}|@.  See --help documentation.")

        logger.debug("Using handler: @|green ${handler.class.name}|@")
        handler.handle();

        long stop = System.currentTimeMillis();
        logger.info("Successfully executed gdrive-cli in @|cyan ${(int) (((double) (stop-start)) / 1000.0d)}|@ seconds")

        lockFile.delete();
    }//end main


    static SystemModeHandler resolveModeHandler(){
        SystemModeHandler handler = null;
        ServiceLoader<SystemModeHandler> loader = ServiceLoader.load(SystemModeHandler);
        Iterator<SystemModeHandler> handlerIterator = loader.iterator();
        while( handlerIterator.hasNext() ){
            SystemModeHandler next = handlerIterator.next();
            if( next.modeSupported == SYSTEM_MODE ) {
                handler = next;
                break;
            }
        }
        return handler;
    }//end resolveModeHandler()

    static def parseArgs(String[] args){
        def cli = new CliBuilder(usage:'gdrive-cli [options]\n  Synchronization tool for Google Drive.\n\n')
        cli.with {
            h longOpt: 'help', 'Show usage information (this message).'
            v longOpt: 'verbose', 'Turn on extensive application debugging information'
            a longOpt: 'auth', 'Initialize the application with google drive.  Should be called at least once, before any other commands.'
        }

        def options = cli.parse(args);
        if( !options ){
            errorAndDie("Unable to parse argument list.  Try --help");
        }

        if( options.h ){
            cli.usage();
            exitGracefully("Please issue a command.");
        }
        if( options.v ){
            PropertyConfigurator.configure(System.getResourceAsStream("/log4j.verbose.properties"));
            logger.debug("Successfully enabled debugging logging.")
        }
        if( options.a ){
            SYSTEM_MODE = AUTH_MODE;
            logger.debug("Setting system mode to '$AUTH_MODE' - in otherwords, not going to synchronize but instead auth with google.")
        }
    }

    public static void exitGracefully( String msg ){
        logger.info(msg);
        System.exit(0);
    }

    public static void errorAndDie( String msg ){
        logger.error(msg);
        System.exit(1);
    }
    public static void errorAndDie( String msg, Throwable t ){
        logger.error(msg, t);
        System.exit(1);
    }


}//end GDriveCliMain()

