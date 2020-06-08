/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2019,2020 Michael Kölling and John Rosenberg
 
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

import bluej.Config;
import bluej.compiler.Diagnostic.DiagnosticOrigin;
import bluej.editor.fixes.Correction;
import bluej.editor.fixes.Correction.TypeCorrectionInfo;
import bluej.editor.fixes.EditorFixesManager.FixSuggestionBase;
import bluej.editor.fixes.FixSuggestion;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.parser.AssistContentThreadSafe;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.FXPlatformConsumer;
import javafx.application.Platform;
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
     * @param replace An action which takes a replacement type name, and substitutes it for the erroneous type name in the original frame.
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

        // Add the fixes: correction, import class and import package
        List<AssistContentThreadSafe> possibleImportsList = possibleImports.collect(Collectors.toList());
        List<AssistContentThreadSafe> possibleCorrectionsList = possibleCorrections.collect(Collectors.toList());
        // For the corrections, we also include some potential classes from packages we consider to be usual.
        possibleCorrectionsList.addAll(possibleImportsList.stream().filter(ac -> Correction.isClassInUsualPackagesForCorrections(ac) && !possibleCorrectionsList.contains(ac)).collect(Collectors.toList()));
        Platform.runLater(() -> corrections.addAll(Correction.winnowAndCreateCorrections(typeName, possibleCorrectionsList.stream().map(TypeCorrectionInfo::new), replace, true)));
        corrections.addAll(possibleImportsList.stream()
            .filter(ac -> ac.getPackage() != null && ac.getName().equals(typeName))
            .flatMap(ac -> Stream.of(new FixSuggestionBase((Config.getString("editor.quickfix.unknownType.fixMsg.class") + ac.getPackage() + "." + ac.getName()), () -> editor.getFrameEditor().addImportFromQuickFix(ac.getPackage() + "." + ac.getName())),
                new FixSuggestionBase((Config.getString("editor.quickfix.unknownType.fixMsg.package") + ac.getPackage() + " (for " + ac.getName() + " class)"), () -> editor.getFrameEditor().addImportFromQuickFix(ac.getPackage() + ".*"))))
            .collect(Collectors.toList()));
    }

    @Override
    @OnThread(Tag.Any)
    public String getMessage()
    {
        return Config.getString("editor.quickfix.unknownType.errorMsg") + typeName;
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
