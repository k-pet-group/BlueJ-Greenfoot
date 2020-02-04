/*
 This file is part of the BlueJ program. 
 Copyright (C) 2020 Michael Kölling and John Rosenberg
 
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

import bluej.Config;
import bluej.compiler.Diagnostic.DiagnosticOrigin;
import bluej.editor.fixes.EditorFixesManager.FixSuggestionBase;
import bluej.editor.fixes.FixSuggestion;
import bluej.editor.stride.FrameEditor;
import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import javafx.application.Platform;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MissingDoubleEqualsError extends DirectSlotError
{
    private final List<FixSuggestion> corrections = new ArrayList<>();

    /**
     * Creates an error about a wrong comparison operator ("=" instead of "==") in a boolean expression.
     * The quick fix will be to replace the single equal operator by a double equal operator.
     *
     * @param slotFragment The fragment with the error.
     */
    @OnThread(Tag.Any)
    public MissingDoubleEqualsError(SlotFragment slotFragment, int startErrorPos, FrameEditor editor)
    {
        super(slotFragment, DiagnosticOrigin.STRIDE_LATE);
        CompletableFuture<String> errorLineTextFutureStr = new CompletableFuture<>();
        Platform.runLater(() -> errorLineTextFutureStr.complete(((ExpressionSlotFragment) slotFragment).getSlot().getText()));
        String errorLineText = null;
        try
        {
            errorLineText = errorLineTextFutureStr.get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            throw new RuntimeException(e);
        }

        String leftCompPart = errorLineText.substring(0, startErrorPos - 1);
        String rightCompPart = errorLineText.substring(startErrorPos);
        corrections.add(new FixSuggestionBase(Config.getString("editor.quickfix.wrongComparisonOperator.fixMsg"), () -> ((ExpressionSlotFragment) slotFragment).getSlot().setText(leftCompPart + "==" + rightCompPart)));
    }

    @Override
    @OnThread(Tag.Any)
    public String getMessage()
    {
        return Config.getString("editor.quickfix.wrongComparisonOperator.errorMsg");
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
