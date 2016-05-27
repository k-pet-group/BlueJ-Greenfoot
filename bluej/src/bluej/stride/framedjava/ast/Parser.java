/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.function.Consumer;

import bluej.parser.JavaParser;
import bluej.parser.ParseFailure;
import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.IfElement;
import bluej.stride.framedjava.elements.LocatableElement;
import bluej.stride.framedjava.elements.ReturnElement;
import bluej.stride.framedjava.elements.WhileElement;

public class Parser
{

    public static boolean parseableAsType(String s)
    {
        JavaParser p = new JavaParser(new StringReader(s), false);
        try
        {
            // TODO not sure this handles multidim arrays with a size
            p.parseTypeSpec(true);
            
            // Only valid if we have parsed all the way to end of the String:
            LocatableToken tok = p.getTokenStream().nextToken();
            if (tok.getType() != JavaTokenTypes.EOF)
                return false;
            
            return true;
        }
        catch (ParseFailure pf)
        {
            return false;
        }
    }
    
    // Checks if it can be parsed as the part following "import " and before the semi-colon
    public static boolean parseableAsImportTarget(String s)
    {
        JavaParser p = new JavaParser(new StringReader("import " + s + ";"), false);
        try
        {
            p.parseImportStatement();
            
            // Only valid if we have parsed all the way to end of the String:
            
            LocatableToken tok = p.getTokenStream().nextToken();
            if (tok.getType() != JavaTokenTypes.EOF)
                return false;
            
            return true;
        }
        catch (ParseFailure pf)
        {
            return false;
        }
    }

    private static final String DUMMY_STEM = "code__dummy__gf3gen__";

    public static class DummyNameGenerator
    {
        private int index = 0;

        public String generateNewDummyName()
        {
            return DUMMY_STEM + (index++);
        }
    }
    
    public static boolean isDummyName(String name)
    {
        return name.startsWith(DUMMY_STEM);
    }

    public static boolean parseableAsNameDef(String s)
    {
        // We don't need to parse, just lex and see if it comes out as an ident token:
        JavaLexer lexer = new JavaLexer(new StringReader(s));
        LocatableToken t = lexer.nextToken();
        LocatableToken t2 = lexer.nextToken();
        if (t.getType() == JavaTokenTypes.IDENT && t2.getType() == JavaTokenTypes.EOF)
            return true;
        else
            return false;
    }

    public static boolean parseableAsExpression(String e)
    {
        return Parser.parseAsExpression(new JavaParser(new StringReader(e), false));
    }
    
    /**
     *  Tries to run the given parser by calling parseExpression.
     * Any ParseFailure exceptions are caught and false is returned.
     * If there is no exception, but after parsing we are not at EOF
     * then false is also returned.
     * true is only returned if there is no ParseFailure, and we parse
     * all the way to EOF
     */
    public static boolean parseAsExpression(JavaParser p)
    {
        try
        {
            p.parseExpression();
            
            // Only valid if we have parsed all the way to end of the String:
            
            LocatableToken tok = p.getTokenStream().nextToken();
            if (tok.getType() != JavaTokenTypes.EOF)
                return false; 
        }
        catch (ParseFailure pf)
        {
            //Debug.message("Invalid expression: " + pf.getMessage());
            return false;
        }
        return true;
        
    }

    public static List<CodeElement> javaToStride(String java) throws ParseFailure
    {
        JavaStrideParser parser = new JavaStrideParser(java);
        parser.parseStatement();
        return parser.getCodeElements();
    }

    private static class JavaStrideParser extends JavaParser
    {
        private final String source;
        private Stack<ExpressionHandler> expressionHandlers = new Stack<>();
        private Stack<StatementHandler> statementHandlers = new Stack<>();
        private List<CodeElement> result = null;

        public JavaStrideParser(String java)
        {
            super(new StringReader(java), true);
            this.source = java;
            statementHandlers.push(r -> this.result = r);
        }

        private static interface StatementHandler
        {
            public void foundStatement(List<CodeElement> statements);

            default public void endBlock() {};
        }

        private static interface ExpressionHandler
        {
            public void expressionBegun(LocatableToken start);
            public void expressionEnd(LocatableToken end);
        }

        private class IfBuilder implements StatementHandler
        {
            // Size is always >= 1, and either equal to blocks.size, or one less than blocks.size (if last one is else)
            private final ArrayList<FilledExpressionSlotFragment> conditions = new ArrayList<>();
            private final ArrayList<List<CodeElement>> blocks = new ArrayList<>();

            public IfBuilder(String condition)
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

        private FilledExpressionSlotFragment toFilled(String exp)
        {
            return new FilledExpressionSlotFragment(exp, exp);
        }

        private OptionalExpressionSlotFragment toOptional(String exp)
        {
            return new OptionalExpressionSlotFragment(exp, exp);
        }

        @Override
        protected void gotReturnStatement(boolean hasValue)
        {
            super.gotReturnStatement(hasValue);
            if (hasValue)
                withExpression(exp -> foundStatement(new ReturnElement(null, toOptional(exp), true)));
            else
                foundStatement(new ReturnElement(null, toOptional(""), true));
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
            withExpression(e -> foundStatement(new CallElement(null, new CallExpressionSlotFragment(e, e), true)));
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
            expressionHandlers.pop().expressionEnd(token);
        }

        @Override
        protected void beginStmtblockBody(LocatableToken token)
        {
            super.beginStmtblockBody(token);
            withStatement(new StatementHandler() {
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



        private String getText(LocatableToken start, LocatableToken end)
        {
            return source.substring(start.getPosition(), end.getPosition());
        }

        private void withExpression(Consumer<String> handler)
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
                public void expressionEnd(LocatableToken end)
                {
                    outstanding -= 1;
                    if (outstanding == 0)
                        handler.accept(getText(start, end));
                    else
                        // We get popped by default; add ourselves back in:
                        expressionHandlers.push(this);
                }
            });
        }

        private void withStatement(StatementHandler handler)
        {
            statementHandlers.push(handler);
        }

        private void foundStatement(CodeElement statement)
        {
            foundStatements(Collections.singletonList(statement));
        }

        private void foundStatements(List<CodeElement> statements)
        {
            statementHandlers.pop().foundStatement(statements);
        }

        public List<CodeElement> getCodeElements()
        {
            return result;
        }
    }

}
