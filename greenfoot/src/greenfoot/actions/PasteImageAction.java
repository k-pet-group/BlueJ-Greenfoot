package greenfoot.actions;

import greenfoot.core.GProject;
import greenfoot.gui.GreenfootFrame;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

public class PasteImageAction extends AbstractAction
{
    private final GreenfootFrame gfFrame;
    
    public PasteImageAction(GreenfootFrame gfFrame)
    {
        super(Config.getString("paste.image"));
        setEnabled(false);
        this.gfFrame = gfFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        pasteImage(gfFrame, gfFrame.getProject());
    }
    
    public static boolean pasteImage(Frame parent, GProject project)
    {
        Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor))
        {
            try
            {
                Image img = (Image) transferable.getTransferData(DataFlavor.imageFlavor);
                BufferedImage buffered = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = buffered.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();
                
                NameDialog dlg = new NameDialog(parent, buffered);
                dlg.setModal(true);
                DialogManager.centreDialog(dlg);
                dlg.setVisible(true);
                
                if (dlg.getFileName() != null)
                {
                    ImageIO.write(buffered, "png", new File(project.getImageDir(), dlg.getFileName() + ".png"));
                    return true;
                }
            }
            catch (UnsupportedFlavorException | IOException ex)
            {
                Debug.reportError(ex);
            }
         
        }
        else
        {
            DialogManager.showMessage(parent, "no-clipboard-image-data");
        }
        return false;
    }

    private static class NameDialog extends EscapeDialog
    {
        private String fileName = null; 

        public NameDialog(Frame parent, BufferedImage img)
        {
            super(parent, Config.getString("editor.paste.image.title"), true);
            makeDialog(img);
        }

        private void makeDialog(BufferedImage img)
        {
            JPanel bodyPanel = new JPanel();
            bodyPanel.setLayout(new BorderLayout());
            bodyPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
       
            bodyPanel.add(new JLabel(new ImageIcon(img)), BorderLayout.NORTH);
            
            JPanel fileNameRow = new JPanel();
            fileNameRow.setLayout(new BoxLayout(fileNameRow, BoxLayout.X_AXIS));
            fileNameRow.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
            JTextField fileNameField = new JTextField();
            fileNameField.setHorizontalAlignment(JTextField.RIGHT);
            fileNameRow.add(new JLabel(Config.getString("editor.paste.image.prompt")));
            fileNameRow.add(fileNameField);
            fileNameRow.add(new JLabel(".png"));
            bodyPanel.add(fileNameRow, BorderLayout.CENTER);

            // add buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

            JButton okButton = BlueJTheme.getOkButton();
            okButton.addActionListener(e -> { fileName = fileNameField.getText(); setVisible(false); });

            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.addActionListener(e -> setVisible(false));

            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
            getRootPane().setDefaultButton(okButton);
            
            bodyPanel.add(buttonPanel, BorderLayout.SOUTH);
            getContentPane().add(bodyPanel, BorderLayout.CENTER);
            pack();
            
            fileNameField.requestFocusInWindow();
        }
        
        public String getFileName()
        {
            return fileName;
        }
    }
    
}
