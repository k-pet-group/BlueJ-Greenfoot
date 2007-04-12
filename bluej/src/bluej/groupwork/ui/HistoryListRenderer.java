package bluej.groupwork.ui;

import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import bluej.groupwork.HistoryInfo;
import bluej.utility.MultiWrapLabel;

/**
 * Renderer for cells in the log/history list.
 * 
 * <p>This is a little complicated because the renderer wraps text at the width
 * of the list. This means that the preferred height of a cell is dependent on the
 * width.
 * 
 * @author Davin McCall
 * @version $Id: HistoryListRenderer.java 4916 2007-04-12 03:57:23Z davmac $
 */
public class HistoryListRenderer extends Box implements ListCellRenderer
{
    private HistoryListModel model;
    
    private JLabel topLabel;
    private MultiWrapLabel commentArea;
    private JLabel spacerLabel;
    
    private JScrollPane container;
    
    private int index;
    
    /**
     * Create a new list renderer.
     */
    public HistoryListRenderer(HistoryListModel model)
    {
        super(BoxLayout.Y_AXIS);
        
        this.model = model;
        
        topLabel = new JLabel();
        Font font = topLabel.getFont();
        topLabel.setAlignmentX(0f);
        topLabel.setFont(font.deriveFont(Font.BOLD));
        add(topLabel);
        
        Box commentBox = new Box(BoxLayout.X_AXIS);
        spacerLabel = new JLabel("      ");
        commentBox.add(spacerLabel);
        commentArea = new MultiWrapLabel();
        commentArea.setAlignmentX(0f);
        //commentArea.setFont(font.deriveFont(font.getSize2D() * 0.9f).deriveFont(0));
        commentArea.setFont(font.deriveFont(0));
        commentBox.add(commentArea);
        commentBox.setAlignmentX(0f);
        add(commentBox);
    }
    
    /**
     * Set the containing scroll pane. This is needed to be able to wrap
     * comment text correctly according to the width of the scroll pane.
     */
    public void setWrapMode(JScrollPane container)
    {
        this.container = container;
    }
    
    /* (non-Javadoc)
     * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
     */
    public Component getListCellRendererComponent(
      JList list,
      Object value,            // value to display
      int index,               // cell index
      boolean isSelected,      // is the cell selected
      boolean cellHasFocus)    // the list and the cell have the focus
    {
        HistoryInfo info = (HistoryInfo) value;
        String topText = info.getDate() + " " + info.getFile() + " " + info.getRevision() + " " + info.getUser();
        topLabel.setText(topText);
        
        String commentText = info.getComment();
        // commentArea.setText("");
        //commentArea.setLineWrap(false);
        //commentArea.setLineWrap(true);
        commentArea.setText(commentText);

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            setOpaque(true);
        }
        else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            setOpaque(false);
        }
        
        if (cellHasFocus) {
            setBorder(new LineBorder(list.getSelectionForeground(), 1));
        }
        else {
            setBorder(new EmptyBorder(1,1,1,1));
        }
        
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        
        if (container != null) {
            Rectangle bbounds = container.getViewportBorderBounds();
            int listWidth = bbounds.width;
            Insets containerInsets = container.getViewport().getInsets();
            listWidth -= containerInsets.left + containerInsets.right;
            listWidth -= spacerLabel.getPreferredSize().width;
            listWidth -= getInsets().left + getInsets().right;
            
            commentArea.setWrapWidth(listWidth);
            commentArea.invalidate();
        }

        // We need to validate to ensure that we have the correct preferred
        // size.
        validate();
        
        // Inform the model of our desired height. If this has changed, the model
        // fires a "cell changed" event so that the list will detect the change.
        model.setCellHeight(index, this.getPreferredSize().height);
        this.index = index;
        
        // Once we return, the list should ask what our preferred size is; it
        // will then call setBounds to set a size.
        return this;
    }
    
    public void setBounds(int x, int y, int width, int height)
    {
        super.setBounds(x, y, width, height);
        
        // Ok, the list has assigned us a size. However, the horizontal size
        // we told the list we needed may now be incorrect, which can cause
        // a horizontal scrollbar to needlessly appear.
                
        validate();
        if (getPreferredSize().getWidth() < width) {
            model.changeChell(index);
        }
    }
    
    public boolean isValidateRoot()
    {
        return true;
    }
}
