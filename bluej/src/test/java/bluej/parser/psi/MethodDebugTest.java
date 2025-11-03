package bluej.parser.psi;

import org.jetbrains.kotlin.psi.KtFile;
import org.junit.Before;
import org.junit.Test;

public class MethodDebugTest {
    
    private PsiEnvironment env;
    
    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
    }
    
    @Test
    public void debugSimpleMethod() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun simpleMethod() {
                }
            }
            """;
        
        // Parse
        KtFile ktFile = env.parseFile("Test.kt", kotlinCode);
        
        // Create recorder
        CallbackRecorder recorder = new CallbackRecorder();
        PsiCallbackVisitor visitor = new PsiCallbackVisitor(recorder);
        
        // Visit
        ktFile.accept(visitor);
        
        // DEBUG: Print ALL callbacks
        System.out.println("===== ALL CALLBACKS =====");
        for (CallbackRecord record : recorder.getRecords()) {
            System.out.println(record.getCallbackName());
        }
        System.out.println("===== TOTAL: " + recorder.getRecords().size() + " =====");
        
        // DEBUG: Print traversal log
        System.out.println("===== TRAVERSAL LOG =====");
        for (String entry : visitor.getTraversalLog()) {
            System.out.println(entry);
        }
    }
}