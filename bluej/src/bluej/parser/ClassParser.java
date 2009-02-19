/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import antlr.RecognitionException;
import antlr.Token;
import antlr.TokenStream;
import antlr.TokenStreamException;
import antlr.collections.AST;
import bluej.parser.ast.LocatableAST;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaLexer;
import bluej.parser.ast.gen.JavaRecognizer;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.ClassScope;
import bluej.parser.symtab.PackageScope;
import bluej.parser.symtab.Scope;
import bluej.parser.symtab.Selection;
import bluej.utility.Debug;

/**
 * Parse class to get info.
 * 
 * To work "properly" this is a more complicated process which potentially requires parsing
 * multiple source files. However, at the moment we parse a single file at a time. We only
 * create dependencies to existing classes in the same package (as supplied).
 * 
 * @author Davin McCall
 * @version $Id: ClassParser.java 6164 2009-02-19 18:11:32Z polle $
 */
public class ClassParser
{
    // TODO: type parameters for methods and types should be inserted into the scope
    // for the method/type (as a type).
    
    public static ClassInfo parse(File file)
        throws RecognitionException
    {
        return parse(file, null);
    }
    
    public static ClassInfo parse(File file, List packageClasses)
        throws RecognitionException
    {
        FileInputStream fr = null;
        try {
            fr = new FileInputStream(file);
            return parse(new InputStreamReader(fr), packageClasses);
        }
        catch (FileNotFoundException fnfe) {
            throw new RecognitionException();
        }
        finally {
            try {
                if (fr != null)
                    fr.close();
            }
            catch (IOException ioe) {}
        }
    }
    
    public static ClassInfo parse(InputStreamReader ir, List packageClasses)
        throws RecognitionException
    {
        return getClassParser(ir, packageClasses).getInfo();
    }
    
    public static List parseList(InputStreamReader ir, List packageClasses)
        throws RecognitionException
    {
        return getClassParser(ir, packageClasses).getInfoList();
    }
    
    public static ClassParser getClassParser(InputStreamReader ir, List packageClasses)
        throws RecognitionException
    {
    // Debug.message("Parsing file: " + file);
    try {
        // We use a lexer pipeline:
        // First, deal with escaped unicode characters:
        EscapedUnicodeReader eur = new EscapedUnicodeReader(ir);

        // Next create the initial lexer stage
        JavaLexer lexer = new JavaLexer(eur);
        lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");
        lexer.setTabSize(1);
        eur.setAttachedScanner(lexer);
        
        // Finally filter out comments and whitespace
        TokenStream filter = new JavaTokenFilter(lexer);

        // create a parser that reads from the scanner
        JavaRecognizer parser = new JavaRecognizer(filter);
        parser.setASTNodeClass("bluej.parser.ast.LocatableAST");
        
        parser.compilationUnit();
        AST node = parser.getAST();
        
        ClassParser cp = new ClassParser();
        try {
            cp.getClassInfo(node, packageClasses);
            return cp;
        }
        catch (RecognitionException re) {
            throw re;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RecognitionException();
        }
    }
    catch (TokenStreamException tse) {
        throw new RecognitionException();
    }
}

    
    /**
     * Get a selection for the beginning of the given token.
     */
    static Selection tokenBeginSelection(Token t)
    {
        int line = t.getLine();
        int col = t.getColumn();
        return new Selection(line, col);
    }
    
    /**
     * Get a selection for the end of the given token.
     */
    static Selection tokenEndSelection(Token t)
    {
        int line = t.getLine();
        int col = t.getColumn() + t.getText().length();
        return new Selection(line, col);
    }
    
    /****************** instance members ***********************/
    
    private ClassInfo classInfo;
    private List classInfoList = new ArrayList();
    
    private ClassParser()
    {
        // Nothing to do.
    }
    
    public ClassInfo getInfo()
    {
        return classInfo;
    }
    
    public List getInfoList()
    {
        return classInfoList;
    }
    
    public void getClassInfo(AST node, List packageClasses) throws RecognitionException
    {
        if (node == null)
            return;
        
        // package statement
        LocatableAST packageDefNode = null;
        if (node.getType() == JavaTokenTypes.PACKAGE_DEF) {
            packageDefNode = (LocatableAST) node;
            node = node.getNextSibling();
        }
        
        PackageScope packageScope = new PackageScope();
        if (packageClasses != null) {
            Iterator i = packageClasses.iterator();
            while (i.hasNext()) {
                String className = (String) i.next();
                packageScope.addType(className);
            }
        }
        Scope compUnitScope = new Scope(packageScope);
        
        // import statements
        while (node != null) {
            int nodeType = node.getType();
            if (nodeType == JavaTokenTypes.IMPORT || nodeType == JavaTokenTypes.STATIC_IMPORT) {
                // TODO can't yet handle static imports - requires knowledge of other class.
                if (nodeType == JavaTokenTypes.IMPORT) {
                    AST importNode = node.getFirstChild();
                    if (importNode.getType() == JavaTokenTypes.DOT) {
                        AST impFirst = importNode.getFirstChild();
                        AST impSecond = impFirst.getNextSibling();
                        // The import name may be a wildcard - we ignore in that case;
                        // we're only interested in explicit identifiers.
                        if (impSecond.getType() == JavaTokenTypes.IDENT) {
                            String importName = TextParser.combineDotNames(importNode, '.');
                            compUnitScope.addType(importName);
                        }
                    }
                }
            }
            else {
                break;
            }
            node = node.getNextSibling();
        }
        
        while (node != null) {            
            // Class def, children are: MODIFIERS, name, EXTENDS_CLAUSE,
            //                       IMPLEMENTS_CLAUSE, OBJBLOCK
            // Interface def, children are: MODIFIERS, name, EXTENDS_CLAUSE,
            //                       OBJBLOCK
            // ... So the only difference is the absence of IMPLEMENTS_CLAUSE
            // in an interface. Incidentally, each child is present regardless
            // of the presence or absence in the source code.
            
            int ntype = node.getType();
            if (ntype != JavaTokenTypes.CLASS_DEF
                    && ntype != JavaTokenTypes.INTERFACE_DEF
                    && ntype != JavaTokenTypes.ENUM_DEF) {
                node = node.getNextSibling();
                continue;
            }
            
            // Check modifiers - search for "public", "abstract"
            AST cnode = node.getFirstChild();
            boolean isPublic = false;
            boolean isAbstract = false;
            if (cnode != null && cnode.getType() == JavaTokenTypes.MODIFIERS) {
                AST ccnode = cnode.getFirstChild();
                while (ccnode != null) {
                    if (ccnode.getType() == JavaTokenTypes.LITERAL_public)
                        isPublic = true;
                    else if (ccnode.getType() == JavaTokenTypes.ABSTRACT)
                        isAbstract = true;
                    ccnode = ccnode.getNextSibling();
                }
                cnode = cnode.getNextSibling();
            }
            
            // look for class name
            if (cnode != null && cnode.getType() == JavaTokenTypes.IDENT) {
                LocatableAST nameNode = (LocatableAST) cnode;
                String name = nameNode.getText();
                compUnitScope.addType(name);
                ClassInfo info = new ClassInfo();
                info.setName(name, isPublic);
                info.setAbstract(isAbstract);
                info.setInterface(ntype == JavaTokenTypes.INTERFACE_DEF);
                info.setEnum(ntype == JavaTokenTypes.ENUM_DEF);
                
                cnode = cnode.getNextSibling();
                
                // type parameters?
                if (cnode != null && cnode.getType() == JavaTokenTypes.TYPE_PARAMETERS) {
                    LocatableAST tparsNode = (LocatableAST) cnode;
                    // first important token is the opening '<'
                    Token langle = tparsNode.getImportantToken(0);
                    Selection tparsSelection = tokenBeginSelection(langle);
                    Selection endSel = new Selection(tparsNode.getEndLine(), tparsNode.getEndColumn());
                    tparsSelection.combineWith(endSel);
                    info.setTypeParametersSelection(tparsSelection);
                    
                    //getTypeParamString(null);
                    // Also get the type parameter names
                    List tpNames = new ArrayList();
                    AST tpAst = tparsNode.getFirstChild();
                    while (tpAst != null) {
                        if (tpAst.getType() == JavaTokenTypes.IDENT) {
                            tpNames.add(getTypeParamString(tpAst));
                            //tpNames.add(tpAst.getText());
                        }
                        tpAst = tpAst.getNextSibling();
                    }
                    info.setTypeParameterTexts(tpNames);
                    cnode = cnode.getNextSibling();
                }
                
                // extends clause
                if (cnode != null && cnode.getType() == JavaTokenTypes.EXTENDS_CLAUSE) {
                    switch (ntype) {
                        case JavaTokenTypes.INTERFACE_DEF:
                            processIfaceExtendsClause(cnode, nameNode, info);
                            break;
                        default:
                            processClassExtendsClause(cnode, nameNode, info);
                    }
                    cnode = cnode.getNextSibling();
                }
                
                // implements clause.
                if (cnode != null && cnode.getType() == JavaTokenTypes.IMPLEMENTS_CLAUSE) {
                    processClassImplementsClause(cnode, nameNode, info);
                    cnode = cnode.getNextSibling();
                }
                
                // Class/interface body
                if (cnode != null && cnode.getType() == JavaTokenTypes.OBJBLOCK) {
                    processObjBlock(cnode, new ClassScope(info, compUnitScope));
                }
                
                classInfoList.add(info);
                
                // If this is the first class, or it's a public class, store it.
                if (classInfo == null || isPublic)
                    classInfo = info;
            }

            // Go onto next node
            node = node.getNextSibling();
        }
        
        Iterator ci = classInfoList.iterator();
        while (ci.hasNext()) {
            ClassInfo cinfo = (ClassInfo) ci.next();
            Iterator i = packageScope.getReferences().iterator();
            while (i.hasNext()) {
                cinfo.addUsed(i.next().toString());
            }
            if (packageDefNode != null) {
                storePackageInfo(packageDefNode, cinfo);
            }
        }
    }
    
    /**
     * Store the package into the ClassInfo structure.
     * @param packageNode   The AST node representing the package in the package declaration
     * @throws RecognitionException
     */
    private void storePackageInfo(LocatableAST packageNode, ClassInfo info)
    throws RecognitionException
    {
        if (packageNode != null) {
            LocatableAST packageNameNode = (LocatableAST) packageNode.getFirstChild();
            
            String pkgName = getQualifiedName(packageNameNode);
            LocatableToken semi = packageNode.getImportantToken(0);
            
            Selection pkgSel = new Selection(packageNode.getLine(), packageNode.getColumn(), packageNode.getLength());
            Selection pkgNameSel = getTypeSel(packageNameNode);
            Selection semiSel = new Selection(semi.getLine(), semi.getColumn(), semi.getLength());
            info.setPackageSelections(pkgSel, pkgNameSel, pkgName, semiSel);
        }
    }
    
    /**
     * Process the "extends" clause (for interfaces) by storing relevant info
     * @param cnode     The node of the clause (type EXTENDS_CLAUSE)
     * @param nameNode   The node representing the name of the class
     * @throws RecognitionException
     */
    private void processIfaceExtendsClause(AST cnode, LocatableAST nameNode, ClassInfo info)
    throws RecognitionException
    {
        LocatableAST superNode = (LocatableAST) cnode.getFirstChild();
        if (superNode != null) {
            
            // set "extends" keyword selection
            LocatableAST extendsN = (LocatableAST) cnode;
            LocatableToken extendsTok = extendsN.getImportantToken(0);
            Selection sel = new Selection(extendsTok.getLine(), extendsTok.getColumn(), extendsTok.getLength());
            ArrayList superInterfaces = new ArrayList();
            //info.setExtendsReplaceSelection(sel);

            superInterfaces.add(sel);
            Selection lastSel = null;
            
            while (superNode != null) {
                String superName = getQualifiedName(superNode);
                info.addImplements(superName);
                
                // set superclass name selection
                sel = getTypeSel(superNode);
                if (lastSel != null) {
                    // This is not the first super interface listed, so there must be
                    // an intervening comma.
                    SourceSpan commaSpan = new SourceSpan(lastSel.getEndLocation(),
                            sel.getStartLocation());
                    Selection commaSel = new Selection(commaSpan);
                    superInterfaces.add(commaSel);
                }
                superInterfaces.add(sel);
                lastSel = sel;
                
                superNode = (LocatableAST) superNode.getNextSibling();
            }
            info.setExtendsInsertSelection(new Selection(lastSel.getEndLine(), lastSel.getEndColumn()));
            info.setInterfaceSelections(superInterfaces);
        }
        else {
            // selection for inserting extends clause including
            // "extends" keyword
            Selection tparSel = info.getTypeParametersSelection();
            Selection s;
            if (tparSel == null) {
                // No type parameters
                s = new Selection(nameNode.getLine(), nameNode.getEndColumn());
            }
            else {
                // insert extends after type parametrs
                s = new Selection(tparSel.getEndLine(), tparSel.getEndColumn());
            }
            info.setExtendsInsertSelection(s);
            info.setInterfaceSelections(Collections.EMPTY_LIST);
        }
    }

    /**
     * Process the "implements" clause (for classes) by storing relevant info
     * @param cnode     The node of the clause (type IMPLEMENTS_CLAUSE)
     * @param nameNode   The node representing the name of the class
     * @throws RecognitionException
     */
    private void processClassImplementsClause(AST cnode, LocatableAST nameNode, ClassInfo info)
    throws RecognitionException
    {
        LocatableAST superNode = (LocatableAST) cnode.getFirstChild();
        if (superNode != null) {
            
            ArrayList superInterfaces = new ArrayList();
            
            LocatableAST implementsN = (LocatableAST) cnode;
            Token implementsTok = implementsN.getImportantToken(0);
            Selection sel = new Selection(implementsTok.getLine(), implementsTok.getColumn(), implementsTok.getText().length());
            superInterfaces.add(sel);

            Selection lastSel = null;

            // Now we expect a series of interfaces
            while (superNode != null) {
                String superName = getQualifiedName(superNode);
                info.addImplements(superName);
                
                // set superclass name selection
                sel = getTypeSel(superNode);
                if (lastSel != null) {
                    // This is not the first super interface listed, so there must be
                    // an intervening comma.
                    SourceSpan commaSpan = new SourceSpan(lastSel.getEndLocation(),
                            sel.getStartLocation());
                    Selection commaSel = new Selection(commaSpan);
                    superInterfaces.add(commaSel);
                }
                superInterfaces.add(sel);
                lastSel = sel;
                
                superNode = (LocatableAST) superNode.getNextSibling();
            }
            info.setImplementsInsertSelection(new Selection(lastSel.getEndLine(), lastSel.getEndColumn()));
            info.setInterfaceSelections(superInterfaces);
        }
        else {
            // selection for inserting implements clause including
            // "implements" keyword
            Selection superSel = info.getSuperReplaceSelection();
            Selection s;
            if (superSel == null) {
                // No extends clause
                Selection tparsSel = info.getTypeParametersSelection();
                if (tparsSel == null) {
                    s = new Selection(nameNode.getLine(), nameNode.getColumn() + nameNode.getText().length());
                }
                else {
                    s = new Selection(tparsSel.getEndLine(), tparsSel.getEndColumn());
                }
            }
            else {
                // insert "implements" after extends clause
                s = new Selection(superSel.getEndLine(), superSel.getEndColumn());
            }
            info.setImplementsInsertSelection(s);
            info.setInterfaceSelections(Collections.EMPTY_LIST);
        }
    }

    /**
     * Process the "extends" clause (for classes) by storing relevant info
     * @param cnode     The node of the clause (type EXTENDS_CLAUSE)
     * @param nameNode   The node representing the name of the class
     * @throws RecognitionException
     */
    private void processClassExtendsClause(AST cnode, LocatableAST nameNode, ClassInfo info)
    throws RecognitionException
    {
        LocatableAST superNode = (LocatableAST) cnode.getFirstChild();
        if (superNode != null) {
            String superName = getQualifiedName(superNode);
            info.setSuperclass(superName);
            
            // set superclass name selection
            Selection sel = getTypeSel(superNode);
            info.setSuperReplaceSelection(sel);

            // set "extends" keyword selection
            LocatableAST extendsN = (LocatableAST) cnode;
            Token extendsTok = extendsN.getImportantToken(0);
            sel = new Selection(extendsTok.getLine(), extendsTok.getColumn(), extendsTok.getText().length());
            info.setExtendsReplaceSelection(sel);
        }
        else {
            // selection for inserting extends clause including
            // "extends" keyword
            // selection for inserting implements clause including
            // "implements" keyword
            Selection tparSel = info.getTypeParametersSelection();
            Selection s;
            if (tparSel == null) {
                // No type parameters
                s = new Selection(nameNode.getLine(), nameNode.getColumn() + nameNode.getText().length());
            }
            else {
                // insert extends after type parametrs
                s = new Selection(tparSel.getEndLine(), tparSel.getEndColumn());
            }
            info.setExtendsInsertSelection(s);
        }
    }
    
    /**
     * Process an OBJBLOCK node (class or interface body) to find references to other
     * classes.
     * @param node    The AST node
     * @param cuScope The block scope (generally a newly created scope)
     */
    private void processObjBlock(AST node, Scope scope)
    {
        AST cnode = node.getFirstChild();
 
        // First we want to do a pass where we add all appropriate symbol names to
        // the scope.
        while (cnode != null) {
            int cnodeType = cnode.getType();
            switch (cnodeType) {
                case JavaTokenTypes.VARIABLE_DEF:
                    // mods, type, id
                    AST vnode = cnode.getFirstChild();
                    vnode = vnode.getNextSibling();
                    vnode = vnode.getNextSibling();
                    // Debug.message("Var definition, name = " + vnode.getText());
                    scope.addVariable(vnode.getText());
                    break;
                case JavaTokenTypes.CLASS_DEF:
                case JavaTokenTypes.INTERFACE_DEF:
                case JavaTokenTypes.ENUM_DEF:
                    // modifiers, identifier
                    AST cdnode = cnode.getFirstChild();
                    cdnode = cdnode.getNextSibling();
                    break;
                default:
            }
            cnode = cnode.getNextSibling();
        }
        
        cnode = node.getFirstChild();
        while (cnode != null) {
            int cnodeType = cnode.getType();
            switch (cnodeType) {
                case JavaTokenTypes.VARIABLE_DEF:
                    processVarDef(cnode, scope);
                    break;
                
                case JavaTokenTypes.METHOD_DEF:
                case JavaTokenTypes.CTOR_DEF:
                    processMethodDef(cnode, scope);
                    break;
                    
                case JavaTokenTypes.ENUM_CONSTANT_DEF:
                    processEnumConstantDef(cnode, scope);
                    break;
                    
                case JavaTokenTypes.INSTANCE_INIT:
                case JavaTokenTypes.STATIC_INIT:
                    processCodeBlock(cnode.getFirstChild(), new Scope(scope));
                    break;

                case JavaTokenTypes.CLASS_DEF:
                case JavaTokenTypes.INTERFACE_DEF:
                case JavaTokenTypes.ENUM_DEF:
                    processTypeDef(cnode, scope);
                    break;
                    
                default:
                    Debug.message("ClassParser: Unhandled node type: " + cnodeType);
            }
            
            cnode = cnode.getNextSibling();
        }
    }
    
    private void processEnumConstantDef(AST node, Scope scope)
    {
        // ENUM_CONSTANT_DEF
        
        node = node.getFirstChild();
        // There may be annotations: skip until we find the identifier
        while (node.getType() != JavaTokenTypes.IDENT)
            node = node.getNextSibling();
        
        String name = node.getText();
        scope.addVariable(name);
        node = node.getNextSibling();
        
        if (node != null && node.getType() == JavaTokenTypes.ELIST) {
            processExpressionList(node, scope);
            node = node.getNextSibling();
        }
        
        if (node != null && node.getType() == JavaTokenTypes.OBJBLOCK) {
            Scope newScope = new Scope(scope);
            processObjBlock(node, newScope);
        }
    }
    
    private void processTypeDef(AST node, Scope scope)
    {
        // A class, interface or enum definition.
        // Find the OBJBLOCK node and process that.
        
        AST cnode = node.getFirstChild();
        while (cnode.getType() != JavaTokenTypes.OBJBLOCK)
            cnode = cnode.getNextSibling();
        
        processObjBlock(cnode, new Scope(scope));
    }
    
    /**
     * Process a code block, generally surrounded by '{ }' brackets in code.
     * In general the scope passed in should be a newly created scope, but it
     * may contain method/constructor parameter names for instance.
     * @param node   The node representing the code block
     * @param scope  The (newly created) scope for this code block
     */
    private void processCodeBlock(AST node, Scope scope)
    {
        node = node.getFirstChild();
        while (node != null) {
            processStatement(node, scope);
            node = node.getNextSibling();
        }
    }
    
    private void processStatement(AST node, Scope scope)
    {
        int ntype = node.getType();
        switch (ntype) {
            case JavaTokenTypes.EXPR:
                processExpression(node, scope);
                break;
                
            case JavaTokenTypes.LABELED_STAT:
                node = node.getFirstChild(); // label (identifier)
                node = node.getNextSibling();
                processStatement(node, scope);
            
            case JavaTokenTypes.CTOR_CALL:
            case JavaTokenTypes.SUPER_CTOR_CALL:
                processExpressionList(node.getFirstChild(), scope);
                break;
                
            case JavaTokenTypes.SLIST:
                processCodeBlock(node, new Scope(scope));
                break;
                
            case JavaTokenTypes.VARIABLE_DEF:
            {
                processVarDef(node, scope);
                                
                // Add the variable name to the current scope
                //   modifiers, TYPE, identifier
                AST cnode = node.getFirstChild(); // modifiers
                cnode = cnode.getNextSibling(); // TYPE node
                String varName = cnode.getNextSibling().getText();
                scope.addVariable(varName);
                break;
            }
            
            case JavaTokenTypes.CLASS_DEF:
            case JavaTokenTypes.INTERFACE_DEF:
            case JavaTokenTypes.ENUM_DEF:
                processTypeDef(node, scope);
                break;
            
            case JavaTokenTypes.LITERAL_if:
            {
                node = node.getFirstChild();
                processExpression(node, scope);
                node = node.getNextSibling();
                processStatement(node, scope);
                node = node.getNextSibling();
                if (node != null) {
                    // "else" clause
                    processStatement(node, scope);
                }
                break;
            }
            
            case JavaTokenTypes.FOR:
            {
                Scope forScope = new Scope(scope);
                node = node.getFirstChild(); // FOR_INIT
                AST cnode = node.getFirstChild();
                if (cnode != null) {
                    processStatement(node.getFirstChild(), forScope);
                }
                node = node.getNextSibling(); // FOR_CONDITION
                AST conditionNode = node.getFirstChild();
                if (conditionNode != null) {
                    processExpression(conditionNode, forScope);
                }
                node = node.getNextSibling(); // FOR_ITERATOR
                AST iteratorNode = node.getFirstChild();
                if (iteratorNode != null) {
                    processExpressionList(iteratorNode, forScope);
                }
                node = node.getNextSibling();
                processStatement(node, forScope);
                break;
            }
                
            case JavaTokenTypes.ELIST:
                // A "for" statement can contain an expression list in its initializer
                processExpressionList(node, scope);
                break;
            
            case JavaTokenTypes.FOR_EACH:
            {
                // java 1.5 style "for" 
                node = node.getFirstChild();
                Scope forScope = new Scope(scope);
                processParameterDef(node, forScope);
                node = node.getNextSibling();
                processExpression(node, scope);
                node = node.getNextSibling();
                processStatement(node, forScope);
                break;
            }
            
            case JavaTokenTypes.LITERAL_while:
            {
                node = node.getFirstChild();
                processExpression(node, scope);
                node = node.getNextSibling();
                processStatement(node, scope);
                break;
            }
            
            case JavaTokenTypes.LITERAL_do:
            {
                node = node.getFirstChild();
                processStatement(node, scope);
                node = node.getNextSibling();
                processExpression(node, scope);
                break;
            }
                        
            case JavaTokenTypes.LITERAL_switch:
            {
                // EXPR  CASE_GROUP  CASE_GROUP  ...
                //   where "EXPR" is the expression to switch on
                //         "CASE_GROUP" is a group of "case" labels
                node = node.getFirstChild();
                processExpression(node, scope);
                Scope switchScope = new Scope(scope);
                node = node.getNextSibling();
                while (node != null) {
                    // Case group
                    AST caseNode = node.getFirstChild();
                    while (caseNode != null) {
                        int caseNodeType = caseNode.getType();
                        if (caseNodeType == JavaTokenTypes.LITERAL_case) {
                            // case label
                            processExpression(caseNode.getFirstChild(), switchScope);
                        }
                        else if (caseNodeType == JavaTokenTypes.SLIST) {
                            // statements
                            processCodeBlock(caseNode, switchScope);
                        }
                        caseNode = caseNode.getNextSibling();
                    }
                    node = node.getNextSibling();
                }
                break;
            }
            
            case JavaTokenTypes.LITERAL_try:
            {
                node = node.getFirstChild();
                processStatement(node, scope);
                node = node.getNextSibling();
                while (node != null) {
                    if (node.getType() == JavaTokenTypes.LITERAL_catch) {
                        AST cnode = node.getFirstChild(); // PARAMETER_DEF
                        AST tnode = cnode.getFirstChild().getNextSibling(); // type
                        String catchTypeName = getFirstLevelName(tnode.getFirstChild());
                        String catchVarName = tnode.getNextSibling().getText();
                        scope.checkType(catchTypeName);
                        
                        // catch block
                        Scope catchBlockScope = new Scope(scope);
                        catchBlockScope.addVariable(catchVarName);
                        processStatement(cnode.getNextSibling(), catchBlockScope);
                    }
                    node = node.getNextSibling();
                }
                break;
            }
            
            case JavaTokenTypes.LITERAL_throw:
            {
                node = node.getFirstChild();
                processExpression(node, scope);
                break;
            }
            
            case JavaTokenTypes.LITERAL_return:
            {
                // "return" statement, possibly with return value
                AST cnode = node.getFirstChild();
                if (cnode != null) {
                    processExpression(cnode, scope);
                }
                break;
            }
            
            case JavaTokenTypes.LITERAL_synchronized:
            {
                node = node.getFirstChild();
                processExpression(node, scope);
                processStatement(node.getNextSibling(), scope);
            }
            
            case JavaTokenTypes.LITERAL_assert:
            {
                node = node.getFirstChild();
                while (node != null) {
                    processExpression(node, scope);
                    node = node.getNextSibling();
                }
            }
            
            case JavaTokenTypes.LITERAL_break:
            case JavaTokenTypes.LITERAL_continue:
            case JavaTokenTypes.EMPTY_STAT:
                break;

            default:
                Debug.message("ClassParser (processCodeBlock) unhandled node type: " + ntype);
        }
    }
    
    /**
     * Process a parameter definition. This creates a reference to the parameter type and
     * adds a variable with the parameter name into scope. It also adds the type and name
     * to the type and name lists, if given, respectively.
     * 
     * @param node   The node representing the parameter definition
     * @param scope  The scope
     * @param paramTypeList  List of parameter types (may be null)
     * @param paramNameList  List of parameter names (may be null)
     * @param varArg  true if the parameter is the final parameter and is marked as variable
     *                arity (eg. int ... x)
     */
    private void processParameterDef(AST node, Scope scope, List<String> paramTypeList,
            List<String> paramNameList)
    {
        // PARAMETER_DEF
        //   MODIFIERS TYPE identifier
        
        boolean isVarArg = node.getType() == JavaTokenTypes.VARIABLE_PARAMETER_DEF;
        
        AST tnode = node.getFirstChild().getNextSibling();
        String typeName = getFirstLevelName(tnode.getFirstChild());
        scope.checkType(typeName); // for dependency tracking
        if (paramTypeList != null) {
            String paramType = getCompleteTypeString(tnode.getFirstChild());
            if (isVarArg)
                paramType += " ...";
            paramTypeList.add(paramType);
        }
        String paramName = tnode.getNextSibling().getText();
        scope.addVariable(paramName);
        if (paramNameList != null)
            paramNameList.add(paramName);
    }
    
    private void processParameterDef(AST node, Scope scope)
    {
        processParameterDef(node, scope, null, null);
    }
    
    /**
     * Process a VARIABLE_DEF node (variable or parameter definition) including declared
     * type and initializer expression if any. 
     * 
     * Does NOT add the variable name to the given scope.
     * 
     * @param node   The VARIABLE_DEF node to process
     * @param scope  The current scope.
     */
    private void processVarDef(AST node, Scope scope)
    {
        // VARIABLE_DEF
        //   modifiers, (TYPE type), identifier, initializer
        AST cnode = node.getFirstChild();
        cnode = cnode.getNextSibling();
        
        // Generate a reference.
        String typeName = getFirstLevelName(cnode.getFirstChild());
        scope.checkType(typeName);
        
        AST initializerNode = cnode.getNextSibling().getNextSibling();
        if (initializerNode != null) {
            // the initializer node is '=', we want the child node
            processExpression(initializerNode.getFirstChild(), scope);
        }
    }
    
    private void processExpression(AST node, Scope scope)
    {
        // Some expressions are wrapped in an EXPR node
        if (node.getType() == JavaTokenTypes.EXPR)
            node = node.getFirstChild();
        
        int ntype = node.getType();
        switch (ntype) {
            case JavaTokenTypes.IDENT:
            {
                // A simple type or variable name
                String name = node.getText();
                if (! scope.checkVariable(name)) {
                    scope.checkType(name);
                }
                break;
            }
            
            case JavaTokenTypes.DOT:
            {
                node = node.getFirstChild();
                AST secondChild = node.getNextSibling();
                
                // Class literal?
                if (secondChild.getType() == JavaTokenTypes.LITERAL_class) {
                    while (node.getType() == JavaTokenTypes.ARRAY_DECLARATOR)
                        node = node.getFirstChild();
                    String className = getFirstLevelName(node);
                    scope.checkType(className);
                    break;
                }
                
                // No, standard dot expression
                processExpression(node, scope);
                if (secondChild.getType() == JavaTokenTypes.LITERAL_new) {
                    // qualified new (used to instantiate inner class)
                    node = secondChild.getFirstChild(); // inner type name
                    node = node.getNextSibling();
                    processExpressionList(node, scope);
                }
                break;
            }
            
            case JavaTokenTypes.SUPER_CTOR_CALL:
            {
                // Normally this is a statement, not an expression. But in the
                // case of a type which extends an instance inner type, the
                // super() call needs to be qualified with an instance.
                //
                // SUPER_CTOR_CALL
                //  ( . expression "super" ) ELIST
                node = node.getFirstChild();
                processExpression(node.getFirstChild(), scope);
                processExpressionList(node.getNextSibling(), scope);
                break;
            }
            
            case JavaTokenTypes.LNOT:
            case JavaTokenTypes.POST_INC:
            case JavaTokenTypes.POST_DEC:
            case JavaTokenTypes.INC:
            case JavaTokenTypes.DEC:
            case JavaTokenTypes.UNARY_MINUS:
            case JavaTokenTypes.UNARY_PLUS:
                // unary operators
                processExpression(node.getFirstChild(), scope);
                break;
            
            case JavaTokenTypes.ASSIGN:
            case JavaTokenTypes.PLUS_ASSIGN:
            case JavaTokenTypes.MINUS_ASSIGN:
            case JavaTokenTypes.STAR_ASSIGN:
            case JavaTokenTypes.DIV_ASSIGN:
            case JavaTokenTypes.MOD_ASSIGN:
            case JavaTokenTypes.SR_ASSIGN:
            case JavaTokenTypes.BSR_ASSIGN:
            case JavaTokenTypes.SL_ASSIGN:
            case JavaTokenTypes.BAND_ASSIGN:
            case JavaTokenTypes.BXOR_ASSIGN:
            case JavaTokenTypes.BOR_ASSIGN:
            case JavaTokenTypes.PLUS:
            case JavaTokenTypes.MINUS:
            case JavaTokenTypes.STAR:
            case JavaTokenTypes.DIV:
            case JavaTokenTypes.MOD:
            case JavaTokenTypes.SR:
            case JavaTokenTypes.BSR:
            case JavaTokenTypes.SL:
            case JavaTokenTypes.EQUAL:
            case JavaTokenTypes.NOT_EQUAL:
            case JavaTokenTypes.LT:
            case JavaTokenTypes.GT:
            case JavaTokenTypes.LE:
            case JavaTokenTypes.GE:
            case JavaTokenTypes.INDEX_OP:
            case JavaTokenTypes.LOR:
            case JavaTokenTypes.LAND:
            case JavaTokenTypes.BAND:
            case JavaTokenTypes.BOR:
            case JavaTokenTypes.BXOR:
            {
                // binary operators
                node = node.getFirstChild();
                processExpression(node, scope);
                node = node.getNextSibling();
                processExpression(node, scope);
                break;
            }
            
            case JavaTokenTypes.QUESTION:
            {
                // trinary "? :" operator
                node = node.getFirstChild();
                processExpression(node, scope);
                node = node.getNextSibling();
                processExpression(node, scope);
                node = node.getNextSibling();
                processExpression(node, scope);
                break;
            }
            
            case JavaTokenTypes.LITERAL_instanceof:
            {
                node = node.getFirstChild();
                processExpression(node, scope);
                node = node.getNextSibling();
                String tname = getFirstLevelName(node.getFirstChild());
                scope.checkType(tname);
                break;
            }
            
            case JavaTokenTypes.METHOD_CALL:
            {
                node = node.getFirstChild();
                processExpression(node, scope);
                processExpressionList(node.getNextSibling(), scope);
                break;
            }
            
            case JavaTokenTypes.TYPECAST:
            {
                // TYPECAST
                //  TYPE expr
                node = node.getFirstChild();
                String typeName = getFirstLevelName(node.getFirstChild());
                scope.checkType(typeName);
                processExpression(node.getNextSibling(), scope);
                break;
            }
            
            case JavaTokenTypes.LITERAL_new:
            {
                // "new" node: type, ELIST (or array declarators)
                node = node.getFirstChild();  // type
                String typeName = getFirstLevelName(node);
                scope.checkType(typeName); // create reference
                node = node.getNextSibling(); // argument list
                if (node.getType() == JavaTokenTypes.ELIST) {
                    processExpressionList(node, scope);
                }
                else {
                    // new array
                    AST anode = node.getFirstChild();
                    while (anode != null && anode.getType() == JavaTokenTypes.ARRAY_DECLARATOR) {
                        AST exprNode = anode.getNextSibling();
                        if (exprNode != null)
                            processExpression(exprNode, scope);
                        anode = anode.getFirstChild();
                    }
                    
                    // optional initializer eg {"one","two","three"}
                    node = node.getNextSibling();
                    if (node != null) {
                        processExpression(node, scope);
                    }
                }
                break;
            }
                            
            case JavaTokenTypes.ARRAY_INIT:
                processExpressionList(node, scope);
                break;
                
            case JavaTokenTypes.NUM_INT:
            case JavaTokenTypes.NUM_DOUBLE:
            case JavaTokenTypes.NUM_FLOAT:
            case JavaTokenTypes.NUM_LONG:
            case JavaTokenTypes.LITERAL_true:
            case JavaTokenTypes.LITERAL_false:
            case JavaTokenTypes.LITERAL_null:
            case JavaTokenTypes.LITERAL_this:
            case JavaTokenTypes.LITERAL_super:
            case JavaTokenTypes.STRING_LITERAL:
            case JavaTokenTypes.CHAR_LITERAL:
                // we can ignore all these
                break;
            
            default:
                Debug.message("ClassParser: Unhandled node type (expression) : " + ntype);
        }
    }
    
    private void processExpressionList(AST node, Scope scope)
    {
        // ELIST node
        node = node.getFirstChild();
        while (node != null) {
            processExpression(node, scope);
            node = node.getNextSibling();
        }
    }
    
    private void processMethodDef(AST node, Scope scope)
    {
        // METHOD_DEF
        //   MODIFIERS TYPE_PARAMETERS? TYPE identifier PARAMETERS block?

        boolean isConstructor = node.getType() == JavaTokenTypes.CTOR_DEF;
        
        List<String> paramTypes = new ArrayList<String>();
        List<String> paramNames = new ArrayList<String>();
        String typeParams = null;
        Token commentToken;
        
        AST cnode = node.getFirstChild(); // Modifiers
        boolean hasModifiers = cnode.getFirstChild() != null;

        // The comment is attached to the first modifier, if there was one, or
        // the return type otherwise
        if (hasModifiers) {
            commentToken = getHiddenBefore(cnode);
            cnode = cnode.getNextSibling();  // Return type (or type parameters)
        }
        else {
            cnode = cnode.getNextSibling();
            commentToken = getHiddenBefore(cnode);
        }
        
        // Skip over type parameters (if present)
        if (cnode.getType() == JavaTokenTypes.TYPE_PARAMETERS) {
            typeParams = getTypeParamList(cnode);
            cnode = cnode.getNextSibling();
        }
        
        // Get the return type as a string
        String rtypeName;
        if (! isConstructor) {
            // Generate a reference to the return type
            rtypeName = getFirstLevelName(cnode.getFirstChild());
            scope.checkType(rtypeName);
            rtypeName = getCompleteTypeString(cnode.getFirstChild());
            cnode = cnode.getNextSibling();  // identifier
        }
        else {
            rtypeName = null;
        }
        
        String methodName = cnode.getText();
        cnode = cnode.getNextSibling();  // parameter block
        AST paramDef = cnode.getFirstChild();
        Scope methodScope = new Scope(scope);
        while (paramDef != null) {
            processParameterDef(paramDef, methodScope, paramTypes, paramNames);
            paramDef = paramDef.getNextSibling();
        }
        
        // process method body if present
        while (cnode != null) {
            if (cnode.getType() == JavaTokenTypes.SLIST) {
                processCodeBlock(cnode, methodScope);
                break;
            }
            cnode = cnode.getNextSibling();
        }
        
        // Add the method into the scope (with attached comment)
        String commentText = null;
        if (commentToken != null)
            commentText = commentToken.getText();
        scope.addMethod(methodName, typeParams, rtypeName, paramTypes, paramNames, commentText);
    }
    
    private String getTypeParamList(AST node)
    {
        // TYPE_PARAMETERS
        String r = "<";
        
        node = node.getFirstChild();
        r += getTypeParamString(node);
        node = node.getNextSibling();
        while (node != null) {
            r += "," + getTypeParamString(node);
            node = node.getNextSibling();
        }
        r += ">";
        
        return r;
    }
    
    private String getTypeParamString(AST node)
    {
        String tpar = node.getText();
        node = node.getFirstChild();
        if (node != null) {
            tpar += " extends ";
            node = node.getFirstChild();
            tpar += getCompleteTypeString(node); 
            node = node.getNextSibling();
            while (node != null) {
                tpar += " & " + getCompleteTypeString(node);
                node = node.getNextSibling();
            }
        }
        return tpar;
    }
    
    private String getCompleteTypeString(AST node)
    {
        // A type is often (but not always) found under a TYPE node. It consists
        // of a child representing the type itself, and then optionally a series
        // of array declarators.
        
        if (node.getType() == JavaTokenTypes.TYPE)
            node = node.getFirstChild();
        
        AST arrayNode = node.getNextSibling();
        int ntype = node.getType();
        String tstring;
        
        if (ntype == JavaTokenTypes.DOT) {
            node = node.getFirstChild();
            AST second = node.getNextSibling();
            tstring = getCompleteTypeString(node) + "." + getCompleteTypeString(second);
            node = second.getNextSibling();
        }
        else {
            tstring = node.getText();
            node = node.getFirstChild();
            if (node != null && node.getType() == JavaTokenTypes.TYPE_ARGUMENT) {
                // Type arguments are present.
                tstring += "<";
                tstring += getTypeArgString(node);
                node = node.getNextSibling();
                while (node != null && node.getType() == JavaTokenTypes.TYPE_ARGUMENT) {
                    tstring += "," + getTypeArgString(node);
                    node = node.getNextSibling();
                }
                tstring += ">";
            }
        }
        
        // check array declarators
        while(arrayNode != null && arrayNode.getType() == JavaTokenTypes.ARRAY_DECLARATOR) {
            tstring += "[]";
            arrayNode = arrayNode.getFirstChild();
        }
        
        return tstring;
    }
    
    private String getTypeArgString(AST node)
    {
        // TYPE_ARGUMENT
        node = node.getFirstChild();
        if (node.getType() == JavaTokenTypes.WILDCARD_TYPE) {
            String targ = "?";
            node = node.getFirstChild();
            if (node != null) {
                if (node.getType() == JavaTokenTypes.TYPE_UPPER_BOUNDS) {
                    targ += " extends ";
                }
                else {
                    targ += " super ";
                }
                targ += getCompleteTypeString(node.getFirstChild());
            }
            return targ;
        }
        
        return getCompleteTypeString(node);
    }
    
    private Token getHiddenBefore(AST node)
    {
        while (node instanceof LocatableAST) {
            LocatableAST lnode = (LocatableAST) node;
            Token r = lnode.getHiddenBefore();
            if (r != null)
                return r;
            node = node.getFirstChild();
        }
        return null;
    }
    
    /**
     * Return the "first level" name in a (possibly qualified) name. For instance
     * in the qualified name "abc.def.xyz" the first level name is "abc".
     * @param node  The node representing the name
     * @return   The first level name
     */
    private String getFirstLevelName(AST node)
    {
        while (node.getType() == JavaTokenTypes.DOT)
            node = node.getFirstChild();
        return node.getText();
    }
    
    /**
     * Get a name which might be qualified.
     * @param node  The node storing the name in x.y.z format
     * @return    The name as it appears in the source
     */
    private String getQualifiedName(AST node)
        throws RecognitionException
    {
        if (node.getType() == JavaTokenTypes.IDENT)
            return node.getText();
        
        if (node.getType() != JavaTokenTypes.DOT)
            throw new RecognitionException();
        
        AST cnode = node.getFirstChild();
        String name = getQualifiedName(cnode);
        name += "." + cnode.getNextSibling().getText();
        return name;
    }
    
    /**
     * Get the selection corresponding to a type, which may have type arguments.
     * @param node  The AST node representing the type
     * @return      The selection for the corresponding type (including type arguments)
     * @throws RecognitionException
     */
    private Selection getTypeSel(LocatableAST node)
    throws RecognitionException
    {
        if (node.getType() == JavaTokenTypes.IDENT) {
            Selection s = new Selection(node.getLine(), node.getColumn(), node.getLength());
            LocatableAST c = (LocatableAST) node.getFirstChild();
            if (c != null) {
                if (c.getType() == JavaTokenTypes.TYPE_ARGUMENT && c.getImportantTokenCount() != 0) {
                    LocatableToken tend = c.getImportantToken(0);
                    int line = tend.getLine();
                    int col = tend.getEndColumn();
                    s.extendEnd(line, col);
                }
            }
            return s;
        }
        
        if (node.getType() != JavaTokenTypes.DOT)
            throw new RecognitionException();
        
        // get the rightmost part 
        LocatableAST lnode = (LocatableAST) node.getFirstChild();
        LocatableAST rnode = (LocatableAST) lnode.getNextSibling();
        Selection s = getTypeSel(rnode);
        
        // iterate down the tree (to the left) and continuously add in the
        // rightmost part to the selection
        while (lnode.getType() == JavaTokenTypes.DOT) {
            node = lnode;
            lnode = (LocatableAST) node.getFirstChild();
        }
        
        if (lnode.getType() != JavaTokenTypes.IDENT)
            throw new RecognitionException();
        
        // finally add in the leftmost part
        Selection ns = new Selection(lnode.getLine(), lnode.getColumn(), lnode.getLength());
        ns.combineWith(s);
        return ns;
    }
}
