package bluej.utility;

import java.io.FilenameFilter;
import java.io.File;

public class PackageFileFilter implements FilenameFilter 
{
    private String fileMask = "";
    private boolean include = true;
	
    public PackageFileFilter(String fileMask, boolean include) 
    {
	this.fileMask = fileMask;
	this.include = include;
    }
	
    public boolean accept(File dir, String name) 
    {
	File theFile = new File(dir + File.separator + name);
	if (theFile.isFile())
	    return (include == true ? name.endsWith(this.fileMask) : !name.endsWith(this.fileMask));
	else if (theFile.isDirectory())
	    return true;
	else
	    return false;
    }
}
