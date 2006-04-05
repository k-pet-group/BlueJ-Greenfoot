package greenfoot.gui;

import javax.swing.JLabel;

import bluej.utility.MultiLineLabel;

/**
 * A label that wraps lines when they exceed a specified length. It wraps on
 * word boundaries only.
 * 
 * @author Poul Henriksen
 * 
 */
public class WrappingMultiLineLabel extends MultiLineLabel
{
    private int cols;

    public WrappingMultiLineLabel(String text, int numCols)
    {
        super(null);
        alignment = LEFT_ALIGNMENT;
        cols = numCols;
        addText(text);
    }

    public void setText(String text)
    {
        addText(text);
    }

    public void addText(String text)
    {
        if (text != null) {
            // get user enforced line breaks
            String strs[] = text.split("\n");

            for (int i = 0; i < strs.length; i++) {
                int lastIndex = 0;
                int index = getLineBreakPoint(strs[i], lastIndex);
                while (index > lastIndex) {
                    addLabel(strs[i].substring(lastIndex, index).trim());
                    lastIndex = index;
                    index = getLineBreakPoint(strs[i], lastIndex);
                }
            }
        }
    }

    /**
     * Gets the next point at which to break the line, assuming that there was a
     * break at lastIndex.
     * 
     * @param str The string
     * @param lastIndex Only look at the string from this index onwards.
     * @return The new index to break the line at. The end has been reached when
     *         index==lastIndex
     */
    private int getLineBreakPoint(String str, int lastIndex)
    {
        int index = str.lastIndexOf(" ", lastIndex + cols);
        // no new whitespace found.
        if (index == lastIndex || index == -1) {
            index = lastIndex + cols;
        }
        // truncate to stay within string bounds
        if (index >= str.length()) {
            index = str.length();
        }
        // if the rest of the string will fit in, just add that
        int strLength = index - lastIndex;
        int rest = str.length() - index;
        if ((strLength + rest) < cols) {
            index = str.length();
        }
        return index;
    }

    private void addLabel(String str)
    {
        if (str.equals(""))
            str = " "; // To make empty lines
        JLabel label = new JLabel(str);
        label.setAlignmentX(alignment);
        add(label);
    }
}
