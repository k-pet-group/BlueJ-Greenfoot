/*
 * @(#)XMLOutputStream.java	1.9 99/11/08
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
 * @version 1.9 11/08/99
 * @author Philip Milne
 */

public class XMLOutputStream extends DataOutputStream 
implements ObjectOutput {     
    String q = "\""; 
    private int indentation = 0; 
    private ExpressionGenerator generator; 
 
    private String indentString() { 
        String result = ""; 
        for(int i = 0; i < indentation; i++) { 
            result = result + "  "; // + "\t"; 
        }
        return result; 
    }
	
    public XMLOutputStream(OutputStream out) { 
        super(out); 
        generator = new ExpressionGenerator(); 
    }
    
    public void writeObject(Object o) throws IOException { 
        Object result = generator.generateExpressionFor(o); 
        
        if (indentation == 0) { 
            // writeString("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"); 
            writeString("<JAVA-OBJECT-ARCHIVE VERSION=\"0.1\">"); 
            indentation++; 
        } 
        
        outputClasses(result); 
        // System.out.println(ArrayEnumerator.enumerationToString2(result));
        output(result, "", null, true); 
    }     

    public void close() throws IOException { 
        indentation--; 
        writeString("</JAVA-OBJECT-ARCHIVE>"); 
        super.close(); 
    }


    private void writeString(String exp) throws IOException { 
        out.write(indentString().getBytes()); 
        out.write(exp.getBytes());     
        out.write(" \n".getBytes());     
    }
    
    private void escape(StringBuffer b, char c, char c2) { 
        for(int i = 0; i < b.length(); i++) { 
            if (b.charAt(i) == c) { 
                b.insert(i++,'\\');
                b.setCharAt(i++, c2);
            }
        } 
    }
    
    private String quote(Object x) { 
        String s = x.toString();  
        StringBuffer sb = new StringBuffer(x.toString()); 
        escape(sb, '"', '"'); 
        escape(sb, '\n', 'n'); 
        escape(sb, '\r', 'r'); 
        return q+sb+q;
    }

    // Duplicated in ExpressionGenerator. 
    private String unqualifiedClassName(Class type) { 
        String name = type.getName(); 
        if (!type.isArray()) {
            return name.substring(name.lastIndexOf('.')+1); 
        }
        else { 
            return unqualifiedClassName(type.getComponentType())+"Array"; 
        }
    }
    
    private Object outputClasses(Object exp) throws IOException { 
        if (!(exp instanceof Object[])) { 
            return exp; 
        } 
        Object[] a = (Object[])exp;
        Object f = (a.length < 1) ? null : a[0]; 
        if (a[0] == "." && a[1] == "Class") { 
            Object[] b = (Object[])a[2]; 
            if (b[0] == "forName") { 
                if (!b[1].getClass().isArray()) System.out.println("b " + ArrayEnumerator.enumerationToString2(b)); 
                Object name = ((Object[])b[1])[1]; 
                try { 
                    Class c = Class.forName((String)name); 
                    String variable = unqualifiedClassName(c); 
                    writeString("<CLASS ID=" + quote(variable) + " NAME=" + quote(name) + "/>"); 
                    return variable; 
                }
                catch (Throwable e) { 
                    e.printStackTrace(); 
                }
            }
        }
        for(int i = 0; i < a.length; i++) { 
            a[i] = outputClasses(a[i]); 
        }
        if (a[0] == "=" && a[1].equals(a[2])) { 
            return a[1]; 
        } 
        return exp; 
    }
    
    private void writeCombination(Object[] b, String attributes, String context, boolean close) throws IOException { 
        // System.out.println("writeCombination: " + ArrayEnumerator.enumerationToString2(b));
        if (b[0] == ".") { 
            // Reciever
            if (b[1] != context) { 
                // System.out.println("Context: " + context + " b[1]: " + b[1]); 
                attributes = attributes + " CLASS=" + quote(b[1]); 
            }               
                
            if (!b[2].getClass().isArray()) { 
                // The PROPERTYREF attribute, denotes a "getProperty" operation. 
                String closer = close ? "/>" : ">"; 
                attributes = attributes + " PROPERTYREF=" + quote(b[2]); 
                writeString("<OBJECT" + attributes + closer);
            } 
            else { 
                Object[] c = (Object[])b[2]; 
                // Method attribute. 
                if (c[0] != "new" && c[0] != "newInstance") { 
                    attributes = attributes + " METHOD=" + quote(c[0]); 
                }
                
                // Arguments. 
                boolean singleStringArg = false; 
                if (c.length == 2 && c[1].getClass().isArray()) { 
                    Object[] d = ((Object[])c[1]); 
                    if (d[0] == "\"") { 
                        attributes = attributes + " VALUE=" + quote(d[1]); 
                        singleStringArg = true; 
                    }
                }
                
                boolean hasArgs = c.length > 1 && !singleStringArg; 
                String closer = (close && !hasArgs) ? "/>" : ">"; 
                writeString("<OBJECT" + attributes + closer); 
                
                if (hasArgs) { 
                    indentation++; 
                    for(int i = 1; i < c.length; i++) { 
                        String argumentTag = ""; 
                        // Only add the ARGUMENT tag if the expression is ambiguous. 
                        if (c[i] != null && c[i].getClass().isArray()) { 
                            Object[] arg = (Object[])c[i]; 
                            if (arg[0] == "=" && arg[2].getClass().isArray()) { 
                                arg = (Object[])arg[2]; 
                            }
                            if (arg[0] == "." && arg[2].getClass().isArray()) { 
                                 Object[] combination = (Object[])arg[2]; 
                                 if (combination[0] != "new" && combination[0] != "newInstance") { 
                                     argumentTag = " ARGUMENT=" + quote(new Integer(i-1)); 
                                 }
                            }
                        } 
                        output(c[i], argumentTag, null, true); 
                    }
                    indentation--; 
                } 
                
                if (close && hasArgs) { 
                    writeString("</OBJECT>");
                } 
            }
        } 
        else { 
            System.err.println("Unknown invocation directive: \"" + b[0] + "\" in " + ArrayEnumerator.enumerationToString2(b));
            return; 
        }
    } 
    
    private String variableName(Object exp) { 
        if (!exp.getClass().isArray()) { 
            return null; 
        }
        Object[] a = (Object[])exp; 
        // In an expression of the form, a = d.foo(...), return "a". 
        if (a[0] == "=") { 
            Object name = a[1]; 
            if (name instanceof String) { 
                 return (String)name; 
            } 
            //  In an expession of the form, a.b = (c = d.foo(...)), return "c". 
            else { 
                return variableName(a[2]); 
            }
        }
        return null; 
    }
    
    private void output(Object exp, String attributes, String context, boolean close) throws IOException { 
        // System.out.println("output: " + ArrayEnumerator.enumerationToString2(exp));
        // "null" is the representation of null. 
        // null is an empty representation which, in the differencing algorithm 
        // means, that no code needs to be executed. In XML these two constructs 
        // seem to be the same ...
        if (exp == null || exp == "null") { 
            writeString("<OBJECT" + attributes + "/>"); 
            return; 
        }
        if (exp instanceof String) { 
            writeString("<OBJECT" + attributes + " IDREF=" + quote(exp) + "/>"); 
            return;                             
        }
        if (!(exp instanceof Object[])) { 
            System.err.println("Unknown primitive type: " + exp);
            return; 
        } 
        Object[] a = (Object[])exp;
        Object f = (a.length < 1) ? null : a[0]; 
        if (f == "\"") { 
            writeString("<OBJECT" + attributes + " VALUE=" + quote(a[1]) + "/>");
            return;
        }
        if (f == "=") { 
            // If the lhs is a property accessor, we annotate this expression 
            // with the PROPERTY attribute - denoting a "setProperty" operation. 
            if (a[1].getClass().isArray()) { 
                Object[] b = (Object[])a[1]; 
                if (b[0] == ".") { 
                    output(a[2], attributes + " PROPERTY=" + quote(b[2]), context, close); 
                    return; 
                } 
                else { 
                    System.err.println("Unrecognised lhs for \"=\" in: " + ArrayEnumerator.enumerationToString2(a));
                    return; 
                }
            }
            else if (a[2].getClass().isArray()) { 
                output(a[2], attributes + " ID=" + quote(a[1]), context, close); 
                return; 
            }
            else { 
                System.err.println("Duplicated variable: " + ArrayEnumerator.enumerationToString2(a));
                return; 
            }
        }
        if (f == "block") { 
            // Write the first statement out first, and leave the 
            // context open so that the rest of the statements may 
            // be evaluated in this context. 
            output(a[1], attributes, context, false);
            indentation++; 
            // Dig out the name of the context. 
            context = variableName(a[1]); 
            // Ignore the last statement. 
            for(int i = 2; i < a.length-1; i++) { 
                output(a[i], "", context, true); 
            }
            indentation--; 
            writeString("</OBJECT>"); 
            return; 
        }
        if (f == ".") { 
            writeCombination(a, attributes, context, close); 
            return; 
        }
        System.err.println("Warning: Unsupported construct: " + ArrayEnumerator.enumerationToString2(a)); 
        return; 
    }
}


