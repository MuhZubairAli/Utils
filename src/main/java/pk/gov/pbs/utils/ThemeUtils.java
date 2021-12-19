package pk.gov.pbs.utils;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;

public class ThemeUtils {
    public static void applyThemedDrawableToView(View view, int resId){
        Context context = view.getContext();
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[] {resId});
        int attributeResourceId = a.getResourceId(0, 0);
        Drawable drawable = context.getResources().getDrawable(attributeResourceId);
        view.setBackground(drawable);
        a.recycle();
    }

    public static int getColorByTheme(Context context, int resID){
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(resID, typedValue, true);
        @ColorInt int color = typedValue.data;
        return color;
    }
}
