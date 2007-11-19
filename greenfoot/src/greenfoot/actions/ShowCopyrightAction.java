package greenfoot.actions;

import bluej.Config;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Action to display a copyright notice.
 *
 * @author mik
 */
public class ShowCopyrightAction extends AbstractAction
{
    private static ShowCopyrightAction instance;
    
     /**
     * Singleton factory method for action.
     */
   public static ShowCopyrightAction getInstance(JFrame parent)
    {
        if(instance == null)
            instance = new ShowCopyrightAction(parent);
        return instance;
    }
    

   private JFrame parent;
    
    /** 
     *  Creates a new instance of ShowCopyrightAction 
     */
    private ShowCopyrightAction(JFrame parent) 
    {
        super(Config.getString("greenfoot.copyright"));
        this.parent = parent;
    }

    /**
     * The action was fired...
     */
    public void actionPerformed(ActionEvent e)
    {
            JOptionPane.showMessageDialog(parent, new String[]{
                "Greenfoot \u00a9 2005-2007 Michael K\u00F6lling, Poul Henriksen.", " ",
                "Greenfoot is available 'as is' free of charge for use and non-commercial", 
                "redistribution. Disassembly of the system is prohibited.",
                "This software may not be sold for profit or included in other", 
                "packages which are sold for profit without written authorisation.", 
                " ",
                "Copyright notice for the class GraphicsUtilities: ",
                " Copyright (c) 2007, Romain Guy",
                " All rights reserved.",
                " ",
                " Redistribution and use in source and binary forms, with or without",
                " modification, are permitted provided that the following conditions",
                " are met:",
                " ",
                "   * Redistributions of source code must retain the above copyright",
                "     notice, this list of conditions and the following disclaimer.",
                "   * Redistributions in binary form must reproduce the above",
                "     copyright notice, this list of conditions and the following",
                "     disclaimer in the documentation and/or other materials provided",
                "     with the distribution.",
                "   * Neither the name of the TimingFramework project nor the names of its",
                "     contributors may be used to endorse or promote products derived",
                "     from this software without specific prior written permission.",
                " ",
                " THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS",
                " \"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT",
                " LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR",
                " A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT",
                " OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,",
                " SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT",
                " LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,",
                " DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY",
                " THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT",
                " (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE",
                " OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
                }, 
                "Copyright, License and Redistribution", JOptionPane.INFORMATION_MESSAGE);
    }
}
