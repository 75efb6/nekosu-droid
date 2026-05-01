package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import ru.nsu.ccfit.zuev.osuplus.R;

public class ConfirmDialogFragment extends BaseFragment {

    private OnResult onResult;

    @StringRes
    private int text;

    public ConfirmDialogFragment() {
        setDismissOnBackgroundClick(true);
    }

    @Override
    protected int getLayoutID() {
        return R.layout.frgdialog_confirm;
    }

    @Override
    protected void onLoadView() {
        findViewById(R.id.okButton).setOnClickListener(v -> {
            if (onResult != null) {
                onResult.onAccept(true);
                dismiss();
            }
        });
        if (text != 0) {
            ((TextView) findViewById(R.id.confirm_message)).setText(text);
        }
        playOnLoadAnim();
    }

    @Override
    public void dismiss() {
        playOnDismissAnim(super::dismiss);
    }

    public ConfirmDialogFragment setMessage(@StringRes int text) {
        this.text = text;
        if (findViewById(R.id.confirm_message) != null) {
            ((TextView) findViewById(R.id.confirm_message)).setText(text);
        }
        return this;
    }

    public void showForResult(OnResult result) {
        this.onResult = result;
        show();
    }

    protected void playOnLoadAnim() {
        View body = findViewById(R.id.frg_body);
        body.setAlpha(0);
        body.setScaleX(0.85f);
        body.setScaleY(0.85f);
        body.setTranslationY(60);
        body.animate().cancel();
        body.animate()
                .alpha(1)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0)
                .setDuration(280)
                .setInterpolator(EasingHelper.asInterpolator(Easing.OutBack))
                .start();
        playBackgroundHideInAnim(200);
    }

    protected void playOnDismissAnim(Runnable runnable) {
        View body = findViewById(R.id.frg_body);
        body.animate().cancel();
        body.animate()
                .alpha(0)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .translationY(60)
                .setDuration(180)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InQuad))
                .setListener(new BaseAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                })
                .start();
        playBackgroundHideOutAnim(180);
    }


    public interface OnResult {
        void onAccept(boolean isAccepted);
    }

}
