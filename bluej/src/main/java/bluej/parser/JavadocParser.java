/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014  Michael Kolling and John Rosenberg 
 
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
package bluej.parser;

import java.io.Reader;

import bluej.parser.entity.EntityResolver;
import bluej.parser.symtab.ClassInfo;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This class in a copy of InfoParser. However, it is tweaked for performance
 * (values are not parsed).
 *
 * @author Fabio Hedayioglu
 */
public class JavadocParser extends InfoParser
{
    /**
     * Construct an InfoParser which reads Java source using the given reader,
     * and resolves reference via the given resolver.
     */
    public JavadocParser(Reader r, EntityResolver resolver)
    {
        super(r, resolver);
    }

    /**
     * Attempt to parse the specified source file, and resolve references via
     * the specified resolver. The source should be assumed to reside in the
     * specified package. Returns null if the source could not be parsed.
     */
    @OnThread(Tag.FXPlatform)
    public static ClassInfo parse(Reader r, EntityResolver resolver, String targetPkg)
    {
        JavadocParser javadocParser = null;
        javadocParser = new JavadocParser(r, resolver);
        javadocParser.targetPkg = targetPkg;
        javadocParser.parseCU();

        if (javadocParser.info != null) {
            javadocParser.info.setParseError(javadocParser.hadError);
            javadocParser.resolveMethodTypes();
            return javadocParser.info;
        }
        return null;
    }
}
