/*
 * @(#)BeanScriptOutputStream.java	1.4 99/11/09
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
 * @version 1.4 11/09/99
 * @author Philip Milne
 */

public class BeanScriptOutputStream extends DataOutputStream 
implements ObjectOutput { 

    private ExpressionGenerator generator; 

    public BeanScriptOutputStream(OutputStream out) { 
        super(out); 
        generator = new ExpressionGenerator(); 
    }

    public void writeObject(Object o) throws IOException { 
        writeString("{ \n"); 
        writeString("\t" + output(generator.generateExpressionFor(o)) + " \n"); 
        writeString("}; \n"); 
    } 
    
    private void writeString(String exp) throws IOException { 
        out.write(exp.getBytes());     
    }
    
    private boolean startsWith(Object exp, String fName) { 
        return (exp instanceof Object[]) && ((Object[])exp)[0] == fName; 
    }
            
    private String output(Object exp) throws IOException { 
        if (!(exp instanceof Object[])) { 
            return (exp == null) ? "null" : exp.toString(); 
        } 
    	Object[] a = (Object[])exp;
        Object f = (a.length < 1) ? null : a[0]; 
    	if (f == "\"") { 
            return "\"" + a[1] + "\""; 
        }
        if (f == ".") { 
            return output(a[1]) + "." + output(a[2]); 
        }
        if (f == "=") { 
            if (!a[1].getClass().isArray()) { 
                String lhs = output(a[1]).toString(); 
                writeString("\tlet " + lhs + " = " + output(a[2]) + "; \n"); 
                return lhs; 
            }
            return output(a[1]) + " := " + output(a[2]);    
        }
        if (f == "block") { 
            for(int i = 1; i < a.length-1; i++) { 
                Object s = output(a[i]); 
                if (a[i].getClass().isArray() && 
                    ((Object[])a[i])[0] != "block" && 
                    (((Object[])a[i])[0] != "=" || 
                     ((Object[])a[i])[1].getClass().isArray())) {  
                    writeString("\t" + s + "; \n"); 
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



