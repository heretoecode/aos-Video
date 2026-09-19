package com.archos.mediacenter.video.leanback;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.squareup.picasso.*;

/** One bounded artwork decode shared visually by the header and its navigation. */
public final class PreviewBackdrop extends Drawable implements Target {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final float density;private final Context context;
    private Bitmap bitmap, previous;
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pending;private long fadeStart;
    private Uri uri;
    public PreviewBackdrop(Context c) { context=c;density=c.getResources().getDisplayMetrics().density; }
    public void load(Uri next) {
        if(java.util.Objects.equals(uri,next)) return;
        Picasso.get().cancelRequest(this);if(pending!=null)handler.removeCallbacks(pending);uri=next;
        pending=()->{if(next==null){previous=bitmap;bitmap=null;fadeStart=android.os.SystemClock.uptimeMillis();invalidateSelf();}else Picasso.get().load(next).resize(1600,900).centerInside().noFade().into(this);};handler.postDelayed(pending,160);
    }
    public void release() { handler.removeCallbacksAndMessages(null);Picasso.get().cancelRequest(this); bitmap=previous=null; uri=null; }
    @Override public void draw(Canvas canvas) {
        Rect b=getBounds(); float h=Math.min(b.height(),420*density);
        canvas.drawColor(0xff0b1b2a);if(bitmap==null&&previous==null){android.graphics.drawable.GradientDrawable utility=PreviewAccent.utility(context);utility.setBounds(b);utility.draw(canvas);}
        paint.setShader(null);
        float fade=Math.min(1f,(android.os.SystemClock.uptimeMillis()-fadeStart)/180f);
        drawImage(canvas,previous,b.width(),h,1-fade);drawImage(canvas,bitmap,b.width(),h,fade);
        paint.setAlpha(255);if(fade<1)invalidateSelf();else previous=null;
        paint.setShader(new LinearGradient(0,0,b.width(),0,new int[]{0xf00b1b2a,0x700b1b2a,0x250b1b2a},null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,b.width(),h,paint);
        paint.setShader(new LinearGradient(0,0,0,h,new int[]{0x800b1b2a,0x100b1b2a,0xff0b1b2a},new float[]{0,.40f,1},Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,b.width(),h,paint);paint.setShader(null);
    }
    private void drawImage(Canvas canvas,Bitmap image,float w,float h,float alpha){if(image==null||image.isRecycled()||image.getWidth()<image.getHeight())return;float scale=Math.max(w/image.getWidth(),h/image.getHeight());paint.setAlpha((int)(255*alpha));canvas.save();canvas.clipRect(0,0,w,h);canvas.drawBitmap(image,null,new RectF(w-image.getWidth()*scale,0,w,image.getHeight()*scale),paint);canvas.restore();}
    @Override public void onBitmapLoaded(Bitmap b,Picasso.LoadedFrom from){if(b.getWidth()<b.getHeight())return;previous=bitmap;bitmap=b;fadeStart=android.os.SystemClock.uptimeMillis();invalidateSelf();}
    @Override public void onBitmapFailed(Exception e,Drawable d){invalidateSelf();}
    @Override public void onPrepareLoad(Drawable d){}
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter f){}
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
