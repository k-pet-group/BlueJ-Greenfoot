package org.bluej.extensions.submitter;

import bluej.extensions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Stack;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import org.bluej.extensions.submitter.properties.TreeData;

/**
 * FileHandler handles the selection of a set of files
 * according to a given String
 * 
 * @author Clive Miller
 * @version $Id: FileHandler.java 2306 2003-11-08 17:46:57Z iau $
 */
class FileHandler
{
    private final BlueJ bj;
    private final TreeData sp;
    private final Collection essentials, include, exclude;
    private File projectDir;

    public FileHandler (BlueJ bj, File pDir, TreeData sp) throws AbortOperationException
    {
        this.bj = bj;
        this.sp = sp;
        essentials = sp.getProps (".file.essential");
        include = sp.getProps (".file.include");
        exclude = sp.getProps (".file.exclude");
        projectDir = pDir;
    }

    /**
     * Gets an array of the required files
     */
    public File[] getFiles() throws AbortOperationException
    {

        return (File[])wantedFiles().toArray (new File[0]);
    }

    /**
     * Strips a file's name of the project path
     * @param file the file to examine
     * @return a string of the file's canonical path with the
     * project path root removed
     * @throws AbortOperationException if the path of the given file
     * does not begin with the project path
     */
    public String getSubName (File file) throws AbortOperationException,
                                                  IOException
    {
        String path = file.getCanonicalPath();
        String parent = projectDir.getCanonicalPath();
        if (!path.startsWith (parent)) {
            throw new AbortOperationException (bj.getLabel("message.filenotinpath"));
        }
        path = path.substring (parent.length()+1);
        return path;
    }
        
    /**
     * Checks a collection of files for the occurance of one
     * by the given name.
     * @param files a collection of File objects. 
     * @return <code>true</code> if the collection contains any files whose name
     * equals the name parameter
     * @param name the name to search for
     * @throws ClassCastException If any item is
     * not a File object
    private boolean containsFile (Collection files, String name)
    {
        if (name == null) return true;
        Iterator it = files.iterator();
        while (it.hasNext())
        {
            if (name.equals (( (File)it.next() ).getName()))
                return true;
        }
        return false;
    }

    /**
     * Turns a collection into a string of the items separated by commas
     */
    private String commaList (Collection list)
    {
        String commaList = "";
        Iterator it = list.iterator();
        while (it.hasNext())
        {
            String item = (String)it.next();
            commaList += item;
            if (it.hasNext()) commaList += ", ";
        }
        return commaList;
    }

    /**
     * A regular expression preprocessor
     * @param item the string to examine, or (if search is a collection of File
     * objects) a pattern.
     * @param search a collection of patterns it should match.
     * These can be strings or File objects, in which case the
     * file name is matched, and item becomes the pattern to match
     * it against. If an object in the collection is neither a String
     * nor a File, it is ignored.
     * @return <code>true</code> if a match can be made
     */
    private static boolean match (String item, Collection search)
    {
        Iterator it = search.iterator();
        while (it.hasNext())
        {
            Object obj = it.next();
            if (obj instanceof String && match (item, (String) obj))
                return true;
            else if (obj instanceof File && match (( (File)obj ).getName(), item)) 
                return true;
        }
        return false;
    }
    
    /**
     * A regular expression reader
     * @param item the string to examine
     * @pattern the rules for it to match
     * @return <code>true</code> if item can match pattern, where
     * a * can replace 0 or more of any character, and 
     * a ? can replace exactly one of any character.
     */
    private static boolean match (String item, String pattern)
    {
        if (item == null || pattern == null) return (item == pattern);
        int i=0, p=0;
        Stack stars = new Stack();
        boolean star = false;
        boolean iend, pend;
        char ic, pc;
        while (!(iend = i >= item.length())
             | !(pend = p >= pattern.length() 
                      || pattern.charAt(p)==';'))
        {
            ic = iend ? 0 : item.charAt(i);
            pc = pend ? 0 : pattern.charAt(p);
            if (star && !pend && !iend)
            {
                if (pc == ic)
                {
                    star = false;
                    i++;
                    p++;
                }
                else
                {
                    i++;
                }
            }
            else if (pc == '?')
            {
                i++;
                p++;
            }
            else if (pc == '*')
            {
                p++;
                stars.push (new Integer (p));
                star = true;
            }
            else if (ic == pc)
            {
                i++;
                p++;
            }
            else
            {
                if (stars.empty() || pend || iend)
                {
                    while (++p < pattern.length() && pattern.charAt(p)!=';');
                    if (p >= pattern.length()) break;
                    p++;
                    i=0;
                    star = false;
                }
                else
                {
                    p = ( (Integer)stars.pop() ).intValue();
                    star = true;
                }
            }
        }
    return (iend && pend || pend && star);
    }
    
    /**
     * Builds a collection of files to submit, based on the rules
     * in essentials, include and exclude.
     */
    private Collection wantedFiles() throws AbortOperationException
    {
        Collection wantedFiles = new ArrayList();
        recurseDirectory (wantedFiles, projectDir);
        
        if (!include.isEmpty()) for (Iterator it = wantedFiles.iterator(); it.hasNext();)
        {
            String checkName = ((File)it.next()).getName();
            if (!match (checkName,essentials) && !match (checkName,include))
                it.remove();
        }
    
        if (!exclude.isEmpty()) for (Iterator it = wantedFiles.iterator(); it.hasNext();)
        {
            String checkName = ((File)it.next()).getName();
            if (match (checkName, exclude)) it.remove();
        }

        Collection unsatisfied = new ArrayList (essentials);
        
        for (Iterator it = unsatisfied.iterator(); it.hasNext();)
            if (match ((String)it.next(), wantedFiles))
                it.remove();
        if (!unsatisfied.isEmpty())
            throw new AbortOperationException (bj.getLabel ("message.filesmissing")+" "+commaList (unsatisfied));
        if (wantedFiles.isEmpty())
            throw new AbortOperationException (bj.getLabel ("message.nofilestosend"));
        return wantedFiles;
    }

    /**
     * Recursively list the directory structure
     *
     * @param bag The list of Files to be added to
     * @param dir The level on which to start
     */
    public static void recurseDirectory (Collection bag, File dir)
    {
        File[] list = dir.listFiles();
        for (int i=0; i<list.length; i++)
        {
            if (list[i].isDirectory())
                recurseDirectory (bag, list[i]);
            else
                bag.add (list[i]);
        }
    }

    /**
     * Checks if the given file is binary. 
     * @param file the file to examine
     * @return <code>true</code> if the file contains any characters below 32
     * (except 9, 10 or 13) or 126.
     */
    public static boolean isBinary (File file) throws IOException
    {
        boolean isBinary = false;

        FileInputStream fis = new FileInputStream (file);
        byte[] buffer = new byte [1024];
        int read;
        while ((read=fis.read(buffer)) > 0)
        {
            for (int i=0; i<read; i++)
            {
                byte b = buffer[i];
                if (b > 126
                 || b < 32 && b != 10 && b != 13 && b != 9)
                {
                    isBinary = true;
                    break;
                }
            }
        }
        fis.close();
        return isBinary;
    }
}

