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
package bluej.di.scopes;

import com.google.inject.ScopeAnnotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as scoped to a Project lifecycle.
 * Only one instance per Project will exist while that project is open.
 * 
 * <p>Types annotated with {@code @ProjectScoped} will have their instances
 * created once per project and cached for the duration of the project's
 * lifecycle. When the project is closed, all project-scoped instances
 * are eligible for garbage collection.
 * 
 * <p>Example usage:
 * <pre>
 * {@literal @}ProjectScoped
 * public class MyProjectService {
 *     {@literal @}Inject
 *     public MyProjectService(Project project) {
 *         // project is automatically injected from the seeded scope
 *     }
 * }
 * </pre>
 * 
 * @see ProjectScope
 * @see ProjectHandle
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ScopeAnnotation
public @interface ProjectScoped {}
