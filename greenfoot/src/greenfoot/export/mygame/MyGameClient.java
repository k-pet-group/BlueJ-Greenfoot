package greenfoot.export.mygame;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
        
        // Debug stuff begins
        
        List<String> commonTags = getCommonTags(hostAddress, 5);
        for (Iterator<String> ii = commonTags.iterator(); ii.hasNext(); ) {
            System.out.println("Common tag: " + ii.next());
        }
        
        ScenarioInfo oldInfo = new ScenarioInfo();
        if (checkExistingScenario(hostAddress, uid, gameName, oldInfo)) {
            System.out.println("Old scenario exists with that name:");
            System.out.println("  short = " + oldInfo.getShortDescription());
            System.out.println("  long = " + oldInfo.getLongDescription());
            List<String> tags = oldInfo.getTags();
            for (Iterator<String> ii = tags.iterator(); ii.hasNext(); ) {
                System.out.println("  tag: " + ii.next());
            }
        }
        else {
            System.out.println("No old scenario with that name.");
        }
        
        // Debug stuff ends
        
        HttpClient httpClient = getHttpClient();
        
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
        List<String> tagsList = info.getTags();
        boolean hasSource = sourceFile != null;
        Part [] parts = new Part[8 + tagsList.size() + (hasSource ? 1 : 0)];
        
        parts[0] = new StringPart("scenario[title]", gameName);
        parts[1] = new StringPart("scenario[main_class]", "greenfoot.export.GreenfootScenarioViewer");
        parts[2] = new StringPart("scenario[width]", "" + width);
        parts[3] = new StringPart("scenario[height]", "" + height);
        parts[4] = new StringPart("scenario[short_description]", shortDescription);
        parts[5] = new StringPart("scenario[long_description]", longDescription);
        parts[6] = new FilePart("scenario[uploaded_data]", new File(jarFileName));
        parts[7] = new FilePart("scenario[screenshot_data]", screenshotFile);
        int tagindex = 8;
        if (hasSource) {
            parts[8] = new FilePart("scenario[source_data]", sourceFile);
            tagindex = 9;
        }
        
        int tagNum = 0;
        for (Iterator<String> i = tagsList.iterator(); i.hasNext(); ) {
            parts[tagindex++] = new StringPart("scenario[tag" + tagNum++ + "]", i.next());
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
     * Get a http client, configured to use the proxy if specified in Greenfoot config
     */
    protected HttpClient getHttpClient()
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
        
        return httpClient;
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
        HttpClient client = getHttpClient();
        
        GetMethod getMethod = new GetMethod(hostAddress +
                "user/"+ uid + "/check_scenario/" + gameName);
        
        int response = client.executeMethod(getMethod);
        if (response > 400) {
            throw new IOException("HTTP error response " + response + " from server.");
        }
        
        Header statusHeader = getMethod.getResponseHeader("X-mygame-scenario");
        if (statusHeader == null) {
            // Weird.
            throw new IOException("X-mygame-scenario header missing from server response");
        }
        else if (!statusHeader.getValue().equals("0 FOUND")) {
            // not found
            return false;
        }

        // found - now we can parse the response
        if (info != null) {
            InputStream responseStream = getMethod.getResponseBodyAsStream();
            parseScenarioXml(info, responseStream);
            info.setTitle(gameName);
        }
        
        return true;
    }
    
    private void parseScenarioXml(ScenarioInfo info, InputStream xmlStream)
        throws IOException
    {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbuilder = dbf.newDocumentBuilder();

            Document doc = dbuilder.parse(xmlStream);
            Element root = doc.getDocumentElement();
            if (root == null || !root.getTagName().equals("scenario")) {
                return;
            }
            
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node childNode = children.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) childNode;
                    if (element.getTagName().equals("shortdescription")) {
                        info.setShortDescription(element.getTextContent());
                    }
                    else if (element.getTagName().equals("longdescription")) {
                        info.setLongDescription(element.getTextContent());
                    }
                    else if (element.getTagName().equals("taglist")) {
                        info.setTags(parseTagListXmlElement(element));
                    }
                }
            }
        }
        catch (ParserConfigurationException pce) {
            // what the heck do we do with this?
        }
        catch (SAXException saxe) {
            
        }
    }
    
    private List<String> parseTagListXmlElement(Element element)
    {
        List<String> tags = new ArrayList<String>();
        
        Node child = element.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                element = (Element) child;
                if (element.getTagName().equals("tag")) {
                    tags.add(element.getTextContent());
                }
            }
            child = child.getNextSibling();
        }
        
        return tags;
    }
    
    /**
     * Get a list of commonly used tags from the server.
     */
    public List<String> getCommonTags(String hostAddress, int maxNumberOfTags)
        throws UnknownHostException, IOException
    {
        HttpClient client = getHttpClient();
        
        GetMethod getMethod = new GetMethod(hostAddress +
                "common-tags/"+ maxNumberOfTags);
        
        int response = client.executeMethod(getMethod);
        if (response > 400) {
            throw new IOException("HTTP error response " + response + " from server.");
        }
        
        // found - now we can parse the response
        InputStream responseStream = getMethod.getResponseBodyAsStream();
        
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbuilder = dbf.newDocumentBuilder();

            Document doc = dbuilder.parse(responseStream);
            Element root = doc.getDocumentElement();
            if (root == null || !root.getTagName().equals("taglist")) {
                return Collections.<String>emptyList();
            }

            return parseTagListXmlElement(root);
        }
        catch(SAXException saxe) { }
        catch(ParserConfigurationException pce) { }
        finally {
            responseStream.close();
        }
        
        return Collections.<String>emptyList();
    }
    
    public abstract void error(String s);
    
    public abstract void status(String s);
     
}
