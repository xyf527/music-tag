package com.xin.musictag.tagging;

public record FieldChange(UpdateAction action, String value) {
    public FieldChange {
        if (action == null) throw new IllegalArgumentException("action is required");
        if (action == UpdateAction.SET && (value == null || value.isBlank())) {
            throw new IllegalArgumentException("SET requires a value");
        }
        if (action != UpdateAction.SET && value != null) {
            throw new IllegalArgumentException("Only SET accepts a value");
        }
    }
    public static FieldChange keep() { return new FieldChange(UpdateAction.KEEP, null); }
    public static FieldChange remove() { return new FieldChange(UpdateAction.REMOVE, null); }
    public static FieldChange set(String value) { return new FieldChange(UpdateAction.SET, value); }
}
