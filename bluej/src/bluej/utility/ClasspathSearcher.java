package bluej.utility;

import bluej.utility.Debug;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

/**
 ** @version $Id: ClasspathSearcher.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** Class to maintain a classpath and search it for files.
 **/
public class ClasspathSearcher
{
    private static final String colon = File.pathSeparator;
    private Vector cp_entries;
	
    public ClasspathSearcher(String classpath)
    {
	cp_entries = new Vector();
		
	if(classpath != null)
	    addClasspath(classpath);
    }
	
    public ClasspathSearcher()
    {
	this(null);
    }

    /**
     * Add elements to the classpath this object searches.
     */
    public void addClasspath(String classpath)
    {
	try {
	    StringTokenizer st = new StringTokenizer(classpath, colon);
	    while(st.hasMoreTokens()) {
		String entry = st.nextToken();
		// Debug.message("ClasspathSearcher: considering " + entry);
		File entryFile = new File(entry);
		if(entryFile.exists() && !cp_entries.contains(entry)) {
		    cp_entries.addElement(entry);
		    // Debug.message("ClasspathSearcher: " + entry + " accepted");
		}
	    }
	} catch(Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Remove elements from the classpath this object searches
     */
    public void removeClasspath(String classpath)
    {
	try {
	    StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);
	    while(st.hasMoreTokens()) {
		String entry = st.nextToken();
		cp_entries.removeElement(entry);
	    }
	} catch(Exception e) {
	    e.printStackTrace();
	}
    }
	
    /**
     * Find the full path name of a class archive in the classpath
     */
    public String getFileName(String archiveName)
    {
	for(int i = 0; i < cp_entries.size(); i++) {
	    String entry = (String)cp_entries.elementAt(i);
	    
	    if(entry.endsWith(archiveName))
		return entry;
	}

	return null;
    }

    /**
     * Find a classfile in the classpath
     */
    public InputStream getFile(String filename) throws IOException
    {
	for(int i = 0; i < cp_entries.size(); i++) {
	    String entry = (String)cp_entries.elementAt(i);
	    
	    if(entry.endsWith(".zip") || entry.endsWith(".jar")) {
		InputStream ret = readZip(entry, filename);
		if(ret !=null)
		    return ret;
	    }
	    else {
		if(File.separatorChar != '/')
		    filename = filename.replace('/', File.separatorChar);
		String fullpath = entry + File.separator + filename;
		File fd = new File(fullpath);
		
		if(fd.exists())
		    return new FileInputStream(fd);
		// else
		// Debug.message("ClasspathSearcher: file " + filename + " not found in " + entry);
	    }
	}

	return null;
    }

    private InputStream readZip(String classzip, String filename) throws IOException
    {
	ZipFile zipf = new ZipFile(classzip);
	ZipEntry entry = zipf.getEntry(filename);

	if(entry == null)
	    return null;

	// long size = entry.getCompressedSize();

	InputStream is = zipf.getInputStream(entry);

	zipf.close();

	return is;
    }
}
