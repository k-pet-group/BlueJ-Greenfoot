/*
 * @(#)Evaluator.java	1.3 99/11/09
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
 * @version 1.3 11/09/99
 * @author Philip Milne
 */

public class Evaluator {
    private Hashtable nameToValue; 
    private Object hashtableNull = new Object();

    // Debugging info. 
    private int evaluations = 0; 
    private boolean showDebuggingInfo = false; 
    
    public Evaluator() { 
    	nameToValue = new Hashtable(); 

    	installPrimitives(); 
    } 
	
    public void showDebuggingInfo(boolean b) { 
        showDebuggingInfo = b; 
    }
            
    private interface Lambda { 
        public Object value(Object[] args) throws Exception;
    }
                
    private class UnboundVariableException extends Exception { 
        public UnboundVariableException(String e) { super(e); }
    }
    
    private void setValueForName(String name, Object value) { 
    	nameToValue.put(name, (value == null) ? hashtableNull : value); 
    }
	
    private Object getValueForName(Object name) throws UnboundVariableException { 
        Object result = nameToValue.get(name); 
        if (result != null) { 
            return (result == hashtableNull) ? null : result; 
        } 
        else { 
            throw(new UnboundVariableException(name.toString())); 
        }
    }

    private Object[] evaluateArray(Object[] a, int start) throws Exception {        
        Object[] result = new Object[a.length - start]; 
        for(int i = start; i < a.length; i++) { 
            result[i - start] = evaluate(a[i]); 
        }
        return result; 
    }
                            
    private void installPrimitives() { 
    	
    	setValueForName("true", Boolean.TRUE); 
        setValueForName("false", Boolean.FALSE); 
        setValueForName("Class", Class.class); 
        setValueForName("null", null); 
        
        setValueForName(".", new Lambda() { 
            public Object value(Object[] a) throws Exception { 
                Object a1 = evaluate(a[1]); 
                Object a2 = a[2]; 
                if (a2.getClass().isArray()) { 
                    Object[] a2a = (Object[])a2; 
                    String name = (String)a2a[0]; 
                    return invoke(a1, name, evaluateArray(a2a, 1)); 
                }
                else { 
                    Class type = (a1 instanceof Class) ? (Class)a1 : a1.getClass(); 
                    // Expose an array's elements as properties. 
                    String pName = a2.toString(); 
                    if (type.isArray() && pName.charAt(0) < 'a') { 
                        return Array.get(a1, new Integer(pName).intValue()); 
                    }
                    Property p = new Property(type, pName, true); 
                    return p.get(a1); 
                }
            } 
        }); 
        
        setValueForName("\"", new Lambda() { 
            public Object value(Object[] a) throws Exception { 
                return a[1]; 
            } 
        }); 
        
        setValueForName("=", new Lambda() { 
            public Object value(Object[] a) throws Exception { 
                Object a1 = a[1]; 
                Object a2 = evaluate(a[2]); 
                if (a1.getClass().isArray() && ((Object[])a1)[0] == ".") { 
                    Object[] lhs = (Object[])a1; 
                    Object a11 = evaluate(lhs[1]); 
                    Class type = a11.getClass(); 
                    String pName = lhs[2].toString(); 
                    if (type.isArray() && pName.charAt(0) < 'a') { 
                        Array.set(a11, new Integer(pName).intValue(), a2); 
                    }
                    else { 
                        Property p = new Property(type, pName, false); 
                        p.set(a11, a2); 
                    }
                }
                else {
                    setValueForName((String)a1, a2); 
                }
                return a2; 
            } 
        }); 
        
        setValueForName("block", new Lambda() { 
            public Object value(Object[] a) throws Exception { 
                for(int i = 1; i < a.length - 1; i++) { 
                    try { 
                        evaluate(a[i]); 
                    } 
                    catch (Throwable e) { 
                        e.printStackTrace(); 
			System.err.println(e); 
                        System.err.println("Continuing ...");
                    };
                }
                return evaluate(a[a.length - 1]); 
            } 
        }); 
        
        setValueForName("array", new Lambda() { 
            public Object value(Object[] a) throws Exception { 
                return evaluateArray(a, 1); 
            } 
        }); 
    }
                
    public Object evaluate(Object expression) throws Exception { 
        try { 
            if (showDebuggingInfo) { 
                System.out.println("Evaluation[" + (evaluations++) + "]: " + ArrayEnumerator.enumerationToString(expression)); 
            }
            if (expression == null || 
                expression instanceof Number ||  
                expression instanceof Boolean  ||  
                expression instanceof Class) { 
                return expression; 
            } 
            if (expression.getClass().isArray()) { 
                Object[] exp = (Object[])expression; 
	        if (exp.length > 0) { 
                    boolean tmp = showDebuggingInfo; 
                    showDebuggingInfo = false; 
	    	    Object head = evaluate(exp[0]);
		    if (head instanceof Lambda) {
                        Lambda l = (Lambda)head; 
                        Object result = l.value(exp); 
                        showDebuggingInfo = tmp; 
                        return result; 
		    }
                    showDebuggingInfo = tmp; 
                }
            } 
            return getValueForName(expression); 
        } 
        catch (Exception e) { 
            System.err.println("Failed to evaluate: " + ArrayEnumerator.enumerationToString2(expression)); 
            throw e;     
        }
    } 
    
    private Class typeToClass(Class type) {
        if (!type.isPrimitive()) return type;
        if (type == Boolean.TYPE) return Boolean.class;
        if (type == Byte.TYPE) return Byte.class;
        if (type == Character.TYPE) return Character.class;
        if (type == Short.TYPE) return Short.class;
        if (type == Integer.TYPE) return Integer.class;
        if (type == Long.TYPE) return Long.class;
        if (type == Float.TYPE) return Float.class;
        if (type == Double.TYPE) return Double.class;
        if (type == Void.TYPE) return Void.class;
        return null;
    }

    private int coercionIndex(Class type) { 
        if (type == Byte.class) return 1;
        // if (type == Character.class) return 2;
        if (type == Short.class) return 3;
        if (type == Integer.class) return 4;
        if (type == Long.class) return 5;
        if (type == Float.class) return 6;
        if (type == Double.class) return 7;
        return -1;
    }
            
    private boolean isAssignableFrom(Class sType, Class dType) { 
        if (sType.isAssignableFrom(dType)) { 
            return true; 
        } 
        // If both types are numbers, return true if a primitive widening 
        // coercion is possible. See JLS $5.1.2. 
        if (Number.class.isAssignableFrom(sType) && Number.class.isAssignableFrom(dType)) { 
            return coercionIndex(sType) >= coercionIndex(dType); 
        }
        return false; 
    }
    
    private boolean matchArgs(Object[] args, Class[] argTypes) {
        boolean match = (args.length == argTypes.length);
        for(int j = 0; j < args.length && match; j++) {
            Class argType = argTypes[j];
            if (argType.isPrimitive()) {
                argType = typeToClass(argType);
            }
            // System.out.println("Matching: " + args[j] + " " + args[j].getClass() + " " + argType); 
            // Consider null an instance of all classes.
            if (args[j] != null && !isAssignableFrom(argType, args[j].getClass())) {
                match = false;
            }
            // System.out.println("Match is " + match); 
        }
        return match;
    }

    // Pending: throw when the match is ambiguous.
    private Method findMethod(Class declaringClass, String name, Object[] args) {
        Method[] methods = declaringClass.getMethods(); 
        for(int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().equals(name)) {
                if (matchArgs(args, method.getParameterTypes())) {
                    return method;
                }
            }
        }
        return null;
    }

    private Object invoke(Object o, String name, Object[] args) throws Exception {
        if (!(o instanceof Class)) {
            Method m = findMethod(o.getClass(), name, args);
            if (m != null) {
                return m.invoke(o, args);
            }
        }
        // For class methods, simluate the effect of a meta class
        // by taking the union of the static methods of the
        // actual class, with the instance methods of "Class.class"
        // and the overloaded "newInstance" methods defined by the
        // constructors.
        // This way the "System" class, for example, will perform both
        // the static method getProperties() and the instance method
        // getSuperclass() defined in "Class.class".
        else {
            if (name == "new") {
                name = "newInstance";
            }
            if (name == "newInstance" && o instanceof Class && args.length != 0) {
                Constructor[] constructors = ((Class)o).getConstructors();
                // PENDING: Implement the resolutuion of ambiguities properly. 
                /*
                For now we simply return the first match - but 
                we run the loop backwards to avoid the problem with 
                java.awt.Color which defines two constructors: 
                    Color(float r, float g, float b) and 
                    Color(int r, int g, int b). 
                The float definition happens to come first but has different 
                semantics to the integer version, requiring 
                each argument to be in the range [0-1]. 
                */
                // for(int i = 0; i < constructors.length; i++) {
                for(int i = constructors.length-1; i >= 0; i--) {
                    Constructor constructor = constructors[i];
                    if (matchArgs(args, constructor.getParameterTypes())) {
                        return constructor.newInstance(args);
                    }
                }
            }
            Method m = findMethod((Class)o, name, args);
            if (m == null) {
                m = findMethod(Class.class, name, args);
            }
            if (m != null) {
                return m.invoke(o, args);
            }
        } 
        String error = "No \"" + name + "\" method defined on: " + o + " with arguments: " + ArrayEnumerator.enumerationToString(args); 
        throw new NoSuchMethodException(error);
    }
}


