/*
 * @(#)ExpressionGenerator.java	1.8 99/11/08
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

import java.io.*; 

import java.util.*; 
import java.lang.reflect.Array; 

/**
 * @version 1.8 11/08/99
 * @author Philip Milne
 */

public class ExpressionGenerator {
    
    private Hashtable instances; 
    private MetaData metaData; 
    private Hashtable valueToName; 
    private Evaluator environment; 
    private Simplifier simplifier; 
    
    public ExpressionGenerator() { 
        metaData = new MetaData(); 
    }
        
    // Not used. 
    public void setMetaData(MetaData metaData) { 
        this.metaData = metaData; 
    }    
    
    // For Java and XML output streams. 
    public Evaluator getEnvironment() { 
        return environment; 
    }    
    
    private String unqualifiedClassName(Class type) { 
        String name = type.getName(); 
        if (!type.isArray()) {
            return name.substring(name.lastIndexOf('.')+1); 
        }
        else { 
            return unqualifiedClassName(type.getComponentType())+"Array"; 
        }
    }
    
    private class InstanceWrapper { 
        public Object o; 
        
        public InstanceWrapper(Object o) { 
            this.o = o; 
        }

        public boolean equals(Object o2) { 
            return (o2.getClass() == InstanceWrapper.class) && (o == ((InstanceWrapper)o2).o); 
        }

        public int hashCode() { 
            return System.identityHashCode(o); 
        }
    }

    private String valueToName(Object o) { 
        InstanceWrapper w = new InstanceWrapper(o); 
        return (String)valueToName.get(w); 
    }

    private void setValueToName(Object o, String name) { 
        valueToName.put(new InstanceWrapper(o), name); 
    }

    private String instanceName(Object instance) { 
        if (instance instanceof Class) {
            return unqualifiedClassName((Class)instance); 
        }
        else { 
            Class type = instance.getClass(); 
            Object size = instances.get(type); 
            int instanceNumber = (size == null) ? 0 : ((Integer)size).intValue() + 1; 
            instances.put(type, new Integer(instanceNumber)); 
            return unqualifiedClassName(type) + instanceNumber; 
        }
    }
        
    private void init() { 
        valueToName = new Hashtable(); 
        environment = new Evaluator(); 
        instances = new Hashtable(); 
        simplifier = new Simplifier(); 
        // environment.showDebuggingInfo(true); 
    }
    
    public Object generateExpressionFor(Object o) { 
        init();
        setValueToName(Class.class, "Class"); 
        Object result = writeNode(o); 
        // System.out.println(ArrayEnumerator.enumerationToString2(result)); 
        result = simplifier.simplify(result); 
        // System.out.println(ArrayEnumerator.enumerationToString2(result)); 
        return result; 
    }
    
    private Object primitiveRepresentation(Object primitive) { 
        if (primitive == null) { 
            return null; 
        }
        if (primitive instanceof String) { 
            return new Object[]{"\"", primitive}; 
        }
        return primitive; 
    }
    
    private Object set(Object exp, Object foo) { 
        return new Object[]{"=", exp, foo}; 
    }
    
    private Object setExp(Object exp, Object value) { 
        return (exp == null) ? value : set(exp, value); 
    }
     
    private Object writeNode(Object node) { 
        return writeNode(node, null); 
    } 
    
    private Object expressionName(Object exp) { 
        if (exp != null && exp.getClass().isArray()) { 
             if (Array.get(exp, 0) == "=") { 
                 return (String)Array.get(exp, 1); 
             }
             if (Array.get(exp, 0) == "block") { 
                 return (String)Array.get(exp, Array.getLength(exp)-1); 
             }
        } 
        return exp; 
    }
    
    private Object[] appendArrays(Object[] a , Object[] b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        Object[] result = new Object[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private Object makeBlock(String name, Object instantiation, Object[] initializer) { 
        if (initializer == null) { 
            return instantiation;      
        }
        else { 
            int n = initializer.length; 
            Object[] result = new Object[n+3]; 
            result[0] = "block"; 
            result[1] = instantiation; 
            for (int i = 0; i < n; i++) { 
                result[i+2] = initializer[i]; 
            } 
            result[n+2] = name;                 
            return result; 
        } 
    }

    private Object writeNode(Object node, Object exp) { 
        // System.out.println("writeNode: " + node + " " + ((node == null) ? "null" : node.getClass().toString()) + " " + ArrayEnumerator.enumerationToString(exp)); 
        String instanceName = (node == null) ? "null" : instanceName(node); 
        
        try { 
            Object reference = environment.evaluate(exp); 
            if (node == null) { 
                if (reference == null) { 
                    return null; 
                }
                else { 
                    exp = set(exp, "null"); 
                    environment.evaluate(exp); 
                    return exp; 
                }
            }

    //  Instantiate this node.  
            ClassInfo info = metaData.getClassInfo(node.getClass()); 
            Object instantiation = info.getConstructor(node, reference); 
            if (instantiation != null) { 
                instantiation = set(instanceName, instantiation); 
            }
            //System.out.println("instantiation: " + ArrayEnumerator.enumerationToString(instantiation)); 

            Object candidate = valueToName(node); 
            //System.out.println("candidate: " + candidate); 
            if (candidate != null) { 
                return (instantiation == null) ? null : setExp(exp, candidate); 
            }            

            if (instantiation != null) { 
                // System.out.println("instantiation: " + ArrayEnumerator.enumerationToString(instantiation));
                instantiation = mapExpression(instantiation); 
                exp = setExp(exp, instantiation); 
                // System.out.println("instantiation (post mapping): " + ArrayEnumerator.enumerationToString(exp));
                reference = environment.evaluate(exp); 
            } 
            else { 
                exp = set(instanceName, exp); 
                environment.evaluate(set(instanceName, new Object[]{"\"", reference}));
            }
            
            setValueToName(node, instanceName); 
            Object[] initializer = metaData.getInitializer(node, reference); 
            //System.out.println("initializer: " + ArrayEnumerator.enumerationToString(initializer));
            initializer = (Object[])mapExpression(initializer); 
            if (initializer != null) { 
                for(int i = 0; i < initializer.length; i++) { 
                    // System.out.println("Evaluating: " + ArrayEnumerator.enumerationToString(initializer[i]));
                    environment.evaluate(initializer[i]); 
                }
            }
            Object[] properties = setProperties(node, instanceName); 
            initializer = appendArrays(initializer, properties); 
            //System.out.println("initializer (post mapping): " + ArrayEnumerator.enumerationToString(initializer));
            return makeBlock(instanceName, exp, initializer); 
        }
        catch (Throwable e) {
            System.err.println("Warning: Failed to make instance of " + ((node == null) ? null : node.getClass()) + "." + " for expression: " + ArrayEnumerator.enumerationToString2(exp));
            // System.err.println("Failed to evaulate: " + ArrayEnumerator.enumerationToString2(expTmp));
            if (e instanceof java.lang.reflect.InvocationTargetException) {
                e = ((java.lang.reflect.InvocationTargetException)e).getTargetException();
            }
            System.err.println(e);
            // e.printStackTrace();
            System.err.println("Continuing ..."); 
            return instanceName; // Note: exp is unbound here. 
        }
    }
    
    // Write out the properties of this instance.                 
    private Object[] setProperties(Object node, String instanceName) { 
        Vector block = new Vector();         
        Enumeration properties = metaData.getProperties(node); 
        while(properties.hasMoreElements()) { 
            Property property = (Property)properties.nextElement(); 
            String propertyName = property.getName();
            try { 
                // System.out.println("Property: " + propertyName + " of " + node.getClass()); 
                Object targetValue = property.get(node); 
                Object getExp = new Object[]{".", instanceName, propertyName};      
                Object name = writeNode(targetValue, getExp); 
                block.addElement(name); 
            }
            catch (Throwable e) { 
                System.err.println("Warning: Failed to assign " + propertyName + " property of " + instanceName + "."); 
                if (e instanceof java.lang.reflect.InvocationTargetException) {
                    e = ((java.lang.reflect.InvocationTargetException)e).getTargetException();
                }
                // e.printStackTrace(); 
                System.err.println(e); 
                System.err.println("Continuing ..."); 
            }
        } 
        return ArrayEnumerator.enumerationToArray(block.elements()); 
    } 
 
    private Object mapExpression(Object exp) { 
        if (exp instanceof Object[]) { 
            Object[] a = (Object[])exp; 
            int n = a.length; 
            if (n == 2 && "unquote".equals(a[0])) {  
                return writeNode(a[1]); 
            }
            Object[] b = new Object[n]; 
            for(int i = 0; i < n; i++) { 
                b[i] = mapExpression(a[i]); 
            } 
            return b; 
        }; 
        return exp; 
    }

}


