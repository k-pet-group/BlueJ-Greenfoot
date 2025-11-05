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
package bluej.parser;

import bluej.parser.lexer.LocatableToken;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Adapter to expose protected JavaParserCallbacks methods as public.
 * 
 * <p>This adapter solves the package access problem: {@code PsiCallbackVisitor} is in
 * {@code bluej.parser.psi} but needs to invoke protected methods from {@code JavaParserCallbacks}
 * in {@code bluej.parser}. The adapter pattern cleanly separates concerns:</p>
 * <ul>
 *   <li>PsiCallbackVisitor stays in its logical package (bluej.parser.psi)</li>
 *   <li>Adapter resides in bluej.parser where it can access protected methods</li>
 *   <li>Adapter exposes public delegation methods for visitor use</li>
 * </ul>
 * 
 * <h2>Design Pattern: Adapter (Wrapper)</h2>
 * <p>Wraps a {@link JavaParserCallbacksBase} instance and delegates all callback invocations
 * to it, exposing protected methods as public for cross-package access.</p>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In PsiCallbackVisitor (bluej.parser.psi package):
 * JavaParserCallbacks actualCallbacks = ...;  // e.g., CallbackRecorder
 * PsiCallbackVisitorAdapter adapter = new PsiCallbackVisitorAdapter(actualCallbacks);
 * adapter.gotDeclBegin(token);  // Public method calls protected method
 * }</pre>
 * 
 * <h2>Performance</h2>
 * <p>Zero overhead - simple delegation with no additional logic or state.</p>
 * 
 * @see JavaParserCallbacksBase The wrapped callback interface
 * @see bluej.parser.psi.PsiCallbackVisitor The consumer of this adapter
 */
@OnThread(Tag.FXPlatform)
public class PsiCallbackVisitorAdapter extends JavaParserCallbacksBase {
    
    /**
     * The wrapped callbacks instance to delegate to.
     */
    private final JavaParserCallbacksBase delegate;
    
    /**
     * Creates an adapter wrapping the given callbacks instance.
     * 
     * @param delegate The JavaParserCallbacks instance to wrap
     * @throws NullPointerException if delegate is null
     */
    public PsiCallbackVisitorAdapter(JavaParserCallbacksBase delegate) {
        if (delegate == null) {
            throw new NullPointerException("Delegate callbacks must not be null");
        }
        this.delegate = delegate;
    }
    
    // ==================== Public Delegation Methods ====================
    // These expose protected JavaParserCallbacks methods as public for PsiCallbackVisitor
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotDeclBegin(LocatableToken)}.
     */
    public void invokeDeclBegin(LocatableToken token) {
        delegate.gotDeclBegin(token);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotModifier(LocatableToken)}.
     */
    public void invokeModifier(LocatableToken token) {
        delegate.gotModifier(token);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#modifiersConsumed()}.
     */
    public void invokeModifiersConsumed() {
        delegate.modifiersConsumed();
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotTypeDef(LocatableToken, int)}.
     */
    public void invokeTypeDef(LocatableToken token, int tdType) {
        delegate.gotTypeDef(token, tdType);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotTypeDefName(LocatableToken)}.
     */
    public void invokeTypeDefName(LocatableToken nameToken) {
        delegate.gotTypeDefName(nameToken);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#beginTypeBody(LocatableToken)}.
     */
    public void invokeBeginTypeBody(LocatableToken leftCurlyToken) {
        delegate.beginTypeBody(leftCurlyToken);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#endTypeBody(LocatableToken, boolean)}.
     */
    public void invokeEndTypeBody(LocatableToken endCurlyToken, boolean included) {
        delegate.endTypeBody(endCurlyToken, included);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotTypeDefEnd(LocatableToken, boolean)}.
     */
    public void invokeTypeDefEnd(LocatableToken token, boolean included) {
        delegate.gotTypeDefEnd(token, included);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#beginTypeDefExtends(LocatableToken)}.
     */
    public void beginTypeDefExtends(LocatableToken token) {
        delegate.beginTypeDefExtends(token);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#endTypeDefExtends()}.
     */
    public void endTypeDefExtends() {
        delegate.endTypeDefExtends();
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#beginTypeDefImplements(LocatableToken)}.
     */
    public void beginTypeDefImplements(LocatableToken token) {
        delegate.beginTypeDefImplements(token);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#endTypeDefImplements()}.
     */
    public void endTypeDefImplements() {
        delegate.endTypeDefImplements();
    }
    
    // ==================== Phase 4 Method Declaration Callbacks ====================
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotMethodDeclaration(LocatableToken, LocatableToken)}.
     */
    public void invokeMethodDeclaration(LocatableToken nameToken, LocatableToken javadocToken) {
        delegate.gotMethodDeclaration(nameToken, javadocToken);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotTypeSpec(java.util.List)}.
     */
    public void invokeTypeSpec(java.util.List<LocatableToken> tokens) {
        delegate.gotTypeSpec(tokens);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotMethodTypeParamsBegin()}.
     */
    public void invokeMethodTypeParamsBegin() {
        delegate.gotMethodTypeParamsBegin();
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotTypeParam(LocatableToken)}.
     */
    public void invokeTypeParam(LocatableToken idToken) {
        delegate.gotTypeParam(idToken);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotTypeParamBound(java.util.List)}.
     */
    public void invokeTypeParamBound(java.util.List<LocatableToken> tokens) {
        delegate.gotTypeParamBound(tokens);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#endMethodTypeParams()}.
     */
    public void invokeEndMethodTypeParams() {
        delegate.endMethodTypeParams();
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotMethodParameter(LocatableToken, LocatableToken)}.
     */
    public void invokeMethodParameter(LocatableToken nameToken, LocatableToken ellipsisToken) {
        delegate.gotMethodParameter(nameToken, ellipsisToken);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotAllMethodParameters()}.
     */
    public void invokeAllMethodParameters() {
        delegate.gotAllMethodParameters();
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#beginMethodBody(LocatableToken)}.
     */
    public void invokeBeginMethodBody(LocatableToken token) {
        delegate.beginMethodBody(token);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#endMethodBody(LocatableToken, boolean)}.
     */
    public void invokeEndMethodBody(LocatableToken token, boolean included) {
        delegate.endMethodBody(token, included);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#endMethodDecl(LocatableToken, boolean)}.
     */
    public void invokeEndMethodDecl(LocatableToken token, boolean included) {
        delegate.endMethodDecl(token, included);
    }
    
    // ==================== Phase 4 Constructor Declaration Callbacks ====================
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotConstructorDecl(LocatableToken, LocatableToken)}.
     */
    public void invokeConstructorDecl(LocatableToken nameToken, LocatableToken javadocToken) {
        delegate.gotConstructorDecl(nameToken, javadocToken);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#beginInitBlock(LocatableToken, LocatableToken)}.
     */
    public void invokeBeginInitBlock(LocatableToken first, LocatableToken lcurly) {
        delegate.beginInitBlock(first, lcurly);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#endInitBlock(LocatableToken, boolean)}.
     */
    public void invokeEndInitBlock(LocatableToken rcurly, boolean included) {
        delegate.endInitBlock(rcurly, included);
    }
    
    // ==================== Phase 4 Field/Property Declaration Callbacks ====================
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#beginFieldDeclarations(LocatableToken)}.
     */
    public void invokeBeginFieldDeclarations(LocatableToken first) {
        delegate.beginFieldDeclarations(first);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#gotField(LocatableToken, LocatableToken, boolean)}.
     */
    public void invokeField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) {
        delegate.gotField(first, idToken, initExpressionFollows);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#endField(LocatableToken, boolean)}.
     */
    public void invokeEndField(LocatableToken token, boolean included) {
        delegate.endField(token, included);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#endFieldDeclarations(LocatableToken, boolean)}.
     */
    public void invokeEndFieldDeclarations(LocatableToken token, boolean included) {
        delegate.endFieldDeclarations(token, included);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacksBase#endDecl(LocatableToken)}.
     */
    public void invokeEndDecl(LocatableToken token) {
        delegate.endDecl(token);
    }
}