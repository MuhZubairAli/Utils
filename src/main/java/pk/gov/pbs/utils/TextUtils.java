package pk.gov.pbs.utils;

import android.text.Html;
import android.text.Spanned;

public class TextUtils {
    /**
     * This method helps creating unordered list from array of strings
     * bullet param specifies bullet style, acceptable characters list could
     * be found at <a href="https://www.w3schools.com/html/html_symbols.asp">Bullet Character</a>
     * @param bullet bullet style as character code
     * @param strings strings to be converted to <ul></ul>
     * @return Spanned text which could be directly set using TextView.setText()
     */
    public static Spanned makeUnorderedList(String bullet, String[] strings) {
        StringBuilder sb = new StringBuilder();
        if (strings != null && strings.length > 0) {
            for (String str : strings){
                if (str != null)
                    sb.append(bullet).append(str).append("<br />");
            }
        }
        return Html.fromHtml(sb.toString());
    }

    public static Spanned makeUnorderedList(String... strings){
        return makeUnorderedList("&#8226; ", strings);
    }
}
