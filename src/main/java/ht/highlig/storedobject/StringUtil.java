package ht.highlig.storedobject;

import android.app.Activity;
import android.net.Uri;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by revant on 3/8/14.
 */
public class StringUtil {
    public static boolean isEmpty(String s) {
        return (s == null || s.length() == 0);
    }

    public static boolean hasData(String s) {
        return !isEmpty(s);
    }

    public static final String EMPTY = "";
    private static final int PAD_LIMIT = 8192;

    /**
     * Efficiently concatenates the string representations of the
     * specified objects.
     *
     * @param chunks the chunks to be concatenated
     * @return the result of the concatenation
     */
    public static String concat(Object... chunks) {
        return join(null, chunks);
    }

    /**
     * Efficiently combines the string representations of the specified
     * objects, delimiting each chunk with the specified delimiter.
     *
     * @param delimiter the specified delimiter
     * @param chunks the chunks to be combined
     * @return the result of the concatenation
     */
    public static String join(String delimiter, Object... chunks) {

        //It's worth doing two passes here to avoid additional allocations, by
        //being more accurate in size estimation
        int nChunks = chunks.length;
        int delimLength = delimiter == null ? 0 : delimiter.length();
        int estimate = delimLength * (nChunks - 1);
        for (Object chunk : chunks) {
            if (chunk != null) {
                estimate += chunk instanceof CharSequence
                        ? ((CharSequence) chunk).length()
                        : 10; /* why not? */
            }
        }

        StringBuilder sb = new StringBuilder(estimate);
        for (int i = 0; i < nChunks; i++) {
            Object chunk = chunks[i];
            if (chunk != null) {
                String string = chunk.toString();
                if (string != null && string.length() > 0) {
                    sb.append(string);
                    if ((i + 1) < nChunks && delimiter != null) {
                        sb.append(delimiter);
                    }
                }
            }
        }

        return sb.toString();
    }

    public static void setHtml(
            Activity activity, View parent, int elementId, int resId) {
        TextView view = (TextView) parent.findViewById(elementId);
        view.setText(Html.fromHtml(activity.getString(resId)));
    }

    public static Uri getUri(Object... chunks) {
        return Uri.parse(concat(chunks));
    }

    public static String strip(String str, String stripChars) {
        if (isEmpty(str)) {
            return str;
        }
        str = stripStart(str, stripChars);
        return stripEnd(str, stripChars);
    }

    public static String stripStart(String str, String stripChars) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        int start = 0;
        if (stripChars == null) {
            while ((start != strLen) && Character.isWhitespace(str.charAt(start))) {
                start++;
            }
        } else if (stripChars.length() == 0) {
            return str;
        } else {
            while ((start != strLen) && (stripChars.indexOf(str.charAt(start)) != -1)) {
                start++;
            }
        }
        return str.substring(start);
    }

    public static String stripEnd(String str, String stripChars) {
        int end;
        if (str == null || (end = str.length()) == 0) {
            return str;
        }

        if (stripChars == null) {
            while ((end != 0) && Character.isWhitespace(str.charAt(end - 1))) {
                end--;
            }
        } else if (stripChars.length() == 0) {
            return str;
        } else {
            while ((end != 0) && (stripChars.indexOf(str.charAt(end - 1)) != -1)) {
                end--;
            }
        }
        return str.substring(0, end);
    }

    /**
     * <p>Repeat a String {@code repeat} times to form a
     * new String.</p>
     *
     * <pre>
     * StringUtils.repeat(null, 2) = null
     * StringUtils.repeat("", 0)   = ""
     * StringUtils.repeat("", 2)   = ""
     * StringUtils.repeat("a", 3)  = "aaa"
     * StringUtils.repeat("ab", 2) = "abab"
     * StringUtils.repeat("a", -2) = ""
     * </pre>
     *
     * @param str  the String to repeat, may be null
     * @param repeat  number of times to repeat str, negative treated as zero
     * @return a new String consisting of the original String repeated,
     *  {@code null} if null String input
     */
    public static String repeat(String str, int repeat) {
        // Performance tuned for 2.0 (JDK1.4)

        if (str == null) {
            return null;
        }
        if (repeat <= 0) {
            return EMPTY;
        }
        int inputLength = str.length();
        if (repeat == 1 || inputLength == 0) {
            return str;
        }
        if (inputLength == 1 && repeat <= PAD_LIMIT) {
            return repeat(str.charAt(0), repeat);
        }

        int outputLength = inputLength * repeat;
        switch (inputLength) {
            case 1 :
                return repeat(str.charAt(0), repeat);
            case 2 :
                char ch0 = str.charAt(0);
                char ch1 = str.charAt(1);
                char[] output2 = new char[outputLength];
                for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
                    output2[i] = ch0;
                    output2[i + 1] = ch1;
                }
                return new String(output2);
            default :
                StringBuilder buf = new StringBuilder(outputLength);
                for (int i = 0; i < repeat; i++) {
                    buf.append(str);
                }
                return buf.toString();
        }
    }

    /**
     * <p>Repeat a String {@code repeat} times to form a
     * new String, with a String separator injected each time. </p>
     *
     * <pre>
     * StringUtils.repeat(null, null, 2) = null
     * StringUtils.repeat(null, "x", 2)  = null
     * StringUtils.repeat("", null, 0)   = ""
     * StringUtils.repeat("", "", 2)     = ""
     * StringUtils.repeat("", "x", 3)    = "xxx"
     * StringUtils.repeat("?", ", ", 3)  = "?, ?, ?"
     * </pre>
     *
     * @param str        the String to repeat, may be null
     * @param separator  the String to inject, may be null
     * @param repeat     number of times to repeat str, negative treated as zero
     * @return a new String consisting of the original String repeated,
     *  {@code null} if null String input
     * @since 2.5
     */
    public static String repeat(String str, String separator, int repeat) {
        if(str == null || separator == null) {
            return repeat(str, repeat);
        } else {
            // given that repeat(String, int) is quite optimized, better to rely on it than try and splice this into it
            String result = repeat(str + separator, repeat);
            return removeEnd(result, separator);
        }
    }

    /**
     * <p>Returns padding using the specified delimiter repeated
     * to a given length.</p>
     *
     * <pre>
     * StringUtils.repeat(0, 'e')  = ""
     * StringUtils.repeat(3, 'e')  = "eee"
     * StringUtils.repeat(-2, 'e') = ""
     * </pre>
     *
     * <p>Note: this method doesn't not support padding with
     * <a href="http://www.unicode.org/glossary/#supplementary_character">Unicode Supplementary Characters</a>
     * as they require a pair of {@code char}s to be represented.
     * If you are needing to support full I18N of your applications
     * consider using {@link #repeat(String, int)} instead.
     * </p>
     *
     * @param ch  character to repeat
     * @param repeat  number of times to repeat char, negative treated as zero
     * @return String with repeated character
     * @see #repeat(String, int)
     */
    public static String repeat(char ch, int repeat) {
        char[] buf = new char[repeat];
        for (int i = repeat - 1; i >= 0; i--) {
            buf[i] = ch;
        }
        return new String(buf);
    }

    /**
     * <p>Removes a substring only if it is at the end of a source string,
     * otherwise returns the source string.</p>
     *
     * <p>A {@code null} source string will return {@code null}.
     * An empty ("") source string will return the empty string.
     * A {@code null} search string will return the source string.</p>
     *
     * <pre>
     * StringUtils.removeEnd(null, *)      = null
     * StringUtils.removeEnd("", *)        = ""
     * StringUtils.removeEnd(*, null)      = *
     * StringUtils.removeEnd("www.domain.com", ".com.")  = "www.domain.com"
     * StringUtils.removeEnd("www.domain.com", ".com")   = "www.domain"
     * StringUtils.removeEnd("www.domain.com", "domain") = "www.domain.com"
     * StringUtils.removeEnd("abc", "")    = "abc"
     * </pre>
     *
     * @param str  the source String to search, may be null
     * @param remove  the String to search for and remove, may be null
     * @return the substring with the string removed if found,
     *  {@code null} if null String input
     * @since 2.1
     */
    public static String removeEnd(String str, String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        if (str.endsWith(remove)) {
            return str.substring(0, str.length() - remove.length());
        }
        return str;
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    // from http://stackoverflow.com/a/481836/759772
    public static int nullSafeStringComparator(final String one, final String two) {
        if (one == null ^ two == null) {
            return (one == null) ? -1 : 1;
        }

        // Both are null
        if (one == null) {
            return 0;
        }

        return one.compareTo(two);
    }

    public static void setText(TextView view, String text) {
        if (isEmpty(text)) {
            view.setVisibility(View.GONE);
            return;
        }

        view.setText(text);
        view.setVisibility(View.VISIBLE);
    }

    private static String markupToHTML(String markup) {
        if (markup == null) {
            return "";
        }
        // replace <username> tag
        markup = markup.replace("<username>", "<font color='#0079b8'><b>");

        // replace </username> tag
        markup = markup.replace("</username>", "</b></font>");

        return markup;
    }

    public static void setHtml(TextView view, String html) {
        view.setVisibility(View.GONE);

        if (html != null && html.length() != 0) {
            view.setText(Html.fromHtml(html));
            view.setVisibility(View.VISIBLE);
        }
    }

    public static void setMarkup(TextView view, String markup) {
        String html = markupToHTML(markup);
        setHtml(view, html);
    }

    public static void setNumber(TextView view, Integer num) {
        view.setVisibility(View.GONE);

        if (num != null && num > 0) {
            view.setText(Integer.toString(num));
            view.setVisibility(View.VISIBLE);
        }
    }

    public static Spannable makeColoredString(String text, int color) {
        Spannable result = new SpannableString(text);
        result.setSpan(new ForegroundColorSpan(color), 0, text.length(),
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return result;
    }

    public static List<ArrayList<String>> chunkList(
            List<String> array, int chuckSize) {
        List<ArrayList<String>> chunks = new ArrayList<ArrayList<String>>();

        ArrayList<String> chunk = new ArrayList<String>();
        for (String anArray : array) {
            chunk.add(anArray);
            if (chunk.size() == chuckSize) {
                chunks.add(chunk);
                chunk = new ArrayList<String>();
            }
        }
        if (chunk.size() > 0) {
            chunks.add(chunk);
        }

        return chunks;
    }

}
