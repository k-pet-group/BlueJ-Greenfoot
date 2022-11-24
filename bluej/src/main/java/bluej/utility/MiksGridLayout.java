/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 
 
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

/**
 * MiksGridLayout - a grid layout with non-homogenous column widths.
 */
public class MiksGridLayout extends GridLayout
{
    // Specifies which row receives additional vertical space
    private int verticalExpandingRow = -1;
    
    /**
     * Creates a grid layout with the specified number of rows and 
     * columns.
     */
    public MiksGridLayout(int rows, int cols)
    {
        this(rows, cols, 0, 0);
    }

    /**
     * Creates a grid layout with the specified number of rows and columns,
     * and the given gap between each column and row.
     */
    public MiksGridLayout(int rows, int cols, int hgap, int vgap)
    {
        super(rows, cols, hgap, vgap);
    }
    
    // Sets which row receives additional vertical space
    // (by default it is the last one)
    public void setVerticallyExpandingRow(int row)
    {
        verticalExpandingRow = row;
    }

    /** 
     * Determines the preferred size of the container argument using 
     * this grid layout. 
     */
    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int nrows = getRows();
            int ncols = getColumns();
            
            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            } else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }

            int[] colWidth = new int[ncols];
            int[] rowHeight = new int[nrows];

            for(int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                Dimension d = comp.getPreferredSize();
                int row = i / ncols;
                int col = i % ncols;
                if (rowHeight[row] < d.height) {
                    rowHeight[row] = d.height;
                }
                if (colWidth[col] < d.width) {
                    colWidth[col] = d.width;
                }
            }            
            
            int allColWidth = 0;
            for(int col = 0; col < ncols; col++) {
                allColWidth += colWidth[col];
            }
            
            int allRowHeight = 0;
            for(int row = 0; row < nrows; row++) {
                allRowHeight += rowHeight[row];
            }
            
            return new Dimension(insets.left + insets.right + allColWidth + (ncols-1)*getHgap(), 
                         insets.top + insets.bottom + allRowHeight + (nrows-1)*getVgap());
        }
    }

    /**
     * Determines the minimum size of the container argument using this 
     * grid layout. 
     */
    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int nrows = getRows();
            int ncols = getColumns();
            
            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            } else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }

            int[] colWidth = new int[ncols];
            int[] rowHeight = new int[nrows];

            for(int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                Dimension d = comp.getMinimumSize();
                int row = i / ncols;
                int col = i % ncols;
                if (rowHeight[row] < d.height) {
                    rowHeight[row] = d.height;
                }
                if (colWidth[col] < d.width) {
                    colWidth[col] = d.width;
                }
            }            
            
            int allColWidth = 0;
            for(int col = 0; col < ncols; col++) {
                allColWidth += colWidth[col];
            }
            
            int allRowHeight = 0;
            for(int row = 0; row < nrows; row++) {
                allRowHeight += rowHeight[row];
            }
            
            return new Dimension(insets.left + insets.right + allColWidth + (ncols-1)*getHgap(), 
                         insets.top + insets.bottom + allRowHeight + (nrows-1)*getVgap());
        }
    }

    /** 
     * Lays out the specified container using this layout. 
     */
    @Override
    public void layoutContainer(Container parent)
    {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int nrows = getRows();
            int ncols = getColumns();
            int hgap = getHgap();
            int vgap = getVgap();
            boolean ltr = parent.getComponentOrientation().isLeftToRight();
            
            if(!ltr)
                throw new IllegalArgumentException("Orientation oher than left-to-right not supported");
        
            if (ncomponents == 0) {
                return;
            }
            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            } else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }
            
            // compute the width of each column (max width of component in column)

            int[] colWidth = new int[ncols];

            for(int col = 0; col < ncols; col++) {
                int w = 0;
                for (int i = col ; i < ncomponents ; i+=ncols) {
                    Component comp = parent.getComponent(i);
                    Dimension d = comp.getPreferredSize();
                    if (w < d.width) {
                        w = d.width;
                    }
                }
                colWidth[col] = w;
            }

            int colSum = 0;     // all columns except last one
            for(int col = 0; col < ncols-1; col++) {
                colSum += colWidth[col];
            }

            int[] rowHeight = new int[nrows];
            int rowSum = 0;
            
            // compute the height of each row (max width of component in row)

            for(int row = 0; row < nrows; row++) {
                int h = 0;
                for (int i = row*ncols ; (i < (row+1)*ncols) && (i < ncomponents) ; i++) {
                    Component comp = parent.getComponent(i);
                    Dimension d = comp.getPreferredSize();
                    if (h < d.height) {
                        h = d.height;
                    }
                }
                rowHeight[row] = h;
            }
            
            //The row that soaks up the extra space:
            int soakRow = verticalExpandingRow == -1 ? nrows-1 : verticalExpandingRow;

            rowSum = 0;     // all rows except soakRow
            for(int row = 0; row < nrows; row++) {
                if (row != soakRow)
                    rowSum += rowHeight[row];
            }

            int parentWidth = parent.getWidth() - (insets.left + insets.right);
            int parentHeight = parent.getHeight() - (insets.top + insets.bottom);
            
            // set width of last column to take all the remaining space
            colWidth[ncols-1] = (parentWidth - (ncols - 1) * hgap) - colSum;
            if(colWidth[ncols-1] < 0)
                colWidth[ncols-1] = 0;
                
            // set height of soakRow to take all the remaining space
            rowHeight[soakRow] = (parentHeight - (nrows - 1) * vgap) - rowSum;
            if(rowHeight[soakRow] < 0)
                rowHeight[soakRow] = 0;
            
            for (int r = 0, y = insets.top ; r < nrows ; y += rowHeight[r] + vgap, r++) {
                int x = insets.left;
                for (int c = 0; c < ncols ; c++) {
                    int i = r * ncols + c;
                    if (i < ncomponents) {
                        parent.getComponent(i).setBounds(x, y, colWidth[c], rowHeight[r]);
                    }
                    x += colWidth[c] + hgap;
                }
            }
        }
    }
    
}
