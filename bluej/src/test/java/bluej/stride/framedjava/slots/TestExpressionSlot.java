package bluej.stride.framedjava.slots;

import static bluej.stride.framedjava.slots.Operator.Precedence.DOT;
import static bluej.stride.framedjava.slots.Operator.Precedence.HIGH;
import static bluej.stride.framedjava.slots.Operator.Precedence.LOW;
import static bluej.stride.framedjava.slots.Operator.Precedence.MEDIUM;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import bluej.stride.framedjava.slots.Operator.Precedence;

public class TestExpressionSlot
{
    // Need to run tests on FX thread:
    @Rule
    public TestRule runOnFXThreadRule = new TestRule() {
        boolean initialised = false;
        @Override public Statement apply(Statement base, Description d) {
            if (!initialised)
            {   
                // Initialise JavaFX:
                new JFXPanel();
                initialised = true;
            }
            return new Statement() {
                @Override public void evaluate() throws Throwable {
                    // Run on FX thread, rethrow any exceptions back on this thread:
                    CompletableFuture<Throwable> thrown = new CompletableFuture<>();
                    Platform.runLater(() -> {
                      try {
                        base.evaluate();
                        thrown.complete(null);
                      } catch( Throwable throwable ) {
                        thrown.complete(throwable);
                      }
                    });
                    Throwable t = thrown.get();
                    if (t != null)
                        throw t;
                }
            };
        }
        
    };
    
    // Class to suppress JUnit's clever string-difference display, which gets very confusing for this
    // sort of test:
    private static class CompareWrapper
    {
        private String s;
        public CompareWrapper(String s) { this.s = s; }
        @Override public boolean equals(Object o) { if (o instanceof CompareWrapper) return s.equals(((CompareWrapper)o).s); else return false; }
        @Override public String toString() { return s; }
    }

    private void testInsert(String insertion, String result)
    {
        InfixExpression e = StructuredSlot.testingModification(token -> new InfixExpression(null, null, token));
        CaretPos p = e.testingInsert(insertion, '\0');
        assertEquals(insertion + " -> " + result, new CompareWrapper(result), new CompareWrapper(e.testingGetState(p)));
        
        // Test string pos <-> caret pos round trip:
        for (int i = 0; i <= insertion.length(); i++)
        {
            CaretPos caretPos = e.stringPosToCaretPos(i, false);
            if (caretPos != null)
                assertEquals("String pos -> caret pos -> string pos", i, e.caretPosToStringPos(caretPos, false));
        }
        
        // Test that no matter where we split, we will also get the same result:
        String noPos = result.replace("$", "");
        for (int split = 1; split < insertion.length(); split++)
        {
            e = StructuredSlot.testingModification(token -> new InfixExpression(null, null, token));
            e.testingInsert(e.testingInsert(insertion.substring(0, split), '\0'),  insertion.substring(split));
            assertEquals(insertion + " -> " + noPos, new CompareWrapper(noPos), new CompareWrapper(e.testingGetState(null)));
        }
        
        
    }
    
    private void testMultiInsert(String multiInsertion, String firstResult, String secondResult)
    {
        InfixExpression e = StructuredSlot.testingModification(token -> new InfixExpression(null, null, token));
        int startNest = multiInsertion.indexOf('{');
        int endNest = multiInsertion.indexOf('}', startNest);
        if (startNest == -1 || endNest == -1)
            throw new IllegalStateException();
        String before = multiInsertion.substring(0, startNest);
        String nest = multiInsertion.substring(startNest + 1, endNest);
        String after = multiInsertion.substring(endNest + 1);
        
        CaretPos p = e.testingInsert(before + "$" + after, '$');
        assertEquals(multiInsertion + " -> " + firstResult, new CompareWrapper(firstResult), new CompareWrapper(e.testingGetState(p)));
        p = e.testingInsert(p,  nest);
        assertEquals(multiInsertion + " -> " + secondResult, new CompareWrapper(secondResult), new CompareWrapper(e.testingGetState(p)));
    }
    
    private void testInsertExisting(String start, String insertion, String result)
    {
        InfixExpression e = StructuredSlot.testingModification(token -> new InfixExpression(null, null, token));
        CaretPos p = e.testingInsert(start, '$');
        p = e.testingInsert(p, insertion);
        
        assertEquals(start + " then \"" + insertion + "\" -> " + result, new CompareWrapper(result), new CompareWrapper(e.testingGetState(p)));
    }
    
    
    private void testBackspace(String insertionInclBackspace, String result)
    {
        testBackspace(insertionInclBackspace, result, true, true);
    }
    
    private void testBackspace(String insertionInclBackspace, String result, boolean testBackspace, boolean testDelete)
    {
        if (testBackspace)
        {
            InfixExpression e = StructuredSlot.testingModification(token -> new InfixExpression(null, null, token));
            CaretPos p = e.testingInsert(insertionInclBackspace, '\b');
            e.positionCaret(p);
            
            p = e.testingBackspace(p);
            assertEquals(insertionInclBackspace.replace("\b", "\\b") + " -> " + result, new CompareWrapper(result), new CompareWrapper(e.testingGetState(p)));
        }
        
        // Now test the same thing using delete:
        int index = insertionInclBackspace.indexOf('\b');
        if (index > 0 && testDelete)
        {
            String before = insertionInclBackspace.substring(0, index);
            String after = insertionInclBackspace.substring(index + 1);
            
            // Move last character from before to after:
            after = before.substring(before.length() - 1) + after;
            before = before.substring(0, before.length() - 1);
            
            // We still use \b because we know it won't occur again in the string:
            String joined = before + "\b" + after;
            InfixExpression e = StructuredSlot.testingModification(token -> new InfixExpression(null, null, token));
            CaretPos p = e.testingInsert(joined, '\b');
            e.positionCaret(p);
            p = e.testingDelete(p);
            assertEquals(joined.replace("\b", "\\DEL") + " -> " + result, new CompareWrapper(result), new CompareWrapper(e.testingGetState(p)));
        }
    }
    
    private void testDeleteSelection(String src, String result)
    {
        InfixExpression e = StructuredSlot.testingModification(token -> new InfixExpression(null, null, token));
        int startNest = src.indexOf('{');
        int endNest = src.indexOf('}', startNest);
        if (startNest == -1 || endNest == -1)
            throw new IllegalStateException();
        String before = src.substring(0, startNest);
        String nest = src.substring(startNest + 1, endNest);
        String after = src.substring(endNest + 1);
        
        CaretPos start = e.testingInsert(before, '\0');
        CaretPos end = e.testingInsert(start, nest);
        e.testingInsert(end, after);
        CaretPos p = e.testingDeleteSelection(start, end);
        
        assertEquals(src + " -> " + result, new CompareWrapper(result), new CompareWrapper(e.testingGetState(p)));
    }
    
    private void testSelectionInsert(char c, String src, String result)
    {
        InfixExpression e = StructuredSlot.testingModification(token -> new InfixExpression(null, null, token));
        int startNest = src.indexOf('{');
        int endNest = src.indexOf('}', startNest);
        if (startNest == -1 || endNest == -1)
            throw new IllegalStateException();
        String before = src.substring(0, startNest);
        String nest = src.substring(startNest + 1, endNest);
        String after = src.substring(endNest + 1);
        
        CaretPos start = e.testingInsert(before, '\0');
        CaretPos end = e.testingInsert(start, nest);
        e.testingInsert(end, after);
        CaretPos p = e.testingInsertWithSelection(start, end, c);
        
        assertEquals(src + " -> " + result, new CompareWrapper(result), new CompareWrapper(e.testingGetState(p)));
    }
    
    private static class CPM
    {
        private int stringPos;
        private CaretPos caretPos;
        public CPM(int stringPos, CaretPos caretPos)
        {
            this.stringPos = stringPos;
            this.caretPos = caretPos;
        }
        @Override
        public String toString()
        {
            return "CPM [stringPos=" + stringPos + ", caretPos=" + caretPos
                    + "]";
        }
    }
    
    private void testCaretPosMap(String src, String result, CPM... cpms)
    {
        testCaretPosMap(src, result, null, cpms);
    }
    
    private void testCaretPosMap(String src, String result, String java, CPM... cpms)
    {
        InfixExpression e = StructuredSlot.testingModification(token -> new InfixExpression(null, null, token));
        CaretPos p = e.testingInsert(src, '\0');
        assertEquals(src + " -> " + result, new CompareWrapper(result), new CompareWrapper(e.testingGetState(p).replace("$", "")));
        for (CPM cpm : cpms)
        {
            assertEquals("Caret to String " + cpm.toString() + " for " + src, cpm.stringPos, e.caretPosToStringPos(cpm.caretPos, java != null));
            assertEquals("String to Caret " + cpm.toString() + " for " + src, cpm.caretPos, e.stringPosToCaretPos(cpm.stringPos, java != null));
        }
        
        if (java != null)
        {
            assertEquals("To Java, from: " + src, java, e.getJavaCode());
        }
    }
    
    // In these tests, curly brackets indicate slots
    // Operators are between each slot, written literally
    // Null operators are written as a single underscore (outside a slot)
    // Strings are enclosed in quotes (no curly brackets)
    // The caret after insertion is indicated by a dollar sign
    
    @Test
    public void testOperators()
    {
        testInsert("aa", "{aa$}");
        testInsert("a+b", "{a}+{b$}");
        testInsert("a+b-c", "{a}+{b}-{c$}");
        testInsert("1++1", "{1}+{+1$}");
        testInsert("1<2&&3<=4&&5==6+8", "{1}<{2}&&{3}<={4}&&{5}=={6}+{8$}");
        testInsert("false!=!false", "{false}!={}!{false$}");
        testInsert("false!=!!!false", "{false}!={}!{}!{}!{false$}");
        testInsert("5==-6", "{5}=={-6$}");
        testInsert("5==--6", "{5}=={}-{-6$}");
        testInsert("5==----6", "{5}=={}-{}-{}-{-6$}");
        testInsert("a.b", "{a}.{b$}");
        testInsert("a..b", "{a}..{b$}");
        testInsert("y-1", "{y}-{1$}");
        testInsert("getY()*1", "{getY}_({})_{}*{1$}");
        testInsert("getY()-1", "{getY}_({})_{}-{1$}");
        testInsert("getY()+-1", "{getY}_({})_{}+{-1$}");

        // Bug found in preview2:
        testInsert("s.length()..10", "{s}.{length}_({})_{}..{10$}");
        // Partials of above:
        testInsert("s.length()", "{s}.{length}_({})_{$}");
        testInsert("s.length().", "{s}.{length}_({})_{}.{$}");
        testInsert("s.length()..", "{s}.{length}_({})_{}..{$}");
        testInsert("s.length()..1", "{s}.{length}_({})_{}..{1$}");

    }
    
    @Test
    public void testNew()
    {
        testInsert("newton", "{newton$}");
        testInsert("new ton", "{}new {ton$}");
    }
    
    @Test
    public void testStrings()
    {
        // With trailing quote
        testInsert("\"hello\"", "{}_\"hello\"_{$}");
        // Without trailing quote (caret stays in string):
        testInsert("\"hello", "{}_\"hello$\"_{}");
        testInsert("\"hello\"+\"world\"", "{}_\"hello\"_{}+{}_\"world\"_{$}");
        testInsert("\"hello\"+\"world\"+(5*6)", "{}_\"hello\"_{}+{}_\"world\"_{}+{}_({5}*{6})_{$}");
        
        // Quote in a string, escaped:
        testInsert("\"\\\"\"", "{}_\"\\\"\"_{$}");
        // Escaped single quote:
        testInsert("\"\\\'\"", "{}_\"\\\'\"_{$}");
        // Unescaped single quote:
        testInsert("\"'\"", "{}_\"'\"_{$}");
        
        // Adding quote later:
        testMultiInsert("abc{\"}def", "{abc$def}", "{abc}_\"$\"_{def}");
        testMultiInsert("abc{\"}", "{abc$}", "{abc}_\"$\"_{}");
        testMultiInsert("{\"}def", "{$def}", "{}_\"$\"_{def}");
        testMultiInsert("abc{\"}.def", "{abc$}.{def}", "{abc}_\"$\"_{}.{def}");
        testMultiInsert("abc{\"}*def", "{abc$}*{def}", "{abc}_\"$\"_{}*{def}");
        testMultiInsert("abc{\"}def()", "{abc$def}_({})_{}", "{abc}_\"$\"_{def}_({})_{}");
        testMultiInsert("abc{\"}()", "{abc$}_({})_{}", "{abc}_\"$\"_{}_({})_{}");

        // Adding string adjacent to String:
        // First, before:
        testInsertExisting("$\"b\"", "\"a", "{}_\"a$\"_{}_\"b\"_{}");
        testInsertExisting("$\"b\"", "\"a\"", "{}_\"a\"_{$}_\"b\"_{}");
        // Also, after:
        testInsertExisting("\"a\"$", "\"b", "{}_\"a\"_{}_\"b$\"_{}");

        // Test character literal:
        testInsert("'a'", "{}_'a'_{$}");
        testInsert("'a", "{}_'a$'_{}");
        // Escaped single quote in character literal:
        testInsert("'\\''", "{}_'\\''_{$}");
        // Escaped double quote in character literal:
        testInsert("'\\\"'", "{}_'\\\"'_{$}");
        // Unescaped double quote in character literal:
        testInsert("'\"'", "{}_'\"'_{$}");
        
        
        // Example found while pasting from BlueJ (double escaped here)
        testInsert("c == '\\\\' || c == '\"' || c == '\\''",
            "{c}=={}_\'\\\\\'_{}||{c}=={}_'\"'_{}||{c}=={}_'\\''_{$}");

        // Deletion:
        testBackspace("\"a\bb\"", "{}_\"$b\"_{}");
        testBackspace("\"\bab\"", "{$ab}");
        testBackspace("\"ab\b\"", "{}_\"a$\"_{}");
        testBackspace("\"ab\"\b", "{ab$}");
    }
    
    @Test
    public void testBrackets()
    {
        testInsert("a+(b-c)", "{a}+{}_({b}-{c})_{$}");
        testInsert("a+(b-(c*d))", "{a}+{}_({b}-{}_({c}*{d})_{})_{$}");
        
        // Without close:
        testInsert("(a+b", "{}_({a}+{b$})_{}");
        
        testInsert("(((", "{}_({}_({}_({$})_{})_{})_{}");
        testInsert("((()", "{}_({}_({}_({})_{$})_{})_{}");
        testInsert("((())", "{}_({}_({}_({})_{})_{$})_{}");
        testInsert("((()))", "{}_({}_({}_({})_{})_{})_{$}");
        
        testInsert("(a+(b*c)+d)", "{}_({a}+{}_({b}*{c})_{}+{d})_{$}");
        
        testMultiInsert("({(MyWorld)}getWorld()).getWidth()",
                "{}_({$getWorld}_({})_{})_{}.{getWidth}_({})_{}",
                "{}_({}_({MyWorld})_{$getWorld}_({})_{})_{}.{getWidth}_({})_{}");
        
        testInsert("a(bc)d", "{a}_({bc})_{d$}");
    }
    
    @Test
    public void testBackspace()
    {
        testBackspace("\bxyz", "{$xyz}");
        testBackspace("x\byz", "{$yz}");
        testBackspace("xy\bz", "{x$z}");
        testBackspace("xyz\b", "{xy$}");
        
        testBackspace("xy\b+ab", "{x$}+{ab}");
        testBackspace("xy+\bab", "{xy$ab}");
        testBackspace("xy+a\bb", "{xy}+{$b}");
        
        testBackspace("new t\bon", "{}new {$on}");
        // This one isn't possible using delete, but is by using backspace:
        testBackspace("new \bton", "{new$ton}", true, false);
        // This one isn't possible using backspace, but is by using delete:
        testBackspace("n\bew ton", "{$ewton}", false, true);
        
        testBackspace("move(\b)", "{move$}");
        testBackspace("(\b)", "{$}");
    }
    
    @Test
    public void testFloating()
    {
        // For some of this, they are a syntax error anyway, so it is not crucial which way we split them
        // Still, a regression might indicate a problem
        
        testInsert("1.0", "{1.0$}");
        testInsert("10.20", "{10.20$}");
        testInsert("a.0", "{a}.{0$}");
        testInsert("1.a", "{1.a$}");
        testInsert("x1.a", "{x1}.{a$}");
        testInsert("+1", "{+1$}");
        testInsert("+1.0", "{+1.0$}");
        testInsert("+1.0e5", "{+1.0e5$}");
        testInsert("+1.0e", "{+1.0e$}");
        testInsert("+1.0e+5", "{+1.0e+5$}");
        testInsert("+1.0e+5+6", "{+1.0e+5}+{6$}");
        testInsert("+1.0p+5", "{+1.0p}+{5$}");
        testInsert("3+1", "{3}+{1$}");
        testInsert("3+1.0", "{3}+{1.0$}");
        testInsert("3+1.0e5", "{3}+{1.0e5$}");
        testInsert("3+1.0e+5", "{3}+{1.0e+5$}");
        testInsert("3+1.0e+5+6", "{3}+{1.0e+5}+{6$}");
        testInsert("3+1.0p+5", "{3}+{1.0p}+{5$}");
        
        testInsert("+1+2+3", "{+1}+{2}+{3$}");
        testInsert("+1++2", "{+1}+{+2$}");
        testInsert("+1++2+3", "{+1}+{+2}+{3$}");
        testInsert("+1++2++3", "{+1}+{+2}+{+3$}");
        testInsert("++1++2++3", "{}+{+1}+{+2}+{+3$}");
        testMultiInsert("+{1}", "{}+{$}", "{+1$}");
        testMultiInsert("+{+1}", "{}+{$}", "{}+{+1$}");
        testMultiInsert("1++{2}", "{1}+{}+{$}", "{1}+{+2$}");
        
        testInsert("1e6", "{1e6$}");
        testInsert("1e-6", "{1e-6$}");
        testInsert("10e20", "{10e20$}");
        testInsert("10e+20", "{10e+20$}");
        testInsert("10e-20", "{10e-20$}");
        
        testInsert("1.0.3", "{1.0}.{3$}");
        testInsert("1.0.3.4", "{1.0}.{3.4$}");
        testInsert("1.0.x3.4", "{1.0}.{x3}.{4$}");
        
        // The problem here is that + is first an operator, then merged back in,
        // so when we preserve the position after plus, it becomes invalid, so we
        // can't easily test that the right thing happens deleting the plus:
        testBackspace("+\b1.0e-5", "{$1.0e-5}", false, true);
        testBackspace("+1\b.0e-5", "{}+{$}.{0e-5}", true, false);
        testBackspace("+1.\b0e-5", "{+1$0e-5}");
        testBackspace("+1.0\be-5", "{+1.$e-5}");
        testBackspace("+1.0e\b-5", "{+1.0$}-{5}");
        testBackspace("+1.0e-\b5", "{+1.0e$5}");
        testBackspace("+1.0e-5\b", "{+1.0e-$}");
        
        testMultiInsert("{1}e-6", "{$e}-{6}", "{1$e-6}");
        testMultiInsert("1{e}-6", "{1$}-{6}", "{1e$-6}");
        testMultiInsert("1e{-}6", "{1e$6}", "{1e-$6}");
        testMultiInsert("1e-{6}", "{1e-$}", "{1e-6$}");
        
        testMultiInsert("{x}1e-6", "{$1e-6}", "{x$1e}-{6}");
        testMultiInsert("1{x}e-6", "{1$e-6}", "{1x$e}-{6}");
        testMultiInsert("1e{x}-6", "{1e$-6}", "{1ex$}-{6}");
        testMultiInsert("1e-{x}6", "{1e-$6}", "{1e-x$6}");
        
        testInsert("1.0", "{1.0$}");
        testInsert("1..0", "{1}..{0$}");
        testBackspace("1..\b0", "{1.$0}", true, false); // backspace after
        testBackspace("1.\b.0", "{1$.0}", false, true); // delete before
        testBackspace("a..\bc", "{a}.{$c}", true, false); // backspace after
        testBackspace("a.\b.c", "{a$}.{c}", false, true); // delete before
    }
    
    @Test
    public void testDeleteBracket()
    {
        testInsert("a+(b*c)", "{a}+{}_({b}*{c})_{$}");
        testBackspace("a+(b*c)\b", "{a}+{b}*{c$}");
        testBackspace("a+(\bb*c)", "{a}+{$b}*{c}");
        
        testInsert("((MyWorld)getWorld()).getWidth()",
                "{}_({}_({MyWorld})_{getWorld}_({})_{})_{}.{getWidth}_({})_{$}");
        testBackspace("((MyWorld)getWorld()).getWidth()\b",
                "{}_({}_({MyWorld})_{getWorld}_({})_{})_{}.{getWidth$}");
        testBackspace("((MyWorld)getWorld()).\bgetWidth()",
                "{}_({}_({MyWorld})_{getWorld}_({})_{})_{$getWidth}_({})_{}");
        testBackspace("((MyWorld)getWorld())\b.getWidth()",
                "{}_({MyWorld})_{getWorld}_({})_{$}.{getWidth}_({})_{}");
        testBackspace("((MyWorld)getWorld(\b)).getWidth()",
                "{}_({}_({MyWorld})_{getWorld$})_{}.{getWidth}_({})_{}");
        testBackspace("((MyWorld)\bgetWorld()).getWidth()",
                "{}_({MyWorld$getWorld}_({})_{})_{}.{getWidth}_({})_{}");
        testBackspace("((\bMyWorld)getWorld()).getWidth()",
                "{}_({$MyWorldgetWorld}_({})_{})_{}.{getWidth}_({})_{}");
        testBackspace("(\b(MyWorld)getWorld()).getWidth()",
                "{$}_({MyWorld})_{getWorld}_({})_{}.{getWidth}_({})_{}");
    }
    
    @Test
    public void testDeleteSelection()
    {
        testDeleteSelection("a{bc}d", "{a$d}");
        testDeleteSelection("a{bc}", "{a$}");
        
        testInsert("a+(b*c)-d", "{a}+{}_({b}*{c})_{}-{d$}");
        testDeleteSelection("{a}+(b*c)-d", "{$}+{}_({b}*{c})_{}-{d}");
        testDeleteSelection("{a+}(b*c)-d", "{$}_({b}*{c})_{}-{d}");
        testDeleteSelection("a+{(b*c)}-d", "{a}+{$}-{d}");
        testDeleteSelection("a{+(b*c)}-d", "{a$}-{d}");
        testDeleteSelection("a{+(b*c)-}d", "{a$d}");
        testDeleteSelection("a+({b*c})-d", "{a}+{}_({$})_{}-{d}");
        
        testInsert("s+\"hello\"+t", "{s}+{}_\"hello\"_{}+{t$}");
        testDeleteSelection("s+\"h{ell}o\"+t", "{s}+{}_\"h$o\"_{}+{t}");
    }
    
    @Test
    public void testSelectionOperation()
    {
        testSelectionInsert('(', "a{bc}d", "{a}_({bc})_{$d}");
        testSelectionInsert('(', "a{b+c}d", "{a}_({b}+{c})_{$d}");
        testSelectionInsert('(', "a{b+}d", "{a}_({b}+{})_{$d}");
        testSelectionInsert('(', "a{b+}", "{a}_({b}+{})_{$}");
        testSelectionInsert('(', "a{b++}", "{a}_({b}+{}+{})_{$}");
        testSelectionInsert('(', "a{b+}+", "{a}_({b}+{})_{$}+{}");
        
        testSelectionInsert('\"', "a{bc}d", "{a}_\"bc\"_{$d}");
        testSelectionInsert('\'', "a{bc}d", "{a}_\'bc\'_{$d}");
        testInsert("ab+cd", "{ab}+{cd$}");
        testSelectionInsert('\"', "a{b+c}d", "{a}_\"b+c\"_{$d}");
        testSelectionInsert('\'', "a{b+c}d", "{a}_\'b+c\'_{$d}");
        
        testInsert("a+\"hello\"+c", "{a}+{}_\"hello\"_{}+{c$}");
        testSelectionInsert('\"', "a+\"h{ell}o\"+c", "{a}+{}_\"hell$o\"_{}+{c}");
        testSelectionInsert('\"', "a+\"hello\"{+c}", "{a}+{}_\"hello\"_{}_\"+c\"_{$}");
    }
    
    private CaretPos makeCaretPos(int... xs)
    {
        CaretPos p = null;
        for (int i = xs.length - 1; i >= 0; i--)
        {
            p = new CaretPos(xs[i], p);
        }
        return p;
    }

    @Test
    public void testCaretPosMap()
    {
        testCaretPosMap("abc", "{abc}",
                new CPM(0, makeCaretPos(0, 0)),
                new CPM(1, makeCaretPos(0, 1)),
                new CPM(2, makeCaretPos(0, 2))
        );
        
        testCaretPosMap("a+b+c", "{a}+{b}+{c}",
            new CPM(0, makeCaretPos(0, 0)),
            new CPM(1, makeCaretPos(0, 1)),
            new CPM(2, makeCaretPos(1, 0)),
            new CPM(3, makeCaretPos(1, 1)),
            new CPM(4, makeCaretPos(2, 0)),
            new CPM(5, makeCaretPos(2, 1))
        );
        testCaretPosMap("\"hi\"", "{}_\"hi\"_{}",
            new CPM(0, makeCaretPos(0, 0)),
            new CPM(1, makeCaretPos(1, 0)),
            new CPM(2, makeCaretPos(1, 1)),
            new CPM(3, makeCaretPos(1, 2)),
            new CPM(4, makeCaretPos(2, 0))
        );
        testCaretPosMap("a*(b+\"cd\"+e)-(f)", "{a}*{}_({b}+{}_\"cd\"_{}+{e})_{}-{}_({f})_{}",
            new CPM(0, makeCaretPos(0, 0)), // before a
            new CPM(1, makeCaretPos(0, 1)), // before *
            new CPM(2, makeCaretPos(1, 0)), // before (
            new CPM(3, makeCaretPos(2, 0, 0)), // before b
            new CPM(4, makeCaretPos(2, 0, 1)), // before +
            new CPM(5, makeCaretPos(2, 1, 0)), // before "
            new CPM(6, makeCaretPos(2, 2, 0)), // before c
            new CPM(7, makeCaretPos(2, 2, 1)), // before d
            new CPM(8, makeCaretPos(2, 2, 2)), // before "
            new CPM(9, makeCaretPos(2, 3, 0)), // before +
            new CPM(10, makeCaretPos(2, 4, 0)), // before e
            new CPM(11, makeCaretPos(2, 4, 1)), // before )
            new CPM(12, makeCaretPos(3, 0)), // before -
            new CPM(13, makeCaretPos(4, 0)), // before (
            new CPM(14, makeCaretPos(5, 0, 0)), // before f
            new CPM(15, makeCaretPos(5, 0, 1)), // before )
            new CPM(16, makeCaretPos(6, 0)) // before end
        );
        
        testCaretPosMap("gW().aO(a,b,c)", "{gW}_({})_{}.{aO}_({a},{b},{c})_{}",
                new CPM(0, makeCaretPos(0, 0)), // before g
                new CPM(1, makeCaretPos(0, 1)), // before W
                new CPM(2, makeCaretPos(0, 2)), // before (
                
                new CPM(3, makeCaretPos(1, 0, 0)), // before )
                
                new CPM(4, makeCaretPos(2, 0)), // before .
                
                new CPM(5, makeCaretPos(3, 0)), // before a
                new CPM(6, makeCaretPos(3, 1)), // before 0
                new CPM(7, makeCaretPos(3, 2)), // before (
                
                new CPM(8, makeCaretPos(4, 0, 0)), // before a
                new CPM(9, makeCaretPos(4, 0, 1)), // before ,
                
                new CPM(10, makeCaretPos(4, 1, 0)), // before b
                new CPM(11, makeCaretPos(4, 1, 1)), // before ,
                
                new CPM(12, makeCaretPos(4, 2, 0)), // before c
                new CPM(13, makeCaretPos(4, 2, 1)), // before )
                new CPM(14, makeCaretPos(5, 0)) // before end
        );
        
        testCaretPosMap("1+2", "{1}+{2}", "1 + 2",
                new CPM(0, makeCaretPos(0, 0)),
                new CPM(1, makeCaretPos(0, 1)),
                new CPM(4, makeCaretPos(1, 0)),
                new CPM(5, makeCaretPos(1, 1))
        );
        
        testCaretPosMap("1++2", "{1}+{+2}", "1 + +2",
                new CPM(0, makeCaretPos(0, 0)),
                new CPM(1, makeCaretPos(0, 1)),
                new CPM(4, makeCaretPos(1, 0)),
                new CPM(5, makeCaretPos(1, 1)),
                new CPM(6, makeCaretPos(1, 2))
        );
                
        // Stride:
        testCaretPosMap("a<:Crab", "{a}<:{Crab}",
                new CPM(0, makeCaretPos(0, 0)),
                new CPM(1, makeCaretPos(0, 1)),
                new CPM(3, makeCaretPos(1, 0)),
                new CPM(4, makeCaretPos(1, 1)),
                new CPM(5, makeCaretPos(1, 2)),
                new CPM(6, makeCaretPos(1, 3)),
                new CPM(7, makeCaretPos(1, 4))
        );
        
        // Java:
        testCaretPosMap("a<:Crab", "{a}<:{Crab}", "a instanceof Crab",
                new CPM(0, makeCaretPos(0, 0)),
                new CPM(1, makeCaretPos(0, 1)),
                new CPM(13, makeCaretPos(1, 0)),
                new CPM(14, makeCaretPos(1, 1)),
                new CPM(15, makeCaretPos(1, 2)),
                new CPM(16, makeCaretPos(1, 3)),
                new CPM(17, makeCaretPos(1, 4))
        );
        
        final int f = "lang.stride.Utility.makeRange".length();
        testCaretPosMap("1..2", "{1}..{2}", "lang.stride.Utility.makeRange(1, 2)",
                new CPM(f + 1, makeCaretPos(0, 0)),
                new CPM(f + 2, makeCaretPos(0, 1)),
                new CPM(f + 4, makeCaretPos(1, 0)),
                new CPM(f + 5, makeCaretPos(1, 1))
        );

        // Last part is semantically wrong, but should be syntactically allowed:
        testCaretPosMap("1,2..3+4,5,6..7..8", "{1},{2}..{3}+{4},{5},{6}..{7}..{8}",
                "1, lang.stride.Utility.makeRange(2, 3 + 4), 5, lang.stride.Utility.makeRange(6, lang.stride.Utility.makeRange(7, 8))",
                new CPM(0, makeCaretPos(0, 0)), // 1
                new CPM(1, makeCaretPos(0, 1)),
                
                new CPM(3 + f + 1, makeCaretPos(1, 0)), // 2
                new CPM(3 + f + 2, makeCaretPos(1, 1)),
                new CPM(3 + f + 4, makeCaretPos(2, 0)), // 3
                new CPM(3 + f + 5, makeCaretPos(2, 1)),
                new CPM(3 + f + 8, makeCaretPos(3, 0)), // 4
                new CPM(3 + f + 9, makeCaretPos(3, 1)),
                
                new CPM(3 + f + 12, makeCaretPos(4, 0)), // 5
                new CPM(3 + f + 13, makeCaretPos(4, 1)),
                
                new CPM(3 + f + 15 + f + 1, makeCaretPos(5, 0)), // 6
                new CPM(3 + f + 15 + f + 2, makeCaretPos(5, 1)),
                
                new CPM(3 + f + 15 + f + 4 + f + 1, makeCaretPos(6, 0)), // 7
                new CPM(3 + f + 15 + f + 4 + f + 2, makeCaretPos(6, 1)),
                new CPM(3 + f + 15 + f + 4 + f + 4, makeCaretPos(7, 0)), // 8
                new CPM(3 + f + 15 + f + 4 + f + 5, makeCaretPos(7, 1))
        );

        // Stride:
        testCaretPosMap("600+\"a\",40", "{600}+{}_\"a\"_{},{40}",
            new CPM(0, makeCaretPos(0,0)), // 600
            new CPM(1, makeCaretPos(0,1)),
            new CPM(2, makeCaretPos(0,2)),

            new CPM(4, makeCaretPos(1,0)), // before closing quote

            new CPM(5, makeCaretPos(2,0)), // before a
            new CPM(6, makeCaretPos(2,1)), // after a

            new CPM(7, makeCaretPos(3,0)), // after closing quote

            new CPM(8, makeCaretPos(4,0)), // 40
            new CPM(9, makeCaretPos(4,1))
        );

        // Java:
        testCaretPosMap("600+\"a\",40", "{600}+{}_\"a\"_{},{40}", "600 + \"a\", 40",
            new CPM(0, makeCaretPos(0,0)), // 600
            new CPM(1, makeCaretPos(0,1)),
            new CPM(2, makeCaretPos(0,2)),

            new CPM(6, makeCaretPos(1,0)), // before closing quote

            new CPM(7, makeCaretPos(2,0)), // before a
            new CPM(8, makeCaretPos(2,1)), // after a

            new CPM(9, makeCaretPos(3,0)), // after closing quote

            new CPM(11, makeCaretPos(4,0)), // 40
            new CPM(12, makeCaretPos(4,1))
        );
    }
    
    private void testPrecedence(String src, Precedence... precedences)
    {
        // Ignore operands
        src = src.replaceAll(" +", "").replaceAll("[a-zA-Z0-9]+", "x");
        
        ArrayList<Boolean> unary = new ArrayList<>();
        ArrayList<Operator> ops = new ArrayList<>();
        
        // Otherwise, operator:
        boolean lastWasOperand = false;
        
        while (src.length() > 0)
        {
            if (src.startsWith("x"))
            {
                lastWasOperand = true;
                src = src.substring(1);
            }
            // Try longer operators first:
            else if (InfixExpression.isExpressionOperator(src.substring(0, 2)))
            {
                ops.add(new Operator(src.substring(0, 2), null));
                src = src.substring(2);
                unary.add(!lastWasOperand);
                lastWasOperand = false;
            }
            else if (InfixExpression.isExpressionOperator(src.substring(0, 1)))
            {
                ops.add(new Operator(src.substring(0, 1), null));
                src = src.substring(1);
                unary.add(!lastWasOperand);
                lastWasOperand = false;
            }
            else if (src.startsWith("()"))
            {
                src = src.substring(2);
                ops.add(null);
                ops.add(null);
                unary.add(false);
                unary.add(false);
                lastWasOperand = true;
            }
            else
            {
                throw new IllegalArgumentException("Unknown operator: " + src);
            }
        }
        
        if (ops.size() != precedences.length)
            throw new IllegalArgumentException("Incorrect number of precedences: " + ops.size() + " vs " + precedences.length);
        
        InfixExpression.calculatePrecedences(ops, unary);
        
        for (int i = 0; i < ops.size(); i++)
        {
            assertEquals("Operator precedence (" + (ops.get(i) == null ? "_" : ops.get(i).get()) + "), index" + i, precedences[i], ops.get(i) == null ? null : ops.get(i).getPrecedence());
        }
    }
    
    @Test
    public void testPrecedence()
    {
        testPrecedence("1+2", HIGH);
        testPrecedence("1+2-3", HIGH, HIGH);
        testPrecedence("1+2*3", MEDIUM, HIGH);
        testPrecedence("1+2*3-4", MEDIUM, HIGH, MEDIUM);
        testPrecedence("1*2+3*4", HIGH, MEDIUM, HIGH);
        
        testPrecedence("1++2", MEDIUM, HIGH);
        testPrecedence("3*-4", MEDIUM, HIGH);
        testPrecedence("-3+4", HIGH, MEDIUM);
        testPrecedence("1 < 2 && 3 <= 4 && 5 == 6", HIGH, MEDIUM, HIGH, MEDIUM, HIGH);
        // Or should the early HIGHs be MEDIUM?
        testPrecedence("1 < 2 && 3 <= 4 && 5 == 6 + 8", HIGH, LOW, HIGH, LOW, MEDIUM, HIGH);
        
        testPrecedence("x+6*4", MEDIUM, HIGH);
        testPrecedence("getX()+6", null, null, HIGH);
        testPrecedence("getX()+6*4", null, null, MEDIUM, HIGH);
        
        testPrecedence("a+b+c", HIGH, HIGH);
        testPrecedence("a.b.c", DOT, DOT);
        testPrecedence("a.b+c", DOT, HIGH);
        testPrecedence("a+b.c", HIGH, DOT);
        testPrecedence("a+b.c*d.e-f", MEDIUM, DOT, HIGH, DOT, MEDIUM);
    }
    
    @Test
    public void testDot()
    {
        testInsert(".", "{}.{$}");
        testInsert("0.", "{0.$}");
        testInsert("a.", "{a}.{$}");
        testInsert("foo()", "{foo}_({})_{$}");
        testInsert("foo().bar()", "{foo}_({})_{}.{bar}_({})_{$}");
        //TODO: see tasks.txt
        //testInsert("foo().", "{foo}_({})_{}.{$}_({})_{}");
        testInsert("foo+().", "{foo}+{}_({})_{}.{$}");
        
        testMultiInsert("foo(){.}a", "{foo}_({})_{$a}", "{foo}_({})_{}.{$a}");
        
        testInsert("foo()0.", "{foo}_({})_{0.$}");
        testBackspace("foo()0\b.", "{foo}_({})_{$}.{}");
    }

    @Test
    public void testOvertype()
    {
        // Opening bracket just before one does overtype:
        testInsertExisting("$()", "move", "{move$}_({})_{}");
        testInsertExisting("move$()", "(",  "{move}_({$})_{}");

        // Most operators, like +, don't overtype:
        testInsertExisting("a$+z", "+", "{a}+{$}+{z}");
        testInsertExisting("a$+z", "+b", "{a}+{b$}+{z}");

        // Commas overtype existing commas, if the slot afterwards is blank
        testInsertExisting("a$,", ",", "{a},{$}");
        testInsertExisting("a$,b", ",", "{a},{$},{b}");
    }
    
    @Test
    public void testSemicolon()
    {
        testInsert(";", "{$}");
        testInsert("foo();", "{foo}_({})_{$}");
        testInsert("\";", "{}_\";$\"_{}");
    }
}
