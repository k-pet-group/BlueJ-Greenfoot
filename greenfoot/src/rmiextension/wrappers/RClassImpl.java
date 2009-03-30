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

import java.io.File;
import java.rmi.RemoteException;

import javax.swing.SwingUtilities;

import bluej.extensions.BClass;
import bluej.extensions.BConstructor;
import bluej.extensions.BField;
import bluej.extensions.BMethod;
import bluej.extensions.BPackage;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.editor.Editor;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RClassImpl.java 6216 2009-03-30 13:41:07Z polle $
 */
public class RClassImpl extends java.rmi.server.UnicastRemoteObject
    implements RClass
{

    BClass bClass;

    /**
     * Package-private constructor. Use WrapperPool to instantiate.
     */
    RClassImpl(BClass bClass)
        throws RemoteException
    {
        this.bClass = bClass;
        if (bClass == null) {
            throw new NullPointerException("Argument can't be null");
        }
    }

    public RClassImpl()
        throws RemoteException
    {

    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
    {
        bClass.compile(waitCompileEnd);
    }

    public void edit()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        final Editor editor = bClass.getEditor();
        if (editor != null) {
            Thread t = new Thread() {
                public void run()
                {
                    editor.setVisible(true);
                }
            };
            SwingUtilities.invokeLater(t);
        }
    }

    /**
     * @param signature
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public RConstructor getConstructor(Class[] signature)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {

        BConstructor bConstructor = bClass.getConstructor(signature);

        RConstructor rConstructor = WrapperPool.instance().getWrapper(bConstructor);
        return rConstructor;
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public RConstructor[] getConstructors()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {

        BConstructor[] bConstructors = bClass.getConstructors();
        int length = bConstructors.length;
        RConstructor[] rConstructors = new RConstructor[length];
        for (int i = 0; i < length; i++) {
            rConstructors[i] = WrapperPool.instance().getWrapper(bConstructors[i]);
        }

        return rConstructors;
    }

    /**
     * @param methodName
     * @param params
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public BMethod getDeclaredMethod(String methodName, Class[] params)
        throws ProjectNotOpenException, ClassNotFoundException
    {
        return null;
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public BMethod[] getDeclaredMethods()
        throws ProjectNotOpenException, ClassNotFoundException
    {
        return bClass.getDeclaredMethods();

    }

    /**
     * @param fieldName
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public RField getField(String fieldName)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {

        BField wrapped = bClass.getField(fieldName);
        RField wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;

    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public BField[] getFields()
        throws ProjectNotOpenException, ClassNotFoundException
    {
        return bClass.getFields();
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RPackage getPackage()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {

        BPackage wrapped = bClass.getPackage();
        RPackage wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;
    }

    /**
     * Gets the superclass of this class if it is a part of the project.
     * 
     * @see #getSuperclassName()
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws ClassNotFoundException
     */
    public RClass getSuperclass()
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException
    {
        BClass wrapped = bClass.getSuperclass();
        RClass wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public boolean isCompiled()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        return bClass.isCompiled();
    }

    public String getToString() throws ProjectNotOpenException, ClassNotFoundException
    {
        return bClass.getName();
    }

    public String getQualifiedName()
        throws RemoteException, ProjectNotOpenException, ClassNotFoundException
    {
        return bClass.getName();
    }

  

    public File getJavaFile()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return bClass.getJavaFile();
    }

    public void remove() throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException
    {
        bClass.remove();
    }


    public void setReadOnly(boolean b) throws RemoteException, ProjectNotOpenException, PackageNotFoundException 
    {
        if(bClass != null && bClass.getEditor() != null) {
            bClass.getEditor().setReadOnly(b);
        }
    }

}