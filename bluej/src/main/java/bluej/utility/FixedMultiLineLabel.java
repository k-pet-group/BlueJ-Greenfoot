/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014  Michael Kolling and John Rosenberg 

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
package bluej.utility;

import java.awt.*;
import javax.swing.*;

/**
 ** A multi-line Label-like Swing component. This class is similar to a
 ** MultiLineLabel, but it has a fixed numer of rows.
 **
 ** @author Michael Kolling
 **/

public class FixedMultiLineLabel extends MultiLineLabel
{
    protected JLabel[] labels;
    protected int rows;

    /**
     ** Constructor - make a multiline label
     **/
    public FixedMultiLineLabel(String text, float alignment, int numRows)
    {
        super(null, alignment);
        rows = numRows;
        labels = new JLabel[rows];
        for(int i=0; i<rows; i++) {
            labels[i] = new JLabel(" ");
            labels[i].setAlignmentX(alignment);
            add(labels[i]);
        }
        addText(text);
    }

    /**
     ** Constructor, defaults to left justified text
     **/
    public FixedMultiLineLabel(String text, int numRows)
    {
        this(text, LEFT_ALIGNMENT, numRows);
    }

    /**
     ** Constructor, empty with the given alignment
     **/
    public FixedMultiLineLabel(float alignment, int numRows)
    {
        this(null, alignment, numRows);
    }

    /**
     ** Constructor - make an empty multiline label
     **/
    public FixedMultiLineLabel(int numRows)
    {
        this(null, LEFT_ALIGNMENT, numRows);
    }

    @Override
    public void setText(String text)
    {
        addText(text);
    }

    @Override
    public void addText(String text)
    {
        int lines = 0;

        if(text != null) {
            String strs[] = Utility.splitLines(text);
            lines = (strs.length < rows ? strs.length : rows);
            Font font = new Font("SansSerif", fontAttributes, 12);

            for (int i = 0; i < lines; i++) {
                labels[i].setText((strs[i].length() == 0 ? " " : strs[i]));
                labels[i].setFont(font);
            }
        }

        for (int i = lines; i < rows; i++)
            labels[i].setText(" ");
    }
}
