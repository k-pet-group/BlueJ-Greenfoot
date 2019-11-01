/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2014,2016,2019  Michael Kolling and John Rosenberg
 
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
package bluej.collect;

import bluej.compiler.*;
import bluej.pkgmgr.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A wrapper for a {@link FXCompileObserver} that also logs the
 * error messages to the data collector.
 * 
 * @author Neil Brown
 *
 */
public class DataCollectionCompileObserverWrapper implements FXCompileObserver
{
    private FXCompileObserver wrapped;
    private List<DiagnosticWithShown> diagnostics = new ArrayList<DiagnosticWithShown>();
    private CompileInputFile[] sources;
    private CompileReason reason;
    private Project project;
    
    public DataCollectionCompileObserverWrapper(Project project, FXCompileObserver wrapped)
    {
        this.project = project;
        this.wrapped = wrapped;
    }

    @Override
    public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence)
    {
        diagnostics.clear();
        this.sources = sources;
        this.reason = reason;
        wrapped.startCompile(sources, reason, type, compilationSequence);

    }

    @Override
    public boolean compilerMessage(Diagnostic diagnostic, CompileType type)
    {
        boolean shownToUser = wrapped.compilerMessage(diagnostic, type);
        if (diagnostic.getFileName() != null)
        {
            File userFile = new File(diagnostic.getFileName());
            for (CompileInputFile input : sources)
            {
                if (input.getJavaCompileInputFile().getName().equals(userFile.getName()))
                {
                    userFile = input.getUserSourceFile();
                    break;
                }
            }
            diagnostics.add(new DiagnosticWithShown(diagnostic, shownToUser, userFile));
        }
        return shownToUser;
    }

    @Override
    public void endCompile(CompileInputFile[] sources, boolean successful, CompileType type, int compilationSequence)
    {
        // Heuristic: if all files are in the same package, record the compile as being with that package
        // (I'm fairly sure the BlueJ interface doesn't let you do cross-package compile,
        // so I think this should always produce one package)
        Set<String> packages = new HashSet<String>();
        for (CompileInputFile f : sources)
        {
            packages.add(project.getPackageForFile(f.getJavaCompileInputFile()));
        }
        bluej.pkgmgr.Package pkg = packages.size() == 1 ? project.getPackage(packages.iterator().next()) : null;
        
        DataCollector.compiled(project, pkg, sources, diagnostics, successful, reason, compilationSequence);
        wrapped.endCompile(sources, successful, type, compilationSequence);
    }

}
