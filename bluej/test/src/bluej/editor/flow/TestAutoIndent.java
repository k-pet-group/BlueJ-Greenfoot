package bluej.editor.flow;

import bluej.parser.InitConfig;
import bluej.parser.TestableDocument;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

/**
 * Tests the behaviour of the auto-indent feature in the Java editor.
 */
public class TestAutoIndent
{
    @BeforeClass
    public static void init()
    {
        InitConfig.init();
    }

    private void runTest(String expected, String src)
    {
        TestableDocument parser = new TestableDocument();
        MultilineStringTracker multilineStringTracker = new MultilineStringTracker(parser.document, () -> {});
        parser.enableParser(true);
        parser.insertString(0, src);
        parser.flushReparseQueue();
        boolean perfect = FlowIndent.calculateIndentsAndApply(parser, parser.document, multilineStringTracker, 0).isPerfect();
        assertEquals(expected, parser.document.getContent(0, parser.document.getLength()).toString());
        assertEquals(expected.equals(src), perfect);
    }

    @Test
    public void testSimple()
    {
        String correct = """
                class Foo   {
                    public void foo();
                    
                    public Foo()
                    {
                        int x = 6;
                        if (true)
                            x = 8;
                    }
                }   
                """;
        String incorrect = """
                class Foo   {
                public void foo();
                
                
                          public Foo()
                        {
                    int x = 6;
                    if (true)
                              x = 8;
                              }
                              }
                """;
        runTest(correct, incorrect);
        // Check correct is not modified:
        runTest(correct, correct);
    }

    @Test
    public void testSelf() throws IOException
    {
        // This file should be perfectly indented, so let's try that
        // (This also makes a good test for not processing multiline Strings) 
        String us = Files.readString(new File("test/src/bluej/editor/flow/TestAutoIndent.java").toPath()).replaceAll("\\r\\n?", "\n");
        runTest(us, us);
    }
}
