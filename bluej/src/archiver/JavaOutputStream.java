/*
 * @(#)JavaOutputStream.java	1.3 99/11/07
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

/**
 * @version 1.3 11/07/99
 * @author Philip Milne
 */

public class JavaOutputStream  extends DataOutputStream 
implements ObjectOutput {     
    static int instanceCount = 0; 
    String className = "Object" + instanceCount++; 
    private ExpressionGenerator generator; 
	
    public JavaOutputStream(OutputStream out) { 
        super(out); 
        generator = new ExpressionGenerator(); 
    }
    
    public JavaOutputStream(OutputStream out, String className) { 
        this(out); 
    	this.className = className; 
    }
    
    public void writeObject(Object o) throws IOException { 
        writeString("public class " + className + " { \n\t");
        writeString("public Object readObject() { \n\t\t");
        writeString("return " + output(generator.generateExpressionFor(o)) + "; \n\t"); 
        writeString("}\n ");
        writeString("}\n ");
    } 
	
    private String capitalize(String propertyName) {
        return propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }        

    private void writeString(String exp) throws IOException { 
        out.write(exp.getBytes());     
    }
    
    private Object evaluate(Object exp) { 
        try { 
	    return generator.getEnvironment().evaluate(exp); 
        } 
	catch (Throwable e) { 
	    System.err.println("Internal error"); 
	    e.printStackTrace(); 
	};
	return null; 
    }
	
    private String output(Object exp) throws IOException { 
        // System.out.println(ArrayEnumerator.enumerationToString(exp)); 
        if (!(exp instanceof Object[])) { 
            return (exp == null ? "null" : exp.toString()); 
        } 
        Object[] a = (Object[])exp;
        Object f = (a.length < 1) ? null : a[0]; 
        if (f == "\"") { 
            return "\"" + a[1].toString() + "\""; 
        }
        if (f == ".") { 
            if (a.length == 3 && a[2].getClass().isArray()) { 
		Object[] b =(Object[])a[2]; 
		if (b[0] == "new") { 
		    Class clazz = (Class)evaluate(a[1]); 
                    // Always use primitive types where possible. 
                    if (clazz == Integer.class || clazz == Boolean.class) { 
                        return ((Object[])b[1])[1].toString(); 
                    }
		    return "new " + clazz.getName() + arrayToString(b, 1, ", "); 
		} 
                else { 
                    return output(a[1]) + "." + output(b[0]) + arrayToString(b, 1, ", "); 
                }
            }
            return output(a[1]) + ".get" + capitalize((String)a[2]) + "()"; 
        }
        if (f == "=") { 
            if (a[1].getClass().isArray()) { 
            	Object[] b = (Object[])a[1]; 
                if (b[0] == ".") { 
                    return output(b[1]) + ".set" + capitalize((String)b[2]) + "(" + output(a[2]) + ")"; 
                }
            } 
	    // The evaulation here is just effecting a lookup. 
	    Object lhs = evaluate(a[1]); 
            if (lhs instanceof Class) { 
                return a[1].toString(); 
            } 
            return lhs.getClass().getName() + " " + output(a[1]) + " = " + output(a[2]);    
        }
        if (f == "block") { 
            for(int i = 1; i < a.length-1; i++) { 
                Object s = output(a[i]); 
                if (a[i].getClass().isArray() && ((Object[])a[i])[0] != "block") {  
                    writeString(s + "; \n\t\t"); 
                }
            }
            return a[a.length - 1].toString(); 
        }
        return output(f) + arrayToString(a, 1, ", ");  
    }
    
    private String arrayToString(Object[] a, int start, String separator) throws IOException { 
        String result = "("; 
        for(int i = start; i < a.length-1; i++) { 
            result = result + output(a[i]) + separator; 
        }
        if (a.length-1 >= start) { 
            result = result + output(a[a.length-1]); 
        } 
        return result + ")"; 
    }
}

