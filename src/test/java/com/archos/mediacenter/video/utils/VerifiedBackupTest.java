package com.archos.mediacenter.video.utils;
import java.io.*;
import java.util.zip.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class VerifiedBackupTest {
 @Rule public TemporaryFolder folder=new TemporaryFolder();
 @Test public void publishesReadableNonemptyArchive()throws Exception{File target=new File(folder.getRoot(),"backup.zip");VerifiedBackup.write(target,zip->{for(String name:new String[]{"media.db","db_version.txt","settings.json"}){zip.putNextEntry(new ZipEntry(name));zip.write("fixture".getBytes("UTF-8"));zip.closeEntry();}});VerifiedBackup.validate(target);assertTrue(target.length()>0);}
 @Test public void failedGenerationNeverPublishesEmptyArchive()throws Exception{File target=new File(folder.getRoot(),"backup.zip");try{VerifiedBackup.write(target,zip->{throw new IOException("Disk unavailable");});fail();}catch(IOException expected){}assertFalse(target.exists());assertEquals(0,folder.getRoot().list().length);}
 @Test public void missingLibraryIsRejected()throws Exception{File target=new File(folder.getRoot(),"backup.zip");try{VerifiedBackup.write(target,zip->{});fail();}catch(IOException expected){}assertFalse(target.exists());}
}
