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
 ** @version $Id: ViewFilter.java 6215 2009-03-30 13:28:25Z polle $
 ** @author Michael Cahill
 **
 ** A filter for views - allows only certain parts of a view to be seen.
 ** Used to implement BlueJ's "public", "package" and "inherited" views.
 **/
public final class ViewFilter
{
	public static final int PUBLIC = Modifier.PUBLIC;
    public static final int PROTECTED = PUBLIC | Modifier.PROTECTED;
	public static final int PACKAGE = PROTECTED | 0x10000;
	public static final int PRIVATE = PACKAGE | Modifier.PRIVATE;
	
	public static final int STATIC = Modifier.STATIC;
	public static final int INSTANCE = 0x20000;
	
	public static final int ABSTRACT = Modifier.ABSTRACT;
	public static final int CONCRETE = 0x40000;
	
	static final int allbits = PRIVATE | STATIC | INSTANCE | ABSTRACT | CONCRETE;
	
	int modifiers;
	
	public ViewFilter(int modifiers)
	{
		if(((modifiers & STATIC) == 0) && ((modifiers & INSTANCE) == 0))
			modifiers |= STATIC | INSTANCE;
			
		if(((modifiers & ABSTRACT) == 0) && ((modifiers & CONCRETE) == 0))
			modifiers |= ABSTRACT | CONCRETE;
			
		this.modifiers = modifiers;
	}
	
	public boolean accept(int othermods)
	{
		if((othermods & 7) == 0)
			othermods |= 0x10000;
		if((othermods & STATIC ) == 0)
			othermods |= INSTANCE;
			
		return ((allbits & othermods & ~modifiers) == 0);
	}
	
	public boolean accept(Member member)
	{
		return accept(member.getModifiers());
	}
	
	public boolean accept(MemberView member)
	{
		return accept(member.getModifiers());
	}
}
