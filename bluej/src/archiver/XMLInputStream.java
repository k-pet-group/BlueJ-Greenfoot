/*
 * @(#)XMLInputStream.java	1.8 99/11/07
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
 * @version 1.8 11/07/99
 * @author Philip Milne
 */

public class XMLInputStream extends AbstractInputStream { 

    private Evaluator environment; 
    private int localVariableCount; 
    private boolean haveReadObject;             
    private boolean haveReadPreamble;             
                
    public XMLInputStream(InputStream in) { 
        super(in); 
        environment = new Evaluator();
    }

    public Object readObject() throws IOException { 
        localVariableCount = 0; 
        emptyLookAheadQueue();
        if (!haveReadPreamble) { 
            // readLine(); 
            readLine(); 
            haveReadPreamble = false;
        }
        haveReadObject = false;
        Object result = null; 
        while (!haveReadObject) { 
            Object exp = parse(); // The operator above ";".
            // System.out.println(ArrayEnumerator.enumerationToString2(exp)); 
            exp = map(exp, null);
            // System.out.println(ArrayEnumerator.enumerationToString2(exp)); 
            result = null; 
            // environment.showDebuggingInfo(true); 
            try { result = environment.evaluate(exp); } catch (Exception e) { e.printStackTrace(); }; 
            // System.out.println(ArrayEnumerator.enumerationToString2(result)); 
        }
        return result; 
    }

    private Object[] parseList() { 
        return ArrayEnumerator.enumerationToArray(new Enumeration() { 
	    public boolean hasMoreElements() { 
		if (peek() == ">") { 
		    readToken();
		} 
                if ((peek() == "<" && peek(1) == "/")) { 
                    readToken(); // Read the "<". 
                    readToken(); // Read the "/". 
                    readToken(); // Read the "foo". 
                    readToken(); // Read the ">". 
                    return false; 
                }
                if ((peek() == "/" && peek(1) == ">")) { 
                    readToken(); // Read the "/". 
                    readToken(); // Read the ">". 
                    return false; 
                }
                // Check for a badly formatted document to prevent hanging. 
                if (peek() == "EOF") { 
                    return false; 
                }
		return true;
            }
            
            public Object nextElement() { 
            	return parse();
            }
        });
    }    
	
    protected Object parse() { 
        Object token = readToken();
        if (token == "<" && peek() != "/") { 
            Object[] input = parseList(); 
	    return input;  
        }
    	else if (peek() == "=") { 
	    readToken(); // the "="
	    return new Object[]{"=", token, readToken()}; 
    	} 
        // Check for a badly formatted document to prevent hanging. 
        else if (token == "EOF" || peek() == "EOF") { 
            return null; 
        }
    	return token; 
    } 

    private Object getAttributeTmp(Object[] a, String attribute) { 
        for(int i = 0; i < a.length; i++) { 
            if (a[i].getClass().isArray()) { 
                Object[] b = (Object[])a[i]; 
                if (b[0] == "=" && b[1] == attribute) {
		    //   System.out.println("found " +attribute+ " in " + a); 
		    return b[2]; 
                }
            }
        }
	//  System.out.println("no " +attribute+ " in " + a); 
    	return null; 
    }
    
    private String getAttribute(Object[] a, String attribute) { 
        return unquote(getAttributeTmp(a, attribute)); // All attributes must be quoted. 
    }
    
    private String unquote(Object o) { 
	if (o == null) { 
	    return null; 
	}
	Object[] a = (Object[])o; 
	return (String)a[1]; 
    } 
	
    private Vector children(Object[] a) { 
        Vector result = new Vector(); 
        for(int i = 1; i < a.length; i++) { 
            if (!a[i].getClass().isArray() || ((Object[])a[i])[0] != "=") {
		result.addElement(a[i]); 
            }
        } 
    	return result; 
    }

    private Object map(Object exp, String context) { 
        //System.out.println("Mapping: " + ArrayEnumerator.enumerationToString(exp)); 
	if (!exp.getClass().isArray()) { 
            return exp; 
        }
        Object[] input = (Object[])exp; 
        Object tag = input[0]; 

        if (tag == "OBJECT") { 
            haveReadObject = true; 
            Object argAttribute = getAttributeTmp(input, "VALUE"); 
            String methodAttribute = getAttribute(input, "METHOD"); 
            String classAttribute = getAttribute(input, "CLASS"); 
            
            String propertyAttribute = getAttribute(input, "PROPERTY"); 
            String propertyrefAttribute = getAttribute(input, "PROPERTYREF"); 
            
            String idAttribute = getAttribute(input, "ID"); 
            String idrefAttribute = getAttribute(input, "IDREF"); 
            
            Object instance = null; 
            Object reciever = context; 
            
            if (methodAttribute == null && 
                idrefAttribute == null && 
                propertyrefAttribute == null && 
                classAttribute != null) {  
                methodAttribute = "new"; 
            } 
            
            Vector ch = children(input); 
            if (idrefAttribute != null) { 
                instance = idrefAttribute; 
                reciever = idrefAttribute;
            } 
            if (argAttribute != null && methodAttribute == null) { 
                instance = argAttribute; 
            } 
            if (classAttribute != null) { 
                reciever = classAttribute;
            } 
            if (propertyrefAttribute != null) { 
                instance = new Object[]{".", reciever, propertyrefAttribute}; 
            } 
            if (methodAttribute != null) { 
                // System.out.println("Method attribute: " + methodAttribute);
                Object[] combination = null; 
                
                Vector args = new Vector(); 
                if (argAttribute != null) { // Single string argument. 
                    args.addElement(argAttribute); 
                } 
                /* else */ 
                { 
                    for(int i = 0; i < ch.size(); i++) { 
                        Object[] child = (Object[])ch.elementAt(i); 
                        // If children are declared neither as properties 
                        // nor methods they are assumed to be arguments to 
                        // the enclosing method ... 
                        boolean isArg = getAttribute(child, "ARGUMENT") != null;                        
                        isArg = isArg || getAttribute(child, "PROPERTY") == null && 
                                         getAttribute(child, "METHOD") == null && 
                                         (getAttribute(child, "PROPERTYREF") == null || getAttribute(child, "CLASS") != null);  
                                         
                        if (isArg) { 
                            ch.removeElementAt(i); 
                            i = i-1; 
                            args.addElement(map(child, context)); 
                        }
                    } 
                    int nArgs = args.size(); 
                    combination = new Object[nArgs + 1]; 
                    combination[0] = methodAttribute; 
                    for (int i = 0; i < nArgs; i++) { 
                        combination[i+1] = args.elementAt(i); // map(args.elementAt(i), context); 
                    }
                }
                instance = new Object[]{".", reciever, combination}; 
            } 
            if (propertyAttribute != null) { 
                Object getter = new Object[]{".", context, propertyAttribute}; 
                instance = new Object[]{"=", getter, instance}; 
            } 
            if (idAttribute != null) { 
                instance = new Object[]{"=", idAttribute, instance}; 
            } 
            if (ch.size() > 0) { 
                // If there's no ID for this instance, make one up. 
                if (idAttribute == null) { 
                    idAttribute = "V" + localVariableCount++; 
                    instance = new Object[]{"=", idAttribute, instance}; 
                }
                ch.insertElementAt("block", 0); 
                ch.insertElementAt(instance, 1); 
                for (int i = 2; i < ch.size(); i++) { 
                    ch.setElementAt(map(ch.elementAt(i), idAttribute), i); 
                }
                ch.addElement(idAttribute); 
                return ArrayEnumerator.enumerationToArray(ch.elements()); 
            }
            else { 
                return instance; 
            }
        } 
        if (tag == "CLASS") { 
            Object id = getAttribute(input, "ID"); 
            Object name = getAttributeTmp(input, "NAME"); 
            return new Object[]{"=", id, 
                       new Object[]{".", "Class", new Object[]{"forName", name}}}; 
        } 
        System.err.println("Unrecognised tag: \"" + tag + "\" in: " + ArrayEnumerator.enumerationToString(exp)); 
        return input; 
    }
}



