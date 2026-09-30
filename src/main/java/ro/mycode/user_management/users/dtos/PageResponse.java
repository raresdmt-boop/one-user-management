package ro.mycode.user_management.users.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Schema(name = "PageResponse", description = "O pagina de rezultate, cu metadatele ei")
public record PageResponse<T>(

        @Schema(description = "Elementele paginii curente; lista goala daca nu exista rezultate")
        List<T> content,

        @Schema(description = "Indicele paginii curente, numarat de la 0", example = "0")
        int page,

        @Schema(description = "Numarul maxim de elemente pe pagina", example = "10")
        int size,

        @Schema(description = "Cate elemente exista in total, pe toate paginile", example = "42")
        long totalElements,

        @Schema(description = "Cate pagini exista in total", example = "5")
        int totalPages,

        @Schema(description = "Daca pagina curenta este ultima", example = "false")
        boolean last) {

    public static <E, T> PageResponse<T> from(Page<E> source, Function<E, T> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isLast());
    }
}
