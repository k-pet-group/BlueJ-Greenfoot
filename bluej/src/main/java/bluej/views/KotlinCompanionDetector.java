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
package bluej.views;

import bluej.utility.Debug;

import kotlin.Metadata;
import kotlin.metadata.KmClass;
import kotlin.metadata.jvm.KotlinClassMetadata;

/**
 * Reads {@code @kotlin.Metadata} from a compiled Kotlin class to locate its
 * companion object, so the companion's methods can be surfaced as static-style
 * operations on the enclosing class (e.g. {@code Foo.Companion.bar()}).
 */
public final class KotlinCompanionDetector
{
    private KotlinCompanionDetector()
    {
        // Utility class — not instantiable
    }

    /**
     * The companion object of a Kotlin class: its declared name (also the name
     * of the static receiver field, e.g. {@code "Companion"}) and the compiled
     * nested class that holds its members.
     */
    public static final class CompanionInfo
    {
        private final String name;
        private final Class<?> companionClass;

        CompanionInfo(String name, Class<?> companionClass)
        {
            this.name = name;
            this.companionClass = companionClass;
        }

        /** The companion name, which is also the static field on the enclosing class (default {@code "Companion"}). */
        public String getName()
        {
            return name;
        }

        /** The compiled nested class holding the companion's members (e.g. {@code Foo$Companion}). */
        public Class<?> getCompanionClass()
        {
            return companionClass;
        }
    }

    /**
     * Returns the companion object of the given class, or {@code null} if the
     * class is not a Kotlin class, has no companion object, or the companion's
     * nested class cannot be resolved.
     *
     * <p>If metadata parsing fails, a warning is logged and {@code null} is
     * returned — graceful degradation to "no companion".
     *
     * @param cl  the compiled class to inspect
     * @return    the companion info, or {@code null}
     */
    public static CompanionInfo getCompanion(Class<?> cl)
    {
        Metadata metadata = cl.getAnnotation(Metadata.class);
        if (metadata == null) {
            return null;
        }

        try {
            KotlinClassMetadata kcm = KotlinClassMetadata.readStrict(metadata);
            if (!(kcm instanceof KotlinClassMetadata.Class)) {
                return null;
            }

            KmClass kmClass = ((KotlinClassMetadata.Class) kcm).getKmClass();
            String companionName = kmClass.getCompanionObject();
            if (companionName == null) {
                return null;
            }

            // The companion compiles to a nested class named Foo$<companionName>.
            // Resolve it via getDeclaredClasses() to avoid a separate Class.forName lookup.
            for (Class<?> nested : cl.getDeclaredClasses()) {
                if (nested.getSimpleName().equals(companionName)) {
                    return new CompanionInfo(companionName, nested);
                }
            }
            return null;
        }
        catch (Exception e) {
            Debug.reportError("Failed to parse Kotlin metadata for " + cl.getName(), e);
            return null;
        }
    }
}
