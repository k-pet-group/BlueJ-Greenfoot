/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.errors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.compiler.Diagnostic.DiagnosticOrigin;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.errors.Correction.CorrectionInfo;
import bluej.stride.framedjava.slots.TypeSlot;
import bluej.stride.generic.AssistContentThreadSafe;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Debug;
import bluej.utility.javafx.FXPlatformConsumer;
import threadchecker.OnThread;
import threadchecker.Tag;

public class UnknownTypeError extends DirectSlotError
{
    private final String typeName;
    private final InteractionManager editor;
    private final List<FixSuggestion> corrections = new ArrayList<>();

    /**
     * Creates an error about an unknown type being used in an expression.  The quick fixes
     * will be to rename to another similarly spelt type name, or to add an import
     * declaration to import the currently-named type from a common package (e.g. unknown type
     * List would offer to import from java.util).
     *
     * @param slotFragment The fragment with the error.
     * @param typeName The name of the type which is used, but was not declared
     * @param replace An action which takes a replacement type name, and substitutes it for the errorneous type name in the original frame.
     * @param editor The editor of the class (used to add imports)
     * @param possibleCorrections The possible other type names (unfiltered: all type names which are in scope)
     * @param possibleImports The possible packages that we could import a class of this name from.
     */
    @OnThread(Tag.Any)
    public UnknownTypeError(SlotFragment slotFragment, String typeName, FXPlatformConsumer<String> replace, InteractionManager editor, Stream<AssistContentThreadSafe> possibleCorrections, Stream<AssistContentThreadSafe> possibleImports)
    {
        super(slotFragment, DiagnosticOrigin.STRIDE_LATE);
        this.typeName = typeName;
        this.editor = editor;
        
        corrections.addAll(Correction.winnowAndCreateCorrections(typeName, possibleCorrections.map(TypeCorrectionInfo::new), replace));
        corrections.addAll(possibleImports
                .filter(ac -> ac.getPackage() != null && ac.getName().equals(typeName))
                .flatMap(ac -> Stream.of(new ImportSingleFix(ac), new ImportPackageFix(ac)))
                .collect(Collectors.toList()));
    }

    @OnThread(Tag.Any)
    private static class TypeCorrectionInfo implements CorrectionInfo
    {
        private AssistContentThreadSafe ac;
        public TypeCorrectionInfo(AssistContentThreadSafe ac) { this.ac = ac; }
        public String getCorrection() { return ac.getName(); }
        public String getDisplay()
        {
            String pkg = ac.getPackage();
            if (pkg == null)
                return ac.getName();
            else
                return ac.getName() + " (" + ac.getPackage() + " package)";
        }
    }
    
    private class ImportSingleFix extends FixSuggestion
    {
        private final AssistContentThreadSafe classInfo;

        @OnThread(Tag.Any)
        public ImportSingleFix(AssistContentThreadSafe ac) { this.classInfo = ac; }

        @Override
        public String getDescription()
        {
            return "Import class " + classInfo.getPackage() + "." + classInfo.getName();
        }

        @Override
        public void execute()
        {
            editor.addImport(classInfo.getPackage() + "." + classInfo.getName());
        }
    }

    private class ImportPackageFix extends FixSuggestion
    {
        private final AssistContentThreadSafe classInfo;

        @OnThread(Tag.Any)
        public ImportPackageFix(AssistContentThreadSafe ac) { this.classInfo = ac; }

        @Override
        public String getDescription()
        {
            return "Import package " + classInfo.getPackage() + " (for " + classInfo.getName() + " class)";
        }

        @Override
        public void execute()
        {
            editor.addImport(classInfo.getPackage() + ".*");
        }
    }    
    
    @Override
    @OnThread(Tag.Any)
    public String getMessage()
    {
        return "Unknown type: " + typeName;
    }

    @Override
    public List<? extends FixSuggestion> getFixSuggestions()
    {
        return corrections;
    }

    @Override
    public boolean isJavaPos()
    {
        return true;
    }
}
