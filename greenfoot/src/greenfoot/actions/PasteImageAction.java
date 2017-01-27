/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2014,2016,2017  Poul Henriksen and Michael Kolling
 
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
package greenfoot.actions;

import greenfoot.core.GProject;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import greenfoot.util.EscapeDialog;

public class PasteImageAction extends AbstractAction
{
    private final EscapeDialog parent;
    private final GProject project;

    public PasteImageAction(EscapeDialog parent, GProject project)
    {
        super(Config.getString("paste.image"));
//        setEnabled(false);
        this.parent = parent;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        pasteImage(parent, project);
    }
    
    public static boolean pasteImage(EscapeDialog parent, GProject project)
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
        private JTextField fileNameField;
        private JButton okButton;

        public NameDialog(EscapeDialog parent, BufferedImage img)
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
            fileNameField = new JTextField();
            fileNameField.setHorizontalAlignment(JTextField.RIGHT);
            fileNameField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { updateOkButton(); }

                @Override
                public void removeUpdate(DocumentEvent e) { updateOkButton(); }

                @Override
                public void changedUpdate(DocumentEvent e) {}
            });

            fileNameRow.add(new JLabel(Config.getString("editor.paste.image.prompt")));
            fileNameRow.add(fileNameField);
            fileNameRow.add(new JLabel(".png"));
            bodyPanel.add(fileNameRow, BorderLayout.CENTER);

            // add buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

            okButton = BlueJTheme.getOkButton();
            okButton.addActionListener(e -> { fileName = fileNameField.getText(); setVisible(false); });
            okButton.setEnabled(false);

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

        private void updateOkButton()
        {
            okButton.setEnabled(!fileNameField.getText().isEmpty());
        }
        
        public String getFileName()
        {
            return fileName;
        }
    }
    
}
