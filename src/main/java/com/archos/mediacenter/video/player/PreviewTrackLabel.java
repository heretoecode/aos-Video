package com.archos.mediacenter.video.player;

import java.util.Locale;

/** Small HUD labels; original track descriptions remain untouched inside choosers. */
public final class PreviewTrackLabel {
 public static String concise(String language,String format,String channels,String fallback){
  String name=language==null?"":language.trim();
  if(name.matches("[a-zA-Z]{2,3}"))name=new Locale(name).getDisplayLanguage();
  if(name.isEmpty()||name.equalsIgnoreCase("und"))name=fallback;
  String codec=format==null?"":format.toUpperCase(Locale.ROOT);
  if(codec.contains("TRUEHD"))codec="TrueHD";else if(codec.contains("EAC3")||codec.contains("E-AC-3"))codec="DD+";else if(codec.contains("AC3")||codec.contains("AC-3"))codec="DD";else if(codec.contains("DTS"))codec="DTS";else if(codec.contains("AAC"))codec="AAC";else codec="";
  String count=channels==null?"":channels.replaceAll("(?i)\\s*(channels?|ch)\\b","").trim();
  if(count.length()>5)count="";
  return name+(codec.isEmpty()?"":" · "+codec)+(count.isEmpty()?"":" "+count);
 }
 private PreviewTrackLabel(){}
}
