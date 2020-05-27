/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2014,2015,2016,2017,2020  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.debugger.gentype.BadInheritanceChainException;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.GenTypeTpar;
import bluej.debugger.gentype.GenTypeWildcard;
import bluej.debugger.gentype.IntersectionType;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ValueCollection;
import bluej.debugmgr.codepad.DeclaredVar;
import bluej.parser.entity.ConstantFloatValue;
import bluej.parser.entity.ConstantIntValue;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.ValueEntity;
import bluej.utility.Debug;
import bluej.utility.JavaNames;
import bluej.utility.JavaReflective;
import bluej.utility.JavaUtils;

/**
 * Parsing routines for the code pad.<p>
 * 
 * This is pretty tricky stuff, we try to following the Java Language Specification
 * (JLS) where possible.
 *  
 * @author Davin McCall
 */
public class TextAnalyzer
{
    private EntityResolver parentResolver;
    private String packageScope;  // evaluation package
    private ValueCollection objectBench;

    private List<DeclaredVar> declVars; // variables declared in the parsed statement block
    private String amendedCommand;  // command string amended with initializations for
                                    // all variables
    
    private ImportsCollection imports;
    private String importCandidate; // any import candidates.
    
    /**
     * TextParser constructor. Defines the class loader and package scope
     * for evaluation.
     */
    public TextAnalyzer(EntityResolver parentResolver, String packageScope, ValueCollection ob)
    {
        this.parentResolver = parentResolver;
        this.packageScope = packageScope;
        this.objectBench = ob;
        imports = new ImportsCollection();
    }
    
    /**
     * Set a new class loader, and clear the imports list.
     */
    public void newClassLoader(ClassLoader newLoader)
    {
        imports.clear();
    }
    
    /**
     * Parse a string entered into the code pad. Return is null if the string
     * is a statement; otherwise the string is an expression and the returned
     * string if the type of the expression (empty if the type cannot be determined).
     * 
     * <p>After calling this method, getDeclaredVars() and getAmendedCommand() can be
     * called - see the documentation for those methods respectively.
     * 
     * <p>If the parsed string is then executed, the confirmCommand() method should
     * subsequently be called.
     */
    @OnThread(Tag.FXPlatform)
    public String parseCommand(String command)
    {
        importCandidate = "";
        amendedCommand = command;
        declVars = Collections.emptyList();
        
        EntityResolver resolver = getResolver(); 
        
        Reflective accessRef = new DummyReflective(JavaNames.combineNames(packageScope, "$SHELL"));
        TypeEntity accessType = new TypeEntity(accessRef);
        TextParser parser = new TextParser(resolver, command, accessType, true);
        
        try {

            // check if it's an import statement
            try {
                parser.parseImportStatement();
                if (parser.atEnd()) {
                    amendedCommand = "";
                    importCandidate = command;
                    return null;
                }
            }
            catch (ParseFailure e) {}

            CodepadVarParser vparser = new CodepadVarParser(resolver, command, accessType);
            try {
                if (vparser.parseVariableDeclarations() != null) {
                    declVars = vparser.getVariables();
                    if (! declVars.isEmpty()) {
                        for (DeclaredVar var : declVars) {
                            if (! var.isInitialized() && ! var.isFinal()) {
                                amendedCommand += "\n" + var.getName();
                                String text;
                                JavaType declVarType = var.getDeclaredType();
                                if (declVarType.isPrimitive()) {
                                    if (declVarType.isNumeric()) {
                                        text = " = 0";
                                    }
                                    else {
                                        text = " = false";
                                    }
                                }
                                else {
                                    // reference type
                                    text = " = null";
                                }
                                amendedCommand += text + ";\n";
                            }
                        }
                        return null; // not an expression
                    }
                }
            }
            catch (ParseFailure e) {}

            // Check if it's an expression
            parser = new TextParser(resolver, command, accessType, true);
            try {
                // Note the TextParser will throw an exception if a parse error occurs.
                // We must catch it.
                parser.parseExpression();
                if (parser.atEnd()) {
                    JavaEntity exprType = parser.getExpressionType();
                    if (exprType == null) {
                        return "";
                    }
                    JavaEntity rval =  exprType.resolveAsValue();
                    if (rval != null) {
                        if (rval.getType().typeIs(JavaType.JT_VOID)) {
                            return null;
                        }
                        JavaType rtype = rval.getType();
                        if (rtype.isPrimitive() || rtype.getArrayComponent() != null) {
                            return rtype.toString();
                        }
                        // Use a reference supertype to return something sensible in case
                        // the result is a capture:
                        return rtype.asSolid().getReferenceSupertypes()[0].toString();
                    }
                    return "";
                }
            }
            catch (ParseFailure e) {}
        }
        catch (Throwable t) {
            // It's best at this stage if an exception (other than a parse failure) occurs that
            // we still do a normal return from this method (so the codepad continues to function).
            // However we'll log the problem.
            Debug.reportError("Exception in parser", t);
        }

        return null;
    }

    private EntityResolver getResolver()
    {
        EntityResolver resolver = new EntityResolver()
        {
            @Override
            public TypeEntity resolveQualifiedClass(String name)
            {
                return parentResolver.resolveQualifiedClass(name);
            }
            
            @Override
            public PackageOrClass resolvePackageOrClass(String name, Reflective querySource)
            {
                String pkgScopePrefix = packageScope;
                if (packageScope.length() > 0) {
                    pkgScopePrefix += ".";
                }

                // Imported class?
                TypeEntity entity = imports.getTypeImport(name);
                if (entity != null)
                {
                    return entity;
                }
                
                // Might be a class in the current package
                TypeEntity rval = parentResolver.resolveQualifiedClass(pkgScopePrefix + name);
                if (rval != null) {
                    return rval;
                }
                
                // Try in java.lang (see JLS 7.5.5)
                rval = parentResolver.resolveQualifiedClass("java.lang." + name);
                if (rval != null) {
                    return rval;
                }
                
                // Try in wildcard imports
                entity = imports.getTypeImportWC(name);
                if (entity != null) {
                    return entity;
                }
                
                // Have to assume it's a package
                return new PackageEntity(name, this);
            }
            
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            public JavaEntity getValueEntity(String name, Reflective querySource)
            {
                NamedValue obVal = objectBench.getNamedValue(name);
                if (obVal != null) {
                    return new ValueEntity(obVal.getGenType());
                }
                List<JavaEntity> importStaticVals = imports.getStaticImports(name);
                if (importStaticVals != null && !importStaticVals.isEmpty()) {
                    return importStaticVals.get(0).getSubentity(name, querySource);
                }
                importStaticVals = imports.getStaticWildcardImports();
                if (importStaticVals != null) {
                    for (JavaEntity importStatic : importStaticVals) {
                        importStatic = importStatic.resolveAsType();
                        if (importStatic == null) {
                            continue;
                        }
                        JavaEntity entity = importStatic.getSubentity(name, querySource);
                        if (entity != null) {
                            return entity;
                        }
                    }
                }
                
                return resolvePackageOrClass(name, querySource);
            }
        };
        return resolver;
    }
    
    /**
     * Called to confirm that the recently parsed command has successfully
     * executed. This allows TextParser to update internal state to reflect
     * changes caused by the execution of the command.
     */
    @OnThread(Tag.FXPlatform)
    public void confirmCommand()
    {
        if (importCandidate.length() != 0) {
            Reader r = new StringReader(importCandidate);
            CodepadImportParser parser = new CodepadImportParser(getResolver(), r);
            parser.parseImportStatement();
            if (parser.isStaticImport()) {
                if (parser.isWildcardImport()) {
                    imports.addStaticWildcardImport(parser.getImportEntity()
                            .resolveAsType(), null, null);
                }
                else {
                    imports.addStaticImport(parser.getMemberName(),
                            parser.getImportEntity().resolveAsType(), null, null);
                }
            }
            else {
                if (parser.isWildcardImport()) {
                    imports.addWildcardImport(parser.getImportEntity().resolveAsPackageOrClass(), null, null);
                }
                else {
                    JavaEntity importEntity = parser.getImportEntity();
                    TypeEntity classEnt = importEntity.resolveAsType();
                    String name = classEnt.getType().toString(true);
                    imports.addNormalImport(name, classEnt, null, null);
                }
            }
        }
    }
    
    /**
     * Get a list of the variables declared in the recently parsed statement
     * block. The return is a List of TextParser.DeclaredVar
     */
    public List<DeclaredVar> getDeclaredVars()
    {
        return declVars;
    }
    
    /**
     * Get the amended command string, which has initializers inserted for variable
     * declarations which were missing initializers.
     */
    public String getAmendedCommand()
    {
        return amendedCommand;
    }
    
    /**
     * Return the imports collection as a sequence of java import statements.
     */
    public String getImportStatements()
    {
        return imports.toString() + importCandidate;
    }
    
    /**
     * Java 1.4 & prior version of trinary "? :" operator. See JLS 2nd ed. 
     * section 15.25.
     * 
     * @throws RecognitionException
     * @throws SemanticException
     */
//    private JavaType questionOperator14(AST node) throws RecognitionException, SemanticException
//    {
//        AST trueAlt = node.getFirstChild().getNextSibling();
//        AST falseAlt = trueAlt.getNextSibling();
//        ExprValue trueAltEv = getExpressionType(trueAlt);
//        ExprValue falseAltEv = getExpressionType(falseAlt);
//        JavaType trueAltType = trueAltEv.getType();
//        JavaType falseAltType = falseAltEv.getType();
//        
//        // if the operands have the same type, that is the result type
//        if (trueAltType.equals(falseAltType))
//            return trueAltType;
//        
//        if (trueAltType.isNumeric() && falseAltType.isNumeric()) {
//            // if one type is short and the other is byte, result type is short
//            if (trueAltType.typeIs(JavaType.JT_SHORT) && falseAltType.typeIs(JavaType.JT_BYTE))
//                return JavaPrimitiveType.getShort();
//            if (falseAltType.typeIs(JavaType.JT_SHORT) && trueAltType.typeIs(JavaType.JT_BYTE))
//                return JavaPrimitiveType.getShort();
//            
//            // if one type is byte/short/char and the other is a constant of
//            // type int whose value fits, the result type is byte/short/char
//            if (falseAltType.typeIs(JavaType.JT_INT) && falseAltEv.knownValue()) {
//                int fval = falseAltEv.intValue();
//                if (isMinorInteger(trueAltType) && trueAltType.couldHold(fval))
//                    return trueAltType;
//            }
//            if (trueAltType.typeIs(JavaType.JT_INT) && trueAltEv.knownValue()) {
//                int fval = trueAltEv.intValue();
//                if (isMinorInteger(falseAltType) && falseAltType.couldHold(fval))
//                    return falseAltType;
//            }
//            
//            // binary numeric promotion is applied
//            return binaryNumericPromotion(trueAltType, falseAltType);
//        }
//        
//        // otherwise it must be possible to convert one type to the other by
//        // assignment conversion:
//        if (trueAltType.isAssignableFrom(falseAltType))
//            return trueAltType;
//        if (falseAltType.isAssignableFrom(trueAltType))
//            return falseAltType;
//        
//        throw new SemanticException();
//    }
    
    /**
     * Test if a given type is one of the "minor" integral types: byte, char
     * or short.
     */
//    private static boolean isMinorInteger(JavaType a)
//    {
//        return a.typeIs(JavaType.JT_BYTE) || a.typeIs(JavaType.JT_CHAR) || a.typeIs(JavaType.JT_SHORT); 
//    }
    
    /**
     * Test whether the given value fits in the range representable by byte, short or char
     */
    private static boolean doesValueFitIntType(long value, JavaType type)
    {
        if (type.typeIs(JavaType.JT_BYTE)) {
            return value <= Byte.MAX_VALUE && value >= Byte.MIN_VALUE;
        }
        else if (type.typeIs(JavaType.JT_CHAR)) {
            return value <= Character.MAX_VALUE && value >= Character.MIN_VALUE;
        }
        else if (type.typeIs(JavaType.JT_SHORT)) {
            return value <= Short.MAX_VALUE && value >= Short.MIN_VALUE;
        }
        
        return false;
    }
    
    /**
     * Java 1.5 version of the trinary "? :" operator.
     * See JLS section 15.25. Note that JLS 3rd ed. differs extensively
     * from JLS 2nd edition. The changes are not backwards compatible.
     */
    public static ValueEntity questionOperator15(ValueEntity condition, ValueEntity trueAlt, ValueEntity falseAlt)
    {
        JavaType trueAltType = trueAlt.getType();
        JavaType falseAltType = falseAlt.getType();
        JavaType conditionType = condition.getType();
        
        // The condition must be boolean:
        if (conditionType == null || ! conditionType.typeIs(JavaType.JT_BOOLEAN)) {
            return null;
        }
        
        // if we don't know the type of both alternatives, we don't
        // know the result type:
        if (trueAltType == null || falseAltType == null) {
            return null;
        }
        
        // Neither argument can be a void type.
        if (trueAltType.isVoid() || falseAltType.isVoid()) {
            return null;
        }
        
        // if the second & third arguments have the same type, then
        // that is the result type:
        if (trueAltType.equals(falseAltType)) {
            if (condition.hasConstantBooleanValue() && ValueEntity.isConstant(trueAlt) && ValueEntity.isConstant(falseAlt)) {
                return condition.getConstantBooleanValue() ? trueAlt : falseAlt;
            }
            return new ValueEntity(trueAltType);
        }

        // If one of the operands is the null type and the other is a reference type,
        // the type of the conditional expression is that of the reference type:
        if (trueAltType.isNull() && !falseAltType.isPrimitive()) {
            // The result cannot be a compile-time constant as expression contains a null
            return new ValueEntity(falseAltType);
        }
        
        // Otherwise:
        String trueAltStr = trueAltType.toString();
        String falseAltStr = falseAltType.toString();
        
        boolean trueIsByte = trueAltStr.equals("byte") || trueAltStr.equals("java.lang.Byte");
        boolean falseIsShort = falseAltStr.equals("short") || falseAltStr.equals("java.lang.Short");
        boolean falseIsByte = falseAltStr.equals("byte") || falseAltStr.equals("java.lang.Byte");
        boolean trueIsShort = trueAltStr.equals("short") || trueAltStr.equals("java.lang.Short");
        
        if (trueIsByte && falseIsShort || falseIsByte && trueIsShort) {
            if (condition.hasConstantBooleanValue() && trueAlt.hasConstantIntValue() && falseAlt.hasConstantIntValue()) {
                long intVal = condition.getConstantBooleanValue() ? trueAlt.getConstantIntValue()
                        : falseAlt.getConstantIntValue();
                return new ConstantIntValue(null, JavaPrimitiveType.getShort(), intVal);
            }
            return new ValueEntity(JavaPrimitiveType.getShort());
        }

        // If one of the types is byte/short/char (possibly boxed) and the other is of type int, and is a constant
        // whose value fits in the first type, then the result is of the first type (unboxed).
        
        JavaType trueUnboxed = unBox(trueAltType);
        boolean trueIsSmallInt = trueUnboxed.typeIs(JavaType.JT_BYTE) || trueUnboxed.typeIs(JavaType.JT_SHORT)
                || trueUnboxed.typeIs(JavaType.JT_CHAR);
        
        if (trueIsSmallInt && falseAltType.typeIs(JavaType.JT_INT) && falseAlt.hasConstantIntValue()) {
            long fval = falseAlt.getConstantIntValue();
            if (doesValueFitIntType(fval, trueAltType)) {
                if (trueAlt.hasConstantIntValue() && condition.hasConstantBooleanValue()) {
                    long val = condition.getConstantBooleanValue() ? trueAlt.getConstantIntValue()
                            : falseAlt.getConstantIntValue();
                    return new ConstantIntValue(null, trueAltType, val);
                }
                return new ValueEntity(trueUnboxed);
            }
        }
        
        JavaType falseUnboxed = unBox(falseAltType);
        boolean falseIsSmallInt = falseUnboxed.typeIs(JavaType.JT_BYTE) || falseUnboxed.typeIs(JavaType.JT_SHORT)
                || falseUnboxed.typeIs(JavaType.JT_CHAR);

        if (falseIsSmallInt && trueAltType.typeIs(JavaType.JT_INT) && trueAlt.hasConstantIntValue()) {
            long fval = trueAlt.getConstantIntValue();
            if (doesValueFitIntType(fval, falseAltType)) {
                if (falseAlt.hasConstantIntValue() && condition.hasConstantBooleanValue()) {
                    long val = condition.getConstantBooleanValue() ? trueAlt.getConstantIntValue()
                            : falseAlt.getConstantIntValue();
                    return new ConstantIntValue(null, falseUnboxed, val);
                }
                return new ValueEntity(falseUnboxed);
            }
        }
        
        // Binary numeric promotion?
        
        if (trueUnboxed.isNumeric() && falseUnboxed.isNumeric()) {
            JavaType rtype = binaryNumericPromotion(trueUnboxed, falseUnboxed);
            if (condition.hasConstantBooleanValue() && ValueEntity.isConstant(trueAlt) && ValueEntity.isConstant(falseAlt)) {
                ValueEntity relevantAlt = condition.getConstantBooleanValue() ? trueAlt : falseAlt;
                if (rtype.typeIs(JavaType.JT_DOUBLE) || rtype.typeIs(JavaType.JT_FLOAT)) {
                    double val;
                    if (relevantAlt.getType().typeIs(JavaType.JT_DOUBLE) || relevantAlt.getType().typeIs(JavaType.JT_FLOAT)) {
                        val = relevantAlt.getConstantFloatValue();
                    }
                    else {
                        // the relevant alternative is an integer promoted to a float
                        val = relevantAlt.getConstantIntValue();
                    }
                    return new ConstantFloatValue(rtype, val);
                }
                long val = condition.getConstantBooleanValue() ? trueAlt.getConstantIntValue()
                        : falseAlt.getConstantIntValue();
                return new ConstantIntValue(null, rtype, val);
            }
            return new ValueEntity(rtype);
        }
        
        // No - reference types.
        
        GenTypeSolid trueBoxed = boxType(trueAltType);
        GenTypeSolid falseBoxed = boxType(falseAltType);
        GenTypeSolid rtype = GenTypeSolid.lub(new GenTypeSolid[] {trueBoxed, falseBoxed});
        return new ValueEntity(rtype.getCapture());
    }
    
    /**
     * binary numeric promotion, as defined by JLS section 5.6.2. Both
     * operands must be (possibly boxed) numeric types.
     */
    public static JavaType binaryNumericPromotion(JavaType a, JavaType b)
    {
        JavaType ua = unBox(a);
        JavaType ub = unBox(b);

        if (ua.typeIs(JavaType.JT_DOUBLE) || ub.typeIs(JavaType.JT_DOUBLE))
            return JavaPrimitiveType.getDouble();

        if (ua.typeIs(JavaType.JT_FLOAT) || ub.typeIs(JavaType.JT_FLOAT))
            return JavaPrimitiveType.getFloat();

        if (ua.typeIs(JavaType.JT_LONG) || ub.typeIs(JavaType.JT_LONG))
            return JavaPrimitiveType.getLong();

        if (ua.isNumeric() && ub.isNumeric()) {
            return JavaPrimitiveType.getInt();
        }
        return null;
    }
    
    /**
     * Unary numeric promotion, as defined by JLS 3rd ed. section 5.6.1
     * Return is null if argument type is not convertible to a numeric type.
     */
    public static JavaType unaryNumericPromotion(JavaType a)
    {
        JavaType ua = unBox(a);
        
        // long float and double are merely unboxed; everything else is unboxed and widened to int:
        if (ua.typeIs(JavaType.JT_DOUBLE))
            return JavaPrimitiveType.getDouble();

        if (ua.typeIs(JavaType.JT_FLOAT))
            return JavaPrimitiveType.getFloat();

        if (ua.typeIs(JavaType.JT_LONG))
            return JavaPrimitiveType.getLong();
        
        if (ua.isNumeric()) {
            return JavaPrimitiveType.getInt();
        }
        return null;
    }
        
    /**
     * Check whether a particular method is callable with particular
     * parameters. If so return information about how specific the call is.
     * If the parameters cannot be applied to this method, return null.
     * 
     * @param targetType   The type of object/class to which the method is
     *                     being applied
     * @param targs     The explicitly specified type arguments used in the
     *                  invocation of a generic method (list of GenTypeClass)
     * @param m       The method to check
     * @param args    The types of the arguments supplied to the method
     * @return   A record with information about the method call
     * @throws RecognitionException
     */
    private static MethodCallDesc isMethodApplicable(GenTypeClass targetType, List<GenTypeParameter> targs, MethodReflective m, JavaType [] args)
    {
        boolean methodIsVarargs = m.isVarArgs();
        MethodCallDesc rdesc = null;
        
        // First try without varargs expansion. If that fails, try with expansion.
        rdesc = isMethodApplicable(targetType, targs, m, args, false);
        if (rdesc == null && methodIsVarargs) {
            rdesc = isMethodApplicable(targetType, targs, m, args, true);
        }
        return rdesc;
    }

    /**
     * Check whether a particular method is callable with particular
     * parameters. If so return information about how specific the call is.
     * If the parameters cannot be applied to this method, return null.<p>
     * 
     * Normally this is called by the other variant of this method, which
     * does not take the varargs parameter.
     * 
     * @param targetType   The type of object/class to which the method is
     *                     being applied
     * @param targs     The explicitly specified type parameters used in the
     *                  invocation of a generic method (list of GenTypeClass)
     * @param m       The method to check
     * @param args    The types of the arguments supplied to the method
     * @param varargs Whether to expand vararg parameters
     * @return   A record with information about the method call
     * @throws RecognitionException
     */
    private static MethodCallDesc isMethodApplicable(GenTypeClass targetType,
            List<GenTypeParameter> targs, MethodReflective m, JavaType [] args, boolean varargs)
    {
        boolean rawTarget = targetType.isRaw();
        boolean boxingRequired = false;
        
        // Check that the number of parameters supplied is allowable. Expand varargs
        // arguments if necessary.
        List<JavaType> mparams = m.getParamTypes();
        if (varargs) {
            // first basic check. The number of supplied arguments must be at least one less than
            // the number of formal parameters.
            if (mparams.size() > args.length + 1)
                return null;

            GenTypeSolid lastArgType = mparams.get(mparams.size() - 1).asSolid();
            JavaType vaType = lastArgType.getArrayComponent();
            List<JavaType> expandedParams = new ArrayList<JavaType>(args.length);
            expandedParams.addAll(mparams);
            expandedParams.remove(expandedParams.size() - 1); // remove the vararg array
            for (int i = mparams.size() - 1; i < args.length; i++) {
                expandedParams.add(vaType);
            }
            mparams = expandedParams;
        }
        else {
            // Not varargs: supplied arguments must match formal parameters
            if (mparams.size() != args.length)
                return null;
        }
        
        // Get type parameters of the method
        List<GenTypeDeclTpar> tparams = Collections.emptyList();
        if ((! rawTarget) || m.isStatic()) {
            tparams = m.getTparTypes();
            tparams = (tparams != null) ? tparams : Collections.<GenTypeDeclTpar>emptyList(); 
        }
        
        // Number of type parameters supplied must match number declared, unless either
        // is zero. Section 15.12.2 of the JLS, "a non generic method may be applicable
        // to an invocation which supplies type arguments" (in which case the type args
        // are ignored).
        if (! targs.isEmpty() && ! tparams.isEmpty() && targs.size() != tparams.size())
            return null;
        
        // Set up a map we can use to put actual/inferred type arguments. Initialise it
        // with the target type's arguments.
        Map<String,GenTypeParameter> tparMap;
        if (rawTarget) {
            if (m.isStatic()) {
                tparMap = new HashMap<String,GenTypeParameter>();
            }
            else {
                tparMap = null;
            }
        }
        else {
            tparMap = targetType.getMap();
        }

        // Perform type inference, if necessary
        if (! tparams.isEmpty() && targs.isEmpty()) {
            // Our initial map has the class type parameters, minus those which are
            // shadowed by the method's type parameters (map to themselves).
            for (Iterator<GenTypeDeclTpar> i = tparams.iterator(); i.hasNext(); ) {
                GenTypeDeclTpar tpar = i.next();
                tparMap.put(tpar.getTparName(), tpar);
            }
            
            // lower bound, equality, and upper bound constraints on type parameters:
            Map<String,Set<GenTypeSolid>> tlbConstraints = new HashMap<String,Set<GenTypeSolid>>();
            Map<String,GenTypeSolid> teqConstraints = new HashMap<String,GenTypeSolid>();
            Map<String,GenTypeSolid> tubConstraints = new HashMap<String,GenTypeSolid>();
            
            // Time for some type inference
            for (int i = 0; i < mparams.size(); i++) {
                if (mparams.get(i).isPrimitive()) {
                    continue;
                }
                
                GenTypeSolid mparam = (GenTypeSolid) mparams.get(i);
                mparam = mparam.mapTparsToTypes(tparMap).asType().asSolid();
                processAtoFConstraint(args[i], mparam, tlbConstraints, teqConstraints, tubConstraints);
            }
            
            // what we have now is a map with tpar constraints.
            // Some tpars may not have been constrained: these are inferred to be the
            // intersection of their upper bounds.
            targs = new ArrayList<GenTypeParameter>();
            Iterator<GenTypeDeclTpar> i = tparams.iterator();
            while (i.hasNext()) {
                GenTypeDeclTpar fTpar = i.next();
                String tparName = fTpar.getTparName();
                GenTypeSolid eqConstraint = teqConstraints.get(tparName);
                // If there's no equality constraint, use the lower bound constraints
                if (eqConstraint == null) {
                    Set<GenTypeSolid> lbConstraintSet = tlbConstraints.get(tparName);
                    if (lbConstraintSet != null) {
                        GenTypeSolid [] lbounds = lbConstraintSet.toArray(new GenTypeSolid[lbConstraintSet.size()]);
                        eqConstraint = GenTypeSolid.lub(lbounds); 
                    }
                    else {
                        // no equality or lower bound constraints: use the upper
                        // bounds of the tpar
                        eqConstraint = tubConstraints.get(tparName);
                        if (eqConstraint == null) {
                            eqConstraint = fTpar.getBound();
                        }
                    }
                }
                targs.add(eqConstraint);
                tparMap.put(tparName, eqConstraint);
            }
        }
        else {
            // Get a map of type parameter names to types from the target type
            // complete the type parameter map with tpars of the method
            Iterator<GenTypeDeclTpar> formalI = tparams.iterator();
            Iterator<GenTypeParameter> actualI = targs.iterator();
            while (formalI.hasNext()) {
                GenTypeDeclTpar formalTpar = formalI.next();
                GenTypeSolid argTpar = (GenTypeSolid) actualI.next();
                
                // first we check that the argument type is a subtype of the
                // declared type.
                GenTypeSolid [] formalUbounds = formalTpar.upperBounds();
                for (int i = 0; i < formalUbounds.length; i++) {
                    formalUbounds[i] = (GenTypeSolid) formalUbounds[i].mapTparsToTypes(tparMap);
                    if (formalUbounds[i].isAssignableFrom(argTpar))
                        break;
                    if (i == formalUbounds.length - 1)
                        return null;
                }
                
                tparMap.put(formalTpar.getTparName(), argTpar);
            }
        }
        
        // For each argument, must check the compatibility of the supplied
        // parameter type with the argument type; and if neither the formal
        // parameter or supplied argument are raw, then must check generic
        // contract as well.
        
        for (int i = 0; i < args.length; i++) {
            JavaType formalArg = mparams.get(i);
            JavaType givenParam = args[i];
            
            // Substitute type arguments.
            formalArg = formalArg.mapTparsToTypes(tparMap).getUpperBound();
            
            // check if the given parameter doesn't match the formal argument
            if (! formalArg.isAssignableFrom(givenParam)) {
                // a boxing conversion followed by a widening reference conversion
                if (! formalArg.isAssignableFrom(boxType(givenParam))) {
                    // an unboxing conversion followed by a widening primitive conversion
                    if (! formalArg.isAssignableFrom(unBox(givenParam))) {
                        return null;
                    }
                }
                boxingRequired = true;
            }
        }
        
        JavaType rType = m.getReturnType().mapTparsToTypes(tparMap).asType().getCapture();
        return new MethodCallDesc(m, mparams, varargs, boxingRequired, rType);
    }

    
    
    /**
     * Process a type inference constraint of the form "A is convertible to F".
     * Note F must be a valid formal parameter: it can't be a wildcard with multiple
     * bounds or an intersection type.
     * 
     * @param a  The argument type
     * @param f  The formal parameter type
     * @param tlbConstraints   lower bound constraints (a Map to Set of GenTypeSolid)
     * @param teqConstraints   equality constraints (a Map to GenTypeSolid)
     */
    private static void processAtoFConstraint(JavaType a, GenTypeSolid f,
            Map<String,Set<GenTypeSolid>> tlbConstraints,
            Map<String,GenTypeSolid> teqConstraints,
            Map<String,GenTypeSolid> tubConstraints)
    {
        a = boxType(a);
        if (a == null) {
            return; // no constraint
        }
        
        if (f instanceof GenTypeTpar) {
            // The constraint T :> A is implied
            GenTypeTpar t = (GenTypeTpar) f;
            Set<GenTypeSolid> constraintsSet = tlbConstraints.get(t.getTparName());
            if (constraintsSet == null) {
                constraintsSet = new HashSet<GenTypeSolid>();
                tlbConstraints.put(t.getTparName(), constraintsSet);
            }
            
            constraintsSet.add(a.asSolid());
        }
        
        // If F is an array of the form U[], and a is an array of the form V[]...
        else if (f.getArrayComponent() != null) {
            if (a.getArrayComponent() != null) {
                if (f.getArrayComponent() instanceof GenTypeSolid) {
                    a = a.getArrayComponent();
                    f = (GenTypeSolid) f.getArrayComponent();
                    processAtoFConstraint(a, f, tlbConstraints, teqConstraints, tubConstraints);
                }
            }
        }
        
        // If F is of the form G<...> and A is convertible to the same form...
        else {
            GenTypeClass cf = f.asClass();
            Map<String,GenTypeParameter> fMap = cf.getMap();
            if (fMap != null && a.asSolid() != null) {
                GenTypeClass [] asts = a.asSolid().getReferenceSupertypes();
                for (int i = 0; i < asts.length; i++) {
                    try {
                        GenTypeClass aMapped = asts[i].mapToSuper(cf.classloaderName());
                        Map<String,GenTypeParameter> aMap = aMapped.getMap();
                        if (aMap != null) {
                            Iterator<String> j = fMap.keySet().iterator();
                            while (j.hasNext()) {
                                String tpName = j.next();
                                GenTypeParameter fPar = fMap.get(tpName);
                                GenTypeParameter aPar = aMap.get(tpName);
                                processAtoFtpar(aPar, fPar, tlbConstraints, teqConstraints, tubConstraints);
                            }
                        }
                    }
                    catch (BadInheritanceChainException bice) {}
                }
            }
        }
        return;
    }
    
    /**
     * Process type parameters from a type inference constraint A convertible-to F.
     */
    private static void processAtoFtpar(GenTypeParameter aPar, GenTypeParameter fPar,
            Map<String,Set<GenTypeSolid>> tlbConstraints, Map<String,GenTypeSolid> teqConstraints,
            Map<String,GenTypeSolid> tubConstraints)
    {
        if (fPar instanceof GenTypeSolid) {
            if (aPar instanceof GenTypeSolid) {
                // aPar = fPar
                processAeqFConstraint((GenTypeSolid) aPar, (GenTypeSolid) fPar, tlbConstraints, teqConstraints);
            }
        } else {
            GenTypeSolid flbound = fPar.getLowerBound();
            if (flbound != null) {
                // F-par is of form "? super ..."
                GenTypeSolid albound = aPar.getLowerBound();
                if (albound != null) {
                    // there should only be one element in albounds
                    // recurse with albounds[0] >> flbound[0]
                    processFtoAConstraint(albound, flbound, tlbConstraints, teqConstraints, tubConstraints);
                }
            } else {
                // F-par is of form "? extends ..."
                GenTypeSolid [] fubounds = fPar.getUpperBound().asSolid().getIntersectionTypes();
                GenTypeSolid [] aubounds = aPar.getUpperBound().asSolid().getIntersectionTypes();
                if (fubounds.length > 0 && aubounds.length > 0) {
                    // recurse with aubounds << fubounds[0]
                    processAtoFConstraint(IntersectionType.getIntersection(aubounds), fubounds[0],
                            tlbConstraints, teqConstraints, tubConstraints);
                }
            }
        }
    }
    
    /**
     * Process a type inference constraint of the form "A is equal to F".
     */
    private static void processAeqFConstraint(GenTypeSolid a, GenTypeSolid f,
            Map<String,Set<GenTypeSolid>> tlbConstraints, Map<String,GenTypeSolid> teqConstraints)
    {
        if (f instanceof GenTypeTpar) {
            // The constraint T == A is implied.
            GenTypeTpar t = (GenTypeTpar) f;
            teqConstraints.put(t.getTparName(), a);
        }
        
        else if (f.getArrayComponent() instanceof GenTypeSolid) {
            // "If F = U[] ... if A is an array type V[], or a type variable with an
            // upper bound that is an array type V[]..."
            GenTypeSolid [] asts;
            if (a instanceof GenTypeDeclTpar)
                asts = ((GenTypeDeclTpar) a).upperBounds();
            else
                asts = new GenTypeSolid[] {a};
            
            for (int i = 0; i < asts.length; i++) {
                JavaType act = asts[i].getArrayComponent();
                if (act instanceof GenTypeSolid) {
                    processAeqFConstraint((GenTypeSolid) act, (GenTypeSolid) f.getArrayComponent(), tlbConstraints, teqConstraints);
                }
            }
        }
        
        else {
            GenTypeClass cf = f.asClass();
            GenTypeClass af = a.asClass();
            if (af != null && cf != null) {
                if (cf.classloaderName().equals(af.classloaderName())) {
                    Map<String,GenTypeParameter> fMap = cf.getMap();
                    Map<String,GenTypeParameter> aMap = af.getMap();
                    if (fMap != null && aMap != null) {
                        Iterator<String> j = fMap.keySet().iterator();
                        while (j.hasNext()) {
                            String tpName = j.next();
                            GenTypeParameter fPar = fMap.get(tpName);
                            GenTypeParameter aPar = aMap.get(tpName);
                            processAeqFtpar(aPar, fPar, tlbConstraints, teqConstraints);
                        }
                    }
                }
            }
        }
    }

    private static GenTypeSolid getSolidUpperBound(GenTypeParameter tpar)
    {
        JavaType ubound = tpar.getUpperBound();
        if (ubound != null) {
            return ubound.asSolid();
        }
        return null;
    }
    
    /**
     * Process type parameters from a type inference constraint A equal-to F.
     */
    private static void processAeqFtpar(GenTypeParameter aPar, GenTypeParameter fPar,
            Map<String,Set<GenTypeSolid>> tlbConstraints, Map<String,GenTypeSolid> teqConstraints)
    {
        if (aPar instanceof GenTypeSolid && fPar instanceof GenTypeSolid) {
            processAeqFConstraint((GenTypeSolid) aPar, (GenTypeSolid) fPar, tlbConstraints, teqConstraints);
        }
        else if (aPar instanceof GenTypeWildcard && fPar instanceof GenTypeWildcard) {
            GenTypeSolid flBound = fPar.getLowerBound();
            GenTypeSolid fuBound = getSolidUpperBound(fPar); 
            // F = ? super U,  A = ? super V
            if (flBound != null) {
                GenTypeSolid alBound = aPar.getLowerBound();
                if (alBound != null)
                    processAeqFConstraint(alBound, flBound, tlbConstraints, teqConstraints);
            }
            // F = ? extends U, A = ? extends V
            else if (fuBound != null) {
                GenTypeSolid auBound = getSolidUpperBound(aPar);
                if (auBound != null) {
                    processAeqFConstraint(auBound, fuBound, tlbConstraints, teqConstraints);
                }
            }
        }
    }

    /**
     * Process a type inference constraint of the form "F is convertible to A".
     */
    private static void processFtoAConstraint(GenTypeSolid a, GenTypeSolid f,
            Map<String,Set<GenTypeSolid>> tlbConstraints, Map<String,GenTypeSolid> teqConstraints,
            Map<String,GenTypeSolid> tubConstraints)
    {
        // If F = T, then T <: A is implied:
        if (f instanceof GenTypeTpar) {
            GenTypeTpar ftpar = (GenTypeTpar) f;
            GenTypeSolid ubcons = tubConstraints.get(ftpar.getTparName());
            if (ubcons == null) {
                ubcons = a;
            }
            else {
                ubcons = IntersectionType.getIntersection(new GenTypeSolid[] {ubcons, a});
            }
            tubConstraints.put(ftpar.getTparName(), ubcons);
        }
        
        // If F = U[] ...
        else if (f.getArrayComponent() instanceof GenTypeSolid) {
            // "If F = U[] ... if A is an array type V[], or a type variable with an
            // upper bound that is an array type V[]..."
            GenTypeSolid [] asts;
            if (a instanceof GenTypeDeclTpar) {
                asts = ((GenTypeDeclTpar) a).upperBounds();
            }
            else {
                asts = new GenTypeSolid[] {a};
            }
            
            for (int i = 0; i < asts.length; i++) {
                JavaType act = asts[i].getArrayComponent().asSolid();
                if (act != null) {
                    processFtoAConstraint((GenTypeSolid) act, (GenTypeSolid) f.getArrayComponent(),
                            tlbConstraints, teqConstraints, tubConstraints);
                }
            }
        }
        
        // If F has form G<..,..>
        else if (f.asClass() != null && a.asClass() != null) {
            GenTypeClass cf = f.asClass();
            GenTypeClass af = a.asClass();
            try {
                GenTypeClass fMapped = cf.mapToSuper(af.classloaderName());
                Map<String,GenTypeParameter> aMap = af.getMap();
                Map<String,GenTypeParameter> fMap = fMapped.getMap();
                if (aMap != null && fMap != null) {
                    Iterator<String> j = fMap.keySet().iterator();
                    while (j.hasNext()) {
                        String tpName = j.next();
                        GenTypeParameter fPar = fMap.get(tpName);
                        GenTypeParameter aPar = aMap.get(tpName);
                        processFtoAtpar(aPar, fPar, tlbConstraints, teqConstraints, tubConstraints);
                    }
                }
            }
            catch (BadInheritanceChainException bice) {}
        }
    }

    /**
     * Process type parameters from a type inference constraint F convertible-to A.
     */
    private static void processFtoAtpar(GenTypeParameter aPar, GenTypeParameter fPar,
            Map<String,Set<GenTypeSolid>> tlbConstraints, Map<String,GenTypeSolid> teqConstraints,
            Map<String,GenTypeSolid> tubConstraints)
    {
        if (fPar instanceof GenTypeSolid) {
            if (aPar instanceof GenTypeSolid) {
                processAeqFConstraint((GenTypeSolid) aPar, (GenTypeSolid) fPar, tlbConstraints, teqConstraints);
            }
            else {
                GenTypeSolid alBound = aPar.getLowerBound();
                if (alBound != null) {
                    // aPar is of the form "? super ..."
                    processAtoFConstraint(alBound, (GenTypeSolid) fPar, tlbConstraints, teqConstraints, tubConstraints);
                }
                else {
                    GenTypeSolid auBound = aPar.getUpperBound().asSolid();
                    if (auBound != null) {
                        processFtoAConstraint(auBound, fPar.asSolid(), tlbConstraints, teqConstraints, tubConstraints);
                    }
                }
            }
        }
        
        else {
            // fPar must be a wildcard
            GenTypeSolid flBound = fPar.getLowerBound();
            if (flBound != null) {
                if (aPar instanceof GenTypeWildcard) {
                    // fPar is ? super ...
                    GenTypeSolid alBound = aPar.getLowerBound();
                    if (alBound != null) {
                        processAtoFConstraint(alBound, flBound, tlbConstraints, teqConstraints, tubConstraints);
                    }
                }
                else {
                    // fPar is ? extends ...
                    GenTypeSolid fuBound = fPar.getUpperBound().asSolid();
                    GenTypeSolid auBound = aPar.getUpperBound().asSolid();
                    if (fuBound != null && auBound != null) {
                        GenTypeSolid [] fuBounds = fuBound.getIntersectionTypes();
                        GenTypeSolid [] auBounds = auBound.getIntersectionTypes();
                        processFtoAConstraint(auBounds[0], fuBounds[0], tlbConstraints, teqConstraints, tubConstraints);
                    }
                }
            }
        }
    }
        
    /**
     * Get the candidate list of methods with the given name and argument types. The returned
     * list will be the maximally specific methods (as defined by the JLS 15.12.2.5). The
     * methods returned in the list might not be <i>appropriate</i> as according to JLS 15.12.3.
     * 
     * @param methodName    The name of the method
     * @param targetType    The type to search for declarations of this method (must have had
     *                       capture conversion applied, if necessary)
     * @param argumentTypes The types of the arguments supplied in the method invocation
     * @param typeArgs      The type arguments, if any, supplied in the method invocation
     * @return  an ArrayList of MethodCallDesc - the list of candidate methods
     * @throws RecognitionException
     */
    public static ArrayList<MethodCallDesc> getSuitableMethods(String methodName,
            GenTypeSolid targetType, JavaType [] argumentTypes, List<GenTypeParameter> typeArgs,
            Reflective accessType)
    {
        ArrayList<MethodCallDesc> suitableMethods = new ArrayList<MethodCallDesc>();
        
        Stack<GenTypeSolid> targetTypes = new Stack<GenTypeSolid>();
        targetTypes.push(targetType);
        Set<GenTypeSolid> doneTypes = new HashSet<GenTypeSolid>();
        
        // JLS 4.5.2 specifies members of parameterized types
        // JLS 4.4, the members of a type variable are the members of the intersection type of the
        //          bounds
        // Mostly straightforward.
        
        while (! targetTypes.isEmpty()) {
            GenTypeSolid topType = targetTypes.pop();
            GenTypeClass targetClass = topType.asClass();
            if (targetClass == null) {
                GenTypeSolid [] bounds = topType.getUpperBounds();
                for (int i = 0; i < bounds.length; i++) {
                    if (doneTypes.add(bounds[i])) {
                        targetTypes.push(bounds[i]);
                    }
                }
                continue;
            }
            
            // Check members of supertypes
            Reflective ref = targetClass.getReflective();
            List<GenTypeClass> supers = ref.getSuperTypes();
            Map<String,GenTypeParameter> tparMap = targetClass.getMap();

            for (GenTypeClass superType : supers) {
                GenTypeClass mapped = superType.mapTparsToTypes(tparMap);
                if (doneTypes.add(mapped)) {
                    targetTypes.push(mapped);
                }
            }
            
            Map<String,Set<MethodReflective>> methodMap = targetClass.getReflective()
                    .getDeclaredMethods();
            Set<MethodReflective> methods = methodMap.get(methodName);

            if (methods == null) {
                continue;
            }
            
            // Find methods that are applicable, and
            // accessible. See JLS 15.12.2.1.
            for (MethodReflective method : methods) {

                // Check accessibility
                if (!JavaUtils.checkMemberAccess(method.getDeclaringType(), targetType, accessType,
                        method.getModifiers(), false)) {
                    continue;
                }
                
                // check that the method is applicable (and under
                // what constraints)
                MethodCallDesc mcd = isMethodApplicable(targetClass, typeArgs, method, argumentTypes);

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
                        MethodCallDesc mc = suitableMethods.get(j);
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
        return suitableMethods;
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
    public static JavaType unBox(JavaType b)
    {
        GenTypeClass c = b.asClass();
        if (c != null) {
            String cName = c.classloaderName();
            if (cName.equals("java.lang.Integer")) {
                return JavaPrimitiveType.getInt();
            }
            else if (cName.equals("java.lang.Long")) {
                return JavaPrimitiveType.getLong();
            }
            else if (cName.equals("java.lang.Short")) {
                return JavaPrimitiveType.getShort();
            }
            else if (cName.equals("java.lang.Byte")) {
                return JavaPrimitiveType.getByte();
            }
            else if (cName.equals("java.lang.Character")) {
                return JavaPrimitiveType.getChar();
            }
            else if (cName.equals("java.lang.Float")) {
                return JavaPrimitiveType.getFloat();
            }
            else if (cName.equals("java.lang.Double")) {
                return JavaPrimitiveType.getDouble();
            }
            else if (cName.equals("java.lang.Boolean")) {
                return JavaPrimitiveType.getBoolean();
            }
        }
        return b;
    }
    
    /**
     * Box a type, if it is a primitive type such as "int".<p>
     * 
     * Other types are returned unchanged, however void/null types return null.<p>
     * 
     * @param b  The type to box
     * @return  The boxed type
     */
    private static GenTypeSolid boxType(JavaType u)
    {
        if (u.isPrimitive()) {
            if (u.typeIs(JavaType.JT_INT)) {
                return new GenTypeClass(new JavaReflective(Integer.class));
            }
            else if (u.typeIs(JavaType.JT_LONG)) {
                return new GenTypeClass(new JavaReflective(Long.class));
            }
            else if (u.typeIs(JavaType.JT_SHORT)) {
                return new GenTypeClass(new JavaReflective(Short.class));
            }
            else if (u.typeIs(JavaType.JT_BYTE)) {
                return new GenTypeClass(new JavaReflective(Byte.class));
            }
            else if (u.typeIs(JavaType.JT_CHAR)) {
                return new GenTypeClass(new JavaReflective(Character.class));
            }
            else if (u.typeIs(JavaType.JT_FLOAT)) {
                return new GenTypeClass(new JavaReflective(Float.class));
            }
            else if (u.typeIs(JavaType.JT_DOUBLE)) {
                return new GenTypeClass(new JavaReflective(Double.class));
            }
            else if (u.typeIs(JavaType.JT_BOOLEAN)) {
                return new GenTypeClass(new JavaReflective(Boolean.class));
            }
        }
        
        return u.asSolid();
    }
        
    /**
     * Conditionally box a type. The type is only boxed if the boolean flag
     * passed in the second parameter is true.<p>
     * 
     * This is a helper method to improve readability.<p>
     * 
     * @see TextAnalyzer#boxType(JavaType)
     * 
     * @param u    The type to box
     * @param box  The flag indicating whether boxing should occur
     * @return
     */
//    private JavaType maybeBox(JavaType u, boolean box)
//    {
//        if (box)
//            return boxType(u);
//        else
//            return u;
//    }
    
    /**
     * A simple structure to hold various information about a method call.
     * 
     * @author Davin McCall
     */
    public static class MethodCallDesc
    {
        public MethodReflective method;
        public List<JavaType> argTypes; // list of GenType
        public boolean vararg;   // is a vararg call
        public boolean autoboxing; // requires autoboxing
        public JavaType retType; // effective return type (before capture conversion)
        
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
        public MethodCallDesc(MethodReflective m, List<JavaType> argTypes, boolean vararg, boolean autoboxing, JavaType retType)
        {
            this.method = m;
            this.argTypes = argTypes;
            this.vararg = vararg;
            this.autoboxing = autoboxing;
            this.retType = retType;
        }
        
        /**
         * Find out which (if any) method call is strictly more specific than the
         * other. Both calls must be valid calls to the same method with the same
         * number of parameters.
         * 
         * @param other  The method to compare with
         * @return 1 if this method is more specific;
         *         -1 if the other method is more specific;
         *         0 if neither method is more specific than the other.
         * 
         * See JLS 15.12.2.5 (by "more specific", we mean what the JLS calls
         * "strictly more specific", more or less. We also take arity into account.
         * This method effectively combines and performs phases 1-3 as specified by
         * JLS 15.12.2.2 - 15.12.2.4
         */
        public int compareSpecificity(MethodCallDesc other)
        {
            if (other.vararg && ! vararg) {
                return 1; // we are more specific
            }
            if (! other.vararg && vararg) {
                return -1; // we are less specific
            }
            
            // I am reasonably sure this gives the same result as the algorithm
            // described in the JLS section 15.12.2.5, and it has the advantage
            // of being a great deal simpler.
            Iterator<JavaType> i = argTypes.iterator();
            Iterator<JavaType> j = other.argTypes.iterator();
            int upCount = 0;
            int downCount = 0;
            
            while (i.hasNext()) {
                JavaType myArg = i.next();
                JavaType otherArg = j.next();
                
                if (myArg.isAssignableFrom(otherArg)) {
                    if (! otherArg.isAssignableFrom(myArg))
                        upCount++;
                }
                else if (otherArg.isAssignableFrom(myArg)) {
                    downCount++;
                }
            }
            
            if (upCount > 0 && downCount == 0) {
                return -1; // other is more specific
            }
            else if (downCount > 0 && upCount == 0) {
                return 1;  // other is less specific
            }
            
            return 0;
        }
    }
}
