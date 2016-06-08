package bluej.stride.framedjava.slots;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import bluej.stride.framedjava.slots.Operator.Precedence;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import static bluej.stride.framedjava.slots.Operator.Precedence.DOT;
import static bluej.stride.framedjava.slots.Operator.Precedence.HIGH;
import static bluej.stride.framedjava.slots.Operator.Precedence.LOW;
import static bluej.stride.framedjava.slots.Operator.Precedence.MEDIUM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestTypeSlot
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
        InfixType e = StructuredSlot.testingModification(token -> new InfixType(null, null, token));
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
            e = StructuredSlot.testingModification(token -> new InfixType(null, null, token));
            e.testingInsert(e.testingInsert(insertion.substring(0, split), '\0'),  insertion.substring(split));
            assertEquals(insertion + " -> " + noPos, new CompareWrapper(noPos), new CompareWrapper(e.testingGetState(null)));
        }
        
        
    }
    
    private void testMultiInsert(String multiInsertion, String firstResult, String secondResult)
    {
        InfixType e = StructuredSlot.testingModification(token -> new InfixType(null, null, token));
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
        InfixType e = StructuredSlot.testingModification(token -> new InfixType(null, null, token));
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
            InfixType e = StructuredSlot.testingModification(token -> new InfixType(null, null, token));
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
            InfixType e = StructuredSlot.testingModification(token -> new InfixType(null, null, token));
            CaretPos p = e.testingInsert(joined, '\b');
            e.positionCaret(p);
            p = e.testingDelete(p);
            assertEquals(joined.replace("\b", "\\DEL") + " -> " + result, new CompareWrapper(result), new CompareWrapper(e.testingGetState(p)));
        }
    }
    
    private void testDeleteSelection(String src, String result)
    {
        InfixType e = StructuredSlot.testingModification(token -> new InfixType(null, null, token));
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
        InfixType e = StructuredSlot.testingModification(token -> new InfixType(null, null, token));
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
        InfixType e = StructuredSlot.testingModification(token -> new InfixType(null, null, token));
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
        testInsert("a+b", "{ab$}");
        testInsert("a+b-c", "{abc$}");
        testInsert("1++1", "{11$}");
        testInsert("5==-6", "{56$}");
        testInsert("a.b", "{a}.{b$}");
        testInsert("a..b", "{a}.{}.{b$}");
        testInsert("getY()*1", "{getY1$}");
        testInsert("getY[]-1", "{getY}_[{}]_{1$}");
    }
    
    @Test
    public void testNew()
    {
        testInsert("newton", "{newton$}");
        testInsert("new ton", "{newton$}");
    }
    
    @Test
    public void testStrings()
    {
        // With trailing quote
        testInsert("\"hello\"", "{hello$}");
        // Without (caret stays in string:
        testInsert("\"hello", "{hello$}");
        // Quote in a string, escaped:
        testInsert("\"\\\"\"", "{$}");
    }
    
    @Test
    public void testBrackets()
    {
        testInsert("a+(b-c)", "{abc$}");
        testInsert("a+(b-(c*d))", "{abcd$}");

        testInsert("a<b>", "{a}_<{b}>_{$}");
        testInsert("a<b<c>>", "{a}_<{b}_<{c}>_{}>_{$}");
        
        // Without close:
        testInsert("<a.b", "{}_<{a}.{b$}>_{}");
        
        testInsert("<<<", "{}_<{}_<{}_<{$}>_{}>_{}>_{}");
        testInsert("[[[", "{}_[{}_[{}_[{$}]_{}]_{}]_{}");
        testInsert("(((", "{$}");
        testInsert("[[[]", "{}_[{}_[{}_[{}]_{$}]_{}]_{}");
        testInsert("[[[]]", "{}_[{}_[{}_[{}]_{}]_{$}]_{}");
        testInsert("[[[]]]", "{}_[{}_[{}_[{}]_{}]_{}]_{$}");
    }
    
    @Test
    public void testBackspace()
    {
        testBackspace("\bxyz", "{$xyz}");
        testBackspace("x\byz", "{$yz}");
        testBackspace("xy\bz", "{x$z}");
        testBackspace("xyz\b", "{xy$}");
        
        testBackspace("xy\b.ab", "{x$}.{ab}");
        testBackspace("xy.\bab", "{xy$ab}");
        testBackspace("xy.a\bb", "{xy}.{$b}");
        
        testBackspace("move[\b]", "{move$}");
        testBackspace("[\b]", "{$}");
        testBackspace("<\b>", "{$}");
    }
    
    @Test
    public void testFloating()
    {
        testInsert("1.0", "{1}.{0$}");
        testInsert("10.20", "{10}.{20$}");
        testInsert("a.0", "{a}.{0$}");
        testInsert("1.a", "{1}.{a$}");
        testInsert("x1.a", "{x1}.{a$}");
        testInsert("+1", "{1$}");
        testInsert("+1.0", "{1}.{0$}");
        testInsert("+1.0e5", "{1}.{0e5$}");
        testInsert("+1.0e", "{1}.{0e$}");
        testInsert("+1.0e+5", "{1}.{0e5$}");
        testInsert("+1.0p+5", "{1}.{0p5$}");
        testInsert("3+1", "{31$}");
        testInsert("3+1.0", "{31}.{0$}");
        testInsert("3+1.0e5", "{31}.{0e5$}");
        testInsert("3+1.0e+5", "{31}.{0e5$}");
        testInsert("3+1.0p+5", "{31}.{0p5$}");
        
        testInsert("1e6", "{1e6$}");
        testInsert("1e-6", "{1e6$}");
        testInsert("10e20", "{10e20$}");
        testInsert("10e+20", "{10e20$}");
        testInsert("10e-20", "{10e20$}");
        
        testInsert("1.0.3", "{1}.{0}.{3$}");
        testInsert("1.0.3.4", "{1}.{0}.{3}.{4$}");
        testInsert("1.0.x3.4", "{1}.{0}.{x3}.{4$}");
        
        
        testInsert("1.0", "{1}.{0$}");
        testInsert("1..0", "{1}.{}.{0$}");
        testBackspace("1..\b0", "{1}.{$0}", true, false); // backspace after
        testBackspace("1.\b.0", "{1$}.{0}", false, true); // delete before
        testBackspace("a..\bc", "{a}.{$c}", true, false); // backspace after
        testBackspace("a.\b.c", "{a$}.{c}", false, true); // delete before
    }
    
    @Test
    public void testDeleteBracket()
    {
        testInsert("a<b.c>", "{a}_<{b}.{c}>_{$}");
        testBackspace("a<b.c>\b", "{ab}.{c$}");
        testBackspace("a<\bb.c>", "{a$b}.{c}");
    }
    
    @Test
    public void testDeleteSelection()
    {
        testDeleteSelection("a{bc}d", "{a$d}");
        testDeleteSelection("a{bc}", "{a$}");
        testDeleteSelection("a<{b}.c>", "{a}_<{$}.{c}>_{}");
        testDeleteSelection("a<{b.}c>", "{a}_<{$c}>_{}");
        testDeleteSelection("a<{b.c}>", "{a}_<{$}>_{}");
        testDeleteSelection("a{<b.c>}", "{a$}");
        testDeleteSelection("a{<b.c>.}d.e", "{a$d}.{e}");
        testDeleteSelection("a{<b.c>.d}.e", "{a$}.{e}");
        testDeleteSelection("a{<b.c>.d.}e", "{a$e}");
    }
    
    @Test
    public void testSelectionOperation()
    {
        testSelectionInsert('<', "a{bc}d", "{a}_<{bc}>_{$d}");
        testSelectionInsert('<', "a{b.c}d", "{a}_<{b}.{c}>_{$d}");
        testSelectionInsert('<', "a{b.}d", "{a}_<{b}.{}>_{$d}");
        testSelectionInsert('<', "a{b.}", "{a}_<{b}.{}>_{$}");
        testSelectionInsert('<', "a{b..}", "{a}_<{b}.{}.{}>_{$}");
        testSelectionInsert('<', "a{b.}.", "{a}_<{b}.{}>_{$}.{}");
    }

    @Test
    public void testNoString()
    {
        testSelectionInsert('\"', "a{bc}d", "{abc$d}");
        testSelectionInsert('\'', "a{bc}d", "{abc$d}");
        testInsert("ab\"", "{ab$}");
        testInsert("a\"b", "{ab$}");
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
        
        testCaretPosMap("a.b.c", "{a}.{b}.{c}",
            new CPM(0, makeCaretPos(0, 0)),
            new CPM(1, makeCaretPos(0, 1)),
            new CPM(2, makeCaretPos(1, 0)),
            new CPM(3, makeCaretPos(1, 1)),
            new CPM(4, makeCaretPos(2, 0)),
            new CPM(5, makeCaretPos(2, 1))
        );

        testCaretPosMap("a<b.c>", "{a}_<{b}.{c}>_{}",
                new CPM(0, makeCaretPos(0, 0)),
                new CPM(1, makeCaretPos(0, 1)),
                new CPM(2, makeCaretPos(1, 0, 0)),
                new CPM(3, makeCaretPos(1, 0, 1)),
                new CPM(4, makeCaretPos(1, 1, 0)),
                new CPM(5, makeCaretPos(1, 1, 1)),
                new CPM(6, makeCaretPos(2, 0))
        );

        testCaretPosMap("a<b.c,d>", "{a}_<{b}.{c},{d}>_{}",
                new CPM(0, makeCaretPos(0, 0)),
                new CPM(1, makeCaretPos(0, 1)),
                new CPM(2, makeCaretPos(1, 0, 0)),
                new CPM(3, makeCaretPos(1, 0, 1)),
                new CPM(4, makeCaretPos(1, 1, 0)),
                new CPM(5, makeCaretPos(1, 1, 1)),
                new CPM(6, makeCaretPos(1, 2, 0)),
                new CPM(7, makeCaretPos(1, 2, 1)),
                new CPM(8, makeCaretPos(2, 0))
        );
    }
    
    @Test
    public void testOvertype()
    {
        // Opening bracket just before one does overtype:
        testInsertExisting("$[]", "int", "{int$}_[{}]_{}");
        testInsertExisting("int$[]", "[",  "{int}_[{$}]_{}");

        // Most operators, like ., don't overtype:
        testInsertExisting("a$.z", ".", "{a}.{$}.{z}");
        testInsertExisting("a$.z", ".b", "{a}.{b$}.{z}");

        // Commas overtype existing commas, if the slot afterwards is blank
        testInsertExisting("a$,", ",", "{a},{$}");
        testInsertExisting("a<b$,>", ",", "{a}_<{b},{$}>_{}");
        testInsertExisting("a$,b", ",", "{a},{$},{b}");
        testInsertExisting("a<$,b>", ",", "{a}_<{},{$},{b}>_{}");
    }
    
    @Test
    public void testBracket()
    {
        testInsert("(", "{$}");
        testInsert("foo()", "{foo$}");
    }
    
    private void testNoComma(String str)
    {
        InfixType type = StructuredSlot.testingModification(token -> new InfixType(null, null, token));
        type.testingInsert(str, '\0');
        AtomicBoolean run = new AtomicBoolean(false);
        type.runIfCommaDirect((a, b) -> run.set(true));
        assertFalse("Checking comma did not run", run.get());
    }

    private void testComma(String src, String before, String after)
    {
        InfixType type = StructuredSlot.testingModification(token -> new InfixType(null, null, token));
        type.testingInsert(src, '\0');
        String[] beforeAfter = new String[] {null, null};
        type.runIfCommaDirect((a, b) -> {beforeAfter[0] = a; beforeAfter[1] = b;});
        assertEquals("Checking before comma: \"" + src + "\"", before, beforeAfter[0]);
        assertEquals("Checking after comma: \"" + src + "\"", after, beforeAfter[1]);
    }
    
    @Test
    public void testComma()
    {
        testNoComma("aaa");
        testNoComma("a.b");
        testNoComma("a<b,c>");
        testComma(",", "", "");
        testComma("a,", "a", "");
        testComma(",b", "", "b");
        testComma("a,b", "a", "b");
        testComma("a<x>,b<y>", "a<x>", "b<y>");
        testComma("a<x,x2>,b<y,c<z1,z2>>", "a<x,x2>", "b<y,c<z1,z2>>");
    }
}
