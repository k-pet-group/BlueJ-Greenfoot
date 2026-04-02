/*
 This file is part of the BlueJ program.
 Copyright (C) 2025,2026  Michael Kolling and John Rosenberg

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import bluej.parser.SourceLocation;
import bluej.parser.SourceSpan;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;

import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
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
import org.jetbrains.kotlin.psi.KtTypeParameterList;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.psi.KtUserType;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Parses Kotlin source files to extract class metadata ({@link ClassInfo})
 * for the class diagram. This is the Kotlin counterpart to
 * {@code bluej.parser.InfoParser} for Java, using PSI APIs from
 * kotlin-compiler-embeddable.
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
            return parse(reader, null, f.getName());
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
        return parse(r, targetPkg, null);
    }

    /**
     * Parse from a Reader with an explicit file name for top-level function
     * support. The file name is used to derive the ClassInfo name when the
     * file contains only top-level functions (no class/object declaration).
     *
     * @param r         reader over Kotlin source
     * @param targetPkg expected package name (for validation), may be null
     * @param fileName  original source file name (e.g., "Utils.kt"), may be null
     * @return ClassInfo with extracted metadata, or null if no declarations found
     */
    public static ClassInfo parse(Reader r, String targetPkg, String fileName)
    {
        // Read full source into String (PSI requires CharSequence)
        String source = KotlinParserUtils.readFully(r);
        if (source.isBlank())
        {
            return null;
        }

        // Build PSI tree (use real file name if available for ktFile.getName())
        KtFile ktFile;
        if (fileName != null)
        {
            ktFile = KotlinEnvironmentManager.getPsiFactory()
                .createFile(fileName, source);
        }
        else
        {
            ktFile = KotlinEnvironmentManager.getPsiFactory()
                .createFile(source);
        }

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
            // No class/object found — check for top-level functions
            List<KtNamedFunction> topLevelFunctions = new ArrayList<>();
            for (KtDeclaration decl : ktFile.getDeclarations())
            {
                if (decl instanceof KtNamedFunction fun)
                {
                    topLevelFunctions.add(fun);
                }
            }
            if (topLevelFunctions.isEmpty())
            {
                return null; // No class AND no functions — truly empty file
            }
            return buildTopLevelFunctionsInfo(
                topLevelFunctions, ktFile, packageName, targetPkg, source);
        }

        // Build ClassInfo from PSI node
        return buildClassInfo(classOrObject, ktFile, packageName, targetPkg, source);
    }

    /**
     * Build a {@link ClassInfo} from a PSI class/object declaration.
     */
    private static ClassInfo buildClassInfo(KtClassOrObject classOrObject,
            KtFile ktFile, String packageName, String targetPkg, String source)
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

        // --- Compute default insertion point for extends/implements ---
        // Position after class header (name, type params, primary constructor)
        // where ": SuperType" would be inserted if no supertypes exist.
        int insertOffset;
        KtPrimaryConstructor ctor = classOrObject.getPrimaryConstructor();
        if (ctor != null)
        {
            // After primary constructor: class Foo(val x: Int)↓
            insertOffset = ctor.getTextRange().getEndOffset();
        }
        else
        {
            KtTypeParameterList tpList = classOrObject.getTypeParameterList();
            if (tpList != null)
            {
                // After type parameters: class Foo<T>↓
                insertOffset = tpList.getTextRange().getEndOffset();
            }
            else
            {
                // After class name: class Foo↓
                PsiElement nameIdent = classOrObject.getNameIdentifier();
                insertOffset = (nameIdent != null)
                    ? nameIdent.getTextRange().getEndOffset()
                    : classOrObject.getTextRange().getStartOffset();
            }
        }
        Selection defaultInsertSel = pointSelection(insertOffset, source);
        info.setExtendsInsertSelection(defaultInsertSel);
        info.setImplementsInsertSelection(defaultInsertSel);

        // --- Supertype list (may update insert selections) ---
        extractSupertypes(classOrObject, info, source);

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

    /**
     * Build a {@link ClassInfo} for a Kotlin file that contains only
     * top-level functions (no class or object declaration). The synthesized
     * ClassInfo represents the facade class that kotlinc generates
     * (e.g., {@code Utils.kt} → {@code UtilsKt.class}).
     *
     * <p>The name is derived from the file stem (e.g., {@code "Utils"} from
     * {@code "Utils.kt"}). The {@code topLevelFunctionsOnly} flag is set to
     * {@code true} so that {@code ClassTarget} can assign a
     * {@code KotlinFileRole} and append the "Kt" suffix for compilation.</p>
     *
     * @param functions   the top-level function declarations found in the file
     * @param ktFile      the PSI file node
     * @param packageName the extracted package name
     * @param targetPkg   expected package name (for validation), may be null
     * @param source      the full source text (for offset→Selection conversion)
     * @return ClassInfo with name, used types, and topLevelFunctionsOnly flag
     */
    private static ClassInfo buildTopLevelFunctionsInfo(
            List<KtNamedFunction> functions,
            KtFile ktFile, String packageName, String targetPkg, String source)
    {
        ClassInfo info = new ClassInfo();

        // --- Name from file stem (e.g., "Utils" from "Utils.kt") ---
        String ktFileName = ktFile.getName();
        String stem = ktFileName.endsWith(".kt")
                ? ktFileName.substring(0, ktFileName.length() - 3)
                : ktFileName;
        info.setName(stem, true);

        // --- Mark as function-only file ---
        info.setTopLevelFunctionsOnly(true);

        // --- Not a class/interface/enum/abstract ---
        info.setInterface(false);
        info.setEnum(false);
        info.setAbstract(false);

        // --- Package handling ---
        if (!packageName.isEmpty())
        {
            info.setPackageSelections(
                new Selection(1, 1),   // package statement placeholder
                new Selection(1, 1),   // package name placeholder
                packageName,           // the actual package name text
                new Selection(1, 1)    // semi placeholder (Kotlin has no semicolons)
            );
        }

        // --- targetPkg validation ---
        if (targetPkg != null && !targetPkg.isEmpty()
            && !targetPkg.equals(packageName))
        {
            info.setParseError(true);
        }

        // --- Imports → used types ---
        extractImports(ktFile, info);

        // --- Extract dependencies and KDoc from each function ---
        for (KtNamedFunction fun : functions)
        {
            extractMethod(fun, info);
        }

        return info;
    }

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

    /**
     * Extract supertypes using PSI's structural distinction:
     * {@link KtSuperTypeCallEntry} (has constructor call → class) vs
     * other entries (no constructor → interface).
     *
     * <p>Also populates the {@link Selection} objects in {@link ClassInfo}
     * needed by {@code FlowEditor} for UI-driven inheritance arrow editing
     * (adding/removing extends/implements via the class diagram).</p>
     *
     * <p>Selection objects populated:</p>
     * <ul>
     *   <li>{@code extendsInsertSelection} → set to null (supertypes exist)</li>
     *   <li>{@code implementsInsertSelection} → after last supertype entry</li>
     *   <li>{@code superReplaceSelection} → span of superclass entry text</li>
     *   <li>{@code extendsReplaceSelection} → span from before {@code :} to
     *       start of superclass entry (the "keyword" part)</li>
     *   <li>{@code interfaceSelections} → list of keyword/comma and interface
     *       name selections for interface editing</li>
     * </ul>
     */
    private static void extractSupertypes(KtClassOrObject classOrObject,
            ClassInfo info, String source)
    {
        List<KtSuperTypeListEntry> entries = classOrObject.getSuperTypeListEntries();

        if (entries.isEmpty())
        {
            return; // insert selections already set to defaults in buildClassInfo
        }

        boolean foundSuperclass = false;

        // Find the colon token `:` (direct child of the class declaration)
        PsiElement colon = findChildByText(classOrObject, ":");

        // Since supertypes exist, null out extendsInsertSelection
        info.setExtendsInsertSelection(null);

        // Build interface selections list (parallels Java's interfaceSelections)
        // Structure: [keyword_sel, iface1_sel, comma_sel, iface2_sel, ...]
        // keyword_sel is ":" (no superclass) or "," (comma after superclass)
        List<Selection> ifaceSelections = null;
        Selection lastCommaSelection = null;

        for (KtSuperTypeListEntry entry : entries)
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

            TextRange entryRange = entry.getTextRange();

            if (entry instanceof KtSuperTypeCallEntry)
            {
                // Constructor call → class supertype: `: Base(...)`
                if (!foundSuperclass)
                {
                    info.setSuperclass(typeName);
                    foundSuperclass = true;

                    // superReplaceSelection = span of this entry (e.g., "Bar()")
                    info.setSuperReplaceSelection(
                        offsetToSelection(entryRange.getStartOffset(),
                                           entryRange.getEndOffset(), source));

                    // extendsReplaceSelection = span from before ":" to start
                    // of the superclass entry (covers " : " keyword part)
                    if (colon != null)
                    {
                        int replaceStart = colon.getTextRange().getStartOffset();
                        // Include preceding whitespace if present
                        if (replaceStart > 0
                            && source.charAt(replaceStart - 1) == ' ')
                        {
                            replaceStart--;
                        }
                        info.setExtendsReplaceSelection(
                            offsetToSelection(replaceStart,
                                               entryRange.getStartOffset(), source));
                    }

                    // Update implementsInsertSelection to after this entry
                    info.setImplementsInsertSelection(
                        pointSelection(entryRange.getEndOffset(), source));
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

                if (ifaceSelections == null)
                {
                    // First interface encountered — initialize the list
                    ifaceSelections = new ArrayList<>();
                    lastCommaSelection = null; // Don't carry over from superclass

                    // Add the "keyword" selection at index 0:
                    // - If superclass precedes: comma between superclass and here
                    // - If no superclass: the colon token
                    if (foundSuperclass)
                    {
                        PsiElement comma = findPrecedingComma(entry);
                        if (comma != null)
                        {
                            ifaceSelections.add(offsetToSelection(
                                comma.getTextRange().getStartOffset(),
                                comma.getTextRange().getEndOffset(), source));
                        }
                    }
                    else if (colon != null)
                    {
                        ifaceSelections.add(offsetToSelection(
                            colon.getTextRange().getStartOffset(),
                            colon.getTextRange().getEndOffset(), source));
                    }
                }

                Selection ifaceSel = offsetToSelection(
                    entryRange.getStartOffset(),
                    entryRange.getEndOffset(), source);

                if (lastCommaSelection != null)
                {
                    // Extend comma selection to reach start of this interface
                    // (matches Java InfoParser's pattern: comma extended to
                    // include whitespace up to the next interface name)
                    lastCommaSelection.extendEnd(
                        ifaceSel.getLine(), ifaceSel.getColumn());
                    ifaceSelections.add(lastCommaSelection);
                    lastCommaSelection = null;
                }

                ifaceSelections.add(ifaceSel);
            }

            // Look for trailing comma after this entry
            PsiElement comma = findFollowingComma(entry);
            if (comma != null)
            {
                lastCommaSelection = offsetToSelection(
                    comma.getTextRange().getStartOffset(),
                    comma.getTextRange().getEndOffset(), source);
            }
            else
            {
                lastCommaSelection = null;
            }
        }

        // Finalize interface selections (need at least keyword + one interface)
        if (ifaceSelections != null && ifaceSelections.size() > 1)
        {
            info.setInterfaceSelections(ifaceSelections);
        }

        // Update implementsInsertSelection to after the very last entry
        KtSuperTypeListEntry lastEntry = entries.get(entries.size() - 1);
        info.setImplementsInsertSelection(
            pointSelection(lastEntry.getTextRange().getEndOffset(), source));
    }

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

    /**
     * Find a direct child PsiElement whose text equals the given string.
     *
     * @return the first matching child, or null if not found
     */
    private static PsiElement findChildByText(PsiElement parent, String text)
    {
        for (PsiElement child = parent.getFirstChild(); child != null;
             child = child.getNextSibling())
        {
            if (text.equals(child.getText()))
            {
                return child;
            }
        }
        return null;
    }

    /**
     * Find a COMMA PsiElement that precedes the given element
     * (skipping whitespace).
     */
    private static PsiElement findPrecedingComma(PsiElement element)
    {
        PsiElement prev = element.getPrevSibling();
        while (prev != null)
        {
            String text = prev.getText();
            if (",".equals(text))
            {
                return prev;
            }
            if (text.trim().isEmpty())
            {
                prev = prev.getPrevSibling();
                continue;
            }
            break; // hit a non-whitespace, non-comma element
        }
        return null;
    }

    /**
     * Find a COMMA PsiElement that follows the given element
     * (skipping whitespace).
     */
    private static PsiElement findFollowingComma(PsiElement element)
    {
        PsiElement next = element.getNextSibling();
        while (next != null)
        {
            String text = next.getText();
            if (",".equals(text))
            {
                return next;
            }
            if (text.trim().isEmpty())
            {
                next = next.getNextSibling();
                continue;
            }
            break;
        }
        return null;
    }

    /**
     * Convert a 0-based absolute offset in the source string to a
     * 1-based line number.
     */
    private static int getLine(int offset, String source)
    {
        int line = 1;
        for (int i = 0; i < offset && i < source.length(); i++)
        {
            if (source.charAt(i) == '\n')
            {
                line++;
            }
        }
        return line;
    }

    /**
     * Convert a 0-based absolute offset in the source string to a
     * 1-based column number.
     */
    private static int getCol(int offset, String source)
    {
        if (offset == 0)
        {
            return 1;
        }
        int col = 1;
        for (int i = offset - 1; i >= 0; i--)
        {
            if (source.charAt(i) == '\n')
            {
                break;
            }
            col++;
        }
        return col;
    }

    /**
     * Create a {@link Selection} spanning from startOffset to endOffset
     * in the source. PSI offsets are 0-based; Selection uses 1-based
     * line/column.
     */
    private static Selection offsetToSelection(int startOffset, int endOffset,
            String source)
    {
        return new Selection(new SourceSpan(
            new SourceLocation(getLine(startOffset, source),
                               getCol(startOffset, source)),
            new SourceLocation(getLine(endOffset, source),
                               getCol(endOffset, source))));
    }

    /**
     * Create a zero-width {@link Selection} at the given offset
     * (an insertion point).
     */
    private static Selection pointSelection(int offset, String source)
    {
        return new Selection(getLine(offset, source), getCol(offset, source));
    }
}
