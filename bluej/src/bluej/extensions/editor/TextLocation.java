package bluej.extensions.editor;

/**
 * A TextLocation object groups two pieces of information: the line number and the column number.
 * They represent a position in the editor's text.
 * A text location represents the gap to the left of the position identified, so
 * that (0, 0) is the start of the file, (0, 1) is between the first and
 * second characters in the file, and so on. There is a TextLocation position to
 * the right of the last character on a line. The column value of this
 * position can be calculated using Editor.getLineLength(int line).
 *
 * @definition An invalid TextLocation is one that, at the time of use, points to an area outside the
 * text being edited.
 * 
 * @version $Id: TextLocation.java 2835 2004-08-04 14:25:55Z damiano $
 */

/*
 * @author Damiano Bolla, University of Kent at Canterbury, 2004
 */  
public class TextLocation
{
    /**
     * Create a TextLocation representing the text position at the specified line and column
     *
     * @param  line    a line number starting from 0
     * @param  column  a column number starting from 0
     */
    public TextLocation(int line, int column)
    {
    }


    /**
     * Sets the line of the text position, leaves the column unchanged.
     *
     * @param  line  the line number starting from zero
     */
    public void setLine(int line)
    {
    }


    /**
     * Returns the line of this text position
     *
     * @return    the line number of this text position
     */
    public int getLine()
    {
        return 0;
    }


    /**
     * Sets the column where this caret should be, leaves the line unchanged.
     *
     * @param  column  the column number starting from zero
     */
    public void setColumn(int column)
    {
    }


    /**
     * Returns the column of this text location
     *
     * @return    the column number of this text location
     */
    public int getColumn()
    {
        return 0;
    }


    /**
     * Set both the line and column where of text location
     *
     * @param  line    a line number starting from zero
     * @param  column  a column number starting from zero
     */
    public void setPosition(int line, int column)
    {
    }
}
