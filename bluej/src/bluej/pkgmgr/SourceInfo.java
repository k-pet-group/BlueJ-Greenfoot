package bluej.pkgmgr;

import bluej.utility.Debug;
import bluej.parser.ClassParser;
import bluej.parser.symtab.ClassInfo;

import java.util.*;

/**
 * A container holding information about a class's source file. The
 * information is collected mainly by the class parser, and used for
 * automatic editing of the source.
 *
 * @author  Michael Kolling
 * @version $Id: SourceInfo.java 330 2000-01-02 13:25:14Z ajp $
 */
public final class SourceInfo
{
    private boolean valid;
    private ClassInfo info;

    public SourceInfo()
    {
        valid = true;
        info = null;
    }

    public boolean isValid()
    {
        return valid;
    }

    public void setSourceModified()
    {
        info = null;
    }

    public ClassInfo getInfo(String sourceFile, Vector classNames)
    {
        if(info == null)
        {
            try {
                info = ClassParser.parse(sourceFile, classNames);

                valid = true;
            }
            catch(Exception e) {
                // uncomment the following line to track parsing problems
                // however it must be disabled in production version or
                // else syntax errors in users programs will cause lots
                // of debug messages
                //e.printStackTrace();

                // exception during parsing
                valid = false;
                info = null;
            }
        }

        return info;
    }

    public void save(Properties props, String prefix)
    {
        // XXX does it make any sense to save this??
        props.put(prefix + ".valid", ""+valid);
    }

    public void load(Properties props, String prefix)
    {
        String validStr = props.getProperty(prefix + ".valid", "true");
        valid = Boolean.valueOf(validStr).booleanValue();
    }

}
