/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2014,2016  Michael Kolling and John Rosenberg 
 
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
import bluej.parser.entity.PositionedResolver;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.UnresolvedArray;
import bluej.parser.entity.UnresolvedEntity;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.JavaParentNode;
import bluej.parser.nodes.MethodNode;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;
import bluej.pkgmgr.Package;
import bluej.utility.JavaNames;

/**
 * The main BlueJ parser, which extracts various information from source code including:
 * <ul>
 * <li> The name of the class
 * <li> The superclass
 * <li> Any implemented interfaces
 * <li> Constructor and method signatures, including parameter names and javadoc comments
 * </ul>
 * 
 * <p>For most of the useful information that the InfoParser discovers, it needs to resolve
 * names against an EntityResolver which must be supplied. If no resolver is supplied the InfoParser
 * does little other than check for parse failure.
 * 
 * @author Davin McCall
 */
public class InfoParser extends EditorParser
{
    protected String targetPkg;
    protected ClassInfo info;
    private int classLevel = 0; // number of nested classes
    private boolean isPublic;
    private boolean isAbstract;
    private String comment;
    private int lastTdType; // last typedef type (TYPEDEF_CLASS, _INTERFACE etc)
    private boolean storeCurrentClassInfo;
    private int arrayCount = 0;
    private boolean methodTypeParams = false;  // Type parameters are for a method

    private List<LocatableToken> lastTypespecToks;
    private boolean modPublic = false;
    private boolean modAbstract = false;
    private List<MethodDesc> methodDescs = new LinkedList<MethodDesc>();
    private MethodDesc currentMethod;
    
    private JavaEntity superclassEntity;
    
    private List<JavaEntity> interfaceEntities;
    //private List<Selection> interfaceSelections;
    
    /** Represents a method description */
    class MethodDesc
    {
        String name;
        JavaEntity returnType; // null for constructors
        List<JavaEntity> paramTypes;
        String paramNames; // space separated list
        String javadocText;
    }
    
    /** Represents an unresolved value identifier expression */
    class UnresolvedVal
    {
        List<LocatableToken> components;
        JavaParentNode resolver;
        Reflective accessSource;
        int accessPosition;
    }
    
    private List<JavaEntity> typeReferences = new LinkedList<JavaEntity>();
    private List<UnresolvedVal> valueReferences = new LinkedList<UnresolvedVal>();
    private UnresolvedVal currentUnresolvedVal;
    
    private boolean gotExtends; // next type spec is the superclass/superinterfaces
    private boolean gotImplements; // next type spec(s) are interfaces
    private List<Selection> interfaceSelections;
    private Selection lastCommaSelection;

    protected boolean hadError;

    private LocatableToken pkgLiteralToken;
    private List<LocatableToken> packageTokens;
    private LocatableToken pkgSemiToken;

    /**
     * Construct an InfoParser which reads Java source using the given reader, and resolves
     * reference via the given resolver.
     */
    public InfoParser(Reader r, EntityResolver resolver)
    {
        super(r, resolver);
    }

    /**
     * Attempt to parse the specified source file. Returns null if the file could not be parsed.
     */
    public static ClassInfo parse(File f) throws FileNotFoundException
    {
        return parse(f, new ClassLoaderResolver(InfoParser.class.getClassLoader()));
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
        InfoParser infoParser = null;
        infoParser = new InfoParser(r, resolver);
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
        
        refloop:
        for (UnresolvedVal val: valueReferences) {
            Iterator<LocatableToken> i = val.components.iterator();
            String name = i.next().getText();
            JavaEntity entity = val.resolver.getValueEntity(name, val.accessSource, val.accessPosition);
            if (entity != null && entity.resolveAsValue() != null) {
                continue refloop;
            }
            while (entity != null && i.hasNext()) {
                TypeEntity typeEnt = entity.resolveAsType();
                if (typeEnt != null && ! typeEnt.getType().isPrimitive()) {
                    addTypeReference(entity.getType());
                }
                entity = entity.getSubentity(i.next().getText(), val.accessSource);
                if (entity != null && entity.resolveAsValue() != null) {
                    continue refloop;
                }
            }
            if (! i.hasNext() && entity != null) {
                TypeEntity typeEnt = entity.resolveAsType();
                if (typeEnt != null && ! typeEnt.getType().isPrimitive()) {
                    addTypeReference(entity.getType());
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
    
    @Override
    protected void error(String msg, int beginLine, int beginColumn, int endLine, int endColumn)
    {
        hadError = true;
        // Just try and recover.
    }

    @Override
    protected void beginTypeBody(LocatableToken token)
    {
        super.beginTypeBody(token);
        classLevel++;
        gotExtends = false;
        gotImplements = false;
    }
    
    @Override
    protected void endTypeBody(LocatableToken token, boolean included)
    {
        super.endTypeBody(token, included);
        classLevel--;
    }
    
    @Override
    protected void gotTypeSpec(List<LocatableToken> tokens)
    {
        lastTypespecToks = tokens;
        super.gotTypeSpec(tokens);
        
        // Dependency tracking
        int tokpos = lineColToPosition(tokens.get(0).getLine(), tokens.get(0).getColumn());
        int topOffset = getTopNodeOffset();
        EntityResolver resolver = new PositionedResolver(scopeStack.peek(), tokpos - topOffset);
        
        JavaEntity tentity = ParseUtils.getTypeEntity(resolver, currentQuerySource(), tokens);
        if (tentity != null && ! gotExtends && ! gotImplements) {
            typeReferences.add(tentity);
        }
        
        boolean isSuper = storeCurrentClassInfo && gotExtends && !info.isInterface();
        boolean isInterface = storeCurrentClassInfo && (gotImplements ||
                (info.isInterface() && gotExtends));

        if (isSuper) {
            // The list of tokens gives us the name of the class that we extend
            superclassEntity = ParseUtils.getTypeEntity(scopeStack.get(0), null, tokens);
            info.setSuperclass(""); // this will be corrected when the type is resolved
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
            JavaEntity interfaceEnt = ParseUtils.getTypeEntity(scopeStack.get(0), null, tokens);
            if (interfaceEnt != null) {
                interfaceEntities.add(interfaceEnt);
            }
            if (tokenStream.LA(1).getType() == JavaTokenTypes.COMMA) {
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

    @Override
    protected void gotMethodTypeParamsBegin()
    {
        super.gotMethodTypeParamsBegin();
        methodTypeParams = true;
    }
    
    @Override
    protected void gotTypeParam(LocatableToken idToken)
    {
        super.gotTypeParam(idToken);
        if (storeCurrentClassInfo && !methodTypeParams && classLevel == 0) {
            info.addTypeParameterText(idToken.getText());
            info.setTypeParametersSelection(getSelection(idToken));
        }
    }
    
    @Override
    protected void endMethodTypeParams()
    {
        super.endMethodTypeParams();
        methodTypeParams = false;
    }
    
    @Override
    protected void gotTypeParamBound(List<LocatableToken> tokens)
    {
        super.gotTypeParamBound(tokens);
        JavaEntity ent = ParseUtils.getTypeEntity(scopeStack.peek(), currentQuerySource(), tokens);
        if (ent != null) {
            typeReferences.add(ent);
        }
    }
    
    @Override
    protected void gotIdentifier(LocatableToken token)
    {
        gotCompoundIdent(token);
        valueReferences.add(currentUnresolvedVal);
    }
    
    @Override
    protected void gotCompoundIdent(LocatableToken token)
    {
        currentUnresolvedVal = new UnresolvedVal();
        currentUnresolvedVal.components = new LinkedList<LocatableToken>();
        currentUnresolvedVal.components.add(token);
        currentUnresolvedVal.resolver = scopeStack.peek();
        currentUnresolvedVal.accessSource = currentQuerySource();
        int tokenPosition = lineColToPosition(token.getLine(), token.getColumn());
        currentUnresolvedVal.accessPosition = tokenPosition - getTopNodeOffset();
    }
    
    @Override
    protected void gotCompoundComponent(LocatableToken token)
    {
        super.gotCompoundComponent(token);
        currentUnresolvedVal.components.add(token);
    }
    
    @Override
    protected void completeCompoundValue(LocatableToken token)
    {
        super.completeCompoundValue(token);
        currentUnresolvedVal.components.add(token);
        valueReferences.add(currentUnresolvedVal);
    }
    
    @Override
    protected void completeCompoundClass(LocatableToken token)
    {
        super.completeCompoundClass(token);
        List<LocatableToken> components = currentUnresolvedVal.components;
        components.add(token);
        Iterator<LocatableToken> i = components.iterator();
        
        int tokpos = lineColToPosition(token.getLine(), token.getColumn());
        int offset = tokpos - getTopNodeOffset();
        
        JavaEntity entity = UnresolvedEntity.getEntity(new PositionedResolver(scopeStack.peek(), offset),
                i.next().getText(), currentQuerySource());
        while (entity != null && i.hasNext()) {
            entity = entity.getSubentity(i.next().getText(), currentQuerySource());
        }
        if (entity != null) {
            typeReferences.add(entity);
        }
    }
    
    @Override
    protected void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken)
    {
        super.gotMethodDeclaration(token, hiddenToken);
        String lastComment = (hiddenToken != null) ? hiddenToken.getText() : null;
        currentMethod = new MethodDesc();
        currentMethod.returnType =  ((MethodNode) scopeStack.peek()).getReturnType();
        currentMethod.name = token.getText();
        currentMethod.paramNames = "";
        currentMethod.paramTypes = new LinkedList<JavaEntity>();
        currentMethod.javadocText = lastComment;
        arrayCount = 0;
    }

    @Override
    protected void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken)
    {
        super.gotConstructorDecl(token, hiddenToken);
        String lastComment = (hiddenToken != null) ? hiddenToken.getText() : null;
        currentMethod = new MethodDesc();
        currentMethod.name = token.getText();
        currentMethod.paramNames = "";
        currentMethod.paramTypes = new LinkedList<JavaEntity>();
        currentMethod.javadocText = lastComment;
        arrayCount = 0;
    }

    @Override
    protected void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken)
    {
        super.gotMethodParameter(token, ellipsisToken);
        if (currentMethod != null && lastTypespecToks != null) {
            currentMethod.paramNames += token.getText() + " ";
            JavaEntity ptype = ParseUtils.getTypeEntity(scopeStack.peek(),
                    currentQuerySource(), lastTypespecToks);
            while (arrayCount > 0) {
                ptype = new UnresolvedArray(ptype);
                arrayCount--;
            }
            if (ellipsisToken != null) {
                ptype = new UnresolvedArray(ptype);                
            }
            currentMethod.paramTypes.add(ptype);
        }
    }
    
    @Override
    protected void gotArrayDeclarator()
    {
        super.gotArrayDeclarator();
        arrayCount++;
    }

    @Override
    protected void gotAllMethodParameters()
    {
        super.gotAllMethodParameters();
        if (storeCurrentClassInfo && classLevel == 1) {
            methodDescs.add(currentMethod);
            currentMethod = null;
        }
    }

    @Override
    protected void gotTypeDef(LocatableToken firstToken, int tdType)
    {
        isPublic = modPublic;
        isAbstract = modAbstract;
        comment = firstToken.getHiddenBefore() == null ? "" : firstToken.getHiddenBefore().getText();
        super.gotTypeDef(firstToken, tdType);
        lastTdType = tdType;
    }

    @Override
    protected void gotTypeDefName(LocatableToken nameToken)
    {
        super.gotTypeDefName(nameToken);
        gotExtends = false; // haven't seen "extends ..." yet
        gotImplements = false;
        if (classLevel == 0) {
            if (info == null || isPublic && !info.foundPublicClass()) {
                info = new ClassInfo();
                info.setName(nameToken.getText(), isPublic);
                info.setEnum(lastTdType == TYPEDEF_ENUM);
                info.setInterface(lastTdType == TYPEDEF_INTERFACE);
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
    }

    @Override
    protected void beginTypeDefExtends(LocatableToken extendsToken)
    {
        super.beginTypeDefExtends(extendsToken);
        if (classLevel == 0 && storeCurrentClassInfo) {
            gotExtends = true;
            SourceLocation extendsStart = info.getExtendsInsertSelection().getStartLocation();
            int extendsEndCol = tokenStream.LA(1).getColumn();
            int extendsEndLine = tokenStream.LA(1).getLine();
            if (extendsStart.getLine() == extendsEndLine) {
                info.setExtendsReplaceSelection(new Selection(extendsEndLine, extendsStart.getColumn(), extendsEndCol - extendsStart.getColumn()));
            }
            else {
                info.setExtendsReplaceSelection(new Selection(extendsEndLine, extendsStart.getColumn(), extendsToken.getEndColumn() - extendsStart.getColumn()));
            }
            info.setExtendsInsertSelection(null);
            
            if (info.isInterface()) {
                interfaceSelections = new LinkedList<Selection>();
                interfaceSelections.add(getSelection(extendsToken));
                interfaceEntities = new LinkedList<JavaEntity>();
            }
        }
    }

    @Override
    protected void beginTypeDefImplements(LocatableToken implementsToken)
    {
        super.beginTypeDefImplements(implementsToken);
        if (classLevel == 0 && storeCurrentClassInfo) {
            gotExtends = false;
            gotImplements = true;
            interfaceSelections = new LinkedList<Selection>();
            interfaceSelections.add(getSelection(implementsToken));
            interfaceEntities = new LinkedList<JavaEntity>();
        }
    }

    @Override
    protected void beginPackageStatement(LocatableToken token)
    {
        super.beginPackageStatement(token);
        pkgLiteralToken = token;
    }

    @Override
    protected void gotPackage(List<LocatableToken> pkgTokens)
    {
        super.gotPackage(pkgTokens);
        packageTokens = pkgTokens;
    }

    @Override
    protected void gotPackageSemi(LocatableToken token)
    {
        super.gotPackageSemi(token);
        pkgSemiToken = token;
    }

    @Override
    protected void gotModifier(LocatableToken token)
    {
        super.gotModifier(token);
        if (token.getType() == JavaTokenTypes.LITERAL_public) {
            modPublic = true;
        }
        else if (token.getType() == JavaTokenTypes.ABSTRACT) {
            modAbstract = true;
        }
    }
    
    @Override
    protected void modifiersConsumed()
    {
        modPublic = false;
        modAbstract = false;
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
}
