package bluej.browser;

import javax.swing.filechooser.*;
import java.io.*;

/**
 * A simple FileFilter subclass to accept on valid library files (i.e., ZIP or JAR extension)
 * Used by the AddLibraryDialog to only allow selection of valid library archive files.
 * 
 * @author $Author: mik $
 * @version 0.1
 */
public class LibraryFileFilter extends FileFilter {
	/**
	 * Check if it is a valid library archive file.
	 * 
	 * @param f the file to be check.
	 * @return true if the file was accepted.
	 */
	public boolean accept(File f) {
		return (f.isDirectory() || 
						f.getName().toLowerCase().endsWith(".jar") ||
						f.getName().toLowerCase().endsWith(".zip"));
	}

	/**
	 * Return a description of the files accepted by this filter.  Used
	 * in the "file types" drop down list in file chooser dialogs.
	 * 
	 * @return a description of the files accepted by this filter.
	 */
	public String getDescription() {
		return "Library files (*.jar;*.zip)";
	}
}
