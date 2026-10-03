package com.xin.musictag.web;

import com.xin.musictag.domain.TagEditPlan;
import com.xin.musictag.tagging.FieldChange;
import com.xin.musictag.tagging.UpdateAction;

public record EditRequest(ChangeRequest title, ChangeRequest artist, ChangeRequest album,
                          ChangeRequest lyrics, String artwork) {
    public TagEditPlan toPlan() {
        FieldChange t = title == null ? FieldChange.keep() : title.toChange();
        FieldChange a = artist == null ? FieldChange.keep() : artist.toChange();
        FieldChange al = album == null ? FieldChange.keep() : album.toChange();
        FieldChange l = lyrics == null ? FieldChange.keep() : lyrics.toChange();
        UpdateAction cover = artwork == null ? UpdateAction.KEEP : UpdateAction.valueOf(artwork.toUpperCase());
        return new TagEditPlan(t, a, al, l, cover);
    }
}
