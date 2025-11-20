package bluej.parser.psi;

import bluej.parser.psi.visitor.BaseVisitor;
import bluej.parser.psi.visitor.FileVisitor;
import org.jetbrains.kotlin.psi.KtFile;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PropertyDebugTest {
    
    private PsiEnvironment env;
    
    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
        assertTrue("PSI environment must be initialized", env.isInitialized());
    }
    
    @Test
    public void debugPropertyCallbacks() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                val name: String = "test"
            }
            """;
        
        KtFile ktFile = env.parseFile("Test.kt", kotlinCode);
        assertNotNull("File should parse", ktFile);
        
        CallbackRecorder recorder = new CallbackRecorder();
        BaseVisitor visitor = new FileVisitor(recorder);
        
        ktFile.accept(visitor);
        
        System.out.println("\n=== CALLBACK SEQUENCE ===");
        System.out.println(recorder.getCallbackSequence());
        System.out.println("\n=== ALL CALLBACKS ===");
        recorder.getRecords().forEach(r -> 
            System.out.println(r.getCallbackName() + ": " + r.getParameters())
        );
        
        System.out.println("\n=== VALIDATION ===");
        System.out.println(recorder.getDetailedValidationSummary());
    }
}