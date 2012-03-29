/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012  Michael Kolling and John Rosenberg 
 
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
package bluej.extensions;


import bluej.debugger.*;
import bluej.debugmgr.objectbench.*;
import bluej.pkgmgr.*;
import bluej.views.*;

/**
 * A wrapper for a constructor of a BlueJ class.
 * Behaviour is similar to reflection API.
 *
 *
 * @author Damiano Bolla, University of Kent at Canterbury, 2003,2004
 */
public class BConstructor
{
    // The problem of consistency here is quite subtle.....
    // I could try to get a kind of id for a ConstructorView and then try to get it back
    // when I need it, but really, the gain is almost nil.
    // What I will do is to have an Identifier with Project,Package,Class given and before doing
    // anything I will check with it. If everything is still there it should be OK.
    // In any case, it it goes wrong we will get an invoker exception !
    
    
    private Identifier parentId;
    private ConstructorView bluej_view;


    /**
     * Constructor.
     * It is duty of the caller to make shure that the parent is valid.
     *
     * @param  aParentId  Description of the Parameter
     * @param  i_view     Description of the Parameter
     */
    BConstructor(Identifier aParentId, ConstructorView i_view)
    {
        parentId = aParentId;
        bluej_view = i_view;
    }


    /**
     * Tests if this constructor matches the given signature.
     *
     * @param  parameter  Description of the Parameter
     * @return            true if it does, false otherwise.
     */
    public boolean matches(Class<?>[] parameter)
    {
        Class<?>[] thisArgs = bluej_view.getParameters();

        // An empty array is equivalent to a null array
        if (thisArgs != null && thisArgs.length <= 0) {
            thisArgs = null;
        }
        if (parameter != null && parameter.length <= 0) {
            parameter = null;
        }

        // If both are null the we are OK
        if (thisArgs == null && parameter == null) {
            return true;
        }

        // If ANY of them is null we are in trouble now. (They MUST be both NOT null)
        if (thisArgs == null || parameter == null) {
            return false;
        }

        // Now I know that BOTH are NOT empty. They MUST be the same length
        if (thisArgs.length != parameter.length) {
            return false;
        }

        for (int index = 0; index < thisArgs.length; index++) {
            if (!thisArgs[index].isAssignableFrom(parameter[index])) {
                return false;
            }
        }

        return true;
    }


    /**
     * Returns the parameters of this constructor.
     * Similar to reflection API.
     *
     * @return    The parameterTypes value
     */
    public Class<?>[] getParameterTypes()
    {
        return bluej_view.getParameters();
    }


    /**
     * Creates a new instance of the object described by this constructor.
     * Similar to reflection API. Note that this method should not be called
     * from the AWT/Swing event-dispatching thread.
     * 
     * <p>The arguments passed in the initargs array may have any type,
     * but the type will determine exactly what is passed to the
     * constructor:
     * 
     * <ul>
     * <li>String - the String will be passed directly to the constructor
     * <li>BObject - the object will be passed directly to the constructor,
     *               though it must be on the object bench for this to work
     * <li>Anything else - toString() is called on the object and the
     *               result is treated as a Java expression, which is
     *               evaluated and passed to the constructor.
     * </ul>
     * 
     * <p>An attempt is made to ensure that the argument types are suitable
     * for the constructor. InvocationArgumentException will be thrown if
     * the arguments are clearly unsuitable, however some cases will
     * generate an InvocationErrorException instead. In such cases no
     * expression arguments will be evaluated.
     *
     * @param  initargs                      Description of the Parameter
     * @return                               Description of the Return Value
     * @throws  ProjectNotOpenException      if the project to which this constructor belongs has been closed by the user.
     * @throws  PackageNotFoundException     if the package to which this constructor belongs has been deleted by the user.
     * @throws  InvocationArgumentException  if the <code>initargs</code> don't match the constructor's arguments.
     * @throws  InvocationErrorException     if an error occurs during the invocation.
     */
    public BObject newInstance(Object[] initargs)
             throws ProjectNotOpenException, PackageNotFoundException,
            InvocationArgumentException, InvocationErrorException
    {
        PkgMgrFrame pkgFrame = parentId.getPackageFrame();

        DirectInvoker invoker = new DirectInvoker(pkgFrame);
        DebuggerObject result = invoker.invokeConstructor(bluej_view, initargs);

        if (result == null) {
            return null;
        }

        String resultName = invoker.getResultName();
        PkgMgrFrame pmf = parentId.getPackageFrame();

        ObjectWrapper wrapper = ObjectWrapper.getWrapper(pmf, pmf.getObjectBench(), result, result.getGenType(), resultName);

        return new BObject(wrapper);
    }

    /**
     * Returns the modifier of this constructor. The
     * {@link java.lang.reflect.Modifier} class can be used to decode the
     * modifiers.
     */
    public int getModifiers()
    {
        return bluej_view.getModifiers();
    }

    /**
     *  Description of the Method
     *
     * @return    Description of the Return Value
     */
    public String toString()
    {
        if (bluej_view != null) {
            return "BConstructor: " + bluej_view.getLongDesc();
        }
        else {
            return "BConstructor: ";
        }
    }
}
