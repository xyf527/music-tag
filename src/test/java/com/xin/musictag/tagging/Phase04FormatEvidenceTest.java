package com.xin.musictag.tagging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Generated sine wave and original plain-color artwork, never checked in. */
class Phase04FormatEvidenceTest {
    @TempDir Path temp;
    static class Probe extends JaudiotaggerAudioTagHandler {
        final String format; Probe(String format){this.format=format;}
        public String format(){return format;}
        public AudioCapabilities capabilities(){return new AudioCapabilities(true,true,true,"experimental probe");}
    }
    static byte[] command(String... args)throws Exception {
        var command=new ArrayList<>(List.of("ffmpeg","-hide_banner","-loglevel","error","-y"));command.addAll(List.of(args));
        var process=new ProcessBuilder(command).redirectErrorStream(true).start();byte[] out=process.getInputStream().readAllBytes();assertEquals(0,process.waitFor(),"Generated fixture or decoding failed");return out;
    }
    void generate(Path path,String codec)throws Exception {
        command("-f","lavfi","-i","sine=frequency=440:duration=2","-ac","2","-c:a",codec,"-strict","-2","-metadata","title=已有标题","-metadata","artist=已有歌手","-metadata","album=已有专辑",path.toString());
    }
    byte[] pcm(Path path)throws Exception{return command("-i",path.toString(),"-map","0:a:0","-f","s16le","-acodec","pcm_s16le","-");}
    Path cover()throws Exception {Path cover=temp.resolve("cover.png");ImageIO.write(new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB),"png",cover.toFile());return cover;}
    void supported(String format,String codec)throws Exception {
        Path original=temp.resolve("original."+format),copy=temp.resolve("copy."+format);generate(original,codec);byte[] bytes=Files.readAllBytes(original),pcm=pcm(original);Files.copy(original,copy);
        AudioTagHandler handler=Map.of("m4a",new M4aTagHandler(),"ogg",new OggTagHandler()).get(format);assertTrue(handler.matchesContent(original));var before=handler.read(copy);assertEquals("已有标题",before.title());assertEquals("已有歌手",before.artist());assertEquals("已有专辑",before.album());
        handler.write(copy,new TagWritePlan(FieldChange.set("新标题"),FieldChange.set("新歌手"),FieldChange.set("新专辑"),FieldChange.set("[00:00.00] 原创歌词"),UpdateAction.SET,cover()));
        var set=handler.read(copy);assertEquals("新标题",set.title());assertEquals("新歌手",set.artist());assertEquals("新专辑",set.album());assertEquals("[00:00.00] 原创歌词",set.lyrics());assertTrue(set.artworkPresent());assertArrayEquals(pcm,pcm(copy));
        handler.write(copy,new TagWritePlan(FieldChange.keep(),FieldChange.keep(),FieldChange.keep(),FieldChange.keep(),UpdateAction.KEEP,null));assertEquals(set,handler.read(copy));assertArrayEquals(pcm,pcm(copy));
        handler.write(copy,new TagWritePlan(FieldChange.remove(),FieldChange.remove(),FieldChange.remove(),FieldChange.remove(),UpdateAction.REMOVE,null));var removed=handler.read(copy);assertNull(removed.title());assertNull(removed.artist());assertNull(removed.album());assertNull(removed.lyrics());assertFalse(removed.artworkPresent());assertArrayEquals(pcm,pcm(copy));assertArrayEquals(bytes,Files.readAllBytes(original));
    }
    @Test void m4aActualReadWriteThreeStatesAndDecode()throws Exception{supported("m4a","aac");}
    @Test void oggVorbisActualReadWriteThreeStatesAndDecode()throws Exception{supported("ogg","vorbis");}
    @Test void opusActualContainerIsDecodableButLibraryCannotReadOrWrite()throws Exception {
        Path opus=temp.resolve("original.opus");generate(opus,"libopus");byte[] original=Files.readAllBytes(opus);assertTrue(pcm(opus).length>0);
        var handler=new Probe("opus");assertThrows(java.io.IOException.class,()->handler.read(opus));
        for(UpdateAction action:UpdateAction.values()) {
            assertThrows(java.io.IOException.class,()->handler.write(opus,new TagWritePlan(FieldChange.set("新标题"),FieldChange.set("新歌手"),FieldChange.set("新专辑"),action==UpdateAction.SET?FieldChange.set("原创歌词"):action==UpdateAction.REMOVE?FieldChange.remove():FieldChange.keep(),action,cover())));
            assertArrayEquals(original,Files.readAllBytes(opus));
        }
    }
}
