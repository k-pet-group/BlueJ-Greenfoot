/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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

import java.rmi.RemoteException;
import java.util.WeakHashMap;

import bluej.extensions.BClass;
import bluej.extensions.BConstructor;
import bluej.extensions.BField;
import bluej.extensions.BObject;
import bluej.extensions.BPackage;
import bluej.extensions.BProject;

/**
 * This class has a pool of RMI-wrappers that have been created. This is used to
 * avoid getting more than one RMI-wrapper for each object. Doing this, makes it
 * possible to use == to test if two objects are the same.
 * 
 * TODO remember to "release" objects when they are no longer needed
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: WrapperPool.java 6216 2009-03-30 13:41:07Z polle $
 */
public class WrapperPool
{

    static private WrapperPool instance;

    private WeakHashMap pool = new WeakHashMap();

    private WrapperPool()
    {

    }

    public static WrapperPool instance()
    {
        if (instance == null) {
            instance = new WrapperPool();
        }
        return instance;
    }

    public RProjectImpl getWrapper(BProject wrapped)
        throws RemoteException
    {
        RProjectImpl wrapper = (RProjectImpl) pool.get(wrapped);
        if (wrapper == null) {
            wrapper = new RProjectImpl(wrapped);
            pool.put(wrapped, wrapper);
        }
        return wrapper;
    }

    public RPackage getWrapper(BPackage wrapped)
        throws RemoteException
    {
        if (wrapped == null) {
            return null;
        }
        RPackage wrapper = (RPackage) pool.get(wrapped);
        if (wrapper == null) {
            wrapper = new RPackageImpl(wrapped);
            pool.put(wrapped, wrapper);
        }
        return wrapper;
    }

    public RClass getWrapper(BClass wrapped)
        throws RemoteException
    {
        if (wrapped == null) {
            return null;
        }
        RClass wrapper = (RClass) pool.get(wrapped);
        if (wrapper == null) {
            wrapper = new RClassImpl(wrapped);
            pool.put(wrapped, wrapper);
        }
        return wrapper;
    }

    /**
     * @param constructor
     * @return
     */
    public RConstructor getWrapper(BConstructor wrapped)
        throws RemoteException
    {
        if (wrapped == null) {
            return null;
        }
        RConstructor wrapper = (RConstructor) pool.get(wrapped);
        if (wrapper == null) {
            wrapper = new RConstructorImpl(wrapped);
            pool.put(wrapped, wrapper);
        }
        return wrapper;
    }

    /**
     * @param object
     * @return
     */
    public RObject getWrapper(BObject wrapped)
        throws RemoteException
    {
        if (wrapped == null) {
            return null;
        }
        RObject wrapper = (RObject) pool.get(wrapped);
        if (wrapper == null) {
            wrapper = new RObjectImpl(wrapped);
            pool.put(wrapped, wrapper);
        }
        return wrapper;
    }

    /**
     * @param wrapped
     * @return
     */
    public RField getWrapper(BField wrapped)
        throws RemoteException
    {
        if (wrapped == null) {
            return null;
        }
        RField wrapper = (RField) pool.get(wrapped);
        if (wrapper == null) {
            wrapper = new RFieldImpl(wrapped);
            pool.put(wrapped, wrapper);
        }
        return wrapper;
    }

}