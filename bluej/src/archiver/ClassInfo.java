/*
 * @(#)ClassInfo.java	1.2 99/10/08
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
 * @version 1.2 10/08/99
 * @author Philip Milne
 */

public class ClassInfo {
    private Constructor constructor; 
    private Vector constructorProperties; 
    private Vector normalProperties; 
    private Class declaringClass; 
        
    public ClassInfo(Class declaringClass) {
        this(declaringClass, null, null); 
    }

    public ClassInfo(Class declaringClass, String[] propertyNames) {
        this(declaringClass, null, propertyNames);  
    }

    public ClassInfo(Class declaringClass, String[] constructorArgs, String[] propertyNames) {
        this.declaringClass = declaringClass; 
        constructorProperties = new Vector(); 
        if (constructorArgs != null) {
            for (int i = 0; i < constructorArgs.length; i++) {
                Property p = new Property(declaringClass, constructorArgs[i], true);
                constructorProperties.addElement(p); 
            } 
        }
        normalProperties = new Vector(); 
        if (propertyNames != null) {
            for (int i = 0; i < propertyNames.length; i++) {
                    Property p = new Property(declaringClass, propertyNames[i]); 
                    normalProperties.addElement(p); 
            }
        }
    }

    public Property getProperty(String propertyName) {
        Vector descriptors = getProperties(); 
        for(int i = 0; i < descriptors.size(); i++) { 
            Property d = (Property)descriptors.elementAt(i); 
            if (propertyName.equals(d.getName())) { 
                return d; 
            }
        }
            return null;
    }

    public void addProperty(Property p) {
        normalProperties.addElement(p); 
    }

    public void removeProperty(String name) {
        normalProperties.removeElement(getProperty(name)); 
    }

    public void addProperty(String name) {
        normalProperties.addElement(new Property(declaringClass, name)); 
    }

    private void addConstructorProperty(Property p) {
        constructor = null; 
        constructorProperties.addElement(p); 
    }

    public Vector getProperties() {
        return getProperties(false); 
    }

    private Vector getProperties(boolean constructor) {
        return constructor ? constructorProperties : normalProperties;
    }

    private Object uq2(Object o) { 
        return new Object[]{"unquote", o}; 
    }
    
    // Should this throw exception? 
    public Object getConstructor(Object newInstance, Object oldInstance) throws Exception { 
        // System.out.println("getConstructor: " + newInstance + " " + oldInstance); 
        if (newInstance.equals(oldInstance)) { 
            return null; 
        }
        int nArgs = constructorProperties.size(); 
        Class newClass = newInstance.getClass(); 
        // Assume the instance is either mutable or a singleton 
        // if it has a nullary constructor. 
        if (nArgs == 0 && oldInstance != null && newClass == oldInstance.getClass()) { 
            return null; 
        }
        Object[] constructor = new Object[nArgs + 1]; 
        constructor[0] = "new"; 
        for(int i = 0; i < nArgs; i++) { 
            Property p = (Property)constructorProperties.elementAt(i); 
            constructor[i+1] = uq2(p.get(newInstance)); 
        } 
        return new Object[]{".", uq2(newClass), constructor}; 
    } 
    
    public Object[] getInitializer(Object newInstance, Object oldInstance) { 
        return null; 
    } 
}
