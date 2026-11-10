/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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
package bluej.utility.javafx;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import bluej.Config;
import bluej.extensions2.SourceType;

import javafx.scene.image.Image;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The small J/K/S marker images that distinguish source types in the class diagram and editor tabs.
 * Loaded on first request, so that classes using them do not need Config at class-initialisation time.
 */
@OnThread(Tag.FX)
public final class SourceTypeIcons
{
    @OnThread(value = Tag.FX, requireSynchronized = true)
    private static final Map<SourceType, Image> headerImages = new EnumMap<>(SourceType.class);

    private SourceTypeIcons()
    {
    }

    /**
     * Get the marker image for a source type. The same instance is returned for every call, so callers
     * may compare by identity.
     *
     * @param type the source type
     * @return the marker image, or null for {@link SourceType#NONE}
     */
    public static synchronized Image headerImage(SourceType type)
    {
        String fileName = switch (type) {
            case Java -> "j.png";
            case Kotlin -> "k.png";
            case Stride -> "s.png";
            case NONE -> null;
        };
        if (fileName == null) {
            return null;
        }
        return headerImages.computeIfAbsent(type,
            t -> JavaFXUtil.loadImage(new File(Config.getBlueJIconPath(), fileName)));
    }
}
