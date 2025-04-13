package home.models.projects;

import java.time.ZonedDateTime;

public record EssentialInfo(
        String name,
        int type,
        boolean favorite,
        ZonedDateTime dateToStart
) {}