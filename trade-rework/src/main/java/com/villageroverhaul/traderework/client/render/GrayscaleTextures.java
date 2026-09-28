package com.villageroverhaul.traderework.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Grey copies of existing textures, made on the client when first drawn (2026-09-28, villager statues): the
 * source is loaded from the resource packs like any texture and every pixel turned into its brightness, alpha
 * kept. Nothing of Vanilla's art ships with the mod, and resource packs that change villagers change the
 * statues too - the texture manager reloads these with all others (ReloadableTexture).
 *
 * Render thread only (called while submitting a frame).
 */
public final class GrayscaleTextures {

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
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getPixel(x, y);
                    // Perceived brightness (Rec. 601), so a red coat and a green one don't turn the same grey.
                    int grey = Math.round(0.299F * ARGB.red(pixel) + 0.587F * ARGB.green(pixel) + 0.114F * ARGB.blue(pixel));
                    image.setPixel(x, y, ARGB.color(ARGB.alpha(pixel), grey, grey, grey));
                }
            }
            return contents;
        }
    }
}
