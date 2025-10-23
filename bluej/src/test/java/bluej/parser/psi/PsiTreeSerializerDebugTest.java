package bluej.parser.psi;

import org.jetbrains.kotlin.psi.KtFile;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Debug test to print actual serializer output for verification.
 */
public class PsiTreeSerializerDebugTest {
    
    private PsiEnvironment env;
    
    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
        assertTrue("PSI environment must be initialized", env.isInitialized());
    }
    
    @Test
    public void debugDataClassOutput() {
        String source = "data class User(val name: String, var age: Int)";
        KtFile file = env.parseFile("User.kt", source);
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        System.out.println("=== DEBUG: Data Class Output ===");
        System.out.println(output);
        System.out.println("=== END DEBUG ===");
        
        // Print for manual verification
        assertTrue("Output should not be empty", output.length() > 0);
    }
    
    @Test
    public void debugComplexFileOutput() {
        String source = 
            "package com.example\n" +
            "\n" +
            "import kotlin.collections.List\n" +
            "\n" +
            "data class Person(val name: String, var age: Int)\n" +
            "\n" +
            "object Logger {\n" +
            "    fun log(message: String) = println(message)\n" +
            "}\n" +
            "\n" +
            "fun main(args: Array<String>) {\n" +
            "    val person = Person(\"Alice\", 30)\n" +
            "    Logger.log(person.name)\n" +
            "}";
        
        KtFile file = env.parseFile("Complex.kt", source);
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        System.out.println("=== DEBUG: Complex File Output ===");
        System.out.println(output);
        System.out.println("=== END DEBUG ===");
        
        assertTrue("Output should not be empty", output.length() > 0);
    }
}