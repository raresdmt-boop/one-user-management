package ro.mycode.user_management.users.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@Builder
@Entity
@NoArgsConstructor
@Table(name="users",
        uniqueConstraints ={ @UniqueConstraint(name="uk_users_email", columnNames = "email")
    }
)
public class User {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id",updatable = false, nullable = false, length = 36)
    private String id;
    @Column(name="first_name", nullable = false, length = 50)
    private String firstName;
    @Column(name="last_name", nullable = false, length = 50)
    private String lastName;
    @Column(name="email", nullable = false,unique = true, length = 100)
    private String email;
    @Column(name="password", nullable = false)
    private String password;
    @Column(name="age")
    private int age;

}
