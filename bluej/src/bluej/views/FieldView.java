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
package bluej.views;

import java.lang.reflect.*;

/**
 ** @version $Id: FieldView.java 6215 2009-03-30 13:28:25Z polle $
 ** @author Michael Cahill
 **
 ** A representation of a Java field in BlueJ
 **/
public final class FieldView extends MemberView
{
    protected Field field;
    protected View type;
	
    /**
     ** Constructor.
     **/
    public FieldView(View view, Field field)
    {
	super(view);
		
	this.field = field;
    }

    /**
     * Returns the Field being manipulated by this View.
     * 
     * @return the Field that this view represent.
     */
    public Field getField ()
      {
      return field;
      }
      
    /**
     ** Returns the name of this method as a String
     **/
    public String getName()
    {
	return field.getName();
    }
	
    /**
     ** Returns a Class object that represents the type of the field represented
     **  by this object.
     **/
    public View getType()
    {
	if(type == null)
	    type = View.getView(field.getType());
		
	return type;
    }

    /**
     ** Returns a string describing this Method.
     **/
    public String toString()
    {
	return field.toString();
    }
	
    public int getModifiers()
    {
	return field.getModifiers();
    }
	
    public String getShortDesc()
    {
	StringBuffer sb = new StringBuffer();
	sb.append(View.getTypeName(field.getType()));
	sb.append(" ");
	sb.append(field.getName());
	return sb.toString();
    }

    public String getLongDesc()
    {
        return getShortDesc();        
    }
    
    /**
     ** Returns a string describing this Field in a human-readable format
     **/
    public String getSignature()
    {
	StringBuffer sb = new StringBuffer();
	//		sb.append(View.getTypeName(field.getType()));
	//		sb.append(" ");
	sb.append(field.getName());
	return sb.toString();
    }
}
