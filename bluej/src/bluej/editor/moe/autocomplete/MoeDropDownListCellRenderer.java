package bluej.editor.moe.autocomplete;

import javax.swing.*;
import java.awt.Component;
import java.awt.Color;
import java.net.URL;
import java.io.*;



/**
 * This class is used by MoeDropDownLists to render each cell in the list.
 * Each cell displays an icon representing the type of the underlying
 * value stored in the data model of the list
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class MoeDropDownListCellRenderer extends JLabel
                                         implements ListCellRenderer {

    /**
     * This constructs a cell renderer for
     * a MoeDropDownList so that each row displays
     * an icon representing its type
     */
    public MoeDropDownListCellRenderer() {
        setOpaque(true);
    }

    /**
     * This is the only method we need to define in order to
     * implement the ListCellRenderer interface.  It is called
     * by the JList every time it needs to display a cell in
     * the list.
     *
     * @param list The JList we're painting.
     * @param value The value returned by list.getModel().getElementAt(index).
     * @param index The cells index.
     * @param isSelected True if the specified cell was selected.
     * @param cellHasFocus True if the specified cell has the focus.
     * @return A component whose paint() method
     *         will render the specified value.
     */
    public Component getListCellRendererComponent(JList list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus){
        setIconTextGap(0);
        MoeDropDownItem item = (MoeDropDownItem) value;

        if (isSelected){
            setIcon(item.getSelectedIcon());
        }
        else{
            setIcon(item.getIcon());
        }

        String s = value.toString();
        setText(s);

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        }
        else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setOpaque(true);
        return this;
    }


 }
