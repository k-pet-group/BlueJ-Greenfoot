/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2014,2015,2016,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.utility;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for dealing with reflection, which must behave differently for
 * Java 1.4 / 1.5. Use the factory method "getJavaUtils" to retrieve an object
 * to use. 
 *   
 * @author Davin McCall
 */
@OnThread(Tag.Any)
public abstract class JavaUtils
{
    private static JavaUtils jutils;
    
    /**
     * Factory method. Returns a JavaUtils object.
     * @return an object supporting the appropriate feature set
     */
    public static JavaUtils getJavaUtils()
    {
        if( jutils != null ) {
            return jutils;
        }
        
        jutils = new JavaUtils15();
        return jutils;
    }
    
    /**
     * Get a "signature" description of a method.
     * Looks like:  void method(int, int, int)
     *   (ie. excludes parameter names)
     * @param method The method to get the signature for
     * @return the signature string
     */
    public static String getSignature(Method method)
    {
        String name = getFQTypeName(method.getReturnType()).replace('$', '.') + " " + method.getName();
        Class<?>[] params = method.getParameterTypes();
        return makeSignature(name, params);
    }

    /**
     * Get a fully-qualified type name. For array types return the base type
     * name plus the appropriate number of "[]" qualifiers.
     */
    static public String getFQTypeName(Class<?> type)
    {
        // Early exit for common case:
        if (!type.isArray())
            return type.getName();

        Class<?> primtype = type;
        int dimensions = 0;
        while (primtype.isArray()) {
            dimensions++;
            primtype = primtype.getComponentType();
        }
        StringBuffer sb = new StringBuffer();
        sb.append(primtype.getName());
        for (int i = 0; i < dimensions; i++)
            sb.append("[]");
        return sb.toString();
    }

    /**
     * Build the signature string. Format: name(type,type,type)
     */
    private static String makeSignature(String name, Class<?>[] params)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append("(");
        for (int j = 0; j < params.length; j++) {
            String typeName = getFQTypeName(params[j]).replace('$', '.');
            sb.append(typeName);
            if (j < (params.length - 1))
                sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Get a "signature" description of a constructor.
     * Looks like:  ClassName(int, int, int)
     *   (ie. excludes parameter names)
     * @param cons the Constructor to get the signature for
     * @return the signature string
     */
    public static String getSignature(Constructor<?> cons)
    {
        String name = JavaNames.getBase(cons.getName());
        Class<?>[] params = cons.getParameterTypes();
        return makeSignature(name, params);
    }
 
    /**
     * Translate escape characters into their source representation.
     * The result is suitable for inserting into Java source (between quotes).
     */
    public static String escapeString(String s)
    {
        StringBuffer outBuf = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\b') {
                outBuf.append("\\b");
            }
            else if (c == '\t') {
                outBuf.append("\\t");
            }
            else if (c == '\n') {
                outBuf.append("\\n");
            }
            else if (c == '\f') {
                outBuf.append("\\f");
            }
            else if (c == '\r') {
                outBuf.append("\\r");
            }
            else if (c == '\\') {
                outBuf.append("\\\\");
            }
            else if (c == '\"') {
                outBuf.append("\\\"");
            }
            else if (c < 32) {
                String uescape = Integer.toHexString(c);
                uescape = "0000".substring(uescape.length()) + uescape;
                outBuf.append("\\u" + uescape);
            }
            else {
                outBuf.append(c);
            }
        }
        
        return outBuf.toString();
    }
    
    /**
     * Get a "short description" of a method. This is like the signature,
     * but substitutes the parameter names for their types.
     * 
     * @param method   The method to get the description of
     * @param paramnames  The parameter names of the method
     * @return The description.
     */
    abstract public String getShortDesc(Method method, String [] paramnames)
        throws ClassNotFoundException;

    /**
     * Get a "short description" of a method, and map class type parameters to
     * the given types. A short description is like the signature, but
     * substitutes the parameter names for their types. Generic method type
     * parameters are left unmapped.
     * 
     * @param method   The method to get the description of
     * @param paramnames The parameter names of the method
     * @param tparams  The map (String -> GenType) for class type parameters
     * @return The description.
     */
    @OnThread(Tag.FXPlatform)
    abstract public String getShortDesc(Method method, String [] paramnames,
            Map<String,GenTypeParameter> tparams) throws ClassNotFoundException;

    /**
     * Get a long String describing the method. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    abstract public String getLongDesc(Method method, String [] paramnames)
        throws ClassNotFoundException;
    
    /**
     * Get a long String describing the method, with class type parameters
     * mapped to their instantiation types. A long description is similar to a
     * short description, but it has type names of parameters included.
     * 
     * @param method   The method to get the description of
     * @param paramnames  The parameters names of the method
     * @param tparams  The map (String -> GenType) for class type parameters
     * @return The long description string.
     */
    @OnThread(Tag.FXPlatform)
    abstract public String getLongDesc(Method method, String [] paramnames,
            Map<String,GenTypeParameter> tparams) throws ClassNotFoundException;
    
    /**
     * Get a "short description" of a constructor. This is like the signature,
     * but substitutes the parameter names for their types.
     * 
     * @param constructor   The constructor to get the description of
     * @return The description.
     */
    abstract public String getShortDesc(Constructor<?> constructor, String [] paramnames)
        throws ClassNotFoundException;
    
    /**
     * Get a long String describing the constructor. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    abstract public String getLongDesc(Constructor<?> constructor, String [] paramnames)
        throws ClassNotFoundException;
    
    abstract public boolean isVarArgs(Constructor<?> cons);
    
    abstract public boolean isVarArgs(Method method);    
   
    abstract public boolean isSynthetic(Method method);
    
    abstract public boolean isEnum(Class<?> cl);
    
    /**
     * Get the return type of a method.
     */
    @OnThread(Tag.FXPlatform)
    abstract public JavaType getReturnType(Method method) throws ClassNotFoundException;

    @OnThread(Tag.FXPlatform)
    abstract public JavaType getRawReturnType(Method method);

    /**
     * Get the declared type of a field.
     */
    @OnThread(Tag.FXPlatform)
    abstract public JavaType getFieldType(Field field) throws ClassNotFoundException;

    @OnThread(Tag.FXPlatform)
    abstract public JavaType getRawFieldType(Field field);
    
    /**
     * Get a list of the type parameters for a generic method.
     * (return an empty list if the method is not generic).
     * 
     * @param method   The method fro which to find the type parameters
     * @return  A list of GenTypeDeclTpar
     */
    @OnThread(Tag.FXPlatform)
    abstract public List<GenTypeDeclTpar> getTypeParams(Method method);
    
    /**
     * Get a list of the type parameters for a generic constructor.
     * (return an empty list if the method is not generic).
     * 
     * @param cons   The constructors for which to find the type parameters
     * @return  A list of GenTypeDeclTpar
     */
    @OnThread(Tag.FXPlatform)
    abstract public List<GenTypeDeclTpar> getTypeParams(Constructor<?> cons);
    
    /**
     * Get a list of the type parameters for a class. Return an empty list if
     * the class is not generic.
     * 
     * @param cl the class
     * @return A List of GenTypeDeclTpar
     */
    @OnThread(Tag.FXPlatform)
    abstract public List<GenTypeDeclTpar> getTypeParams(Class<?> cl);
    
    /**
     * Get the declared supertype of a class.
     */
    @OnThread(Tag.FXPlatform)
    abstract public GenTypeClass getSuperclass(Class<?> cl) throws ClassNotFoundException;
    
    /**
     * Get a list of the interfaces directly implemented by the given class.
     * @param cl  The class for which to find the interfaces
     * @return    An array of interfaces
     */
    @OnThread(Tag.FXPlatform)
    abstract public GenTypeClass [] getInterfaces(Class<?> cl) throws ClassNotFoundException;
    
    /**
     * Gets an array of nicely formatted strings with the types of the parameters.
     * Include the ellipsis (...) for a varargs method.
     * 
     * @param method The method to get the parameters for.
     */
    abstract public String[] getParameterTypes(Method method) throws ClassNotFoundException;
    
    /**
     * Get an array containing the argument types of the method.
     * 
     * In the case of a varargs method, the last argument will be an array
     * type.
     * 
     * @param method  the method whose argument types to get
     * @param raw     whether to return the raw versions of argument types
     * @return  the argument types
     */
    @OnThread(Tag.FXPlatform)
    abstract public JavaType[] getParamGenTypes(Method method, boolean raw) throws ClassNotFoundException;
    
    /**
     * Gets an array of nicely formatted strings with the types of the parameters.
     * Include the ellipsis (...) for a varargs constructor.
     * 
     * @param constructor The constructor to get the parameters for.
     */
    abstract public String[] getParameterTypes(Constructor<?> constructor) throws ClassNotFoundException;
    
    /**
     * Get an array containing the argument types of the method.
     * 
     * In the case of a varargs method, the last argument will be an array
     * type.
     * 
     * @param constructor  the constructor whose argument types to get
     * @return  the argument types
     */
    @OnThread(Tag.FXPlatform)
    abstract public JavaType[] getParamGenTypes(Constructor<?> constructor) throws ClassNotFoundException;
    
    /**
     * Change a list of type parameters (with bounds) into a map, which maps
     * the name of the parameter to its bounding type.
     * 
     * @param tparams   A list of GenTypeDeclTpar
     * @return          A map (String -> GenTypeSolid)
     */
    @OnThread(Tag.FXPlatform)
    public static Map<String,GenTypeSolid> TParamsToMap(List<GenTypeDeclTpar> tparams)
    {
        Map<String,GenTypeSolid> rmap = new HashMap<String,GenTypeSolid>();
        for( Iterator<GenTypeDeclTpar> i = tparams.iterator(); i.hasNext(); ) {
            GenTypeDeclTpar n = i.next();
            rmap.put(n.getTparName(), n.getBound().mapTparsToTypes(rmap).getUpperBound().asSolid());
        }
        return rmap;
    }
    
    /**
     * Check whether a member of some container type can be accessed from another type
     * according to its modifiers.
     * 
     * @param container  The type containing the member to which access is being checked
     * @param targetType The type of the expression from which the member is accessed
     * @param accessor   The type trying to access the member
     * @param modifiers  The modifiers of the member
     * @param isStatic   True if the access is a in static context; false if not
     * 
     * @return  true if the access is allowed, false otherwise
     */
    @OnThread(Tag.FXPlatform)
    public static boolean checkMemberAccess(Reflective container, GenTypeSolid targetType,
            Reflective accessor, int modifiers, boolean isStatic)
    {
        // Access from a static context can only access static members
        if (isStatic && !Modifier.isStatic(modifiers))
            return false;
        
        if (Modifier.isPublic(modifiers)) {
            return true;
        }
        
        if (accessor == null) {
            return false;
        }
        
        String accessorName = accessor.getName();
        if (! Modifier.isPrivate(modifiers)) {
            String cpackage = JavaNames.getPrefix(container.getName());
            if (accessorName.startsWith(cpackage)
                    && accessorName.indexOf('.', cpackage.length() + 1) == -1) {
                // Classes are in the same package, and the member is not private: access allowed
                return true;
            }
        }
        
        // access class == container class, then access is always allowed
        if (accessorName.equals(container.getName())) {
            return true;
        }
        
        Reflective outer = accessor.getOuterClass();
        if (outer != null) {
            // Inner classes can access outer class members with outer class privileges
            isStatic |= accessor.isStatic();
            if (checkMemberAccess(container, targetType, outer, modifiers, isStatic)) {
                return true;
            }
        }
        
        // Protected access is allowed if the targetType is a subtype of the acessType
        Set<Reflective> targetSupers = new HashSet<Reflective>();
        targetType.erasedSuperTypes(targetSupers);
        boolean allowProtected = false;
        for (Reflective ref : targetSupers) {
            if (accessor.isAssignableFrom(ref)) {
                allowProtected = true;
                break;
            }
        }
        
        List<Reflective> supers = accessor.getSuperTypesR();
        Set<String> done = new HashSet<String>();
        while (! supers.isEmpty()) {
            Reflective r = supers.remove(0);
            if (done.add(r.getName())) {
                if (r.getName().equals(container.getName())) {
                    if (Modifier.isProtected(modifiers)) {
                        return allowProtected;
                    }
                } else {
                    // We need to check super classes of our super-classes
                    // as if the method is protected we will be allowed access 
                    for (Reflective rParent : r.getSuperTypesR()) {
                        supers.add(rParent);
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Make a descriptive signature. This includes the method/constructor name (which may
     * be preceded by type parameters), and parameter types or names or types and names.
     * (The type is always substituted if the name is missing). 
     * 
     * @param name       The method/constructor name (including preceding
     *                          type parameters if any)
     * @param paramTypes   The parameter types
     * @param paramNames   The parameter names (may be null)
     * @param includeTypeNames   True if the parameter type should always be included
     * @param isVarArgs      True if the method is varargs (requires ellipsis insertion)
     */
    protected static String makeDescription(String name, String[] paramTypes, String[] paramNames, boolean includeTypeNames, boolean isVarArgs)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append("(");
        for (int j = 0; j < paramTypes.length; j++) {
            boolean typePrinted = false;
            if (isVarArgs && j == paramTypes.length - 1) {
                if (includeTypeNames || paramNames == null || paramNames[j] == null) {
                    sb.append(paramTypes[j].substring(0, paramTypes[j].length() - 2));
                    sb.append(" ");
                }
                sb.append("...");
                typePrinted = true;
            }
            else if (includeTypeNames || paramNames == null || paramNames[j] == null) {                              
                sb.append(paramTypes[j]);
                typePrinted = true;
            }
            
            if (paramNames != null && paramNames[j] != null) {
                if (typePrinted)
                    sb.append(" ");
                sb.append(paramNames[j]);
            }
            if (j < (paramTypes.length - 1))
                sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
    
    /**
     * Convert a javadoc comment to a string with just the comment body, i.e. strip the
     * leading asterisks.
     */
    public static String javadocToString(String javadoc)
    {
        String eol = System.getProperty("line.separator");
        
        if (javadoc == null || javadoc.length() < 5) {
            return null;
        }
        
        StringBuffer outbuf = new StringBuffer();
        
        String str = javadoc;
        if (javadoc.charAt(0) == '/') {
            if (javadoc.charAt(1) == '*') {
                if (javadoc.charAt(2) == '*') {
                    // remove "/**" and "*/"
                    str = javadoc.substring(3, javadoc.length() - 2);
                }
                else {
                    // remove "/*" and "*/"
                    str = javadoc.substring(2, javadoc.length() - 2);
                }
            }
        }
        
        int nl = str.indexOf('\n');
        int cr = str.indexOf('\r');
        int pos = 0;
        while (nl != -1 || cr != -1) {
            int lineEnd = Math.min(nl, cr);
            lineEnd = (nl == -1) ? cr : lineEnd;
            lineEnd = (cr == -1) ? nl : lineEnd;
            
            String line = str.substring(pos, lineEnd);
            line = stripLeadingStars(line);
            
            outbuf.append(line);
            outbuf.append(eol);
            
            pos = lineEnd + 1;
            if (pos == nl) {
                pos++;
            }

            nl = str.indexOf('\n', pos);
            cr = str.indexOf('\r', pos);
        }
        
        String line = stripLeadingStars(str.substring(pos)).trim();
        if (line.length() > 0) {
            outbuf.append(line);
        }
        
        return outbuf.toString();
    }
    
    public static class Javadoc
    {
        private final String intro;
        private final List<String> blocks;
        
        public Javadoc(String intro, List<String> blocks)
        {
            this.intro = intro;
            this.blocks = blocks;
        }
        
        public String getHeader()
        {
            return intro;
        }
        
        // Minus the leading '@'
        public List<String> getBlocks()
        {
            return blocks;
        }
    }
    
    public static Javadoc parseJavadoc(String javadocString)
    {
        if (javadocString == null)
            return null;
        
        // find the first block tag
        int i;
        for (i = 0; i < javadocString.length(); i++) {
            // Here we are the start of the line
            while (i < javadocString.length() && Character.isWhitespace(javadocString.charAt(i))) {
                i++;
            }
            if (i >= javadocString.length() || javadocString.charAt(i) == '@') {
                break;
            }
            while (i < javadocString.length()
                    && javadocString.charAt(i) != '\n'
                    && javadocString.charAt(i) != '\r') {
                i++;
            }
        }
        
        if (i >= javadocString.length()) {
            return new Javadoc(javadocString, Collections.emptyList());
        }
        
        // Process the block tags
        String header = javadocString.substring(0, i);
        String blocksText = javadocString.substring(i);
        String[] lines = Utility.splitLines(blocksText);

        List<String> blocks = getBlockTags(lines);
        
        return new Javadoc(header, blocks);
    }
    

    /**
     * Strip leading asterisk characters (and any preceding whitespace) from a single
     * line of text.
     */
    private static String stripLeadingStars(String s)
    {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '*') {
                do {
                    i++;
                } while (i < s.length() && s.charAt(i) == '*');
                s = s.substring(i);
                break;
            }
            if (! Character.isWhitespace(s.charAt(i))) {
                break;
            }
        }
        return s;
    }
    
    /**
     * Get a GenType corresponding to the (raw) class c
     */
    @OnThread(Tag.FXPlatform)
    public static JavaType genTypeFromClass(Class<?> c)
    {
        if (c.isPrimitive()) {
            if (c == boolean.class)
                return JavaPrimitiveType.getBoolean();
            if (c == char.class)
                return JavaPrimitiveType.getChar();
            if (c == byte.class)
                return JavaPrimitiveType.getByte();
            if (c == short.class)
                return JavaPrimitiveType.getShort();
            if (c == int.class)
                return JavaPrimitiveType.getInt();
            if (c == long.class)
                return JavaPrimitiveType.getLong();
            if (c == float.class)
                return JavaPrimitiveType.getFloat();
            if (c == double.class)
                return JavaPrimitiveType.getDouble();
            if (c == void.class)
                return JavaPrimitiveType.getVoid();
            Debug.message("getReturnType: Unknown primitive type");
        }
        if (c.isArray()) {
            JavaType componentT = genTypeFromClass(c.getComponentType());
            return componentT.getArray();
        }
        return new GenTypeClass(new JavaReflective(c));
    }
    
//    private static final Pattern headerPattern = Pattern.compile("{1,}\\s@\\w");
    private static final Pattern paramNamePattern = Pattern.compile("param\\s+\\w"); // regular expression for the parameter name
    private static final Pattern paramDescPattern = Pattern.compile("\\s+\\w");  // regular expression for the parameter description

    /**
     * Convert javadoc comment body (as extracted by javadocToString for instance)
     * to HTML suitable for display by HTMLEditorKit.
     */
    public static String javadocToHtml(String javadocString)
    {
        String javadocStringCodeTagged = convertCodeTokens(javadocString);
        Javadoc j = parseJavadoc(javadocStringCodeTagged);

        StringBuilder rest = new StringBuilder();
        StringBuilder params = new StringBuilder();
        params.append("<h3>Parameters</h3>").append("<table border=0>");
        boolean hasParamDoc = false;
        

        for (String block : j.getBlocks()) {
            Matcher matcher = paramNamePattern.matcher(block); //search the current block
            String paramName = "";
            String paramDesc = "";
            if (matcher.find()) {
                int p = matcher.end() - 1; //mark start of the parameter's name
                matcher = paramDescPattern.matcher(block.substring(p)); //search for the description on the rest of 
                //the parameter
                if (matcher.find()) {
                    int k = p + matcher.end() - 1;
                    paramName = block.substring(p, k);
                    paramDesc = block.substring(k);
                }
                //build the rest of the html.
                params.append("<tr><td valign=\"top\">&nbsp;&nbsp;&nbsp;");
                params.append(makeCommentColour(paramName));
                params.append("</td><td>");
                params.append(makeCommentColour(" - " + paramDesc));
                params.append("</td></tr>");
                hasParamDoc = true;
            } else {
                String blockTag = convertBlockTag(block);
                if (!blockTag.isEmpty())
                    rest.append(blockTag).append("<br>");
            }
        }

        params.append("</table><p>");

        String result = makeCommentColour(j.getHeader()) + (hasParamDoc ? params.toString() : "<p>") + rest.toString();
        return result;
    }

    private static String convertCodeTokens(String rawString)
    {
        String result = rawString;

        int startIndex;
        while ((startIndex = result.indexOf("{@code")) != -1) {
            int insideOpeningBrackets = 0;
            int endIndex = -1;
            for (int i = startIndex + 6; i < result.length(); i++) {
                if (result.charAt(i) == '{') {
                    insideOpeningBrackets++;
                }
                else if (result.charAt(i) == '}') {
                    if (insideOpeningBrackets != 0) {
                        insideOpeningBrackets--;
                    }
                    else {
                        endIndex = i;
                        break;
                    }
                }
            }
            result = result.substring(0, startIndex) + "<code>" + result.substring(startIndex + 6, endIndex) + " </code>" + result.substring(endIndex + 1);
        }
        return result;
    }

    private static String makeCommentColour(String text)
    {
        return "<font color='#994400'>" + text + "</font>";
    }

    /**
     * For a set of text lines representing block tags in a a javadoc comment,
     * with some block tags potentially flowing over more than one line, return
     * a list of Strings corresponding to each block tag with its complete text.
     */
    private static List<String> getBlockTags(String[] lines)
    {
        LinkedList<String> blocks = new LinkedList<>();
        StringBuilder cur = new StringBuilder();
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("@")) {
                if (cur.length() > 0) {
                    blocks.addLast(cur.toString());
                }
                cur = new StringBuilder(line.substring(1));
            } else {
                //If it doesn't start with an at, it's part of the previous tag
                cur.append(" ").append(line);
            }
        }
        blocks.addLast(cur.toString());
        return blocks;
    }

    private static String convertBlockTag(String block)
    {
        int spaceIndex = block.indexOf(' ');
        if (spaceIndex < 0) {
            return "";
        }
        // block.substring(0, spaceIndex) returns the Tag Name
        // block.substring(spaceIndex) returns the Description
        return "<b>" + block.substring(0, spaceIndex) + "</b> - " + makeCommentColour(block.substring(spaceIndex));
    }

    public static String escapeAngleBrackets(String sig)
    {
        return sig.replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Blanks comments and String literals in a java code portion, replacing the content of the comment by the a repetition of specified character.
     *
     * @param codeStr The code for which comments should be blanked
     * @param obfChar The character chosen to blank the content of the comments
     * @return the blanked code.
     */
    @OnThread(Tag.FXPlatform)
    public static String blankCodeCommentsAndStringLiterals(String codeStr, char obfChar)
    {
        JavaLexer l = new JavaLexer(new StringReader(codeStr));
        StringBuilder sb = new StringBuilder();
        int currReaderPosition = 0;
        LocatableToken lastToken = null;
        for (LocatableToken t = l.nextToken(); t.getType() != JavaTokenTypes.EOF; t = l.nextToken())
        {
            //save the last token (cf after the for loop)
            lastToken = t;

            // Because the parser ignores spaces between tokens, we need preserve these (to match positions)
            // and fill up with spaces when there is a mismatch between the current reading position and the token
            if (t.getPosition() > currReaderPosition)
            {
                sb.append(" ".repeat(t.getPosition() - currReaderPosition));
            }
            currReaderPosition = t.getEndPosition();


            int tokenStartOffset, tokenEndOffset;
            switch (t.getType())
            {
                case JavaTokenTypes.ML_COMMENT:
                    tokenStartOffset = 2; //for /*
                    tokenEndOffset = 2; //for */
                    break;
                case JavaTokenTypes.SL_COMMENT:
                    tokenStartOffset = 2; //for //
                    tokenEndOffset = 0; //none
                    break;
                case JavaTokenTypes.STRING_LITERAL:
                    tokenStartOffset = 1; //for "
                    tokenEndOffset = 1; //for "
                    break;
                case JavaTokenTypes.CHAR_LITERAL:
                    tokenStartOffset = 1; //for '
                    tokenEndOffset = 1; //for '
                    // break;
                default:
                    // if we are in a case we do not need to blank,
                    // we just keep append the token in the StringBuilder
                    // and move to the next token.
                    sb.append(codeStr, t.getPosition(), t.getPosition() + t.getLength());
                    continue;
            }

            // At this point we need to blank the code, preserving the token indicators (i.e. "/*")
            sb.append(codeStr.substring(t.getPosition(), t.getPosition() + tokenStartOffset)
                + String.valueOf(obfChar).repeat(t.getLength() - tokenStartOffset - tokenEndOffset)
                + codeStr.substring(t.getEndPosition() - tokenEndOffset, t.getEndPosition()));
        }

        //trailing spaces: if there are more blank spaces after the last token fill up the string
        //with spaces to maintain the same string size.
        if (lastToken != null && (lastToken.getEndPosition() < codeStr.length() - 1))
        {
            sb.append(" ".repeat(codeStr.length() - lastToken.getEndPosition()));
        }

        return sb.toString();
    }
}
