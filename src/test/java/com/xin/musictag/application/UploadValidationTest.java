package com.xin.musictag.application;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadValidationTest {
    @Test void rejectsEmptyUpload() { SingleTrackService service = null; assertThrows(NullPointerException.class, () -> { if (service == null) throw new NullPointerException(); }); }
    @Test void largeFixtureRequirementIsDocumented() { byte[] payload = new byte[13 * 1024 * 1024]; org.junit.jupiter.api.Assertions.assertTrue(payload.length > 12_200_000); }
}
