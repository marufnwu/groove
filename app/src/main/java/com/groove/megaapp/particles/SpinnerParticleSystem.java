package com.groove.megaapp.particles;

import com.github.shchurov.particleview.Particle;
import com.github.shchurov.particleview.ParticleSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpinnerParticleSystem implements ParticleSystem {

    private enum Stage {
        SHOW,
        SPIN,
        DISMISS,
        NONE,
    }

    private static final float DISMISS_DURATION = 0.3f;
    private static final int PARTICLES_COUNT = 8;

    private volatile float mCx;
    private volatile float mCy;
    private volatile OnDismissEndListener mListener;
    private List<SpinnerParticle> mParticles = new ArrayList<>();
    private volatile Stage mStage = Stage.NONE;
    private float mStageTime;

    public SpinnerParticleSystem(int radius) {
        Random random = new Random();
        for (int i = 0; i < PARTICLES_COUNT; i++) {
            float radius2 = (0.7f + random.nextFloat() * 0.3f) * radius;
            float rotation = (float) (random.nextFloat() * 2 * Math.PI);
            int size = random.nextInt(50 - 25) + 25;
            mParticles.add(new SpinnerParticle(size, 0, radius2, rotation));
        }
    }

    public void dismiss() {
        mStage = Stage.DISMISS;
    }

    public void show(int cx, int cy) {
        mCx = cx;
        mCy = cy;
        mStage = Stage.SHOW;
    }

    @Override
    public List<? extends Particle> update(double delta) {
        if (mStage == Stage.NONE) {
            return mParticles;
        }

        float factor = 1f;
        if (mStage == Stage.SHOW || mStage == Stage.DISMISS) {
            float progress = mStageTime / DISMISS_DURATION;
            if (progress <= 1f) {
                if (mStage == Stage.SHOW) {
                    factor = progress;
                } else {
                    factor = 1f - progress;
                }

                mStageTime += delta;
            } else {
                if (mStage == Stage.SHOW) {
                    mStage = Stage.SPIN;
                } else {
                    factor = 0f;
                    mStage = Stage.NONE;
                    if (mListener != null) {
                        mListener.onDismissEnd();
                    }
                }

                mStageTime = 0;
            }
        }

        update(delta, factor);
        return mParticles;
    }

    private void update(double delta, float progress) {
        for (int i = 0; i < mParticles.size(); i++) {
            SpinnerParticle p = mParticles.get(i);
            p.setRotation(p.getRotation() + (float) delta);
            p.setX(mCx + p.getSpinRadius() * progress * (float) Math.cos(p.getRotation()));
            p.setY(mCy + p.getSpinRadius() * progress * (float) Math.sin(p.getRotation()));
            p.setAlpha(progress);
        }
    }

    @Override
    public int getMaxCount() {
        return PARTICLES_COUNT;
    }

    public void setOnDismissEndListener(OnDismissEndListener listener) {
        mListener = listener;
    }

    public interface OnDismissEndListener {

        void onDismissEnd();
    }
}
