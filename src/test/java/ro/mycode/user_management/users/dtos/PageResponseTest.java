package ro.mycode.user_management.users.dtos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import ro.mycode.user_management.support.UserFixtures;
import ro.mycode.user_management.users.models.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResponse (unitate) - impachetarea unei pagini Spring Data")
class PageResponseTest {

    @Test
    @DisplayName("from copiaza metadatele paginii si mapeaza continutul")
    void from_copiesMetadata_andMapsContent() {
        // Arrange
        List<User> users = List.of(
                UserFixtures.persisted(UserFixtures.KNOWN_ID, "Cristian", "Tudor", "a@gmail.com", 30),
                UserFixtures.persisted(UserFixtures.OTHER_ID, "Radu", "Popescu", "b@gmail.com", 41));
        PageRequest request = PageRequest.of(0, 2, Sort.by("lastName"));
        PageImpl<User> page = new PageImpl<>(users, request, 5);

        // Act
        PageResponse<UserResponse> response = PageResponse.from(page, UserResponse::from);

        // Assert
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().getFirst().firstName()).isEqualTo("Cristian");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.last()).isFalse();
    }

    @Test
    @DisplayName("last este true pe ultima pagina")
    void from_marksLastPage() {
        // Arrange
        List<User> users = List.of(UserFixtures.persisted());
        PageImpl<User> page = new PageImpl<>(users, PageRequest.of(2, 2), 5);

        // Act
        PageResponse<UserResponse> response = PageResponse.from(page, UserResponse::from);

        // Assert
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.last()).isTrue();
    }

    @Test
    @DisplayName("o pagina goala produce content gol, nu null")
    void from_emptyPage_hasEmptyContent() {
        // Arrange
        PageImpl<User> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        // Act
        PageResponse<UserResponse> response = PageResponse.from(page, UserResponse::from);

        // Assert
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.last()).isTrue();
    }
}
