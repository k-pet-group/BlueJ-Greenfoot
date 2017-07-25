/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2017 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.convert;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.parser.JavaParser;
import bluej.parser.ParseFailure;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.ParamFragment;
import bluej.stride.framedjava.ast.SuperThis;
import bluej.stride.framedjava.ast.SuperThisFragment;
import bluej.stride.framedjava.ast.ThrowsTypeFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.convert.ConversionWarning.UnsupportedFeature;
import bluej.stride.framedjava.elements.BreakElement;
import bluej.stride.framedjava.elements.CaseElement;
import bluej.stride.framedjava.elements.ClassElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.CommentElement;
import bluej.stride.framedjava.elements.ConstructorElement;
import bluej.stride.framedjava.elements.ForeachElement;
import bluej.stride.framedjava.elements.IfElement;
import bluej.stride.framedjava.elements.ImportElement;
import bluej.stride.framedjava.elements.InterfaceElement;
import bluej.stride.framedjava.elements.MethodProtoElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.elements.ReturnElement;
import bluej.stride.framedjava.elements.SwitchElement;
import bluej.stride.framedjava.elements.ThrowElement;
import bluej.stride.framedjava.elements.TryElement;
import bluej.stride.framedjava.elements.VarElement;
import bluej.stride.framedjava.elements.WhileElement;
import bluej.utility.JavaUtils;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

import static bluej.parser.lexer.JavaTokenTypes.SL_COMMENT;

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
public class JavaStrideParser extends JavaParser
{
    /** The original source code being transformed */
    private final String source;
    /** 
     * The stack of expression handlers.
     * The top item, if any, gets passed expression begin/end events.
     * If the stack is empty, the expression is ignored.
     */
    private final Stack<ExpressionBuilder> expressionHandlers = new Stack<>();
    /**
     * The stack of statement/defintion handlers.
     * The top item, if any, gets passed completed statements/definitions.
     * Should never be empty
     */
    private final Stack<StatementHandler> statementHandlers = new Stack<>();
    /**
     * The stack of actual-argument handlers.
     * The top item, if any, gets passed argument list begin/another/end events.
     * If the stack is empty, the arguments are ignored.
     */
    private final Stack<ArgumentListHandler> argumentHandlers = new Stack<>();
    /**
     * The stack of types currently being defined.
     */
    private final Stack<TypeDefHandler> typeDefHandlers = new Stack<>();
    /**
     * The stack of methods currently being defined.
     */
    private final Stack<MethodBuilder> methods = new Stack<>();
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
    // See comment for prevTypes
    private final Stack<Consumer<String>> typeHandlers = new Stack<>();
    /**
     * The stack of if-statements currently being defined.
     */
    private final Stack<IfBuilder> ifHandlers = new Stack<>();
    /**
     * The stack of switch-statements currently being defined.
     */
    private final Stack<SwitchHandler> switchHandlers = new Stack<>();
    /**
     * The stack of for/foreach-statements currently being defined.
     */
    private final Stack<ForHandler> forHandlers = new Stack<>();

    /**
     * The stack of field/variable declarations currently being defined.
     */
    private final Stack<FieldOrVarBuilder> curField = new Stack<>();
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
    private final Stack<List<Modifier>> modifiers = new Stack<>();
    /**
     * Whether we are testing.  If so, we use a special string for all warning comments
     */
    private final boolean testing;

    /**
     * A small class to handle the list of warnings
     */
    private class WarningManager
    {
        /**
         * Any warnings encountered in the conversion process.
         */
        private final List<ConversionWarning> warnings = new ArrayList<>();

        /**
         * Adds a warning, and calls gotComment to add a comment
         */
        public void add(ConversionWarning warning)
        {
            add(warning, JavaStrideParser.this::gotComment);
        }

        /**
         * Adds a warning, and calls the given callback to add a warning comment
         */
        public void add(ConversionWarning warning, Consumer<LocatableToken> commentAdd)
        {
            warnings.add(warning);
            LocatableToken dummyToken = new LocatableToken(SL_COMMENT, "// " + (testing ? ("WARNING:" + warning.getClass().getName()) : warning.getMessage()));
            dummyToken.setPosition(-1, -1, -1, -1, -1, dummyToken.getText().length());
            commentAdd.accept(dummyToken);
        }
    }
    private final WarningManager warnings = new WarningManager();

    /**
     * The top-most StatementHandler, which collects the result of the parse.
     */
    private final StatementHandler result = new StatementHandler(false) { public void endBlock() { } };
    /**
     * The stack of try/catch/finally currently being defined
     */
    private Stack<TryBuilder> tries = new Stack<>();
    /**
     * The name of the package declaration seen in the file (null if none) 
     */
    private String pkg;
    /**
     * The list of imports seen in the file.
     */
    private final List<String> imports = new ArrayList<>();

    /**
     * Constructors a parser for the given Java source code.
     * @param java The source code to parse.
     * @param testing True if we are testing (alters the unsupported warning comment format)
     */
    public JavaStrideParser(String java, boolean testing)
    {
        super(new StringReader(java), true);
        this.source = java;
        this.testing = testing;
        statementHandlers.push(result);
    }

    /**
     * Gets the list of conversion warnings seen (call after parsing)
     */
    public List<ConversionWarning> getWarnings()
    {
        return warnings.warnings;
    }

    private class SwitchHandler
    {
        // The initial expression in the switch
        private Expression expression;
        // The expressions in the cases (one per case)
        private final List<Expression> cases = new ArrayList<>();
        // The code inside each case (one per case)
        private final List<List<CodeElement>> caseContents = new ArrayList<>();
        // The code in the default block.  null if no default
        private List<CodeElement> defaultContents;
        // Keeps track of whether we are in the default block
        private boolean inDefault;

        public void gotSwitchExpression(Expression e)
        {
            this.expression = e;
        }

        // Completes the parse of the switch and returns the switch element.
        public SwitchElement end()
        {
            List<CodeElement> caseFrames = new ArrayList<>();
            for (int i = 0; i < cases.size(); i++)
            {
                caseFrames.add(new CaseElement(null, cases.get(i).toFilled(), caseContents.get(i), true));
            }

            return new SwitchElement(null, expression.toFilled(),
                    caseFrames, defaultContents, true);
        }

        // Begins the block after the switch (...) header
        public void beginBlock()
        {
            withStatement(newHandler());
        }

        private StatementHandler newHandler()
        {
            return new StatementHandler(false)
            {
                @Override
                public void endBlock()
                {
                    storePrevCode(this);
                    JavaStrideParser.this.foundStatement(switchHandlers.pop().end());
                }
            };
        }

        // Called when we have found a new "case expression:", passing the expression
        public void gotCase(Expression e)
        {
            // Important that we store prev code before adding to cases:
            storePrevCode(null);
            cases.add(e);
            withStatement(newHandler());
        }

        // When we find a new case/default label, we must store the code
        // we've seen in the previous case/default bit.
        private void storePrevCode(StatementHandler handler)
        {
            List<CodeElement> prevContent = (handler == null ? statementHandlers.pop() : handler).getContent(false);
            if (cases.isEmpty() && !inDefault)
            {
                // Content before first case; discard it
            }
            else
            {
                if (inDefault)
                    defaultContents.addAll(prevContent);
                else
                    caseContents.add(prevContent);
            }
        }

        // Called when we have found a default: label.
        public void gotDefault()
        {
            storePrevCode(null);
            // Important we set these up after storing previous code:
            inDefault = true;
            defaultContents = new ArrayList<>();
            withStatement(newHandler());
        }
    }

    private class ForHandler
    {
        // The type of the for-each variable, or classic-for variable(s).  null if empty
        private String type;
        // Should always be at least one var for classic-for, or exactly one for for-each:
        private final List<String> vars = new ArrayList<>();
        // Should always be the same size as vars, null indicates no initialiser for that var
        private final List<Expression> inits = new ArrayList<>();
        // Whether this is for-each (true) or classic-for (false)
        private boolean isEach;
        // The expression looped through in a for-each loop (null in classic-for)
        private Expression eachVar;
        // The expression in third-place in a classic-for. null if empty or for-each
        private Expression post;
        // The expression in second-place in a classic-for. null if empty or for-each
        private Expression condition; // null means empty

        // Called when we have seen the type at the start of the for
        public void gotType(String type, List<Modifier> modifiers)
        {
            this.type = type;
            // Our for-each loops are always final so we ignore final modifier:
            modifiers.removeIf(t -> t.isKeyword("final"));
            warnUnsupportedModifiers("for-loop", modifiers);
        }

        // Called when we have seen the name of a loop variable
        public void gotName(String name)
        {
            this.vars.add(name);
            // Assume no initializer unless we find one:
            this.inits.add(null);
        }

        // Called when we know we have a for-each loop.  The expression
        // is the part after the colon.
        public void gotEach(Expression e)
        {
            isEach = true;
            eachVar = e;
        }

        // Called with the initialiser expression of a variable in a classic-for loop
        public void gotVarInit(Expression e)
        {
            this.inits.set(this.vars.size() - 1, e);
        }

        // Ends the for-loop, giving the contents of the body.
        // Returns the loop content.  It's a list because classic-for loops
        // get transformed into initialisers->while 
        public List<CodeElement> end(List<CodeElement> content)
        {
            if (isEach)
                return Arrays.asList(new ForeachElement(null, new TypeSlotFragment(type, type), new NameDefSlotFragment(vars.get(0)), eachVar.toFilled(), content, true));
            else if (type != null && type.equals("int") && vars.size() == 1 && inits.size() == 1 && inits.get(0).isIntegerLiteral() && condition != null && condition.lessThanIntegerLiteral(vars.get(0)) && post != null && post.isIncrementByOne(vars.get(0)))
            {
                // Transform to for each:
                String lowerBound = inits.get(0).getJava();
                String upperBound = condition.getUpperBound();
                return Collections.singletonList(new ForeachElement(null,
                        new TypeSlotFragment("int", "int"),
                        new NameDefSlotFragment(vars.get(0)),
                        new FilledExpressionSlotFragment(lowerBound + ".." + upperBound, "lang.stride.Utility.makeRange(" + lowerBound + ", " + upperBound + ")"),
                        content, true
                    ));
            }
            else
            {
                List<CodeElement> initAndLoop = new ArrayList<>();
                for (int i = 0; i < vars.size(); i++)
                {
                    initAndLoop.add(new VarElement(null, null, false, false, toType(type), new NameDefSlotFragment(vars.get(i)), inits.get(i) == null ? null : inits.get(i).toFilled(), true));
                }
                List<CodeElement> loopBody = new ArrayList<>(content);
                if (post != null)
                    loopBody.add(post.toStatement());
                initAndLoop.add(new WhileElement(null, condition != null ? condition.toFilled() : new FilledExpressionSlotFragment("true", "true"), loopBody, true));
                return initAndLoop;
            }
        }

        // Called when we have seen the third-part of a classic-for.  Pass null if empty
        public void gotPost(Expression e)
        {
            this.post = e;
        }

        // Called when we have seen the second-part of a classic-for.  Pass null if empty
        public void gotCondition(Expression e)
        {
            this.condition = e;
        }
    }

    /**
     * A class to handle statements/declarations when we have seen them.
     * Can be used for single items or for blocks (see constructor).
     */
    private abstract class StatementHandler
    {
        /**
         * The list of items seen.
         */
        private final List<CodeElement> content = new ArrayList<>();
        /**
         * The list of comments we have seen since they were last dealt with.
         * Each item includes it's /* * / (space here to avoid ending this comment!) or // delimiters
         * The list gets cleared once the comments have been dealt with.
         */
        private final List<LocatableToken> comments = new ArrayList<>();
        // See constructor
        private final boolean expectingSingle;

        /**
         * Constructs a statement handler
         * @param expectingSingle Whether we expect one call of foundStatement,
         *     or several until we are manually stopped.  There are various contexts:
         *   - A for-loop, while-loop, if-statement, etc.  In this case
         *     you may find a single statement, or you may find a block,
         *     but the block will get parsed by beginBlockStmtBody which
         *     will install a further StatementHandler to collect the block.
         *     So you put an outer new StatementHandler(true) [i.e. expectingSingle=true]
         *     on the stack.  If it's a single statement, we will get one
         *     statement and finish.  If it's a block, the inner StatementHandler(false)
         *     [expectingSingle=false) will collect it and pass it to us as one
         *     list, and we finish.
         *   - A block has begun.  This might be in beginStmtBlockBody, or
         *     where we know a block occurs (e.g. try-statement, class definition).
         *     In this case, pass expectingSingle=false because we know we are in
         *     a block.  You may need to manually call endBlock yourself in this case.
         */
        public StatementHandler(boolean expectingSingle)
        {
            this.expectingSingle = expectingSingle;

            // Steal comments at current position from outer handler:
            if (!statementHandlers.isEmpty())
            {
                StatementHandler prev = statementHandlers.peek();
                int curPosition = getCurPosition();
                Predicate<LocatableToken> afterCurPosition = t -> t.getPosition() >= curPosition;
                comments.addAll(prev.comments.stream().filter(afterCurPosition).collect(Collectors.toList()));
                prev.comments.removeIf(afterCurPosition);
            }
        }

        // Called when contents have been found.
        public final void foundStatement(List<CodeElement> statements)
        {
            CommentElement el = collateComments(false);
            if (el != null)
                content.add(el);
            content.addAll(statements);
            // Unless we are expecting just one item (e.g. body of while),
            // we keep ourselves on the stack until someone removes us,
            // by putting ourselves back on the stack:
            if (!expectingSingle)
                withStatement(this);
            else
                endBlock();
        }

        // Called to get the content found by this StatementHandler,
        // including any comments.
        public final List<CodeElement> getContent(boolean eof)
        {
            CommentElement el = collateComments(eof);
            if (el != null)
                content.add(el);
            if (!comments.isEmpty())
            {
                // Pass any left-over comments to our parent:
                comments.forEach(statementHandlers.peek()::gotComment);
            }
            return content;
        }

        // Called when a comment has been found while parsing
        public final void gotComment(LocatableToken token)
        {
            comments.add(token);
        }
        
        // Joins together all comments we have seen into one comment element.
        // Pass eof=true to force collection of all comments, even those
        // beyond the point of our current parse.
        private CommentElement collateComments(boolean eof)
        {
            // We collect comments which the token stream has seen in the comments list.
            // But sometimes the parser looks ahead, or puts tokens back into the stream.
            // By then we have seen the comment, but really it's ahead of our current position.
            // So when we gather up the comments, we must only gather those which are behind us:
            int curPosition = getCurPosition();
            Predicate<LocatableToken> behindCurPosition = t -> eof || t.getPosition() < curPosition;
            if (!comments.stream().anyMatch(behindCurPosition))
                return null;
            CommentElement el = new CommentElement(comments.stream().filter(behindCurPosition).map(LocatableToken::getText).map(JavaStrideParser::processComment).collect(Collectors.joining(" ")));
            comments.removeIf(behindCurPosition);
            return el;
        }

        // Works out the current parse position
        private int getCurPosition()
        {
            return (int) Optional.ofNullable(getTokenStream().getMostRecent()).map(LocatableToken::getPosition).orElse(0);
        }

        /**
         * When endBlock is called, this StatementHandler will usually just have been removed from the
         * stack of handlers.  You should use this to deal with the results of the collected block
         * (call getContent to get them).
         */
        public abstract void endBlock();

        // If the most recent comment was a /**..*/ comment, return it, else return null
        public final String getJavadoc()
        {
            if (!comments.isEmpty() && comments.get(comments.size() - 1).getText().startsWith("/**"))
            {
                return processComment(comments.remove(comments.size() - 1).getText());
            }
            return null;
        }

        // Steal the comments from this handler.
        public List<CodeElement> stealComments()
        {
            return Stream.of(collateComments(false)).filter(c -> c != null).collect(Collectors.toList());
        }
    }

    /**
     * A class keeping track of an if/else-if/else block being put together.
     */
    private class IfBuilder
    {
        // The blocks (first is if, then optional else-if, last is optional else)
        private final ArrayList<List<CodeElement>> blocks = new ArrayList<>();
        // Size is always >= 1, and either equal to blocks.size, or one less than blocks.size (if last one is else)
        private final ArrayList<FilledExpressionSlotFragment> conditions = new ArrayList<>();
        
        // Constructs a builder, given the initial if-condition
        public IfBuilder(Expression condition)
        {
            this.conditions.add(condition.toFilled());
        }

        // A new block will follow, either for if, else-if or else
        public void addCondBlock()
        {
            withStatement(new StatementHandler(true)
            {
                @Override
                public void endBlock()
                {
                    blocks.add(getContent(false));
                }
            });
        }

        // Found an else-if, an expression will follow
        public void addElseIf()
        {
            withExpression(e -> conditions.add(e.toFilled()));
        }

        // Ends the whole if.
        public void endIf()
        {
            JavaStrideParser.this.foundStatement(new IfElement(null,
                conditions.get(0), blocks.get(0),
                conditions.subList(1, conditions.size()), blocks.subList(1, conditions.size()),
                blocks.size() > conditions.size() ? blocks.get(blocks.size() - 1) : null,
                true
            ));
        }
    }

    @Override
    protected void beginWhileLoop(LocatableToken token)
    {
        super.beginWhileLoop(token);
        withExpression(exp -> {
            withStatement(new StatementHandler(true) {
                @Override
                public void endBlock()
                {
                    JavaStrideParser.this.foundStatement(new WhileElement(null, exp.toFilled(), getContent(false), true));
                }
            });
        });
    }

    @Override
    protected void beginIfStmt(LocatableToken token)
    {
        super.beginIfStmt(token);
        withExpression(exp -> {
            ifHandlers.add(new IfBuilder(exp));
        });
    }

    @Override
    protected void beginIfCondBlock(LocatableToken token)
    {
        super.beginIfCondBlock(token);
        ifHandlers.peek().addCondBlock();
    }

    @Override
    protected void gotElseIf(LocatableToken token)
    {
        super.gotElseIf(token);
        ifHandlers.peek().addElseIf();
    }
    
    @Override
    protected void endIfStmt(LocatableToken token, boolean included)
    {
        super.endIfStmt(token, included);
        ifHandlers.pop().endIf();
    }

    @Override
    protected void gotReturnStatement(boolean hasValue)
    {
        super.gotReturnStatement(hasValue);
        if (hasValue)
            withExpression(exp -> foundStatement(new ReturnElement(null, exp.toOptional(), true)));
        else
            foundStatement(new ReturnElement(null, null, true));
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
        withExpression(e -> foundStatement(e.toStatement()));
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
    protected void gotBinaryOperator(LocatableToken token)
    {
        super.gotBinaryOperator(token);
        if (!expressionHandlers.isEmpty())
        {
            expressionHandlers.peek().binaryOperator(token);
        }
    }

    @Override
    protected void gotUnaryOperator(LocatableToken token)
    {
        super.gotUnaryOperator(token);
        if (!expressionHandlers.isEmpty())
        {
            expressionHandlers.peek().unaryOperator(token);
        }
    }

    @Override
    protected void gotPostOperator(LocatableToken token)
    {
        super.gotPostOperator(token);
        if (!expressionHandlers.isEmpty())
        {
            expressionHandlers.peek().postOperator(token);
        }
    }

    @Override
    protected void beginStmtblockBody(LocatableToken token)
    {
        super.beginStmtblockBody(token);
        withStatement(new StatementHandler(false)
        {
            @Override
            public void endBlock()
            {
                // We were just collecting the block -- pass to parent handler:
                foundStatements(getContent(false));
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
    protected void gotMethodTypeParamsBegin()
    {
        super.gotMethodTypeParamsBegin();
        warnings.add(new UnsupportedFeature("generic methods"));
    }

    @Override
    protected void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken)
    {
        super.gotConstructorDecl(token, hiddenToken);
        methods.push(new MethodBuilder(null, null, modifiers.peek(), statementHandlers.peek().getJavadoc()));
    }

    @Override
    protected void gotMethodDeclaration(LocatableToken nameToken, LocatableToken hiddenToken)
    {
        super.gotMethodDeclaration(nameToken, hiddenToken);
        methods.push(new MethodBuilder(prevTypes.pop(), nameToken.getText(), modifiers.peek(), statementHandlers.peek().getJavadoc()));
    }

    @Override
    protected void beginMethodBody(LocatableToken token)
    {
        super.beginMethodBody(token);
        methods.peek().hasBody = true;
        withStatement(new StatementHandler(false)
        {
            @Override
            public void endBlock()
            {

            }
        });
    }

    @Override
    protected void endMethodDecl(LocatableToken token, boolean included)
    {
        super.endMethodDecl(token, included);
        MethodBuilder details = methods.pop();
        List<CodeElement> body = details.hasBody ? statementHandlers.pop().getContent(false) : null;
        String name = details.name;
        List<ThrowsTypeFragment> throwsTypes = details.throwsTypes.stream().map(t -> new ThrowsTypeFragment(toType(t))).collect(Collectors.toList());
        List<Modifier> modifiers = details.modifiers;
        //Note: this modifies the list:
        AccessPermission permission = removeAccess(modifiers, AccessPermission.PROTECTED);
        if (name != null)
        {
            boolean _final = modifiers.removeIf(t -> t.isKeyword("final"));
            boolean _static = modifiers.removeIf(t -> t.isKeyword("static"));
            // We determine abstract by lack of body, but we shouldn't warn about it:
            modifiers.removeIf(t -> t.isKeyword("abstract"));
            // We ignore @Override:
            modifiers.removeIf(t -> t.isAnnotation("@Override"));
            // Any remaining are unrecognised:
            warnUnsupportedModifiers("method", modifiers);
            String type = details.type;
            if (details.hasBody)
                foundStatement(new NormalMethodElement(null, new AccessPermissionFragment(permission),
                    _static, _final, toType(type), new NameDefSlotFragment(name), details.parameters,
                    throwsTypes, body, new JavadocUnit(details.comment), true));
            else
                foundStatement(new MethodProtoElement(null, toType(type), new NameDefSlotFragment(name),
                    details.parameters, throwsTypes, new JavadocUnit(details.comment), true));
        }
        else
        {
            // Any remaining are unrecognised:
            warnUnsupportedModifiers("method", modifiers);
            SuperThis delegate = SuperThis.fromString(details.constructorCall);
            Expression delegateArgs = delegate == null ? null : new Expression(details.constructorArgs, " , ", warnings::add);
            foundStatement(new ConstructorElement(null, new AccessPermissionFragment(permission),
                details.parameters,
                throwsTypes, delegate == null ? null : new SuperThisFragment(delegate), delegateArgs == null ? null : delegateArgs.toSuperThis(), body, new JavadocUnit(details.comment), true));
        }
    }

    private static TypeSlotFragment toType(String t)
    {
        if (t == null)
            return null;
        else
            return new TypeSlotFragment(t, t);
    }

    private void warnUnsupportedModifiers(String context, List<Modifier> modifiers)
    {
        modifiers.forEach(t -> warnings.add(new ConversionWarning.UnsupportedModifier(context, t.toString())));
    }

    private static AccessPermission removeAccess(List<Modifier> modifiers, AccessPermission defaultAccess)
    {
        // If they make the item package-visible, we will turn this into defaultAccess:
        AccessPermission permission = defaultAccess;
        // These are not else-if, so that we remove all recognised modifiers:
        if (modifiers.removeIf(t -> t.isKeyword("private")))
            permission = AccessPermission.PRIVATE;
        if (modifiers.removeIf(t -> t.isKeyword("protected")))
            permission = AccessPermission.PROTECTED;
        if (modifiers.removeIf(t -> t.isKeyword("public")))
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
            warnings.add(new ConversionWarning.UnsupportedFeature("varargs"));
        String type = prevTypes.pop();
        methods.peek().parameters.add(new ParamFragment(toType(type), new NameDefSlotFragment(token.getText())));
    }

    @Override
    protected void gotConstructorCall(LocatableToken token)
    {
        super.gotConstructorCall(token);
        MethodBuilder method = methods.peek();
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
            modifiers.peek().add(new Modifier.KeywordModifier(token));
    }

    @Override
    protected void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow)
    {
        super.gotAnnotation(annName, paramsFollow);
        if (!modifiers.isEmpty())
        {
            Modifier.AnnotationModifier ann = new Modifier.AnnotationModifier(annName);
            modifiers.peek().add(ann);
            if (paramsFollow)
                withArgumentList(exps -> ann.setParams(exps));
        }
    }

    @Override
    protected void beginFieldDeclarations(LocatableToken first)
    {
        super.beginFieldDeclarations(first);
        curField.push(new FieldOrVarBuilder(prevTypes.pop(), modifiers.peek()));
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
        curField.push(new FieldOrVarBuilder(prevTypes.pop(), modifiers.peek()));
        handleFieldOrVar(idToken, inited, null);
    }

    @Override
    protected void gotArrayDeclarator()
    {
        super.gotArrayDeclarator();
        if (!prevTypes.isEmpty())
            prevTypes.push(prevTypes.pop() + "[]");
    }

    private void handleFieldOrVar(LocatableToken idToken, boolean initExpressionFollows, AccessPermission defaultAccess)
    {
        FieldOrVarBuilder details = curField.peek();
        // Important we take copy:
        List<Modifier> modifiers = new ArrayList<>(details.modifiers);
        //Note: this modifies the list:
        AccessPermission permission = removeAccess(modifiers, defaultAccess);
        boolean _final = modifiers.removeIf(t -> t.isKeyword("final"));
        boolean _static = modifiers.removeIf(t -> t.isKeyword("static"));
        // Any remaining are unrecognised:
        warnUnsupportedModifiers("variable", modifiers);

        Consumer<Expression> handler = e -> foundStatement(new VarElement(null,
            permission == null ? null : new AccessPermissionFragment(permission), _static, _final,
            toType(details.type), new NameDefSlotFragment(idToken.getText()),
            e == null ? null : e.toFilled(), true));
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
        withTypeDef(td -> foundStatement(td), false);
    }

    @Override
    protected void gotInnerType(LocatableToken start)
    {
        super.gotInnerType(start);
        // Ignore inner class:
        withTypeDef(inner -> {}, true);
        warnings.add(new UnsupportedFeature("inner " + start.getText()));
    }

    @Override
    protected void gotTypeDef(LocatableToken firstToken, int tdType)
    {
        super.gotTypeDef(firstToken, tdType);
        if (tdType == TYPEDEF_EPIC_FAIL)
            return;
        typeDefHandlers.peek().typeDefBegun(firstToken);            
        List<Modifier> modifiers = this.modifiers.peek();
        switch (tdType)
        {
            case TYPEDEF_CLASS:
                typeDefHandlers.peek().startedClass(modifiers, statementHandlers.peek().getJavadoc());
                break;
            case TYPEDEF_INTERFACE:
                typeDefHandlers.peek().startedInterface(modifiers, statementHandlers.peek().getJavadoc());
                break;
            case TYPEDEF_ANNOTATION:
                warnings.add(new UnsupportedFeature("annotation"));
                break;
            case TYPEDEF_ENUM:
                warnings.add(new UnsupportedFeature("enum"));
                break;
            default:
                throw new ParseFailure("Typedef parse failure");

        }
    }

    @Override
    protected void gotTypeDefEnd(LocatableToken token, boolean included)
    {
        super.gotTypeDefEnd(token, included);
        if (!typeDefHandlers.isEmpty())
        {
            typeDefHandlers.pop().typeDefEnd(token);
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
        withStatement(new StatementHandler(false)
        {
            @Override
            public void endBlock()
            {
            }
        });
    }

    @Override
    protected void endTypeBody(LocatableToken endCurlyToken, boolean included)
    {
        super.endTypeBody(endCurlyToken, included);
        List<CodeElement> content = statementHandlers.pop().getContent(false);
        if (!typeDefHandlers.isEmpty())
            typeDefHandlers.peek().gotContent(content);
    }

    @Override
    protected void gotPackage(List<LocatableToken> pkgTokens)
    {
        super.gotPackage(pkgTokens);
        this.pkg = pkgTokens.stream().map(LocatableToken::getText).collect(Collectors.joining());
    }

    @Override
    public void gotComment(LocatableToken token)
    {
        super.gotComment(token);
        statementHandlers.peek().gotComment(token);
        if (!expressionHandlers.isEmpty())
        {
            expressionHandlers.forEach(handler -> {
                handler.beginMask(token);
                handler.endMask(token);
            });
        }
    }

    private static String processComment(String comment)
    {
        if (comment.startsWith("//"))
            comment = comment.substring(2).trim();
        else
            comment = JavaUtils.javadocToString(comment).trim();
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
        return comment;
    }

    @Override
    protected void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken)
    {
        super.gotBreakContinue(keywordToken, labelToken);
        if (keywordToken.getType() == JavaTokenTypes.LITERAL_break)
        {
            foundStatement(new BreakElement(null, true));
            if (labelToken != null)
                warnings.add(new ConversionWarning.UnsupportedFeature("break label"));
        }
        else
        {
            warnings.add(new ConversionWarning.UnsupportedFeature(keywordToken.getText()));
        }
    }

    @Override
    protected void gotThrow(LocatableToken token)
    {
        super.gotThrow(token);
        withExpression(e -> foundStatement(new ThrowElement(null, e.toFilled(), true)));
    }

    @Override
    protected void beginTryCatchSmt(LocatableToken token, boolean hasResource)
    {
        super.beginTryCatchSmt(token, hasResource);
        if (hasResource)
            warnings.add(new ConversionWarning.UnsupportedFeature("try-with-resource"));
        tries.push(new TryBuilder());
    }

    @Override
    protected void beginTryBlock(LocatableToken token)
    {
        super.beginTryBlock(token);
        withStatement(new StatementHandler(false)
        {
            @Override
            public void endBlock()
            {
                tries.peek().tryContent.addAll(getContent(false));
            }
        });
    }

    @Override
    protected void endTryBlock(LocatableToken token, boolean included)
    {
        super.endTryBlock(token, included);
        statementHandlers.pop().endBlock();
    }

    @Override
    protected void gotCatchFinally(LocatableToken token)
    {
        super.gotCatchFinally(token);
        if (token.getType() == JavaTokenTypes.LITERAL_catch)
            tries.peek().catchTypes.push(new ArrayList<>());
        else
            withStatement(new StatementHandler(true)
            {
                @Override
                public void endBlock()
                {
                    tries.peek().finallyContents = new ArrayList<>(getContent(false));
                }
            });

    }

    @Override
    protected void gotCatchVarName(LocatableToken token)
    {
        super.gotCatchVarName(token);
        tries.peek().catchNames.add(token.getText());
        tries.peek().catchTypes.peek().add(prevTypes.pop());
        withStatement(new StatementHandler(true)
        {
            @Override
            public void endBlock()
            {
                tries.peek().catchBlocks.add(getContent(false));
            }
        });
    }

    @Override
    protected void gotMultiCatch(LocatableToken token)
    {
        super.gotMultiCatch(token);
        tries.peek().catchTypes.peek().add(prevTypes.pop());
    }

    @Override
    protected void endTryCatchStmt(LocatableToken token, boolean included)
    {
        super.endTryCatchStmt(token, included);
        TryBuilder details = tries.pop();
        List<TypeSlotFragment> catchTypes = new ArrayList<>();
        List<NameDefSlotFragment> catchNames = new ArrayList<>();
        List<List<CodeElement>> catchBlocks = new ArrayList<>();
        for (int i = 0; i < details.catchNames.size(); i++)
        {
            final int iFinal = i;
            // We replicate each catch block for each type that's inside:
            details.catchTypes.get(i).forEach(type -> {
                catchTypes.add(new TypeSlotFragment(type, type));
                catchNames.add(new NameDefSlotFragment(details.catchNames.get(iFinal)));
                catchBlocks.add(new ArrayList<>(details.catchBlocks.get(iFinal)));

            });
        }
        foundStatement(new TryElement(null, details.tryContent, catchTypes, catchNames, catchBlocks, details.finallyContents, true));

    }

    @Override
    protected void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken)
    {
        super.gotImport(tokens, isStatic, importToken, semiColonToken);
        imports.add(tokens.stream().map(LocatableToken::getText).collect(Collectors.joining()));
    }

    @Override
    protected void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken)
    {
        super.gotWildcardImport(tokens, isStatic, importToken, semiColonToken);
        imports.add(tokens.stream().map(LocatableToken::getText).collect(Collectors.joining()) + ".*");
    }

    @Override
    protected void beginSwitchStmt(LocatableToken token)
    {
        super.beginSwitchStmt(token);
        switchHandlers.push(new SwitchHandler());
        withExpression(e -> switchHandlers.peek().gotSwitchExpression(e));
    }

    @Override
    protected void beginSwitchBlock(LocatableToken token)
    {
        super.beginSwitchBlock(token);
        switchHandlers.peek().beginBlock();
    }

    @Override
    protected void gotSwitchCase()
    {
        super.gotSwitchCase();
        withExpression(e -> switchHandlers.peek().gotCase(e));
    }

    @Override
    protected void gotSwitchDefault()
    {
        super.gotSwitchDefault();
        switchHandlers.peek().gotDefault();
    }

    @Override
    protected void endSwitchBlock(LocatableToken token)
    {
        super.endSwitchBlock(token);
        statementHandlers.pop().endBlock();
    }

    @Override
    protected void beginForLoop(LocatableToken token)
    {
        super.beginForLoop(token);
        forHandlers.push(new ForHandler());
        modifiers.push(new ArrayList<>());

    }

    @Override
    protected void gotForInit(LocatableToken first, LocatableToken idToken)
    {
        super.gotForInit(first, idToken);
        // Whether for or for-each, we will have seen a type:
        forHandlers.peek().gotType(prevTypes.pop(), modifiers.peek());
        forHandlers.peek().gotName(idToken.getText());
    }

    @Override
    protected void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows)
    {
        super.gotSubsequentForInit(first, idToken, initFollows);
        forHandlers.peek().gotName(idToken.getText());
        if (initFollows)
            withExpression(e -> forHandlers.peek().gotVarInit(e));
    }

    @Override
    protected void determinedForLoop(boolean forEachLoop, boolean initFollows)
    {
        super.determinedForLoop(forEachLoop, initFollows);
        if (forEachLoop)
        {
            withExpression(e -> forHandlers.peek().gotEach(e));
        }
        else if (initFollows)
        {
            withExpression(e -> forHandlers.peek().gotVarInit(e));
        }
    }

    @Override
    protected void gotForIncrement(boolean isPresent)
    {
        super.gotForIncrement(isPresent);
        if (isPresent)
            withExpression(e -> forHandlers.peek().gotPost(e));
    }

    @Override
    protected void gotForTest(boolean isPresent)
    {
        super.gotForTest(isPresent);
        if (isPresent)
            withExpression(e -> forHandlers.peek().gotCondition(e));
    }

    @Override
    protected void beginForLoopBody(LocatableToken token)
    {
        super.beginForLoopBody(token);
        withStatement(new StatementHandler(true)
        {
            @Override
            public void endBlock()
            {
                JavaStrideParser.this.foundStatements(forHandlers.pop().end(getContent(false)));
            }
        });
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

    @Override
    protected void gotAssert()
    {
        super.gotAssert();
        warnings.add(new UnsupportedFeature("assert"));
    }

    @Override
    protected void beginSynchronizedBlock(LocatableToken token)
    {
        super.beginSynchronizedBlock(token);
        warnings.add(new UnsupportedFeature("synchronized"));
    }

    @Override
    protected void beginInitBlock(LocatableToken first, LocatableToken lcurly)
    {
        super.beginInitBlock(first, lcurly);
        warnings.add(new UnsupportedFeature("initializer block"));
        // Add a statement handler to soak up and ignore all the statements:
        withStatement(new StatementHandler(false) {
            @Override
            public void endBlock()
            {
            }
        });
    }

    @Override
    protected void endInitBlock(LocatableToken rcurly, boolean included)
    {
        super.endInitBlock(rcurly, included);
        statementHandlers.pop();
    }

    @Override
    protected void beginAnonClassBody(LocatableToken token, boolean isEnumMember)
    {
        super.beginAnonClassBody(token, isEnumMember);
        beginExpressionMask(token);
        warnings.add(new UnsupportedFeature("anonymous class"));
        // Add a statement handler to soak up and ignore all the statements:
        withStatement(new StatementHandler(false) {
            @Override
            public void endBlock()
            {
            }
        });
    }

    private void beginExpressionMask(LocatableToken from)
    {
        if (!expressionHandlers.isEmpty())
            expressionHandlers.peek().beginMask(from);
    }

    private void endExpressionMask(LocatableToken from)
    {
        if (!expressionHandlers.isEmpty())
            expressionHandlers.peek().endMask(from);
    }

    @Override
    protected void endAnonClassBody(LocatableToken token, boolean included)
    {
        super.endAnonClassBody(token, included);
        statementHandlers.pop();
        endExpressionMask(token);
    }

    @Override
    protected void gotLambdaFormalName(LocatableToken name)
    {
        super.gotLambdaFormalName(name);
        warnUnsupportedModifiers("lambda parameter", modifiers.peek());
        // Hide modifiers from expression:
        modifiers.peek().forEach(mod -> {
            expressionHandlers.peek().beginMask(mod.getStart());
            expressionHandlers.peek().endMask(mod.getEnd());
        });
    }

    @Override
    protected void gotLambdaFormalParam()
    {
        super.gotLambdaFormalParam();
        modifiers.push(new ArrayList<>());
    }

    @Override
    protected void gotLambdaFormalType(List<LocatableToken> tokens)
    {
        super.gotLambdaFormalType(tokens);
        warnings.add(new UnsupportedFeature("lambda parameter type"));
        // Hide type from expression:
        expressionHandlers.peek().beginMask(tokens.get(0));
        expressionHandlers.peek().endMask(tokens.get(tokens.size() - 1));
    }

    @Override
    protected void beginLambda(boolean lambdaIsBlock, LocatableToken openCurly)
    {
        super.beginLambda(lambdaIsBlock, openCurly);
        if (lambdaIsBlock)
        {
            warnings.add(new UnsupportedFeature("lambda block"));
            beginExpressionMask(openCurly);
            // Add a statement handler to soak up and ignore all the statements:
            withStatement(new StatementHandler(true) {
                @Override
                public void endBlock()
                {
                }
            });
        }
    }

    @Override
    protected void endLambda(LocatableToken closeCurly)
    {
        super.endLambda(closeCurly);
        if (closeCurly != null)
            endExpressionMask(closeCurly);
    }

    @Override
    protected void gotQuestionColon(LocatableToken token)
    {
        super.gotQuestionColon(token);
        expressionHandlers.peek().beginMask(token);
        expressionHandlers.peek().endMask(token);
    }

    @Override
    protected void gotQuestionOperator(LocatableToken token)
    {
        super.gotQuestionOperator(token);
        warnings.add(new UnsupportedFeature("conditional operator (.. ? .. : ..)"));
        expressionHandlers.peek().beginMask(token);
        expressionHandlers.peek().endMask(token);
    }

    private String getText(LocatableToken start, LocatableToken endExcl, List<Mask> masks)
    {
        if (masks.isEmpty())
            return source.substring(start.getPosition(), endExcl.getPosition());
        else
        {
            int prev = start.getPosition();
            StringBuilder r = new StringBuilder();
            for (Mask m : masks)
            {
                if (m.getStart().getPosition() >= prev && m.getEnd().getPosition() <= endExcl.getPosition())
                {
                    r.append(source.substring(prev, m.getStart().getPosition()));
                    prev = m.getEnd().getPosition() + m.getEnd().getLength();
                }
            }
            r.append(source.substring(prev, endExcl.getPosition()));
            return r.toString();
        }
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
        expressionHandlers.push(new ExpressionBuilder(handler, this::getText, warnings::add));
    }
    
    // A delegate for our TypeDefHandler implementation
    private static interface TypeDefDelegate
    {
        public void gotName(String name);

        public CodeElement end();

        public void gotContent(CodeElement element);

        void gotImplements(String type);

        void gotExtends(String type);
    }
    
    private class InterfaceDelegate implements TypeDefDelegate
    {
        private final List<Modifier> modifiers;
        private final String doc;
        private String name;
        private final List<CodeElement> fields = new ArrayList<>();
        private final List<CodeElement> methods = new ArrayList<>();
        private final List<CommentElement> pendingComments = new ArrayList<>();
        private final List<String> extendsTypes = new ArrayList<>();

        public InterfaceDelegate(List<Modifier> modifiers, String doc)
        {
            this.modifiers = new ArrayList<>(modifiers);
            this.doc = doc;
        }

        @Override
        public void gotName(String name)
        {
            this.name = name;
        }

        @Override
        public void gotExtends(String type)
        {
            extendsTypes.add(type);
        }

        @Override
        public void gotImplements(String type)
        {
            
        }

        @Override
        public void gotContent(CodeElement element)
        {
            if (element instanceof VarElement)
            {
                fields.addAll(pendingComments);
                fields.add(element);
            }
            else if (element instanceof MethodProtoElement)
            {
                methods.addAll(pendingComments);
                methods.add(element);
            }
            else if (element instanceof CommentElement)
            {
                pendingComments.add((CommentElement)element);
                return;
            }
            else
            {
                warnings.add(new ConversionWarning.UnsupportedFeature(element.getClass().toString()), t -> pendingComments.add(new CommentElement(processComment(t.getText()))));
                return;
            }
            pendingComments.clear();
        }

        @Override
        public CodeElement end()
        {
            if (!methods.isEmpty())
                methods.addAll(pendingComments);
            else
                fields.addAll(pendingComments);
            pendingComments.clear();
            // Public is the default so don't warn it's unsupported:
            modifiers.removeIf(t -> t.isKeyword("public"));
            warnUnsupportedModifiers("interface", modifiers);
            return new InterfaceElement(null, null, new NameDefSlotFragment(name),
                extendsTypes.stream().map(t -> toType(t)).collect(Collectors.toList()), fields, methods,
                new JavadocUnit(doc), pkg == null ? null : pkg, importsForCU(), true);
        }
    }
    
    private class ClassDelegate implements TypeDefDelegate
    {
        private final List<Modifier> modifiers;
        private final String doc;
        private String name;
        private final List<CodeElement> fields = new ArrayList<>();
        private final List<CodeElement> constructors = new ArrayList<>();
        private final List<CodeElement> methods = new ArrayList<>();
        private final List<CommentElement> pendingComments = new ArrayList<>();
        private String extendsType;
        private final List<String> implementsTypes = new ArrayList<>();

        public ClassDelegate(List<Modifier> modifiers, String doc)
        {
            this.modifiers = new ArrayList<>(modifiers);
            this.doc = doc;
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
            {
                fields.addAll(pendingComments);
                fields.add(element);
            }
            else if (element instanceof ConstructorElement)
            {
                constructors.addAll(pendingComments);
                constructors.add(element);
            }
            else if (element instanceof NormalMethodElement || element instanceof MethodProtoElement)
            {
                methods.addAll(pendingComments);
                methods.add(element);
            }
            else if (element instanceof CommentElement)
            {
                pendingComments.add((CommentElement)element);
                return;
            }
            else
            {
                warnings.add(new ConversionWarning.UnsupportedFeature(element.getClass().toString()), t -> pendingComments.add(new CommentElement(processComment(t.getText()))));
                return;
            }
            pendingComments.clear();
        }

        @Override
        public CodeElement end()
        {
            if (!methods.isEmpty())
                methods.addAll(pendingComments);
            else if (!constructors.isEmpty())
                constructors.addAll(pendingComments);
            else
                fields.addAll(pendingComments);
            pendingComments.clear();
            boolean _abstract = modifiers.removeIf(t -> t.isKeyword("abstract"));
            // Public is the default so don't warn it's unsupported:
            modifiers.removeIf(t -> t.isKeyword("public"));
            warnUnsupportedModifiers("class", modifiers);
            return new ClassElement(null, null, _abstract, new NameDefSlotFragment(name),
                toType(extendsType),
                implementsTypes.stream().map(t -> toType(t)).collect(Collectors.toList()), fields,
                constructors, methods,
                new JavadocUnit(doc), pkg == null ? null : pkg, importsForCU(), true);
        }
    }

    /**
     * Gets the list of ImportElements found when processing this compilation unit.
     * Any import of lang.stride.* will be ignored because that is already implicit in Stride.
     * (This can happen in particular when you convert Stride to Java and back again)
     */
    private List<ImportElement> importsForCU()
    {
        return imports.stream()
            .filter(imp -> !imp.equals("lang.stride.*"))
            .map(imp -> new ImportElement(imp, null, true))
            .collect(Collectors.toList());
    }

    private void withTypeDef(Consumer<CodeElement> handler, boolean inner)
    {
        typeDefHandlers.push(new TypeDefHandler()
        {
            TypeDefDelegate delegate = null;

            @Override
            public void typeDefBegun(LocatableToken start)
            {
            }

            @Override
            public void typeDefEnd(LocatableToken end)
            {
                if (delegate != null)
                {
                    handler.accept(delegate.end());
                }
            }

            @Override
            public void startedClass(List<Modifier> modifiers, String doc)
            {
                // We discard inner class modifiers to avoid warning about them,
                // since we discard the whole inner class anyway:
                delegate = new ClassDelegate(inner ? Collections.emptyList() : modifiers, doc);
                if (!inner)
                    statementHandlers.peek().stealComments().forEach(delegate::gotContent);
            }

            @Override
            public void startedInterface(List<Modifier> modifiers, String doc)
            {
                // We discard inner class modifiers to avoid warning about them,
                // since we discard the whole inner class anyway:
                delegate = new InterfaceDelegate(inner ? Collections.emptyList() : modifiers, doc);
                if (!inner)
                    statementHandlers.peek().stealComments().forEach(delegate::gotContent);
            }

            @Override
            public void gotName(String name)
            {
                if (delegate != null)
                    delegate.gotName(name);
            }

            @Override
            public void gotContent(List<CodeElement> content)
            {
                if (delegate != null)
                    content.forEach(delegate::gotContent);
            }

            @Override
            public void typeDefImplements(String type)
            {
                if (delegate != null)
                    delegate.gotImplements(type);
            }

            @Override
            public void typeDefExtends(String type)
            {
                if (delegate != null)
                    delegate.gotExtends(type);
            }
        });
    }

    private void withStatement(StatementHandler handler)
    {
        statementHandlers.push(handler);
    }

    private class ArgumentListHandler
    {
        private final List<Expression> args = new ArrayList<>();
        private int outstanding = 0;
        private final Consumer<List<Expression>> argHandler;

        public ArgumentListHandler(Consumer<List<Expression>> argHandler)
        {
            this.argHandler = argHandler;
        }

        public void argumentListBegun()
        {
            outstanding += 1;
            if (outstanding == 1)
            {
                withExpression(args::add);
            }
        }

        public void gotArgument()
        {
            if (outstanding == 1)
            {
                withExpression(args::add);
            }
        }

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
    }


    private void withArgumentList(Consumer<List<Expression>> argHandler)
    {
        argumentHandlers.push(new ArgumentListHandler(argHandler));
    }

    private void foundStatement(CodeElement statement)
    {
        foundStatements(Collections.singletonList(statement));
    }

    private void foundStatements(List<CodeElement> statements)
    {
        statementHandlers.pop().foundStatement(statements);
    }

    @Override
    protected void finishedCU(int state)
    {
        if (state == 1)
        {
            // Finished after the import statements; need to capture those by themselves:
            result.foundStatement(Utility.mapList(imports, i -> new ImportElement(i, null, true)));
        }
    }

    /**
     * Gets the overall result of the parse
     */
    public List<CodeElement> getCodeElements()
    {
        return result.getContent(true);
    }

}
