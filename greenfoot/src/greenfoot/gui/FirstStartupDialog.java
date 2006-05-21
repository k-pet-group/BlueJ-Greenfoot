package greenfoot.gui;

import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import bluej.BlueJTheme;

/**
 * This is a dialog box presented to the user the very first time greenfoot is
 * started. The purpose it to make it easy to use greenfoot for the very first
 * time by presenting the user with a few options to select among. The dialog
 * will only be shown the very first time after installing greenfoot.
 * 
 * @author Poul Henriksen
 * @version $id:$
 */
public class FirstStartupDialog extends JDialog
{
    public FirstStartupDialog()
    {
        super((Frame) null, "Greenfoot");
        buildUI();
        pack();
    }

    public void buildUI() {
        JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(BlueJTheme.dialogBorder);
        
        int spacingLarge = BlueJTheme.componentSpacingLarge;
        int spacingSmall = BlueJTheme.componentSpacingSmall;
        
        String headerText = "Welcome to Greenfoot!";
        String welcomeText = "Greenfoot lets you work on projects (also called 'scenarios'). To use Greenfoot, you first have to open an existing scenario, or create a new one. Several sample scenarios are included in a standard Greenfoot installlation.";
        String questionText = "What would you like to do?";
        
        //TODO maybe a nicely rendered image instead
        JLabel headerLabel = new JLabel(headerText);
        Font f = headerLabel.getFont();
        f = f.deriveFont((f.getSize() + 4f));
        headerLabel.setFont(f);
        WrappingMultiLineLabel welcomeLabel = new WrappingMultiLineLabel(welcomeText, 60);
        JLabel questionLabel = new JLabel(questionText);
        
       /* headerLabel.setBorder(new EmptyBorder(5,5,5,5));
        welcomeLabel.setBorder(new EmptyBorder(5,5,5,5));        
        questionLabel.setBorder(new EmptyBorder(5,5,5,5));*/
        
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        questionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        
        contentPane.add(headerLabel);   
        contentPane.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, spacingSmall));
        contentPane.add(welcomeLabel);
        
        contentPane.add(Box.createVerticalGlue());
        contentPane.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, spacingLarge));
        contentPane.add(questionLabel);
        contentPane.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, spacingSmall));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS)); //
        JButton tutorialButton = new JButton("Open tutorial and tutorial scenario");
        JButton openButton = new JButton("Choose a scenario");
        JButton createButton = new JButton("Create a new scenario");
      //  JButton openButton = new JButton("Continue without scenario");        
               
        
        tutorialButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        openButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        Dimension bigSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        tutorialButton.setMaximumSize(bigSize);
        openButton.setMaximumSize(bigSize);
        createButton.setMaximumSize(bigSize);
        
        
        buttonPanel.add(tutorialButton);
        buttonPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, spacingSmall));
        buttonPanel.add(openButton);
        buttonPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, spacingSmall));
        buttonPanel.add(createButton);
        
        JPanel nonGreedyPanel = new JPanel();
        nonGreedyPanel.add(buttonPanel);
        contentPane.add(nonGreedyPanel);
    }
    
}

