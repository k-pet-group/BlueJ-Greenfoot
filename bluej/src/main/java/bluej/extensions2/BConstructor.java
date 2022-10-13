/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2;


import bluej.debugger.DebuggerObject;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.ConstructorView;

/**
 * A wrapper for a constructor of a BlueJ class.
 * Behaviour is similar to reflection API.
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
     * Constructor. It is the duty of the caller to make sure that the parent is valid.
     *
     * @param  aParentId  the {@link Identifier} of the parent class.
     * @param  i_view     the {@link ConstructorView} of this constructor.
     */
    BConstructor(Identifier aParentId, ConstructorView i_view)
    {
        parentId = aParentId;
        bluej_view = i_view;
    }


    /**
     * Tests if this constructor matches the given signature.
     *
     * @param  parameter  signature of the constructor to compare.
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
     * Returns the parameters' types of this constructor.
     * Similar to reflection API.
     *
     * @return    An array of {@link Class} objects representing the parameters' types of the constructor.
     */
    public Class<?>[] getParametersTypes()
    {
        return bluej_view.getParameters();
    }


    /**
     * Creates a new instance of the object described by this constructor.
     * Similar to reflection API. Note that this method should not be called
     * from the JavaFX (GUI) thread.
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
     * @param  initargs                     An array of {@link Object} objects representing the arguments of this constructor.
     * @return                              A {@link BObject} object instancied with this constructor.
     * @throws ProjectNotOpenException      if the project to which this constructor belongs has been closed by the user.
     * @throws PackageNotFoundException     if the package to which this constructor belongs has been deleted by the user.
     * @throws InvocationArgumentException  if the <code>initargs</code> don't match the constructor's arguments.
     * @throws InvocationErrorException     if an error occurs during the invocation.
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
     *
     * @return An int value representing the modifiers which can be decoded with <code>java.lang.reflect.Modifier</code>.
     *
     */
    public int getModifiers()
    {
        return bluej_view.getModifiers();
    }

    /**
     *  Returns a string representation of the Object.
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
