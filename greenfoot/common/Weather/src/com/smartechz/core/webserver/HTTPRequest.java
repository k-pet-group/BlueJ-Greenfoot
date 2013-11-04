package com.smartechz.core.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.smartechz.core.stream.StreamReader;

public class HTTPRequest {
    
    private static InputStream getInputStream(String url)
            throws MalformedURLException, IOException
    {
        URLConnection gb = (new URL(url)).openConnection();
        return gb.getInputStream();
    }

    public static StreamReader getStreamReader(String url)
            throws MalformedURLException, IOException
    {
        return new StreamReader(getInputStream(url));
    }

    public static String getString(String url) throws MalformedURLException, IOException
    {
        StringBuffer sb = new StringBuffer();
        StreamReader in = HTTPRequest.getStreamReader(url);
        if (in.hasNextLine()) {
            sb.append(in.nextLine());
            while (in.hasNextLine()) {
                sb.append("\n");
                sb.append(in.nextLine());
            }
        }
        in.close();
        return sb.toString();
    }

    public static Document getXMLDoc(String url) throws MalformedURLException, IOException,
            ParserConfigurationException, SAXException
    {
        InputStream ip = HTTPRequest.getInputStream(url);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(ip);
        doc.getDocumentElement().normalize();

        return doc;
    }
}
