/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import java.util.EventObject;

import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.testmgr.record.InvokerRecord;
import bluej.views.CallableView;

/**
 * The event which occurs while editing a package
 *
 * @author  Andrew Patterson
 */
public class PackageEditorEvent extends EventObject
{
    public final static int TARGET_CALLABLE = 1;
    public final static int TARGET_REMOVE = 2;
    public final static int TARGET_OPEN = 3;
    public final static int TARGET_RUN = 4;
    public final static int TARGET_BENCHTOFIXTURE = 5; // only for unit tests
    public final static int TARGET_FIXTURETOBENCH = 6; // only for unit tests
    public final static int TARGET_MAKETESTCASE = 7;    // only for unit tests

    public final static int OBJECT_PUTONBENCH = 8;

    protected int id;
    protected CallableView cv;
    protected DebuggerObject obj;
    protected InvokerRecord ir;
    protected GenTypeClass iType;
    protected String name;

    public PackageEditorEvent(Object source, int id)
    {
        super(source);
        this.id = id;
    }

    public PackageEditorEvent(Object source, int id, String packageName)
    {
        super(source);

        this.id = id;
        this.name = packageName;
    }

    public PackageEditorEvent(Object source, int id, CallableView cv)
    {
        super(source);

        if (id != TARGET_CALLABLE)
            throw new IllegalArgumentException();

        this.id = id;
        this.cv = cv;
    }

    /**
     * Construct an event for a "put object on bench" request (OBJECT_PUTONBENCH)
     * 
     * @param source  The source of the event
     * @param id      The event id (OBJECT_PUTONBENCH)
     * @param obj     The object to put on the bench
     * @param iType   The publicly-accessible type of the object
     *       The iType parameter is used to provide an acting type for the object if the
     *       actual type is inaccessible (is private to another package or class).
     * @param ir      The record for the invocation used to obtain the object
     * 
     */
    public PackageEditorEvent(Object source, int id, DebuggerObject obj, GenTypeClass iType, InvokerRecord ir)
    {
        super(source);

        if (id != OBJECT_PUTONBENCH)
            throw new IllegalArgumentException();

        this.id = id;
        this.obj = obj;
        this.iType = iType;
	this.ir = ir;
    }

    public int getID()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public CallableView getCallable()
    {
        return cv;
    }

    public DebuggerObject getDebuggerObject()
    {
        return obj;
    }
    
    public GenTypeClass getIType()
    {
        return iType;
    }
    
    public InvokerRecord getInvokerRecord()
    {
    	return ir;	
    }
}
