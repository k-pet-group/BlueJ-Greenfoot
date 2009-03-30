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

import greenfoot.util.GreenfootUtil;

import java.rmi.RemoteException;

import bluej.extensions.BField;
import bluej.extensions.BObject;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RFieldImpl.java 6216 2009-03-30 13:41:07Z polle $
 */
public class RFieldImpl extends java.rmi.server.UnicastRemoteObject
    implements RField
{
    private BField bField;

    /**
     * @param class1
     */
    public RFieldImpl(BField bField)
        throws RemoteException
    {
        this.bField = bField;
        if (bField == null) {
            throw new NullPointerException("Argument can't be null");
        }
    }

    /**
     * @throws RemoteException
     */
    protected RFieldImpl()
        throws RemoteException
    {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @return
     */
    public int getModifiers()
    {
        return bField.getModifiers();
    }

    /**
     * @return
     */
    public String getName()
    {
        return bField.getName();
    }

    /**
     * @return
     */
    public Class getType()
    {
        return bField.getType();
    }

    /**
     * @param onThis
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RObject getValue(RObject onThis)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        try {
            Object fieldValue = bField.getValue(null);

            if (fieldValue instanceof BObject) {

                BObject bFieldValue = (BObject) fieldValue;

                String newInstanceName = "noName";
                
                try {
                    String className = bFieldValue.getBClass().getName();
                    className = GreenfootUtil.extractClassName(className);
                    newInstanceName = className.substring(0, 1).toLowerCase() + className.substring(1);
                }
                catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                    
                //must add to object bench in order to get the menu later
                bFieldValue.addToBench(newInstanceName);

                RObject wrapper = WrapperPool.instance().getWrapper(bFieldValue);
                return wrapper;
            }
            else {
                return null;
            }
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //Object obj =bField.getValue(onThis.getBObject());
        // onThis.getField(RField)
        return null;
    }

    /**
     * @param fieldName
     * @return
     */
    public boolean matches(String fieldName)
    {
        return bField.matches(fieldName);
    }
}