package org.a5calls.android.a5calls.util;

import android.content.Context;
import android.text.style.StrikethroughSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.RenderProps;
import io.noties.markwon.SpanFactory;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.simple.ext.SimpleExtPlugin;

public class MarkdownUtil {
    /**
     * Set up markwon with strikethrough using ~ or ~~.
     */
    public static void setUpScript(TextView view, String text, Context context) {
        final Markwon markwon = Markwon.builder(context)
                // Strikethrough with ~~
                .usePlugin(StrikethroughPlugin.create())
                // Strikethrough with ~
                .usePlugin(SimpleExtPlugin.create(plugin -> plugin
                        .addExtension(1, '~', new SpanFactory() {
                            @Override
                            public Object getSpans(
                                    @NonNull MarkwonConfiguration configuration,
                                    @NonNull RenderProps props) {
                                return new StrikethroughSpan();
                            }
                        })))
                .build();
        markwon.setMarkdown(view, text);
    }
}
