package pk.gov.pbs.utils;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import java.util.Locale;

public class ThemeUtils {
    protected static Typeface mUrduFontFace;

    public static Typeface getUrduFontTypeFace(Context context) {
        if (mUrduFontFace == null)
            mUrduFontFace = Typeface.createFromAsset(context.getAssets(),
                    "fonts/Mehr_Nastaliq_Web_v.2.0.ttf");
        return mUrduFontFace;
    }

    /**
     * setup text style based on locale like Font, Size (theme independent(
     * @param locale locale of the app, specified in LabelProvider constructor
     * @param textViews subjects to which styles are to be applied
     */
    public static void setupTextViewStylesByLocale(Locale locale, TextView... textViews){
        if (textViews.length > 0) {
            if (locale.getLanguage().equalsIgnoreCase("ur"))
                ThemeUtils.applyUrduFontTypeFace(textViews);
        }
    }

    public static void applyUrduFontTypeFace(TextView... textViews){
        if (mUrduFontFace == null)
            getUrduFontTypeFace(textViews[0].getContext());

        for (TextView tv : textViews)
            tv.setTypeface(mUrduFontFace);
    }

    public static void applyThemedDrawableToView(View view, int resId){
        Context context = view.getContext();
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[] {resId});
        int attributeResourceId = a.getResourceId(0, 0);
        Drawable drawable = context.getResources().getDrawable(attributeResourceId);
        view.setBackground(drawable);
        a.recycle();
    }

    public static Drawable getDrawableByTheme(Context context, int resId) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[] {resId});
        int attributeResourceId = a.getResourceId(0, 0);
        Drawable drawable = context.getResources().getDrawable(attributeResourceId);
        a.recycle();
        return drawable;
    }

    public static int getColorByTheme(Context context, int resID){
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(resID, typedValue, true);
        @ColorInt int color = typedValue.data;
        return color;
    }
}
