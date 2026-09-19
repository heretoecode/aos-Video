package com.archos.mediacenter.video.leanback;
import android.content.Context;
import android.view.*;
import androidx.recyclerview.widget.*;
/** Commit focus after scrolling a not-yet-attached target on the same key press. */
public class PreviewFocusRecycler extends RecyclerView {
 private int pending=NO_POSITION;
 public PreviewFocusRecycler(Context c){super(c);setItemAnimator(null);setPreserveFocusAfterLayout(true);}
 protected boolean focusablePosition(int position){return true;}
 @Override public boolean requestChildRectangleOnScreen(View child,android.graphics.Rect rect,boolean immediate){return super.requestChildRectangleOnScreen(child,rect,true);}
 @Override public boolean dispatchKeyEvent(KeyEvent e){
  if(e.getAction()!=KeyEvent.ACTION_DOWN)return super.dispatchKeyEvent(e);
  if(pending!=NO_POSITION)return e.getKeyCode()>=KeyEvent.KEYCODE_DPAD_UP&&e.getKeyCode()<=KeyEvent.KEYCODE_DPAD_RIGHT||super.dispatchKeyEvent(e);
  View focus=findFocus(),item=focus==null?null:findContainingItemView(focus);
  LayoutManager lm=getLayoutManager();if(item==null||!(lm instanceof LinearLayoutManager)||getAdapter()==null)return super.dispatchKeyEvent(e);
  int pos=getChildAdapterPosition(item),target=pos;boolean horizontal=((LinearLayoutManager)lm).getOrientation()==HORIZONTAL;
  if(pos==NO_POSITION)return super.dispatchKeyEvent(e);int key=e.getKeyCode();
  if(horizontal&&(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)){target+=key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1;if(target<0||target>=getAdapter().getItemCount())return true;}
  else if(!horizontal&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)){
   int direction=key==KeyEvent.KEYCODE_DPAD_UP?-1:1;
   if(lm instanceof GridLayoutManager){GridLayoutManager g=(GridLayoutManager)lm;int group=g.getSpanSizeLookup().getSpanGroupIndex(pos,g.getSpanCount()),span=g.getSpanSizeLookup().getSpanIndex(pos,g.getSpanCount());target=NO_POSITION;int best=Integer.MAX_VALUE,targetGroup=NO_POSITION;
    for(int i=pos+direction;i>=0&&i<getAdapter().getItemCount();i+=direction){int row=g.getSpanSizeLookup().getSpanGroupIndex(i,g.getSpanCount());if(row==group)continue;if(targetGroup!=NO_POSITION&&row!=targetGroup)break;if(!focusablePosition(i))continue;targetGroup=row;int distance=Math.abs(g.getSpanSizeLookup().getSpanIndex(i,g.getSpanCount())-span);if(distance<best){target=i;best=distance;}}
   }else target+=direction;
  }
  int step=target<pos?-1:1;while(target>=0&&target<getAdapter().getItemCount()&&target!=pos&&!focusablePosition(target))target+=step;
  if(target>=0&&target<getAdapter().getItemCount()&&target!=pos&&item.isFocusable()){
   ViewHolder attached=findViewHolderForAdapterPosition(target);
   if(attached!=null&&attached.itemView.requestFocus())return true;
  }
  if(target>=0&&target<getAdapter().getItemCount()&&target!=pos&&findViewHolderForAdapterPosition(target)==null){
   pending=target;scrollToPosition(target);postOnAnimation(new Runnable(){int tries;public void run(){if(!isAttachedToWindow()||!hasFocus()){pending=NO_POSITION;return;}ViewHolder h=findViewHolderForAdapterPosition(pending);if(h!=null){pending=NO_POSITION;h.itemView.requestFocus();h.itemView.refreshDrawableState();h.itemView.invalidate();}else if(tries++<8)postOnAnimation(this);else pending=NO_POSITION;}});return true;
  }
  return super.dispatchKeyEvent(e);
 }
}
