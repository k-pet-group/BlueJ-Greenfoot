/*
 This file is part of the BlueJ program.
 Copyright (C) 2025  Michael Kolling and John Rosenberg

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
package bluej.parser.psi;

/**
 * Exception thrown when PSI parsing operations fail.
 * 
 * <p>This exception is used throughout the PSI infrastructure to signal failures in parsing
 * Kotlin source code using IntelliJ's PSI (Program Structure Interface). Common scenarios
 * include syntax errors, file I/O issues during parsing, or internal PSI environment failures.</p>
 * 
 * <p><b>When to Throw</b>:</p>
 * <ul>
 *   <li>When {@link PsiEnvironment#parseFile(String, String)} encounters unparseable Kotlin syntax</li>
 *   <li>When {@link KotlinPsiParser} fails to initialize or process source code</li>
 *   <li>When PSI tree operations fail due to invalid state or corrupted structures</li>
 *   <li>When file I/O operations required for parsing fail</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Example 1: Parsing with error handling
 * try {
 *     PsiEnvironment env = PsiEnvironment.getInstance();
 *     KtFile ktFile = env.parseFile("Example.kt", sourceCode);
 *     // Process ktFile...
 * } catch (PsiParseException e) {
 *     logger.error("Failed to parse Kotlin file: " + e.getMessage(), e);
 *     // Handle parse failure...
 * }
 * 
 * // Example 2: Wrapping lower-level exceptions
 * try {
 *     KtFile ktFile = parseKotlinSource(filename, content);
 * } catch (IOException e) {
 *     throw new PsiParseException("Failed to read source file: " + filename, e);
 * }
 * 
 * // Example 3: Simple error message
 * if (!isValidKotlinSyntax(sourceCode)) {
 *     throw new PsiParseException("Invalid Kotlin syntax detected");
 * }
 * }</pre>
 * 
 * <p><b>Design Rationale</b>:</p>
 * <p>This exception replaces generic {@code Exception} usage in PSI parsing operations,
 * providing clearer error handling semantics and better type safety. It allows callers to
 * distinguish PSI parsing failures from other types of errors.</p>
 * 
 * <p><b>Thread Safety</b>: This exception class is thread-safe and immutable once constructed.</p>
 * 
 * @see PsiEnvironment
 * @see KotlinPsiParser
 * @see PsiTreeSerializer
 * @since BlueJ 5.4.0
 */
public class PsiParseException extends Exception {
    
    /** Serial version UID for exception serialization compatibility. */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new PSI parse exception with no detail message.
     * 
     * <p>This constructor should rarely be used. Prefer constructors that provide
     * descriptive error messages for better debugging and error reporting.</p>
     */
    public PsiParseException() {
        super();
    }
    
    /**
     * Constructs a new PSI parse exception with the specified detail message.
     * 
     * <p>The message should clearly describe what parsing operation failed and why.
     * Include relevant context such as file names, line numbers, or syntax elements
     * that caused the failure.</p>
     * 
     * @param message the detail message explaining the parsing failure
     */
    public PsiParseException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new PSI parse exception with the specified detail message and cause.
     * 
     * <p>This constructor is particularly useful when wrapping lower-level exceptions
     * (such as {@code IOException} from file operations) to provide additional context
     * about the parsing operation that failed.</p>
     * 
     * @param message the detail message explaining the parsing failure
     * @param cause the underlying cause of the parsing failure (may be {@code null})
     */
    public PsiParseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new PSI parse exception with the specified cause.
     * 
     * <p>The detail message will be derived from the cause's message.
     * Use this constructor when the underlying cause provides sufficient context,
     * or when the parsing failure is entirely due to the underlying exception.</p>
     * 
     * @param cause the underlying cause of the parsing failure (may be {@code null})
     */
    public PsiParseException(Throwable cause) {
        super(cause);
    }
}