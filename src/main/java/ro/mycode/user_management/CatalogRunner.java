package ro.mycode.user_management;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.mycode.user_management.users.repository.UserRepository;

import java.util.List;

@Component
@Profile("demo")
@Order(2)
public class CatalogRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    public CatalogRunner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        banner("Derived queries");
        System.out.println(userRepository.findByEmail("cristian.tudor@gmail.com").orElseThrow().getEmail());
        System.out.println(userRepository.findByFirstNameAndLastName("Cristian", "Tudor"));
        System.out.println(userRepository.findByAgeGreaterThan(25));
        System.out.println(userRepository.findByAgeBetween(15, 20));
        System.out.println(userRepository.findByLastNameContainingIgnoreCase("Ste"));
        System.out.println(userRepository.existsByEmail("cristian.tudor@gmail.com"));

        banner("JPQL si nativ");
        System.out.println(userRepository.findByEmailAndPassword("cristian.tudor@gmail.com", "parola123"));
        System.out.println(userRepository.findAdultUsers(18));
        System.out.println(userRepository.findByEmailNative("cristian.tudor@gmail.com"));

        banner("Sortare, limitare, paginare");
        System.out.println(userRepository.findTop3ByOrderByAgeDesc());
        System.out.println(userRepository.findByEmailIn(List.of("cristian.tudor@gmail.com", "nu.exista@gmail.com")));
        System.out.println(userRepository.findByEmailEndingWith("@gmail.com"));
        System.out.println(userRepository.countByAgeLessThan(30));
        System.out.println(userRepository.findAllByOrderByLastNameAscFirstNameAsc());
        System.out.println(userRepository.findByAgeGreaterThanEqual(18, PageRequest.of(0, 2, Sort.by("age").descending())).getContent());

        banner("Agregare, proiectie, update");
        System.out.println(userRepository.findAverageAge());
        System.out.println(userRepository.searchByName("tud"));
        userRepository.findByLastNameOrderByFirstNameAsc("Tudor")
                .forEach(summary -> System.out.println(summary.getFirstName() + " " + summary.getEmail()));
        System.out.println(userRepository.updatePasswordByEmail("cristian.tudor@gmail.com", "parolaNoua123"));
        System.out.println(userRepository.findByEmail("cristian.tudor@gmail.com").orElseThrow().getPassword());
    }

    private void banner(String title) {
        System.out.println("============" + title + "============");
    }
}
