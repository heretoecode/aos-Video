package com.archos.mediacenter.video.leanback.settings;

import androidx.preference.*;
import com.archos.mediacenter.video.BuildConfig;
import com.archos.mediacenter.video.leanback.PreviewDialog;

final class PreviewBuildInfo {
 static void install(PreferenceFragmentCompat fragment,PreferenceCategory about){
  Preference build=about.findPreference("preferences_version");if(build!=null){build.setSummary(BuildConfig.VERSION_NAME);build.setOnPreferenceClickListener(p->{PreviewDialog.read(fragment.requireContext(),"SUPERNOVA Preview · Build information",describe());return true;});}
  if(about.findPreference("preview_build_info")!=null)return;
  Preference identity=new Preference(fragment.requireContext());identity.setKey("preview_build_info");identity.setTitle("Build information");identity.setSummary(BuildConfig.VERSION_NAME+" · "+BuildConfig.BUILD_TYPE);
  identity.setOnPreferenceClickListener(p->{PreviewDialog.read(fragment.requireContext(),"SUPERNOVA Preview · Build information",describe());return true;});about.addPreference(identity);
  Preference notes=new Preference(fragment.requireContext());notes.setKey("preview_whats_new");notes.setTitle("What's New");notes.setSummary("Release notes bundled with this build");notes.setOnPreferenceClickListener(p->{String body;try(java.io.InputStream stream=fragment.requireContext().getAssets().open("preview-whats-new.txt")){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;while((n=stream.read(chunk))>0)bytes.write(chunk,0,n);body=bytes.toString("UTF-8");}catch(java.io.IOException missing){body="Release notes are unavailable in this build.";}PreviewDialog.read(fragment.requireContext(),"What's New",body);return true;});about.addPreference(notes);
 }
 private static String describe(){return "Custom build: SUPERNOVA Preview 4.1\nVersion: "+BuildConfig.VERSION_NAME+"\nVersion code: "+BuildConfig.VERSION_CODE+"\nPackage: "+BuildConfig.APPLICATION_ID+"\nCustom Git SHA: "+BuildConfig.PREVIEW_GIT_SHA+"\nBuilt (UTC): "+BuildConfig.PREVIEW_BUILD_UTC+"\nBuild type: "+BuildConfig.BUILD_TYPE+"\nAPK ABIs: armeabi-v7a, arm64-v8a, x86, x86_64 (universal)\nDevice ABIs: "+android.text.TextUtils.join(", ",android.os.Build.SUPPORTED_ABIS)+"\nAndroid: "+android.os.Build.VERSION.RELEASE+" (API "+android.os.Build.VERSION.SDK_INT+")\nSource baseline version: 6.4.63\nUpstream commit: not recorded; no unverified SHA is claimed.\nPrior delivered custom baseline: 345636900b577bfec90a22d117fe9b6d062b9f2b\n\nBundled release notes do not check for or install updates.";}
}
