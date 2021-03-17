/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.target;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.javafx.AbstractOperation;
import bluej.utility.javafx.JavaFXUtil;

import java.io.File;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Properties;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * A sub package (or parent package)
 * 
 * @author Michael Cahill
 */
public class PackageTarget extends Target
{
    static final int MIN_WIDTH = 60;
    static final int MIN_HEIGHT = 40;

    private static final int TAB_HEIGHT = 12;

    static final String openStr = Config.getString("pkgmgr.packagemenu.open");
    static final String removeStr = Config.getString("pkgmgr.packagemenu.remove");

    @OnThread(Tag.FXPlatform)
    private boolean isMoveable = true;

    public PackageTarget(Package pkg, String baseName)
    {
        super(pkg, baseName, "Package");

        JavaFXUtil.addStyleClass(pane, "package-target");

        Label name = new Label(baseName);
        JavaFXUtil.addStyleClass(name, "package-target-name");
        name.setMaxWidth(9999.0);
        pane.setTop(name);
        setSize(calculateWidth(name, baseName, DEF_WIDTH), DEF_HEIGHT + TAB_HEIGHT);

        Pane center = new Pane();
        BorderPane centerWrapper = new BorderPane(center);
        pane.setCenter(centerWrapper);
        JavaFXUtil.addStyleClass(centerWrapper, "package-target-preview-wrapper");
        JavaFXUtil.addStyleClass(center, "package-target-preview");

        double pos[] = new double[] {0.25,0.5,  0.75, 0.2,   0.6,0.8};
        for (int i = 0; i < 3; i++)
        {
            Pane r = new Pane();
            r.setMouseTransparent(true);
            // was: 15, 10
            r.prefWidthProperty().bind(center.widthProperty().multiply(0.2));
            r.prefHeightProperty().bind(center.heightProperty().multiply(0.2));
            r.layoutXProperty().bind(center.widthProperty().multiply(pos[i*2+0]-0.1));
            r.layoutYProperty().bind(center.heightProperty().multiply(pos[i*2+1]-0.1));
            /*
            //r.setMaxWidth(Region.USE_PREF_SIZE);
            //r.setMaxHeight(Region.USE_PREF_SIZE);
            r.setMinWidth(10);
            r.setMinHeight(5);
            AnchorPane.setLeftAnchor(r, pos[i * 2 + 0] * center.getWidth());
            AnchorPane.setRightAnchor(r, (pos[i * 2 + 0]+0.2) * center.getWidth());
            AnchorPane.setTopAnchor(r, pos[i * 2 + 1] * center.getHeight());
            AnchorPane.setBottomAnchor(r, (pos[i * 2 + 1]+0.2) * center.getHeight());
            int iFinal = i;
            JavaFXUtil.addChangeListener(center.widthProperty(), w -> {
                AnchorPane.setLeftAnchor(r, pos[iFinal * 2 + 0] * w.doubleValue());
                AnchorPane.setRightAnchor(r, (pos[iFinal * 2 + 0]+0.2) * w.doubleValue());
            });
            JavaFXUtil.addChangeListener(center.heightProperty(), h -> {
                AnchorPane.setTopAnchor(r, pos[iFinal * 2 + 1] * h.doubleValue());
                AnchorPane.setBottomAnchor(r, (pos[iFinal * 2 + 1]+0.2) * h.doubleValue());
            });*/

            JavaFXUtil.addStyleClass(r, "package-target-preview-item");

            center.getChildren().add(r);
        }
    }

    /**
     * Return the target's base name (ie the name without the package name). eg.
     * Target
     */
    @OnThread(Tag.Any)
    public String getBaseName()
    {
        return getIdentifierName();
    }

    /**
     * Return the target's name, including the package name. eg. bluej.pkgmgr
     */
    public String getQualifiedName()
    {
        return getOpenPkgName();
    }

    @Override
    public void load(Properties props, String prefix)
        throws NumberFormatException
    {
        super.load(props, prefix);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        props.put(prefix + ".type", "PackageTarget");
    }

    /**
     * Deletes applicable files (directory and ALL contentes) prior to this
     * PackageTarget being removed from a Package.
     */
    public void deleteFiles()
    {
        deleteDir(new File(getPackage().getPath(), getBaseName()));
    }

    /**
     * Delete a directory recursively.
     * This method will delete all files and subdirectories in any
     * directory without asking questions. Use with care.
     *
     * @param directory   The directory that will be deleted.
     */
    private void deleteDir(File directory)
    {
        File[] fileList = directory.listFiles();

        // If it is a file or an empty directory, delete
        if (fileList == null) {
            try{
                directory.delete();
            } catch (SecurityException se){
                Debug.message("Trouble deleting: "+directory+se);
            }
        }
        else {
            if (getPackage().getProject().prepareDeleteDir(directory)) {
                // delete all subdirectories
                for(int i=0;i<Array.getLength(fileList);i++) {
                    deleteDir(fileList[i]);
                }

                // then delete the directory (when it is empty)
                directory.delete();
            }
        }
    }
    
    /**
     * Called when a package icon in a GraphEditor is double clicked. Creates a
     * new PkgFrame when a package is drilled down on.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void doubleClick(boolean openInNewWindow)
    {
        getPackage().getEditor().openPackage(getOpenPkgName());
    }

    @Override
    public List<? extends AbstractOperation<Target>> getContextOperations()
    {
        if (isRemovable())
            return List.of(new OpenPkgAction(), new RemovePkgAction());
        else
            return List.of(new OpenPkgAction());
    }

    @OnThread(Tag.Any)
    protected boolean isRemovable()
    {
        return true;
    }

    @OnThread(Tag.Any)
    protected String getOpenPkgName()
    {
        return getPackage().getQualifiedName(getBaseName());
    }

    @Override
    public void remove()
    {
        if (!isRemovable())
            return;
        
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());
        String name = getQualifiedName();
        PkgMgrFrame[] f = PkgMgrFrame.getAllProjectFrames(pmf.getProject(), name);

        if (f != null)
        {
            DialogManager.showErrorFX(pmf.getWindow(), "remove-package-open");
        }
        else
        {
            // Check they realise that this will delete ALL the files.
            int response = DialogManager.askQuestionFX(pmf.getWindow(), "really-remove-package");

            // if they agree
            if (response == 0)
            {
                deleteFiles();
                getPackage().getProject().removePackage(getQualifiedName());
                getPackage().removeTarget(this);
            }
        }
    }

    /**
     * Removes the package associated with this target unconditionally.
     */
    public void removeImmediate()
    {
        deleteFiles();
        getPackage().removeTarget(this);
        getPackage().getProject().removePackage(getQualifiedName());
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void setSize(int width, int height)
    {
        super.setSize(Math.max(width, MIN_WIDTH), Math.max(height, MIN_HEIGHT));
    }

    @OnThread(Tag.FXPlatform)
    public boolean isMoveable()
    {
        return isMoveable;
    }

    @OnThread(Tag.FXPlatform)
    public void setIsMoveable(boolean isMoveable)
    {
        this.isMoveable = isMoveable;
    }
    
    private static class OpenPkgAction extends AbstractOperation<Target>
    {
        public OpenPkgAction()
        {
            super("openPkg", Combine.ALL, null);
        }

        @Override
        public void activate(List<Target> targets)
        {
            for (Target target : targets)
            {
                if (target instanceof PackageTarget)
                {
                    PackageTarget packageTarget = (PackageTarget) target;
                    packageTarget.getPackage().getEditor().openPackage(packageTarget.getOpenPkgName());
                }
            }
        }

        @Override
        public List<ItemLabel> getLabels()
        {
            return List.of(new ItemLabel(new ReadOnlyStringWrapper(openStr), MenuItemOrder.EDIT));
        }
    }

    private static class RemovePkgAction extends AbstractOperation<Target>
    {
        public RemovePkgAction()
        {
            super("removePkg", Combine.ALL, null);
        }

        @Override
        public void activate(List<Target> targets)
        {
            for (Target target : targets)
            {
                if (target instanceof PackageTarget)
                    ((PackageTarget)target).remove();
            }
        }

        @Override
        public List<ItemLabel> getLabels()
        {
            return List.of(new ItemLabel(new ReadOnlyStringWrapper(removeStr), MenuItemOrder.REMOVE));
        }
    }
}
