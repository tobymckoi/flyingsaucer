package org.xhtmlrenderer.css.value;

import org.xhtmlrenderer.css.constants.IdentValue;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: tobe
 * Date: 2005-jun-23
 * Time: 00:28:43
 * To change this template use File | Settings | File Templates.
 */
public class FontSpecification {
    private float size;
    private IdentValue fontWeight;
    private String[] families;
    private IdentValue fontStyle;
    private IdentValue variant;
    private Boolean kerning;
    private Boolean ligatures;

    public FontSpecification(String[] families, float size, IdentValue fontWeight, IdentValue fontStyle, IdentValue variant, Boolean kerning, Boolean ligatures) {
        this.families = families;
        this.size = size;
        this.fontWeight = fontWeight;
        this.fontStyle = fontStyle;
        this.variant = variant;
        this.kerning = kerning;
        this.ligatures = ligatures;
    }

    public FontSpecification(String[] families, float size, IdentValue fontWeight, IdentValue fontStyle, IdentValue variant) {
        this(families, size, fontWeight, fontStyle, variant, null, null);
    }

    public float getSize() {
        return size;
    }

    public IdentValue getFontWeight() {
        return fontWeight;
    }

    public String[] getFamilies() {
        return families;
    }

    public IdentValue getFontStyle() {
        return fontStyle;
    }

    public IdentValue getVariant() {
        return variant;
    }

    public Boolean isKerning() {
        return kerning;
    }

    public Boolean isLigatures() {
        return ligatures;
    }

    /**
     * Returns a FontSpecification that inherits the new kerning and ligatures
     * settings. If this font specification already defines kerning or ligatures
     * then preserve those settings.
     * 
     * @param newKerning
     * @param newLigatures
     * @return 
     */
    public FontSpecification deriveWithKerningAndLigatures(Boolean newKerning, Boolean newLigatures) {
        // If nothing being changed,
        if (newKerning == null && newLigatures == null) {
            return this;
        }
        // If this kerning and ligatures both not set then inherit the new
        // settings.
        if (kerning == null && ligatures == null) {
            return new FontSpecification(families, size, fontWeight, fontStyle, variant, newKerning, newLigatures);
        }
        // If ligatures is set but not kerning inherit the kerning
        else if (kerning == null) {
            return new FontSpecification(families, size, fontWeight, fontStyle, variant, newKerning, ligatures);
        }
        // If kerning is set but not ligatures inherit the ligatures
        else if (ligatures == null) {
            return new FontSpecification(families, size, fontWeight, fontStyle, variant, kerning, newLigatures);
        }
        // Otherwise kerning and ligatures already defined so return this.
        return this;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("Font specification: ");
        sb
                .append(" families: " + Arrays.asList(families).toString())
                .append(" size: " + size)
                .append(" weight: " + fontWeight)
                .append(" style: " + fontStyle)
                .append(" variant: " + variant)
                .append(" kerning: " + kerning)
                .append(" ligatures: " + ligatures);
        return sb.toString();
    }

}
