package bluej;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * The URLDecoder properly decodes special characters in URLs.
 * ATTENTION: The class needs jdk 1.4 to be compiled!
 * This class cen be removed, and its code inlined, once we are using jdk 1.4+.
 *
 * @author  Michael Kolling
 * @version $Id: URLDecoder.java 2102 2003-07-08 14:01:17Z mik $
 */
public class URLDecoder
{
    /**
     * Return the path element, properly decoded.
     */
    public static String getPath(String url)
    {
        try {
            return new URI(url).getPath();
        } 
        catch (URISyntaxException e) {
            return null;
        }
    }
}
