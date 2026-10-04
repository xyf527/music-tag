package com.xin.musictag;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

/** All fixtures are newly generated, original sine waves and plain-color artwork. */
public final class Phase04Fixtures {
    private Phase04Fixtures(){}
    public static byte[] ffmpeg(String...args)throws Exception {
        var command=new ArrayList<>(List.of("ffmpeg","-hide_banner","-loglevel","error","-y"));command.addAll(List.of(args));
        var process=new ProcessBuilder(command).redirectErrorStream(true).start();byte[] bytes=process.getInputStream().readAllBytes();assertEquals(0,process.waitFor(),"Fixture generation/decoding failed");return bytes;
    }
    public static Path generate(Path root,String format)throws Exception {
        String encoder=Map.of("mp3","libmp3lame","flac","flac","wav","pcm_s16le","m4a","aac","ogg","vorbis","opus","libopus").get(format);Path path=root.resolve("sample."+format);
        ffmpeg("-f","lavfi","-i","sine=frequency=440:duration=2","-ac","2","-c:a",encoder,"-strict","-2","-metadata","title=原创标题","-metadata","artist=原创歌手","-metadata","album=原创专辑",path.toString());return path;
    }
    public static byte[] pcm(Path path)throws Exception{return ffmpeg("-i",path.toString(),"-map","0:a:0","-f","s16le","-acodec","pcm_s16le","-");}
    public static Path cover(Path root)throws Exception {Path path=root.resolve("cover.png");ImageIO.write(new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB),"png",path.toFile());return path;}
}
