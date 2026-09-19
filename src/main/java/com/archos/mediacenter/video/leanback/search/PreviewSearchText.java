package com.archos.mediacenter.video.leanback.search;
import java.text.Normalizer;
import java.util.Locale;
/** Unicode comparison over indexed local titles, not an online search or a second index. */
public final class PreviewSearchText {
 public static String fold(String value){return value==null?"":Normalizer.normalize(value,Normalizer.Form.NFKD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replace('ł','l').replace('ø','o').replace("ß","ss");}
 public static boolean matches(String query,String... values){String needle=fold(query).trim();for(String value:values)if(fold(value).contains(needle))return true;return false;}
}
