package com.skide.utils;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StyleSpanMerger {


    public static StyleSpans<Collection<String>> merge(@Nullable StyleSpans<Collection<String>>
                                                               spans, int lineLength, int offset, int styleLength, String cssClass) {
        if (spans != null) {
            StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder();
            builder.add(Collections.emptyList(), offset);
            builder.add(Collections.singletonList(cssClass), styleLength);
            builder.add(Collections.emptyList(), lineLength - offset - styleLength);
            StyleSpans<Collection<String>> spansToGoOnTop = builder.create();
            spans = spans.overlay(spansToGoOnTop, (bottomSpan, list) -> {
                List<String> l = new ArrayList<>(bottomSpan.size() + list.size());
                l.addAll(bottomSpan);
                l.addAll(list);
                return l;
            });
        }

        return spans;
    }

}
