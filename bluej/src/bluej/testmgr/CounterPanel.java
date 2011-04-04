/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011  Michael Kolling and John Rosenberg 
 
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
package bluej.testmgr;

import java.awt.*;

import javax.swing.*;

import bluej.Config;

/**
 * A panel with test run counters.
 *
 * @author  Andrew Patterson (derived from JUnit src)
 */
public class CounterPanel extends JPanel
{
    private JLabel fNumberOfErrors;
    private JLabel fNumberOfFailures;
    private JLabel fTotalTime;
    private JLabel fNumberOfRuns;
    final static Icon fFailureIcon = Config.getFixedImageAsIcon("failure.gif");
    final static Icon fErrorIcon = Config.getFixedImageAsIcon("error.gif");

    private int fTotal;

    public CounterPanel()
    {
        super(new GridBagLayout());
        fNumberOfErrors= createOutputField(5);
        fNumberOfFailures= createOutputField(5);
        fNumberOfRuns= createOutputField(9);
        fTotalTime = createOutputField(9);

        addToGrid(new JLabel(Config.getString("testdisplay.counter.runs"), JLabel.CENTER),
                0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0));
        addToGrid(fNumberOfRuns,
                1, 0, 1, 1, 0.33, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 8, 0, 0));

        addToGrid(new JLabel(Config.getString("testdisplay.counter.errors"), fErrorIcon, SwingConstants.LEFT),
                2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 8, 0, 0));
        addToGrid(fNumberOfErrors,
                3, 0, 1, 1, 0.33, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 8, 0, 0));

        addToGrid(new JLabel(Config.getString("testdisplay.counter.failures"), fFailureIcon, SwingConstants.LEFT),
                4, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 8, 0, 0));
        addToGrid(fNumberOfFailures,
                5, 0, 1, 1, 0.33, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 8, 0, 0));

        addToGrid(new JLabel(Config.getString("testdisplay.counter.totalTime"), SwingConstants.LEFT),
                6, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 8, 0, 0));
        addToGrid(fTotalTime,
                7, 0, 1, 1, 0.33, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 8, 0, 0));

        setMaximumSize(new Dimension(getMaximumSize().width, getPreferredSize().height));
    }

    private JLabel createOutputField(int width) {
        JLabel field= new JLabel("0");
        Dimension size = field.getMinimumSize();
        size.width *= width;
        field.setMinimumSize(size);

        return field;
    }

    public void addToGrid(Component comp,
            int gridx, int gridy, int gridwidth, int gridheight,
            double weightx, double weighty,
            int anchor, int fill,
            Insets insets) {

        GridBagConstraints constraints= new GridBagConstraints();
        constraints.gridx= gridx;
        constraints.gridy= gridy;
        constraints.gridwidth= gridwidth;
        constraints.gridheight= gridheight;
        constraints.weightx= weightx;
        constraints.weighty= weighty;
        constraints.anchor= anchor;
        constraints.fill= fill;
        constraints.insets= insets;
        add(comp, constraints);
    }

    public void reset() {
        setLabelValue(fNumberOfErrors, 0);
        setLabelValue(fNumberOfFailures, 0);
        setLabelValue(fTotalTime, 0);
        setLabelValue(fNumberOfRuns, 0);
        fTotal= 0;
    }

    public void setTotal(int value) {
        fTotal= value;
    }

    public void setRunValue(int value) {
        fNumberOfRuns.setText(Integer.toString(value) + "/" + fTotal);
    }

    public void setErrorValue(int value) {
        setLabelValue(fNumberOfErrors, value);
    }

    public void setFailureValue(int value) {
        setLabelValue(fNumberOfFailures, value);
    }

    public void setTotalTime(int value) {
        fTotalTime.setText(Integer.toString(value)+"ms");
    }

    private void setLabelValue(JLabel label, int value) {
        label.setText(Integer.toString(value));
    }
}