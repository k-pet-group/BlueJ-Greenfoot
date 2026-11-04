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

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtTypeAlias;
import org.jetbrains.kotlin.psi.KtUserType;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Bounded supertype name/kind resolution using PSI declarations and a caller's
 * source-first type lookup. This is not Kotlin semantic analysis.
 */
@OnThread(Tag.FXPlatform)
public final class KotlinHierarchyResolver
{
    /** The declaration kind; UNKNOWN reserves a name without claiming a usable type. */
    public enum TypeKind
    {
        /** A class or object declaration. */
        CLASS,
        /** An interface declaration. */
        INTERFACE,
        /** A present name whose type cannot safely be established. */
        UNKNOWN
    }

    /** Looks up an exact source-qualified type name without guessing its package. */
    @FunctionalInterface
    public interface TypeLookup
    {
        /**
         * @param qualifiedName exact source-qualified name to look up
         * @return its kind, UNKNOWN for an unusable/shadowing declaration, or null if absent
         */
        @OnThread(Tag.FXPlatform)
        TypeKind lookup(String qualifiedName);
    }

    record Supertype(String name, TypeKind kind)
    {
    }

    private static final List<String> DEFAULT_IMPORTS = List.of(
        "java.lang", "kotlin", "kotlin.annotation", "kotlin.collections", "kotlin.comparisons",
        "kotlin.io", "kotlin.ranges", "kotlin.sequences", "kotlin.text", "kotlin.jvm");

    private final KtFile file;
    private final KtClassOrObject owner;
    private final TypeLookup lookup;
    private final Map<String, TypeKind> lookedUp = new HashMap<>();

    KotlinHierarchyResolver(KtFile file, KtClassOrObject owner, TypeLookup lookup)
    {
        this.file = file;
        this.owner = owner;
        this.lookup = lookup;
    }

    // Use identifiers rather than source text so generic arguments and backticks do not enter FQNs.
    private static String typeName(KtUserType type)
    {
        if (type.getReferencedName() == null) {
            return null;
        }
        if (type.getQualifier() == null) {
            return type.getReferencedName();
        }
        String prefix = typeName(type.getQualifier());
        return prefix == null ? null : prefix + "." + type.getReferencedName();
    }

    Supertype resolve(KtUserType type)
    {
        String name = typeName(type);
        if (name == null) {
            return null;
        }
        int dot = name.indexOf('.');
        String head = dot < 0 ? name : name.substring(0, dot);
        String tail = dot < 0 ? "" : name.substring(dot);

        // Type parameters and nested classifier declarations can hide imports even in incomplete source.
        if (owner.getTypeParameters().stream().anyMatch(p -> head.equals(p.getName()))
            || owner.getDeclarations().stream().anyMatch(d ->
                (d instanceof KtClassOrObject || d instanceof KtTypeAlias) && head.equals(d.getName()))) {
            return null;
        }

        String localName = qualify(file.getPackageFqName().asString(), head);
        TypeKind local = declaredType(file, localName);
        Set<String> explicit = new LinkedHashSet<>();
        for (KtImportDirective imp : file.getImportDirectives()) {
            if (!imp.isAllUnder() && imp.getImportedFqName() != null && imp.getImportedName() != null
                && head.equals(imp.getImportedName().asString())) {
                explicit.add(imp.getImportedFqName().asString());
            }
        }
        if (explicit.size() > 1 || (local != null && !explicit.isEmpty()
            && !explicit.contains(localName))) {
            return null;
        }
        if (local != null) {
            return known(localName + tail, tail.isEmpty() ? local : find(localName + tail));
        }
        if (!explicit.isEmpty()) {
            String imported = explicit.iterator().next() + tail;
            TypeKind kind = find(imported);
            return kind == TypeKind.UNKNOWN ? null : new Supertype(imported, kind);
        }

        TypeKind samePackage = find(localName);
        if (samePackage != null) {
            return known(localName + tail, tail.isEmpty() ? samePackage : find(localName + tail));
        }

        if (dot >= 0) {
            // First try imported outer types (Outer.Inner), then a source-qualified name.
            Set<String> candidates = starCandidates(head);
            if (!candidates.isEmpty()) {
                Supertype importedHead = unique(candidates);
                if (importedHead == null) {
                    return null;
                }
                String nested = importedHead.name() + tail;
                return known(nested, find(nested));
            }
            TypeKind kind = find(name);
            return kind == TypeKind.UNKNOWN ? null : new Supertype(name, kind);
        }
        return unique(starCandidates(name));
    }

    private Set<String> starCandidates(String name)
    {
        List<String> stars = new ArrayList<>();
        for (KtImportDirective imp : file.getImportDirectives()) {
            if (imp.isAllUnder() && imp.getImportedFqName() != null) {
                stars.add(imp.getImportedFqName().asString());
            }
        }
        Set<String> candidates = candidates(stars, name);
        // A present but unusable candidate still blocks lower-priority/default imports.
        return candidates.isEmpty() ? candidates(DEFAULT_IMPORTS, name) : candidates;
    }

    private Set<String> candidates(List<String> packages, String name)
    {
        Set<String> candidates = new LinkedHashSet<>();
        for (String pkg : packages) {
            String candidate = qualify(pkg, name);
            if (find(candidate) != null) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private Supertype unique(Set<String> candidates)
    {
        if (candidates.size() != 1) {
            return null;
        }
        String name = candidates.iterator().next();
        return known(name, find(name));
    }

    private Supertype known(String name, TypeKind kind)
    {
        return kind == null || kind == TypeKind.UNKNOWN ? null : new Supertype(name, kind);
    }

    private TypeKind find(String name)
    {
        if (!lookedUp.containsKey(name)) {
            TypeKind declared = declaredType(file, name);
            lookedUp.put(name, declared != null ? declared : lookup.lookup(name));
        }
        return lookedUp.get(name);
    }

    private static String qualify(String pkg, String name)
    {
        return pkg.isEmpty() ? name : pkg + "." + name;
    }

    // Source identity only: never resolve this declaration's own parents recursively.
    static TypeKind declaredType(KtFile file, String qualifiedName)
    {
        String pkg = file.getPackageFqName().asString();
        String prefix = pkg.isEmpty() ? "" : pkg + ".";
        if (!qualifiedName.startsWith(prefix)) {
            return null;
        }
        String[] names = qualifiedName.substring(prefix.length()).split("\\.");
        List<KtDeclaration> declarations = file.getDeclarations();
        for (int i = 0; i < names.length; i++) {
            KtDeclaration match = null;
            for (KtDeclaration declaration : declarations) {
                if (names[i].equals(declaration.getName())
                    && (declaration instanceof KtClassOrObject || declaration instanceof KtTypeAlias)) {
                    if (match != null) {
                        return TypeKind.UNKNOWN;
                    }
                    match = declaration;
                }
            }
            if (match instanceof KtClassOrObject co) {
                if (i == names.length - 1) {
                    return co instanceof KtClass c && c.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS;
                }
                declarations = co.getDeclarations();
            }
            else {
                return match == null ? null : TypeKind.UNKNOWN;
            }
        }
        return null;
    }

    /**
     * Read the kind of a public library type, including nested types, without initialising it.
     * Callers must exclude source-backed/stale project output before using this lookup.
     *
     * @param qualifiedName source-qualified type name
     * @param loader library or validated compiled-output loader
     * @return the kind, or null if unavailable or inaccessible
     */
    public static TypeKind libraryType(String qualifiedName, ClassLoader loader)
    {
        return libraryType(qualifiedName, loader, resource -> true);
    }

    /**
     * Look up a library type only if its class resource is permitted, before loading any bytecode.
     *
     * @param qualifiedName source-qualified type name
     * @param loader library or validated compiled-output loader
     * @param permittedResource predicate excluding stale/untracked project output
     * @return the kind, or null if absent, inaccessible, or disallowed
     */
    public static TypeKind libraryType(String qualifiedName, ClassLoader loader, Predicate<URL> permittedResource)
    {
        String binaryName = qualifiedName;
        while (true) {
            try {
                URL resource = loader.getResource(binaryName.replace('.', '/') + ".class");
                if (resource == null) {
                    throw new ClassNotFoundException(binaryName);
                }
                if (!permittedResource.test(resource)) {
                    return null;
                }
                Class<?> cls = Class.forName(binaryName, false, loader);
                for (Class<?> enclosing = cls; enclosing != null; enclosing = enclosing.getEnclosingClass()) {
                    if (!Modifier.isPublic(enclosing.getModifiers())) {
                        return null;
                    }
                }
                return cls.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS;
            }
            catch (ClassNotFoundException e) {
                int dot = binaryName.lastIndexOf('.');
                if (dot < 0) {
                    return null;
                }
                binaryName = binaryName.substring(0, dot) + "$" + binaryName.substring(dot + 1);
            }
            catch (LinkageError | SecurityException e) {
                return null;
            }
        }
    }
}
