package ro.mycode.user_management.users;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ro.mycode.user_management.users.repository.UserRepository;

import java.util.List;

@Component
public class CatalogRunner implements CommandLineRunner {

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        System.out.println(userRepository.findByEmail("cristian.tudor@gmail.com").get().getEmail());
        System.out.println(userRepository.findByFirstNameAndLastName("Cristian", "Tudor"));
        System.out.println(userRepository.findByAgeGreaterThan(25));
        System.out.println(userRepository.findByAgeBetween(15, 20));
        System.out.println(userRepository.findByLastNameContainingIgnoreCase("Ste"));
        System.out.println(userRepository.existsByEmail("cristian.tudor@gmail.com"));
//        userRepository.deleteByEmail("bogdan.alexandrescu@gmail.com");

        System.out.println(userRepository.findByEmailAndPassword("cristian.tudor@gmail.com", "parola123"));
        System.out.println(userRepository.findAdultUsers(18));
        System.out.println(userRepository.findByEmailNative("cristian.tudor@gmail.com"));

        // ===== adăugat la review (exemple noi, nu face parte din soluția lui Rareș) =====

        System.out.println(userRepository.findTop3ByOrderByAgeDesc());
        System.out.println(userRepository.findByEmailIn(List.of("cristian.tudor@gmail.com", "nu.exista@gmail.com")));
        System.out.println(userRepository.findByEmailEndingWith("@gmail.com"));
        System.out.println(userRepository.countByAgeLessThan(30));
        System.out.println(userRepository.findAllByOrderByLastNameAscFirstNameAsc());
        System.out.println(userRepository.findByAgeGreaterThanEqual(18, PageRequest.of(0, 2, Sort.by("age").descending())).getContent());
        System.out.println(userRepository.findAverageAge());
        System.out.println(userRepository.searchByName("tud"));
        System.out.println(userRepository.updatePasswordByEmail("cristian.tudor@gmail.com", "parolaNoua123"));
    }
}
