/*
 This file is part of the BlueJ program. 
 Copyright (C) 2024  Michael Kolling and John Rosenberg
 
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
package bluej.parser.psi.visitor;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.psi.JavaParserCallbacksAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.psi.PsiComment;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtilKt;
import org.jetbrains.kotlin.psi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI visitor for class-level member declarations.
 *
 * <h2>Architectural Role</h2>
 * <p>ClassVisitor handles class member declarations including methods, properties, constructors,
 * init blocks, nested classes, and companion objects. It is designed to work alongside
 * {@link FileVisitor} which handles file-level constructs.</p>
 *
 * <h2>Context-Aware Design (CRITICAL)</h2>
 * <p><b>ClassVisitor KNOWS it is handling class members.</b> This is the key architectural
 * difference from shared helper methods in {@link BaseVisitor}:</p>
 * <ul>
 *   <li>ClassVisitor makes context-specific callback decisions</li>
 *   <li>Methods use {@code callbacks.endMethodDecl()} - NOT {@code callbacks.endDecl()}</li>
 *   <li>Properties use {@code callbacks.beginFieldDeclarations()} - NOT variable declarations</li>
 *   <li><b>NO</b> {@code PsiTreeUtil.getParentOfType()} calls to detect context</li>
 * </ul>
 *
 * <h2>Delegation Pattern</h2>
 * <p>ClassVisitor uses {@link BaseVisitor} helpers for data extraction, then makes
 * context-aware callback decisions:</p>
 * <pre>{@code
 * // ClassVisitor KNOWS this is a class method
 * @Override
 * public void visitNamedFunction(KtNamedFunction function) {
 *     FunctionSignatureResult sig = extractFunctionSignature(function); // BaseVisitor helper
 *
 *     // Context-aware: ClassVisitor always uses method callbacks
 *     callbacks.beginMethodDecl(...);
 *     // ... process using helper data ...
 *     callbacks.endMethodDecl(endToken, hasBody); // NOT endDecl()!
 * }
 * }</pre>
 *
 * <h2>Handled Declarations</h2>
 * <ul>
 *   <li>{@link #visitNamedFunction(KtNamedFunction)} - Class methods (member functions)</li>
 *   <li>{@link #visitProperty(KtProperty)} - Class fields (member properties)</li>
 *   <li>{@link #visitClass(KtClass)} - Nested classes, interfaces, enums</li>
 *   <li>{@link #visitObjectDeclaration(KtObjectDeclaration)} - Companion objects, nested objects</li>
 *   <li>{@link #visitClassInitializer(KtClassInitializer)} - Init blocks</li>
 *   <li>{@link #visitPrimaryConstructor(KtPrimaryConstructor)} - Primary constructors</li>
 *   <li>{@link #visitSecondaryConstructor(KtSecondaryConstructor)} - Secondary constructors</li>
 * </ul>
 *
 * <h2>Body Delegation</h2>
 * <p>Method and constructor bodies are delegated to {@link MethodBodyVisitor} for
 * statement and expression parsing.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This visitor is NOT thread-safe. Each class body traversal should create a new
 * visitor instance.</p>
 *
 * @see BaseVisitor Base class with context-agnostic helper methods
 * @see FileVisitor Visitor for file-level constructs
 * @see MethodBodyVisitor Visitor for method body statements
 */
public class ClassVisitor extends BaseVisitor {

    /**
     * Creates a new class visitor for processing class member declarations.
     *
     * @param callbacks The callback adapter for parser integration (must not be null)
     */
    public ClassVisitor(@NotNull JavaParserCallbacksAdapter callbacks) {
        super(callbacks);
    }

    // ==================== Method Declarations ====================

    /**
     * Visits a method declaration (member function) within a class.
     *
     * <p><b>Context-Aware:</b> ClassVisitor KNOWS this is a class method, so it uses
     * {@code callbacks.endMethodDecl()} rather than {@code callbacks.endDecl()}.</p>
     *
     * <h3>Callback Sequence</h3>
     * <ol>
     *   <li>{@code gotDeclBegin(token)} - Begin method declaration</li>
     *   <li>{@code gotModifier(token)} × n - Process modifiers</li>
     *   <li>{@code gotTypeSpec(tokens)} - Return type specification</li>
     *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
     *   <li>Type parameters (if generic)</li>
     *   <li>{@code gotMethodDeclaration(nameToken, javadocToken)} - Method name</li>
     *   <li>Parameter processing</li>
     *   <li>{@code gotAllMethodParameters()} - Parameters complete</li>
     *   <li>Body delegation to {@link MethodBodyVisitor}</li>
     *   <li>{@code endMethodDecl(token, hasBody)} - End method (CLASS CONTEXT)</li>
     * </ol>
     *
     * @param function The method declaration PSI element
     */
    @Override
    public void visitNamedFunction(@NotNull KtNamedFunction function) {
        function.accept(new FunctionVisitor(callbacks, false));
    }

    // ==================== Property/Field Declarations ====================

    /**
     * Visits a property declaration (class field).
     *
     * <p><b>Context-Aware:</b> ClassVisitor KNOWS this is a class field, so it uses
     * {@code callbacks.beginFieldDeclarations()} / {@code callbacks.endFieldDeclarations()}.
     * Local variables in method bodies are handled by {@link MethodBodyVisitor}.</p>
     *
     * <h3>Callback Sequence</h3>
     * <ol>
     *   <li>{@code gotDeclBegin(token)} - Begin field declaration</li>
     *   <li>{@code beginFieldDeclarations(token)} - Begin field block</li>
     *   <li>{@code gotModifier(token)} × n - Process modifiers</li>
     *   <li>{@code gotTypeSpec(tokens)} - Property type</li>
     *   <li>{@code gotField(first, idToken, hasInitializer)} - Field declaration</li>
     *   <li>{@code endField(token, true)} - End field</li>
     *   <li>{@code endFieldDeclarations(token, true)} - End field block</li>
     * </ol>
     *
     * @param property The property declaration PSI element
     */
    @Override
    public void visitProperty(@NotNull KtProperty property) {
        String propertyName = property.getName();

        // 1. Begin field declarations - CLASS CONTEXT: use field callbacks
        LocatableToken propertyToken = createToken(
                property.getValOrVarKeyword(),
                property.isVar() ? JavaTokenTypes.LITERAL_var : JavaTokenTypes.LITERAL_val
        );
        callbacks.gotDeclBegin(propertyToken);
        callbacks.beginFieldDeclarations(propertyToken);

        LocatableToken lastToken = null;

        // 2. Process modifiers
        ModifierSet modifiers = extractModifiers(property.getModifierList());
        for (ModifierToken modToken : modifiers.modifierTokens()) {
            callbacks.gotModifier(modToken.token());
        }

        // 3. Property type
        List<LocatableToken> typeTokens = processPropertyType(property);
        callbacks.gotTypeSpec(typeTokens);

        if (typeTokens != null && !typeTokens.isEmpty()) {
            lastToken = typeTokens.get(typeTokens.size() - 1);
        }

        // 4. Field declaration
        PsiElement nameIdentifier = property.getNameIdentifier();
        if (nameIdentifier != null && propertyName != null) {
            LocatableToken nameToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
            boolean hasInitializer = property.hasInitializer();
            callbacks.gotField(propertyToken, nameToken, hasInitializer);
            if (lastToken == null) {
                lastToken = nameToken;
            }
        }

        // 5. End field
        callbacks.endField(lastToken, true);

        // Find the final token for ending field declarations
        LocatableToken finalToken = lastToken;
        try {
//            PsiElement possiblyLast = property;
//            while (possiblyLast.getChildren().length > 0) {
//                PsiElement[] children = possiblyLast.getChildren();
//                possiblyLast = children[children.length - 1];
//            }
            finalToken = createToken(PsiTreeUtilKt.getLastLeaf(property));
        } catch (Exception e) {
            // Fallback to lastToken
        }

        // 6. End field declarations - CLASS CONTEXT
        callbacks.endFieldDeclarations(finalToken, true);
    }

    // ==================== Top-Level Type Declarations ====================

    /**
     * Visits a class declaration (class, interface, or enum).
     *
     * <p><b>Context-Aware:</b> FileVisitor handles the class HEADER only (modifiers, name,
     * type parameters, supertypes). The class BODY is delegated to {@link ClassVisitor}.</p>
     *
     * <h3>Callback Sequence</h3>
     * <ol>
     *   <li>{@code gotDeclBegin(token)} - Begin declaration</li>
     *   <li>{@code gotModifier(token)} × n - Process modifiers</li>
     *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
     *   <li>{@code gotTypeDef(token, tdType)} - Type definition</li>
     *   <li>{@code gotTypeDefName(nameToken)} - Type name</li>
     *   <li>Supertype processing</li>
     *   <li>{@code beginTypeBody(token)} - Begin body</li>
     *   <li><b>DELEGATION:</b> ClassVisitor processes all members</li>
     *   <li>{@code endTypeBody(token, true)} - End body</li>
     *   <li>{@code gotTypeDefEnd(token, true)} - End declaration</li>
     * </ol>
     *
     * @param ktClass The class declaration PSI element
     */
    @Override
    public void visitClass(@NotNull KtClass ktClass) {
        if (!parseTypeDefPart2) {
            String className = ktClass.getName();

            // 1. Begin declaration
            int tdType = determineTypeDefType(ktClass);
            PsiElement declElement = PsiTreeUtilKt.getFirstLeaf(ktClass.getFirstChild());
            while (declElement instanceof PsiComment || declElement instanceof PsiWhiteSpace) {
                declElement = PsiTreeUtilKt.getFirstLeaf(declElement.getNextSibling());
            }
            LocatableToken declToken = createToken(declElement);
            callbacks.gotDeclBegin(declToken);

            // 2. Process modifiers
            KtModifierList modifierList = ktClass.getModifierList();
            if (modifierList != null) {
                processModifiers(modifierList);
            }
            callbacks.modifiersConsumed();

            // 3. Type definition
            LocatableToken classToken = declToken; // createToken(ktClass.getClassOrInterfaceKeyword(), tdType);

            callbacks.gotTypeDef(classToken, tdType);

            LocatableToken nameToken = null;

            // 4. Type name
            if (className != null && ktClass.getNameIdentifier() != null) {
                nameToken = createToken(ktClass.getNameIdentifier(), JavaTokenTypes.IDENT);
                if (callbacks.isInEmitRange(classToken)) {
                    callbacks.gotTypeDefName(nameToken);
                } else {
//                    callbacks.gotTypeDefEnd(getTokenStream().LA(1), false);
                    return;
                }
            }
            else if (ktClass.getLastChild() instanceof PsiErrorElement) {
                if (callbacks.isInEmitRange(classToken)) {
                    var nextToken = getTokenStream().LA(1);
                    callbacks.gotTypeDefEnd(nextToken, false);
                }
                return;
            }
        }

        // 5. Process supertypes
        processSuperTypes(ktClass);

        if (parseTypeDefPart2) {
            KtClassBody body = ktClass.getBody();

            if (body != null) {
                PsiElement lBrace = body.getLBrace();

                if (lBrace != null) {
                    LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                    this.callbacks.skipToToken(lBraceToken);
                }
                else {
                    clearLastToken();
                }
            } else {
                PsiElement lastElement = findLastNonErrorElement(ktClass);
                var lastToken = createToken(lastElement);
                callbacks.skipToToken(lastToken);
                clearLastToken();
            }

//            return;
            throw new ParseTypeDefPart2FinishedHackException();
        }

        if (!parseTypeDefPart2) {
            LocatableToken finalToken = null;
            boolean finalTokenIncluded = true;
            boolean hadBody = false;

            // 6. Process class body - DELEGATE TO ClassVisitor
            KtClassBody body = ktClass.getBody();
            if (body != null) {
                hadBody = true;

                body.accept(this);

                finalToken = getLastToken();
            }

            // Handle primary constructor without body (for classes like `class Foo(val x: Int)`)
            if (!hadBody) {
                KtPrimaryConstructor primaryConstructor = ktClass.getPrimaryConstructor();
                if (primaryConstructor != null) {
                    PsiElement lastElement = findLastNonErrorElement(ktClass);
                    var lastToken = createToken(lastElement);
                    callbacks.skipToToken(lastToken);
                    finalToken = lastToken;
                }
            }

            if (finalToken == null) {
                finalToken = getLastToken();
                if (finalToken == null) {
                    finalToken = getTokenStream().LA(1);
                }
                finalTokenIncluded = !hadBody && finalToken.getType() != JavaTokenTypes.EOF;
                this.clearLastToken();
            }

            // 7. End declaration
            callbacks.gotTypeDefEnd(finalToken, finalTokenIncluded);
            callbacks.reachedCUstate(2);
        }
    }
//
//    /**
//     * Processes the class body by delegating member declarations to ClassVisitor.
//     *
//     * <p>This method handles the structural callbacks for the class body (braces),
//     * but delegates all member declaration processing to {@link ClassVisitor}.</p>
//     *
//     * @param classBody The class body to process
//     * @param ktClass The parent class (for accessing primary constructor)
//     */
//    private void processClassBody(KtClassBody classBody, KtClass ktClass) {
//        PsiElement lBrace = classBody.getLBrace();
//        PsiElement rBrace = classBody.getRBrace();
//        boolean typeBodyStarted = false;
//
//        if (lBrace != null) {
//            LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
//            callbacks.beginTypeBody(lBraceToken);
//            typeBodyStarted = callbacks.isInEmitRange(lBraceToken);
//        }
//
//        // DELEGATION: Use ClassVisitor for all member declarations
//        ClassVisitor classVisitor = new ClassVisitor(callbacks);
//        for (KtDeclaration declaration : classBody.getDeclarations()) {
//            declaration.accept(classVisitor);
//        }
//
//        if (typeBodyStarted) {
//            if (rBrace != null) {
//                LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
//                callbacks.endTypeBody(rBraceToken, true);
//            } else {
//                var token = this.getTokenStream().nextToken();
//                if (lBrace != null) {
//                    callbacks.endTypeBody(token, false);
//                }
//            }
//        }
//    }

    /**
     * Helper method to process supertypes for nested classes.
     */
    private void processNestedClassSupertypes(KtClassOrObject classOrObject) {
        List<KtSuperTypeListEntry> superTypeEntries = classOrObject.getSuperTypeListEntries();
        if (superTypeEntries.isEmpty()) {
            return;
        }

        // Find superclass (first entry with constructor call)
        KtSuperTypeListEntry superClassEntry = null;
        for (KtSuperTypeListEntry entry : superTypeEntries) {
            if (entry instanceof KtSuperTypeCallEntry) {
                if (superClassEntry == null) {
                    superClassEntry = entry;
                }
            }
        }

        // Process superclass
        if (superClassEntry != null) {
            KtTypeReference typeRef = superClassEntry.getTypeReference();
            if (typeRef != null) {
                LocatableToken extendsToken = createToken(typeRef, JavaTokenTypes.IDENT);
                callbacks.beginTypeDefExtends(extendsToken);
                callbacks.endTypeDefExtends();
            }
        }

        // Process interfaces
        boolean foundInterface = false;
        for (KtSuperTypeListEntry entry : superTypeEntries) {
            if (!(entry instanceof KtSuperTypeCallEntry)) {
                if (!foundInterface) {
                    KtTypeReference typeRef = entry.getTypeReference();
                    if (typeRef != null) {
                        LocatableToken implToken = createToken(typeRef, JavaTokenTypes.IDENT);
                        callbacks.beginTypeDefImplements(implToken);
                        callbacks.endTypeDefImplements();
                        foundInterface = true;
                    }
                }
            }
        }
    }

    // ==================== Object Declarations ====================

    /**
     * Visits an object declaration (singleton or companion object).
     *
     * <p><b>Context-Aware:</b> ClassVisitor handles companion objects and nested
     * singleton objects as class-level constructs.</p>
     *
     * <h3>Callback Sequence</h3>
     * <ol>
     *   <li>{@code gotDeclBegin(token)} - Begin declaration</li>
     *   <li>{@code gotModifier(token)} × n - Process modifiers</li>
     *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
     *   <li>{@code gotTypeDef(token, LITERAL_class)} - Object as class type</li>
     *   <li>{@code gotTypeDefName(nameToken)} - Object name or "Companion"</li>
     *   <li>{@code beginTypeBody(token)} - Begin body</li>
     *   <li>Visit members via recursive ClassVisitor</li>
     *   <li>{@code endTypeBody(token, true)} - End body</li>
     *   <li>{@code gotTypeDefEnd(token, true)} - End declaration</li>
     * </ol>
     *
     * @param declaration The object declaration PSI element
     */
    @Override
    public void visitObjectDeclaration(@NotNull KtObjectDeclaration declaration) {
        // 1. Begin declaration
        LocatableToken objectToken = createToken(declaration.getObjectKeyword(), JavaTokenTypes.LITERAL_object);
        callbacks.gotDeclBegin(objectToken);

        // 2. Process modifiers
        ModifierSet modifiers = extractModifiers(declaration.getModifierList());
        for (ModifierToken modToken : modifiers.modifierTokens()) {
            callbacks.gotModifier(modToken.token());
        }
        callbacks.modifiersConsumed();

        // 3. Type definition - objects are mapped to classes
        callbacks.gotTypeDef(objectToken, JavaTokenTypes.LITERAL_class);

        // 4. Object name (or "Companion" for companion objects)
        String name = declaration.getName();
        if (name != null && declaration.getNameIdentifier() != null) {
            LocatableToken nameToken = createToken(declaration.getNameIdentifier(), JavaTokenTypes.IDENT);
            callbacks.gotTypeDefName(nameToken);
        } else if (declaration.isCompanion()) {
            LocatableToken nameToken = createTokenWithText(declaration, "Companion", JavaTokenTypes.IDENT);
            callbacks.gotTypeDefName(nameToken);
        }

        // 5. Process supertypes (objects can implement interfaces)
        processNestedClassSupertypes(declaration);

        // 6. Object body
        KtClassBody body = declaration.getBody();
        if (body != null) {
            PsiElement lBrace = body.getLBrace();
            PsiElement rBrace = body.getRBrace();

            if (lBrace != null && rBrace != null) {
                LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                callbacks.beginTypeBody(lBraceToken);

                // Create new ClassVisitor for object body
                ClassVisitor objectVisitor = new ClassVisitor(callbacks);
                for (KtDeclaration decl : body.getDeclarations()) {
                    decl.accept(objectVisitor);
                }

                LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                callbacks.endTypeBody(rBraceToken, true);
            }
        }

        // 7. End declaration
        callbacks.gotTypeDefEnd(objectToken, true);
    }

    // ==================== Init Blocks ====================

    /**
     * Visits an init block (class initializer).
     *
     * <p><b>Context-Aware:</b> Init blocks are class-level constructs that execute
     * during object construction.</p>
     *
     * <h3>Callback Sequence</h3>
     * <ol>
     *   <li>{@code gotDeclBegin(initToken)} - Begin init block</li>
     *   <li>{@code beginInitBlock(initToken, lBraceToken)} - Begin block</li>
     *   <li>Body traversal via {@link MethodBodyVisitor}</li>
     *   <li>{@code endInitBlock(rBraceToken, true)} - End block</li>
     * </ol>
     *
     * @param initializer The init block PSI element
     */
    @Override
    public void visitClassInitializer(@NotNull KtClassInitializer initializer) {
        // 1. Begin init block
        PsiElement initKeyword = initializer.getInitKeyword();

        LocatableToken initToken = createToken(initKeyword, JavaTokenTypes.LITERAL_init);
        callbacks.gotDeclBegin(initToken);

        boolean begunInit = false;
        LocatableToken lastToken = null;

        // 2. Process init block body
        KtExpression bodyExpr = initializer.getBody();
        if (bodyExpr instanceof KtBlockExpression block) {
            PsiElement lBrace = block.getLBrace();

            if (lBrace != null) {
                LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                callbacks.beginInitBlock(initToken, lBraceToken);
                begunInit = true;
            }

            // Delegate to MethodBodyVisitor
            MethodBodyVisitor bodyVisitor = new MethodBodyVisitor(callbacks);
            block.accept(bodyVisitor);

            PsiElement rBrace = block.getRBrace();
            if (rBrace != null) {
                LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                lastToken = rBraceToken;
            }
        }

        if (lastToken == null) {
            lastToken = createToken(initializer.getLastChild());
        }

        // 3. End init block
        if (begunInit) {
            callbacks.endInitBlock(lastToken, true);
        } else {
            callbacks.endDecl(lastToken);
        }
    }

    // ==================== Constructor Declarations ====================

    /**
     * Visits a primary constructor declaration.
     *
     * <p><b>Context-Aware:</b> Primary constructors are class-level constructs that
     * appear in the class header.</p>
     *
     * <h3>Callback Sequence</h3>
     * <ol>
     *   <li>{@code gotDeclBegin(token)} - Begin constructor declaration</li>
     *   <li>{@code gotModifier(token)} × n - Process modifiers</li>
     *   <li>{@code gotConstructorDecl(token, javadoc, name)} - Constructor declaration</li>
     *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
     *   <li>Parameter processing</li>
     *   <li>{@code gotAllMethodParameters()} - Parameters complete</li>
     *   <li>{@code endMethodDecl(token, true)} - End constructor (CLASS CONTEXT)</li>
     * </ol>
     *
     * @param constructor The primary constructor PSI element
     */
    @Override
    public void visitPrimaryConstructor(@NotNull KtPrimaryConstructor constructor) {
        // 1. Begin declaration
        LocatableToken declToken = null;

        // 2. Process modifiers
        ModifierSet modifiers = extractModifiers(constructor.getModifierList());
        if (!modifiers.modifierTokens().isEmpty()) {
            declToken = modifiers.firstModifierToken();
            callbacks.gotDeclBegin(declToken);
            for (ModifierToken modToken : modifiers.modifierTokens()) {
                callbacks.gotModifier(modToken.token());
            }
        } else if (constructor.getConstructorKeyword() != null) {
            declToken = createToken(constructor.getConstructorKeyword(), JavaTokenTypes.LITERAL_constructor);
            callbacks.gotDeclBegin(declToken);
        } else {
            KtParameterList paramList = constructor.getValueParameterList();
            if (paramList != null && paramList.getLeftParenthesis() != null) {
                declToken = createToken(paramList.getLeftParenthesis(), JavaTokenTypes.LPAREN);
                callbacks.gotDeclBegin(declToken);
            } else {
                return; // Cannot process without any tokens
            }
        }

        // 3. Constructor declaration (use class name)
        PsiElement parent = constructor.getParent();
        LocatableToken nameToken;
        if (parent instanceof KtClass ktClass) {
            PsiElement classNameId = ktClass.getNameIdentifier();
            if (classNameId != null) {
                nameToken = createToken(classNameId, JavaTokenTypes.IDENT);
            } else {
                nameToken = createToken(constructor, JavaTokenTypes.IDENT);
            }
        } else {
            nameToken = createToken(constructor, JavaTokenTypes.IDENT);
        }

        callbacks.gotConstructorDecl(declToken, null, nameToken.getText());

        // 4. Modifiers consumed
        callbacks.modifiersConsumed();

        // 5. Parameters
        FunctionParametersResult params = extractConstructorParameters(constructor);
        if (params.hasParameterList()) {
            for (ParameterData param : params.parameters()) {
                if (param.firstToken() != null) {
                    callbacks.beginFormalParameter(param.firstToken());
                }
                callbacks.gotTypeSpec(param.typeTokens());
                if (param.nameToken() != null) {
                    callbacks.gotMethodParameter(param.nameToken(), param.varargToken());
                }
            }
            callbacks.gotAllMethodParameters();
        }

        // 6. End declaration - CLASS CONTEXT: use endMethodDecl
        KtParameterList paramList = constructor.getValueParameterList();
        if (paramList == null || paramList.getRightParenthesis() == null) {
            callbacks.endDecl(getLastToken());
            return;
        }

        LocatableToken endToken = createToken(paramList.getRightParenthesis(), JavaTokenTypes.RPAREN);
        callbacks.endMethodDecl(endToken, true);
    }

    /**
     * Visits a secondary constructor declaration.
     *
     * <p><b>Context-Aware:</b> Secondary constructors appear in the class body and
     * have explicit bodies with delegation calls.</p>
     *
     * <h3>Callback Sequence</h3>
     * <ol>
     *   <li>{@code gotDeclBegin(token)} - Begin constructor declaration</li>
     *   <li>{@code gotModifier(token)} × n - Process modifiers</li>
     *   <li>{@code gotConstructorDecl(token, javadoc, name)} - Constructor declaration</li>
     *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
     *   <li>Parameter processing</li>
     *   <li>{@code gotAllMethodParameters()} - Parameters complete</li>
     *   <li>{@code beginMethodBody(lBrace)} - Begin body (if present)</li>
     *   <li>Body traversal via {@link MethodBodyVisitor}</li>
     *   <li>{@code endMethodBody(rBrace, true)} - End body</li>
     *   <li>{@code endMethodDecl(token, true)} - End constructor (CLASS CONTEXT)</li>
     * </ol>
     *
     * @param constructor The secondary constructor PSI element
     */
    @Override
    public void visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor) {
        if (constructor == null) {
            return;
        }

        // Skip if no callbacks configured
        if (callbacks == null) {
            super.visitSecondaryConstructor(constructor);
            return;
        }

        // 1. Begin declaration
        LocatableToken declToken = null;

        // 2. Process modifiers
        ModifierSet modifiers = extractModifiers(constructor.getModifierList());
        if (!modifiers.modifierTokens().isEmpty()) {
            declToken = modifiers.firstModifierToken();
            callbacks.gotDeclBegin(declToken);
            for (ModifierToken modToken : modifiers.modifierTokens()) {
                callbacks.gotModifier(modToken.token());
            }
        } else if (constructor.getConstructorKeyword() != null) {
            declToken = createToken(constructor.getConstructorKeyword(), JavaTokenTypes.LITERAL_constructor);
            callbacks.gotDeclBegin(declToken);
        } else {
            KtParameterList paramList = constructor.getValueParameterList();
            if (paramList != null && paramList.getLeftParenthesis() != null) {
                declToken = createToken(paramList.getLeftParenthesis(), JavaTokenTypes.LPAREN);
                callbacks.gotDeclBegin(declToken);
            } else {
                return; // Cannot process without any tokens
            }
        }

        // 3. Constructor declaration (use class name)
        PsiElement parent = constructor.getParent();
        if (parent instanceof KtClassBody) {
            parent = parent.getParent();
        }
        LocatableToken nameToken;
        if (parent instanceof KtClass ktClass) {
            PsiElement classNameId = ktClass.getNameIdentifier();
            if (classNameId != null) {
                nameToken = createToken(classNameId, JavaTokenTypes.IDENT);
            } else {
                nameToken = createToken(constructor.getConstructorKeyword(), JavaTokenTypes.IDENT);
            }
        } else {
            nameToken = createToken(constructor, JavaTokenTypes.IDENT);
        }

        callbacks.gotConstructorDecl(declToken, null, nameToken.getText());

        // 4. Modifiers consumed
        callbacks.modifiersConsumed();

        // 5. Parameters
        FunctionParametersResult params = extractConstructorParameters(constructor);
        if (params.hasParameterList()) {
            for (ParameterData param : params.parameters()) {
                if (param.firstToken() != null) {
                    callbacks.beginFormalParameter(param.firstToken());
                }
                callbacks.gotTypeSpec(param.typeTokens());
                if (param.nameToken() != null) {
                    callbacks.gotMethodParameter(param.nameToken(), param.varargToken());
                }
            }
            callbacks.gotAllMethodParameters();
        }

        // 6. Constructor body (if present)
        KtBlockExpression body = constructor.getBodyExpression();
        LocatableToken endToken = null;

        if (body != null) {
            PsiElement lBrace = body.getLBrace();
            if (lBrace != null) {
                LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                callbacks.beginMethodBody(lBraceToken);
            }

            // Delegate to MethodBodyVisitor
            MethodBodyVisitor bodyVisitor = new MethodBodyVisitor(callbacks);
            body.accept(bodyVisitor);

            PsiElement rBrace = body.getRBrace();
            if (rBrace != null) {
                LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                callbacks.endMethodBody(rBraceToken, true);
                endToken = rBraceToken;
            }
        }

        // 7. End declaration - CLASS CONTEXT: use endMethodDecl
        if (endToken == null) {
            KtParameterList paramList = constructor.getValueParameterList();
            if (paramList != null && paramList.getRightParenthesis() != null) {
                endToken = createToken(paramList.getRightParenthesis(), JavaTokenTypes.RPAREN);
            }
        }

        if (endToken != null) {
            callbacks.endMethodDecl(endToken, true);
        } else {
            callbacks.endDecl(getLastToken());
        }
    }

    // ==================== Class Body Handling ====================

    /**
     * Visits a class body, processing all member declarations.
     *
     * <p>This method is called when traversing into a class body. It processes
     * all declarations using this ClassVisitor's context-aware callbacks.</p>
     *
     * @param classBody The class body PSI element
     */
    @Override
    public void visitClassBody(@NotNull KtClassBody classBody) {
        PsiElement lBrace = classBody.getLBrace();
        PsiElement rBrace = classBody.getRBrace();
        boolean typeBodyStarted = false;

        if (lBrace != null) {
            LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
            callbacks.beginTypeBody(lBraceToken);
            typeBodyStarted = callbacks.isInEmitRange(lBraceToken);
        }

        // DELEGATION: Use ClassVisitor for all member declarations
        ClassVisitor classVisitor = new ClassVisitor(callbacks);
        for (KtDeclaration declaration : classBody.getDeclarations()) {
            declaration.accept(classVisitor);
        }

        if (typeBodyStarted) {
            if (rBrace != null) {
                LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                callbacks.endTypeBody(rBraceToken, true);
            } else {
                var token = this.getTokenStream().nextToken();
                if (lBrace != null) {
                    callbacks.endTypeBody(token, false);
                }
            }
        }
    }

    /**
     * Visits an enum entry within an enum class.
     *
     * <p>Enum entries are currently treated as opaque constructs - full
     * enum support may be added in a future phase.</p>
     *
     * @param enumEntry The enum entry PSI element
     */
    @Override
    public void visitEnumEntry(@NotNull KtEnumEntry enumEntry) {
        // TODO: Full enum entry support in future phase
        // Currently skipped to avoid incomplete handling
    }


    // ==================== Helper Methods ====================

    /**
     * Processes Kotlin modifiers and invokes corresponding callbacks.
     *
     * <p>Maps Kotlin modifiers to Java equivalents for callback invocation.</p>
     *
     * @param modifierList The Kotlin modifier list to process (must not be null)
     */
    private void processModifiers(KtModifierList modifierList) {
        if (modifierList == null) {
            return;
        }

        // Use BaseVisitor helper to extract modifiers
        ModifierSet modifiers = extractModifiers(modifierList);
        for (ModifierToken modToken : modifiers.modifierTokens()) {
            callbacks.gotModifier(modToken.token());
        }
    }

    /**
     * Processes superclass and implemented interfaces.
     *
     * <p>Kotlin syntax: First supertype with constructor call is superclass,
     * remaining supertypes are interfaces.</p>
     *
     * @param classOrObject The Kotlin class or object with potential supertypes
     */
    private void processSuperTypes(KtClassOrObject classOrObject) {
        List<KtSuperTypeListEntry> superTypeEntries = classOrObject.getSuperTypeListEntries();
        if (superTypeEntries.isEmpty()) {
            return;
        }

        // Find superclass (first entry with constructor call)
        KtSuperTypeListEntry superClassEntry = null;
        List<KtSuperTypeListEntry> interfaceEntries = new ArrayList<>();

        for (KtSuperTypeListEntry entry : superTypeEntries) {
            if (entry instanceof KtSuperTypeCallEntry) {
                if (superClassEntry == null) {
                    superClassEntry = entry;
                } else {
                    interfaceEntries.add(entry);
                }
            } else {
                interfaceEntries.add(entry);
            }
        }

        // Process superclass
        if (superClassEntry != null) {
            processSuperClass(superClassEntry);
        }

        // Process interfaces
        if (!interfaceEntries.isEmpty()) {
            processInterfaces(interfaceEntries);
        }
    }

    /**
     * Processes the superclass (extends).
     */
    private void processSuperClass(KtSuperTypeListEntry superClassEntry) {
        KtTypeReference typeRef = superClassEntry.getTypeReference();
        if (typeRef == null) {
            return;
        }

        LocatableToken extendsToken = createToken(typeRef, JavaTokenTypes.IDENT);
        callbacks.beginTypeDefExtends(extendsToken);
        callbacks.endTypeDefExtends();
    }

    /**
     * Processes implemented interfaces.
     */
    private void processInterfaces(List<KtSuperTypeListEntry> interfaceEntries) {
        if (interfaceEntries.isEmpty()) {
            return;
        }

        KtTypeReference firstInterface = interfaceEntries.get(0).getTypeReference();
        if (firstInterface != null) {
            LocatableToken implToken = createToken(firstInterface, JavaTokenTypes.IDENT);
            callbacks.beginTypeDefImplements(implToken);
            callbacks.endTypeDefImplements();
        }
    }
}
