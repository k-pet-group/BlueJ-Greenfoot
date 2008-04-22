package greenfoot.export.mygame;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

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
            String jarFileName, File sourceFile, File screenshotFile, int width, int height,
            ScenarioInfo info)
        throws UnknownHostException, IOException
    {
        String gameName = info.getTitle();
        String shortDescription = info.getShortDescription();
        String longDescription = info.getLongDescription();
        
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
        Part [] parts;
        boolean hasSource = sourceFile != null;
        if (hasSource) {
            parts = new Part[9];
        }
        else {
            parts = new Part[8];
        }
        
        parts[0] = new StringPart("scenario[title]", gameName);
        parts[1] = new StringPart("scenario[main_class]", "greenfoot.export.GreenfootScenarioViewer");
        parts[2] = new StringPart("scenario[width]", "" + width);
        parts[3] = new StringPart("scenario[height]", "" + height);
        parts[4] = new StringPart("scenario[short_description]", shortDescription);
        parts[5] = new StringPart("scenario[long_description]", longDescription);
        parts[6] = new FilePart("scenario[uploaded_data]", new File(jarFileName));
        parts[7] = new FilePart("scenario[screenshot_data]", screenshotFile);
        if (hasSource) {
            parts[8] = new FilePart("scenario[source_data]", sourceFile);
        }
        
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
    
    /**
     * Check whether a pre-existing scenario with the given title exists. Returns true
     * if the scenario exists or false if not.
     * 
     * @param hostAddress   The game server address
     * @param uid           The username of the user
     * @param gameName      The scenario title
     * @param info        If not null, this will have the title, short description,
     *                    long description and tags set to whatever the existing
     *                    scenario has. 
     * @return
     */
    public boolean checkExistingScenario(String hostAddress, String uid,
            String gameName, ScenarioInfo info)
        throws UnknownHostException, IOException
    {
        // Just a stub for now
        return false;
    }
    
    /**
     * Get a list of commonly used tags from the server.
     */
    public List<String> getCommonTags(String hostAddress, int maxNumberOfTags)
        throws UnknownHostException, IOException
    {
        // just a stub for now
        List<String> rlist = new ArrayList<String>();
        rlist.add("game");
        rlist.add("simulation");
        return rlist;
    }
    
    public abstract void error(String s);
    
    public abstract void status(String s);
     
}
