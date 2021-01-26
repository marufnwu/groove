package com.groove.megaapp.particles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.github.shchurov.particleview.SimpleTextureAtlasPacker;
import com.github.shchurov.particleview.TextureAtlas;
import com.github.shchurov.particleview.TextureAtlasFactory;

import java.util.Collections;
import java.util.List;

import com.groove.megaapp.R;
import com.groove.megaapp.utils.BitmapUtil;

public class SpinnerTextureAtlasFactory implements TextureAtlasFactory {

    private static final int ATLAS_SIZE = 300;

    private Context mContext;

    public SpinnerTextureAtlasFactory(Context context) {
        mContext = context;
    }

    @Override
    public TextureAtlas createTextureAtlas() {
        Drawable drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_music_note);
        //noinspection ConstantConditions
        List<Bitmap> drawables = Collections.singletonList(BitmapUtil.getBitmap(drawable));
        return new SimpleTextureAtlasPacker()
                .pack(drawables, ATLAS_SIZE, ATLAS_SIZE);
    }
}
