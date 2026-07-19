package com.orbital.signicons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.orbital.signicons.IconTextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignText;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Function;

@Mixin(AbstractSignRenderer.class)
public class SignRendererMixin {

    private static final float ICON_SIZE_MULTIPLIER = 0.9f;
    private static final float ICON_ADVANCE_MULTIPLIER = 1.15f;

    @Shadow @Final private Font font;

    @Redirect(
            method = "renderSignText(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SignText;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIIZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/SignText;getRenderMessages(ZLjava/util/function/Function;)[Lnet/minecraft/util/FormattedCharSequence;"
            )
    )
    private FormattedCharSequence[] signicons$getRenderMessages(SignText signText, boolean filtered, Function<Component, FormattedCharSequence> splitter) {
        return signText.getRenderMessages(filtered, component -> {
            if (IconTextUtil.parse(component.getString()) != null) {
                List<FormattedCharSequence> list = this.font.split(component, Integer.MAX_VALUE / 2);
                return list.isEmpty() ? FormattedCharSequence.EMPTY : list.get(0);
            }
            return splitter.apply(component);
        });
    }

    @Redirect(
            method = "renderSignText(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SignText;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIIZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"
            )
    )
    private void signicons$drawTextOrIcons(
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
            font.drawInBatch(text, x, y, color, dropShadow, matrix, bufferSource, displayMode, backgroundColor, packedLight);
            return;
        }

        Level level = Minecraft.getInstance().level;
        float iconSize = font.lineHeight * ICON_SIZE_MULTIPLIER;

        float totalWidth = 0;
        for (IconTextUtil.Segment segment : segments) {
            if (segment instanceof IconTextUtil.TextSegment textSeg) {
                totalWidth += font.width(textSeg.text());
            } else {
                totalWidth += iconSize * ICON_ADVANCE_MULTIPLIER;
            }
        }
        float cursorX = -totalWidth / 2.0f;

        for (IconTextUtil.Segment segment : segments) {
            if (segment instanceof IconTextUtil.TextSegment textSeg) {
                if (!textSeg.text().isEmpty()) {
                    FormattedCharSequence part = FormattedCharSequence.forward(textSeg.text(), Style.EMPTY);
                    font.drawInBatch(part, cursorX, y, color, dropShadow, matrix, bufferSource, displayMode, backgroundColor, packedLight);
                    cursorX += font.width(textSeg.text());
                }
            } else if (segment instanceof IconTextUtil.IconSegment iconSeg) {
                PoseStack itemPoseStack = new PoseStack();
                itemPoseStack.last().pose().set(matrix);
                itemPoseStack.translate(cursorX, y + iconSize * 0.12f, 0.02f);
                itemPoseStack.scale(iconSize, -iconSize, iconSize * 0.02f);

                Minecraft.getInstance().getItemRenderer().renderStatic(
                        iconSeg.stack(),
                        ItemDisplayContext.GUI,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        itemPoseStack,
                        bufferSource,
                        level,
                        0
                );
                cursorX += iconSize * ICON_ADVANCE_MULTIPLIER;
            }
        }
    }
}