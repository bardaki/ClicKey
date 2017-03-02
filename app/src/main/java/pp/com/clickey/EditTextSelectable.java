package pp.com.clickey;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;


/**
 * Created by baryariv on 24/02/2017.
 */

public class EditTextSelectable extends EditText {


    public EditTextSelectable(Context context) {
        super(context);
    }

    public EditTextSelectable(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextSelectable(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void onSelectionChanged(int selStart, int selEnd) {
        Log.e("Selection", "start: " + String.valueOf(selStart) + " end: " + String.valueOf(selEnd));
        setSelection(selStart);
    }
}
