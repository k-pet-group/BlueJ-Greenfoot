/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013  Michael Kolling and John Rosenberg 
 
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
package bluej.classmgr;

import bluej.utility.Debug;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Class to maintain a list of ClassPathEntry's.
 *
 * @author  Andrew Patterson
 */
public class ClassPath
{
    /**
     * The actual list of class path entries
     */
    private ArrayList<ClassPathEntry> entries = new ArrayList<ClassPathEntry>();

    /**
     * Construct an empty ClassPath
     */
    public ClassPath()
    {
    }

    /**
     * Construct a ClassPath which is a copy of an existing ClassPath
     */
    public ClassPath(ClassPath classpath)
    {
        addClassPath(classpath);
    }

    /**
     * Construct a ClassPath from a delimitered String of entries
     *
     * @param   classpath   A ; or : seperated String with entries
     * @param   genericdescription  A String which can be used to
     *          generically describe these entries
     */
    public ClassPath(String classpath, String genericdescription)
    {
        addClassPath(classpath, genericdescription);
    }

    /**
     * Construct a Classpath from an array of URLs
     * 
     * @param urls
     *            an array of File URLs
     */
    public ClassPath(URL urls[])
    {
        for(int i=0; i<urls.length; i++) {
            try {
            ClassPathEntry cpe = new ClassPathEntry(new File(new URI(urls[i].toString())), "");

            if(!entries.contains(cpe))
                entries.add(cpe);
            }
            catch(URISyntaxException use) { }
        }
    }


    /**
     * Construct a Classpath from an array of Files.
     * @param files an array of File
     */
    public ClassPath(File files[])
    {
        for(int index=0; index<files.length; index++) {
            entries.add( new ClassPathEntry(files[index], ""));
        }
    }


    /**
     * Return the list of entries (mutable, so only for close friends)
     */
    protected List<ClassPathEntry> getEntries()
    {
        return entries;
    }

    /**
     * Return the list of entries (immutable)
     */
    public List<ClassPathEntry> getPathEntries()
    {
        return Collections.unmodifiableList(entries);
    }


    /**
     * Remove elements from the classpath
     *
     * @param   classpath   A ; or : separated String of class path entries to
     *                      remove
     */
    public void removeClassPath(String classpath)
    {
        try {
            StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);

            while(st.hasMoreTokens()) {
                String entry = st.nextToken();

                entries.remove(new ClassPathEntry(entry, ""));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove all entries from the class path
     */
    public void removeAll()
    {
        entries.clear();
    }

    /**
     * Add a copy of an existing ClassPath
     *
     * @param   classpath   A ClassPath object to add a copy of
     */
    public void addClassPath(ClassPath classpath)
    {
        // make a copy of the entries.. don't just add the entries to the
        // new class path

        Iterator<ClassPathEntry> it = classpath.entries.iterator();

        while (it.hasNext()) {

            ClassPathEntry nextEntry = it.next();

            try {
                ClassPathEntry cpentry = (ClassPathEntry)nextEntry.clone();

                if(!entries.contains(cpentry)) {
                    entries.add(cpentry);
                }
            } catch(CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add from a classpath string all the libraries which it references
     *
     * @param   classpath   a string containing a sequence of filenames
     *              separated by a path separator character
     * @param   genericdescription  a string which will be used as the
     *                  description for all entries created for
     *                  this classpath
     */
    public void addClassPath(String classpath, String genericdescription)
    {
        if (classpath == null)
            return;

        try {
            StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);

            while(st.hasMoreTokens()) {
                String entry = st.nextToken();
                ClassPathEntry cpentry = new ClassPathEntry(entry, genericdescription);

                if(!entries.contains(cpentry))
                    entries.add(cpentry);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * Return the class path entries as an ArrayList of URL.
     * @return a non null but possibly empty ArrayList of URL.
     */
    public ArrayList<URL> getURLs()
    {
        Iterator<ClassPathEntry> it = entries.iterator();
        ArrayList<URL> risul = new ArrayList<URL>();

        while (it.hasNext()) {
            ClassPathEntry path = it.next();

            try {
                risul.add(path.getURL());
            } catch(MalformedURLException mue) {
                Debug.reportError("ClassPath.getURLs() bad path="+path);
            }
        }

        return risul;
    }

    /**
     * Find a file in the classpath
     *
     * @param   filename    a string which specifies a file to look
     *              for throughout the class path
     *          this filename is in native slash seperated form
     *          ie foo/bar for UNIX and foo\bar for Windows
     */
    public InputStream getFile(String filename) throws IOException
    {
        Iterator<ClassPathEntry> it = entries.iterator();

        while (it.hasNext()) {
            ClassPathEntry nextEntry = it.next();

            // each entry can be either a jar/zip file or a directory
            // or neither in which case we ignore it

            if(nextEntry.isJar()) {
                InputStream ret = readJar(nextEntry.getFile(), filename);

                if (ret != null)
                    return ret;
            } else if (nextEntry.isClassRoot()) {
                File fd = new File(nextEntry.getFile(), filename);

                if(fd.exists())
                    return new FileInputStream(fd);
            }
        }
        return null;
    }

    /**
     * Retrieve an entry out of a jar file
     *
     * @param   classjar    a file representing the jar to look in
     * @param   filename    a string which specifies a file to look
     *              for in the jar
     */
    private InputStream readJar(File classjar, String filename) throws IOException
    {
        JarFile jarf = new JarFile(classjar);

        // filenames are passed into us in native slash separated form.
        // jar files require us to always use the forward slash when looking
        // for files so if we are on a system where / is not the actual
        // separator character we have to first fix the filename up

        if(File.separatorChar != '/')
            filename = filename.replace(File.separatorChar, '/');

        JarEntry entry = jarf.getJarEntry(filename);

        if(entry == null) {
            return null;
        }

        InputStream is = jarf.getInputStream(entry);

        return is;
    }

    /**
     * Create a string with this class path as a separated list of strings.
     * The separator character is system dependent (see File.pathSeparatorChar).
     * 
     * @return  The classpath as string.
     */
    public String toString()
    {
        return asList(File.pathSeparatorChar, false);
    }
    
    /**
     * Create a string with this class path as a separated list of strings.
     * The separator character can be specified.
     * 
     * @param separator  The character to be used to separate entries.
     * @param useURL    
     * @return  The classpath as string.
     */
    public String asList(char separator, boolean useURL)
    {
        StringBuffer buf = new StringBuffer();

        Iterator<ClassPathEntry> it = entries.iterator();

        while (it.hasNext()) {
            ClassPathEntry nextEntry = it.next();

            if(useURL) {
                try {
                    buf.append(nextEntry.getURL());
                }
                catch (MalformedURLException e) {}
            } else
                buf.append(nextEntry.getPath());
            // we want to append a separator to all but the last entry
            if(it.hasNext())
                buf.append(separator);
        }

        return buf.toString();        
    }
}
