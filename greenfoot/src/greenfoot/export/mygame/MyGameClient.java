package greenfoot.export.mygame;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import bluej.Config;

/**
 * MyGame client.
 * 
 * @author Davin McCall
 */
public abstract class MyGameClient
{
    public final MyGameClient submit(String hostAddress, String uid, String password,
            String gameName, String fileName, int width, int height,
            String shortDescription, String longDescription)
        throws UnknownHostException, IOException
    {
        HttpClient httpClient = new HttpClient();
        
        String proxyHost = Config.getPropString("proxy.host", null);
        String proxyPortStr = Config.getPropString("proxy.port", null);
        if (proxyHost != null && proxyHost.length() != 0 && proxyPortStr != null) {
            HostConfiguration hostConfig = httpClient.getHostConfiguration();

            int proxyPort = 80;
            try {
                proxyPort = Integer.parseInt(proxyPortStr);
            }
            catch (NumberFormatException nfe) {}

            hostConfig.setProxy(proxyHost, proxyPort);
            // TODO prompt for user/password
            String proxyUser = Config.getPropString("proxy.user", null);
            String proxyPass = Config.getPropString("proxy.password", null);
            if (proxyUser != null) {
                AuthScope authScope = new AuthScope(proxyHost, proxyPort);
                Credentials proxyCreds =
                    new UsernamePasswordCredentials(proxyUser, proxyPass);
                httpClient.getState().setProxyCredentials(authScope, proxyCreds);
            }
        }
        
        // Authenticate user and initiate session
        PostMethod postMethod = new PostMethod(hostAddress + "account/authenticate");
        
        postMethod.addParameter("user[username]", uid);
        postMethod.addParameter("user[password]", password);
        
        int response = httpClient.executeMethod(postMethod);
        
        if (response == 407) {
            // proxy auth required
        }
        
        if (response > 400) {
            error("Unrecognized response from the server"); // TODO i18n
            return this;
        }
        
        // Check authentication result
        Header statusHeader = postMethod.getResponseHeader("X-mygame-status");
        if (statusHeader == null) {
            error("Unrecognized response from the server"); // TODO i18n
            return this;
        }
        else if (!statusHeader.getValue().equals("0 OK")) {
            error("Invalid username or password"); // TODO i18n
            return this;
        }
        
        // Send the scenario and associated info
        Part [] parts = {
                new StringPart("scenario[title]", gameName),
                new StringPart("scenario[main_class]", "greenfoot.export.GreenfootScenarioViewer"),
                new StringPart("scenario[width]", "" + width),
                new StringPart("scenario[height]", "" + height),
                new StringPart("scenario[short_description]", shortDescription),
                new StringPart("scenario[long_description]", longDescription),
                new FilePart("scenario[uploaded_data]", new File(fileName))
        };
        
        postMethod = new PostMethod(hostAddress + "upload-scenario");
        postMethod.setRequestEntity(new MultipartRequestEntity(parts,
                postMethod.getParams()));
        
        response = httpClient.executeMethod(postMethod);
        if (response > 400) {
            error("Unrecognized response from the server"); // TODO i18n
            return this;
        }
        
        statusHeader = postMethod.getResponseHeader("X-mygame-status");
        if (statusHeader == null) {
            error("Unrecognized response from the server"); // TODO i18n
            return this;
        }
        else if (!statusHeader.getValue().equals("0 OK")) {
            error("Error while uploading scenario to server"); // TODO i18n
            return this;
        }
        
        // Done.
        status("Upload complete.");
        
        return this;
    }
    
    public abstract void error(String s);
    
    public abstract void status(String s);
     
}
