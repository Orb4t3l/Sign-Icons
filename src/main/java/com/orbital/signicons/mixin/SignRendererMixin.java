package com.orbital.signicons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.orbital.signicons.IconTextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(SignRenderer.class)
public class SignRendererMixin {

    @Redirect(
            method = "renderSignText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I"
            )
    )
    private int signicons$drawTextOrIcons(
            Font font,
            FormattedCharSequence text,
            float x,
            float y,
            int color,
            boolean dropShadow,
            Matrix4f matrix,
            MultiBufferSource bufferSource,
            Font.DisplayMode displayMode,
            int backgroundColor,
            int packedLight
    ) {
        StringBuilder rawBuilder = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            rawBuilder.appendCodePoint(codePoint);
            return true;
        });
        String raw = rawBuilder.toString();

        List<IconTextUtil.Segment> segments = IconTextUtil.parse(raw);
        if (segments == null) {
            return font.drawInBatch(text, x, y, color, dropShadow, matrix, bufferSource, displayMode, backgroundColor, packedLight);
        }

        Level level = Minecraft.getInstance().level;
        float cursorX = x;

        for (IconTextUtil.Segment segment : segments) {
            if (segment instanceof IconTextUtil.TextSegment textSeg) {
                if (!textSeg.text().isEmpty()) {
                    FormattedCharSequence part = FormattedCharSequence.forward(textSeg.text(), Style.EMPTY);
                    font.drawInBatch(part, cursorX, y, color, dropShadow, matrix, bufferSource, displayMode, backgroundColor, packedLight);
                    cursorX += font.width(textSeg.text());
                }
            } else if (segment instanceof IconTextUtil.IconSegment iconSeg) {
                float iconSize = font.lineHeight;

                PoseStack itemPoseStack = new PoseStack();
                itemPoseStack.last().pose().set(matrix);
                itemPoseStack.translate(cursorX / 64.0, y / 64.0, 0.0);
                itemPoseStack.scale(iconSize / 64.0f, iconSize / 64.0f, 1.0f);

                Minecraft.getInstance().getItemRenderer().renderStatic(
                        iconSeg.stack(),
                        ItemDisplayContext.FIXED,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        itemPoseStack,
                        bufferSource,
                        level,
                        0
                );
                cursorX += iconSize;
            }
        }

        return (int) cursorX;
    }
}