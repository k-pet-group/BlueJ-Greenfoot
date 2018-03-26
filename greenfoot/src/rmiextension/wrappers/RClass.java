/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2014,2015,2016,2017  Poul Henriksen and Michael Kolling
 
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
package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;
import java.util.List;

import bluej.extensions.BField;
import bluej.extensions.BMethod;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.SourceType;
import bluej.extensions.editor.Editor;

/**
 * Remote BlueJ class interface.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public interface RClass
    extends java.rmi.Remote
{
    /**
     * Gets the superclass of this class (if it has one).
     * 
     * @param inRemoteCallback  whether this method is being called from a method which was itself invoked
     *                          by a callback from this virtual machine, which blocks the dispatch thread.
     */
    RClass getSuperclass(boolean inRemoteCallback)
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException;

    boolean isCompiled(boolean inRemoteCallback)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    String getQualifiedName()
        throws RemoteException;

    void remove()
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException;

    /**
     * Put the editor for this class in or out of read-only mode.
     * 
     * @see Editor#setReadOnly(boolean)
     */
    void setReadOnly(boolean b)
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException ;

    /**
     * Check whether this class has a source file.
     */
    SourceType getSourceType()
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException;

}
