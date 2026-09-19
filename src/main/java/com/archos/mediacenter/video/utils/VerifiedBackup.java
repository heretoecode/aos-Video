package com.archos.mediacenter.video.utils;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Publish only a closed, readable archive. Failed generation never leaves a finished empty ZIP. */
public final class VerifiedBackup {
    public interface Writer { void write(ZipOutputStream zip) throws IOException; }
    public static void write(File destination,Writer writer) throws IOException {
        File parent=destination.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Cannot create backup folder");
        File temporary=File.createTempFile("nova-export-",".part",parent);
        try {
            try(FileOutputStream file=new FileOutputStream(temporary);ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(file))){writer.write(zip);zip.finish();zip.flush();file.getFD().sync();}
            validate(temporary);
            if(destination.exists())throw new IOException("Backup destination already exists");
            if(!temporary.renameTo(destination))throw new IOException("Cannot publish completed backup");
        }finally{if(temporary.exists())temporary.delete();}
    }
    public static void validate(File file)throws IOException{
        if(file.length()==0)throw new IOException("Backup archive is empty");
        try(ZipFile zip=new ZipFile(file)){
            for(String required:new String[]{"media.db","db_version.txt","settings.json"}){ZipEntry item=zip.getEntry(required);if(item==null||item.getSize()<=0)throw new IOException("Backup is missing "+required);}
            Enumeration<? extends ZipEntry> items=zip.entries();byte[] bytes=new byte[8192];
            while(items.hasMoreElements()){ZipEntry item=items.nextElement();if(item.isDirectory())continue;CRC32 crc=new CRC32();long size=0;try(InputStream input=zip.getInputStream(item)){int n;while((n=input.read(bytes))!=-1){crc.update(bytes,0,n);size+=n;}}
                if(size!=item.getSize()||crc.getValue()!=item.getCrc())throw new IOException("Backup verification failed: "+item.getName());
            }
        }
    }
    private VerifiedBackup(){}
}
