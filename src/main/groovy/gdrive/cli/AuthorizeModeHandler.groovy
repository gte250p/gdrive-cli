package gdrive.cli

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonParser
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.About
import com.google.api.services.drive.model.FileList
import com.google.api.services.drive.model.Permission
import gdrive.cli.config.GDriveConfig
import org.apache.commons.lang.StringUtils
import org.apache.log4j.Logger

import static gdrive.cli.GDriveCliMain.*

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;





/**
 * Created by brad on 6/18/14.
 */
class AuthorizeModeHandler implements SystemModeHandler {

    static Logger logger = Logger.getLogger(AuthorizeModeHandler)

    private static final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"; // Opens the code in a browser, to be copied and pasted in.
    private static final Collection<String> SCOPES = [
            "profile",
            "email"
    ];

    @Override
    String getModeSupported() {
        return AUTH_MODE;
    }

    @Override
    void handle() {
        String refreshToken = GDriveCliMain.CONFIG.getRefreshToken();
        if( StringUtils.isNotEmpty(refreshToken) ){
            logger.debug("A refreshToken: @|cyan $refreshToken|@ was found.  Using this to access drive...")
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setClientSecrets(getClientSecrets())
                    .setJsonFactory(new JacksonFactory())
                    .setTransport(new NetHttpTransport())
                    .build();
            credential.setRefreshToken(refreshToken);
            logger.debug("Calling refresh token...")
            credential.refreshToken();

            // TODO Everything below here is just to test we made a connection appropriately.  If we connected ok, then we're done.
//            logger.debug("Constructing new drive service...");
//            Drive service = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), credential).setApplicationName(Constants.APP_NAME).build();
//
//            Drive.Files.List filesReq = service.files().list();
//            filesReq.setMaxResults(10);
//            FileList files = null;
//            logger.debug("Permissions response: "+permissions);

            logger.info("Successfully refreshed GDrive CLI's google access token.")
            return;
        }


        GoogleCredential credential = buildCredentialTheHardWay();
        logger.info("Your refresh token is: ["+credential.refreshToken+"], access token: ${credential.accessToken}");
        GDriveCliMain.CONFIG.gdriveCliJson.put(Constants.REFRESH_TOKEN_KEY, credential.refreshToken);
        GDriveCliMain.CONFIG.saveGDriveCliJson();

    }//end handle()





    //==================================================================================================================
    //  Google's Code From: https://developers.google.com/+/domains/authentication/
    //==================================================================================================================
    Collection<String> buildScopes(){
        def scopes = []
        scopes.addAll(SCOPES);
        scopes.addAll(DriveScopes.DRIVE);
        return scopes;
    }

    GoogleClientSecrets getClientSecrets() {
        return GDriveCliMain.CONFIG.clientSecrets;
    }

    GoogleCredential buildCredentialTheHardWay() {
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), new JacksonFactory(), getClientSecrets(), buildScopes())
                .setApprovalPrompt("force")

        // Set the access type to offline so that the token can be refreshed.
        // By default, the library will automatically refresh tokens when it
        // can, but this can be turned off by setting
        // dfp.api.refreshOAuth2Token=false in your ads.properties file.
                .setAccessType("offline").build();

        // This command-line server-side flow example requires the user to open the
        // authentication URL in their browser to complete the process. In most
        // cases, your app will use a browser-based server-side flow and your
        // user will not need to copy and paste the authorization code. In this
        // type of app, you would be able to skip the next 5 lines.
        // You can also look at the client-side and one-time-code flows for other
        // options at https://developers.google.com/+/web/signin/
        String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
        logger.info("Please open the following URL in your browser then type the authorization code:\n    $url");
        logger.info("\nPlease enter your auth code: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String code = br.readLine();
        // End of command line prompt for the authorization code.
        logger.debug("Authorization code: [@|cyan "+code?.trim()+"|@]");

        logger.debug("Converting authorization code into credential...");
        GoogleTokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(new NetHttpTransport())
                .setJsonFactory(new JacksonFactory())
                .setClientSecrets(getClientSecrets())
                .addRefreshListener(
                    new CredentialRefreshListener() {
                        @Override
                        public void onTokenResponse(Credential credential, TokenResponse tokenResponse2) {
                            // Handle success.
                            logger.info("Credential was refreshed successfully.");
                        }

                        @Override
                        public void onTokenErrorResponse(Credential credential, TokenErrorResponse tokenErrorResponse) {
                            // Handle error.
                            logger.error("Credential was not refreshed successfully. Redirect to error page or login screen.");
                        }
                    })
        // You can also add a credential store listener to have credentials
        // stored automatically.
        //.addRefreshListener(new CredentialStoreRefreshListener(userId, credentialStore))
                .build();

        // Set authorized credentials.
        credential.setFromTokenResponse(tokenResponse);
        // Though not necessary when first created, you can manually refresh the
        // token, which is needed after 60 minutes.
        credential.refreshToken();

        return credential;
    }//end authorize()


}//end AuthorizeModeHandler()
