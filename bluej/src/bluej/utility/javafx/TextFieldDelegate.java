/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2018 Michael Kölling and John Rosenberg
 
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
package bluej.utility.javafx;

import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(Tag.FXPlatform)
public interface TextFieldDelegate<IDENTIFIER>
{
    void insert(IDENTIFIER id, int index, String text);

    // This will be called speculatively by deletePrevious and deleteNext, in case
    // there is a selection.  It should return false if there is no selection (but not give an exception)
    boolean deleteSelection();

    boolean deletePrevious(IDENTIFIER id, int caretPosition, boolean atStart);
    boolean deleteNext(IDENTIFIER id, int caretPosition, boolean atEnd);

    boolean previousWord(IDENTIFIER id, boolean atStart);

    boolean nextWord(IDENTIFIER id, boolean atEnd);

    boolean endOfNextWord(IDENTIFIER id, boolean atEnd);

    void backwardAtStart(IDENTIFIER id);
    void forwardAtEnd(IDENTIFIER id);
    
    void deselect();

    boolean copy();

    boolean cut();

    void delete(IDENTIFIER id,
            int start, int end);

    boolean selectBackward(IDENTIFIER id, int caretPosition);

    boolean selectForward(IDENTIFIER id, int caretPosition, boolean atEnd);

    boolean selectAll(IDENTIFIER id);
    
    boolean selectNextWord(IDENTIFIER id);

    boolean selectPreviousWord(IDENTIFIER id);

    boolean home(IDENTIFIER id);

    boolean end(IDENTIFIER id, boolean asPartOfNextWordCommand);

    boolean selectHome(IDENTIFIER id, int caretPosition);

    boolean selectEnd(IDENTIFIER id, int caretPosition);

    void moveTo(double sceneX, double sceneY, boolean b);

    void selectTo(double sceneX, double sceneY);

    // Called when escape is pressed
    void escape();
    
    // Called when selection is complete (mouse is released)
    void selected();

    void clicked();
    
    void caretMoved();
}
