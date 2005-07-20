package bluej.runtime;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import bluej.classmgr.ClassPath;
import bluej.classmgr.ProjectClassLoader;
import javax.swing.*;

/**
 * This class is now just a place to hold a method, it is worth to consider to scrap it.
 *
 * @author Andrew Patterson
 * @version $Id: RemoteClassMgr.java 3473 2005-07-20 18:00:29Z damiano $
 */
public class RemoteClassMgr
{
    /**
     * Return a project specific class loader
     */
    public URLClassLoader newURLClassLoader(String urlListAsString)
    {   
        URL [] urls = convertStringToUrlArray(urlListAsString);
        return new URLClassLoader(urls);
    }
    
    /**
     * Return an arry of URL as the result of converting a String of URL separated by \n into an array of URL.
     * @param urlListAsString 
     * @return 
     */
    private URL [] convertStringToUrlArray(String urlListAsString)
    {
        ArrayList risul = new ArrayList();
        
        String [] splits = urlListAsString.split("\n");
        for ( int index=0; index<splits.length; index++ )  
            try {
                risul.add(new URL(splits[index]));
            }
            catch (MalformedURLException mfue) {
                // Should never happen but if it does we want to know about it
                JOptionPane.showMessageDialog(null,"RemoteClassMgr() Malformed URL="+splits[index]);
            }
            
        return (URL [])risul.toArray(new URL[risul.size()]);
    }

}
