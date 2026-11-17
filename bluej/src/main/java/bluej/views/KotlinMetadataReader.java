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

import java.lang.annotation.Annotation;

import kotlin.Metadata;
import kotlin.metadata.jvm.JvmMetadataUtil;
import kotlin.metadata.jvm.KotlinClassMetadata;

/** Reads Kotlin metadata without requiring the student's annotation class to belong to the IDE loader. */
final class KotlinMetadataReader
{
    private KotlinMetadataReader()
    {
    }

    // Keep failures visible to the existing detectors' logging and graceful-degradation paths.
    static KotlinClassMetadata read(Class<?> type) throws ReflectiveOperationException
    {
        Metadata metadata = type.getAnnotation(Metadata.class);
        if (metadata == null) {
            for (Annotation annotation : type.getDeclaredAnnotations()) {
                if (annotation.annotationType().getName().equals(Metadata.class.getName())) {
                    metadata = JvmMetadataUtil.Metadata(
                        (Integer) value(annotation, "k"), (int[]) value(annotation, "mv"),
                        (String[]) value(annotation, "d1"), (String[]) value(annotation, "d2"),
                        (String) value(annotation, "xs"), (String) value(annotation, "pn"),
                        (Integer) value(annotation, "xi"));
                    break;
                }
            }
        }
        return metadata == null ? null : KotlinClassMetadata.readStrict(metadata);
    }

    private static Object value(Annotation annotation, String name) throws ReflectiveOperationException
    {
        return annotation.annotationType().getMethod(name).invoke(annotation);
    }
}
