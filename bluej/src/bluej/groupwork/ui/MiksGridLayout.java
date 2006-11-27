package bluej.groupwork.ui;
import java.awt.*;

/**
 * MiksGridLayout - a grid layout with non-homogenous column widths.
 * 
 * @version $Id: MiksGridLayout.java 4704 2006-11-27 00:07:19Z bquig $
 */
public class MiksGridLayout extends GridLayout
{
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

    /** 
     * Determines the preferred size of the container argument using 
     * this grid layout. 
     */
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
            int w = 0;
            int h = 0;

            for(int col = 0; col < ncols; col++) {
                w = 0;
                for (int i = col ; i < ncomponents ; i+=ncols) {
                    Component comp = parent.getComponent(i);
                    Dimension d = comp.getPreferredSize();
                    if (w < d.width) {
                        w = d.width;
                    }
                    if (h < d.height) {
                        h = d.height;
                    }
                }
                colWidth[col] = w;
            }
            
            int allColWidth = 0;
            for(int col = 0; col < ncols; col++) {
                allColWidth += colWidth[col];
            }
            
            return new Dimension(insets.left + insets.right + allColWidth + (ncols-1)*getHgap(), 
                         insets.top + insets.bottom + nrows*h + (nrows-1)*getVgap());
        }
    }

    /**
     * Determines the minimum size of the container argument using this 
     * grid layout. 
     */
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
            int h = 0;
            int allColWidth = 0;
            
            for(int col = 0; col < ncols; col++) {
                int w = 0;
                for (int i = col ; i < ncomponents ; i+=ncols) {
                    Component comp = parent.getComponent(i);
                    Dimension d = comp.getMinimumSize();
                    if (w < d.width) {
                        w = d.width;
                    }
                    if (h < d.height) {
                        h = d.height;
                    }
                }
                colWidth[col] = w;
                allColWidth += w;
            }

            return new Dimension(insets.left + insets.right + allColWidth + (ncols-1)*getHgap(), 
                         insets.top + insets.bottom + nrows*h + (nrows-1)*getVgap());
        }
    }

    /** 
     * Lays out the specified container using this layout. 
     */
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
        
            if (ncomponents == 0) {
                return;
            }
            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            } else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }
            
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

            int w = parent.getWidth() - (insets.left + insets.right);
            int h = parent.getHeight() - (insets.top + insets.bottom);
            h = (h - (nrows - 1) * vgap) / nrows;
            colWidth[ncols-1] = (w - (ncols - 1) * hgap) - colSum;
            if(colWidth[ncols-1] < 0)
                colWidth[ncols-1] = 0;
                
            if (ltr) {
                for (int r = 0, y = insets.top ; r < nrows ; r++, y += h + vgap) {
                    int x = insets.left;
                    for (int c = 0; c < ncols ; c++) {
                        int i = r * ncols + c;
                        if (i < ncomponents) {
                            parent.getComponent(i).setBounds(x, y, colWidth[c], h);
                        }
                        x += colWidth[c] + hgap;
                    }
                }
            } else {
                for (int r = 0, y = insets.top ; r < nrows ; r++, y += h + vgap) {
                    int x = parent.getWidth() - insets.right - colWidth[0];
                    for (int c = 0; c < ncols ; c++) {
                        int i = r * ncols + c;
                        if (i < ncomponents) {
                            parent.getComponent(i).setBounds(x, y, w, h);
                        }
                        x -= colWidth[c] + hgap;
                    }
                }
            }
        }
    }
    
}
