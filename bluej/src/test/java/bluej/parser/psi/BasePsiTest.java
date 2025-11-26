package bluej.parser.psi;

import bluej.extensions2.SourceType;
import bluej.parser.psi.visitor.BaseVisitor;
import bluej.parser.psi.visitor.FileVisitor;
import org.jetbrains.kotlin.psi.KtFile;

import java.nio.charset.Charset;
import java.util.UUID;

import static org.junit.Assert.*;

public class BasePsiTest {
    protected PsiEnvironment env;

    /**
     * Helper method to parse Kotlin code and visit with CallbackRecorder.
     *
     * @param kotlinCode The Kotlin source code to parse
     * @return CallbackRecorder with captured callbacks
     * @throws PsiParseException if parsing fails
     */
    protected CallbackRecorder parseAndVisit(String kotlinCode) throws PsiParseException {
        UUID uuid = UUID.randomUUID();
        String name = uuid.toString() + ".kt";

        return parseAndVisit(SourceInput.fromNamedString(kotlinCode, SourceType.Kotlin, Charset.defaultCharset(), name, name, null));
    }

    protected CallbackRecorder parseAndVisit(SourceInput sourceInput) throws PsiParseException {
        String kotlinCode = sourceInput.content();

        // Parse Kotlin code to KtFile
        KtFile ktFile = env.parseFile("Test.kt", kotlinCode);
        assertNotNull("File should parse successfully", ktFile);

        // Create recorder and visitor
        CallbackRecorder recorder = new CallbackRecorder();
        BaseVisitor visitor = new FileVisitor(recorder);

        // Visit the file (triggers class and constructor visitation)
        ktFile.accept(visitor);

        // Validate callback pairing is balanced after traversal
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();

        if (!result.isBalanced() || result.hasErrors()) {
            System.err.println("Callback validation summary:\n\n" + result.getValidationSummary());
        }
        else {
            System.out.println("Callback validation summary:\n\n" + result.getValidationSummary());
        }

        assertTrue("Callback pairing should be balanced after traversal", result.isBalanced());
        assertFalse("Should have no validation errors after traversal", result.hasErrors());

        return recorder;
    }
}
