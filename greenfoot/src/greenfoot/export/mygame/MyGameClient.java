/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2017,2018  Poul Henriksen and Michael Kolling
 
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import bluej.Config;
import greenfoot.event.PublishEvent;
import greenfoot.event.PublishListener;

/**
 * MyGame client.
 * 
 * @author Davin McCall
 */
public class MyGameClient
{
    private PublishListener listener;
    
    /**
     * Construct a MyGameClient instance, which issues updates/error responses to a specified listener.
     * 
     * @param listener  The listener to receive progress update / error notifications
     */
    public MyGameClient(PublishListener listener)
    {
        this.listener = listener;
        
        // Disable logging, prevents guff going to System.err
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }
    
    public final MyGameClient submit(String hostAddress, String uid, String password,
            String jarFileName, File sourceFile, File screenshotFile, int width, int height,
            ExportInfo info)
        throws IOException
    {
        DefaultHttpClient httpClient = getHttpClient();
        HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 20 * 1000); // 20s timeout
        
        // Authenticate user and initiate session
        HttpPost postMethod = new HttpPost(hostAddress + "account/authenticate");
       
        MultipartEntity postParams = new MultipartEntity();
        Charset utf8 = StandardCharsets.UTF_8;
        postParams.addPart(new FormBodyPart("user[username]", new StringBody(uid, utf8)));
        postParams.addPart(new FormBodyPart("user[password]", new StringBody(password, utf8)));
        postMethod.setEntity(postParams);
        
        HttpResponse httpResponse = httpClient.execute(postMethod);
        int response = httpResponse.getStatusLine().getStatusCode();
        
        if (response == 407 && listener != null) {
            // proxy auth required
            String[] authDetails = listener.needProxyAuth();
            if (authDetails != null) {
                Object defProxy = httpClient.getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY);
                if (defProxy instanceof HttpHost) {
                    HttpHost proxy = (HttpHost) defProxy;
                    AuthScope authScope = new AuthScope(proxy.getHostName(), proxy.getPort());
                    Credentials proxyCreds =
                            new UsernamePasswordCredentials(authDetails[0], authDetails[1]);
                    httpClient.getCredentialsProvider().setCredentials(authScope, proxyCreds);

                    // Now retry:
                    httpResponse = httpClient.execute(postMethod);
                    response = httpResponse.getStatusLine().getStatusCode();
                }
            }
        }
        
        if (response > 400) {
            error(Config.getString("export.publish.errorResponse") + " - " + response);
            httpClient.getConnectionManager().shutdown();
            return this;
        }
        
        // Check authentication result
        if(! handleResponse(httpResponse)) {
            httpClient.getConnectionManager().shutdown();
            return this;
        }
                
        EntityUtils.consume(httpResponse.getEntity());
        
        // Send the scenario and associated info
        List<String> tagsList = info.getTags();
        boolean hasSource = sourceFile != null;
        //determining the number of parts to send through
        //use a temporary map holder
        Map<String, String> partsMap = new HashMap<>();
        if (info.isUpdate())
        {
            partsMap.put("scenario[update_description]", info.getUpdateDescription());
        }
        else
        {
            partsMap.put("scenario[long_description]", info.getLongDescription());
            partsMap.put("scenario[short_description]", info.getShortDescription());
        }

        MultipartEntity mpe = new MultipartEntity();
        mpe.addPart(new FormBodyPart("scenario[title]", new StringBody(info.getTitle(), utf8)));
        mpe.addPart(new FormBodyPart("scenario[main_class]",
                new StringBody("greenfoot.export.GreenfootScenarioViewer", utf8)));
        mpe.addPart(new FormBodyPart("scenario[width]", new StringBody("" + width, utf8)));
        mpe.addPart(new FormBodyPart("scenario[height]", new StringBody("" + height, utf8)));
        mpe.addPart(new FormBodyPart("scenario[url]", new StringBody(info.getUrl(), utf8)));
        mpe.addPart(new ProgressTrackingPart("scenario[uploaded_data]", new File(jarFileName), this));

        for (String key : partsMap.keySet())
        {
            mpe.addPart(new FormBodyPart(key, new StringBody(partsMap.get(key), utf8)));
        }

        if (hasSource)
        {
            mpe.addPart(new ProgressTrackingPart("scenario[source_data]", sourceFile, this));
        }
        if (screenshotFile!= null)
        {
            mpe.addPart(new ProgressTrackingPart("scenario[screenshot_data]", screenshotFile, this));
        }

        int tagNum = 0;
        for (String tag : tagsList)
        {
            mpe.addPart(new FormBodyPart("scenario[tag" + tagNum++ + "]", new StringBody(tag, utf8)));
        }
        
        postMethod = new HttpPost(hostAddress + "upload-scenario");
        postMethod.setEntity(mpe);
        
        httpResponse = httpClient.execute(postMethod);
        response = httpResponse.getStatusLine().getStatusCode();
        if (response > 400)
        {
            error(Config.getString("export.publish.errorResponse") + " - " + response);
            httpClient.getConnectionManager().shutdown();
            return this;
        }
        
        if (!handleResponse(httpResponse))
        {
            httpClient.getConnectionManager().shutdown();
            return this;
        }

        EntityUtils.consume(httpResponse.getEntity());
        
        // Done.
        listener.uploadComplete(new PublishEvent(PublishEvent.STATUS));
        
        httpClient.getConnectionManager().shutdown();
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
     */
    private boolean handleResponse(HttpResponse postMethod)
    {        
        Header statusHeader = postMethod.getLastHeader("X-mygame-status");
        if (statusHeader == null)
        {
            error(Config.getString("export.publish.errorResponse"));
            return false;
        }

        String responseString = statusHeader.getValue();
        int spaceIndex = responseString.indexOf(" ");
        if (spaceIndex == -1)
        {
            error(Config.getString("export.publish.errorResponse"));
            return false;
        }
        try
        {
            int statusCode = Integer.parseInt(responseString.substring(0, spaceIndex));
            switch(statusCode)
            {
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
        catch (NumberFormatException nfe)
        {
            error(Config.getString("export.publish.errorResponse"));
            return false;
        }
    }
    
    /**
     * Get a http client, configured to use the proxy if specified in Greenfoot config
     */
    private DefaultHttpClient getHttpClient()
    {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 20 * 1000); // 20s timeout
        DefaultHttpClient httpClient = new DefaultHttpClient(params);
        
        String proxyHost = Config.getPropString("proxy.host", null);
        String proxyPortStr = Config.getPropString("proxy.port", null);
        if (proxyHost != null && proxyHost.length() != 0 && proxyPortStr != null)
        {
            int proxyPort = 80;
            try
            {
                proxyPort = Integer.parseInt(proxyPortStr);
            }
            catch (NumberFormatException nfe) {}

            HttpHost proxy = new HttpHost(proxyHost, proxyPort, "http");
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            
            String proxyUser = Config.getPropString("proxy.user", null);
            String proxyPass = Config.getPropString("proxy.password", null);
            if (proxyUser != null)
            {
                AuthScope authScope = new AuthScope(proxyHost, proxyPort);
                Credentials proxyCreds = new UsernamePasswordCredentials(proxyUser, proxyPass);
                httpClient.getCredentialsProvider().setCredentials(authScope, proxyCreds);
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
     * @return  true if the scenario exists on the server
     */
    public boolean checkExistingScenario(String hostAddress, String uid,
            String gameName, ScenarioInfo info)
        throws IOException
    {
        HttpClient client = getHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 20 * 1000);
        // 20 second timeout is quite generous
        
        String encodedName = URLEncoder.encode(gameName, "UTF-8");
        encodedName = encodedName.replace("+", "%20");
        HttpGet getMethod = new HttpGet(hostAddress +
                "user/"+ uid + "/check_scenario/" + encodedName);
        
        HttpResponse httpResponse = client.execute(getMethod);
        int response = httpResponse.getStatusLine().getStatusCode();
        if (response > 400)
        {
            throw new IOException("HTTP error response " + response + " from server.");
        }
        
        Header statusHeader = httpResponse.getLastHeader("X-mygame-scenario");
        if (statusHeader == null)
        {
            // Weird.
            throw new IOException("X-mygame-scenario header missing from server response");
        }
        else if (!statusHeader.getValue().equals("0 FOUND"))
        {
            // not found
            return false;
        }

        // found - now we can parse the response
        if (info != null)
        {
            InputStream responseStream = httpResponse.getEntity().getContent();
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
            for (int i = 0; i < children.getLength(); i++)
            {
                Node childNode = children.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element element = (Element) childNode;
                    switch (element.getTagName())
                    {
                        case "shortdescription":
                            info.setShortDescription(element.getTextContent());
                            break;
                        case "longdescription":
                            info.setLongDescription(element.getTextContent());
                            break;
                        case "taglist":
                            info.setTags(parseTagListXmlElement(element));
                            break;
                        case "webpage":
                            info.setUrl(element.getTextContent());
                            break;
                        case "hassource":
                            info.setIncludeSource(element.getTextContent().equals("true"));
                            break;
                    }
                }
            }
        }
        catch (ParserConfigurationException pce)
        {
            // what the heck do we do with this?
        }
        catch (SAXException saxe) { }
    }
    
    private List<String> parseTagListXmlElement(Element element)
    {
        List<String> tags = new ArrayList<>();
        
        Node child = element.getFirstChild();
        while (child != null)
        {
            if (child.getNodeType() == Node.ELEMENT_NODE)
            {
                element = (Element) child;
                if (element.getTagName().equals("tag"))
                {
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
     * @throws org.apache.http.conn.ConnectTimeoutException if the connection timed out
     * @throws IOException if some other I/O exception occurs
     */
    public List<String> getCommonTags(String hostAddress, int maxNumberOfTags)
        throws UnknownHostException, IOException
    {
        HttpClient client = getHttpClient();
        
        HttpGet getMethod = new HttpGet(hostAddress +
                "common-tags/"+ maxNumberOfTags);
        
        HttpResponse httpResponse = client.execute(getMethod); 
        int response = httpResponse.getStatusLine().getStatusCode();
        if (response > 400) {
            throw new IOException("HTTP error response " + response + " from server.");
        }
        
        // found - now we can parse the response
        try (InputStream responseStream = httpResponse.getEntity().getContent())
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();

            Document doc = documentBuilder.parse(responseStream);
            Element root = doc.getDocumentElement();
            if (root == null || !root.getTagName().equals("taglist"))
            {
                return Collections.emptyList();
            }

            return parseTagListXmlElement(root);
        }
        catch (SAXException | ParserConfigurationException ignore) { }

        return Collections.emptyList();
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
