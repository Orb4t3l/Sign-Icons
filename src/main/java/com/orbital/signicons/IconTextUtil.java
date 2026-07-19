package com.orbital.signicons;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IconTextUtil {

    private static final Pattern TOKEN = Pattern.compile(
            ":([a-z0-9_.\\-]+)(?::([a-z0-9_.\\-]+))?:"
    );

    private IconTextUtil() {}

    public sealed interface Segment permits TextSegment, IconSegment {}

    public record TextSegment(String text) implements Segment {}

    public record IconSegment(ItemStack stack) implements Segment {}

    public static List<Segment> parse(String rawLine) {
        Matcher matcher = TOKEN.matcher(rawLine);

        List<Segment> segments = null;
        int lastEnd = 0;

        while (matcher.find()) {
            String namespace = "minecraft";
            String path;
            if (matcher.group(2) != null) {
                namespace = matcher.group(1);
                path = matcher.group(2);
            } else {
                path = matcher.group(1);
            }

            Identifier id = Identifier.tryParse(namespace + ":" + path);
            if (id == null) continue;

            Item item = Registries.ITEM.get(id);
            if (item == null || item.equals(Items.AIR)) {
                continue;
            }

            if (segments == null) segments = new ArrayList<>();

            if (matcher.start() > lastEnd) {
                segments.add(new TextSegment(rawLine.substring(lastEnd, matcher.start())));
            }
            segments.add(new IconSegment(new ItemStack(item)));
            lastEnd = matcher.end();
        }

        if (segments == null) return null;

        if (lastEnd < rawLine.length()) {
            segments.add(new TextSegment(rawLine.substring(lastEnd)));
        }
        return segments;
    }
}