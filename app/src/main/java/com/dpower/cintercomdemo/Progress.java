package com.dpower.cintercomdemo;

import android.app.Dialog;
import android.content.Context;

/**
 * 加载框
 */
public class Progress {
    private static Dialog progressDialog;

    public static void showProgress(Context context) {
        if (progressDialog == null) {
            progressDialog = new Dialog(context,R.style.progress_dialog);
            progressDialog.setContentView(R.layout.progress_dialog);
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            progressDialog.show();
        }
    }

    public static void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}
