package ro.mycode.user_management.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ro.mycode.user_management.users.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,String> {

    Optional<User> findByEmail(String email);
    List<User> findByFirstNameAndLastName(String firstName, String lastName);
    List<User> findByAgeGreaterThan(int age);
    List<User> findByAgeBetween(int minAge, int maxAge);
    List<User> findByLastNameContainingIgnoreCase(String lastName);
    boolean existsByEmail(String email);
    void deleteByEmail(String email);

    @Query("select u from User u where u.email= :email AND u.password = :password")
    Optional<User> findByEmailAndPassword(@Param("email") String email, @Param("password") String password);

    @Query("select u from User u where u.age >= :minAge")
    List<User> findAdultUsers(@Param("minAge") int minAge);

    @Query(value="SELECT * FROM users WHERE email = :email", nativeQuery = true)
    Optional<User> findByEmailNative(@Param("email") String email);
}
