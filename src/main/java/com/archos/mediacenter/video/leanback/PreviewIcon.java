package com.archos.mediacenter.video.leanback;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.Locale;
/** Small outline icons shared by Nova menus, controls and preference categories. */
public final class PreviewIcon extends Drawable {
 private final String kind;private final Paint p=new Paint(3);
 public PreviewIcon(String name){kind=name.toLowerCase(Locale.ROOT);p.setColor(0xffb9d8e9);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
 private void line(Canvas c,float... pts){Path q=new Path();q.moveTo(pts[0],pts[1]);for(int i=2;i<pts.length;i+=2)q.lineTo(pts[i],pts[i+1]);c.drawPath(q,p);}
 public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);
  if(kind.contains("check")){line(c,5,12,10,17,20,6);}
  else if(kind.contains("clear")||kind.contains("close")||kind.contains("remove")||kind.contains("delete")){line(c,6,6,18,18);line(c,18,6,6,18);}
  else if(kind.contains("home")||kind.contains("discovery")){line(c,2,11,12,3,22,11);line(c,5,10,5,21,10,21,10,15,14,15,14,21,19,21,19,10);}
  else if(kind.equals("general")||kind.contains("settings")||kind.contains("advanced")){for(int y=5;y<=19;y+=7)line(c,3,y,21,y);c.drawCircle(8,5,2,p);c.drawCircle(16,12,2,p);c.drawCircle(10,19,2,p);}
  else if(kind.contains("appearance")){c.drawCircle(12,12,9,p);c.drawCircle(8,8,1,p);c.drawCircle(15,7,1,p);c.drawCircle(18,13,1,p);line(c,6,16,10,16,12,20);}
  else if(kind.contains("trakt")){c.drawCircle(12,12,9,p);line(c,5,12,9,16,17,8);}
  else if(kind.contains("streaming")||kind.contains("integrations")){c.drawArc(2,3,22,23,205,100,false,p);c.drawArc(6,9,18,23,210,90,false,p);c.drawCircle(12,19,1,p);}
  else if(kind.contains("library")){c.drawRect(3,4,8,21,p);c.drawRect(11,4,16,21,p);line(c,19,4,22,20);}
  else if(kind.contains("play")||kind.contains("resume")||kind.contains("trailer")){line(c,8,4,20,12,8,20,8,4);}
  else if(kind.contains("year")||kind.contains("date")||kind.contains("added")){c.drawRoundRect(3,5,21,21,2,2,p);line(c,3,10,21,10);line(c,8,3,8,7);line(c,16,3,16,7);}
  else if(kind.contains("order")||kind.contains("ascending")||kind.contains("descending")||kind.contains("newest")||kind.contains("oldest")){line(c,7,3,7,21);line(c,3,7,7,3,11,7);line(c,17,3,17,21);line(c,13,17,17,21,21,17);}
  else if(kind.contains("filter")||kind.contains("genre")){line(c,3,5,21,5,14,13,14,20,10,18,10,13,3,5);}
  else if(kind.contains("time")||kind.contains("last")||kind.contains("runtime")||kind.contains("speed")){c.drawCircle(12,12,9,p);line(c,12,6,12,12,16,15);}
  else if(kind.contains("audio")||kind.contains("sound")){line(c,3,9,7,9,12,5,12,19,7,15,3,15,3,9);c.drawArc(10,5,22,19,-65,130,false,p);}
  else if(kind.contains("sub")){c.drawRoundRect(2,5,22,19,2,2,p);line(c,5,11,10,11);line(c,14,11,19,11);line(c,5,15,13,15);line(c,17,15,19,15);}
  else if(kind.contains("network")||kind.contains("source")||kind.contains("storage")){c.drawRoundRect(3,3,21,10,2,2,p);c.drawRoundRect(3,14,21,21,2,2,p);c.drawPoint(7,7,p);c.drawPoint(7,18,p);}
  else if(kind.contains("info")||kind.contains("about")||kind.contains("details")){c.drawCircle(12,12,9,p);line(c,12,11,12,17);c.drawPoint(12,7,p);}
  else if(kind.contains("list")||kind.contains("sort")||kind.contains("title")||kind.contains("chapter")){for(int y=5;y<=19;y+=7){c.drawPoint(3,y,p);line(c,8,y,21,y);}}
  else if(kind.contains("star")||kind.contains("rating")||kind.contains("popular")||kind.contains("trending")){line(c,12,2,15,9,22,9,17,14,19,21,12,17,5,21,7,14,2,9,9,9,12,2);}
  else if(kind.contains("more")||kind.contains("actions")){c.drawCircle(4,12,1,p);c.drawCircle(12,12,1,p);c.drawCircle(20,12,1,p);}
  else if(kind.contains("right")||kind.contains("next")){line(c,9,5,16,12,9,19);}
  else {for(int y=3;y<=14;y+=11)for(int x=3;x<=14;x+=11)c.drawRoundRect(x,y,x+7,y+7,1,1,p);}
  c.restore();
 }
 public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 public static void apply(android.widget.TextView view,String kind,int size){PreviewIcon icon=new PreviewIcon(kind);int d=Math.round(size*view.getResources().getDisplayMetrics().density);icon.setBounds(0,0,d,d);view.setCompoundDrawables(icon,null,null,null);view.setCompoundDrawablePadding(Math.round(9*view.getResources().getDisplayMetrics().density));}
}
