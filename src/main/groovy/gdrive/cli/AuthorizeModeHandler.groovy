package gdrive.cli

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonParser
import com.google.api.client.json.jackson.JacksonFactory
import org.apache.log4j.Logger
import org.json.JSONObject

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

    private static final String CLIENTSECRETS_LOCATION = "/client-secrets.json";
    private static final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"; // Opens the code in a browser, to be copied and pasted in.
    private static final List<String> SCOPES = [
            "https://www.googleapis.com/auth/drive.file",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile"];


    private GoogleClientSecrets clientSecrets = null;

    public AuthorizeModeHandler(){
        logger.debug("Reading client secrets...");
        String clientSecretsText = System.getResourceAsStream(CLIENTSECRETS_LOCATION).text;
        logger.debug("Successfully read: \n[$clientSecretsText]");
        if( clientSecretsText == null || clientSecretsText.trim().length() == 0 )
            throw new NullPointerException("There is not any client secrets data.  Cannot establish connection to google.")

        JacksonFactory factory = new JacksonFactory();
        JsonParser parser = factory.createJsonParser(clientSecretsText);
        clientSecrets = parser.parseAndClose(GoogleClientSecrets.class);
        logger.debug("Client secrets: $clientSecrets")
    }


    @Override
    String getModeSupported() {
        return AUTH_MODE;
    }

    @Override
    void handle() {
        GoogleCredential credential = authorize();
        logger.info("Your access token is: ["+credential.accessToken+"]");
    }//end handle()





    //==================================================================================================================
    //  Google's Code From: https://developers.google.com/+/domains/authentication/
    //==================================================================================================================
    GoogleClientSecrets getClientSecrets() {
        return clientSecrets;
    }

    def authorize() {
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), new JacksonFactory(), getClientSecrets(), SCOPES)
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
        logger.info("Authorization code: [@|cyan "+code?.trim()+"|@]");

        logger.debug("Testing...");
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
                }
        )
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
