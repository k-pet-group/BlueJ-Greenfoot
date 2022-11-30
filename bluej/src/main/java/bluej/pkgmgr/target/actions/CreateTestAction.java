/*
 This file is part of the BlueJ program. 
 Copyright (C) 2020 Michael Kölling and John Rosenberg 
 
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
package bluej.pkgmgr.target.actions;

import bluej.extensions2.SourceType;
import bluej.pkgmgr.PackageEditor;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget;
import bluej.pkgmgr.target.EditableTarget;
import bluej.pkgmgr.target.Target;
import bluej.utility.javafx.JavaFXUtil;
import javafx.event.ActionEvent;
import javafx.scene.control.MenuItem;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Action which creates a test
 */
@OnThread(Tag.FXPlatform)
public class CreateTestAction extends ClassTargetOperation
{
    /**
     * Constructor for the CreateTestAction object
     */
    public CreateTestAction()
    {
        super("createTest", Combine.ONE, null, ClassTarget.createTestStr, MenuItemOrder.CREATE_TEST, EditableTarget.MENU_STYLE_INBUILT);
    }

    @Override
    protected void execute(ClassTarget createTestFor)
    {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(createTestFor.getPackage());

        if (pmf != null) {
            String testClassName = createTestFor.getIdentifierName() + "Test";
            pmf.createNewClass(testClassName, "unittest", SourceType.Java, true, -1, -1);
            // we want to check that the previous called actually
            // created a unit test class as a name clash with an existing
            // class would not. This prevents a non unit test becoming
            // associated with a class unintentionally
            Target target = createTestFor.getPackage().getTarget(testClassName);
            DependentTarget assoc = null;
            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;
                if (ct != null && ct.isUnitTest()) {
                    assoc = (DependentTarget) createTestFor.getPackage().getTarget(createTestFor.getIdentifierName() + "Test");
                }
            }
            DependentTarget assocFinal = assoc;
            PackageEditor pkgEd = createTestFor.getPackage().getEditor();
            if (assocFinal != null)
                createTestFor.setAssociation(assocFinal);
            createTestFor.updateAssociatePosition();
            pkgEd.repaint();
        }
    }
}
