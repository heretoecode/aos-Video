package com.archos.mediacenter.video.player;
import com.archos.mediacenter.utils.introdb.IntroSegments;
import org.junit.Test;
import static org.junit.Assert.*;
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(application=android.app.Application.class,sdk=28)
public class PreviewUpNextTest {
 @Test public void realCreditsTimingOverridesFallback(){IntroSegments segments=new IntroSegments();segments.add(IntroSegments.Type.CREDITS,new IntroSegments.Segment(1700000L,1800000L,0,0,"fixture"));assertEquals(1700000,PreviewUpNext.trigger(1800000,segments));assertEquals(1770000,PreviewUpNext.trigger(1800000,null));}
 @Test public void remoteSelectionConsumesReleaseWithoutPausingPlayback(){
  org.robolectric.android.controller.ActivityController<android.app.Activity> host=org.robolectric.Robolectric.buildActivity(android.app.Activity.class).setup();PreviewUpNext prompt=new PreviewUpNext(host.get());try{
   android.widget.LinearLayout root=new android.widget.LinearLayout(host.get());root.setOrientation(android.widget.LinearLayout.VERTICAL);android.widget.TextView video=new android.widget.TextView(host.get());video.setFocusableInTouchMode(true);root.addView(video);android.widget.LinearLayout panel=new android.widget.LinearLayout(host.get()),actions=new android.widget.LinearLayout(host.get());panel.addView(actions);root.addView(panel);host.get().setContentView(root);int[] chosen={0};
   for(int i=0;i<2;i++){android.widget.TextView b=new android.widget.TextView(host.get());b.setFocusableInTouchMode(true);b.setOnClickListener(v->{chosen[0]++;org.robolectric.util.ReflectionHelpers.setField(prompt,"panel",null);});actions.addView(b);}
   video.requestFocus();org.robolectric.util.ReflectionHelpers.setField(prompt,"panel",panel);
   for(int key:new int[]{android.view.KeyEvent.KEYCODE_DPAD_UP,android.view.KeyEvent.KEYCODE_DPAD_RIGHT,android.view.KeyEvent.KEYCODE_DPAD_CENTER}){assertTrue(prompt.handleKey(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,key),()->{}));assertTrue(prompt.handleKey(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,key),()->{}));}
   assertEquals(1,chosen[0]);
  }finally{prompt.stop();host.pause().stop().destroy();}
 }
}
