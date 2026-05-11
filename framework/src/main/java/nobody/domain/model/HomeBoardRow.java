package nobody.domain.model;

public record HomeBoardRow(
        Long id,
        String title,
        String description,
        Long threads,
        String tone
) {
}

