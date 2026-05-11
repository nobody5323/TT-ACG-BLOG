package nobody.domain.model;

public record HomeCategoryRow(
        Long id,
        String name,
        String slug,
        String description,
        String accent
) {
}

