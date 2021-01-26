package com.groove.megaapp.particles;

import com.github.shchurov.particleview.Particle;

public class SpinnerParticle extends Particle {

    private final float mRadius;

    public SpinnerParticle(int size, int texture, float radius, float rotation) {
        super(size, size, 0, 0, texture);
        setAlpha(.1f);
        setRotation(rotation);
        mRadius = radius;
    }

    public float getSpinRadius() {
        return mRadius;
    }
}
