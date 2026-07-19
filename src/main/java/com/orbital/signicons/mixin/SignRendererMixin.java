package com.orbital.signicons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.orbital.signicons.IconTextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.SignText;
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
            method = "submitSignText(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Z)V",
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
            method = "submitSignText(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitText(Lcom/mojang/blaze3d/vertex/PoseStack;FFLnet/minecraft/util/FormattedCharSequence;ZLnet/minecraft/client/gui/Font$DisplayMode;IIII)V"
            )
    )
    private void signicons$submitTextOrIcons(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float x,
            float y,
            FormattedCharSequence text,
            boolean dropShadow,
            Font.DisplayMode displayMode,
            int lightCoords,
            int color,
            int backgroundColor,
            int outlineColor
    ) {
        StringBuilder rawBuilder = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            rawBuilder.appendCodePoint(codePoint);
            return true;
        });
        String raw = rawBuilder.toString();

        List<IconTextUtil.Segment> segments = IconTextUtil.parse(raw);
        if (segments == null) {
            collector.submitText(poseStack, x, y, text, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
            return;
        }

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
                    collector.submitText(poseStack, cursorX, y, part, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
                    cursorX += font.width(textSeg.text());
                }
            } else if (segment instanceof IconTextUtil.IconSegment iconSeg) {
                poseStack.pushPose();
                poseStack.translate(cursorX, y + iconSize * 0.12f, 0.02f);
                poseStack.scale(iconSize, -iconSize, iconSize * 0.02f);

                ItemStackRenderState state = new ItemStackRenderState();
                Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                        state,
                        iconSeg.stack(),
                        ItemDisplayContext.GUI,
                        Minecraft.getInstance().level,
                        null,
                        0
                );
                state.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);

                poseStack.popPose();
                cursorX += iconSize * ICON_ADVANCE_MULTIPLIER;
            }
        }
    }
}