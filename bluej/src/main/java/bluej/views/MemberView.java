/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011  Michael Kolling and John Rosenberg 
 
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

import threadchecker.OnThread;
import threadchecker.Tag;

import java.lang.reflect.Modifier;

/**
 * A representation of a Java class member in BlueJ.
 *
 * @author  Michael Cahill
 */
public abstract class MemberView
{
    private final View view;
    private Comment comment;

    protected MemberView(View view)
    {
        if (view == null)
            throw new NullPointerException();

        this.view = view;
    }

    /**
     * @return the View of the class or interface that declares this member.
     */
    public View getDeclaringView()
    {
        return view;
    }

    /**
     * @return the name of the class or interface that declares this member.
     */
    public String getClassName()
    {
        return view.getQualifiedName();
    }

    /**
     * Returns the Java language modifiers for the member or
     * constructor represented by this Member, as an integer.  The
     * Modifier class should be used to decode the modifiers in
     * the integer.
     * @see Modifier
     */
    @OnThread(Tag.Any)
    public abstract int getModifiers();

    /**
     * Returns a string describing this member in a human-readable format
     */
    public abstract String getSignature();

    /**
     * Sets the (javadoc) comment for this Member
     */
    void setComment(Comment comment)
    {
        this.comment = comment;
    }

    /**
     * Returns the (javadoc) comment for this Member
     */
    public Comment getComment()
    {
        if (view != null) {
            view.loadComments();
        }

        return comment;
    }

    /**
     * Get a short String describing this member
     */
    public abstract String getShortDesc();

    /**
     * Get a longer String describing this member
     */
    public abstract String getLongDesc();

    /**
     * @return a boolean indicating whether this member is static
     */
    public boolean isStatic()
    {
        return Modifier.isStatic(getModifiers());
    }

    public String toString()
    {
        return view.toString();
    }
}
