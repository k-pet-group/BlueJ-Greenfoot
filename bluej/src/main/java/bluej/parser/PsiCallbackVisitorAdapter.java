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
 * <p>Wraps a {@link JavaParserCallbacks} instance and delegates all callback invocations
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
 * @see JavaParserCallbacks The wrapped callback interface
 * @see bluej.parser.psi.PsiCallbackVisitor The consumer of this adapter
 */
@OnThread(Tag.Any)
public class PsiCallbackVisitorAdapter extends JavaParserCallbacks {
    
    /**
     * The wrapped callbacks instance to delegate to.
     */
    private final JavaParserCallbacks delegate;
    
    /**
     * Creates an adapter wrapping the given callbacks instance.
     * 
     * @param delegate The JavaParserCallbacks instance to wrap
     * @throws NullPointerException if delegate is null
     */
    public PsiCallbackVisitorAdapter(JavaParserCallbacks delegate) {
        if (delegate == null) {
            throw new NullPointerException("Delegate callbacks must not be null");
        }
        this.delegate = delegate;
    }
    
    // ==================== Public Delegation Methods ====================
    // These expose protected JavaParserCallbacks methods as public for PsiCallbackVisitor
    
    /**
     * Public delegation to {@link JavaParserCallbacks#gotDeclBegin(LocatableToken)}.
     */
    public void invokeDeclBegin(LocatableToken token) {
        delegate.gotDeclBegin(token);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacks#modifiersConsumed()}.
     */
    public void invokeModifiersConsumed() {
        delegate.modifiersConsumed();
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacks#gotTypeDef(LocatableToken, int)}.
     */
    public void invokeTypeDef(LocatableToken token, int tdType) {
        delegate.gotTypeDef(token, tdType);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacks#gotTypeDefName(LocatableToken)}.
     */
    public void invokeTypeDefName(LocatableToken nameToken) {
        delegate.gotTypeDefName(nameToken);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacks#beginTypeBody(LocatableToken)}.
     */
    public void invokeBeginTypeBody(LocatableToken leftCurlyToken) {
        delegate.beginTypeBody(leftCurlyToken);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacks#endTypeBody(LocatableToken, boolean)}.
     */
    public void invokeEndTypeBody(LocatableToken endCurlyToken, boolean included) {
        delegate.endTypeBody(endCurlyToken, included);
    }
    
    /**
     * Public delegation to {@link JavaParserCallbacks#gotTypeDefEnd(LocatableToken, boolean)}.
     */
    public void invokeTypeDefEnd(LocatableToken token, boolean included) {
        delegate.gotTypeDefEnd(token, included);
    }
}