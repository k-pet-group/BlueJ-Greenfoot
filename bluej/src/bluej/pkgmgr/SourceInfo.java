package bluej.pkgmgr;

import bluej.utility.Debug;

import java.util.Properties;

/** 
 ** A container holding information about a class's source file. The 
 ** information is collected mainly by the class parser, and used for 
 ** automatic editing of the source.
 **
 ** @author Michael Kolling
 **
 ** @version
 **/

public final class SourceInfo
{
    private boolean valid;

    public SourceInfo()
    {
	valid = true;
    }

    public void setValid(boolean valid)
    {
	this.valid = valid;
    }

    public boolean isValid()
    {
	return valid;
    }

    public void save(Properties props, String prefix)
    {
	props.put(prefix + ".valid", ""+valid);
    }
	
    public void load(Properties props, String prefix)
    {
	String validStr = props.getProperty(prefix + ".valid", "true");
	valid = Boolean.valueOf(validStr).booleanValue();
    }

}
