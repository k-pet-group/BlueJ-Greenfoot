/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg

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
package bluej.editor.flow;

import bluej.editor.flow.Document.Bias;

/**
 * A position within a document, that stays persistent.  So if you insert text
 * in the document before it, the position should move accordingly.  If you delete
 * a portion that includes the position, the position moves to the start of the deleted
 * range.
 */
public class TrackedPosition
{
    // package-visible for access by document classes:
    int position;
    final Bias bias;
    private final Document document;
    
    // Package-visible constructor
    public TrackedPosition(Document document, int initialPosition, Bias bias)
    {
        this.document = document;
        this.position = initialPosition;
        this.bias = bias;
    }

    void updateTrackedPosition(int removedStartCharIncl, int removedEndCharExcl, int insertedLength)
    {
        if (this.position > removedStartCharIncl || (this.position == removedStartCharIncl && this.bias == Bias.FORWARD))
        {
            if (this.position < removedEndCharExcl || this.position == removedEndCharExcl && this.bias != Bias.FORWARD)
            {
                this.position = removedStartCharIncl;
            }
            else
            {
                this.position += insertedLength - (removedEndCharExcl - removedStartCharIncl);
            }
        }
    }

    public int getLine()
    {
        return document.getLineFromPosition(position);
    }

    public int getColumn()
    {
        return document.getColumnFromPosition(position);
    }

    public int getPosition()
    {
        return position;
    }

    public void moveBy(int amount)
    {
        if (amount <= 0)
        {
            position = Math.max(0, position + amount);
        }
        else
        {
            position = Math.min(position + amount, document.getLength());
        }
    }

    public void moveTo(int target)
    {
        moveBy(target - position);
    }
    
    public void moveToLineColumn(int line, int column)
    {
        moveTo(document.getLineStart(line) + column);
    }
}
