package com.xin.musictag.audio;

import com.xin.musictag.domain.EditPlan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JaudiotaggerHandlerTest {
    @Test void wavRejectsMutationButAllowsKeep() {
        JaudiotaggerHandler handler = new JaudiotaggerHandler("wav");
        assertFalse(handler.supports(new EditPlan("x", com.xin.musictag.domain.UpdateMode.SET, null,
                com.xin.musictag.domain.UpdateMode.KEEP, null, com.xin.musictag.domain.UpdateMode.KEEP,
                null, com.xin.musictag.domain.UpdateMode.KEEP, null, com.xin.musictag.domain.UpdateMode.KEEP)));
        assertTrue(handler.supports(EditPlan.keepAll()));
    }
}
