package com.villageroverhaul.traderework.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Grey copies of existing textures, made on the client when first drawn (2026-09-28, villager statues): the
 * source is loaded from the resource packs like any texture and every pixel turned into its brightness, pulled
 * towards middle grey by CONTRAST and shifted by a little white noise (NOISE), alpha kept. Nothing of
 * Vanilla's art ships with the mod, and resource packs that change villagers change the statues too - the
 * texture manager reloads these with all others (ReloadableTexture).
 *
 * Render thread only (called while submitting a frame).
 */
public final class GrayscaleTextures {

    /** Contrast of the grey copies around middle grey: 0.55 = 45 % less than the source (Tweak-Werte.md) - reads as smoother stone. */
    public static final float CONTRAST = 0.55F;
    /** White noise on top, per pixel up to this share of the full 0-255 range up or down (Tweak-Werte.md) - a grainy stone surface. */
    public static final float NOISE = 0.03F;
    private static final float MIDDLE_GREY = 128.0F;

    private static final Set<Identifier> REGISTERED = new HashSet<>();

    private GrayscaleTextures() {
    }

    /** The grey copy of source, registered on first use. */
    public static Identifier of(Identifier source) {
        Identifier grey = Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "grayscale/" + source.getNamespace() + "/" + source.getPath());
        if (REGISTERED.add(grey)) {
            Minecraft.getInstance().getTextureManager().registerAndLoad(grey, new GrayscaleTexture(grey, source));
        }
        return grey;
    }

    private static final class GrayscaleTexture extends ReloadableTexture {

        private final Identifier source;

        GrayscaleTexture(Identifier id, Identifier source) {
            super(id);
            this.source = source;
        }

        @Override
        public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
            TextureContents contents = TextureContents.load(resourceManager, source);
            NativeImage image = contents.image();
            // Seeded by the source, so a texture gets the same grain after every resource reload.
            RandomSource random = RandomSource.create(source.hashCode());
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getPixel(x, y);
                    // Perceived brightness (Rec. 601), so a red coat and a green one don't turn the same grey.
                    float brightness = 0.299F * ARGB.red(pixel) + 0.587F * ARGB.green(pixel) + 0.114F * ARGB.blue(pixel);
                    float noise = (random.nextFloat() * 2.0F - 1.0F) * NOISE * 255.0F;
                    int grey = Math.clamp(Math.round(MIDDLE_GREY + (brightness - MIDDLE_GREY) * CONTRAST + noise), 0, 255);
                    image.setPixel(x, y, ARGB.color(ARGB.alpha(pixel), grey, grey, grey));
                }
            }
            return contents;
        }
    }
}
