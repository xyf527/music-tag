package com.xin.musictag.tagging;

import java.nio.file.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

final class ContentSignatures {
    private ContentSignatures(){}
    static byte[] head(Path file)throws IOException{try(var in=Files.newInputStream(file)){return in.readNBytes(512);}}
    static boolean at(byte[] bytes,int offset,String value){byte[] signature=value.getBytes(StandardCharsets.ISO_8859_1);return offset>=0&&bytes.length>=offset+signature.length&&Arrays.equals(Arrays.copyOfRange(bytes,offset,offset+signature.length),signature);}
    static boolean ogg(Path file,String packet)throws IOException{byte[] bytes=head(file);if(!at(bytes,0,"OggS")||bytes.length<27||bytes[4]!=0)return false;int segments=Byte.toUnsignedInt(bytes[26]);return segments>0&&at(bytes,27+segments,packet);}
}
