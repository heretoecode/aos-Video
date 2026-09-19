package com.archos.mediacenter.video.leanback;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Thin indeterminate ring; driven only while its host is visible. */
public final class ThinSpinner extends Drawable implements android.graphics.drawable.Animatable,Runnable {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private boolean running;private float angle;
 public void draw(Canvas c){Rect b=getBounds();float stroke=Math.max(1,b.width()/14f);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setStrokeCap(Paint.Cap.ROUND);RectF r=new RectF(b);r.inset(stroke,stroke);paint.setColor(0x304ca9ce);c.drawOval(r,paint);paint.setColor(0xff59d8ff);c.drawArc(r,angle,95,false,paint);}
 public void start(){if(!running){running=true;run();}}public void stop(){running=false;unscheduleSelf(this);}public boolean isRunning(){return running;}public void run(){if(!running)return;angle=(angle+9)%360;invalidateSelf();scheduleSelf(this,android.os.SystemClock.uptimeMillis()+25);}public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
