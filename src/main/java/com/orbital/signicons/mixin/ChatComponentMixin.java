package com.orbital.signicons.mixin;

import com.orbital.signicons.IconTextUtil;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow private boolean newMessageSinceScroll;

    @Shadow private boolean isChatHidden() { return false; }
    @Shadow public abstract int getWidth();
    @Shadow public abstract double getScale();
    @Shadow public abstract int getLinesPerPage();
    @Shadow private int getLineHeight() { return 0; }
    @Shadow private double screenToChatX(double x) { return 0; }
    @Shadow private double screenToChatY(double y) { return 0; }
    @Shadow private int getMessageEndIndexAt(double x, double y) { return -1; }
    @Shadow private int getTagIconLeft(GuiMessage.Line line) { return 0; }
    @Shadow private void drawTagIcon(GuiGraphics guiGraphics, int x, int y, GuiMessageTag.Icon icon) {}
    @Shadow private static double getTimeFactor(int age) { return 0; }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void signicons$render(GuiGraphics guiGraphics, int guiTicks, int mouseX, int mouseY, boolean focusedParam, CallbackInfo ci) {
        if (this.isChatHidden()) {
            ci.cancel();
            return;
        }

        int linesPerPage = this.getLinesPerPage();
        int totalLines = this.trimmedMessages.size();
        if (totalLines <= 0) {
            ci.cancel();
            return;
        }

        boolean focused = this.minecraft.screen instanceof net.minecraft.client.gui.screens.ChatScreen;
        float scale = (float) this.getScale();
        int chatWidth = Mth.ceil((float) this.getWidth() / scale);
        int guiHeight = guiGraphics.guiHeight();
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.pose().translate(4.0F, 0.0F);
        int bottomY = Mth.floor((float) (guiHeight - 40) / scale);
        int clickedEndIndex = this.getMessageEndIndexAt(this.screenToChatX(mouseX), this.screenToChatY(mouseY));
        double chatOpacity = this.minecraft.options.chatOpacity().get() * 0.9F + 0.1F;
        double bgOpacity = this.minecraft.options.textBackgroundOpacity().get();
        double lineSpacing = this.minecraft.options.chatLineSpacing().get();
        int lineHeight = this.getLineHeight();
        int textYOffset = (int) Math.round(-8.0D * (lineSpacing + 1.0D) + 4.0D * lineSpacing);
        int visibleCount = 0;

        for (int i = 0; i + this.chatScrollbarPos < this.trimmedMessages.size() && i < linesPerPage; ++i) {
            int index = i + this.chatScrollbarPos;
            GuiMessage.Line line = this.trimmedMessages.get(index);
            if (line == null) continue;

            int age = guiTicks - line.addedTime();
            if (age < 200 || focused) {
                double factor = focused ? 1.0D : getTimeFactor(age);
                int textAlpha = (int) (255.0D * factor * chatOpacity);
                int bgAlpha = (int) (255.0D * factor * bgOpacity);
                ++visibleCount;

                if (textAlpha > 3) {
                    int lineTopY = bottomY - i * lineHeight;
                    int textY = lineTopY + textYOffset;
                    guiGraphics.pose().pushMatrix();
                    guiGraphics.fill(-4, lineTopY - lineHeight, chatWidth + 4 + 4, lineTopY, bgAlpha << 24);
                    GuiMessageTag tag = line.tag();
                    if (tag != null) {
                        int tagColor = tag.indicatorColor() | textAlpha << 24;
                        guiGraphics.fill(-4, lineTopY - lineHeight, -2, lineTopY, tagColor);
                        if (index == clickedEndIndex && tag.icon() != null) {
                            int iconLeft = this.getTagIconLeft(line);
                            int iconY = textY + 9;
                            this.drawTagIcon(guiGraphics, iconLeft, iconY, tag.icon());
                        }
                    }

                    signicons$drawLine(guiGraphics, this.minecraft.font, line.content(), 0, textY, 16777215 + (textAlpha << 24));
                    guiGraphics.pose().popMatrix();
                }
            }
        }

        long queueSize = this.minecraft.getChatListener().queueSize();
        if (queueSize > 0L) {
            int queueTextAlpha = (int) (128.0D * chatOpacity);
            int queueBgAlpha = (int) (255.0D * bgOpacity);
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(0.0F, (float) bottomY);
            guiGraphics.fill(-2, 0, chatWidth + 4, 9, queueBgAlpha << 24);
            guiGraphics.drawString(this.minecraft.font, net.minecraft.network.chat.Component.translatable("chat.queue", queueSize), 0, 1, 16777215 + (queueTextAlpha << 24));
            guiGraphics.pose().popMatrix();
        }

        if (focused) {
            int lh = this.getLineHeight();
            int totalHeight = totalLines * lh;
            int visibleHeight = visibleCount * lh;
            int scrollY = this.chatScrollbarPos * visibleHeight / totalLines - bottomY;
            int barHeight = visibleHeight * visibleHeight / totalHeight;
            if (totalHeight != visibleHeight) {
                int alpha = scrollY > 0 ? 170 : 96;
                int barColor = this.newMessageSinceScroll ? 13382451 : 3355562;
                int barX = chatWidth + 4;
                guiGraphics.fill(barX, -scrollY, barX + 2, -scrollY - barHeight, barColor + (alpha << 24));
                guiGraphics.fill(barX + 2, -scrollY, barX + 1, -scrollY - barHeight, 13421772 + (alpha << 24));
            }
        }

        guiGraphics.pose().popMatrix();
        ci.cancel();
    }

    private static void signicons$drawLine(GuiGraphics guiGraphics, Font font, FormattedCharSequence sequence, int x, int y, int color) {
        StringBuilder rawBuilder = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            rawBuilder.appendCodePoint(codePoint);
            return true;
        });
        String raw = rawBuilder.toString();

        List<IconTextUtil.Segment> segments = IconTextUtil.parse(raw);
        if (segments == null) {
            guiGraphics.drawString(font, sequence, x, y, color);
            return;
        }

        float cursorX = x;
        float iconSize = font.lineHeight * 0.9f;
        float iconAdvance = iconSize * 1.15f;

        for (IconTextUtil.Segment segment : segments) {
            if (segment instanceof IconTextUtil.TextSegment textSeg) {
                if (!textSeg.text().isEmpty()) {
                    guiGraphics.drawString(font, textSeg.text(), (int) cursorX, y, color);
                    cursorX += font.width(textSeg.text());
                }
            } else if (segment instanceof IconTextUtil.IconSegment iconSeg) {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(cursorX, y - iconSize / 10.0f);
                float scale = iconSize / 16.0f;
                guiGraphics.pose().scale(scale, scale);
                guiGraphics.renderItem(iconSeg.stack(), 0, 0);
                guiGraphics.pose().popMatrix();
                cursorX += iconAdvance;
            }
        }
    }
}