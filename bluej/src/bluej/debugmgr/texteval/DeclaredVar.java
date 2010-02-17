/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.texteval;

import bluej.debugger.gentype.JavaType;

/**
 * A class to represent a variable declared by a statement. This contains
 * the variable name and type, and whether or not it was initialized.
 */
public class DeclaredVar
{
    private boolean isVarInit = false;
    private JavaType declVarType;
    private String varName;
    private boolean isFinal = false;
    
    public DeclaredVar(boolean isVarInit, boolean isFinal, JavaType varType, String varName)
    {
        this.isVarInit = isVarInit;
        this.declVarType = varType;
        this.varName = varName;
        this.isFinal = isFinal;
    }
    
    /**
     * Check whether the variable declaration included an initialization.
     */
    public boolean isInitialized()
    {
        return isVarInit;
    }
    
    /**
     * Get the type of variable which was declared by the recently parsed
     * statement. 
     */
    public JavaType getDeclaredType()
    {
        return declVarType;
    }
    
    /**
     * Get the name of the declared variable.
     */
    public String getName()
    {
        return varName;
    }
    
    /**
     * Check whether the variable was declared "final".
     */
    public boolean isFinal()
    {
        return isFinal;
    }
}
