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

import java.util.Collections;
import java.util.List;

import bluej.editor.fixes.FixSuggestion;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.stride.framedjava.ast.JavaFragment;

public class JavaCompileError extends CodeError
{
    private final int startPos;
    private final int endPos;
    private final String message;
    
    @OnThread(Tag.Any)
    public JavaCompileError(JavaFragment slotElement, int startPos, int endPos, String message, int identifier)
    {
        super(slotElement, identifier);
        this.startPos = startPos;
        this.endPos = endPos;
        if (endPos < startPos) {
            throw new IllegalArgumentException("JavaCompileError ends before it begins");
        }
        this.message = message;
    }

    @Override
    @OnThread(Tag.Any)
    public String getMessage()
    {
        return message;
    }

    @Override
    public List<FixSuggestion> getFixSuggestions()
    {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "JavaCompileError [startPos=" + startPos + ", endPos="
                + endPos + ", message=" + message + "]";
    }
    @Override
    public int getStartPosition()
    {
        return startPos;
    }

    @Override
    public int getEndPosition()
    {
        return endPos;
    }
    
    @Override
    public boolean isJavaPos()
    {
        return true;
    }
}
