package com.archos.mediacenter.video.leanback;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.squareup.picasso.*;

/** One bounded artwork decode shared visually by the header and its navigation. */
public final class PreviewBackdrop extends Drawable implements Target {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final float density;
    private Bitmap bitmap;
    private Uri uri;
    public PreviewBackdrop(Context c) { density=c.getResources().getDisplayMetrics().density; }
    public void load(Uri next) {
        if(java.util.Objects.equals(uri,next)) return;
        Picasso.get().cancelRequest(this); uri=next; bitmap=null; invalidateSelf();
        if(next!=null) Picasso.get().load(next).resize(1600,900).centerCrop().noFade().into(this);
    }
    public void release() { Picasso.get().cancelRequest(this); bitmap=null; uri=null; }
    @Override public void draw(Canvas canvas) {
        Rect b=getBounds(); float h=Math.min(b.height(),420*density);
        canvas.drawColor(0xff0b1b2a);
        paint.setShader(null);
        if(bitmap!=null&&!bitmap.isRecycled()) {
            float scale=Math.max(b.width()/(float)bitmap.getWidth(),h/bitmap.getHeight());
            float width=bitmap.getWidth()*scale,height=bitmap.getHeight()*scale;
            canvas.save();canvas.clipRect(0,0,b.width(),h);canvas.drawBitmap(bitmap,null,new RectF(b.width()-width,0,b.width(),height),paint);canvas.restore();
        }
        paint.setShader(new LinearGradient(0,0,b.width(),0,new int[]{0xf00b1b2a,0x700b1b2a,0x250b1b2a},null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,b.width(),h,paint);
        paint.setShader(new LinearGradient(0,0,0,h,new int[]{0x800b1b2a,0x100b1b2a,0xff0b1b2a},new float[]{0,.40f,1},Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,b.width(),h,paint);paint.setShader(null);
    }
    @Override public void onBitmapLoaded(Bitmap b,Picasso.LoadedFrom from){bitmap=b;invalidateSelf();}
    @Override public void onBitmapFailed(Exception e,Drawable d){bitmap=null;invalidateSelf();}
    @Override public void onPrepareLoad(Drawable d){}
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter f){}
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
