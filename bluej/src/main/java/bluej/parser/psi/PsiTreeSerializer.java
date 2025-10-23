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

import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializes Kotlin PSI trees into human-readable text format.
 * 
 * <p><b>Output Format</b>: Hierarchical text with line/column information for debugging and analysis.</p>
 * 
 * <p><b>Thread Safety</b>: Stateless, thread-safe (all methods are static)</p>
 * 
 * <p><b>Performance</b>: Typically ~1-5ms for standard Kotlin files</p>
 * 
 * <p><b>Design Pattern</b>: Stateless utility class with pure functions (PSI tree → text)</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * PsiEnvironment env = PsiEnvironment.getInstance();
 * KtFile ktFile = env.parseFile("Example.kt", sourceCode);
 * String serialized = PsiTreeSerializer.serialize(ktFile);
 * PsiTreeSerializer.writeToFile(serialized, outputPath);
 * }</pre>
 * 
 * @see <a href="docs/planning/tasks/03-psi-serializer.md">Task 03 Specification</a>
 * @see <a href="docs/architecture/kotlin-psi-parser-facade-design.md">Architecture Design</a>
 * @since BlueJ 5.4.0
 */
public final class PsiTreeSerializer {
    
    // ==================== CONSTANTS ====================
    
    /** Separator line for file header */
    private static final String SEPARATOR = "=".repeat(80);
    
    /** Indentation string per level */
    private static final String INDENT = "  ";
    
    // ==================== CONSTRUCTOR ====================
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private PsiTreeSerializer() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Serialize a KtFile to human-readable text.
     * 
     * <p>Traverses the PSI tree and generates formatted output including:</p>
     * <ul>
     *   <li>Package declarations</li>
     *   <li>Import statements</li>
     *   <li>Classes (regular, data, sealed, enum, interface)</li>
     *   <li>Objects (singleton, companion)</li>
     *   <li>Functions (top-level, member, with modifiers)</li>
     *   <li>Properties (top-level, member, with accessors)</li>
     *   <li>Type parameters and supertypes</li>
     *   <li>Location information (line, column)</li>
     * </ul>
     * 
     * @param ktFile The PSI file to serialize
     * @return Formatted text representation
     * @throws NullPointerException if ktFile is null
     */
    public static String serialize(KtFile ktFile) {
        if (ktFile == null) {
            throw new NullPointerException("KtFile cannot be null");
        }
        
        StringBuilder sb = new StringBuilder();
        serializeFile(ktFile, sb);
        return sb.toString();
    }
    
    /**
     * Write serialized PSI tree to file.
     * 
     * <p>Creates parent directories if they don't exist.</p>
     * 
     * @param content The serialized content
     * @param outputPath Path to output file
     * @throws IOException If file writing fails
     * @throws NullPointerException if content or outputPath is null
     */
    public static void writeToFile(String content, Path outputPath) throws IOException {
        if (content == null) {
            throw new NullPointerException("Content cannot be null");
        }
        if (outputPath == null) {
            throw new NullPointerException("Output path cannot be null");
        }
        
        // Create parent directories if needed
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        
        // Write content with UTF-8 encoding
        Files.writeString(outputPath, content, StandardCharsets.UTF_8);
    }
    
    // ==================== PRIVATE SERIALIZATION ====================
    
    /**
     * Serialize KtFile top-level structure.
     * 
     * <p>Includes file header, package, imports, and top-level declarations.</p>
     */
    private static void serializeFile(KtFile ktFile, StringBuilder sb) {
        // Header
        sb.append("PSI Tree: ").append(ktFile.getName()).append("\n");
        sb.append(SEPARATOR).append("\n");
        
        // File info
        sb.append("KtFile: ").append(ktFile.getName()).append("\n");
        
        // Package
        KtPackageDirective pkg = ktFile.getPackageDirective();
        if (pkg != null && pkg.getQualifiedName() != null && !pkg.getQualifiedName().isEmpty()) {
            sb.append("  Package: ").append(pkg.getQualifiedName()).append("\n");
        }
        
        // Imports
        List<KtImportDirective> imports = ktFile.getImportDirectives();
        if (!imports.isEmpty()) {
            sb.append("  Imports:\n");
            for (KtImportDirective imp : imports) {
                if (imp.getImportedFqName() != null) {
                    sb.append("    - ").append(imp.getImportedFqName()).append("\n");
                }
            }
        }
        
        // Declarations
        List<KtDeclaration> declarations = ktFile.getDeclarations();
        if (!declarations.isEmpty()) {
            sb.append("\n");
            for (KtDeclaration decl : declarations) {
                serializeDeclaration(decl, sb, 1);
                sb.append("\n");
            }
        }
    }
    
    /**
     * Serialize a declaration (dispatches to specific handlers).
     * 
     * <p>Handles type-specific serialization with fallback for unknown types.</p>
     */
    private static void serializeDeclaration(KtDeclaration decl, StringBuilder sb, int indent) {
        if (decl instanceof KtClass) {
            serializeClass((KtClass) decl, sb, indent);
        } else if (decl instanceof KtObjectDeclaration) {
            // Check object before function since companion objects are also declarations
            serializeObject((KtObjectDeclaration) decl, sb, indent);
        } else if (decl instanceof KtNamedFunction) {
            serializeFunction((KtNamedFunction) decl, sb, indent);
        } else if (decl instanceof KtProperty) {
            serializeProperty((KtProperty) decl, sb, indent);
        } else if (decl instanceof KtTypeAlias) {
            serializeTypeAlias((KtTypeAlias) decl, sb, indent);
        } else {
            // Generic fallback for unknown types
            String ind = indent(indent);
            sb.append(ind).append("Declaration: ");
            sb.append(decl.getClass().getSimpleName());
            appendLocation(decl, sb);
            sb.append("\n");
        }
    }
    
    /**
     * Serialize a class declaration (class, interface, enum, data class, sealed class).
     *
     * <p>Includes modifiers, type parameters, supertypes, primary constructor parameters
     * (for data classes), and recursively serializes members.</p>
     */
    private static void serializeClass(KtClass klass, StringBuilder sb, int indent) {
        String ind = indent(indent);
        
        // Class header with type
        sb.append(ind);
        if (klass.isInterface()) {
            sb.append("Interface: ");
        } else if (klass.isEnum()) {
            sb.append("Enum: ");
        } else if (klass.isData()) {
            sb.append("Data Class: ");
        } else if (klass.isSealed()) {
            sb.append("Sealed Class: ");
        } else if (klass.isInner()) {
            sb.append("Inner Class: ");
        } else if (klass.isAnnotation()) {
            sb.append("Annotation Class: ");
        } else {
            sb.append("Class: ");
        }
        
        sb.append(klass.getName() != null ? klass.getName() : "<anonymous>");
        appendLocation(klass.getNameIdentifier(), sb);
        sb.append("\n");
        
        // Modifiers
        List<String> modifiers = extractModifiers(klass);
        if (!modifiers.isEmpty()) {
            sb.append(ind).append("  Modifiers: ");
            sb.append(String.join(", ", modifiers)).append("\n");
        }
        
        // Type parameters
        List<KtTypeParameter> typeParams = klass.getTypeParameters();
        if (!typeParams.isEmpty()) {
            sb.append(ind).append("  Type Parameters: [");
            for (int i = 0; i < typeParams.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(typeParams.get(i).getName());
            }
            sb.append("]\n");
        }
        
        // Primary constructor parameters (especially important for data classes)
        KtPrimaryConstructor primaryConstructor = klass.getPrimaryConstructor();
        if (primaryConstructor != null) {
            List<KtParameter> params = primaryConstructor.getValueParameters();
            if (!params.isEmpty()) {
                sb.append(ind).append("  Primary Constructor Parameters:\n");
                for (KtParameter param : params) {
                    serializeConstructorParameter(param, sb, indent + 2);
                }
            }
        }
        
        // Supertypes
        List<KtSuperTypeListEntry> supertypes = klass.getSuperTypeListEntries();
        if (!supertypes.isEmpty()) {
            sb.append(ind).append("  Supertypes:\n");
            for (KtSuperTypeListEntry supertype : supertypes) {
                sb.append(ind).append("    - ").append(supertype.getText()).append("\n");
            }
        }
        
        // Members (recursive)
        List<KtDeclaration> members = klass.getDeclarations();
        if (!members.isEmpty()) {
            sb.append("\n");
            for (KtDeclaration member : members) {
                serializeDeclaration(member, sb, indent + 1);
            }
        }
    }
    
    /**
     * Serialize a function declaration.
     * 
     * <p>Includes modifiers, type parameters, return type, parameters, and body presence.</p>
     */
    private static void serializeFunction(KtNamedFunction func, StringBuilder sb, int indent) {
        String ind = indent(indent);
        
        sb.append(ind).append("Function: ").append(func.getName());
        appendLocation(func.getNameIdentifier(), sb);
        sb.append("\n");
        
        // Modifiers
        List<String> modifiers = extractModifiers(func);
        if (!modifiers.isEmpty()) {
            sb.append(ind).append("  Modifiers: ");
            sb.append(String.join(", ", modifiers)).append("\n");
        }
        
        // Type parameters
        List<KtTypeParameter> typeParams = func.getTypeParameters();
        if (!typeParams.isEmpty()) {
            sb.append(ind).append("  Type Parameters: [");
            for (int i = 0; i < typeParams.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(typeParams.get(i).getName());
            }
            sb.append("]\n");
        }
        
        // Return type
        KtTypeReference returnType = func.getTypeReference();
        sb.append(ind).append("  Return Type: ");
        sb.append(returnType != null ? returnType.getText() : "Unit");
        sb.append("\n");
        
        // Parameters
        List<KtParameter> params = func.getValueParameters();
        sb.append(ind).append("  Parameters: [");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            KtParameter param = params.get(i);
            sb.append(param.getName()).append(": ");
            KtTypeReference paramType = param.getTypeReference();
            sb.append(paramType != null ? paramType.getText() : "?");
            
            // Default value indicator
            if (param.hasDefaultValue()) {
                sb.append(" = <default>");
            }
        }
        sb.append("]\n");
        
        // Body presence
        sb.append(ind).append("  Body: ");
        sb.append(func.hasBody() ? "present" : "absent");
        sb.append("\n");
    }
    
    /**
     * Serialize a property declaration.
     * 
     * <p>Includes modifiers (val/var), type, initializer, and accessor information.</p>
     */
    private static void serializeProperty(KtProperty prop, StringBuilder sb, int indent) {
        String ind = indent(indent);
        
        sb.append(ind).append("Property: ").append(prop.getName());
        appendLocation(prop.getNameIdentifier(), sb);
        sb.append("\n");
        
        // Modifiers (includes val/var)
        List<String> modifiers = extractModifiers(prop);
        // Add val/var explicitly if not in modifiers
        if (prop.isVar()) {
            if (!modifiers.contains("var")) {
                modifiers.add(0, "var");
            }
        } else {
            if (!modifiers.contains("val")) {
                modifiers.add(0, "val");
            }
        }
        
        if (!modifiers.isEmpty()) {
            sb.append(ind).append("  Modifiers: ");
            sb.append(String.join(", ", modifiers)).append("\n");
        }
        
        // Type
        KtTypeReference type = prop.getTypeReference();
        sb.append(ind).append("  Type: ");
        sb.append(type != null ? type.getText() : "<inferred>");
        sb.append("\n");
        
        // Initializer
        sb.append(ind).append("  Initializer: ");
        sb.append(prop.hasInitializer() ? "present" : "absent");
        sb.append("\n");
        
        // Getter
        sb.append(ind).append("  Getter: ");
        sb.append(prop.getGetter() != null ? "present" : "absent");
        sb.append("\n");
        
        // Setter
        sb.append(ind).append("  Setter: ");
        sb.append(prop.getSetter() != null ? "present" : "absent");
        sb.append("\n");
    }
    
    /**
     * Serialize an object declaration (singleton object or companion object).
     * 
     * <p>Includes supertypes and recursively serializes members.</p>
     */
    private static void serializeObject(KtObjectDeclaration obj, StringBuilder sb, int indent) {
        String ind = indent(indent);
        
        sb.append(ind);
        if (obj.isCompanion()) {
            sb.append("Companion Object: ");
        } else {
            sb.append("Object: ");
        }
        sb.append(obj.getName() != null ? obj.getName() : "<anonymous>");
        appendLocation(obj.getNameIdentifier(), sb);
        sb.append("\n");
        
        // Supertypes
        List<KtSuperTypeListEntry> supertypes = obj.getSuperTypeListEntries();
        if (!supertypes.isEmpty()) {
            sb.append(ind).append("  Supertypes:\n");
            for (KtSuperTypeListEntry supertype : supertypes) {
                sb.append(ind).append("    - ").append(supertype.getText()).append("\n");
            }
        }
        
        // Members (recursive)
        List<KtDeclaration> members = obj.getDeclarations();
        if (!members.isEmpty()) {
            sb.append("\n");
            for (KtDeclaration member : members) {
                serializeDeclaration(member, sb, indent + 1);
            }
        }
    }
    
    /**
     * Serialize a primary constructor parameter.
     *
     * <p>For data classes and other classes with primary constructors,
     * parameters can also be properties (val/var modifiers).</p>
     */
    private static void serializeConstructorParameter(KtParameter param, StringBuilder sb, int indent) {
        String ind = indent(indent);
        
        sb.append(ind).append("Property: ").append(param.getName());
        appendLocation(param.getNameIdentifier(), sb);
        sb.append("\n");
        
        // Check if it's a property (val/var)
        List<String> modifiers = new ArrayList<>();
        if (param.hasValOrVar()) {
            modifiers.add(param.isVarArg() ? "vararg" : (param.isMutable() ? "var" : "val"));
        }
        
        // Add other modifiers
        KtModifierList modList = param.getModifierList();
        if (modList != null) {
            if (modList.hasModifier(KtTokens.PRIVATE_KEYWORD)) modifiers.add("private");
            if (modList.hasModifier(KtTokens.PROTECTED_KEYWORD)) modifiers.add("protected");
            if (modList.hasModifier(KtTokens.INTERNAL_KEYWORD)) modifiers.add("internal");
        }
        
        if (!modifiers.isEmpty()) {
            sb.append(ind).append("  Modifiers: ");
            sb.append(String.join(", ", modifiers)).append("\n");
        }
        
        // Type
        KtTypeReference type = param.getTypeReference();
        sb.append(ind).append("  Type: ");
        sb.append(type != null ? type.getText() : "<unknown>");
        sb.append("\n");
        
        // Default value
        sb.append(ind).append("  Initializer: ");
        sb.append(param.hasDefaultValue() ? "present" : "absent");
        sb.append("\n");
        
        // For primary constructor properties, getter/setter are implicit
        if (param.hasValOrVar()) {
            sb.append(ind).append("  Getter: implicit\n");
            if (param.isMutable()) {
                sb.append(ind).append("  Setter: implicit\n");
            } else {
                sb.append(ind).append("  Setter: absent\n");
            }
        }
    }
    
    /**
     * Serialize a type alias declaration.
     *
     * <p>Includes type parameters and aliased type.</p>
     */
    private static void serializeTypeAlias(KtTypeAlias alias, StringBuilder sb, int indent) {
        String ind = indent(indent);
        
        sb.append(ind).append("TypeAlias: ").append(alias.getName());
        appendLocation(alias.getNameIdentifier(), sb);
        sb.append("\n");
        
        // Type parameters
        List<KtTypeParameter> typeParams = alias.getTypeParameters();
        if (!typeParams.isEmpty()) {
            sb.append(ind).append("  Type Parameters: [");
            for (int i = 0; i < typeParams.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(typeParams.get(i).getName());
            }
            sb.append("]\n");
        }
        
        // Aliased type
        KtTypeReference typeRef = alias.getTypeReference();
        sb.append(ind).append("  Aliased Type: ");
        sb.append(typeRef != null ? typeRef.getText() : "<unresolved>");
        sb.append("\n");
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Create indentation string.
     * 
     * @param level Indentation level (0-based)
     * @return Indentation string
     */
    private static String indent(int level) {
        return INDENT.repeat(Math.max(0, level));
    }
    
    /**
     * Extract modifiers from declaration.
     * 
     * <p>Includes visibility, inheritance, function, class, and property modifiers.</p>
     * 
     * @param owner Declaration with modifiers
     * @return List of modifier strings
     */
    private static List<String> extractModifiers(KtModifierListOwner owner) {
        List<String> mods = new ArrayList<>();
        KtModifierList modList = owner.getModifierList();
        if (modList == null) return mods;
        
        // Visibility modifiers
        if (modList.hasModifier(KtTokens.PUBLIC_KEYWORD)) mods.add("public");
        if (modList.hasModifier(KtTokens.PRIVATE_KEYWORD)) mods.add("private");
        if (modList.hasModifier(KtTokens.PROTECTED_KEYWORD)) mods.add("protected");
        if (modList.hasModifier(KtTokens.INTERNAL_KEYWORD)) mods.add("internal");
        
        // Inheritance modifiers
        if (modList.hasModifier(KtTokens.ABSTRACT_KEYWORD)) mods.add("abstract");
        if (modList.hasModifier(KtTokens.FINAL_KEYWORD)) mods.add("final");
        if (modList.hasModifier(KtTokens.OPEN_KEYWORD)) mods.add("open");
        if (modList.hasModifier(KtTokens.OVERRIDE_KEYWORD)) mods.add("override");
        if (modList.hasModifier(KtTokens.SEALED_KEYWORD)) mods.add("sealed");
        
        // Function modifiers
        if (modList.hasModifier(KtTokens.SUSPEND_KEYWORD)) mods.add("suspend");
        if (modList.hasModifier(KtTokens.INLINE_KEYWORD)) mods.add("inline");
        if (modList.hasModifier(KtTokens.INFIX_KEYWORD)) mods.add("infix");
        if (modList.hasModifier(KtTokens.OPERATOR_KEYWORD)) mods.add("operator");
        if (modList.hasModifier(KtTokens.TAILREC_KEYWORD)) mods.add("tailrec");
        if (modList.hasModifier(KtTokens.EXTERNAL_KEYWORD)) mods.add("external");
        
        // Class modifiers
        if (modList.hasModifier(KtTokens.DATA_KEYWORD)) mods.add("data");
        if (modList.hasModifier(KtTokens.INNER_KEYWORD)) mods.add("inner");
        if (modList.hasModifier(KtTokens.COMPANION_KEYWORD)) mods.add("companion");
        if (modList.hasModifier(KtTokens.ENUM_KEYWORD)) mods.add("enum");
        if (modList.hasModifier(KtTokens.ANNOTATION_KEYWORD)) mods.add("annotation");
        
        // Property modifiers
        if (modList.hasModifier(KtTokens.CONST_KEYWORD)) mods.add("const");
        if (modList.hasModifier(KtTokens.LATEINIT_KEYWORD)) mods.add("lateinit");
        
        return mods;
    }
    
    /**
     * Append location information (line, column) to StringBuilder.
     *
     * <p>Calculates line/column by counting newlines in the file text up to the element's offset.
     * This approach works without PsiDocumentManager which is not available in the Kotlin
     * compiler's repackaged IntelliJ Platform.</p>
     *
     * <p>Gracefully handles errors - if location unavailable, continues silently.</p>
     *
     * @param element PSI element to get location from (KtDeclaration or PsiElement)
     * @param sb StringBuilder to append to
     */
    private static void appendLocation(org.jetbrains.kotlin.com.intellij.psi.PsiElement element, StringBuilder sb) {
        if (element == null) return;
        
        try {
            TextRange range = element.getTextRange();
            if (range == null) return;
            
            int offset = range.getStartOffset();
            String fileText = element.getContainingFile().getText();
            
            // Count newlines up to offset to get line number
            int line = 1;
            int col = 1;
            for (int i = 0; i < offset && i < fileText.length(); i++) {
                if (fileText.charAt(i) == '\n') {
                    line++;
                    col = 1;
                } else {
                    col++;
                }
            }
            
            sb.append(" (line ").append(line);
            sb.append(", col ").append(col).append(")");
        } catch (Exception e) {
            // Location unavailable, skip silently
        }
    }
}