/*
 This file is part of the BlueJ program.
 Copyright (C) 2024  Michael Kolling and John Rosenberg

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
package bluej.parser.kotlin;

import bluej.parser.lexer.LocatableToken;
import bluej.parser.symtab.ClassInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Parses Kotlin source files to extract class metadata ({@link ClassInfo})
 * for the class diagram. Uses token-stream scanning via {@link KotlinLexer}
 * to find the top-level class declaration and extract its structure.
 *
 * <p>This is the Kotlin parallel to {@code bluej.parser.InfoParser} for Java.
 * Unlike the Java InfoParser (which uses a full recursive-descent parser with
 * callbacks), this implementation uses a simpler single-pass token scan —
 * sufficient for BlueJ's one-class-per-file model.</p>
 *
 * <h3>Parsing strategy</h3>
 * <ol>
 *   <li>Tokenize the full file with {@link KotlinLexer}</li>
 *   <li>Scan for {@code package} declaration</li>
 *   <li>Skip imports</li>
 *   <li>Accumulate class modifiers ({@code abstract}, {@code open}, {@code data}, etc.)</li>
 *   <li>Match class/interface/object/enum declaration</li>
 *   <li>Extract type parameters, primary constructor params, supertype list</li>
 *   <li>Populate and return {@link ClassInfo}</li>
 * </ol>
 *
 * <h3>Supertype disambiguation heuristic</h3>
 * <p>Kotlin uses {@code :} for both extends and implements. Without type
 * resolution, we use this heuristic: supertypes followed by {@code (...)}
 * are classes (the first is the superclass), and those without parentheses
 * are interfaces. This is correct for the MVP subset.</p>
 *
 * @author BlueJ Team
 */
@OnThread(Tag.FXPlatform)
public class KotlinInfoParser
{
    /**
     * Parse a .kt file and extract class metadata.
     *
     * @param f the Kotlin source file
     * @return ClassInfo with name, superclass, interfaces, modifiers;
     *         or null if no class declaration found
     * @throws FileNotFoundException if the file doesn't exist
     */
    public static ClassInfo parse(File f) throws FileNotFoundException
    {
        try (FileReader reader = new FileReader(f))
        {
            return parse(reader, null);
        }
        catch (FileNotFoundException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Parse from a Reader and extract class metadata.
     *
     * @param r         reader over Kotlin source
     * @param targetPkg expected package name (for validation), may be null
     * @return ClassInfo with extracted metadata, or null if no class found
     */
    public static ClassInfo parse(Reader r, String targetPkg)
    {
        // Tokenize the entire file
        KotlinLexer lexer = new KotlinLexer(r);
        List<LocatableToken> tokens = tokenizeAll(lexer);

        if (tokens.isEmpty())
        {
            return null;
        }

        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    /**
     * Consume all tokens from the lexer into a list, filtering out
     * whitespace and dangling newlines.
     */
    private static List<LocatableToken> tokenizeAll(KotlinLexer lexer)
    {
        List<LocatableToken> tokens = new ArrayList<>();
        LocatableToken token;
        while (true)
        {
            token = lexer.nextToken();
            int type = token.getType();
            if (type == KotlinToken.EOF)
            {
                break;
            }
            // Skip whitespace
            if (type == KotlinToken.WHITE_SPACE || type == KotlinToken.DANGLING_NEWLINE)
            {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    // -----------------------------------------------------------------------
    // Internal token-stream parser
    // -----------------------------------------------------------------------

    /**
     * Stateful token-stream parser that walks through tokens and extracts
     * class metadata.
     */
    @OnThread(Tag.FXPlatform)
    private static class Parser
    {
        private final List<LocatableToken> tokens;
        private int pos = 0;

        // Accumulated state
        private boolean isAbstract = false;
        private boolean isOpen = false;
        private boolean isSealed = false;
        private boolean isData = false;
        private boolean isInner = false;
        private boolean isEnum = false;
        private boolean isPublic = true; // Kotlin classes are public by default
        private String lastDocComment = null;

        Parser(List<LocatableToken> tokens)
        {
            this.tokens = tokens;
        }

        ClassInfo parse()
        {
            // Phase 1: Skip package declaration
            skipPackage();

            // Phase 2: Skip import statements
            skipImports();

            // Phase 3: Find and parse the top-level class declaration
            return parseTopLevelDeclaration();
        }

        // --- Phase 1: Package ---

        private void skipPackage()
        {
            if (currentType() == KotlinToken.KW_PACKAGE)
            {
                pos++; // skip 'package' keyword
                // Skip: foo.bar.baz
                while (pos < tokens.size() && !isStatementEnd())
                {
                    pos++;
                }
            }
        }

        // --- Phase 2: Imports ---

        private void skipImports()
        {
            while (currentType() == KotlinToken.KW_IMPORT)
            {
                pos++; // skip 'import' keyword
                // Skip: foo.bar.Baz
                while (pos < tokens.size() && !isStatementEnd())
                {
                    pos++;
                }
            }
        }

        // --- Phase 3: Top-level declaration ---

        private ClassInfo parseTopLevelDeclaration()
        {
            // Collect modifiers and annotations before the class keyword
            while (pos < tokens.size())
            {
                int type = currentType();

                // Track KDoc comments
                if (type == KotlinToken.DOC_COMMENT)
                {
                    lastDocComment = currentText();
                    pos++;
                    continue;
                }

                // Skip other comments
                if (KotlinToken.isComment(type))
                {
                    pos++;
                    continue;
                }

                // Skip annotations (@Foo)
                if (type == KotlinToken.AT)
                {
                    skipAnnotation();
                    continue;
                }

                // Accumulate modifiers
                if (type == KotlinToken.KW_ABSTRACT) { isAbstract = true; pos++; continue; }
                if (type == KotlinToken.KW_OPEN) { isOpen = true; pos++; continue; }
                if (type == KotlinToken.KW_SEALED) { isSealed = true; pos++; continue; }
                if (type == KotlinToken.KW_DATA) { isData = true; pos++; continue; }
                if (type == KotlinToken.KW_INNER) { isInner = true; pos++; continue; }
                if (type == KotlinToken.KW_ENUM) { isEnum = true; pos++; continue; }
                if (type == KotlinToken.KW_PRIVATE) { isPublic = false; pos++; continue; }
                if (type == KotlinToken.KW_INTERNAL) { isPublic = false; pos++; continue; }
                if (type == KotlinToken.KW_PROTECTED) { isPublic = false; pos++; continue; }
                if (type == KotlinToken.KW_PUBLIC) { isPublic = true; pos++; continue; }

                // Skip other modifier keywords that don't affect ClassInfo
                if (type == KotlinToken.KW_FINAL || type == KotlinToken.KW_ANNOTATION
                    || type == KotlinToken.KW_VALUE || type == KotlinToken.KW_INLINE
                    || type == KotlinToken.KW_EXTERNAL || type == KotlinToken.KW_EXPECT
                    || type == KotlinToken.KW_ACTUAL)
                {
                    pos++;
                    continue;
                }

                // Class declaration
                if (type == KotlinToken.KW_CLASS)
                {
                    pos++;
                    return parseClassBody(false);
                }

                // Interface declaration
                if (type == KotlinToken.KW_INTERFACE)
                {
                    pos++;
                    return parseClassBody(true);
                }

                // Object declaration
                if (type == KotlinToken.KW_OBJECT)
                {
                    pos++;
                    return parseObjectBody();
                }

                // Not a declaration we recognize — skip
                pos++;
            }

            return null; // No class declaration found
        }

        // --- Parse class/interface body ---

        private ClassInfo parseClassBody(boolean isInterface)
        {
            // Expect: <Name> [<TypeParams>] [(PrimaryConstructor)] [: SuperTypes] [{...}]
            if (currentType() != KotlinToken.IDENTIFIER)
            {
                return null;
            }

            String className = currentText();
            pos++;

            ClassInfo info = new ClassInfo();
            info.setName(className, isPublic);
            info.setInterface(isInterface);
            info.setAbstract(isAbstract || isSealed || isInterface);
            info.setEnum(isEnum);

            if (lastDocComment != null)
            {
                info.addComment(className, lastDocComment, null);
            }

            // Type parameters: <T, U : Comparable<U>>
            if (currentType() == KotlinToken.LT)
            {
                parseTypeParameters(info);
            }

            // Primary constructor parameters: (val name: String, val age: Int)
            if (currentType() == KotlinToken.LPAR)
            {
                parseConstructorParams(info);
            }

            // Supertype list: : Base(), Interface1, Interface2
            if (currentType() == KotlinToken.COLON)
            {
                pos++; // skip ':'
                parseSupertypeList(info);
            }

            return info;
        }

        // --- Parse object declaration ---

        private ClassInfo parseObjectBody()
        {
            // Expect: <Name> [: SuperTypes] [{...}]
            // Handle: companion object { } (no name)
            if (currentType() != KotlinToken.IDENTIFIER)
            {
                return null;
            }

            String objectName = currentText();
            pos++;

            ClassInfo info = new ClassInfo();
            info.setName(objectName, isPublic);
            info.setAbstract(false);

            if (lastDocComment != null)
            {
                info.addComment(objectName, lastDocComment, null);
            }

            // Supertype list
            if (currentType() == KotlinToken.COLON)
            {
                pos++; // skip ':'
                parseSupertypeList(info);
            }

            return info;
        }

        // --- Type parameters ---

        private void parseTypeParameters(ClassInfo info)
        {
            // Skip '<', collect type param text until matching '>'
            pos++; // skip '<'
            int depth = 1;
            StringBuilder currentParam = new StringBuilder();

            while (pos < tokens.size() && depth > 0)
            {
                int type = currentType();
                if (type == KotlinToken.LT)
                {
                    depth++;
                    currentParam.append(currentText());
                }
                else if (type == KotlinToken.GT)
                {
                    depth--;
                    if (depth > 0)
                    {
                        currentParam.append(currentText());
                    }
                }
                else if (type == KotlinToken.COMMA && depth == 1)
                {
                    // Separator between type params
                    String param = currentParam.toString().trim();
                    if (!param.isEmpty())
                    {
                        info.addTypeParameterText(param);
                    }
                    currentParam = new StringBuilder();
                }
                else
                {
                    currentParam.append(currentText());
                }
                pos++;
            }

            // Add last type parameter
            String param = currentParam.toString().trim();
            if (!param.isEmpty())
            {
                info.addTypeParameterText(param);
            }
        }

        // --- Primary constructor parameters ---

        private void parseConstructorParams(ClassInfo info)
        {
            // Skip '(', extract parameter type names for 'used' list
            pos++; // skip '('
            int depth = 1;

            while (pos < tokens.size() && depth > 0)
            {
                int type = currentType();
                if (type == KotlinToken.LPAR)
                {
                    depth++;
                }
                else if (type == KotlinToken.RPAR)
                {
                    depth--;
                }
                else if (depth == 1 && type == KotlinToken.COLON)
                {
                    // After ':' comes the parameter type
                    pos++; // skip ':'
                    // Read the type name (could be qualified: foo.Bar)
                    String typeName = readTypeName();
                    if (typeName != null && !isPrimitiveKotlinType(typeName))
                    {
                        info.addUsed(typeName);
                    }
                    continue; // readTypeName already advanced pos
                }
                pos++;
            }
        }

        // --- Supertype list ---

        private void parseSupertypeList(ClassInfo info)
        {
            // Parse comma-separated supertypes until '{' or EOF
            // Heuristic: if followed by '(' it's a class (superclass), otherwise interface
            boolean foundSuperclass = false;

            while (pos < tokens.size())
            {
                int type = currentType();

                // End of supertype list
                if (type == KotlinToken.LBRACE || type == KotlinToken.KW_WHERE)
                {
                    break;
                }

                // Skip comments
                if (KotlinToken.isComment(type))
                {
                    pos++;
                    continue;
                }

                // Read the supertype name
                if (type == KotlinToken.IDENTIFIER)
                {
                    String supertypeName = readTypeName();
                    if (supertypeName == null)
                    {
                        break;
                    }

                    // Skip type arguments if present: <Foo, Bar>
                    if (currentType() == KotlinToken.LT)
                    {
                        skipBalanced(KotlinToken.LT, KotlinToken.GT);
                    }

                    // Check if followed by '(' — class constructor call
                    if (currentType() == KotlinToken.LPAR)
                    {
                        // It's a class supertype
                        if (!foundSuperclass)
                        {
                            info.setSuperclass(supertypeName);
                            foundSuperclass = true;
                        }
                        else
                        {
                            info.addUsed(supertypeName);
                        }
                        // Skip the constructor arguments
                        skipBalanced(KotlinToken.LPAR, KotlinToken.RPAR);
                    }
                    else
                    {
                        // No parens — it's an interface
                        info.addImplements(supertypeName);
                    }

                    // Skip comma between supertypes
                    if (currentType() == KotlinToken.COMMA)
                    {
                        pos++;
                    }
                }
                else
                {
                    pos++;
                }
            }
        }

        // --- Utility methods ---

        /**
         * Read a possibly qualified type name (e.g., "Foo" or "foo.bar.Baz").
         * Returns the simple name (last segment). Advances pos.
         */
        private String readTypeName()
        {
            if (currentType() != KotlinToken.IDENTIFIER)
            {
                return null;
            }

            String name = currentText();
            pos++;

            // Handle qualified names: skip dots and take the last identifier
            while (currentType() == KotlinToken.DOT)
            {
                pos++; // skip '.'
                if (currentType() == KotlinToken.IDENTIFIER)
                {
                    name = currentText();
                    pos++;
                }
                else
                {
                    break;
                }
            }

            return name;
        }

        /**
         * Skip a balanced pair of tokens (e.g., parentheses, angle brackets).
         */
        private void skipBalanced(int openType, int closeType)
        {
            if (currentType() != openType)
            {
                return;
            }
            pos++; // skip open
            int depth = 1;
            while (pos < tokens.size() && depth > 0)
            {
                int type = currentType();
                if (type == openType) depth++;
                else if (type == closeType) depth--;
                pos++;
            }
        }

        /**
         * Skip an annotation: @Foo or @Foo(args).
         */
        private void skipAnnotation()
        {
            pos++; // skip '@'
            // Skip annotation name (possibly qualified)
            while (currentType() == KotlinToken.IDENTIFIER || currentType() == KotlinToken.DOT)
            {
                pos++;
            }
            // Skip annotation arguments if present
            if (currentType() == KotlinToken.LPAR)
            {
                skipBalanced(KotlinToken.LPAR, KotlinToken.RPAR);
            }
        }

        /**
         * Check if current token marks the end of a statement
         * (semicolon or start of next declaration).
         */
        private boolean isStatementEnd()
        {
            int type = currentType();
            return type == KotlinToken.KW_IMPORT
                || type == KotlinToken.KW_CLASS
                || type == KotlinToken.KW_INTERFACE
                || type == KotlinToken.KW_OBJECT
                || type == KotlinToken.KW_FUN
                || type == KotlinToken.KW_VAL
                || type == KotlinToken.KW_VAR
                || type == KotlinToken.KW_ABSTRACT
                || type == KotlinToken.KW_OPEN
                || type == KotlinToken.KW_SEALED
                || type == KotlinToken.KW_DATA
                || type == KotlinToken.KW_ENUM
                || type == KotlinToken.KW_PRIVATE
                || type == KotlinToken.KW_PUBLIC
                || type == KotlinToken.KW_INTERNAL
                || type == KotlinToken.KW_PROTECTED
                || type == KotlinToken.KW_ANNOTATION
                || type == KotlinToken.AT;
        }

        private int currentType()
        {
            if (pos >= tokens.size()) return KotlinToken.EOF;
            return tokens.get(pos).getType();
        }

        private String currentText()
        {
            if (pos >= tokens.size()) return "";
            return tokens.get(pos).getText();
        }

        /**
         * Check if a type name is a Kotlin primitive/built-in type
         * (which shouldn't be added to the 'used' list).
         */
        private static boolean isPrimitiveKotlinType(String name)
        {
            return switch (name)
            {
                case "Int", "Long", "Short", "Byte",
                     "Double", "Float",
                     "Boolean", "Char",
                     "String", "Unit", "Nothing", "Any" -> true;
                default -> false;
            };
        }
    }
}
