/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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

import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;

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
import org.jetbrains.kotlin.psi.KtSecondaryConstructor;
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
 * for the class diagram. This is the Kotlin counterpart to
 * {@code bluej.parser.InfoParser} for Java, using PSI APIs from
 * kotlin-compiler-embeddable.
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
        try (FileReader reader = new FileReader(f)) {
            return parse(reader, null, f.getName());
        }
        catch (FileNotFoundException e) {
            throw e;
        }
        catch (Exception e) {
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
        String source = KotlinParserUtils.readFully(r);
        if (source.isBlank()) {
            return null;
        }

        KtFile ktFile;
        if (fileName != null) {
            ktFile = KotlinEnvironmentManager.getPsiFactory()
                .createFile(fileName, source);
        } else {
            ktFile = KotlinEnvironmentManager.getPsiFactory()
                .createFile(source);
        }

        FqName packageFqName = ktFile.getPackageFqName();
        String packageName = packageFqName.isRoot() ? "" : packageFqName.asString();
        Selection[] pkgSelections = KotlinParserUtils.packageSelections(ktFile, source);

        KtClassOrObject classOrObject = null;
        for (KtDeclaration decl : ktFile.getDeclarations()) {
            if (decl instanceof KtClassOrObject co) {
                classOrObject = co;
                break;
            }
        }

        if (classOrObject == null) {
            List<KtNamedFunction> topLevelFunctions = new ArrayList<>();
            for (KtDeclaration decl : ktFile.getDeclarations()) {
                if (decl instanceof KtNamedFunction fun) {
                    topLevelFunctions.add(fun);
                }
            }
            if (topLevelFunctions.isEmpty()) {
                return null; // No class AND no functions — truly empty file
            }
            return buildTopLevelFunctionsInfo(
                topLevelFunctions, ktFile, packageName, pkgSelections, targetPkg);
        }

        // Mixed content (class + functions/properties) — return null to prevent file rename
        for (KtDeclaration decl : ktFile.getDeclarations()) {
            if (decl instanceof KtNamedFunction || decl instanceof KtProperty) {
                return null;
            }
        }

        return buildClassInfo(classOrObject, ktFile, packageName, pkgSelections, targetPkg);
    }

    /**
     * Build a {@link ClassInfo} from a PSI class/object declaration.
     */
    private static ClassInfo buildClassInfo(KtClassOrObject classOrObject,
            KtFile ktFile, String packageName, Selection[] pkgSelections,
            String targetPkg)
    {
        ClassInfo info = new ClassInfo();

        String name = classOrObject.getName();
        if (name == null) {
            return null;
        }

        boolean isPublic = !classOrObject.hasModifier(KtTokens.PRIVATE_KEYWORD)
            && !classOrObject.hasModifier(KtTokens.INTERNAL_KEYWORD)
            && !classOrObject.hasModifier(KtTokens.PROTECTED_KEYWORD);
        info.setName(name, isPublic);

        if (classOrObject instanceof KtClass ktClass) {
            info.setInterface(ktClass.isInterface());
            info.setEnum(ktClass.isEnum());
            info.setAbstract(
                ktClass.hasModifier(KtTokens.ABSTRACT_KEYWORD)
                || ktClass.isSealed()
                || ktClass.isInterface());
        } else {
            // KtObjectDeclaration -- not abstract, not interface, not enum
            info.setAbstract(false);
            info.setInterface(false);
            info.setEnum(false);
        }

        if (pkgSelections != null) {
            info.setPackageSelections(
                pkgSelections[0],     // package keyword span
                pkgSelections[1],     // package name span
                packageName,          // the actual package name text
                pkgSelections[2]      // zero-length post-name marker (no semicolons in Kotlin)
            );
        }

        if (targetPkg != null && !targetPkg.isEmpty()
            && !targetPkg.equals(packageName)) {
            info.setParseError(true);
        }

        extractImports(ktFile, info);

        extractTypeParameters(classOrObject, info);

        extractPrimaryConstructor(classOrObject, info, ktFile);

        extractSecondaryConstructors(classOrObject, info, ktFile);

        extractSupertypes(classOrObject, info);

        extractClassBody(classOrObject, info, ktFile);

        String classKDoc = extractKDoc(classOrObject);
        if (classKDoc != null) {
            info.addComment(name, classKDoc, null);
        }

        return info;
    }

    /**
     * Build a ClassInfo for a file containing only top-level functions.
     * The name is derived from the file stem (e.g., Utils from Utils.kt).
     */
    private static ClassInfo buildTopLevelFunctionsInfo(
            List<KtNamedFunction> functions,
            KtFile ktFile, String packageName, Selection[] pkgSelections,
            String targetPkg)
    {
        ClassInfo info = new ClassInfo();

        String ktFileName = ktFile.getName();
        String stem = ktFileName.toLowerCase().endsWith(".kt")
                ? ktFileName.substring(0, ktFileName.length() - 3)
                : ktFileName;
        info.setName(stem, true);

        info.setTopLevelFunctionsOnly(true);

        info.setInterface(false);
        info.setEnum(false);
        info.setAbstract(false);

        if (pkgSelections != null) {
            info.setPackageSelections(
                pkgSelections[0],     // package keyword span
                pkgSelections[1],     // package name span
                packageName,          // the actual package name text
                pkgSelections[2]      // zero-length post-name marker (no semicolons in Kotlin)
            );
        }

        if (targetPkg != null && !targetPkg.isEmpty()
            && !targetPkg.equals(packageName)) {
            info.setParseError(true);
        }

        extractImports(ktFile, info);

        for (KtNamedFunction fun : functions) {
            extractMethod(fun, info, ktFile);
        }

        return info;
    }

    /**
     * Extract imported types and add non-primitive ones to the "used" list.
     */
    private static void extractImports(KtFile ktFile, ClassInfo info)
    {
        for (KtImportDirective imp : ktFile.getImportDirectives()) {
            FqName importedFqName = imp.getImportedFqName();
            if (importedFqName != null) {
                String simpleName = importedFqName.shortName().asString();
                if (!isPrimitiveKotlinType(simpleName)) {
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
        for (KtTypeParameter tp : typeParams) {
            String tpText = tp.getText();
            if (tpText != null && !tpText.isEmpty()) {
                info.addTypeParameterText(tpText);
            }
        }
    }

    /**
     * Extract the primary constructor: add its parameter types to the "used"
     * list and emit a comment carrying the parameter names so the right-click
     * "new ..." menu can show them.
     */
    private static void extractPrimaryConstructor(KtClassOrObject classOrObject,
            ClassInfo info, KtFile ktFile)
    {
        KtPrimaryConstructor ctor = classOrObject.getPrimaryConstructor();
        if (ctor == null) {
            return;
        }

        String className = classOrObject.getName();
        if (className == null) {
            return;
        }

        List<KtParameter> params = ctor.getValueParameters();
        addUsedTypes(params, info);

        String target = buildConstructorTarget(className, params, ktFile);
        info.addComment(target, extractKDoc(ctor), buildParamNames(params));
    }

    /**
     * Extract each secondary constructor: add parameter types to the "used"
     * list and emit a comment carrying the parameter names. Overloaded
     * constructors produce distinct targets via their (resolved) parameter types.
     */
    private static void extractSecondaryConstructors(KtClassOrObject classOrObject,
            ClassInfo info, KtFile ktFile)
    {
        String className = classOrObject.getName();
        if (className == null) {
            return;
        }

        for (KtSecondaryConstructor ctor : classOrObject.getSecondaryConstructors()) {
            List<KtParameter> params = ctor.getValueParameters();
            addUsedTypes(params, info);

            String target = buildConstructorTarget(className, params, ktFile);
            info.addComment(target, extractKDoc(ctor), buildParamNames(params));
        }
    }

    /**
     * Add the non-primitive parameter types of a parameter list to the
     * "used" (dependency) list.
     */
    private static void addUsedTypes(List<KtParameter> params, ClassInfo info)
    {
        for (KtParameter param : params) {
            KtTypeReference typeRef = param.getTypeReference();
            if (typeRef != null) {
                String typeName = extractSimpleName(typeRef);
                if (typeName != null && !isPrimitiveKotlinType(typeName)) {
                    info.addUsed(typeName);
                }
            }
        }
    }

    /**
     * Extract supertype names from the class declaration.
     * {@link bluej.editor.flow.KotlinLanguageSupport} handles all editing
     * operations via PSI directly, so only type names are needed here —
     * no Selection objects.
     */
    private static void extractSupertypes(KtClassOrObject classOrObject,
            ClassInfo info)
    {
        for (KtSuperTypeListEntry entry : classOrObject.getSuperTypeListEntries()) {
            KtTypeReference typeRef = entry.getTypeReference();
            if (typeRef == null) {
                continue;
            }

            String typeName = extractSimpleName(typeRef);
            if (typeName == null) {
                continue;
            }

            if (entry instanceof KtSuperTypeCallEntry) {
                // Constructor call → superclass: `: Base(...)`
                info.setSuperclass(typeName);
            } else {
                // Plain or delegated entry → interface: `: Interface`
                info.addImplements(typeName);
            }
        }
    }

    /**
     * Extract methods and properties from the class body.
     * Adds their types to the "used" list and their KDoc comments
     * to the comments list.
     */
    private static void extractClassBody(KtClassOrObject classOrObject, ClassInfo info,
            KtFile ktFile)
    {
        KtClassBody body = classOrObject.getBody();
        if (body == null) {
            return;
        }

        for (KtDeclaration member : body.getDeclarations()) {
            if (member instanceof KtNamedFunction fun) {
                extractMethod(fun, info, ktFile);
            } else if (member instanceof KtProperty prop) {
                extractProperty(prop, info);
            } else if (member instanceof KtObjectDeclaration obj && obj.isCompanion()) {
                // Companion methods are surfaced as static-style operations on the
                // enclosing class (View.addCompanionMethods), so emit their comments
                // too — otherwise the call-method menu shows no parameter names.
                extractClassBody(obj, info, ktFile);
            }
        }
    }

    /**
     * Extract a method's parameter types, return type, and parameter names.
     * A comment is emitted for every method (so the right-click "call method"
     * menu can show parameter names), mirroring Java's {@code InfoParser};
     * KDoc text is attached only when present.
     */
    private static void extractMethod(KtNamedFunction fun, ClassInfo info, KtFile ktFile)
    {
        String methodName = fun.getName();
        if (methodName == null) {
            return;
        }

        addUsedTypes(fun.getValueParameters(), info);

        KtTypeReference returnTypeRef = fun.getTypeReference();
        if (returnTypeRef != null) {
            String returnType = extractSimpleName(returnTypeRef);
            if (returnType != null && !isPrimitiveKotlinType(returnType)) {
                info.addUsed(returnType);
            }
        }

        String target = buildMethodTarget(fun, ktFile);
        info.addComment(target, extractKDoc(fun), buildParamNames(fun.getValueParameters()));
    }

    /**
     * Extract a property's type and add it to the "used" list.
     */
    private static void extractProperty(KtProperty prop, ClassInfo info)
    {
        KtTypeReference typeRef = prop.getTypeReference();
        if (typeRef != null) {
            String typeName = extractSimpleName(typeRef);
            if (typeName != null && !isPrimitiveKotlinType(typeName)) {
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
        if (typeElement instanceof KtUserType userType) {
            return extractNameFromUserType(userType);
        }
        if (typeElement instanceof KtNullableType nullable) {
            KtTypeElement inner = nullable.getInnerType();
            if (inner instanceof KtUserType userType) {
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
        if (text == null || text.isEmpty()) {
            return null;
        }
        if (text.endsWith("?")) {
            text = text.substring(0, text.length() - 1);
        }
        int lt = text.indexOf('<');
        if (lt > 0) {
            text = text.substring(0, lt);
        }
        int dot = text.lastIndexOf('.');
        if (dot >= 0) {
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
             child = child.getNextSibling()) {
            if (child instanceof KDoc) {
                return child.getText();
            }
        }
        return null;
    }

    /**
     * Build the method target string for {@link ClassInfo#addComment}. The
     * format must match {@code JavaUtils.getSignature(Method)} of the compiled
     * method exactly, since comments are attached to members by exact string
     * equality: {@code "<erased-FQ-return> name(<erased-FQ-type>, ...)"}, e.g.
     * {@code "void setX(int)"}.
     */
    private static String buildMethodTarget(KtNamedFunction fun, KtFile ktFile)
    {
        StringBuilder target = new StringBuilder();
        target.append(jvmReturnTypeName(fun.getTypeReference(), ktFile));
        target.append(' ');
        target.append(fun.getName());
        target.append('(');
        target.append(joinParamTypes(fun.getValueParameters(), ktFile));
        target.append(')');
        return target.toString();
    }

    /**
     * Build a constructor target string for {@link ClassInfo#addComment}. Must
     * match {@code JavaUtils.getSignature(Constructor)} of the compiled
     * constructor: {@code "SimpleClassName(<erased-FQ-type>, ...)"} (no return
     * type), e.g. {@code "Flight(java.lang.String, java.lang.String)"}.
     */
    private static String buildConstructorTarget(String className,
            List<KtParameter> params, KtFile ktFile)
    {
        return className + "(" + joinParamTypes(params, ktFile) + ")";
    }

    /**
     * Join the erased-FQ JVM type names of a parameter list with ", "
     * (the separator {@code JavaUtils.makeSignature} uses).
     */
    private static String joinParamTypes(List<KtParameter> params, KtFile ktFile)
    {
        StringJoiner types = new StringJoiner(", ");
        for (KtParameter param : params) {
            types.add(jvmTypeName(param.getTypeReference(), ktFile));
        }
        return types.toString();
    }

    /**
     * Build a space-separated list of parameter names for
     * {@link ClassInfo#addComment}.
     *
     * @return parameter names string, or null if there are no parameters
     */
    private static String buildParamNames(List<KtParameter> params)
    {
        if (params.isEmpty()) {
            return null;
        }

        StringJoiner names = new StringJoiner(" ");
        for (KtParameter param : params) {
            String name = param.getName();
            if (name != null) {
                names.add(name);
            }
        }
        String result = names.toString();
        return result.isEmpty() ? null : result;
    }

    /**
     * Map a method return type reference to its erased JVM name. A missing or
     * (non-nullable) {@code Unit} return type maps to {@code "void"}; everything
     * else follows {@link #jvmTypeName}.
     */
    private static String jvmReturnTypeName(KtTypeReference typeRef, KtFile ktFile)
    {
        if (typeRef == null) {
            // Expression-body function with an inferred return type: single-file PSI
            // cannot resolve it, so we guess "void". View.loadClassComments matches
            // such targets return-type-agnostically. TODO once the Kotlin Analysis API
            // is integrated, resolve the real inferred type and drop that fallback.
            return "void";
        }
        if (!(typeRef.getTypeElement() instanceof KtNullableType)
            && "Unit".equals(extractSimpleName(typeRef))) {
            return "void";
        }
        return jvmTypeName(typeRef, ktFile);
    }

    /**
     * Map a Kotlin type reference to the erased, fully-qualified JVM type name
     * that reflection would report for the compiled member. Kotlin built-ins
     * use a fixed table (nullable built-ins box to their wrapper); user types
     * are resolved from the single file via {@link #resolveUserTypeFqn}.
     */
    private static String jvmTypeName(KtTypeReference typeRef, KtFile ktFile)
    {
        if (typeRef == null) {
            return "java.lang.Object";
        }
        String simple = extractSimpleName(typeRef);
        if (simple == null) {
            return "java.lang.Object";
        }
        boolean nullable = typeRef.getTypeElement() instanceof KtNullableType;
        String builtin = jvmBuiltinType(simple, nullable);
        if (builtin != null) {
            return builtin;
        }
        return resolveUserTypeFqn(simple, typeRef, ktFile);
    }

    /**
     * Map a Kotlin built-in type to its JVM name, or null if not a built-in.
     * Nullable primitives box to their wrapper (e.g. {@code Int? -> Integer}).
     */
    private static String jvmBuiltinType(String simpleName, boolean nullable)
    {
        return switch (simpleName) {
            case "Int" -> nullable ? "java.lang.Integer" : "int";
            case "Long" -> nullable ? "java.lang.Long" : "long";
            case "Short" -> nullable ? "java.lang.Short" : "short";
            case "Byte" -> nullable ? "java.lang.Byte" : "byte";
            case "Double" -> nullable ? "java.lang.Double" : "double";
            case "Float" -> nullable ? "java.lang.Float" : "float";
            case "Boolean" -> nullable ? "java.lang.Boolean" : "boolean";
            case "Char" -> nullable ? "java.lang.Character" : "char";
            case "String" -> "java.lang.String";
            case "Any" -> "java.lang.Object";
            case "Unit" -> "kotlin.Unit";
            default -> null;
        };
    }

    /**
     * Resolve a user-defined type's simple name to a fully-qualified name using
     * only single-file PSI information, the way Java's {@code InfoParser} does:
     * already-qualified source text, then explicit imports, then the file's
     * package, falling back to the bare name. Star imports and types reachable
     * only via the project classpath cannot be resolved here (see the parser
     * spec's known limitations).
     */
    private static String resolveUserTypeFqn(String simpleName,
            KtTypeReference typeRef, KtFile ktFile)
    {
        // 1. Already qualified in source (e.g. "com.example.Foo")
        String stripped = stripTypeDecoration(typeRef.getText());
        if (stripped != null && stripped.indexOf('.') > 0) {
            return stripped.replace('$', '.');
        }

        // 2. Explicit (non-star) import whose short name matches
        for (KtImportDirective imp : ktFile.getImportDirectives()) {
            if (imp.isAllUnder()) {
                continue;
            }
            FqName fq = imp.getImportedFqName();
            if (fq != null && simpleName.equals(fq.shortName().asString())) {
                return fq.asString();
            }
        }

        // 3. Same package as this file
        FqName pkg = ktFile.getPackageFqName();
        if (pkg != null && !pkg.isRoot()) {
            return pkg.asString() + "." + simpleName;
        }

        // 4. Bare simple name (default package, or unresolved)
        return simpleName;
    }

    /**
     * Strip nullable markers and generic arguments from raw type text, keeping
     * any package qualifier. {@code "Foo<Bar>?" -> "Foo"}, {@code "a.b.C" -> "a.b.C"}.
     */
    private static String stripTypeDecoration(String text)
    {
        if (text == null) {
            return null;
        }
        text = text.trim();
        int lt = text.indexOf('<');
        if (lt > 0) {
            text = text.substring(0, lt);
        }
        if (text.endsWith("?")) {
            text = text.substring(0, text.length() - 1);
        }
        return text.trim();
    }

    /**
     * Check if a type name is a Kotlin primitive/built-in type
     * (which shouldn't be added to the 'used' list).
     */
    private static boolean isPrimitiveKotlinType(String name)
    {
        return switch (name) {
            case "Int", "Long", "Short", "Byte",
                 "Double", "Float",
                 "Boolean", "Char",
                 "String", "Unit", "Nothing", "Any" -> true;
            default -> false;
        };
    }

}
