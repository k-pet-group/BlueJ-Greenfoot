package bluej.parser.ast;

import java.io.File;
import java.util.*;

import antlr.collections.*;
import antlr.*;

/**
 *
     Returns the name of the entity (class, interface, array class, primitive type,
     or void) represented by this Class object, as a String.

    If this class object represents a reference type that is not an array type
     then the binary name of the class is returned, as specified by the Java
     Language Specification, Second Edition. If this class object represents
      a primitive type or void, then the name returned is the name determined
       by the following table. The encoding of element type names is as follows:

 B            byte
 C            char
 D            double
 F            float
 I            int
 J            long
 Lclassname;  class or interface
 S            short
 Z            boolean
 V	    void
 

    If this class object represents a class of arrays, then the internal form of the
    name consists of the name of the element type as specified above, preceded by
    one or more "[" characters representing the depth of array nesting. Thus:

 (new Object[3]).getClass().getName()
 

    returns "[Ljava.lang.Object;" and:

 (new int[3][4][5][6][7][8][9]).getClass().getName()
 

    returns "[[[[[[[I". The class or interface name
    classname is given in fully qualified form as shown in the example above.

Returns:
the name of the class or interface represented by this object.
*/

/**
 * parser.setASTNOdeCLass(SymTabAST.class.getName());
 *
 * make sure you also call setTokenObjectClass for the lexer as well
 */
public class TypeInfoAST extends antlr.CommonAST
{
    private String typeText;        // used to build fully qualified type names
    protected Class theType = null;
    
    public TypeInfoAST()
    {
        super();
    }

    public TypeInfoAST(Token tok)
    {
        super(tok);
    }

    public void setTypeInfo(Class theType)
    {
        this.theType = theType;
    }

    public void setTypeInfo(TypeInfoAST tia)
    {
        this.theType = tia.theType;
        this.typeText = tia.typeText;
    }

    public void setTypeInfo(java.util.List typeList)
    {
        System.out.println("ELIST\n----");
        Iterator it = typeList.iterator();
        while(it.hasNext()) {
            System.out.println(it.next());
        }
    }

    public boolean hasTypeInfo()
    {
        return theType != null;
    }
     
    public String getTypeInfo()
    {
        if (hasTypeInfo())
            return theType.toString();
        else
            return "no type information";
    }

    public Class getTypeInfoClass()
    {
        return theType;
    }

    /*
     *
     */
    void setTypeText(String type)
    {
        this.typeText = type;
    }

    String getTypeText()
    {
        return typeText;
    }
       
  /**
   * initialized this node with input node
   * @param t
   * @return <code>void</code>
   */
    public void initialize(AST t)
    {
        super.initialize(t);
    }

  /**
   * initializes the node with input <code>Token</code>
   * @param t
   * @return <code>void</code>
   */
    public void initialize(Token t)
    {
        super.initialize(t);
    }

/* widening
    * byte to short, int, long, float, or double
    * short to int, long, float, or double
    * char to int, long, float, or double
    * int to long, float, or double
    * long to float or double
    * float to double 

   narrowing
   
       * byte to char
    * short to byte or char
    * char to byte or short
    * int to byte, short, or char
    * long to byte, short, char, or int
    * float to byte, short, char, int, or long
* double to byte, short, char, int, long, or float 
   */

    /**
     *
     */
    public boolean checkBasicType(Class type)
    {
        return type.isPrimitive();
    }
     
    /**
     * Apply binaryNumericPromotion to two types
     * JLS 5.6.2
     */
    public Class doBinaryNumericPromotion(Class ltype, Class rtype)
    {
        if (ltype.equals(double.class) || rtype.equals(double.class))
            return double.class;
            
        if (ltype.equals(float.class) || rtype.equals(float.class))
            return float.class;
        
        if (ltype.equals(long.class) || rtype.equals(long.class))
            return long.class;
            
        if (ltype.equals(byte.class) || ltype.equals(short.class) ||
             ltype.equals(char.class) || ltype.equals(int.class)) {
                if (rtype.equals(byte.class) || rtype.equals(short.class) ||
                     rtype.equals(char.class) || rtype.equals(int.class)) {
                        return int.class;
                }
        }
                
        throw new IllegalArgumentException("binaryNumericPromotion of non numeric type");       
    }
    
    public boolean checkCompatibleTypes(String ltype, String rtype)
    {
        // identity compatibility
        if (ltype.equals(rtype))
            return true;        
        
        return false;
    }
    
    public void performBinaryNumOp(TypeInfoAST l, TypeInfoAST r)
    {
        setTypeInfo(doBinaryNumericPromotion(l.getTypeInfoClass(), r.getTypeInfoClass()));
    }

    public void performBinaryBoolOp(TypeInfoAST l, TypeInfoAST r)
    {
        setTypeInfo(Boolean.TYPE);
    }
   
}
