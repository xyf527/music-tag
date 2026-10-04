package com.xin.musictag;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DeploymentResourcesTest {
    @TempDir Path root;
    Path bin,log; String commit="a".repeat(40);
    @BeforeEach void setup()throws Exception {
        bin=Files.createDirectories(root.resolve("bin"));log=root.resolve("commands.log");
        Path fake=Path.of("src/test/sh/deployment-tool.sh").toAbsolutePath();fake.toFile().setExecutable(true);
        for(String tool:List.of("uname","stat","df","ss","sleep","flock","curl","docker"))Files.createSymbolicLink(bin.resolve(tool),fake);
        Path scripts=Files.createDirectories(root.resolve("deploy/m710q"));
        for(String name:List.of("common.sh","preflight.sh","health.sh","deploy.sh","rollback.sh")){Path p=scripts.resolve(name);Files.copy(Path.of("deploy/m710q",name),p);p.toFile().setExecutable(true);}
        Files.copy(Path.of("compose.yaml"),root.resolve("compose.yaml"));
        for(String name:List.of("uploads","working","outputs","reports","logs"))Files.createDirectories(root.resolve("data").resolve(name));env("fake:good");
    }
    void env(String image)throws Exception {Files.writeString(root.resolve(".env"),"MUSIC_TAG_IMAGE="+image+"\nMYSQL_HOST=invalid.example\nMYSQL_DATABASE=fake_database\nMYSQL_USER=fake_user\nMYSQL_PASSWORD=FAKE_ONLY\nDATA_ROOT="+root.resolve("data")+"\n");}
    int run(String script,Map<String,String> extra)throws Exception {
        var p=new ProcessBuilder("bash",root.resolve("deploy/m710q/"+script).toString(),commit).redirectErrorStream(true);
        p.environment().put("PATH",bin+":"+System.getenv("PATH"));p.environment().put("DEPLOY_ROOT",root.toString());p.environment().put("TEST_LOG",log.toString());p.environment().put("TEST_COMMIT",commit);p.environment().putAll(extra);
        var process=p.start();String output=new String(process.getInputStream().readAllBytes());int result=process.waitFor();assertFalse(output.contains("FAKE_ONLY"));return result;
    }
    @Test void deployAndRollbackOnFailurePreserveData()throws Exception {
        assertEquals(0,run("deploy.sh",Map.of()));assertEquals("fake:good\n",Files.readString(root.resolve(".deployment/current-image")));
        Path preserved=root.resolve("data/uploads/valuable");Files.writeString(preserved,"valuable");env("fake:bad");
        assertNotEquals(0,run("deploy.sh",Map.of()));assertEquals("fake:good\n",Files.readString(root.resolve(".deployment/current-image")));assertTrue(Files.exists(preserved));
        String commands=Files.readString(log);assertTrue(commands.contains("fake:bad"));assertTrue(commands.contains("fake:good"));assertFalse(commands.contains("down"));
    }
    @Test void occupiedForeignPortStopsBeforeUpgrade()throws Exception {
        assertNotEquals(0,run("deploy.sh",Map.of("TEST_PORT_BUSY","true")));assertFalse(Files.readString(log).contains("up -d"));
        assertEquals(0,run("preflight.sh",Map.of("TEST_PORT_BUSY","true","TEST_PROJECT_OWNER","true")));
    }
    @Test void resourcesKeepMysqlExternalAndUseNonRootPersistentStorage()throws Exception {
        String compose=Files.readString(Path.of("compose.yaml")),dockerfile=Files.readString(Path.of("Dockerfile"));
        assertFalse(compose.contains("image: mysql"));assertTrue(compose.contains("18081"));assertTrue(compose.contains("MYSQL_HOST"));assertTrue(compose.contains("MINIO_ENDPOINT"));
        for(String name:List.of("uploads","working","outputs","reports","logs"))assertTrue(compose.contains("/"+name));
        assertTrue(dockerfile.contains("USER 10001:10001"));assertTrue(dockerfile.contains("linux/amd64"));assertTrue(Files.readString(Path.of(".dockerignore")).contains("src/main/resources/application.yml"));
    }
}
