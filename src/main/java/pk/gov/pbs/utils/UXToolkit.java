package pk.gov.pbs.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

/**
 * Todo: add functionality to add buttons to progress dialog (i,e for location ProgressDialog after some time show button to use last location)
 */

public class UXToolkit {
    protected final CustomActivity context;
    protected final LayoutInflater mLayoutInflater;
    protected final InputMethodManager mInputMethodManager;
    protected AlertDialog.Builder mDialogBuilder;
    protected ProgressDialog progressDialog;

    public UXToolkit(CustomActivity _context){
        context = _context;
        mInputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        mLayoutInflater = LayoutInflater.from(context);
    }

    public AlertDialog.Builder getDialogBuilder(){
        if(mDialogBuilder == null) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                mDialogBuilder = new AlertDialog.Builder(context);
            else
                mDialogBuilder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
        }
        return mDialogBuilder;
    }

    private View inflateInfoAlertDialogue(String title, String message){
        Spanned htm = Html.fromHtml(message);
        View dlg = mLayoutInflater.inflate(R.layout.custom_dialogue_alert,null);
        ((TextView) dlg.findViewById(R.id.tv_title)).setText(title);
        ((TextView) dlg.findViewById(R.id.tv_message)).setText(htm);
        return dlg;
    }

    public void hideKeyboardFrom(View view) {
        if (view == null)
            view = context.getWindow().getCurrentFocus();
        if (view == null) {
            view = new View(context);
            view.setFocusable(true);
            view.requestFocus();
        }
        mInputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);

    }

    public void showKeyboardTo(View view) {
        if (view == null)
            view = context.getCurrentFocus();
        if (view == null) {
            view = new View(context);
            view.setFocusable(true);
            view.requestFocus();
        }
        mInputMethodManager.showSoftInput(view, 0);
    }

    public AlertDialog buildAlertDialogue(String title, String message,@Nullable String positiveButtonLabel,@Nullable UXEventListeners.AlertDialogueEventListener callback){
        AlertDialog alertDialog;
        if(positiveButtonLabel == null)
            positiveButtonLabel = context.getResources().getString(R.string.label_btn_ok);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            Spanned htm = Html.fromHtml(message);
            alertDialog = getDialogBuilder()
                    .setTitle(title)
                    .setMessage(htm)
                    .setCancelable(false)
                    .setPositiveButton(
                            positiveButtonLabel
                            , (dialog, which) -> {
                                if(callback != null)
                                    callback.onOK();
                            }
                    )
                    .create();
        } else {
            alertDialog = getDialogBuilder()
                    .setView(inflateInfoAlertDialogue(title, message))
                    .setCancelable(false)
                    .setPositiveButton(
                            positiveButtonLabel
                            , (dialog, which) -> {
                                if (callback != null)
                                    callback.onOK();
                            }
                    )
                    .create();
        }
        return alertDialog;
    }

    public void showAlertDialogue(String title, String message, String positiveButtonLabel, @Nullable UXEventListeners.AlertDialogueEventListener event){
        StaticUtils.getHandler().post(()->{
            try {
                buildAlertDialogue(title, message, positiveButtonLabel, event).show();
            } catch (Exception e){
                Spanned htm = Html.fromHtml(message);
                showToast(htm);

                if(event != null)
                    event.onOK();

                ExceptionReporter.handle(e);
            }
        });
    }

    public void showAlertDialogue(String title, String message, @Nullable UXEventListeners.AlertDialogueEventListener event){
        showAlertDialogue(title, message, null, event);
    }

    public void showAlertDialogue(int title, int message, UXEventListeners.AlertDialogueEventListener event){
        showAlertDialogue(context.getString(title), context.getString(message), event);
    }

    public void showAlertDialogue(String title, String message){
        showAlertDialogue(title,message,null);
    }

    public void showAlertDialogue(int title, int message){
        showAlertDialogue(context.getString(title), context.getString(message), null);
    }

    public void showAlertDialogue(String message, UXEventListeners.AlertDialogueEventListener event){
        showAlertDialogue(context.getString(R.string.title_default_alert_dialogue), message, event);
    }

    public void showAlertDialogue(int message, UXEventListeners.AlertDialogueEventListener event){
        showAlertDialogue(context.getString(R.string.title_default_alert_dialogue), context.getString(message), event);
    }

    public void showAlertDialogue(String message){
        showAlertDialogue(context.getString(R.string.title_default_alert_dialogue), message, null);
    }

    public void showAlertDialogue(int message){
        showAlertDialogue(context.getString(R.string.title_default_alert_dialogue), context.getString(message), null);
    }

    public AlertDialog showConfirmDialogue(String title, String message, String positiveBtnLabel, String negativeBtnLabel, UXEventListeners.ConfirmDialogueEventsListener events){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            try {
                Spanned htm = Html.fromHtml(message);
                AlertDialog dialog = getDialogBuilder()
                        .setTitle(title)
                        .setMessage(htm)
                        .setCancelable(false)
                        .setPositiveButton(positiveBtnLabel, (dialog1, which) -> {
                            events.onOK();
                        })
                        .setNegativeButton(negativeBtnLabel, (dialog12, which) -> {
                            events.onCancel();
                        })
                        .create();

                dialog.show();
                return dialog;
            } catch (Exception e){
                ExceptionReporter.handle(e);
            }
        } else {
            try {
                AlertDialog alert = getDialogBuilder()
                        .setView(inflateInfoAlertDialogue(title, message))
                        .setCancelable(false)
                        .setPositiveButton(
                                positiveBtnLabel
                                , (dialog, which) -> events.onOK())
                        .setNegativeButton(
                                negativeBtnLabel
                                , (dialog, which) -> events.onCancel())
                        .create();
                alert.show();
                return alert;
            } catch (WindowManager.BadTokenException e) {
                ExceptionReporter.handle(e);
            } catch (Exception e) {
                ExceptionReporter.handle(e);
            }
        }
        return null;
    }

    public boolean showConfirmDialogue(String title, String message, UXEventListeners.ConfirmDialogueEventsListener events){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            try {
                Spanned htm = Html.fromHtml(message);
                AlertDialog dialog = getDialogBuilder()
                        .setTitle(title)
                        .setMessage(htm)
                        .setCancelable(false)
                        .setPositiveButton(R.string.label_btn_ok, (dialog1, which) -> {
                            events.onOK();
                        })
                        .setNegativeButton(R.string.label_btn_cancel, (dialog12, which) -> {
                            events.onCancel();
                        })
                        .create();

                dialog.show();
                return true;
            } catch (Exception e){
                ExceptionReporter.handle(e);
                return false;
            }
        } else {
            try {
                AlertDialog alert = getDialogBuilder()
                        .setView(inflateInfoAlertDialogue(title, message))
                        .setCancelable(false)
                        .setPositiveButton(
                                R.string.label_btn_ok
                                , (dialog, which) -> events.onOK())
                        .setNegativeButton(
                                context
                                        .getResources()
                                        .getString(R.string.label_btn_cancel)
                                , (dialog, which) -> events.onCancel())
                        .create();
                alert.show();
                return true;
            } catch (WindowManager.BadTokenException e) {
                ExceptionReporter.handle(e);
                return false;
            } catch (Exception e) {
                ExceptionReporter.handle(e);
                return false;
            }
        }
    }

    public boolean showConfirmDialogue(String message, UXEventListeners.ConfirmDialogueEventsListener events){
        return showConfirmDialogue(context.getString(R.string.title_default_confirm_dialogue),message,events);
    }

    public boolean showConfirmDialogue(int message, UXEventListeners.ConfirmDialogueEventsListener events){
        return showConfirmDialogue(context.getString(R.string.title_default_confirm_dialogue),context.getString(message),events);
    }

    public boolean showConfirmDialogue(int title, int message, UXEventListeners.ConfirmDialogueEventsListener events){
        return showConfirmDialogue(context.getString(title),context.getString(message),events);
    }

    public void changeProgressDialogueMessage(String message){
        synchronized (this) {
            if (progressDialog != null)
                progressDialog.setMessage(message);
        }
    }

    public void showProgressDialogue(String message, boolean cancelable){
        synchronized (this) {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(context);
                progressDialog.setMessage(message);
            } else
                changeProgressDialogueMessage(message);
            showProgressDialogue(cancelable);
        }
    }

    public void showProgressDialogue(String message){
        showProgressDialogue(message, false);
    }

    public void showProgressDialogue(int stringResource, boolean cancelable){
        showProgressDialogue(context.getResources().getString(stringResource), cancelable);
    }

    public void showProgressDialogue(int stringResource){
        showProgressDialogue(stringResource, false);
    }

    public void dismissProgressDialogue(){
        synchronized (this) {
            if (progressDialog != null) {
                if (progressDialog.isShowing())
                    progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }

    public void showProgressDialogue(){
        showProgressDialogue(false);
    }

    private void showProgressDialogue(boolean cancelable){
        if(!progressDialog.isShowing()) {
            progressDialog.setCancelable(cancelable);
            progressDialog.show();
        }
    }

    public void showToast(String message){
        synchronized (this) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    public void showToast(int stringResource){
        synchronized (this) {
            Toast.makeText(context, context.getResources().getString(stringResource), Toast.LENGTH_LONG).show();
        }
    }

    public void showToast(Spanned htm) {
        synchronized (this) {
            Toast.makeText(context, htm, Toast.LENGTH_LONG).show();
        }
    }

}
