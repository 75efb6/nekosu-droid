package ru.nsu.ccfit.zuev.osu.game;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.camera.hud.HUD;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.util.FPSCounter;
import org.anddev.andengine.opengl.font.Font;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

public class GlobalFPSOverlay extends HUD {
    private final FPSCounter fpsCounter;
    private final ChangeableText fpsText;
    private final StringBuilder sb = new StringBuilder(32);
    private int cachedFrameLimit = 60;

    public GlobalFPSOverlay() {
        Font font = ResourceManager.getInstance().getFont("smallFont");
        fpsText = new ChangeableText(
                0, 0, font,
                "0 FPS (0 ms)", 32
        );
        fpsText.setAlpha(0.85f);
        attachChild(fpsText);

        fpsCounter = new FPSCounter() {
            @Override
            public void onUpdate(float pSecondsElapsed) {
                super.onUpdate(pSecondsElapsed);

                if (cachedFrameLimit == 60) {
                    var activity = GlobalManager.getInstance().getMainActivity();
                    if (activity != null) {
                        cachedFrameLimit = Math.round(activity.getRefreshRate());
                    }
                }
                int frameLimit = cachedFrameLimit;
                int currentFps = Math.min(Math.round(this.getFPS()), frameLimit);
                float frameTime = this.mFrames > 0 ? (this.mSecondsElapsed / this.mFrames) * 1000f : 0;

                sb.setLength(0);
                sb.append(currentFps).append(" FPS (").append((int) frameTime).append(" ms)");
                fpsText.setText(sb.toString());

                float ratio = (float) currentFps / frameLimit;
                if (ratio >= 0.8f) {
                    fpsText.setColor(0f, 1f, 0f);
                } else if (ratio >= 0.65f) {
                    fpsText.setColor(1f, 1f, 0f);
                } else {
                    fpsText.setColor(1f, 0f, 0f);
                }

                int w = Config.getRES_WIDTH();
                if (w > 0) {
                    fpsText.setPosition(w - fpsText.getWidth() - 5, 5);
                }

                if (this.mSecondsElapsed >= 1f) {
                    this.reset();
                }
            }
        };

        registerUpdateHandler(fpsCounter);
    }

    public void attachToCamera(Camera camera) {
        camera.setHUD(this);
    }
}
