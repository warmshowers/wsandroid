package fi.bitrite.android.ws.util;

import android.text.Html;
import android.text.Spanned;

/**
 * General simple tools, mostly public methods.
 */
public class Tools {

    // Convert text ("About me" == Comments from user data) to form to add to TextView
    public static Spanned siteHtmlToHtml(String text) {
        return Html.fromHtml(text.replace("\n", "<br/>"));
    }
}
