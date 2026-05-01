package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.view.View;

import androidx.annotation.StringRes;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import ru.nsu.ccfit.zuev.osuplus.R;

public class LoadingFragment extends BaseFragment {

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_loading;
    }

    @Override
    protected void onLoadView() {
        playOnLoadAnim();
    }

    @Override
    public void dismiss() {
        playOnDismissAnim(super::dismiss);
    }

    protected void playOnLoadAnim() {
        View body = findViewById(R.id.frg_body);
        if(body == null) return;
        body.setAlpha(0);
        body.setScaleX(0.8f);
        body.setScaleY(0.8f);
        body.animate().cancel();
        body.animate()
                .alpha(1)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(EasingHelper.asInterpolator(Easing.OutBack))
                .start();
        playBackgroundHideInAnim(200);
    }

    protected void playOnDismissAnim(Runnable runnable) {
        View body = findViewById(R.id.frg_body);
        if(body == null) return;
        body.animate().cancel();
        body.animate()
                .alpha(0)
                .scaleX(0.8f)
                .scaleY(0.8f)
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

}