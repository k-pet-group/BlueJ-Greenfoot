/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.debugger.jdi;

import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;

/*
 * Jdi Utilities, java 1.5 version.
 * 
 * @author Davin McCall
 * @version $Id: JdiUtils15.java 6215 2009-03-30 13:28:25Z polle $
 */
public class JdiUtils15 extends JdiUtils
{
    public boolean hasGenericSig(ObjectReference obj)
    {
        return obj.referenceType().genericSignature() != null;
    }

    public String genericSignature(Field f)
    {
        return f.genericSignature();
    }

    public String genericSignature(ReferenceType rt)
    {
        return rt.genericSignature();
    }

    public String genericSignature(LocalVariable lv)
    {
        return lv.genericSignature();
    }
    
    public boolean isEnum(ClassType ct)
    {
        return ct.isEnum();
    }
}
