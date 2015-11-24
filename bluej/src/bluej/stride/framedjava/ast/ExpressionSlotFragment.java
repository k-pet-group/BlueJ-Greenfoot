/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.ast;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.ast.links.PossibleMethodUseLink;
import bluej.stride.framedjava.ast.links.PossibleTypeLink;
import bluej.stride.framedjava.ast.links.PossibleVarLink;
import javafx.application.Platform;
import bluej.parser.JavaParser;
import bluej.parser.lexer.LocatableToken;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.errors.UndeclaredVariableInExpressionError;
import bluej.stride.framedjava.errors.UndeclaredVariableLvalueError;
import bluej.stride.framedjava.errors.UnneededSemiColonError;
import bluej.stride.framedjava.frames.AssignFrame;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Utility;

public abstract class ExpressionSlotFragment extends StringSlotFragment
{
    private ExpressionSlot slot;
    private String javaCode;
    
    // Each plain is a non-compound ident
    private final List<LocatableToken> plains = new ArrayList<>();
    private List<LocatableToken> curCompound = null;
    private final List<List<LocatableToken>> compounds = new ArrayList<>();
    private final List<List<LocatableToken>> types = new ArrayList<>();
    private Map<String, CodeElement> vars;
    private AssignFrame assignmentLHSParent; // non-null iff we are the LHS of an assignment

    // Constructor when generated from slot
    public ExpressionSlotFragment(String content, String javaCode, ExpressionSlot slot)
    {
        super(content);
        this.javaCode = javaCode;
        this.slot = slot;
        
        // Temporary, while migrating from old XML:
        if (javaCode == null)
            this.javaCode = content;
        
        Parser.parseAsExpression(new JavaParser(new StringReader(wrapForParse(this.javaCode)), false)
        {
            @Override
            protected void gotIdentifier(LocatableToken token)
            {
                plains.add(unwrapForParse(token));
            }

            @Override
            protected void gotIdentifierEOF(LocatableToken token)
            {
                plains.add(unwrapForParse(token));
            }

            @Override
            protected void gotArrayTypeIdentifier(LocatableToken token)
            {
                // Stop it calling gotIdentifier
            }

            @Override
            protected void gotParentIdentifier(LocatableToken token)
            {
                // Stop it calling gotIdentifier
            }

            @Override
            protected void gotCompoundIdent(LocatableToken token)
            {
                if (curCompound != null)
                    throw new IllegalStateException();
                curCompound = new ArrayList<>();
                curCompound.add(token);
            }

            @Override
            protected void gotCompoundComponent(LocatableToken token)
            {
                if (curCompound == null || curCompound.isEmpty())
                    throw new IllegalStateException();
                curCompound.add(token);
            }

            @Override
            protected void gotMemberAccess(LocatableToken token)
            {
                //compounds.add(finishCompound(token));
            }

            @Override
            protected void completeCompoundValue(LocatableToken token)
            {
                compounds.add(finishCompound(token));
            }

            private List<LocatableToken> finishCompound(LocatableToken token)
            {
                if (curCompound == null || curCompound.isEmpty())
                    throw new IllegalStateException();
                curCompound.add(token);
                List<LocatableToken> r = Utility.mapList(curCompound, ExpressionSlotFragment.this::unwrapForParse);
                curCompound = null;
                return r;
            }

            @Override
            protected void completeCompoundClass(LocatableToken token)
            {
                types.add(finishCompound(token));
            }

            @Override
            protected void gotTypeSpec(List<LocatableToken> tokens)
            {
                types.add(Utility.mapList(tokens, ExpressionSlotFragment.this::unwrapForParse));
            }


        });
    }
    
    // Constructor when deserialised from XML
    public ExpressionSlotFragment(String content, String javaCode)
    {
        this(content, javaCode, null);
    }
    
    // Copy constructor
    public ExpressionSlotFragment(ExpressionSlotFragment f)
    {
        this(f.content, f.javaCode);
    }
    
    // Used by XML serialisation:
    public String getJavaCode()
    {
        return javaCode;
    }

    @Override
    public String getJavaCode(Destination dest, ExpressionSlot<?> completing)
    {
        // If we are code completing, use the exact text:
        if (!dest.substitute() || slot == completing || (javaCode != null && Parser.parseableAsExpression(wrapForParse(javaCode))))
            return javaCode;
        
        return "true";
    }
    
    @Override
    public ExpressionSlot getSlot()
    {
        return slot;
    }

    public void registerSlot(ExpressionSlot slot)
    {
        if (this.slot == null)
            this.slot = slot;
    }

    /**
     * Returns false if this expression can be empty and still valid for compilation,
     * or true if this expression is required for compilation
     * @return
     */
    protected abstract boolean isRequired();
    
    // By default, no modification:
    protected String wrapForParse(String orig)
    {
        return orig;
    }
    // By default, no unwrapping:
    protected LocatableToken unwrapForParse(LocatableToken token)
    {
        return token;
    }

    @Override
    public Stream<SyntaxCodeError> findEarlyErrors()
    {
        if (content != null && content.endsWith(";"))
            // Must check this before general parse errors:
            return Stream.of(new UnneededSemiColonError(this, () -> getSlot().setText(content.substring(0, content.length() - 1))));
        else if (content != null && content.isEmpty() && isRequired())
            return Stream.of(new SyntaxCodeError(this, "Expression cannot be empty"));
        else if (content == null || !Parser.parseableAsExpression(wrapForParse(getJavaCode())))
            return Stream.of(new SyntaxCodeError(this, "Invalid expression"));
        else
            return Stream.empty();
    }
    
    
    @Override
    public Future<List<CodeError>> findLateErrors(InteractionManager editor, CodeElement parent)
    {
        CompletableFuture<List<CodeError>> f = new CompletableFuture<>();
        Platform.runLater(() -> ASTUtility.withLocalsParamsAndFields(parent, editor, getPosInSourceDoc(), includeDirectDecl(), vars -> {
            this.vars = vars;

            //Debug.message("This: " + getClass() + " " + this + "+" + getPosInSourceDoc().offset);
            //Debug.message("Vars: " + this.vars.keySet().stream().collect(Collectors.joining(", ")));
            //Debug.message("Plains: " + plains.stream().map(t -> t.getText()).collect(Collectors.joining(", ")));
            
            //Debug.message("Assign LHS: " + assignmentLHSParent + " Java: \"" + getJavaCode() + "\"");
            
            f.complete(plains.stream().map(identToken -> {
                if (!vars.containsKey(identToken.getText())) {
                    if (assignmentLHSParent != null && identToken.getText().equals(getJavaCode())) {
                        return new UndeclaredVariableLvalueError(this, assignmentLHSParent, vars.keySet());
                    }
                    return new UndeclaredVariableInExpressionError(this, identToken.getText(), identToken.getColumn() - 1, 
                            identToken.getColumn() - 1 + identToken.getLength(), slot, vars.keySet());
                }
                return null;
            }).filter(x -> x != null).collect(Collectors.toList()));
            // TODO errors for compounds and types
        }));
        return f;
    }

    /**
     * Whether to include our parent's declarations when looking for variables.  You usually
     * don't want to do this.  For example, a declared variable in a var frame is not in scope
     * in the initialisation expression.  However, this is needed for a special case with constructors,
     * where a constructor's params are in scope for the super/this line, which is a direct child.
     * (For other method-related items, the method is a grandparent, e.g. parent of assignment which
     * is parent of expression, so we still don't need to include the *parent*)
     */
    protected boolean includeDirectDecl()
    {
        return false;
    }

    public void markAssignmentLHS(AssignFrame parent)
    {
        assignmentLHSParent = parent;
    }

    public Map<String, CodeElement> getVars()
    {
        return vars;
    }
}
