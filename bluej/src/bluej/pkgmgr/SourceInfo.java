/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
 * @version $Id: SourceInfo.java 6164 2009-02-19 18:11:32Z polle $
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
