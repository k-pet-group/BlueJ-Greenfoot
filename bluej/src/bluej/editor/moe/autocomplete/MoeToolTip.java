package bluej.editor.moe.autocomplete;

import bluej.Config;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicToolTipUI;
import java.awt.*;

import java.io.*;   // For configuration file reading.
import java.util.Properties;

/**
 * This class extends JToolTip to create a tool tip that is capable
 * of displaying HTML text.  This is necessary because we need to
 * display sections of text in bold and this cannot be done using
 * a standard JToolTip.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class MoeToolTip extends JToolTip{

    private Color BackgroundColor;

    public MoeToolTip() {
        updateUI();
        getBackGroundColour();
        setBackground(BackgroundColor);
    }

    public void updateUI() {
        setUI(MoeToolTipUI.createUI(this));
    }

    private void getBackGroundColour(){
      Properties editorProps = Config.moe_props;

      String colorStr;
      int  colorInt;

      colorStr = editorProps.getProperty("tool.tip.background.colour","FFFFD6");
      try {
          colorInt = Integer.parseInt(colorStr,16);
      }
      catch (NumberFormatException e) {
          colorInt = 0xFFFFD6;
      }
      BackgroundColor = new Color(colorInt);
    }

}


class MoeToolTipUI extends BasicToolTipUI {
    static MoeToolTipUI sharedInstance = new MoeToolTipUI();

    static JToolTip tip;
    protected CellRendererPane rendererPane;

    private static JEditorPane textArea ;

    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    public MoeToolTipUI() {
        super();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        tip = (JToolTip)c;
        rendererPane = new CellRendererPane();
        c.add(rendererPane);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);

        c.remove(rendererPane);
        rendererPane = null;
    }

    public void paint(Graphics g, JComponent c) {
        Dimension size = c.getSize();
        textArea.setBackground(c.getBackground());

        rendererPane.paintComponent(g, textArea, c, 1, 1,
                        size.width - 1, size.height - 1, true);
    }

    public Dimension getPreferredSize(JComponent c) {
        String tipText = ((JToolTip)c).getTipText();

        if (tipText == null) return new Dimension(0,0);
        textArea = new JEditorPane("text/html", tipText );

        rendererPane.removeAll();
        rendererPane.add(textArea );
        textArea.setBorder(BorderFactory.createEmptyBorder(0,2,0,2));

        Dimension dim = textArea.getPreferredSize();

        dim.height += 2;
        dim.width += 2;
        return dim;
    }

    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }
}
