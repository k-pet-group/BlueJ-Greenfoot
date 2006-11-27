package bluej.groupwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import bluej.pkgmgr.Package;

/**
 * A FilenameFilter that filters out files that reside in a directory named
 * 'CVS' or 'CVSROOT'
 * It also filters out files  
 *
 */
public class CodeFileFilter implements FilenameFilter {

	boolean includePkgFiles;
	List patterns = null;
	
	/**
	 * Construct a filter.
	 * @param includePkgFiles if true, pkg files are accepted
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public CodeFileFilter(List ignore, boolean includePkgFiles)
    {
		this.includePkgFiles = includePkgFiles;
		patterns = makePatterns(ignore);
	}
	
	private List makePatterns(List ignore)
    {
		List patterns = new LinkedList();
		for (Iterator i = ignore.iterator(); i.hasNext();) {
			String patternString = (String) i.next();
			try{
				Pattern p = Pattern.compile(patternString);
				patterns.add(p);
			} catch (PatternSyntaxException pse){
				System.err.println("couldn't parse: " + patternString);
			}
		}
		return patterns;
	}
	
	private boolean matchesPatterns(String input){
		for (Iterator i = patterns.iterator(); i.hasNext();) {
			Pattern pattern = (Pattern) i.next();
			Matcher matcher = pattern.matcher(input);
			if (matcher.matches()){
				return true;
			}
		}
		return false;
	}
	/**
	 * Determins which files should be included
	 * @param dir the directory in which the file was found.
	 * @param name the name of the file.
	 */
	public boolean accept(File dir, String name) {
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
		if (!includePkgFiles && name.equals(Package.pkgfileName)){
			result = false;
		}
        // the old bluej.pkg backup file
		if (name.equals("bluej.pkh")){
			result = false;
		}	
		if (name.equals("team.defs")){
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
		if (result) {
			//System.out.println("Repository:509 accepted: " + name + " in " + dir.getAbsolutePath());
		}else{
			//System.out.println("Repository:509 rejected: " + name + " in " + dir.getAbsolutePath());
		}
		return result;
	}
	
	/**
	 * Get the type of a file
	 * @param filename the name of the file
	 * @return a string with the type of the file.
	 */
	private String getFileType(String filename) {
		int lastDotIndex = filename.lastIndexOf('.');
		if (lastDotIndex > -1 && lastDotIndex < filename.length()){
			return filename.substring(lastDotIndex + 1);
		}
		return "";
	}
}
