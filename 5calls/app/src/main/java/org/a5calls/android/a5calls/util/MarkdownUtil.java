package org.a5calls.android.a5calls.util;

import android.content.Context;
import android.widget.TextView;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import ru.noties.markwon.Markwon;
import ru.noties.markwon.SpannableConfiguration;
import ru.noties.markwon.renderer.SpannableRenderer;

/**
 * Shim to get around LinkMovementMethod while using Markwon.
 */
public class MarkdownUtil {
    /**
     * Temporary work-around for Markwon.setMarkdown(view, script); while LinkMovementMethod
     * crashes during selection.
     * See https://github.com/noties/Markwon/issues/41
     * and https://github.com/noties/Markwon/tree/v1.0.6#quick-start.
     */
    public static void setUpScript(TextView view, String script, Context context) {
        // create a Parser instance (can be done manually)
        // internally creates default Parser instance & registers `strike-through` & `tables` extension
        final Parser parser = Markwon.createParser();

        // core class to display markdown, can be obtained via this method,
        // which creates default instance (no images handling though),
        // or via `builder` method, which lets you to configure this instance
        //
        // `this` refers to a Context instance
        final SpannableConfiguration configuration = SpannableConfiguration.create(context);

        // it's better **not** to re-use this class between multiple calls
        final SpannableRenderer renderer = new SpannableRenderer();

        final Node node = parser.parse(script);
        final CharSequence text = renderer.render(configuration, node);

        // for links in markdown to be clickable
        view.setMovementMethod(BetterLinkMovementMethod.getInstance());

        // we need these due to the limited nature of Spannables to invalidate TextView
        Markwon.unscheduleDrawables(view);
        Markwon.unscheduleTableRows(view);

        view.setText(text);

        Markwon.scheduleDrawables(view);
        Markwon.scheduleTableRows(view);
    }
}
