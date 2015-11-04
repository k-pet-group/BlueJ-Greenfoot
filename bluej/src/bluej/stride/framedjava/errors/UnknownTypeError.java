/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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

import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.errors.Correction.CorrectionInfo;
import bluej.stride.generic.AssistContentThreadSafe;
import bluej.stride.generic.InteractionManager;
import bluej.stride.slots.TypeTextSlot;

public class UnknownTypeError extends DirectSlotError
{
    private final String typeName;
    private final InteractionManager editor;
    private List<FixSuggestion> corrections;
    
    public UnknownTypeError(TypeSlotFragment slotFragment, String typeName, TypeTextSlot slot, InteractionManager editor, Stream<AssistContentThreadSafe> possibleCorrections, Stream<AssistContentThreadSafe> possibleImports)
    {
        super(slotFragment);
        this.typeName = typeName;
        this.editor = editor;
        
        corrections = new ArrayList<>(Correction.winnowAndCreateCorrections(typeName, possibleCorrections.map(TypeCorrectionInfo::new), s -> slot.setText(s)));
        corrections.addAll(possibleImports
                .filter(ac -> ac.getPackage() != null && ac.getName().equals(typeName))
                .flatMap(ac -> Stream.of(new ImportSingleFix(ac), new ImportPackageFix(ac)))
                .collect(Collectors.toList()));
    }
    
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
