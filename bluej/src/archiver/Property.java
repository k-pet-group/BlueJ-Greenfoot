/*
 * @(#)Property.java	1.2 99/10/19
 *
 * Copyright 1997, 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */
package archiver;

import java.util.*; 
import java.lang.reflect.*; 

/**
 * @version 1.2 10/19/99
 * @author Philip Milne
 */

public class Property { 
    private String name; 
    private Method setter; 
    private Method getter; 
    private Field  field; 

    public String getName() {
        return name; 
    }

    private boolean isReadable() {
        return (field != null) || (getter != null); 
    }

    private boolean isWritable() {
        return (field != null) || (setter != null); 
    }

    public Property() {
    }

    public Property(Class declaringClass, String name, boolean readOnly) { 
        // System.out.println("making new property for " + declaringClass + " name " + name + " readOnly " + readOnly); 
        this.name = name; 
        this.getter = getGetter(declaringClass, name); 
        if (this.getter != null) { 
            if (!readOnly) {
                Class argType = getter.getReturnType(); 
                this.setter = getSetter(declaringClass, argType, name); 
            }
            return;
        } 
        // Static members like System.out. If we put this code 
        // after the method code (as here) the check for 
        // statics is not needed. 
        field = getField(declaringClass, name); 
        if (field != null) { 
            field.setAccessible(true);
        }
        // System.out.println("Getter: " + this.getter + " setter: " + this.setter); 
    }

    public Property(Class declaringClass, String name) { 
        this(declaringClass, name, false); 
    }

    public Property(String name, Method getter, Method setter) { 
        this.name = name; 
        this.getter = getter; 
        this.setter = setter; 
    }

    public Object get(Object o) throws Exception { 
        if (!isReadable()) {
            throw new Exception("Cannot read " + name + " property instance of " + o.getClass()); 
        }

        if (field != null) { 
            // System.out.println("Getting " + name + " property of: " + o + " from field " + field); 
            return field.get(o); 
        }
        else {
            // System.out.println("Getting " + name + " property of: " + o + " with method " + getter); 
            return getter.invoke(o, new Object[]{}); 
        }
    }

    public void set(Object o, Object value) throws Exception { 
        if (!isWritable()) {
            throw new Exception("Cannot write " + name + " property of instance of " + o.getClass()  + " Field: " + field + "Setter: " + setter); 
        }
        if (field != null) { 
            // System.out.println("Setting " + name + " property of: " + o + " for field " + field); 
            field.set(o, value); 
        }
        else {
            // System.out.println("Setting " + name + " property of: " + o + " with method " + setter + " to " + value); 
            setter.invoke(o, new Object[]{value}); 
        }
    }


    private void printDiagnostic(String exceptionName, Object o, Object[] args) { 
        String addendum = (args.length != 1) ? "" : " to " + args[0]; 
        System.err.println(exceptionName + " while trying to set " + name + " for " + o + addendum); 
    }

    private Method getMethod(Class declaringClass, String methodName, Class[] argTypes) { 
        try {
            return declaringClass.getMethod(methodName, argTypes); 
        }
        catch (NoSuchMethodException e) { 
            // System.err.println(e); 
        }
        catch (SecurityException e) {
            System.err.println(e); 
        } 
        return null; 
    }

    private Field getField(Class declaringClass, String fieldName) { 
        try {
            return declaringClass.getDeclaredField(fieldName); 
        }
        catch (NoSuchFieldException e) { 
            // System.err.println(e); 
        }
        catch (SecurityException e) {
            System.err.println(e); 
        }
        return null; 
    }
        
    private String capitalize(String propertyName) {
        return propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }        

    private Method getGetter(Class declaringClass, String propertyName) { 
        Method method; 
        Class[] argTypes = new Class[0]; 
        String capitalizedName = capitalize(propertyName); 
        method = getMethod(declaringClass, "get"+capitalizedName, argTypes); 
        if (method == null) { 
            method = getMethod(declaringClass, "is"+capitalizedName, argTypes);
        }
        if (method == null) { 
            method = getMethod(declaringClass, propertyName, argTypes);
        } 
        if (method == null) { 
            // System.err.println("Could not find getter for property: " + propertyName + " on " + declaringClass); 
        } 
        return method; 
    }

    private Method getSetter(Class declaringClass, Class argType, String propertyName) {
        Method method; 
        String capitalizedName = capitalize(propertyName);             
        Class[] argTypes = new Class[]{argType}; 
        method = getMethod(declaringClass,  "set" + capitalizedName, argTypes); 
        if (method == null) { 
            // System.err.println("Could not find setter for property: " + propertyName + " on " + declaringClass + " arguments " + argType); 
        }
        return method; 
    }
}


