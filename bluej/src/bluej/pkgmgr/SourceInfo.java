package bluej.pkgmgr;

import java.io.File;
import java.util.List;

import bluej.parser.ClassParser;
import bluej.parser.symtab.ClassInfo;

/**
 * A container holding information about a class's source file. The
 * information is collected mainly by the class parser, and used for
 * automatic editing of the source.
 *
 * @author  Michael Kolling
 * @version $Id: SourceInfo.java 3588 2005-09-26 00:18:07Z davmac $
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

    public ClassInfo getInfo(File sourceFile, Package pkg)
    {
        if(info == null)
        {
            try {
                List classNames = pkg.getAllClassnames();
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

    /**
     * Similar to getInfo, but do not parse if info is not available.
     * Instead, return null, if we got no info.
     */
    public ClassInfo getInfoIfAvailable()
    {
        return info;
    }
}
