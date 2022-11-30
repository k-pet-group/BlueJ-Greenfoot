/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2019,2020 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.slots;

import java.util.List;

import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.editor.fixes.SuggestionList;
import bluej.utility.javafx.FXPlatformConsumer;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 25/05/2016.
 */
public interface StructuredCompletionCalculator
{
    @OnThread(Tag.FXPlatform)
    public void withCalculatedSuggestionList(JavaFragment.PosInSourceDoc pos, ExpressionSlot<?> completing, CodeElement codeEl, SuggestionList.SuggestionListListener clickListener, String targetType, boolean completingStartOfSlot, FXPlatformConsumer<SuggestionList> handler);

    public String getName(int selected);
    public List<String> getParams(int selected);
    public char getOpening(int selected);
}
