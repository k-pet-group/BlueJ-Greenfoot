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

import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.StringJoiner;

import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassBody;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtNullableType;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtPrimaryConstructor;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry;
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry;
import org.jetbrains.kotlin.psi.KtTypeElement;
import org.jetbrains.kotlin.psi.KtTypeParameter;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.psi.KtUserType;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Parses Kotlin source files to extract class metadata ({@link ClassInfo})
 * for the class diagram. Uses PSI-based extraction via
 * {@link KotlinEnvironmentManager#getPsiFactory()} to build a full parse tree
 * and extract metadata directly from PSI nodes.
 *
 * <p>This is the Kotlin parallel to {@code bluej.parser.InfoParser} for Java.
 * Unlike the Java InfoParser (which uses a full recursive-descent parser with
 * callbacks), this implementation uses PSI APIs from
 * {@code kotlin-compiler-embeddable} for reliable structural extraction.</p>
 *
 * <h3>Parsing strategy</h3>
 * <ol>
 *   <li>{@code KtPsiFactory.createFile(source)} builds a PSI tree</li>
 *   <li>{@code KtFile.getPackageFqName()} extracts the package name</li>
 *   <li>{@code KtFile.getImportDirectives()} extracts imports for the "used" list</li>
 *   <li>Find the first {@code KtClassOrObject} in top-level declarations</li>
 *   <li>Extract modifiers, supertypes, type parameters, constructor params,
 *       and body members via PSI methods</li>
 *   <li>Populate and return {@link ClassInfo}</li>
 * </ol>
 *
 * <h3>Supertype disambiguation</h3>
 * <p>PSI structurally distinguishes class supertypes ({@code KtSuperTypeCallEntry}
 * — has constructor call) from interface supertypes ({@code KtSuperTypeEntry}
 * — no constructor call). This is more reliable than the previous
 * parentheses-heuristic approach.</p>
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
        // Read full source into String (PSI requires CharSequence)
        String source = readFully(r);
        if (source == null || source.isBlank())
        {
            return null;
        }

        // Build PSI tree
        KtFile ktFile = KotlinEnvironmentManager.getPsiFactory().createFile(source);

        // Extract package name
        FqName packageFqName = ktFile.getPackageFqName();
        String packageName = packageFqName.isRoot() ? "" : packageFqName.asString();

        // Find the first top-level KtClassOrObject declaration
        KtClassOrObject classOrObject = null;
        for (KtDeclaration decl : ktFile.getDeclarations())
        {
            if (decl instanceof KtClassOrObject co)
            {
                classOrObject = co;
                break;
            }
        }

        if (classOrObject == null)
        {
            return null;
        }

        // Build ClassInfo from PSI node
        return buildClassInfo(classOrObject, ktFile, packageName, targetPkg);
    }

    // -----------------------------------------------------------------------
    // Core PSI extraction
    // -----------------------------------------------------------------------

    /**
     * Build a {@link ClassInfo} from a PSI class/object declaration.
     */
    private static ClassInfo buildClassInfo(KtClassOrObject classOrObject,
            KtFile ktFile, String packageName, String targetPkg)
    {
        ClassInfo info = new ClassInfo();

        // --- Class identity ---
        String name = classOrObject.getName();
        if (name == null)
        {
            return null;
        }

        boolean isPublic = !classOrObject.hasModifier(KtTokens.PRIVATE_KEYWORD)
            && !classOrObject.hasModifier(KtTokens.INTERNAL_KEYWORD)
            && !classOrObject.hasModifier(KtTokens.PROTECTED_KEYWORD);
        info.setName(name, isPublic);

        // --- Modifiers ---
        if (classOrObject instanceof KtClass ktClass)
        {
            info.setInterface(ktClass.isInterface());
            info.setEnum(ktClass.isEnum());
            info.setAbstract(
                ktClass.hasModifier(KtTokens.ABSTRACT_KEYWORD)
                || ktClass.isSealed()
                || ktClass.isInterface());
        }
        else
        {
            // KtObjectDeclaration — not abstract, not interface, not enum
            info.setAbstract(false);
            info.setInterface(false);
            info.setEnum(false);
        }

        // --- Package name (fixes gap: package not stored) ---
        if (!packageName.isEmpty())
        {
            info.setPackageSelections(
                new Selection(1, 1),   // package statement placeholder
                new Selection(1, 1),   // package name placeholder
                packageName,           // the actual package name text
                new Selection(1, 1)    // semi placeholder (Kotlin has no semicolons)
            );
        }

        // --- targetPkg validation (fixes gap: targetPkg unused) ---
        if (targetPkg != null && !targetPkg.isEmpty()
            && !targetPkg.equals(packageName))
        {
            info.setParseError(true);
        }

        // --- Imports → used types (fixes gap: imports not tracked) ---
        extractImports(ktFile, info);

        // --- Type parameters ---
        extractTypeParameters(classOrObject, info);

        // --- Primary constructor parameter types → used list ---
        extractPrimaryConstructor(classOrObject, info);

        // --- Supertype list ---
        extractSupertypes(classOrObject, info);

        // --- Class body: methods and properties (fixes gap: no body parsing) ---
        extractClassBody(classOrObject, info);

        // --- KDoc comment on the class itself ---
        String classKDoc = extractKDoc(classOrObject);
        if (classKDoc != null)
        {
            info.addComment(name, classKDoc, null);
        }

        return info;
    }

    // -----------------------------------------------------------------------
    // Import extraction
    // -----------------------------------------------------------------------

    /**
     * Extract imported types and add non-primitive ones to the "used" list.
     */
    private static void extractImports(KtFile ktFile, ClassInfo info)
    {
        for (KtImportDirective imp : ktFile.getImportDirectives())
        {
            FqName importedFqName = imp.getImportedFqName();
            if (importedFqName != null)
            {
                String simpleName = importedFqName.shortName().asString();
                if (!isPrimitiveKotlinType(simpleName))
                {
                    info.addUsed(simpleName);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Type parameter extraction
    // -----------------------------------------------------------------------

    /**
     * Extract type parameters (e.g., {@code <T>}, {@code <T : Comparable<T>>}).
     */
    private static void extractTypeParameters(KtClassOrObject classOrObject, ClassInfo info)
    {
        List<KtTypeParameter> typeParams = classOrObject.getTypeParameters();
        for (KtTypeParameter tp : typeParams)
        {
            String tpText = tp.getText();
            if (tpText != null && !tpText.isEmpty())
            {
                info.addTypeParameterText(tpText);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Primary constructor extraction
    // -----------------------------------------------------------------------

    /**
     * Extract parameter types from the primary constructor and add
     * non-primitive ones to the "used" list.
     */
    private static void extractPrimaryConstructor(KtClassOrObject classOrObject, ClassInfo info)
    {
        KtPrimaryConstructor ctor = classOrObject.getPrimaryConstructor();
        if (ctor == null)
        {
            return;
        }

        for (KtParameter param : ctor.getValueParameters())
        {
            KtTypeReference typeRef = param.getTypeReference();
            if (typeRef != null)
            {
                String typeName = extractSimpleName(typeRef);
                if (typeName != null && !isPrimitiveKotlinType(typeName))
                {
                    info.addUsed(typeName);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Supertype extraction
    // -----------------------------------------------------------------------

    /**
     * Extract supertypes using PSI's structural distinction:
     * {@link KtSuperTypeCallEntry} (has constructor call → class) vs
     * other entries (no constructor → interface).
     */
    private static void extractSupertypes(KtClassOrObject classOrObject, ClassInfo info)
    {
        boolean foundSuperclass = false;

        for (KtSuperTypeListEntry entry : classOrObject.getSuperTypeListEntries())
        {
            KtTypeReference typeRef = entry.getTypeReference();
            if (typeRef == null)
            {
                continue;
            }

            String typeName = extractSimpleName(typeRef);
            if (typeName == null)
            {
                continue;
            }

            if (entry instanceof KtSuperTypeCallEntry)
            {
                // Constructor call → class supertype: `: Base(...)`
                if (!foundSuperclass)
                {
                    info.setSuperclass(typeName);
                    foundSuperclass = true;
                }
                else
                {
                    // Multiple constructor-call supertypes (unusual but possible)
                    info.addUsed(typeName);
                }
            }
            else
            {
                // Plain or delegated → interface: `: Iface` or `: Iface by impl`
                info.addImplements(typeName);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Class body extraction
    // -----------------------------------------------------------------------

    /**
     * Extract methods and properties from the class body.
     * Adds their types to the "used" list and their KDoc comments
     * to the comments list.
     */
    private static void extractClassBody(KtClassOrObject classOrObject, ClassInfo info)
    {
        KtClassBody body = classOrObject.getBody();
        if (body == null)
        {
            return;
        }

        for (KtDeclaration member : body.getDeclarations())
        {
            if (member instanceof KtNamedFunction fun)
            {
                extractMethod(fun, info);
            }
            else if (member instanceof KtProperty prop)
            {
                extractProperty(prop, info);
            }
        }
    }

    /**
     * Extract a method's parameter types, return type, and KDoc comment.
     */
    private static void extractMethod(KtNamedFunction fun, ClassInfo info)
    {
        String methodName = fun.getName();
        if (methodName == null)
        {
            return;
        }

        // Collect parameter types for "used" list
        for (KtParameter param : fun.getValueParameters())
        {
            KtTypeReference typeRef = param.getTypeReference();
            if (typeRef != null)
            {
                String typeName = extractSimpleName(typeRef);
                if (typeName != null && !isPrimitiveKotlinType(typeName))
                {
                    info.addUsed(typeName);
                }
            }
        }

        // Collect return type for "used" list
        KtTypeReference returnTypeRef = fun.getTypeReference();
        if (returnTypeRef != null)
        {
            String returnType = extractSimpleName(returnTypeRef);
            if (returnType != null && !isPrimitiveKotlinType(returnType))
            {
                info.addUsed(returnType);
            }
        }

        // Store KDoc comment if present
        String docComment = extractKDoc(fun);
        if (docComment != null)
        {
            String target = buildMethodTarget(fun);
            String paramNames = buildParamNames(fun);
            info.addComment(target, docComment, paramNames);
        }
    }

    /**
     * Extract a property's type and add it to the "used" list.
     */
    private static void extractProperty(KtProperty prop, ClassInfo info)
    {
        KtTypeReference typeRef = prop.getTypeReference();
        if (typeRef != null)
        {
            String typeName = extractSimpleName(typeRef);
            if (typeName != null && !isPrimitiveKotlinType(typeName))
            {
                info.addUsed(typeName);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------

    /**
     * Extract the simple (unqualified) type name from a type reference.
     * Handles user types ({@code Foo}), qualified types ({@code com.example.Foo}),
     * nullable types ({@code Foo?}), and generic types ({@code List<String>}).
     *
     * @return the simple type name, or null if it cannot be determined
     */
    private static String extractSimpleName(KtTypeReference typeRef)
    {
        KtTypeElement typeElement = typeRef.getTypeElement();
        if (typeElement instanceof KtUserType userType)
        {
            return extractNameFromUserType(userType);
        }
        if (typeElement instanceof KtNullableType nullable)
        {
            KtTypeElement inner = nullable.getInnerType();
            if (inner instanceof KtUserType userType)
            {
                return extractNameFromUserType(userType);
            }
        }
        // Fallback: parse from text
        return extractNameFromText(typeRef.getText());
    }

    /**
     * Extract the simple name from a {@link KtUserType}, handling qualified
     * names like {@code com.example.Foo} by returning just {@code "Foo"}.
     */
    private static String extractNameFromUserType(KtUserType userType)
    {
        // getReferencedName() returns the simple (last segment) name
        // For "com.example.Foo", the KtUserType tree is nested:
        //   KtUserType("Foo") with qualifier KtUserType("example") with qualifier KtUserType("com")
        // So getReferencedName() on the outermost already gives us "Foo"
        String name = userType.getReferencedName();
        return name;
    }

    /**
     * Fallback: extract simple name from raw type text.
     * Strips nullable markers, generic parameters, and qualifiers.
     */
    private static String extractNameFromText(String text)
    {
        if (text == null || text.isEmpty())
        {
            return null;
        }
        // Remove nullable marker
        if (text.endsWith("?"))
        {
            text = text.substring(0, text.length() - 1);
        }
        // Remove generic parameters
        int lt = text.indexOf('<');
        if (lt > 0)
        {
            text = text.substring(0, lt);
        }
        // Get simple name (last segment of qualified name)
        int dot = text.lastIndexOf('.');
        if (dot >= 0)
        {
            text = text.substring(dot + 1);
        }
        return text.trim().isEmpty() ? null : text.trim();
    }

    /**
     * Extract KDoc comment text from a declaration.
     * Returns the raw KDoc text (including delimiters), or null if absent.
     */
    private static String extractKDoc(PsiElement decl)
    {
        // KDoc appears as a direct child PsiElement of the declaration
        for (PsiElement child = decl.getFirstChild(); child != null;
             child = child.getNextSibling())
        {
            if (child instanceof KDoc)
            {
                return child.getText();
            }
        }
        return null;
    }

    /**
     * Build the method target string for {@link ClassInfo#addComment}.
     * Format: {@code "ReturnType methodName(ParamType1,ParamType2)"}
     */
    private static String buildMethodTarget(KtNamedFunction fun)
    {
        StringBuilder target = new StringBuilder();

        // Return type (or "Unit" if not specified)
        KtTypeReference returnTypeRef = fun.getTypeReference();
        if (returnTypeRef != null)
        {
            target.append(returnTypeRef.getText());
        }
        else
        {
            target.append("Unit");
        }

        target.append(' ');
        target.append(fun.getName());
        target.append('(');

        // Parameter types
        StringJoiner params = new StringJoiner(",");
        for (KtParameter param : fun.getValueParameters())
        {
            KtTypeReference paramType = param.getTypeReference();
            if (paramType != null)
            {
                params.add(paramType.getText());
            }
            else
            {
                params.add("Any");
            }
        }
        target.append(params);
        target.append(')');

        return target.toString();
    }

    /**
     * Build a space-separated list of parameter names for
     * {@link ClassInfo#addComment}.
     *
     * @return parameter names string, or null if no parameters
     */
    private static String buildParamNames(KtNamedFunction fun)
    {
        List<KtParameter> params = fun.getValueParameters();
        if (params.isEmpty())
        {
            return null;
        }

        StringJoiner names = new StringJoiner(" ");
        for (KtParameter param : params)
        {
            String name = param.getName();
            if (name != null)
            {
                names.add(name);
            }
        }
        String result = names.toString();
        return result.isEmpty() ? null : result;
    }

    /**
     * Read all content from a Reader into a String.
     *
     * @return the full content, or null on I/O error
     */
    private static String readFully(Reader r)
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) != -1)
            {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        }
        catch (IOException e)
        {
            return null;
        }
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
