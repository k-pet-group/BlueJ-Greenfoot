/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2014  Michael Kolling and John Rosenberg 
 
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

import bluej.debugger.gentype.Reflective;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
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
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class in a copy of InfoParser. However, it is tweaked for performance
 * (values are not parsed).
 *
 * @author Fabio Hedayioglu
 * @author Davin McCall
 */
public class JavadocParser extends EditorParser
{

    private String targetPkg;
    private ClassInfo info;
    private int classLevel = 0; // number of nested classes
    private boolean isPublic;
    private boolean isAbstract;
    private int lastTdType; // last typedef type (TYPEDEF_CLASS, _INTERFACE etc)
    private boolean storeCurrentClassInfo;
    private int arrayCount = 0;

    private List<LocatableToken> lastTypespecToks;
    private boolean modPublic = false;
    private boolean modAbstract = false;
    private List<MethodDesc> methodDescs = new LinkedList<MethodDesc>();
    private MethodDesc currentMethod;

    private JavaEntity superclassEntity;

    private List<JavaEntity> interfaceEntities;

    private List<Selection> interfaceSelections;

    /**
     * Represents a method description
     */
    class MethodDesc
    {

        String name;
        JavaEntity returnType; // null for constructors
        List<JavaEntity> paramTypes;
        String paramNames; // space separated list
        String javadocText;
    }

    /**
     * Represents an unresolved value identifier expression
     */
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

    private Selection lastCommaSelection;

    private boolean hadError;

    private LocatableToken pkgLiteralToken;
    private List<LocatableToken> packageTokens;
    private LocatableToken pkgSemiToken;

    /**
     * Construct an InfoParser which reads Java source using the given reader,
     * and resolves reference via the given resolver.
     */
    public JavadocParser(Reader r, EntityResolver resolver)
    {
        super(r, resolver);
    }

    /**
     * Attempt to parse the specified source file, and resolve references via
     * the specified resolver. The source should be assumed to reside in the
     * specified package. Returns null if the source could not be parsed.
     */
    public static ClassInfo parse(Reader r, EntityResolver resolver, String targetPkg)
    {
        JavadocParser javadocParser = null;
        javadocParser = new JavadocParser(r, resolver);
        javadocParser.targetPkg = targetPkg;
        javadocParser.parseCU();

        if (javadocParser.info != null) {
            javadocParser.info.setParseError(javadocParser.hadError);
            javadocParser.resolveComments();
            return javadocParser.info;
        }
        return null;
    }

    /**
     * All type references and method declarations are unresolved after parsing.
     * Call this method to resolve them.
     */
    public void resolveComments()
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
            } else {
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
     * This is the qualified, erased type name, with "." rather than "$"
     * separating inner class names from the outer class names, and the package
     * name (if it matches the target package) stripped.
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
        if (tentity != null && !gotExtends && !gotImplements) {
            typeReferences.add(tentity);
        }

        boolean isSuper = storeCurrentClassInfo && gotExtends && !info.isInterface();
        boolean isInterface = storeCurrentClassInfo && (gotImplements
                || (info.isInterface() && gotExtends));

        if (isSuper) {
            // The list of tokens gives us the name of the class that we extend
            superclassEntity = ParseUtils.getTypeEntity(scopeStack.get(0), null, tokens);
            info.setSuperclass(""); // this will be corrected when the type is resolved
            Selection superClassSelection = getSelection(tokens);
            info.setSuperReplaceSelection(superClassSelection);
            info.setImplementsInsertSelection(new Selection(superClassSelection.getEndLine(),
                    superClassSelection.getEndColumn()));
        } else if (isInterface) {
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
            } else {
                info.setInterfaceSelections(interfaceSelections);
                if (!info.isInterface()) {
                    info.setImplementsInsertSelection(new Selection(interfaceSel.getEndLine(),
                            interfaceSel.getEndColumn()));
                } else {
                    info.setExtendsInsertSelection(new Selection(interfaceSel.getEndLine(),
                            interfaceSel.getEndColumn()));
                }
            }
        }
    }

    @Override
    protected void gotTypeParam(LocatableToken idToken)
    {
        super.gotTypeParam(idToken);
        info.addTypeParameterText(idToken.getText());
        info.setTypeParametersSelection(getSelection(idToken));
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
        currentMethod.returnType = ((MethodNode) scopeStack.peek()).getReturnType();
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
        if (currentMethod != null) {
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
                Selection insertSelection = new Selection(nameToken.getLine(), nameToken.getEndColumn());
                info.setExtendsInsertSelection(insertSelection);
                info.setImplementsInsertSelection(insertSelection);
                if (pkgSemiToken != null) {
                    info.setPackageSelections(getSelection(pkgLiteralToken), getSelection(packageTokens),
                            joinTokens(packageTokens), getSelection(pkgSemiToken));
                }
                storeCurrentClassInfo = true;
            } else {
                storeCurrentClassInfo = false;
            }
        }
    }

    @Override
    protected void gotTypeDefExtends(LocatableToken extendsToken)
    {
        super.gotTypeDefExtends(extendsToken);
        if (classLevel == 0 && storeCurrentClassInfo) {
            gotExtends = true;
            SourceLocation extendsStart = info.getExtendsInsertSelection().getStartLocation();
            int extendsEndCol = tokenStream.LA(1).getColumn();
            int extendsEndLine = tokenStream.LA(1).getLine();
            if (extendsStart.getLine() == extendsEndLine) {
                info.setExtendsReplaceSelection(new Selection(extendsEndLine, extendsStart.getColumn(), extendsEndCol - extendsStart.getColumn()));
            } else {
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
    protected void gotTypeDefImplements(LocatableToken implementsToken)
    {
        super.gotTypeDefImplements(implementsToken);
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
        } else if (token.getType() == JavaTokenTypes.ABSTRACT) {
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
