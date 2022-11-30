/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011  Michael Kolling and John Rosenberg 
 
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

import java.util.ArrayList;
import java.util.List;

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerField;
import bluej.utility.Debug;

import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.ReferenceType;

/**
 * Represents an class running on the user (remote) machine.
 *
 * @author     Michael Kolling
 * @created    December 26, 2000
 */
public class JdiClass extends DebuggerClass
{
    ReferenceType remoteClass;  // the remote class represented
    List<Field> staticFields;

    // -- instance methods --

    /**
     *  Create a remote class object.
     *
     *@param  obj  the remote debugger object (Jdi code) this encapsulates.
     */
    public JdiClass(ReferenceType remoteClass)
    {
        this.remoteClass = remoteClass;
        getRemoteFields();
    }


    /*
     * Return the name of this class (fully qualified).
     *
     * @return    The class name
     */
    @Override
    public String getName()
    {
        return remoteClass.name();
    }

    @Override
    public List<DebuggerField> getStaticFields()
    {
        List<Field> visibleFields = remoteClass.visibleFields();
        List<DebuggerField> rlist = new ArrayList<DebuggerField>(staticFields.size());
        for (Field field : staticFields) {
            rlist.add(new JdiField(field, null, ! visibleFields.contains(field)));
        }
        return rlist;
    }

    /*
     * Returns true if this represents a Java interface
     */
    @Override
    public boolean isInterface()
    {
        return remoteClass instanceof InterfaceType;
    }

    /*
     * Returns true if this represents an enum
     */
    @Override
    public boolean isEnum()
    {
        if (remoteClass instanceof ClassType) {
            return JdiUtils.getJdiUtils().isEnum((ClassType) remoteClass);
        }
        return false;
    }

    /**
     *  Get the list of fields for this object.
     */
    private void getRemoteFields()
    {
        staticFields = new ArrayList<Field>();

        if (remoteClass != null) {
            List<Field> allFields = remoteClass.allFields();
            for (int i = 0; i < allFields.size(); i++) {
                Field field = (Field) allFields.get(i);
                if (field.isStatic())
                    staticFields.add(field);
            }
        }
        else {
            Debug.reportError("cannot get fields for remote class");
        }
    }
}
