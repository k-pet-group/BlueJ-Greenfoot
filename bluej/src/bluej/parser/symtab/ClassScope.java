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
package bluej.parser.symtab;

import java.util.Iterator;
import java.util.List;

/**
 * A scope for a type. This can save method comments in a ClassInfo structure.
 * 
 * @author Davin McCall
 * @version $Id$
 */
public class ClassScope extends Scope
{
    private ClassInfo info;
    
    public ClassScope(ClassInfo info, Scope parent)
    {
        super(parent);
        this.info = info;
    }
    
    public void addMethod(String name, String tpars, String retType, List paramTypes, List paramNames, String comment)
    {
        String target = "";
        String paramNamesString = null;
        
        // type parameters
        if (tpars != null)
            target = tpars + " ";
        
        if (retType != null)
            target += retType + " ";
        
        target += name + "(";
        if (paramTypes != null) {
            Iterator i = paramTypes.iterator();
            while (i.hasNext()) {
                target += i.next();
                if (i.hasNext()) {
                    target += ", ";
                }
            }
        }
        target += ")";
        
        // parameter names
        if (paramNames != null && ! paramNames.isEmpty()) {
            Iterator i = paramNames.iterator();
            paramNamesString = i.next().toString();
            while (i.hasNext()) {
                paramNamesString += " " + i.next();
            }
        }
        
        info.addComment(target, comment, paramNamesString);
    }
}
