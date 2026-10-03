package com.xin.musictag.web;

import com.xin.musictag.tagging.FieldChange;
import com.xin.musictag.tagging.UpdateAction;

public record ChangeRequest(String action, String value) {
    public FieldChange toChange() {
        UpdateAction a = action == null ? UpdateAction.KEEP : UpdateAction.valueOf(action.toUpperCase());
        return new FieldChange(a, a == UpdateAction.SET ? value : null);
    }
}
