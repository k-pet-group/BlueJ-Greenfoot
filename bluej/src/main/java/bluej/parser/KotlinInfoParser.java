/*
 This file is part of the BlueJ program. 
 Copyright (C) 2009,2010,2011,2012,2014,2016,2022,2024  Michael Kolling and John Rosenberg

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageResolver;
import bluej.parser.entity.TypeEntity;
import bluej.parser.lexer.KotlinTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.JavaParentNode;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;
import bluej.pkgmgr.Package;
import bluej.utility.JavaNames;
import bluej.parser.ParseUtils;

/**
 * The main BlueJ parser for Kotlin, which extracts various information from source code including:
 * <ul>
 * <li> The name of the class
 * <li> The superclass
 * <li> Any implemented interfaces
 * <li> Constructor and method signatures, including parameter names and kdoc comments
 * </ul>
 * 
 * <p>For most of the useful information that the KotlinInfoParser discovers, it needs to resolve
 * names against an EntityResolver which must be supplied. If no resolver is supplied the KotlinInfoParser
 * does little other than check for parse failure.
 * 
 */
public class KotlinInfoParser extends KotlinParser
{
    protected String targetPkg;
    protected ClassInfo info;
    private int classLevel = 0; // number of nested classes
    private boolean isPublic;
    private boolean isAbstract;
    private String comment;
    private boolean storeCurrentClassInfo;
    private int arrayCount = 0;
    private int lastTdType = 0; // last typedef type (0 for class, 1 for interface)

    private List<LocatableToken> lastTypespecToks;
    private boolean modPublic = false;
    private boolean modAbstract = false;
    private List<MethodDesc> methodDescs = new LinkedList<MethodDesc>();
    private MethodDesc currentMethod;

    private JavaEntity superclassEntity;

    private List<JavaEntity> interfaceEntities;

    /** Represents a method description */
    class MethodDesc
    {
        String name;
        JavaEntity returnType; // null for constructors
        List<JavaEntity> paramTypes;
        String paramNames; // space separated list
        String javadocText;
    }

    private List<JavaEntity> typeReferences = new LinkedList<JavaEntity>();

    private boolean gotExtends; // next type spec is the superclass/superinterfaces
    private boolean gotImplements; // next type spec(s) are interfaces
    private List<Selection> interfaceSelections;
    private Selection lastCommaSelection;

    private Stack<JavaParentNode> scopeStack = new Stack<JavaParentNode>();

    protected boolean hadError;

    private LocatableToken pkgLiteralToken;
    private List<LocatableToken> packageTokens;
    private LocatableToken pkgSemiToken;

    /**
     * Construct a KotlinInfoParser which reads Kotlin source using the given reader, and resolves
     * reference via the given resolver.
     */
    public KotlinInfoParser(Reader r, EntityResolver resolver)
    {
        super(r);
        // Note: We don't use the resolver in this simplified implementation
    }

    /**
     * Attempt to parse the specified source file. Returns null if the file could not be parsed.
     */
    public static ClassInfo parse(File f) throws FileNotFoundException
    {
        return parse(f, new ClassLoaderResolver(KotlinInfoParser.class.getClassLoader()));
    }

    /**
     * Attempt to parse the specified source file, and resolve references via the specified
     * resolver. Returns null if the file could not be parsed.
     */
    public static ClassInfo parse(File f, EntityResolver resolver) throws FileNotFoundException
    {
        FileInputStream fis = new FileInputStream(f);
        ClassInfo info = parse(new BufferedReader(new InputStreamReader(fis)), resolver, null);
        try {
            fis.close();
        }
        catch (IOException ioe) {}
        return info;
    }

    /**
     * Attempt to parse the specified source file, and resolve references via the specified
     * package (and its project). Returns null if the file could not be parsed.
     */
    @OnThread(Tag.FXPlatform)
    public static ClassInfo parseWithPkg(File f, Package pkg) throws FileNotFoundException
    {
        FileInputStream fis = new FileInputStream(f);
        EntityResolver resolver = new PackageResolver(pkg.getProject().getEntityResolver(),
                pkg.getQualifiedName());
        Reader reader = new InputStreamReader(fis, pkg.getProject().getProjectCharset());
        reader = new BufferedReader(reader);
        ClassInfo info = parse(reader, resolver, pkg.getQualifiedName());
        try {
            fis.close();
        }
        catch (IOException ioe) {}
        return info;
    }

    /**
     * Attempt to parse the specified source file, and resolve references via the specified
     * resolver. The source should be assumed to reside in the specified package.
     * Returns null if the source could not be parsed.
     */
    @OnThread(Tag.FXPlatform)
    public static ClassInfo parse(Reader r, EntityResolver resolver, String targetPkg)
    {
        KotlinInfoParser infoParser = new KotlinInfoParser(r, resolver);
        infoParser.targetPkg = targetPkg;
        infoParser.parseCU();

        if (infoParser.info != null) {
            infoParser.info.setParseError(infoParser.hadError);
            infoParser.resolveComments();
            return infoParser.info;
        }
        return null;
    }

    /**
     * All type references and method declarations are unresolved after parsing.
     * Call this method to resolve them.
     */
    @OnThread(Tag.FXPlatform)
    public void resolveComments()
    {
        resolveMethodTypes();

        // Now also resolve references
        for (JavaEntity entity: typeReferences) {
            entity = entity.resolveAsType();
            if (entity != null) {
                JavaType etype = entity.getType();
                if (! etype.isPrimitive()) {
                    addTypeReference(etype);
                }
            }
        }

        if (superclassEntity != null) {
            superclassEntity = superclassEntity.resolveAsType();
            if (superclassEntity != null) {
                JavaType sceType = superclassEntity.getType();
                GenTypeClass scecType = sceType.asClass();
                if (scecType != null) {
                    info.setSuperclass(scecType.getReflective().getName());
                }
            }
        }

        if (interfaceEntities != null && ! interfaceEntities.isEmpty()) {
            for (JavaEntity ifaceEnt : interfaceEntities) {
                TypeEntity iEnt = ifaceEnt.resolveAsType();
                if (iEnt != null) {
                    GenTypeClass iType = iEnt.getType().asClass();
                    if (iType != null) {
                        info.addImplements(iType.getReflective().getName());
                        continue;
                    }
                }
                info.addImplements(""); // gap filler
            }
        }
    }

    /**
     * Resolve the method parameter and return types to their fully qualified types.
     */
    @OnThread(Tag.FXPlatform)
    protected void resolveMethodTypes()
    {
        methodLoop:
        for (MethodDesc md : methodDescs) {
            // Build the method signature
            String methodSig;

            if (md.returnType != null) {
                md.returnType = md.returnType.resolveAsType();
                if (md.returnType == null) {
                    continue;
                }
                methodSig = getTypeString(md.returnType) + " " + md.name + "(";
            }
            else {
                // constructor
                methodSig = md.name + "(";
            }

            Iterator<JavaEntity> i = md.paramTypes.iterator();
            while (i.hasNext()) {
                JavaEntity paramEnt = i.next();
                if (paramEnt == null) {
                    continue methodLoop;
                }
                TypeEntity paramType = paramEnt.resolveAsType();
                if (paramType == null) {
                    continue methodLoop;
                }
                methodSig += getTypeString(paramType);
                if (i.hasNext()) {
                    methodSig += ", ";
                }
            }

            methodSig += ")";
            md.paramNames = md.paramNames.trim();
            info.addComment(methodSig, md.javadocText, md.paramNames);
        }
    }

    /**
     * Get a String describing a type as suitable for writing to the ctxt file.
     * This is the qualified, erased type name, with "." rather than "$" separating
     * inner class names from the outer class names, and the package name (if it
     * matches the target package) stripped.
     */
    private String getTypeString(JavaEntity entity)
    {
        String erasedType = entity.getType().getErasedType().toString();
        if (targetPkg != null && targetPkg.length() != 0) {
            if (erasedType.startsWith(targetPkg + ".")) {
                erasedType = erasedType.substring(targetPkg.length() + 1);
            }
        }
        return erasedType.replace("$", ".");
    }

    /**
     * Add a reference to a type, and recursively process its type arguments (if any)
     */
    private void addTypeReference(JavaType type)
    {
        GenTypeClass ctype = type.asClass();
        if (ctype != null) {
            addTypeReference(ctype.getErasedType().toString());
            List<? extends GenTypeParameter> plist = ctype.getTypeParamList();
            // Process type arguments:
            for (GenTypeParameter param : plist) {
                GenTypeSolid sparam = param.asSolid();
                if (sparam != null) {
                    addTypeReference(sparam);
                }
                else {
                    // primitive or wildcard type
                    // (primitives are technically not allowed).
                    JavaType upperBound = param.getUpperBound();
                    JavaType lowerBound = param.getLowerBound();
                    if (upperBound != null) addTypeReference(upperBound);
                    if (lowerBound != null) addTypeReference(lowerBound);
                }
            }
        }
    }

    /**
     * Add a reference to a type (fully-qualified type name) to the information to return.
     */
    private void addTypeReference(String typeString)
    {

        // Handle Kotlin primitive types
        if (isKotlinPrimitiveType(typeString)) {
            info.addUsed(typeString);
            return;
        }

        String prefix = JavaNames.getPrefix(typeString);
        if (prefix.equals(targetPkg)) {
            String name = JavaNames.getBase(typeString);
            int dollar = name.indexOf('$');
            if (dollar != -1) {
                name = name.substring(0, dollar);
            }
            info.addUsed(name);
        }
    }

    /**
     * Check if the given type name is a Kotlin primitive type.
     */
    private boolean isKotlinPrimitiveType(String typeName)
    {
        return typeName.equals("Int") || 
               typeName.equals("Long") || 
               typeName.equals("Short") || 
               typeName.equals("Byte") || 
               typeName.equals("Float") || 
               typeName.equals("Double") || 
               typeName.equals("Boolean") || 
               typeName.equals("Char") || 
               typeName.equals("String") || 
               typeName.equals("Unit") || 
               typeName.equals("Any") || 
               typeName.equals("Nothing");
    }

    protected void error(String msg, int beginLine, int beginColumn, int endLine, int endColumn)
    {
        hadError = true;
        // Just try and recover.
    }

    @Override
    public void beginPackageStatement(LocatableToken token)
    {
        super.beginPackageStatement(token);
        pkgLiteralToken = token;
    }

    @Override
    public void gotPackage(List<LocatableToken> pkgTokens)
    {
        super.gotPackage(pkgTokens);
        packageTokens = pkgTokens;
    }

    @Override
    public void gotPackageSemi(LocatableToken token)
    {
        super.gotPackageSemi(token);
        pkgSemiToken = token;
    }

    @Override
    public void gotModifier(LocatableToken token)
    {
        super.gotModifier(token);
        if (token.getType() == KotlinTokenTypes.LITERAL_public) {
            modPublic = true;
        }
        else if (token.getType() == KotlinTokenTypes.ABSTRACT) {
            modAbstract = true;
        }
    }

    @Override
    public void modifiersConsumed()
    {
        modPublic = false;
        modAbstract = false;
    }

    @Override
    public void beginTypeBody(LocatableToken token)
    {
        classLevel++;
        gotExtends = false;
        gotImplements = false;
    }

    @Override
    public void endTypeBody(LocatableToken token, boolean included)
    {
        classLevel--;
    }

    @Override
    public void gotIdentifier(LocatableToken token)
    {
        super.gotIdentifier(token);

        // When an identifier is encountered, check if it's a class name
        if (info != null && token.getText() != null) {
            // Add the identifier to the "used" list if it's a class name
            // This will handle both direct class references and field accesses
            addTypeReference(token.getText());
        }
    }

    @Override
    public void gotPropertyType(List<LocatableToken> tokens)
    {
        super.gotPropertyType(tokens);

        // When a property type is encountered, add it to the "used" list
        if (info != null && tokens != null && !tokens.isEmpty()) {
            // Process each token individually
            for (LocatableToken token : tokens) {
                if (token.getType() == KotlinTokenTypes.IDENT && !isKotlinPrimitiveType(token.getText())) {
                    // Add the identifier to the "used" list
                    info.addUsed(token.getText());
                }
            }
        }
    }

    @Override
    public void gotTypeDef(LocatableToken firstToken, int tdType)
    {
        isPublic = modPublic;
        isAbstract = modAbstract;
        lastTdType = tdType;
        comment = firstToken.getHiddenBefore() == null ? "" : firstToken.getHiddenBefore().getText();
    }

    @Override
    public void gotTypeDefName(LocatableToken nameToken)
    {
        gotExtends = false; // haven't seen "extends ..." yet
        gotImplements = false;

        // Process any class, not just top-level ones
        if (info == null || isPublic && !info.foundPublicClass()) {
            info = new ClassInfo();
            info.setName(nameToken.getText(), isPublic);
            info.setInterface(lastTdType == 1); // 1 for interface
            info.setAbstract(isAbstract);
            info.addComment(info.getName(), comment, null);
            Selection insertSelection = new Selection(nameToken.getLine(), nameToken.getEndColumn());
            info.setExtendsInsertSelection(insertSelection);
            info.setImplementsInsertSelection(insertSelection);
            if (pkgSemiToken != null) {
                info.setPackageSelections(getSelection(pkgLiteralToken), getSelection(packageTokens),
                        joinTokens(packageTokens), getSelection(pkgSemiToken));
            }
            storeCurrentClassInfo = true;
        }
        else {
            storeCurrentClassInfo = false;
        }
    }

    @Override
    public void beginTypeDefExtends(LocatableToken extendsToken)
    {
        if (classLevel == 0 && storeCurrentClassInfo) {
            gotExtends = true;
            interfaceSelections = new LinkedList<Selection>();
            interfaceSelections.add(getSelection(extendsToken));
            interfaceEntities = new LinkedList<JavaEntity>();
        }
    }

    @Override
    public void gotTypeSpec(List<LocatableToken> tokens)
    {
        lastTypespecToks = tokens;

        boolean isSuper = storeCurrentClassInfo && gotExtends && !info.isInterface();
        boolean isInterface = storeCurrentClassInfo && (gotImplements ||
                (info.isInterface() && gotExtends));

        if (isSuper) {
            // The list of tokens gives us the name of the class that we extend
            // For simplicity, we'll just use the token text as the superclass name
            String superclassName = joinTokens(tokens);
            info.setSuperclass(superclassName);
            Selection superClassSelection = getSelection(tokens);
            info.setSuperReplaceSelection(superClassSelection);
            info.setImplementsInsertSelection(new Selection(superClassSelection.getEndLine(),
                    superClassSelection.getEndColumn()));
        }
        else if (isInterface) {
            Selection interfaceSel = getSelection(tokens);
            if (lastCommaSelection != null) {
                lastCommaSelection.extendEnd(interfaceSel.getLine(), interfaceSel.getColumn());
                interfaceSelections.add(lastCommaSelection);
                lastCommaSelection = null;
            }
            interfaceSelections.add(interfaceSel);
            // For simplicity, we'll just use the token text as the interface name
            String interfaceName = joinTokens(tokens);
            info.addImplements(interfaceName);

            if (tokenStream.LA(1).getType() == KotlinTokenTypes.COMMA) {
                lastCommaSelection = getSelection(tokenStream.LA(1));
            }
            else {
                info.setInterfaceSelections(interfaceSelections);
                if (! info.isInterface()) {
                    info.setImplementsInsertSelection(new Selection(interfaceSel.getEndLine(),
                            interfaceSel.getEndColumn()));
                }
                else {
                    info.setExtendsInsertSelection(new Selection(interfaceSel.getEndLine(),
                            interfaceSel.getEndColumn()));
                }
            }
        }
    }

    private Selection getSelection(LocatableToken token)
    {
        if (token.getLine() <= 0 || token.getColumn() <= 0) {
            System.out.println("" + token);
        }
        if (token.getLength() < 0) {
            System.out.println("Bad length: " + token.getLength());
            System.out.println("" + token);
        }
        return new Selection(token.getLine(), token.getColumn(), token.getLength());
    }

    private Selection getSelection(List<LocatableToken> tokens)
    {
        Iterator<LocatableToken> i = tokens.iterator();
        Selection s = getSelection(i.next());
        if (i.hasNext()) {
            LocatableToken last = i.next();
            while (i.hasNext()) {
                last = i.next();
            }
            s.combineWith(getSelection(last));
        }
        return s;
    }

    /**
     * Join a sequence of tokens together to form a string.
     */
    private String joinTokens(List<LocatableToken> tokens)
    {
        StringBuffer r = new StringBuffer();
        for (LocatableToken token : tokens) {
            r.append(token.getText());
        }
        return r.toString();
    }
}
