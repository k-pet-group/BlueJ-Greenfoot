package bluej.classmgr;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.net.*;

/**
 * Class to maintain a list of ClassPathEntry's.
 *
 * @author  Andrew Patterson
 * @version $Id: ClassPath.java 1818 2003-04-10 13:31:55Z fisker $
 */
public class ClassPath
{
    /**
     * The actual list of class path entries
     */
    private ArrayList entries = new ArrayList();

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
     * @param   an array of File URLs
     */
    public ClassPath(URL urls[])
    {
	for(int i=0; i<urls.length; i++) {
            ClassPathEntry cpe = new ClassPathEntry(urls[i].getFile(), "");

            if(!entries.contains(cpe))
                entries.add(cpe);
        }
    }

    /**
     * Return the list of entries
     */
    protected List getEntries()
    {
        return entries;
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

                entries.remove(entry);
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

        Iterator it = classpath.entries.iterator();

        while (it.hasNext()) {

            ClassPathEntry nextEntry = (ClassPathEntry)it.next();

            try {
                ClassPathEntry cpentry = (ClassPathEntry)nextEntry.clone();

                if(!entries.contains(cpentry))
                    entries.add(cpentry);
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
                String name = (new File(entry)).getName();
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
     * Return the class path entries as an array of URL's
     */
    public URL[] getURLs()
    {
        Iterator it = entries.iterator();
        URL u[] = new URL[entries.size()];
        int current = 0;

        while (it.hasNext()) {
            ClassPathEntry nextEntry = (ClassPathEntry)it.next();

            try {
                u[current] = nextEntry.getURL();
                // Debug.message(u[current].toString());
            } catch(MalformedURLException mue) {

            }

            current++;
        }

        return u;
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
        Iterator it = entries.iterator();

        while (it.hasNext()) {
            ClassPathEntry nextEntry = (ClassPathEntry)it.next();

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

    public String toString()
    {
        StringBuffer buf = new StringBuffer();

        Iterator it = entries.iterator();

        while (it.hasNext()) {
            ClassPathEntry nextEntry = (ClassPathEntry)it.next();

            buf.append(nextEntry.getPath());
            buf.append(File.pathSeparatorChar);
        }

        return buf.toString();
    }
}
