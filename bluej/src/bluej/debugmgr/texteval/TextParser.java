package bluej.debugmgr.texteval;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.TokenStreamHiddenTokenFilter;
import antlr.collections.AST;
import bluej.Config;
import bluej.debugger.gentype.*;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.parser.ast.gen.JavaLexer;
import bluej.parser.ast.gen.JavaRecognizer;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.utility.JavaReflective;
import bluej.utility.JavaUtils;

/**
 * Parsing routines for the code pad.
 *  
 * @author Davin McCall
 * @version $Id: TextParser.java 3075 2004-11-09 00:10:18Z davmac $
 */
public class TextParser
{
    private ClassLoader classLoader;
    private String packageScope;  // evaluation package
    private ObjectBench objectBench;

    private static JavaUtils jutils = JavaUtils.getJavaUtils();
    private static boolean java15 = Config.isJava15();
    
    /**
     * TextParser constructor. Defines the class loader and package scope
     * for evaluation.
     */
    TextParser(ClassLoader classLoader, String packageScope, ObjectBench ob)
    {
        this.classLoader = classLoader;
        this.packageScope = packageScope;
        this.objectBench = ob;
    }
    
    /**
     * Attempt to load, by its unqualified name,  a class which might be in the
     * current package or which might be in java.lang.
     */
    private Class loadUnqualifiedClass(String className) throws ClassNotFoundException
    {
        boolean qualified = false;
        int index = 0;
        
        // It's an unqualified name - try package scope
        try {
            if (packageScope.length() != 0)
                return classLoader.loadClass(packageScope + "." + className);
            else
                return classLoader.loadClass(className);
        }
        catch(ClassNotFoundException cnfe) {}
        
        // Finally, try java.lang
        return classLoader.loadClass("java.lang." + className);
    }
    
    /**
     * Extract the type from a node. The type is in the form of a qualified
     * or unqualified class name, with optional type parameters.
     * 
     * @param node   The node representing the type
     * 
     * @throws RecognitionException, SemanticException
     */
    private GenType getType(AST node) throws RecognitionException, SemanticException
    {        
        if (node.getType() == JavaTokenTypes.IDENT) {
            // A class name
            // the children are the type parameters
            List params = getTParams(node.getFirstChild());
            
            try {
                Class c = loadUnqualifiedClass(node.getText());
                Reflective r = new JavaReflective(c);
                return new GenTypeClass(r, params);
            }
            catch(ClassNotFoundException cnfe) {
                throw new SemanticException();
            }
        }
        else if (node.getType() == JavaTokenTypes.DOT) {
            // The children nodes are: the qualified class name, and then
            // the type arguments
            AST packageNode = node.getFirstChild();
            PackageOrClass porc = getPackageOrClass(packageNode);
            
            // String packagen = combineDotNames(packageNode, '.');

            AST classNode = packageNode.getNextSibling();
            List params = getTParams(classNode.getNextSibling());

            PackageOrClass nodePorC = porc.getPackageOrClassMember(classNode.getText());
            GenTypeClass nodeClass = (GenTypeClass) nodePorC.getType();
            return new GenTypeClass(nodeClass.getReflective(), params);
        }
        else throw new RecognitionException();
    }
    
    /**
     * Get a sequence of type parameters.
     * 
     * @param node  The node representing the first tpar (or null).
     * @return      A list of GenType representing the type parameters
     * @throws RecognitionException
     * @throws SemanticException
     */
    private List getTParams(AST node) throws RecognitionException, SemanticException
    {
        List params = new LinkedList();
        
        // get the type parameters
        while(node != null && node.getType() == JavaTokenTypes.TYPE_ARGUMENT) {
            GenType tparType = getType(node.getFirstChild());
            params.add(tparType);
            node = node.getNextSibling();
        }
        
        return params;
    }
    
    /**
     * Return a string represenetation of a node which represents a dot-name
     * (eg "a", "a.b", "a.b.c" etc). A custom seperator can be used.
     * 
     * @param node    The node representing the dot-name
     * @param seperator  The seperator to use in the returned string
     * @return        The string representation of the node
     * @throws RecognitionException
     */
    String combineDotNames(AST node, char seperator) throws RecognitionException
    {
        if (node.getType() == JavaTokenTypes.IDENT)
            return node.getText();
        else if (node.getType() == JavaTokenTypes.DOT) {
            AST fchild = node.getFirstChild();
            AST nchild = fchild.getNextSibling();
            if (nchild.getType() != JavaTokenTypes.IDENT)
                throw new RecognitionException();
            return combineDotNames(fchild, seperator) + seperator + nchild.getText(); 
        }
        else
            throw new RecognitionException();
    }
    
    /**
     * Get the type from a node which must by context be an inner class type.
     * @param node   The node representing the type
     * @param outer  The containing type
     * *
     * @throws SemanticException
     * @throws RecognitionException
     */
    GenType getInnerType(AST node, Reflective outer) throws SemanticException, RecognitionException
    {
        if (node.getType() == JavaTokenTypes.IDENT) {
            // A simple name<params> expression
            List params = getTParams(node.getFirstChild());
            
            String name = outer.getName() + '$' + node.getText();
            try {
                Class theClass = classLoader.loadClass(name);
                Reflective r = new JavaReflective(theClass);
                return new GenTypeClass(r, params);
            }
            catch (ClassNotFoundException cnfe) {
                throw new SemanticException();
            }
        }
        else if (node.getType() == JavaTokenTypes.DOT) {
            // A name.name<params> style expression
            // The children nodes are: the qualified class name, and then
            // the type arguments
            AST packageNode = node.getFirstChild();
            String dotnames = combineDotNames(packageNode, '$');

            AST classNode = packageNode.getNextSibling();
            List params = getTParams(classNode.getNextSibling());

            String name = outer.getName() + '$' + dotnames + '$' + node.getText();
            try {
                Class c = classLoader.loadClass(name);
                Reflective r = new JavaReflective(c);
                return new GenTypeClass(r, params);
            }
            catch(ClassNotFoundException cnfe) {
                throw new SemanticException();
            }
            
        }
        else
            throw new RecognitionException();
    }
    
    /**
     * Parse a node as an entity (which could be a package, class or value).
     * @throws SemanticException
     * @throws RecognitionException
     */
    Entity getEntity(AST node) throws SemanticException, RecognitionException
    {
        // simple case first:
        if (node.getType() == JavaTokenTypes.IDENT) {
            // Treat it first as a variable, then a type, then as a package.
            String nodeText = node.getText();
            ObjectWrapper ow = objectBench.getObject(nodeText);
            if (ow != null)
                return new ValueEntity(ow.getGenType());
            
            try {
                Class c = loadUnqualifiedClass(nodeText);
                return new ClassEntity(c);
            }
            catch (ClassNotFoundException cnfe) {
                return new PackageEntity(nodeText);
            }
        }
        
        // A dot-node in the form xxx.identifier:
        if (node.getType() == JavaTokenTypes.DOT) {
            AST firstChild = node.getFirstChild();
            AST secondChild = firstChild.getNextSibling();
            if (secondChild.getType() == JavaTokenTypes.IDENT) {
                Entity firstpart = getEntity(firstChild);
                return firstpart.getSubentity(secondChild.getText());
            }
            // Don't worry about xxx.super, it shouldn't be used at this
            // level.
        }
        
        // Anything else must be an expression, therefore a value:
        GenType exprType = getExpressionType(node);
        return new ValueEntity(exprType);
    }
    
    /**
     * Get an entity which by context must be either a package or class.
     * *
     * @throws SemanticException
     */
    private PackageOrClass getPackageOrClass(AST node) throws SemanticException
    {
        // simple case first:
        if (node.getType() == JavaTokenTypes.IDENT) {
            // Treat it first as a type, then as a package.
            String nodeText = node.getText();
            
            try {
                Class c = loadUnqualifiedClass(nodeText);
                return new ClassEntity(c);
            }
            catch (ClassNotFoundException cnfe) {
                return new PackageEntity(nodeText);
            }
        }
        
        // A dot-node in the form xxx.identifier:
        if (node.getType() == JavaTokenTypes.DOT) {
            AST firstChild = node.getFirstChild();
            AST secondChild = firstChild.getNextSibling();
            if (secondChild.getType() == JavaTokenTypes.IDENT) {
                PackageOrClass firstpart = getPackageOrClass(firstChild);
                return firstpart.getPackageOrClassMember(secondChild.getText());
            }
        }
        
        throw new SemanticException();
    }
    
    /**
     * Get an expression list node as an array of GenType
     * 
     * @throws SemanticException
     * @throws RecognitionException
     */
    private GenType [] getExpressionList(AST node) throws SemanticException, RecognitionException
    {
        int num = node.getNumberOfChildren();
        GenType [] r = new GenType[num];
        AST child = node.getFirstChild();
        
        // loop through the child nodes
        for (int i = 0; i < num; i++) {
            r[i] = getExpressionType(child);
            child = child.getNextSibling();
        }
        return r;
    }
    
    /**
     * Check whether a particular method is callable with particular
     * parameters. If so return information about how specific the call is.
     * If the parameters cannot be applied to this method, return null.
     * 
     * @param targetType   The type of object/class to which the method is
     *                     being applied
     * @param tpars     The explicitly specified type parameters used in the
     *                  invocation of a generic method
     * @param m       The method to check
     * @param args    The types of the arguments supplied to the method
     * @return   A record with information about the method call
     * @throws RecognitionException
     */
    private MethodCallDesc isMethodApplicable(GenTypeClass targetType, Map tpars, Method m, GenType [] args)
        throws RecognitionException
    {
        // TODO support varargs, autoboxing/unboxing, generic methods
        // the rule for autoboxing seems to be, if two methods are applicable
        // but one requires autoboxing and the other doesn't, choose the one
        // that doesn't. Otherwise amibiguity.
        
        // First check that at least the number of parameters is correct. If
        // type parameters are explicitly stated, also check that their number
        // is correct.
        GenType [] mparams = JavaUtils.getJavaUtils().getParamGenTypes(m, targetType.isRaw());
        if (mparams.length != args.length)
            return null;
        List tparams = JavaUtils.getJavaUtils().getTypeParams(m);
        if (! tpars.isEmpty() && tpars.size() != tparams.size())
            return null;
        // Map methodTPars = JavaUtils.TParamsToMap(tparams);
        
        // at the moment, doesn't handle generic methods.
        if (! tparams.isEmpty())
            throw new RecognitionException();
        
        // Get a map of type parameter names to types from the target type,
        // but remove mappings for type parameter names which also occur as
        // part of the generic method declaration (as these override those
        // from the class declaration)
        Map targetTpars = targetType.getMap();
        if (targetTpars != null) {
            Iterator i = tparams.iterator();
            while (i.hasNext())
                targetTpars.remove(((GenTypeDeclTpar) i.next()).getTparName());
        }
        
        // For each argument, must check the compatibility of the supplied
        // parameter type with the argument type; and if neither the formal
        // parameter or supplied argument are raw, then must check generic
        // contract as well.
        
        for (int i = 0; i < args.length; i++) {
            GenType formalArg = mparams[i];
            GenType givenParam = args[i];
            
            // If type arguments specified, use those. Also bring in class
            // type parameters.
            formalArg = formalArg.mapTparsToTypes(targetTpars);
            formalArg = formalArg.mapTparsToTypes(tpars);
            
            // check if the given parameter doesn't match the formal argument
            if (! formalArg.isAssignableFrom(givenParam))
                return null;
        }
        
        GenType rType = jutils.getReturnType(m).mapTparsToTypes(targetType.getMap());
        return new MethodCallDesc(m, Arrays.asList(mparams), false, false, rType);
    }
    
    /**
     * Get the return type of a method call expression. This is quite
     * complicated; see JLS section 15.12
     *
     * @throws RecognitionException
     * @throws SemanticException
     */
    private GenType getMethodCallReturnType(AST node) throws RecognitionException, SemanticException
    {
        // For a method call node, the first child is the method name
        // (possibly a dot-name) and the second is an ELIST.
        //
        // In the case of the method name being a dot-name, it may also
        // be a generic type. In this case the children of the dot-node
        // are:
        //
        // <object-expression> <type-arg-1> <type-arg-2> .... <methodname>
        //
        // Where <object-expression> may actually be a type (ie. invoking
        // a static method).
        
        JavaUtils jutils = JavaUtils.getJavaUtils();
        
        AST firstArg = node.getFirstChild();
        AST secondArg = firstArg.getNextSibling();
        if (secondArg.getType() != JavaTokenTypes.ELIST)
            throw new RecognitionException();
        
        // we don't handle method calls where the object/class is not
        // specified...
        // that would be calling a shell class method. The user shouldn't
        // do that.
        
        if (firstArg.getType() == JavaTokenTypes.DOT) {
            AST targetNode = firstArg.getFirstChild();
            Entity callTarget = getEntity(targetNode);
            GenType targetType = callTarget.getType();
                            
            // now get method name, and argument types;
            List typeArgs = new ArrayList(5);
            GenType [] argumentTypes = getExpressionList(secondArg);
            String methodName = null;
            AST searchNode = targetNode.getNextSibling();
            
            // get the type arguments and method name
            while (searchNode != null) {
                int nodeType = searchNode.getType();
                // type argument?
                if (nodeType == JavaTokenTypes.TYPE_ARGUMENT) {
                    GenType taType = getType(searchNode.getFirstChild());
                    typeArgs.add(taType);
                }
                // method name?
                else if (nodeType == JavaTokenTypes.IDENT) {
                    methodName = searchNode.getText();
                    break;
                }
                else
                    break;
                
                searchNode = searchNode.getNextSibling();
            }
            
            // If no method name, this doesn't seem to be valid grammar
            if (methodName == null)
                throw new RecognitionException();
            
            // match the call to a method:
            // TODO it's possible to call a method on an object of
            // unknown (wildcard) type
            if (targetType instanceof GenTypeClass) {
                GenTypeClass targetClass = (GenTypeClass) targetType;
                try {
                    Class c = classLoader.loadClass(targetClass.rawName());
                    Method [] methods = c.getMethods();
                        
                    ArrayList suitableMethods = new ArrayList();
                    
                    // Find methods that are applicable, and
                    // accessible. See JLS 15.12.2.1.
                    // The JLS doesn't cover some 1.5 features though,
                    // such as varargs, generic methods/parameters, etc,
                    // so we take a best-guess approach.
                        
                    for (int i = 0; i < methods.length; i++) {
                        // ignore bridge methods
                        if (jutils.isBridge(methods[i]))
                            continue;
                        
                        // check method name
                        if (methods[i].getName().equals(methodName)) {
                            // map target type to declaring type
                            Class declClass = methods[i].getDeclaringClass();
                            Map m = targetClass.mapToSuper(declClass.getName());
                            
                            // check that the method is applicable (and under
                            // what constraints)
                            
                            // TODO should pass a map constructed from typeArgs
                            // as the second parameter here, when support for
                            // generic methods is added.
                            MethodCallDesc mcd = isMethodApplicable(targetClass, new HashMap(), methods[i], argumentTypes);
                            
                            // Iterate through the current candidates, and:
                            // - replace one or more of them with this one
                            //   (this one is more precise)
                            //   OR
                            // - add this one (no more or less precise than
                            //   any other candidates)
                            //   OR
                            // - discard this one (less precise than another)

                            if (mcd != null) {
                                boolean replaced = false;
                                for (int j = 0; j < suitableMethods.size(); j++) {
                                    //suitableMethods.add(methods[i]);
                                    MethodCallDesc mc = (MethodCallDesc) suitableMethods.get(j);
                                    int compare = mcd.compareSpecificity(mc);
                                    if (compare == 1) {
                                        // this method is more specific
                                        suitableMethods.remove(j);
                                        j--;
                                    }
                                    else if (compare == -1) {
                                        // other method is more specific
                                        replaced = true;
                                        break;
                                    }
                                }
                                
                                if (! replaced)
                                    suitableMethods.add(mcd);
                            }
                        }
                    }

                    if (suitableMethods.size() == 1) {
                        MethodCallDesc mcd = (MethodCallDesc) suitableMethods.get(0);
                        return mcd.retType;
                    }
                    else
                        throw new SemanticException();
                }
                catch (ClassNotFoundException cnfe) {
                    return null; // shouldn't happen
                }
            }
            
            // method call on a non-class type
            throw new SemanticException();
        }
        
        // don't try and handle unqualified method calls
        return null;
    }
    
    /**
     * Get the type from a TYPE node (normally found in a typecast). The node
     * need not actually be a TYPE node, but must conform to the same
     * structure.
     * 
     * @param node the node containing the type
     * @return
     * @throws RecognitionException
     * @throws SemanticException
     */
    GenType getTypeFromTypeNode(AST node) throws RecognitionException, SemanticException
    {
        AST firstChild = node.getFirstChild();
        GenType baseType;
        
        switch (firstChild.getType()) {
            case JavaTokenTypes.LITERAL_char:
                baseType = new GenTypeChar();
                break;
            case JavaTokenTypes.LITERAL_byte:
                baseType = new GenTypeByte();
                break;
            case JavaTokenTypes.LITERAL_boolean:
                baseType = new GenTypeBool();
                break;
            case JavaTokenTypes.LITERAL_short:
                baseType = new GenTypeShort();
                break;
            case JavaTokenTypes.LITERAL_int:
                baseType = new GenTypeInt();
                break;
            case JavaTokenTypes.LITERAL_long:
                baseType = new GenTypeLong();
                break;
            case JavaTokenTypes.LITERAL_float:
                baseType = new GenTypeFloat();
                break;
            case JavaTokenTypes.LITERAL_double:
                baseType = new GenTypeDouble();
                break;
            default:
                // not a primitive type
                baseType = getType(firstChild);
        }
        
        // check for array declarators
        AST arrayNode = firstChild.getNextSibling();
        while (arrayNode != null && arrayNode.getType() == JavaTokenTypes.ARRAY_DECLARATOR) {
            baseType = new GenArray(baseType);
            arrayNode = arrayNode.getFirstChild();
        }
        
        return baseType;
    }
    
    /**
     * Unbox a type, if it is a class type which represents a primitive type
     * in object form (eg. java.lang.Integer).<p>
     * 
     * Other class types are returned unchanged.<p>
     * 
     * To determine whether unboxing occurred, compare the result with the
     * object which was passed in. (The same object will be returned if no
     * unboxing took place).
     * 
     * @param b  The type to unbox
     * @return  The unboxed type
     */
    private GenType unBox(GenType b)
    {
        if (b instanceof GenTypeClass) {
            GenTypeClass c = (GenTypeClass) b;
            String cName = c.rawName();
            if (c.equals("java.lang.Integer"))
                return new GenTypeInt();
            else if (c.equals("java.lang.Long"))
                return new GenTypeLong();
            else if (c.equals("java.lang.Short"))
                return new GenTypeShort();
            else if (c.equals("java.lang.Byte"))
                return new GenTypeByte();
            else if (c.equals("java.lang.Character"))
                return new GenTypeChar();
            else if (c.equals("java.lang.Float"))
                return new GenTypeFloat();
            else if (c.equals("java.lang.Double"))
                return new GenTypeDouble();
            else if (c.equals("java.lang.Boolean"))
                return new GenTypeBool();
            else
                return b;
        }
        else
            return b;
    }
    
    /**
     * Box a type, if it is a primitive type such as "int".<p>
     * 
     * Other types are returned unchanged.<p>
     * 
     * To determine whether boxing occurred, compare the result with the
     * object which was passed in. (The same object will be returned if no
     * boxing took place).
     * 
     * @param b  The type to box
     * @return  The boxed type
     */
    private GenType boxType(GenType u)
    {
        if (u instanceof GenTypePrimitive) {
            if (u instanceof GenTypeInt)
                return new GenTypeClass(new JavaReflective(Integer.class));
            else if (u instanceof GenTypeLong)
                return new GenTypeClass(new JavaReflective(Long.class));
            else if (u instanceof GenTypeShort)
                return new GenTypeClass(new JavaReflective(Short.class));
            else if (u instanceof GenTypeByte)
                return new GenTypeClass(new JavaReflective(Byte.class));
            else if (u instanceof GenTypeChar)
                return new GenTypeClass(new JavaReflective(Character.class));
            else if (u instanceof GenTypeFloat)
                return new GenTypeClass(new JavaReflective(Float.class));
            else if (u instanceof GenTypeDouble)
                return new GenTypeClass(new JavaReflective(Double.class));
            else if (u instanceof GenTypeBool)
                return new GenTypeClass(new JavaReflective(Boolean.class));
            else
                return u;
        }
        else
            return u;
    }
    
    /**
     * Conditionally box a type. The type is only boxed if the boolean flag
     * passed in the second parameter is true.<p>
     * 
     * This is a helper method to improve readability.<p>
     * 
     * @see TextParser#boxType(GenType)
     * 
     * @param u    The type to box
     * @param box  The flag indicating whether boxing should occur
     * @return
     */
    private GenType maybeBox(GenType u, boolean box)
    {
        if (box)
            return boxType(u);
        else
            return u;
    }
    
    /**
     * Get the (static) result type of some expression.
     * 
     * @param node  The node representing the expression
     * @return The result type of the expression
     * 
     * @throws RecognitionException
     * @throws SemanticException
     */
    GenType getExpressionType(AST node) throws RecognitionException, SemanticException
    {        
        // Expressions are represented as an EXPR node but sometimes part of
        // an expression can consist of a complete expression which is not
        // itself headed by an EXPR node. This method can handle both cases.
        AST fcNode;
        if (node.getType() == JavaTokenTypes.EXPR)
            fcNode = node.getFirstChild();
        else
            fcNode = node;
        
        
        switch (fcNode.getType()) {
            // "new xxxxx<>()"
            case JavaTokenTypes.LITERAL_new:
                return getTypeFromTypeNode(fcNode);
            
            // dot node
            case JavaTokenTypes.DOT:
            {
                AST firstDotArg = fcNode.getFirstChild();
                AST secondDotArg = firstDotArg.getNextSibling();
                
                // qualified new:  expression.new xxxxx<>()
                if (secondDotArg.getType() == JavaTokenTypes.LITERAL_new) {
                    // The class being instantiated needs to be resolved in terms
                    // of the type of the expression to the left.
                    GenType fpType = getExpressionType(firstDotArg);
                    
                    // now evaluate the qualified new in the context of the type
                    // of the expression
                    if (fpType instanceof GenTypeClass) {
                        GenType type = getInnerType(secondDotArg.getFirstChild(), ((GenTypeClass)fpType).getReflective());
                        return type;
                    }
                    
                    if (fpType == null)
                        return null;
                    else
                        throw new SemanticException();
                }
                
                // member field/type of some type
                if (secondDotArg.getType() == JavaTokenTypes.IDENT) {
                    
                    Entity entity = getEntity(firstDotArg);
                    entity = entity.getSubentity(secondDotArg.getText());
                    
                    return entity.getType();
                }
                
                // class literal
                if (secondDotArg.getType() == JavaTokenTypes.LITERAL_class) {
                    if (! Config.isJava15())
                        return new GenTypeClass(new JavaReflective(Class.class));
                    // TODO incomplete - return "Class<X>", not just "Class"
                    return new GenTypeClass(new JavaReflective(Class.class));
                }
                
                // not worrying about - .this, .super etc. They can't be used in
                // the code pad context.
            }
            
            // method call
            case JavaTokenTypes.METHOD_CALL:
                return getMethodCallReturnType(fcNode);
            
            // Object bench object
            case JavaTokenTypes.IDENT:
            {
                String objectName = fcNode.getText();
                ObjectWrapper ow = objectBench.getObject(objectName);
                
                if (ow == null)
                    throw new SemanticException();
                
                return ow.getGenType();
            }
            
            // type cast.
            case JavaTokenTypes.TYPECAST:
                return getTypeFromTypeNode(fcNode.getFirstChild());
            
            // array element access
            case JavaTokenTypes.INDEX_OP:
            {
                GenType t = getExpressionType(fcNode.getFirstChild());
                GenType componentType = t.getArrayComponent();
                if (componentType != null)
                    return componentType;
                else
                    throw new SemanticException();
            }
            
            // various literal types.
            
            case JavaTokenTypes.STRING_LITERAL:
                return new GenTypeClass(new JavaReflective(String.class));
            
            case JavaTokenTypes.CHAR_LITERAL:
                return new GenTypeChar();
            
            case JavaTokenTypes.NUM_INT:
                return new GenTypeInt();
            
            case JavaTokenTypes.NUM_FLOAT:
                return new GenTypeFloat();
            
            case JavaTokenTypes.NUM_DOUBLE:
                return new GenTypeDouble();
            
            case JavaTokenTypes.LITERAL_true:
            case JavaTokenTypes.LITERAL_false:
            case JavaTokenTypes.LITERAL_instanceof:
                return new GenTypeBool();
            
            // boolean operators
            
            case JavaTokenTypes.LT:
            case JavaTokenTypes.LE:
            case JavaTokenTypes.GT:
            case JavaTokenTypes.GE:
            case JavaTokenTypes.EQUAL:
            case JavaTokenTypes.NOT_EQUAL:
            case JavaTokenTypes.LNOT:
            case JavaTokenTypes.LAND:
            case JavaTokenTypes.LOR:
                return new GenTypeBool();
            
            // shift operators. The result type is the same as the (unboxed)
            // first operand type.
            
            case JavaTokenTypes.SL:
            case JavaTokenTypes.SR:
            case JavaTokenTypes.BSR:
                return unBox(getExpressionType(fcNode.getFirstChild()));
            
            // assignment operators. The result type is the same as the first
            // operand type.
            
            case JavaTokenTypes.SL_ASSIGN:
            case JavaTokenTypes.SR_ASSIGN:
            case JavaTokenTypes.BSR_ASSIGN:
            case JavaTokenTypes.PLUS_ASSIGN:
            case JavaTokenTypes.MINUS_ASSIGN:
            case JavaTokenTypes.STAR_ASSIGN:
            case JavaTokenTypes.DIV_ASSIGN:
            case JavaTokenTypes.MOD_ASSIGN:
            case JavaTokenTypes.BAND_ASSIGN:
            case JavaTokenTypes.BOR_ASSIGN:
            case JavaTokenTypes.BXOR_ASSIGN:
                return getExpressionType(fcNode.getFirstChild());

            // arithmetic operations, other binary ops
            case JavaTokenTypes.MINUS:
            case JavaTokenTypes.STAR:
            case JavaTokenTypes.DIV:
            case JavaTokenTypes.MOD:
            case JavaTokenTypes.BAND:
            case JavaTokenTypes.BOR:
            case JavaTokenTypes.BXOR:
            {
                AST leftNode = fcNode.getFirstChild();
                AST rightNode = leftNode.getNextSibling();
                GenType leftType = unBox(getExpressionType(leftNode));
                GenType rightType = unBox(getExpressionType(rightNode));
                if (leftType.isAssignableFrom(rightType))
                    return leftType;
                else
                    return rightType;
            }

            // plus needs special handling, as it is also the string
            // concatenation operator
            case JavaTokenTypes.PLUS:
            {
                AST leftNode = fcNode.getFirstChild();
                AST rightNode = leftNode.getNextSibling();
                GenType leftType = unBox(getExpressionType(leftNode));
                GenType rightType = unBox(getExpressionType(rightNode));
                if (leftType.isAssignableFrom(rightType) || leftType.toString().equals("java.lang.String"))
                    return leftType;
                else
                    return rightType;
            }
            
            // increment/decrement, binary not - unary operators
            case JavaTokenTypes.POST_INC:
            case JavaTokenTypes.POST_DEC:
            case JavaTokenTypes.INC:
            case JavaTokenTypes.DEC:
            case JavaTokenTypes.BNOT:
                return unBox(getExpressionType(fcNode.getFirstChild()));
            
            // ? : operator  (trinary operator)
            // This one is nasty. Even javac doesn't allow certain things that
            // it should, for instance:
            //    List<? extends Thread> lt = new LinkedList<Thread>();
            //    List<? extends Thread> lq = new LinkedList<Thread>();
            //    (true ? lt : lq).add(new Thread());  // ERROR(!)
            // In above case the javac result type is:
            //    List<capture of ? extends Object>.
            //
            // Also must handle autoboxing:
            //    true ? 3 : new Object()
            // result type is:
            //    Integer
            case JavaTokenTypes.QUESTION:
            {
                AST trueAlt = fcNode.getFirstChild().getNextSibling();
                AST falseAlt = trueAlt.getNextSibling();
                GenType trueAltType = getExpressionType(trueAlt);
                GenType falseAltType = getExpressionType(falseAlt);
                GenType trueUnboxed = unBox(trueAltType);
                GenType falseUnboxed = unBox(falseAltType);
                
                if (trueUnboxed.isPrimitive() && falseUnboxed.isPrimitive()) {
                    // Return the most precise. If both original types were
                    // boxed types, the result must be boxed. Which is stupid.
                    // But that's java for you :-).
                    boolean box = trueAltType != trueUnboxed && falseAltType != falseUnboxed;
                    if (trueAltType.isAssignableFrom(falseAltType))
                        return maybeBox(trueAltType, box);
                    else if (falseAltType.isAssignableFrom(trueAltType))
                        return maybeBox(falseAltType, box);
                }
                
                // Not a primitive ? :, so box both:
                trueAltType = boxType(trueAltType);
                falseAltType = boxType(falseAltType);
                
                // TODO this must work for GenTypeParameterizable, not just GenTypeSolid
                if (trueAltType instanceof GenTypeSolid && falseAltType instanceof GenTypeSolid) {
                    GenTypeClass rt = GenTypeSolid.gcd((GenTypeSolid) trueAltType, (GenTypeSolid) falseAltType);
                    return rt;
                }
            }
            
            default:
        }
        
        // Debug.message("Unknown type: " + fcNode.getType());
        return null;
    }
    
    /**
     * Obtain a parser which can be used to parse a command string.
     * @param s  the string to parse
     */
    static JavaRecognizer getParser(String s)
    {
        StringReader r = new StringReader(s);
        
        // create a scanner that reads from the input stream passed to us
        JavaLexer lexer = new JavaLexer(r);
        lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");

        // with a tab size of one, the rows and column numbers that
        // locatable token returns are model coordinates in the editor
        // (not view coordinates)
        // ie a keyword may appear to start at column 14 because of tabs
        // but in the actual document model its really at column 4
        // so we set our tabsize to 1 so that it maps directly to the
        // document model
        lexer.setTabSize(1);

        // create a filter to handle our comments
        TokenStreamHiddenTokenFilter filter;
        filter = new TokenStreamHiddenTokenFilter(lexer);
        filter.hide(JavaRecognizer.SL_COMMENT);
        filter.hide(JavaRecognizer.ML_COMMENT);

        // create a parser that reads from the scanner
        JavaRecognizer parser = new JavaRecognizer(filter);
        parser.setASTNodeClass("bluej.parser.ast.LocatableAST");
        
        return parser;
    }

    // need to return two seperate pieces of info: is it an expression or
    // is it a statement, and, if an expression, what is the return type if known?
    // return null if a statement (or otherwise unparseable), "" if an expression
    // but type not known, or "sometype" if the type is known. 
    String parseCommand(String command)
    {
        boolean parsedOk = false;
        AST rootAST;

        // Multiple semi-colon to avoid hitting "end-of-file" which breaks
        // parse
        JavaRecognizer parser = getParser("{" + command + "};;;;;");
        
        // start parsing at the classBlock rule
        try {
            parser.compoundStatement();
            rootAST = parser.getAST();
            parsedOk = true;
            // Debug.message("It seems to be a statement.");
        }
        catch(RecognitionException re) { }
        catch(TokenStreamException tse) { }
      
        if (! parsedOk) {
            // It might just be an expression. Multiple semi-colon to ensure
            // end-of-file not hit (causes parse failure).
            parser = getParser(command + ";;;;;");
            
            try {
                parser.expression();
                rootAST = parser.getAST();
                parsedOk = true;
                // Debug.message("It seems to be an expression:");
                // bluej.utility.Debug.message(rootAST.toStringTree());
                // root type always == 28, which is EXPR
                // AST childAst = rootAST.getFirstChild();
                GenType t = getExpressionType(rootAST);
                // Debug.message("got type = " + t);
                if (t == null)
                    return "";
                else
                    return t.toString();
            }
            catch(RecognitionException re) { }
            catch(SemanticException se) { }
            catch(TokenStreamException tse) { }
            return "";
        }
        return null;
    }

    /**
     * A simple structure to hold various information about a method call.
     * 
     * @author Davin McCall
     */
    private class MethodCallDesc
    {
        public Method method;
        public List argTypes; // list of GenType
        public boolean vararg;   // is a vararg call
        public boolean autoboxing; // requires autoboxing
        public GenType retType; // effective return type
        
        /**
         * Constructor for MethodCallDesc.
         * 
         * @param m   The method being called
         * @param argTypes   The effective types of the arguments, as an
         *                   ordered list
         * @param vararg      Whether the method is being called as a vararg
         *                    method
         * @param autoboxing  Whether autoboxing is required for parameters
         * @param retType     The effective return type
         */
        public MethodCallDesc(Method m, List argTypes, boolean vararg, boolean autoboxing, GenType retType)
        {
            this.method = m;
            this.argTypes = argTypes;
            this.vararg = vararg;
            this.autoboxing = autoboxing;
            this.retType = retType;
        }
        
        /**
         * Find out which (if any) method call is more specific than the other.
         * Both calls must be valid calls to the same method with the same
         * number of parameters.
         * 
         * @param other  The method to compare with
         * @return 1 if this method is more specific;
         *         -1 if the other method is more specific;
         *         0 if neither method is more specific than the other.
         * 
         * See JLS 15.12.2.2
         */
        public int compareSpecificity(MethodCallDesc other)
        {
            if (other.vararg && ! vararg)
                return 1; // we are more specific
            if (! other.vararg && vararg)
                return -1; // we are less specific
            
            Iterator i = argTypes.iterator();
            Iterator j = other.argTypes.iterator();
            int upCount = 0;
            int downCount = 0;
            
            while (i.hasNext()) {
                GenType myArg = (GenType) i.next();
                GenType otherArg = (GenType) j.next();
                
                if (myArg.isAssignableFrom(otherArg)) {
                    if (! otherArg.isAssignableFrom(myArg))
                        upCount++;
                }
                else if (otherArg.isAssignableFrom(myArg))
                    downCount++;
            }
            
            if (upCount > 0 && downCount == 0)
                return -1; // other is more specific
            else if (downCount > 0 && upCount == 0)
                return 1;  // other is less specific
            
            // finally, if one method is declared in an interface and the
            // other in a class, the one from the class is "more specific".
            // TODO this should only be true if the method signatures are
            // actually the same.
            if (method.getDeclaringClass().isInterface() && ! other.method.getDeclaringClass().isInterface())
                return -1;
            else if (! method.getDeclaringClass().isInterface() && other.method.getDeclaringClass().isInterface())
                return 1;
            else
                return 0;
        }
    }
    
    /**
     * An exception class used to indicate that a semantic occur was detected
     * in the code being parsed.
     * 
     * @author Davin McCall
     */
    class SemanticException extends Exception
    {
        // Nothing to see here.
    }
    
    /**
     * An entity which occurs in the source in a place where context alone
     * cannot determine whether the entity is a package, class or value.
     * 
     * @author Davin McCall
     */
    abstract class Entity
    {
        static final int ENTITY_PACKAGE = 0;
        static final int ENTITY_CLASS = 1;
        static final int ENTITY_VALUE = 2;
        
        abstract int entityType();
        
        // getType throws SemanticException if the entity doesn't have an
        // assosciated type (for instance a package)
        abstract GenType getType() throws SemanticException;
        
        abstract Entity getSubentity(String name) throws SemanticException;
    }
    
    abstract class PackageOrClass extends Entity
    {
        // same as getSubentity, but cannot yield a value
        abstract PackageOrClass getPackageOrClassMember(String name) throws SemanticException;
    }
    
    class PackageEntity extends PackageOrClass
    {
        String packageName;
        
        PackageEntity(String pname)
        {
            packageName = pname;
        }
        
        int entityType()
        {
            return ENTITY_PACKAGE;
        }
        
        GenType getType() throws SemanticException
        {
            throw new SemanticException();
        }
        
        Entity getSubentity(String name) throws SemanticException
        {
            Class c;
            try {
                c = classLoader.loadClass(packageName + '.' + name);
                return new ClassEntity(c);
            }
            catch (ClassNotFoundException cnfe) {
                return new PackageEntity(packageName + '.' + name);
            }
        }
        
        PackageOrClass getPackageOrClassMember(String name) throws SemanticException
        {
            return (PackageOrClass) getSubentity(name);
        }
        
        public String toString()
        {
            return "package: " + packageName;
        }
    }
    
    class ClassEntity extends PackageOrClass
    {
        Class thisClass;
        
        ClassEntity(Class c)
        {
            thisClass = c;
        }
        
        int entityType()
        {
            return ENTITY_CLASS;
        }
        
        GenType getType() throws SemanticException
        {
            return new GenTypeClass(new JavaReflective(thisClass));
        }
        
        Entity getSubentity(String name) throws SemanticException
        {
            // subentity of a class could be a member type or field
            // Is it a field?
            Field f = null;
            try {
                f = thisClass.getField(name);
                GenType fieldType = JavaUtils.getJavaUtils().getFieldType(f);
                return new ValueEntity(fieldType);
            }
            catch (NoSuchFieldException nsfe) {}

            // Is it a member type?
            return getPackageOrClassMember(name);
        }
        
        PackageOrClass getPackageOrClassMember(String name) throws SemanticException
        {
            // Is it a member type?
            Class c;
            try {
                c = classLoader.loadClass(thisClass.getName() + '$' + name);
                return new ClassEntity(c);
            }
            catch (ClassNotFoundException cnfe) {
                // No more options - it must be an error
                throw new SemanticException();
            }
        }
    }
    
    class ValueEntity extends Entity
    {
        GenType type;
        
        ValueEntity(GenType type)
        {
            this.type = type;
        }
        
        int entityType()
        {
            return ENTITY_VALUE;
        }
        
        GenType getType()
        {
            return type;
        }
        
        Entity getSubentity(String name)
            throws SemanticException
        {
            // Should be a member field.
            if (!(type instanceof GenTypeClass))
                throw new SemanticException();

            // get the class part of our type
            GenTypeClass thisClass = (GenTypeClass) type;
            Reflective r = thisClass.getReflective();
            Class c;
            try {
                c = classLoader.loadClass(r.getName());
            }
            catch (ClassNotFoundException cnfe) {
                // shouldn't happen
                throw new SemanticException();
            }

            //  Try and find the field
            Field f = null;
            try {
                f = c.getField(name);
                // Map type parameters to declaring class
                Class declarer = f.getDeclaringClass();
                Map tparMap = thisClass.mapToSuper(declarer.getName());

                GenType fieldType;
                if (tparMap != null) {
                    // Not raw. Apply type parameters to field declaration.
                    fieldType = JavaUtils.getJavaUtils().getFieldType(f);
                    fieldType = fieldType.mapTparsToTypes(tparMap);
                }
                else
                    fieldType = JavaUtils.getJavaUtils().getRawFieldType(f);

                return new ValueEntity(fieldType);
            }
            catch (NoSuchFieldException nsfe) {
                throw new SemanticException();
            }
        }
    }
}
