package com.orbital.signicons.mixin;

import com.orbital.signicons.IconTextUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(SignEditScreen.class)
public class SignEditScreenMixin {

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"
            ),
            require = 0
    )
    private int signicons$drawStringOrIcons(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color, boolean dropShadow) {
        return signicons$render(guiGraphics, font, text, x, y, color, dropShadow);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"
            ),
            require = 0
    )
    private int signicons$drawCenteredStringOrIcons(GuiGraphics guiGraphics, Font font, String text, int centerX, int y, int color) {
        List<IconTextUtil.Segment> segments = IconTextUtil.parse(text);
        if (segments == null) {
            return guiGraphics.drawCenteredString(font, text, centerX, y, color);
        }
        float totalWidth = signicons$measure(font, segments);
        int startX = (int) (centerX - totalWidth / 2.0f);
        return signicons$render(guiGraphics, font, text, startX, y, color, false);
    }

    private int signicons$render(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color, boolean dropShadow) {
        List<IconTextUtil.Segment> segments = IconTextUtil.parse(text);
        if (segments == null) {
            return guiGraphics.drawString(font, text, x, y, color, dropShadow);
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

        return (int) cursorX;
    }

    private float signicons$measure(Font font, List<IconTextUtil.Segment> segments) {
        float width = 0;
        float iconSize = font.lineHeight * 1.8f;
        for (IconTextUtil.Segment segment : segments) {
            if (segment instanceof IconTextUtil.TextSegment textSeg) {
                width += font.width(textSeg.text());
            } else {
                width += iconSize;
            }
        }
        return width;
    }
}