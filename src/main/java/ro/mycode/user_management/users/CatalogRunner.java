package ro.mycode.user_management.users;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ro.mycode.user_management.users.repository.UserRepository;

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
    }
}
