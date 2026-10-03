package com.xin.musictag.domain;

import com.xin.musictag.tagging.FieldChange;
import com.xin.musictag.tagging.UpdateAction;

public record TagEditPlan(FieldChange title, FieldChange artist, FieldChange album,
                          FieldChange lyrics, UpdateAction artwork) { }
