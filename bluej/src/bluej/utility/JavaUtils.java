/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
package bluej.utility;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bluej.Config;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.JavaType;

/**
 * Utilities for dealing with reflection, which must behave differently for
 * Java 1.4 / 1.5. Use the factory method "getJavaUtils" to retrieve an object
 * to use. 
 *   
 * @author Davin McCall
 * @version $Id: JavaUtils.java 6164 2009-02-19 18:11:32Z polle $
 */
public abstract class JavaUtils {

    private static JavaUtils jutils;
    
    /**
     * Factory method. Returns a JavaUtils object.
     * @return an object supporting the appropriate feature set
     */
    public static JavaUtils getJavaUtils()
    {
        if( jutils != null ) {
            return jutils;
        }
        
        if (Config.isJava15()) {
            try {
                Class J15Class = Class.forName("bluej.utility.JavaUtils15");
                jutils = (JavaUtils)J15Class.newInstance();
            }
            catch(ClassNotFoundException cnfe) { }
            catch(IllegalAccessException iae) { }
            catch(InstantiationException ie) { }
        }
        else {
            jutils = new JavaUtils14();
        }
        
        return jutils;
    }
    
    /**
     * Get a "signature" description of a method.
     * Looks like:  void method(int, int, int)
     *   (ie. excludes parameter names)
     * @param method The method to get the signature for
     * @return the signature string
     */
    abstract public String getSignature(Method method);
    
    /**
     * Get a "signature" description of a constructor.
     * Looks like:  ClassName(int, int, int)
     *   (ie. excludes parameter names)
     * @param cons the Constructor to get the signature for
     * @return the signature string
     */
    abstract public String getSignature(Constructor cons);
 
    /**
     * Get a "short description" of a method. This is like the signature,
     * but substitutes the parameter names for their types.
     * 
     * @param method   The method to get the description of
     * @param paramnames  The parameter names of the method
     * @return The description.
     */
    abstract public String getShortDesc(Method method, String [] paramnames);

    /**
     * Get a "short description" of a method, and map class type parameters to
     * the given types. A short description is like the signature, but
     * substitutes the parameter names for their types. Generic method type
     * parameters are left unmapped.
     * 
     * @param method   The method to get the description of
     * @param paramnames The parameter names of the method
     * @param tparams  The map (String -> GenType) for class type parameters
     * @return The description.
     */
    abstract public String getShortDesc(Method method, String [] paramnames, Map tparams);

    /**
     * Get a long String describing the method. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    abstract public String getLongDesc(Method method, String [] paramnames);
    
    /**
     * Get a long String describing the method, with class type parameters
     * mapped to their instantiation types. A long description is similar to a
     * short description, but it has type names of parameters included.
     * 
     * @param method   The method to get the description of
     * @param paramnames  The parameters names of the method
     * @param tparams  The map (String -> GenType) for class type parameters
     * @return The long description string.
     */
    abstract public String getLongDesc(Method method, String [] paramnames, Map tparams);
    
    /**
     * Get a "short description" of a constructor. This is like the signature,
     * but substitutes the parameter names for their types.
     * 
     * @param constructor   The constructor to get the description of
     * @return The description.
     */
    abstract public String getShortDesc(Constructor constructor, String [] paramnames);
    
    /**
     * Get a long String describing the constructor. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    abstract public String getLongDesc(Constructor constructor, String [] paramnames);
    
    abstract public boolean isVarArgs(Constructor cons);
    
    abstract public boolean isVarArgs(Method method);    
   
    abstract public boolean isSynthetic(Method method);
    
    abstract public boolean isEnum(Class cl);    
    
    /**
     * Get the return type of a method.
     */
    abstract public JavaType getReturnType(Method method);
    
    abstract public JavaType getRawReturnType(Method method);

    /**
     * Get the declared type of a field.
     */
    abstract public JavaType getFieldType(Field field);
    
    abstract public JavaType getRawFieldType(Field field);
    
    /**
     * Get a list of the type parameters for a generic method.
     * (return an empty list if the method is not generic).
     * 
     * @param method   The method fro which to find the type parameters
     * @return  A list of GenTypeDeclTpar
     */
    abstract public List getTypeParams(Method method);
    
    /**
     * Get a list of the type parameters for a generic constructor.
     * (return an empty list if the method is not generic).
     * 
     * @param method   The method fro which to find the type parameters
     * @return  A list of GenTypeDeclTpar
     */
    abstract public List getTypeParams(Constructor cons);
    
    /**
     * Get a list of the type parameters for a class. Return an empty list if
     * the class is not generic.
     * 
     * @param cl the class
     * @return A List of GenTypeDeclTpar
     */
    abstract public List getTypeParams(Class cl);
    
    /**
     * Get the declared supertype of a class.
     */
    abstract public GenTypeClass getSuperclass(Class cl);
    
    /**
     * Get a list of the interfaces directly implemented by the given class.
     * @param cl  The class for which to find the interfaces
     * @return    An array of interfaces
     */
    abstract public GenTypeClass [] getInterfaces(Class cl);
    
    /**
     * Gets an array of nicely formatted strings with the types of the parameters.
     * Include the ellipsis (...) for a varargs method.
     * 
     * @param method The method to get the parameters for.
     */
    abstract public String[] getParameterTypes(Method method);
    
    /**
     * Get an array containing the argument types of the method.
     * 
     * In the case of a varargs method, the last argument will be an array
     * type.
     * 
     * @param method  the method whose argument types to get
     * @param raw     whether to return the raw versions of argument types
     * @return  the argument types
     */
    abstract public JavaType[] getParamGenTypes(Method method, boolean raw);
    
    /**
     * Gets an array of nicely formatted strings with the types of the parameters.
     * Include the ellipsis (...) for a varargs constructor.
     * 
     * @param constructor The constructor to get the parameters for.
     */
    abstract public String[] getParameterTypes(Constructor constructor);
    
    /**
     * Get an array containing the argument types of the method.
     * 
     * In the case of a varargs method, the last argument will be an array
     * type.
     * 
     * @param method  the method whose argument types to get
     * @return  the argument types
     */
    abstract public JavaType[] getParamGenTypes(Constructor constructor);


    
    /**
     * Build a JavaType structure from a "Class" object.
     */
    abstract public JavaType genTypeFromClass(Class t);
    
    /**
     * Open a web browser to show the given URL. On Java 6+ we can use
     * the desktop integration functionality of the JDK to do this. On
     * prior versions we fall back to older methods.
     * 
     * @return true if successful
     */
    public boolean openWebBrowser(URL url)
    {
        // For now, do this via reflection so that BlueJ can be built
        // on Java 5 and earlier.
        
        try {
            Class cl = Class.forName("java.awt.Desktop");
            Method m = cl.getMethod("isDesktopSupported", new Class[0]);
            Boolean result = (Boolean) m.invoke(null, null);
            if (result.booleanValue()) {
                // The Desktop abstraction is supported
                m = cl.getMethod("getDesktop", new Class[0]);
                Object desktop = m.invoke(null, null);
                
                // Invoke the browse method
                m = cl.getMethod("browse", new Class[] {URI.class});
                m.invoke(desktop, new Object[] {url.toURI()});
                return true;
            }
        }
        catch (ClassNotFoundException cnfe) {}
        catch (NoSuchMethodException nsme) {}
        catch (IllegalAccessException iae) {}
        catch (InvocationTargetException ite) {}
        catch (URISyntaxException use) {}
        return false;
    }
    
    /**
     * Change a list of type parameters (with bounds) into a map, which maps
     * the name of the parameter to its bounding type.
     * 
     * @param tparams   A list of GenTypeDeclTpar
     * @return          A map (String -> GenTypeSolid)
     */
    public static Map TParamsToMap(List tparams)
    {
        Map rmap = new HashMap();
        for( Iterator i = tparams.iterator(); i.hasNext(); ) {
            GenTypeDeclTpar n = (GenTypeDeclTpar)i.next();
            rmap.put(n.getTparName(), n.getBound().mapTparsToTypes(rmap));
        }
        return rmap;
    }

    /**
     * Make a descriptive signature. This includes the method/constructor name (which may
     * be preceded by type parameters), and parameter types or names or types and names.
     * (The type is always substituted if the name is missing). 
     * 
     * @param name       The method/constructor name (including preceding
     *                          type parameters if any)
     * @param paramTypes   The parameter types
     * @param paramNames   The parameter names (may be null)
     * @param includeTypeNames   True if the parameter type should always be included
     * @param isVarArgs      True if the method is varargs (requires ellipsis insertion)
     */
    protected static String makeDescription(String name, String[] paramTypes, String[] paramNames, boolean includeTypeNames, boolean isVarArgs)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append("(");
        for (int j = 0; j < paramTypes.length; j++) {
            boolean typePrinted = false;
            if (isVarArgs && j == paramTypes.length - 1) {
                if (includeTypeNames || paramNames == null || paramNames[j] == null) {
                    sb.append(paramTypes[j].substring(0, paramTypes[j].length() - 2));
                    sb.append(" ");
                }
                sb.append("...");
                typePrinted = true;
            }
            else if (includeTypeNames || paramNames == null || paramNames[j] == null) {                              
                sb.append(paramTypes[j]);
                typePrinted = true;
            }
            
            if (paramNames != null && paramNames[j] != null) {
                if (typePrinted)
                    sb.append(" ");
                sb.append(paramNames[j]);
            }
            if (j < (paramTypes.length - 1))
                sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
