/*
 * Copyright (c) 2007, Romain Guy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the TimingFramework project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package greenfoot.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * Class to create drop shadows for images.
 * <p>
 * 
 * The implementation in this class is taken from the DropShadowDemo in the StaticEffects project by Romain Guy.
 * 
 * @author Poul Henriksen
 */
public class ShadowRenderer
{

    /**
     * <p>A shadow renderer needs three properties to generate shadows.
     * These properties are:</p> 
     * <ul>
     *   <li><i>size</i>: The size, in pixels, of the shadow. This property also
     *   defines the fuzzyness.</li>
     *   <li><i>opacity</i>: The opacity, between 0.0 and 1.0, of the shadow.</li>
     *   <li><i>color</i>: The color of the shadow. Shadows are not meant to be
     *   black only.</li>
     * </ul>
     * @param image the image to create a drop shadow for.
     * @param size the size of the shadow in pixels. Defines the fuzziness.
     * @param opacity the opacity of the shadow.
     * @param color the color of the shadow.
     */
    public static BufferedImage createDropShadow(BufferedImage image, int size, float opacity, Color color)
    {
        BufferedImage shadow = new BufferedImage(image.getWidth() + 4 * size, image.getHeight() + 4 * size,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = shadow.createGraphics();
        g2.drawImage(image, size * 2, size * 2, null);

        g2.setComposite(AlphaComposite.SrcIn);
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255f * opacity) ));
        g2.fillRect(0, 0, shadow.getWidth(), shadow.getHeight());

        g2.dispose();

        shadow = getGaussianBlurFilter(size, true).filter(shadow, null);
        shadow = getGaussianBlurFilter(size, false).filter(shadow, null);

        return shadow;
    }

    public static ConvolveOp getGaussianBlurFilter(int radius, boolean horizontal)
    {
        if (radius < 1) {
            throw new IllegalArgumentException("Radius must be >= 1");
        }

        int size = radius * 2 + 1;
        float[] data = new float[size];

        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0.0f;

        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            int index = i + radius;
            data[index] = (float) Math.exp(-distance / twoSigmaSquare) / sigmaRoot;
            total += data[index];
        }

        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }

        Kernel kernel = null;
        if (horizontal) {
            kernel = new Kernel(size, 1, data);
        }
        else {
            kernel = new Kernel(1, size, data);
        }
        return new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
    }
}
