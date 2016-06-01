package bluej.stride.framedjava.ast;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import bluej.parser.JavaParser;
import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.stride.framedjava.elements.BreakElement;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.ClassElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.CommentElement;
import bluej.stride.framedjava.elements.ConstructorElement;
import bluej.stride.framedjava.elements.IfElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.elements.ReturnElement;
import bluej.stride.framedjava.elements.ThrowElement;
import bluej.stride.framedjava.elements.VarElement;
import bluej.stride.framedjava.elements.WhileElement;
import bluej.utility.JavaUtils;
import bluej.utility.Utility;

/**
 * A parser for parsing Java with the purpose of converting it to Stride.
 * 
 * The JavaParser class does not build an AST, but rather makes callbacks
 * as it parses.  Thus it is up to us to keep track of state and build
 * a Stride AST.  We keep track of state in two ways:
 * 
 *  - We have handlers for expressions, statements, etc, rather than a state
 *    machine with say integer states.  If we parse a block, we install a
 *    statement handler to accept statements and join them into a list.
 *    Handlers work well where we know in advance that we expect a particular
 *    item.  We always use stacks of handlers, because the constructs
 *    can usually nest, so outer handlers are often there waiting for some
 *    inner item to finish and be dealt with.
 *    
 *  - In some other cases, e.g. modifiers, rather than having upfront handlers,
 *    we just build a stack of things we've seen, to be dealt with once
 *    we know what it is later on.
 */
class JavaStrideParser extends JavaParser
{
    /** The original source code being transformed */
    private final String source;
    /** 
     * The stack of expression handlers.
     * The top item, if any, gets passed expression begin/end events.
     * If the stack is empty, the expression is ignored.
     */
    private final Stack<ExpressionHandler> expressionHandlers = new Stack<>();
    private final Stack<StatementHandler> statementHandlers = new Stack<>();
    /**
     * The stack of actual-argument handlers.
     * The top item, if any, gets passed argument list begin/another/end events.
     * If the stack is empty, the arguments are ignored.
     */
    private final Stack<ArgumentListHandler> argumentHandlers = new Stack<>();
    private final Stack<TypeDefHandler> typeDefHandlers = new Stack<>();
    private final Stack<MethodDetails> methods = new Stack<>();
    /**
     * Types are an awkward case.  Sometimes you know in advance that you
     * expect a type (e.g. after "throws" or "extends").  Other times, you
     * only know what a type is for once you've seen later tokens
     * (e.g. if you see a type in a method, it could be beginning a field
     * or a method).  So we need both handlers for types we expect,
     * and a list/stack of types we have seen but will handle later.
     * The rule is simple: if typeHandlers is not empty, that receives
     * the type.  Otherwise, the type gets put on the top of the prevTypes stack.
     * 
     * Note that types don't nest (unlike, say, expressions), so we don't
     * have to worry about keeping track of the outermost.
     * 
     * Also note that typeHandlers are not popped by default; you must
     * do the popping.
     */
    private final Stack<String> prevTypes = new Stack<>();
    private final Stack<Consumer<String>> typeHandlers = new Stack<>();
    
    private final Stack<FieldOrVarDetails> curField = new Stack<>();
    /**
     * Modifiers seen.  Each time we start seeing modifiers, we push a new list
     * on to the stack, then add them to the list at the top of the stack.
     * When modifiersConsumed is called, we pop the stack.  If you want to use
     * the modifiers, you must peek (NOT pop!) before modifiersConsumed is called.
     * 
     * I'm fairly confident that modifier lists can't nest, which would mean this
     * stack is only ever 0 or 1 items high, but it doesn't harm to have a stack
     * and it fits with the design of everything else.
     */
    private final Stack<List<LocatableToken>> modifiers = new Stack<>();
    /**
     * The list of comments we have seen since they were last dealt with.
     * Each item includes it's /* * / (space here to avoid ending this comment!) or // delimiters
     * The list gets cleared once the comments have been dealt with.
     */
    private final List<String> comments = new ArrayList<>();

    /**
     * Any warnings encountered in the conversion process.
     */
    private final List<String> warnings = new ArrayList<>();
    private int startThrows;
    
    private final BlockCollector result = new BlockCollector();

    JavaStrideParser(String java)
    {
        super(new StringReader(java), true);
        this.source = java;
        statementHandlers.push(result);
    }
    
    private static class MethodDetails
    {
        public final String name;
        public final List<LocatableToken> modifiers = new ArrayList<>();
        public final List<ParamFragment> parameters = new ArrayList<>();
        public final List<String> throwsTypes = new ArrayList<>();
        public final String comment; // may be null
        public String constructorCall; // may be null
        public List<Expression> constructorArgs; // may be null
        public final String type;

        public MethodDetails(String type, String name, List<LocatableToken> modifiers, String comment)
        {
            this.type = type;
            this.name = name;
            this.modifiers.addAll(modifiers);
            this.comment = comment;
        }
    }
    
    private static class FieldOrVarDetails
    {
        public final String type;
        public final List<LocatableToken> modifiers = new ArrayList<>();

        public FieldOrVarDetails(String type, List<LocatableToken> modifiers)
        {
            this.type = type;
            this.modifiers.addAll(modifiers);
        }
    }
    
    private static class Expression
    {
        public final String stride;
        public final String java;

        private Expression(String stride, String java)
        {
            this.java = java;
            this.stride = stride;
        }
    }

    private static interface StatementHandler
    {
        public void foundStatement(List<CodeElement> statements);

        default public void endBlock()
        {
        }

        ;
    }

    private static interface ExpressionHandler
    {
        public void expressionBegun(LocatableToken start);

        // Return true if we should be removed from the stack
        public boolean expressionEnd(LocatableToken end);
    }

    private static interface TypeDefHandler
    {
        public void typeDefBegun(LocatableToken start);

        public void typeDefEnd(LocatableToken end);

        public void gotName(String name);

        public void startedClass(List<LocatableToken> modifiers);

        void gotContent(List<CodeElement> content);

        void typeDefExtends(String type);
        
        void typeDefImplements(String type);
    }

    private static interface ArgumentListHandler
    {
        public void argumentListBegun();

        public void gotArgument();

        public void argumentListEnd();
    }

    private class BlockCollector implements StatementHandler
    {
        private final List<CodeElement> content = new ArrayList<>();

        @Override
        public void foundStatement(List<CodeElement> statements)
        {
            content.addAll(statements);
            // We keep ourselves on the stack until someone removes us:
            withStatement(this);
        }

        public List<CodeElement> getContent()
        {
            return content;
        }
    }

    private class IfBuilder implements StatementHandler
    {
        // Size is always >= 1, and either equal to blocks.size, or one less than blocks.size (if last one is else)
        private final ArrayList<FilledExpressionSlotFragment> conditions = new ArrayList<>();
        private final ArrayList<List<CodeElement>> blocks = new ArrayList<>();

        public IfBuilder(Expression condition)
        {
            this.conditions.add(toFilled(condition));
        }

        public void addCondBlock()
        {
            blocks.add(new ArrayList<>());
            withStatement(this);
        }

        public void addElseIf()
        {
            withExpression(e -> conditions.add(toFilled(e)));
        }

        public void endIf()
        {
            JavaStrideParser.this.foundStatement(new IfElement(null,
                conditions.get(0), blocks.get(0),
                conditions.subList(1, conditions.size()), blocks.subList(1, conditions.size()),
                blocks.size() > conditions.size() ? blocks.get(blocks.size() - 1) : null,
                true
            ));
        }

        @Override
        public void foundStatement(List<CodeElement> statements)
        {
            blocks.get(blocks.size() - 1).addAll(statements);
        }
    }

    @Override
    protected void beginWhileLoop(LocatableToken token)
    {
        super.beginWhileLoop(token);
        withExpression(exp -> {
            withStatement(body -> {
                foundStatement(new WhileElement(null, toFilled(exp), body, true));
            });
        });
    }

    @Override
    protected void beginIfStmt(LocatableToken token)
    {
        super.beginIfStmt(token);
        withExpression(exp -> {
            withStatement(new IfBuilder(exp));
        });
    }

    @Override
    protected void beginIfCondBlock(LocatableToken token)
    {
        super.beginIfCondBlock(token);
        getIfBuilder(false).addCondBlock();
    }

    @Override
    protected void gotElseIf(LocatableToken token)
    {
        super.gotElseIf(token);
        getIfBuilder(false).addElseIf();
    }

    @Override
    protected void endIfStmt(LocatableToken token, boolean included)
    {
        super.endIfStmt(token, included);
        getIfBuilder(true).endIf();
    }

    // If true, pop it from stack.  If false, peek and leave it on stack
    private IfBuilder getIfBuilder(boolean pop)
    {
        if (statementHandlers.peek() instanceof IfBuilder)
            return (IfBuilder)(pop ? statementHandlers.pop() : statementHandlers.peek());
        else
            return null;
    }

    private FilledExpressionSlotFragment toFilled(Expression exp)
    {
        return new FilledExpressionSlotFragment(exp.stride, exp.java);
    }

    private OptionalExpressionSlotFragment toOptional(Expression exp)
    {
        return new OptionalExpressionSlotFragment(exp.stride, exp.java);
    }

    @Override
    protected void gotReturnStatement(boolean hasValue)
    {
        super.gotReturnStatement(hasValue);
        if (hasValue)
            withExpression(exp -> foundStatement(new ReturnElement(null, toOptional(exp), true)));
        else
            foundStatement(new ReturnElement(null, toOptional(new Expression("", "")), true));
    }

    @Override
    protected void gotEmptyStatement()
    {
        super.gotEmptyStatement();
        foundStatements(Collections.emptyList());
    }

    @Override
    protected void gotStatementExpression()
    {
        super.gotStatementExpression();
        withExpression(e -> foundStatement(new CallElement(null, new CallExpressionSlotFragment(e.stride, e.java), true)));
    }

    @Override
    protected void beginExpression(LocatableToken token)
    {
        super.beginExpression(token);
        if (!expressionHandlers.isEmpty())
            expressionHandlers.peek().expressionBegun(token);
    }

    @Override
    protected void endExpression(LocatableToken token, boolean emptyExpression)
    {
        super.endExpression(token, emptyExpression);
        if (!expressionHandlers.isEmpty())
        {
            if (expressionHandlers.peek().expressionEnd(token))
                expressionHandlers.pop();
        }
    }

    @Override
    protected void beginStmtblockBody(LocatableToken token)
    {
        super.beginStmtblockBody(token);
        withStatement(new StatementHandler()
        {
            final ArrayList<CodeElement> block = new ArrayList<>();

            @Override
            public void endBlock()
            {
                // We were just collecting the block -- pass to parent handler:
                foundStatements(block);
            }

            @Override
            public void foundStatement(List<CodeElement> statements)
            {
                block.addAll(statements);
                //By default we are popped; re-add:
                withStatement(this);
            }
        });
    }

    @Override
    protected void endStmtblockBody(LocatableToken token, boolean included)
    {
        super.endStmtblockBody(token, included);
        statementHandlers.pop().endBlock();
    }

    @Override
    protected void beginThrows(LocatableToken token)
    {
        super.beginThrows(token);
        typeHandlers.push(type -> methods.peek().throwsTypes.add(type));
    }

    @Override
    protected void endThrows()
    {
        super.endThrows();
        typeHandlers.pop();
    }

    @Override
    protected void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken)
    {
        super.gotConstructorDecl(token, hiddenToken);
        methods.push(new MethodDetails(null, null, modifiers.peek(), getJavadoc()));
        withStatement(new BlockCollector());
    }

    @Override
    protected void gotMethodDeclaration(LocatableToken nameToken, LocatableToken hiddenToken)
    {
        super.gotMethodDeclaration(nameToken, hiddenToken);
        methods.push(new MethodDetails(prevTypes.pop(), nameToken.getText(), modifiers.peek(), getJavadoc()));
        withStatement(new BlockCollector());
    }

    @Override
    protected void endMethodDecl(LocatableToken token, boolean included)
    {
        super.endMethodDecl(token, included);
        List<CodeElement> body = ((BlockCollector)statementHandlers.pop()).getContent();
        MethodDetails details = methods.pop();
        String name = details.name;
        List<ThrowsTypeFragment> throwsTypes = details.throwsTypes.stream().map(t -> new ThrowsTypeFragment(toType(t))).collect(Collectors.toList());
        List<LocatableToken> modifiers = details.modifiers;
        //Note: this modifies the list:
        AccessPermission permission = removeAccess(modifiers, AccessPermission.PROTECTED);
        if (name != null)
        {
            boolean _final = modifiers.removeIf(t -> t.getText().equals("final"));
            boolean _static = modifiers.removeIf(t -> t.getText().equals("static"));
            // Any remaining are unrecognised:
            warnUnsupportedModifiers("method", modifiers);
            String type = details.type;
            foundStatement(new NormalMethodElement(null, new AccessPermissionFragment(permission),
                _static, _final, toType(type), new NameDefSlotFragment(name), details.parameters,
                throwsTypes, body, new JavadocUnit(details.comment), true));
        }
        else
        {
            // Any remaining are unrecognised:
            warnUnsupportedModifiers("method", modifiers);
            SuperThis delegate = SuperThis.fromString(details.constructorCall);
            Expression delegateArgs = delegate == null ? null : new Expression(
                details.constructorArgs.stream().map(e -> e.stride).collect(Collectors.joining(",")),
                details.constructorArgs.stream().map(e -> e.java).collect(Collectors.joining(","))
            );
            foundStatement(new ConstructorElement(null, new AccessPermissionFragment(permission),
                details.parameters,
                throwsTypes, delegate == null ? null : new SuperThisFragment(delegate), delegateArgs == null ? null : new SuperThisParamsExpressionFragment(delegateArgs.stride, delegateArgs.java), body, new JavadocUnit(details.comment), true));
        }
    }

    private static TypeSlotFragment toType(String t)
    {
        if (t == null)
            return null;
        else
            return new TypeSlotFragment(t, t);
    }

    private void warnUnsupportedModifiers(String context, List<LocatableToken> modifiers)
    {
        modifiers.forEach(t -> warnings.add("Unsupported " + context + " modifier: " + t.getText()));
    }

    private static AccessPermission removeAccess(List<LocatableToken> modifiers, AccessPermission defaultAccess)
    {
        // If they make the item package-visible, we will turn this into defaultAccess:
        AccessPermission permission = defaultAccess;
        // These are not else-if, so that we remove all recognised modifiers:
        if (modifiers.removeIf(t -> t.getText().equals("private")))
            permission = AccessPermission.PRIVATE;
        if (modifiers.removeIf(t -> t.getText().equals("protected")))
            permission = AccessPermission.PROTECTED;
        if (modifiers.removeIf(t -> t.getText().equals("public")))
            permission = AccessPermission.PUBLIC;
        return permission;
    }

    @Override
    protected void gotTypeSpec(List<LocatableToken> tokens)
    {
        super.gotTypeSpec(tokens);
        String type = tokens.stream().map(LocatableToken::getText).collect(Collectors.joining());
        if (!typeHandlers.isEmpty())
            typeHandlers.peek().accept(type);
        else
            prevTypes.add(type);
    }

    @Override
    protected void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken)
    {
        super.gotMethodParameter(token, ellipsisToken);
        if (ellipsisToken != null) //TODO or support it?
            warnings.add("Unsupported feature: varargs");
        String type = prevTypes.pop();
        methods.peek().parameters.add(new ParamFragment(toType(type), new NameDefSlotFragment(token.getText())));
    }

    @Override
    protected void gotConstructorCall(LocatableToken token)
    {
        super.gotConstructorCall(token);
        MethodDetails method = methods.peek();
        method.constructorCall = token.getText();
        // This will be parsed as an expression statement, so we need to cancel
        // that and replace with our own handler to discard:
        expressionHandlers.pop();
        withExpression(e -> {
        });
        withArgumentList(args -> {
            method.constructorArgs = args;
        });
    }

    @Override
    protected void gotDeclBegin(LocatableToken token)
    {
        super.gotDeclBegin(token);
        modifiers.push(new ArrayList<>());
    }

    @Override
    protected void beginFormalParameter(LocatableToken token)
    {
        super.beginFormalParameter(token);
        modifiers.push(new ArrayList<>());
    }

    @Override
    protected void modifiersConsumed()
    {
        super.modifiersConsumed();
        modifiers.pop();
    }

    @Override
    protected void gotModifier(LocatableToken token)
    {
        super.gotModifier(token);
        if (!modifiers.isEmpty())
            modifiers.peek().add(token);
    }

    @Override
    protected void beginFieldDeclarations(LocatableToken first)
    {
        super.beginFieldDeclarations(first);
        curField.push(new FieldOrVarDetails(prevTypes.pop(), modifiers.peek()));
    }

    @Override
    protected void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows)
    {
        super.gotField(first, idToken, initExpressionFollows);
        handleFieldOrVar(idToken, initExpressionFollows, AccessPermission.PROTECTED);
    }

    @Override
    protected void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited)
    {
        super.gotVariableDecl(first, idToken, inited);
        curField.push(new FieldOrVarDetails(prevTypes.pop(), modifiers.peek()));
        handleFieldOrVar(idToken, inited, null);
    }

    private void handleFieldOrVar(LocatableToken idToken, boolean initExpressionFollows, AccessPermission defaultAccess)
    {
        FieldOrVarDetails details = curField.peek();
        // Important we take copy:
        List<LocatableToken> modifiers = new ArrayList<>(details.modifiers);
        //Note: this modifies the list:
        AccessPermission permission = removeAccess(modifiers, defaultAccess);
        boolean _final = modifiers.removeIf(t -> t.getText().equals("final"));
        boolean _static = modifiers.removeIf(t -> t.getText().equals("static"));
        // Any remaining are unrecognised:
        warnUnsupportedModifiers("variable", modifiers);

        Consumer<Expression> handler = e -> foundStatement(new VarElement(null,
            permission == null ? null : new AccessPermissionFragment(permission), _static, _final,
            toType(details.type), new NameDefSlotFragment(idToken.getText()),
            e == null ? null : toFilled(e), true));
        if (initExpressionFollows)
            withExpression(handler);
        else
            handler.accept(null);
    }

    @Override
    protected void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows)
    {
        super.gotSubsequentField(first, idToken, initFollows);
        handleFieldOrVar(idToken, initFollows, AccessPermission.PROTECTED);
    }

    @Override
    protected void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited)
    {
        super.gotSubsequentVar(first, idToken, inited);
        handleFieldOrVar(idToken, inited, null);
    }

    @Override
    protected void endFieldDeclarations(LocatableToken token, boolean included)
    {
        super.endFieldDeclarations(token, included);
        curField.pop();
    }

    @Override
    protected void gotTopLevelDecl(LocatableToken token)
    {
        super.gotTopLevelDecl(token);
        withTypeDef(td -> foundStatement(td));
    }

    @Override
    protected void gotTypeDef(LocatableToken firstToken, int tdType)
    {
        super.gotTypeDef(firstToken, tdType);
        if (tdType == TYPEDEF_EPIC_FAIL)
            return;
        if (!typeDefHandlers.isEmpty())
            typeDefHandlers.peek().typeDefBegun(firstToken);
        List<LocatableToken> modifiers = this.modifiers.peek();
        switch (tdType)
        {
            case TYPEDEF_CLASS:
                if (!typeDefHandlers.isEmpty())
                    typeDefHandlers.peek().startedClass(modifiers);
                break;
        }
    }

    @Override
    protected void gotTypeDefEnd(LocatableToken token, boolean included)
    {
        super.gotTypeDefEnd(token, included);
        if (!typeDefHandlers.isEmpty())
        {
            typeDefHandlers.peek().typeDefEnd(token);
        }
    }

    @Override
    protected void beginTypeDefExtends(LocatableToken extendsToken)
    {
        super.beginTypeDefExtends(extendsToken);
        typeHandlers.push(type -> {
            typeDefHandlers.peek().typeDefExtends(type);
        });
    }

    @Override
    protected void endTypeDefExtends()
    {
        super.endTypeDefExtends();
        typeHandlers.pop();
    }

    @Override
    protected void beginTypeDefImplements(LocatableToken implementsToken)
    {
        super.beginTypeDefImplements(implementsToken);
        typeHandlers.push(type -> {
            typeDefHandlers.peek().typeDefImplements(type);
        });
    }

    @Override
    protected void endTypeDefImplements()
    {
        super.endTypeDefImplements();
        typeHandlers.pop();
    }

    @Override
    protected void gotTypeDefName(LocatableToken nameToken)
    {
        super.gotTypeDefName(nameToken);
        if (!typeDefHandlers.isEmpty())
            typeDefHandlers.peek().gotName(nameToken.getText());
    }

    @Override
    protected void beginTypeBody(LocatableToken leftCurlyToken)
    {
        super.beginTypeBody(leftCurlyToken);
        withStatement(new BlockCollector());
    }

    @Override
    protected void endTypeBody(LocatableToken endCurlyToken, boolean included)
    {
        super.endTypeBody(endCurlyToken, included);
        List<CodeElement> content = ((BlockCollector)statementHandlers.pop()).getContent();
        if (!typeDefHandlers.isEmpty())
            typeDefHandlers.peek().gotContent(content);
    }

    @Override
    public void gotComment(LocatableToken token)
    {
        super.gotComment(token);
        String comment = token.getText();
        if (comment.startsWith("//"))
            comment = comment.substring(2).trim();
        else
            comment = JavaUtils.javadocToString(comment);
        comment = Arrays.stream(Utility.split(comment, System.getProperty("line.separator")))
                .map(String::trim)
                .reduce((a, b) -> {
                    a = a.isEmpty() ? "\n" : a;
                    if (a.endsWith("\n"))
                        return a + (b.isEmpty() ? "\n" : b);
                    else if (b.isEmpty())
                        return a + "\n";
                    else
                        return a + " " + b;
                }).orElse("");
        comments.add(comment);
    }

    @Override
    protected void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken)
    {
        super.gotBreakContinue(keywordToken, labelToken);
        if (keywordToken.getType() == JavaTokenTypes.LITERAL_break)
        {
            foundStatement(new BreakElement(null, true));
            if (labelToken != null)
                warnings.add("Unsupported feature: label on break");
        }
        else
        {
            warnings.add("Unsupported feature: " + keywordToken.getText());
        }
    }

    @Override
    protected void gotThrow(LocatableToken token)
    {
        super.gotThrow(token);
        withExpression(e -> foundStatement(new ThrowElement(null, toFilled(e), true)));
    }

    @Override
    protected void beginArgumentList(LocatableToken token)
    {
        super.beginArgumentList(token);
        if (!argumentHandlers.isEmpty())
            argumentHandlers.peek().argumentListBegun();
    }

    @Override
    protected void endArgumentList(LocatableToken token)
    {
        super.endArgumentList(token);
        if (!argumentHandlers.isEmpty())
            argumentHandlers.peek().argumentListEnd();
    }

    @Override
    protected void endArgument()
    {
        super.endArgument();
        if (!argumentHandlers.isEmpty())
            argumentHandlers.peek().gotArgument();
    }

    private String getJavadoc()
    {
        if (!comments.isEmpty())
            return comments.remove(comments.size() - 1);
        else
            return null;
    }

    private String getText(LocatableToken start, LocatableToken end)
    {
        return source.substring(start.getPosition(), end.getPosition());
    }

    /**
     * Passes the next complete expression to the handler.
     * 
     * This will consume the next outermost expression.  Inner expressions
     * will be ignored unless another handler is installed in the mean-time.
     * 
     * e.g. Let's say you call withExpression(h).  Then you parse:
     * 0+(1+2)+3;
     * 
     * The 0 will be the start of the outermost expression.  The 1 will
     * also begin an expression, but this handler will just ignore
     * the fact that there was an inner expression, and will wait
     * until the 3 which ends the expression, and will pass
     * "0+(1+2)+3" to the handler.  It will not pass "1+2".
     * 
     * @param handler The callback for the next outermost expression.
     */
    private void withExpression(Consumer<Expression> handler)
    {
        expressionHandlers.push(new ExpressionHandler()
        {
            LocatableToken start;
            // Amount of expressions begun but not ending:
            int outstanding = 0;

            @Override
            public void expressionBegun(LocatableToken start)
            {
                // Only record first begin:
                if (outstanding == 0)
                    this.start = start;
                outstanding += 1;
            }

            @Override
            public boolean expressionEnd(LocatableToken end)
            {
                outstanding -= 1;
                // If the outermost has finished, pass it to handler:
                if (outstanding == 0)
                {
                    String java = getText(start, end);
                    handler.accept(new Expression(replaceInstanceof(java), uniformSpacing(java)));
                    // Finished now:
                    return true;
                }
                else
                    // Not finished yet:
                    return false;
            }
        });
    }
    
    private static interface TypeDefDelegate
    {
        public void gotName(String name);

        public CodeElement end();

        public void gotContent(CodeElement element);

        void gotImplements(String type);

        void gotExtends(String type);
    }
    
    private class ClassDelegate implements TypeDefDelegate
    {
        private final List<LocatableToken> modifiers;
        private String name;
        private final List<VarElement> fields = new ArrayList<>();
        private final List<ConstructorElement> constructors = new ArrayList<>();
        private final List<NormalMethodElement> methods = new ArrayList<>();
        private String extendsType;
        private final List<String> implementsTypes = new ArrayList<>();

        public ClassDelegate(List<LocatableToken> modifiers)
        {
            this.modifiers = new ArrayList<>(modifiers);
        }

        @Override
        public void gotName(String name)
        {
            this.name = name;
        }

        @Override
        public void gotExtends(String type)
        {
            extendsType = type;
        }

        @Override
        public void gotImplements(String type)
        {
            implementsTypes.add(type);
        }

        @Override
        public void gotContent(CodeElement element)
        {
            if (element instanceof VarElement)
                fields.add((VarElement)element);
            else if (element instanceof ConstructorElement)
                constructors.add((ConstructorElement)element);
            else if (element instanceof NormalMethodElement)
                methods.add((NormalMethodElement)element);
            else
                warnings.add("Unsupported class member: " + element.getClass());
        }

        @Override
        public CodeElement end()
        {
            boolean _abstract = modifiers.removeIf(t -> t.getText().equals("abstract"));
            // Public is the default so don't warn it's unsupported:
            modifiers.remove("public");
            warnUnsupportedModifiers("class", modifiers);
            return new ClassElement(null, null, _abstract, new NameDefSlotFragment(name),
                toType(extendsType),
                implementsTypes.stream().map(t -> toType(t)).collect(Collectors.toList()), fields,
                constructors, methods,
                null, null, Collections.emptyList(), true);
        }
    }

    private void withTypeDef(Consumer<CodeElement> handler)
    {
        typeDefHandlers.push(new TypeDefHandler()
        {
            TypeDefDelegate delegate = null;
            // Amount of typeDefs begun but not ending:
            int outstanding = 0;

            @Override
            public void typeDefBegun(LocatableToken start)
            {
                // Only record first begin:
                outstanding += 1;
            }

            @Override
            public void typeDefEnd(LocatableToken end)
            {
                outstanding -= 1;
                if (outstanding == 0)
                {
                    handler.accept(delegate.end());
                }
            }

            @Override
            public void startedClass(List<LocatableToken> modifiers)
            {
                if (outstanding == 1)
                    delegate = new ClassDelegate(modifiers);
            }

            @Override
            public void gotName(String name)
            {
                if (outstanding == 1)
                    delegate.gotName(name);
            }

            @Override
            public void gotContent(List<CodeElement> content)
            {
                if (outstanding == 1)
                    content.forEach(delegate::gotContent);
            }

            @Override
            public void typeDefImplements(String type)
            {
                if (outstanding == 1)
                    delegate.gotImplements(type);
            }

            @Override
            public void typeDefExtends(String type)
            {
                if (outstanding == 1)
                    delegate.gotExtends(type);
            }
        });
    }

    private void withStatement(StatementHandler handler)
    {
        statementHandlers.push(handler);
    }

    private void withArgumentList(Consumer<List<Expression>> argHandler)
    {
        argumentHandlers.push(new ArgumentListHandler()
        {
            final List<Expression> args = new ArrayList<>();
            int outstanding = 0;

            @Override
            public void argumentListBegun()
            {
                outstanding += 1;
                if (outstanding == 1)
                {
                    withExpression(args::add);
                }
            }

            @Override
            public void gotArgument()
            {
                if (outstanding == 1)
                {
                    withExpression(args::add);
                }
            }

            @Override
            public void argumentListEnd()
            {
                if (outstanding == 1)
                {
                    // We were expecting another argument; cancel that:
                    expressionHandlers.pop();
                    argHandler.accept(args);
                    // Remove us from the handlers:
                    argumentHandlers.pop();
                }
                outstanding -= 1;
            }
        });
    }

    private void foundStatement(CodeElement statement)
    {
        foundStatements(Collections.singletonList(statement));
    }

    private void foundStatements(List<CodeElement> statements)
    {
        if (comments.isEmpty())
            statementHandlers.pop().foundStatement(statements);
        else
        {
            ArrayList<CodeElement> all = new ArrayList<>();
            all.add(new CommentElement(comments.stream().collect(Collectors.joining(" "))));
            comments.clear();
            all.addAll(statements);
            statementHandlers.pop().foundStatement(all);
        }
    }

    public List<CodeElement> getCodeElements()
    {
        return result.getContent();
    }

    // package-visible for testing
    static String replaceInstanceof(String src)
    {
        // It is a bit inefficient to re-lex the string, but
        // it's easiest this way and conversion is not particularly time sensitive:
        JavaLexer lexer = new JavaLexer(new StringReader(src));
        StringBuilder r = new StringBuilder();
        while (true)
        {
            LocatableToken token = lexer.nextToken();
            if (token.getType() == JavaTokenTypes.EOF)
                return r.toString();
            if (r.length() != 0)
                r.append(" ");
            if (token.getType() == JavaTokenTypes.LITERAL_instanceof)
                r.append("<:");
            else
                r.append(token.getText());
        }
    }

    static String uniformSpacing(String src)
    {
        // It is a bit inefficient to re-lex the string, but
        // it's easiest this way and conversion is not particularly time sensitive:
        JavaLexer lexer = new JavaLexer(new StringReader(src));
        StringBuilder r = new StringBuilder();
        while (true)
        {
            LocatableToken token = lexer.nextToken();
            if (token.getType() == JavaTokenTypes.EOF)
                return r.toString();
            if (r.length() != 0)
                r.append(" ");
            r.append(token.getText());
        }
    }

}
