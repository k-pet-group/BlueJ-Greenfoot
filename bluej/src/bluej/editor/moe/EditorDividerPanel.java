package bluej.editor.moe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import bluej.Config;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;

public class EditorDividerPanel extends JPanel implements MouseListener {
    
    boolean expanded=true;
    protected JLabel expandCollapseButton;
    private final static String EXPAND_COLLAPSE_NAVIVIEW= "expandCollapseNaviview";
    private NaviView nav;
    private ImageIcon openNavArrow;
    private ImageIcon closeNavArrow;
    
    public EditorDividerPanel(NaviView naviview) {
        super();
        nav=naviview;
        openNavArrow=Config.getImageAsIcon("image.replace.open");
        closeNavArrow=Config.getImageAsIcon("image.replace.close");
        
        setPreferredSize(new Dimension(closeNavArrow.getIconWidth()+2, 0));
        setMaximumSize(new Dimension(closeNavArrow.getIconWidth()+2, Integer.MAX_VALUE));
        
        setLayout(new DBoxLayout(DBox.X_AXIS, 0, 0));
        expandCollapseButton=new JLabel();
        expandCollapseButton.setName(EXPAND_COLLAPSE_NAVIVIEW);
        expandCollapseButton.addMouseListener(this);
        expandCollapseButton.setIcon(closeNavArrow);
        add(expandCollapseButton, BorderLayout.CENTER);
    }

    protected boolean isExpanded() {
        return expanded;
    }

    protected void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void mouseClicked(MouseEvent e) {
        JComponent src = (JComponent) e.getSource();
        if (src.getName()==EXPAND_COLLAPSE_NAVIVIEW){  
            if (isExpanded()){
                nav.setVisible(false);
                setExpanded(false);
                expandCollapseButton.setIcon(openNavArrow);
            }           
            else{
                nav.setVisible(true);
                setExpanded(true);
                expandCollapseButton.setIcon(closeNavArrow);
            }
        }       
    }

    public void mouseEntered(MouseEvent e) {
        
    }

    public void mouseExited(MouseEvent e) {
        
    }

    public void mousePressed(MouseEvent e) {
        
    }

    public void mouseReleased(MouseEvent e) {
        
    }
}
