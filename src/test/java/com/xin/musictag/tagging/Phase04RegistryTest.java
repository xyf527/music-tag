package com.xin.musictag.tagging;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class Phase04RegistryTest {
    @Test void newProviderDefinesOwnCapabilityAndNoCentralFormatListIsRequired(){
        AudioTagHandler provider=new AudioTagHandler(){
            public String format(){return "testformat";}
            public AudioCapabilities capabilities(){return new AudioCapabilities(true,false,false,"test provider");}
            public AudioMetadata read(Path file){return null;}
            public void write(Path file,TagWritePlan plan){}
        };
        var registry=new AudioTagHandlerRegistry(List.of(provider,new UnsupportedOpusHandler()));assertSame(provider,registry.require("TESTFORMAT"));assertTrue(registry.supportsOutput("testformat"));assertEquals(List.of("TESTFORMAT"),registry.outputFormats());assertFalse(registry.matrix().get("opus").output());assertFalse(registry.matrix().get("opus").readable());
        assertThrows(IllegalArgumentException.class,()->new AudioTagHandlerRegistry(List.of(provider,provider)));
    }
    @Test void wavAcceptsLyricsAndCoverPlansWhileOpusRemainsUnsupported() {
        var wav=new WavTagHandler();var opus=new UnsupportedOpusHandler();
        for(UpdateAction action:List.of(UpdateAction.SET,UpdateAction.REMOVE)){
            var lyrics=action==UpdateAction.SET?FieldChange.set("原创歌词"):FieldChange.remove();
            assertDoesNotThrow(()->wav.capabilities().require(new TagWritePlan(FieldChange.keep(),FieldChange.keep(),FieldChange.keep(),lyrics,UpdateAction.KEEP,null)));
            assertDoesNotThrow(()->wav.capabilities().require(new TagWritePlan(FieldChange.keep(),FieldChange.keep(),FieldChange.keep(),FieldChange.keep(),action,null)));
        }
        for(UpdateAction action:UpdateAction.values())assertEquals("UNSUPPORTED_FORMAT",assertThrows(com.xin.musictag.domain.ProcessingException.class,()->opus.write(Path.of("unused.opus"),new TagWritePlan(FieldChange.keep(),FieldChange.keep(),FieldChange.keep(),FieldChange.keep(),action,null))).code());
    }
}
