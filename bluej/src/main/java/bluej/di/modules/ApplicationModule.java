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
package bluej.di.modules;

import bluej.pkgmgr.ProjectFactory;
import com.google.inject.AbstractModule;

/**
 * Root Guice module for the BlueJ application.
 *
 * <p>This module installs all sub-modules and provides application-wide
 * bindings. It serves as the entry point for Guice configuration.
 *
 * <p>Currently installed modules:
 * <ul>
 *   <li>{@link ProjectScopeModule} - Configures project-scoped dependencies</li>
 * </ul>
 *
 * <p>Application-scoped singletons:
 * <ul>
 *   <li>{@link ProjectFactory} - Manages project lifecycle and DI scopes</li>
 * </ul>
 *
 * @see ProjectScopeModule
 * @see ProjectFactory
 */
public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        // Install project scope module
        install(new ProjectScopeModule());

        // Application-scoped singletons
        // (ProjectFactory is @Singleton-annotated, so just binding the class is enough)
        bind(ProjectFactory.class);
    }
}
