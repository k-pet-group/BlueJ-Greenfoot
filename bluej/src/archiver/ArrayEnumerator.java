/*
 * @(#)ArrayEnumerator.java	1.1 99/09/27
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

import java.util.Enumeration; 
import java.util.Vector; 

/**
 * @version 1.1 09/27/99
 * @author Philip Milne
 */

public class ArrayEnumerator implements Enumeration { 
    Object[] a; 
    int index; 

    public ArrayEnumerator(Object[] a) { 
        this.a = a; 
    }
        
    public Object nextElement() { 
        return a[index++]; 
    }

    public boolean hasMoreElements() { 
        return (a != null) && index < a.length;  
    }
        
    public static Object[] enumerationToArray(Enumeration e) {
        return enumerationToArray(e, 0); 
    }
    
    private static Object[] enumerationToArray(Enumeration e, int size) {
        if (!e.hasMoreElements()) {
            return new Object[size]; 
        } else {
            Object o = e.nextElement(); 
            Object[] result = enumerationToArray(e, size+1); 
            result[size] = o; 
            return result; 
        }
    }

    public static String enumerationToString(Object o) { 
        Enumeration e; 
         
        if (o == null) {
            return "null"; 
        }
        else if (o instanceof Vector) {
            e = ((Vector)o).elements(); 
        }
        else if (o.getClass().isArray()) {
            e = new ArrayEnumerator((Object[])o); 
        }
        else if (o instanceof Enumeration) {
            e = ((Enumeration)o); 
        }
        else return o.toString(); 
         
        String result = "["; 
        while(e.hasMoreElements()) { 
            result = result + enumerationToString(e.nextElement()); 
            if (e.hasMoreElements()) { 
                result = result + ", "; 
            }
        }
        result = result + "]"; 
        return result; 
    } 
    
    public static String enumerationToString2(Object o) { 
        return enumerationToString2(o, "");
    }
    
    public static String enumerationToString2(Object o, String indent) { 
        Enumeration e; 
         
        if (o == null) {
            return "null"; 
        }
        else if (o instanceof Vector) {
            e = ((Vector)o).elements(); 
        }
        else if (o.getClass().isArray()) {
            e = new ArrayEnumerator((Object[])o); 
        }
        else if (o instanceof Enumeration) {
            e = ((Enumeration)o); 
        }
        else return o.toString(); 
         
        String result = "["; 
        boolean inBlock = false; 
        String indent2 = indent; 
        while(e.hasMoreElements()) { 
            Object exp = e.nextElement(); 
            if (inBlock) { 
                result = result + indent2;   
            }
            if (exp == "block") { 
                indent2 = indent + "  "; 
                inBlock = true; 
            } 
            result = result + enumerationToString2(exp, indent2); 
            if (e.hasMoreElements()) { 
                result = result + ", "; 
            }
            if (inBlock) { 
                result = result + "\n"; 
            }
        }
        if (inBlock) { 
            result = result + indent;   
        }
        result = result + "]"; 
        return result; 
    }
}



