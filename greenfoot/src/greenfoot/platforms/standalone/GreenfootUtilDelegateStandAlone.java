/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.platforms.standalone;

import greenfoot.GreenfootImage;
import greenfoot.platforms.GreenfootUtilDelegate;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class GreenfootUtilDelegateStandAlone implements GreenfootUtilDelegate
{
    /** Holds images for classes. Avoids loading the same image twice. Key is the filename */
    public static Map<String, GreenfootImage> classImages = new HashMap<String, GreenfootImage>();
    
    public void createSkeleton(String className, String superClassName, File file, String templateFileName)
    throws IOException
    {
        // Not needed in stand alone
    }

    public URL getResource(String path)
    {
        // Resources from the standalone should always be in a jar, which means
        // they should contain the character "!". If we do get a URL back, and
        // it doesn't contain a ! it is probably because it didn't exists, but
        // the webserver produced an error page at the given URL instead of
        // returning a fail. Therefore, we need to explicitly test for the
        // existence of a ! in the returned URL.
        URL res = this.getClass().getClassLoader().getResource(path);
        if (res != null && res.toString().contains("!")) {  
            return res;
        }
        else {
            if (path.indexOf('\\') != -1) {
                // Looks suspiciously like a Windows path.
                path = path.replace('\\', '/');
                res = this.getClass().getClassLoader().getResource(path);
                if (res != null && res.toString().contains("!")) {  
                    return res;
                }
            }
            return null;
        }
    }
    
    public Iterable<String> getResources(String path)
    {
        Set<String> files = new HashSet<String>();
        try
        {
            // We can't look for the subdirectory (e.g. sounds) because it won't be found as a file, we
            // must look for a given file, so let's look for our own class file:
            URL url = getResource(getClass().getName().replace(".", "/")+".class");
            // Inspired by http://www.uofr.net/~greg/java/get-resource-listing.html
            if (url != null && "jar".equals(url.getProtocol()))
            {
                String jarPath = url.getPath().substring(5, url.getPath().indexOf("!")); //strip out only the JAR file
                JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
                Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar                
                while(entries.hasMoreElements()) {
                  String name = entries.nextElement().getName();
                  if (name.startsWith(path) && name.length() > path.length()) { //filter according to the path
                    String entry = name.substring(path.length() + 1); // add one for slash
                    files.add(entry);                    
                  }
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("IO Exception reading JAR in getResources: " + e.toString());
        }
        return files;
    }

    /**
     * Returns the path to a small version of the greenfoot logo.
     */
    public String getGreenfootLogoPath()
    {    
        return this.getClass().getClassLoader().getResource("greenfoot.png").toString();
    }
    
    public void removeCachedImage(String fileName)
    {
        synchronized (classImages) {
            classImages.remove(fileName);
        }
    }
   

    public boolean addCachedImage(String fileName, GreenfootImage image)
    {
        synchronized (classImages) {
            classImages.put(fileName, image);
        }
        return true;
    }
    
    public GreenfootImage getCachedImage(String fileName)
    {
        synchronized (classImages) {
            return classImages.get(fileName);
        }
    }
    
    public boolean isNullCachedImage(String fileName)
    {
        if (classImages.containsKey(fileName) && classImages.get(fileName)==null){
            return true;
        }
        return false;
    }
    
    @Override
    public void displayMessage(Component parent, String messageText)
    {
        System.err.println(messageText);
    }
}
