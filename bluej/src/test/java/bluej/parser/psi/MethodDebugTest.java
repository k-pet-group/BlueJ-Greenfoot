package bluej.parser.psi;

import bluej.parser.psi.visitor.BaseVisitor;
import bluej.parser.psi.visitor.FileVisitor;
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
        BaseVisitor visitor = new FileVisitor(recorder);
        
        // Visit
        ktFile.accept(visitor);
        
        // DEBUG: Print ALL callbacks
        System.out.println("===== ALL CALLBACKS =====");
        for (CallbackRecord record : recorder.getRecords()) {
            System.out.println(record.getCallbackName());
        }
        System.out.println("===== TOTAL: " + recorder.getRecords().size() + " =====");
        
        // DEBUG: Validation result
        System.out.println("===== VALIDATION RESULT =====");
        System.out.println(recorder.getValidationResult().getValidationSummary());
    }
}