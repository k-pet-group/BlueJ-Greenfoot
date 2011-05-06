/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.export.mygame;

import greenfoot.event.PublishEvent;
import greenfoot.event.PublishListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.logging.LogFactory;
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
public class MyGameClient
{
    private PublishListener listener;
    
    public MyGameClient(PublishListener listener)
    {
        this.listener = listener;
        
        // Disable logging, prevents guff going to System.err
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }
    
    public final MyGameClient submit(String hostAddress, String uid, String password,
            String jarFileName, File sourceFile, File screenshotFile, int width, int height,
            ScenarioInfo info)
        throws UnknownHostException, IOException
    {
        String gameName = info.getTitle();
        String shortDescription = info.getShortDescription();
        String longDescription = info.getLongDescription();
        String updateDescription = info.getUpdateDescription();
        String gameUrl = info.getUrl();
        
        HttpClient httpClient = getHttpClient();
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(20 * 1000); // 20s timeout
        
        // Authenticate user and initiate session
        PostMethod postMethod = new PostMethod(hostAddress + "account/authenticate");
        
        postMethod.addParameter("user[username]", uid);
        postMethod.addParameter("user[password]", password);
        
        int response = httpClient.executeMethod(postMethod);
        
        if (response == 407 && listener != null) {
            // proxy auth required
            String[] authDetails = listener.needProxyAuth();
            if (authDetails != null) {
                String proxyHost = httpClient.getHostConfiguration().getProxyHost();
                int proxyPort = httpClient.getHostConfiguration().getProxyPort();
                AuthScope authScope = new AuthScope(proxyHost, proxyPort);
                Credentials proxyCreds =
                    new UsernamePasswordCredentials(authDetails[0], authDetails[1]);
                httpClient.getState().setProxyCredentials(authScope, proxyCreds);

                // Now retry:
                response = httpClient.executeMethod(postMethod);
            }
        }
        
        if (response > 400) {
            error(Config.getString("export.publish.errorResponse") + " - " + response);
            return this;
        }
        
        // Check authentication result
        if(! handleResponse(postMethod)) {
            return this;
        }
        
        // Send the scenario and associated info
        List<String> tagsList = info.getTags();
        boolean hasSource = sourceFile != null;
        //determining the number of parts to send through
        //use a temporary map holder
        Map<String, String> partsMap = new HashMap<String, String>();
        if (info.isUpdate()){
            partsMap.put("scenario[update_description]", updateDescription);
        }
        else {
            partsMap.put("scenario[long_description]", longDescription);
            partsMap.put("scenario[short_description]", shortDescription);
        }
        int size = partsMap.size();
       
        if (screenshotFile!= null){
            size=size+1;
        }

        //base number of parts is 6
        int counter=6;
        Part [] parts = new Part[ counter + size + tagsList.size() + (hasSource ? 1 : 0)];
        parts[0] = new StringPart("scenario[title]", gameName, "UTF-8");
        parts[1] = new StringPart("scenario[main_class]", "greenfoot.export.GreenfootScenarioViewer", "UTF-8");
        parts[2] = new StringPart("scenario[width]", "" + width, "UTF-8");
        parts[3] = new StringPart("scenario[height]", "" + height, "UTF-8");
        parts[4] = new StringPart("scenario[url]", gameUrl, "UTF-8");
        parts[5] = new ProgressTrackingPart("scenario[uploaded_data]", new File(jarFileName), this);
        Iterator <String> mapIterator=partsMap.keySet().iterator();
        String key="";
        String obj="";
        while (mapIterator.hasNext()){
            key = mapIterator.next().toString();
            obj = partsMap.get(key).toString();
            parts[counter]= new StringPart(key, obj, "UTF-8");
            counter=counter+1;
        }
        
        if (hasSource) {
            parts[counter] = new ProgressTrackingPart("scenario[source_data]", sourceFile, this);
            counter=counter+1;
        }
        if (screenshotFile!= null){
            parts[counter] = new ProgressTrackingPart("scenario[screenshot_data]", screenshotFile, this);
            counter=counter+1;
        }

        int tagNum = 0;
        for (Iterator<String> i = tagsList.iterator(); i.hasNext(); ) {
            parts[counter] = new StringPart("scenario[tag" + tagNum++ + "]", i.next());
            counter=counter+1;
        }
        
        postMethod = new PostMethod(hostAddress + "upload-scenario");
        postMethod.setRequestEntity(new MultipartRequestEntity(parts,
                postMethod.getParams()));
        
        response = httpClient.executeMethod(postMethod);
        if (response > 400) {
            error(Config.getString("export.publish.errorResponse") + " - " + response);
            return this;
        }
        
        if(! handleResponse(postMethod)) {
            return this;
        }
        
        // Done.
        listener.uploadComplete(new PublishEvent(PublishEvent.STATUS));
        
        return this;
    }

    /**
     * Checks the result of of the given method. Should only be called after the
     * postMethod has been executed.
     * 
     * If the check is successful the method will return true. Otherwise it will
     * print an error message and return false.
     * 
     * @param postMethod The method to check the result for.
     * @return True if the execution was successful, false otherwise.
     * @throws NumberFormatException
     */
    private boolean handleResponse(PostMethod postMethod)
    {
        Header statusHeader = postMethod.getResponseHeader("X-mygame-status");
        if (statusHeader == null) {
            error(Config.getString("export.publish.errorResponse"));
            return false;
        }
        String responseString = statusHeader.getValue();
        int spaceIndex = responseString.indexOf(" ");
        if (spaceIndex == -1) {
            error(Config.getString("export.publish.errorResponse"));
            return false;
        }
        try {
            int statusCode = Integer.parseInt(responseString.substring(0, spaceIndex));
            switch(statusCode) {
            case 0 :
                // Everything is good!
                return true;
            case 1 :
                error(Config.getString("export.publish.errorPassword"));
                return false;
            case 2 :
                error(Config.getString("export.publish.errorTooLarge"));
                return false;
            default :
                // Unknown error - print it!
                error(responseString.substring(spaceIndex + 1));
            return false;
            }
        }
        catch (NumberFormatException nfe) {
            error(Config.getString("export.publish.errorResponse"));
            return false;
        }
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
     * @param info        If not null, this will on return have the title, short description,
     *                    long description and tags set to whatever the existing scenario has. 
     * @return  true iff the scenario exists on the server
     */
    public boolean checkExistingScenario(String hostAddress, String uid,
            String gameName, ScenarioInfo info)
        throws UnknownHostException, IOException
    {
        HttpClient client = getHttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(20 * 1000);
        // 20 second timeout is quite generous
        
        String encodedName = URLEncoder.encode(gameName, "UTF-8");
        encodedName = encodedName.replace("+", "%20");
        GetMethod getMethod = new GetMethod(hostAddress +
                "user/"+ uid + "/check_scenario/" + encodedName);
        
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
                    else if (element.getTagName().equals("webpage")) {
                        info.setUrl(element.getTextContent());
                    }
                    else if (element.getTagName().equals("hassource")) {
                        info.setHasSource(element.getTextContent().equals("true"));
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
     * 
     * @throws UnknownHostException if the host is unknown
     * @throws org.apache.commons.httpclient.ConnectTimeoutException if the connection timed out
     * @throws IOException if some other I/O exception occurs
     */
    public List<String> getCommonTags(String hostAddress, int maxNumberOfTags)
        throws UnknownHostException, IOException
    {
        HttpClient client = getHttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(20 * 1000);
        
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
    
    /**
     * An error occurred.
     */
    private void error(String s)
    {
        listener.errorRecieved(new PublishEvent(s, PublishEvent.ERROR));
    }
    
    /**
     * The specified number of bytes have just been sent.
     */
    public void progress(int bytes)
    {
        listener.progressMade(new PublishEvent(bytes, PublishEvent.PROGRESS));
    }
    
    /**
     * Prompt the user for proxy authentication details (username and password).
     * 
     * @return A 2-element array with the username as the first element and the password as the second,
     *         or null if the user elected to cancel the upload.
     */
    public String[] promptProxyAuth()
    {
        return listener.needProxyAuth();
    }
}
