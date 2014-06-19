package gdrive.cli

import gdrive.cli.config.GDriveConfig

/**
 * Created by brad on 6/18/14.
 */
public interface SystemModeHandler {

    public String getModeSupported();

    public void handle();

}//end SystemModeHandler()
