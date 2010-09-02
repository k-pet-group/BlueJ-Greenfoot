/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009, 2010  Poul Henriksen and Michael Kolling 
 
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
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
public abstract class MyGameClient
{
    public MyGameClient()
    {
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
        
        // Authenticate user and initiate session
        PostMethod postMethod = new PostMethod(hostAddress + "account/authenticate");
        
        postMethod.addParameter("user[username]", uid);
        postMethod.addParameter("user[password]", password);
        
        int response = httpClient.executeMethod(postMethod);
        
        if (response == 407) {
            // proxy auth required
        }
        
        if (response > 400) {
            error("Unrecognized response from the server: " + response); // TODO i18n
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
        boolean update=false;
        int index=5;
        if (updateDescription!=null && updateDescription.length()>0) {
            index=index+1;
            update=true;
        }
        if (screenshotFile!=null) {
            index=index+1;
        }
        //Initial export ->there is a short and long descrp 
        if (!update){
            index=index+2;
        }
        int tagindex = index+1;
        Part [] parts = new Part[index+1 + tagsList.size() + (hasSource ? 1 : 0)];
        parts[0] = new StringPart("scenario[title]", gameName);
        parts[1] = new StringPart("scenario[main_class]", "greenfoot.export.GreenfootScenarioViewer");
        parts[2] = new StringPart("scenario[width]", "" + width);
        parts[3] = new StringPart("scenario[height]", "" + height);
        parts[4] = new StringPart("scenario[url]", gameUrl);
        parts[5] = new ProgressTrackingPart("scenario[uploaded_data]", new File(jarFileName), this);
        switch (index){
        case 6:
            //can't be a initial export as that index is exactly 7
            //could be either a update desc or a screenshot
            if (updateDescription!=null && updateDescription.length()>0 ){
                parts[6] = new StringPart("scenario[update_description]", updateDescription); 
            }
            else {
                parts[6] = new ProgressTrackingPart("scenario[screenshot_data]", screenshotFile, this); 
            }  
            break;
        case 7:
            if (update)
            {
                //both a update desc or a screenshot
                parts[6] = new StringPart("scenario[update_description]", updateDescription); 
                parts[7] = new ProgressTrackingPart("scenario[screenshot_data]", screenshotFile, this);
            }
            else {
                parts[6] = new StringPart("scenario[short_description]", shortDescription);
                parts[7] = new StringPart("scenario[long_description]", longDescription);
            }
            break;
        case 8:
            parts[6] = new StringPart("scenario[short_description]", shortDescription);
            parts[7] = new StringPart("scenario[long_description]", longDescription);
            parts[8] = new ProgressTrackingPart("scenario[screenshot_data]", screenshotFile, this);
        default:

            break;
        }
        if (hasSource) {
            parts[tagindex] = new ProgressTrackingPart("scenario[source_data]", sourceFile, this);
            tagindex++;
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
            error("Unrecognized response from the server: " + response); // TODO i18n
            return this;
        }
        
        if(! handleResponse(postMethod)) {
            return this;
        }
        
        // Done.
        status("Upload complete.");
        
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
            error("Unrecognized response from the server."); // TODO i18n
            return false;
        }
        String responseString = statusHeader.getValue();
        int spaceIndex = responseString.indexOf(" ");
        if (spaceIndex == -1) {
            error("Unrecognized response from the server."); // TODO i18n
            return false;
        }
        try {
            int statusCode = Integer.parseInt(responseString.substring(0, spaceIndex));
            switch(statusCode) {
            case 0 :
                // Everything is good!
                return true;
            case 1 :
                error("Invalid username or password"); // TODO i18n
                return false;
            case 2 :
                error("The scenario is too large"); // TODO i18n
                return false;
            default :
                // Unknown error - print it!
                error(responseString.substring(spaceIndex + 1));
            return false;
            }
        }
        catch (NumberFormatException nfe) {
            error("Unrecognized response from the server."); // TODO i18n
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
    
    /**
     * The specified number of bytes have just been sent.
     */
    public abstract void progress(int bytes);
     
}
