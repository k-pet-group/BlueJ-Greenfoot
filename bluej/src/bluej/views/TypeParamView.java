/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2020  Michael Kolling and John Rosenberg
 
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
package bluej.views;

import bluej.debugger.gentype.GenTypeDeclTpar;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Represents a formal type parameter for a generic class
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: TypeParamView.java 6215 2009-03-30 13:28:25Z polle $
 */
public class TypeParamView
{
    protected GenTypeDeclTpar paramType;
    protected View view;

    /**
     * Constructor.
     * 
     * @param view The view of the generic class to which this type parameter belongs
     * @param paramType The type parameter
     */
    protected TypeParamView(View view, GenTypeDeclTpar paramType) {
        if (view == null) {
            throw new NullPointerException();
        }
        if (paramType == null) {
            throw new NullPointerException();
        }
        this.view = view;
        this.paramType = paramType;
    }

    /**
     * Returns the name of this formal type parameter as a String
     */
    public String getName() {
        return paramType.getTparName();
    }

    /**
     * @return the View of the class or interface that declares this member.
     */
    public View getDeclaringView() {
        return view;
    }

    /**
     * Returns a string describing this type parameter. This includes name and bound as written in Java. <br>
     * Eaxample: T extends Integer
     */
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public String toString() {
        return paramType.toString(true);
    }
    
    public GenTypeDeclTpar getParamType() {
        return paramType;
    }
}