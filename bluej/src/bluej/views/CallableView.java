/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2016,2019,2020  Michael Kolling and John Rosenberg
 
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

import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.views.FormattedPrintWriter.ColorScheme;
import bluej.views.FormattedPrintWriter.SizeScheme;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A "callable" is the generalisation of a Constructor and a Method. This class
 * contains aspects common to both of those.
 * 
 * @author Michael Kolling
 *  
 */
public abstract class CallableView extends MemberView
{
    /**
     * Constructor.
     */
    public CallableView(View view)
    {
        super(view);
    }

    /**
     * @returns a boolean indicating whether this method has parameters
     */
    public abstract boolean hasParameters();
    
    /**
     * @returns a boolean indicating whether this method uses var args
     */
    public abstract boolean isVarArgs();

    /**
     * Indicates whether the callable view has type parameters.
     */
    public abstract boolean isGeneric();

    /**
     * Indicates whether the callable view represents a constructor.
     */
    public abstract boolean isConstructor();

    /**
     * Check whether this method returns void
     */
    public abstract boolean isVoid();
    
    /**
     * Count of parameters
     * @returns the number of parameters
     */
    public int getParameterCount()
    {
        return getParameters().length;
    }

    /**
     * Get an array of Class objects representing parameter classes
     * @return  array of Class objects
     */
    public abstract Class<?>[] getParameters();
    
    /**
     * Get an array of GenType objects representing the parameter types of the
     * callable. For a varargs callable, the last parameter type will be an
     * array (and {@link #isVarArgs()} will return true).
     * 
     * @param raw  whether to return raw versions of the parameter types
     * @return  the parameter types
     */
    public abstract JavaType[] getParamTypes(boolean raw);

    /**
     * Get the type parameters for this callable as an array of GenTypeDeclTpar
     */
    public abstract GenTypeDeclTpar[] getTypeParams() throws ClassNotFoundException;
    
    /**
     * Gets an array of strings with the names of the parameters
     * @return
     */
    public String[] getParamNames()
    {
        Comment c = getComment();
        if( c == null )
            return null;
        return c.getParamNames();
    }
    
    /**
     * Gets an array of nicely formatted strings with the types of the parameters 
     */
    public abstract String[] getParamTypeStrings();
    
    /**
     * Print the method to a formatting print writer.
     */
    @OnThread(Tag.FXPlatform)
    public void print(FormattedPrintWriter out)
    {
        print(out, 0);
    }

    @OnThread(Tag.FXPlatform)
    public void print(FormattedPrintWriter out, int indents)
    {
        Comment comment = getComment();
        if(comment != null)
            comment.print(out, indents);

        out.setItalic(false);
        out.setBold(true);
        out.setColor(ColorScheme.DEFAULT);
        out.setSize(SizeScheme.DEFAULT);
        for(int i=0; i<indents; i++)
            out.indentLine();
        out.println(getLongDesc());
    }

}
