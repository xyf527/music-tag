package com.xin.musictag;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BatchPageSafetyTest {
    @Test void actualPageScriptRendersDangerousValuesAsTextAndSupportsBothSelectors() throws Exception {
        Process process = new ProcessBuilder("node","src/test/js/batch-page.test.cjs").redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertEquals(0,process.waitFor(),output);
        assertTrue(output.contains("batch page safety passed"));
    }
}
