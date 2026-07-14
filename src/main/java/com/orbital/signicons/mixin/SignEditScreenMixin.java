package com.orbital.signicons.mixin;

import com.orbital.signicons.IconTextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractSignEditScreen.class)
public abstract class SignEditScreenMixin {

    @Shadow @Final private SignBlockEntity sign;
    @Shadow private SignText text;
    @Shadow @Final private String[] messages;
    @Shadow private int frame;
    @Shadow private int line;
    @Shadow private TextFieldHelper signField;
    @Shadow
    protected abstract Vector3f getSignTextScale();

    @Inject(method = "renderSignText(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void signicons$renderSignText(GuiGraphics guiGraphics, CallbackInfo ci) {
        Font font = Minecraft.getInstance().font;
        guiGraphics.pose().translate(0.0F, 0.0F, 4.0F);
        Vector3f scaleVec = this.getSignTextScale();
        guiGraphics.pose().scale(scaleVec.x(), scaleVec.y(), scaleVec.z());

        int color = this.text.getColor().getTextColor();
        boolean blink = this.frame / 6 % 2 == 0;
        int cursorPos = this.signField.getCursorPos();
        int selectionPos = this.signField.getSelectionPos();
        int lineHeight = this.sign.getTextLineHeight();
        int halfBlock = 4 * lineHeight / 2;
        int cursorLineY = this.line * lineHeight - halfBlock;

        for (int i = 0; i < this.messages.length; ++i) {
            String msg = this.messages[i];
            if (msg == null) continue;

            if (font.isBidirectional()) {
                msg = font.bidirectionalShaping(msg);
            }

            int lineWidth = signicons$width(font, msg);
            int startX = -lineWidth / 2;
            int y = i * lineHeight - halfBlock;

            signicons$drawLine(guiGraphics, font, msg, startX, y, color, false);

            if (i == this.line && cursorPos >= 0 && blink) {
                int cursorX = signicons$width(font, msg.substring(0, Math.max(Math.min(cursorPos, msg.length()), 0))) - lineWidth / 2;
                if (cursorPos >= msg.length()) {
                    guiGraphics.drawString(font, "_", cursorX, cursorLineY, color, false);
                }
            }
        }

        for (int i = 0; i < this.messages.length; ++i) {
            String msg = this.messages[i];
            if (msg == null || i != this.line || cursorPos < 0) continue;

            int lineWidth = signicons$width(font, msg);
            int cursorX = signicons$width(font, msg.substring(0, Math.max(Math.min(cursorPos, msg.length()), 0))) - lineWidth / 2;

            if (blink && cursorPos < msg.length()) {
                guiGraphics.fill(cursorX, cursorLineY - 1, cursorX + 1, cursorLineY + lineHeight, -16777216 | color);
            }

            if (selectionPos != cursorPos) {
                int lo = Math.min(cursorPos, selectionPos);
                int hi = Math.max(cursorPos, selectionPos);
                int loX = signicons$width(font, msg.substring(0, lo)) - lineWidth / 2;
                int hiX = signicons$width(font, msg.substring(0, hi)) - lineWidth / 2;
                int minX = Math.min(loX, hiX);
                int maxX = Math.max(loX, hiX);
                guiGraphics.fill(RenderType.guiTextHighlight(), minX, cursorLineY, maxX, cursorLineY + lineHeight, -16776961);
            }
        }

        ci.cancel();
    }

    private static final float ICON_SIZE_MULTIPLIER = 0.9f;
    private static final float ICON_ADVANCE_MULTIPLIER = 1.15f;

    private static int signicons$width(Font font, String text) {
        List<IconTextUtil.Segment> segments = IconTextUtil.parse(text);
        if (segments == null) return font.width(text);
        float width = 0;
        float iconSize = font.lineHeight * ICON_SIZE_MULTIPLIER;
        for (IconTextUtil.Segment segment : segments) {
            if (segment instanceof IconTextUtil.TextSegment t) {
                width += font.width(t.text());
            } else {
                width += iconSize * ICON_ADVANCE_MULTIPLIER;
            }
        }
        return (int) width;
    }

    private static void signicons$drawLine(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color, boolean dropShadow) {
        List<IconTextUtil.Segment> segments = IconTextUtil.parse(text);
        if (segments == null) {
            guiGraphics.drawString(font, text, x, y, color, dropShadow);
            return;
        }

        float cursorX = x;
        float iconSize = font.lineHeight * ICON_SIZE_MULTIPLIER;

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
                cursorX += iconSize * ICON_ADVANCE_MULTIPLIER;
            }
        }
    }
}