package com.archos.mediacenter.video.leanback;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.leanback.widget.*;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.GridLayoutManager;
import java.util.*;
/** Source grid over Nova's existing discovery adapters and click handlers. */
public final class PreviewSources extends FrameLayout {
 private final ObjectAdapter rows;private final OnItemViewClickedListener click;private final RecyclerView grid;private final List<Item> items=new ArrayList<>();private final List<ObjectAdapter> observed=new ArrayList<>();private final Adapter adapter=new Adapter();
 private final ObjectAdapter.DataObserver observer=new ObjectAdapter.DataObserver(){public void onChanged(){refresh();}public void onItemRangeChanged(int s,int n){refresh();}public void onItemRangeInserted(int s,int n){refresh();}public void onItemRangeRemoved(int s,int n){refresh();}};
 private static class Item{Object value;Presenter presenter;ListRow row;String heading;}
 public PreviewSources(Context c,ObjectAdapter rows,OnItemViewClickedListener click){super(c);this.rows=rows;this.click=click;setFocusable(true);setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);setBackground(PreviewAccent.utility(c));grid=new PreviewFocusRecycler(c){@Override protected boolean focusablePosition(int p){return p>=0&&p<items.size()&&items.get(p).heading==null;}};GridLayoutManager layout=new GridLayoutManager(c,4);layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup(){public int getSpanSize(int p){return items.get(p).heading==null?1:4;}});grid.setLayoutManager(layout);grid.setItemAnimator(null);grid.setPadding(dp(28),dp(10),dp(28),dp(20));grid.setClipToPadding(false);grid.setAdapter(adapter);addView(grid);}
 protected void onAttachedToWindow(){super.onAttachedToWindow();rows.registerObserver(observer);refresh();}
 protected void onDetachedFromWindow(){rows.unregisterObserver(observer);for(ObjectAdapter a:observed)a.unregisterObserver(observer);observed.clear();super.onDetachedFromWindow();}
 public boolean atTop(){View focus=grid.findFocus();View child=focus==null?null:grid.findContainingItemView(focus);if(child==null)return false;int pos=grid.getChildAdapterPosition(child),first=0;while(first<items.size()&&items.get(first).heading!=null)first++;GridLayoutManager lm=(GridLayoutManager)grid.getLayoutManager();return pos>=0&&first<items.size()&&lm.getSpanSizeLookup().getSpanGroupIndex(pos,4)==lm.getSpanSizeLookup().getSpanGroupIndex(first,4)&&!grid.canScrollVertically(-1);}
 private void refresh(){post(()->{if(!isAttachedToWindow())return;Object focused=null;View f=grid.findFocus();View item=f==null?null:grid.findContainingItemView(f);if(item!=null){int p=grid.getChildAdapterPosition(item);if(p>=0&&p<items.size())focused=items.get(p).value;}for(ObjectAdapter a:observed)a.unregisterObserver(observer);observed.clear();items.clear();
  for(int i=0;i<rows.size();i++){Object value=rows.get(i);if(!(value instanceof ListRow))continue;ListRow row=(ListRow)value;ObjectAdapter children=row.getAdapter();observed.add(children);children.registerObserver(observer);if(children.size()==0)continue;Item heading=new Item();heading.heading=String.valueOf(row.getHeaderItem().getName());items.add(heading);for(int j=0;j<children.size();j++){Item e=new Item();e.value=children.get(j);e.presenter=children.getPresenter(e.value);e.row=row;items.add(e);}}
  final Object restore=focused;adapter.notifyDataSetChanged();if(restore!=null)for(int i=0;i<items.size();i++)if(items.get(i).value==restore){final int p=i;grid.scrollToPosition(p);grid.post(()->{RecyclerView.ViewHolder h=grid.findViewHolderForAdapterPosition(p);if(h!=null)h.itemView.requestFocus();});break;}
 });}
 private void readLabels(View v,List<String> out){if(v instanceof TextView){String t=((TextView)v).getText().toString().trim();if(!t.isEmpty()&&!out.contains(t))out.add(t);}if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)readLabels(((ViewGroup)v).getChildAt(i),out);}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private GradientDrawable bg(boolean f){GradientDrawable d=new GradientDrawable();d.setColor(f?0xdd25445c:0xbb192f45);d.setCornerRadius(dp(6));d.setStroke(dp(f?2:1),f?0xff59d8ff:0xff304b60);return d;}
 class Holder extends RecyclerView.ViewHolder{Presenter presenter;Presenter.ViewHolder nativeHolder;Holder(View v){super(v);}}
 class Adapter extends RecyclerView.Adapter<Holder>{public int getItemCount(){return items.size();}public Holder onCreateViewHolder(ViewGroup p,int type){FrameLayout card=new FrameLayout(getContext());RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,-2);lp.setMargins(0,dp(5),dp(12),dp(10));card.setLayoutParams(lp);return new Holder(card);}public void onBindViewHolder(Holder h,int position){Item item=items.get(position);FrameLayout card=(FrameLayout)h.itemView;if(h.presenter!=null&&h.nativeHolder!=null)h.presenter.onUnbindViewHolder(h.nativeHolder);card.removeAllViews();h.presenter=null;h.nativeHolder=null;
   if(item.heading!=null){TextView label=new TextView(getContext());label.setText(item.heading);label.setTextSize(19);label.setTextColor(0xffa8cce7);label.setPadding(0,dp(10),0,dp(4));card.addView(label);card.setFocusable(false);card.setBackground(null);card.setForeground(null);card.setOnClickListener(null);return;}
   h.presenter=item.presenter;h.nativeHolder=item.presenter.onCreateViewHolder(card);item.presenter.onBindViewHolder(h.nativeHolder,item.value);View nativeView=h.nativeHolder.view;
   // Keep the native holder for its existing source actions, but draw our compact tile.
   java.util.List<String> labels=new ArrayList<>();readLabels(nativeView,labels);
   LinearLayout tile=new LinearLayout(getContext());tile.setGravity(Gravity.CENTER_VERTICAL);tile.setPadding(dp(12),dp(8),dp(12),dp(8));
   ImageView icon=new ImageView(getContext());ImageView original=nativeView.findViewById(com.archos.mediacenter.video.R.id.image);icon.setImageDrawable(original!=null&&original.getDrawable()!=null?original.getDrawable():new PreviewIcon("network"));icon.setScaleType(ImageView.ScaleType.FIT_CENTER);tile.addView(icon,new LinearLayout.LayoutParams(dp(30),dp(30)));
   LinearLayout text=new LinearLayout(getContext());text.setOrientation(android.widget.LinearLayout.VERTICAL);text.setPadding(dp(12),0,0,0);tile.addView(text,new LinearLayout.LayoutParams(0,-2,1));
   for(int i=0;i<Math.min(2,labels.size());i++){TextView label=new TextView(getContext());label.setText(labels.get(i));label.setTextSize(i==0?13:10);label.setTextColor(i==0?0xffffffff:0xffa7bfd0);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);text.addView(label);}
   card.addView(tile,new FrameLayout.LayoutParams(-1,dp(76)));card.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);card.setFocusable(true);card.setBackground(PreviewDialog.surface(getContext(),false));card.setForeground(PreviewDialog.focus(getContext()));card.setOnClickListener(v->click.onItemClicked(h.nativeHolder,item.value,null,item.row));}

 public void onViewRecycled(Holder h){if(h.presenter!=null)h.presenter.onUnbindViewHolder(h.nativeHolder);}}
}
