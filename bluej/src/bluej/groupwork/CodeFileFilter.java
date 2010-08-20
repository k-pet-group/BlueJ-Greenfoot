/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import bluej.pkgmgr.BlueJPackageFile;
import bluej.utility.Debug;

/**
 * A FilenameFilter that filters out files based on a list of patterns. It also
 * filters out a standard set of file types (such as bluej.pkh files, ctxt files).
 *
 * @author fisker
 */
public class CodeFileFilter implements FileFilter, FilenameFilter
{
    private boolean includePkgFiles;
    private List<Pattern> patterns = null;
    private FileFilter parentFilter = null;

    /**
     * Construct a filter.
     * @param ignore  List of file patterns to ignore
     * @param includePkgFiles if true, pkg files are accepted
     * @param 
     */
    public CodeFileFilter(List<String> ignore, boolean includePkgFiles, FileFilter parent)
    {
        this.includePkgFiles = includePkgFiles;
        patterns = makePatterns(ignore);
        parentFilter = parent;
    }

    private List<Pattern> makePatterns(List<String> ignore)
    {
        List<Pattern> patterns = new LinkedList<Pattern>();
        for (Iterator<String> i = ignore.iterator(); i.hasNext();) {
            String patternString = (String) i.next();
            try{
                Pattern p = Pattern.compile(patternString);
                patterns.add(p);
            } catch (PatternSyntaxException pse){
                Debug.message("Couldn't parse ignore pattern: " + patternString);
            }
        }
        return patterns;
    }

    private boolean matchesPatterns(String input)
    {
        for (Iterator<Pattern> i = patterns.iterator(); i.hasNext();) {
            Pattern pattern = i.next();
            Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines which files should be included
     * @param dir the directory in which the file was found.
     * @param name the name of the file.
     */
    public boolean accept(File dir, String name)
    {
        boolean result = true;

        if(name.equals("doc") || dir.getName().equals("doc")){
            result = false;
        }
        if (name.equals("CVS") || dir.getName().equals("CVS")){
            result = false;
        }
        if (name.equals("CVSROOT") || dir.getName().equalsIgnoreCase("CVSROOT")){
            result = false;
        }

        /* when a package is first created. pkg files should be
         * added and committed. If we don't, BlueJ can't know which folders
         * are packages
         */ 
        if (!includePkgFiles && BlueJPackageFile.isPackageFileName(name)){
            result = false;
        }
        // the old bluej.pkg backup file
        if (name.equals("bluej.pkh")){
            result = false;
        }	
        if (name.equals("team.defs")){
            result = false;
        }
        if (name.equals(".DS_Store")){
            result = false;
        }
        if (getFileType(name).equals("ctxt")){
            result = false;
        }
        if (name.charAt(name.length() -1) == '~'){
            result = false;
        }
        if (name.charAt(name.length() -1) == '#'){
            result = false;
        }
        if (name.endsWith("#backup")){
            result = false;
        }
        if (name.startsWith(".#")){
            result = false;
        }
        if (matchesPatterns(name)){
            result = false;
        }

        if (result && parentFilter != null) {
            result = parentFilter.accept(new File(dir, name));
        }

        return result;
    }

    public boolean accept(File pathname)
    {
        File parent = pathname.getParentFile();
        return accept(parent, pathname.getName());
    }

    /**
     * Get the type of a file
     * @param filename the name of the file
     * @return a string with the type of the file.
     */
    private String getFileType(String filename)
    {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > -1 && lastDotIndex < filename.length()){
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}
