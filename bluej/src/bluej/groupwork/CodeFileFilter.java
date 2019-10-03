/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2017,2018,2019  Michael Kolling and John Rosenberg
 
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
    private boolean includeDirectories;
    private List<Pattern> patterns = null;
    private FileFilter parentFilter = null;
    private File projectDir;

    /**
     * Construct a filter, which has a flag to whether include Package Files.
     *
     * @param ignore          List of file patterns to ignore.
     * @param projectDir      The directory of the project.
     * @param parent          The filter which will be applied on the parent directory
     */
    public CodeFileFilter(List<String> ignore, File projectDir, FileFilter parent)
    {
        this.projectDir = projectDir;
        patterns = makePatterns(ignore);
        parentFilter = parent;
    }

    /**
     * Construct a filter, which has two flags to whether to include Directories and Package Files.
     *
     * @param ignore              List of file patterns to ignore.
     * @param includeDirectories  If true, pkg files are accepted.
     * @param projectDir          The directory of the project.
     * @param parent              The filter which will be applied on the parent directory
     */
    public CodeFileFilter(List<String> ignore, boolean includeDirectories, File projectDir, FileFilter parent)
    {
        this(ignore, projectDir, parent);
        this.includeDirectories = includeDirectories;
    }

    private List<Pattern> makePatterns(List<String> ignore)
    {
        List<Pattern> patterns = new LinkedList<>();
        for (String patternString: ignore) {
            try {
                patterns.add(Pattern.compile(patternString));
            }
            catch (PatternSyntaxException pse) {
                Debug.message("Couldn't parse ignore pattern: " + patternString);
            }
        }
        return patterns;
    }

    private boolean matchesPatterns(String input)
    {
        for (Pattern pattern: patterns) {
            Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines which files should be included
     *
     * @param dir the directory in which the file was found.
     * @param name the name of the file.
     */
    @Override
    public boolean accept(File dir, String name)
    {
        File file = new File(dir, name);
        if (!includeDirectories && file.isDirectory()) {
            return false;
        }

        // Exclude everything inside the "doc" top-level directory:
        File tdir = file;
        String tname = name;
        while (! tdir.equals(projectDir)) {
            tname = tdir.getName();
            tdir = tdir.getParentFile();
            if (tdir == null) {
                return false;
            }
        }
        if (tname.equals("doc")) {
            return false;
        }
        
        if (name.equals("CVS") || dir.getName().equals("CVS")) {
            return false;
        }
        if (name.equals("CVSROOT") || dir.getName().equalsIgnoreCase("CVSROOT")) {
            return false;
        }
        
        // the old bluej.pkg backup file
        if (name.equals("bluej.pkh")) {
            return false;
        }
        if (name.equals("team.defs")) {
            return false;
        }
        if (name.equals(".DS_Store")) {
            return false;
        }
        if (getFileType(name).equals("ctxt")) {
            return false;
        }
        if (name.charAt(name.length() -1) == '~') {
            return false;
        }
        if (name.charAt(name.length() -1) == '#') {
            return false;
        }
        if (name.endsWith("#backup")) {
            return false;
        }
        if (name.startsWith(".#")) {
            return false;
        }
        if (matchesPatterns(name)) {
            return false;
        }

        if (parentFilter != null) {
            return parentFilter.accept(new File(dir, name));
        }

        return true;
    }

    @Override
    public boolean accept(File pathname)
    {
        return accept(pathname.getParentFile(), pathname.getName());
    }

    /**
     * Get the type of a file
     * @param filename the name of the file
     * @return a string with the type of the file.
     */
    private String getFileType(String filename)
    {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > -1 && lastDotIndex < filename.length()) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}
