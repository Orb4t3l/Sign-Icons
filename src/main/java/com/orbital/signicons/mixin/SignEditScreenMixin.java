package com.orbital.signicons.mixin;

import com.orbital.signicons.IconTextUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(AbstractSignEditScreen.class)
public class SignEditScreenMixin {

    @Redirect(
            method = "renderSignText(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
                    ordinal = 0
            )
    )
    private void signicons$drawStringOrIcons(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color, boolean dropShadow) {
        List<IconTextUtil.Segment> segments = IconTextUtil.parse(text);
        if (segments == null) {
            guiGraphics.drawString(font, text, x, y, color, dropShadow);
            return;
        }

        float cursorX = x;
        float iconSize = font.lineHeight * 1.8f;

        for (IconTextUtil.Segment segment : segments) {
            if (segment instanceof IconTextUtil.TextSegment textSeg) {
                if (!textSeg.text().isEmpty()) {
                    guiGraphics.drawString(font, textSeg.text(), (int) cursorX, y, color, dropShadow);
                    cursorX += font.width(textSeg.text());
                }
            } else if (segment instanceof IconTextUtil.IconSegment iconSeg) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(cursorX, y - iconSize / 4.0f, 0.0f);
                float scale = iconSize / 16.0f;
                guiGraphics.pose().scale(scale, scale, 1.0f);
                guiGraphics.renderItem(iconSeg.stack(), 0, 0);
                guiGraphics.pose().popPose();
                cursorX += iconSize;
            }
        }
    }
}