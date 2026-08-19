package ro.mycode.user_management;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ro.mycode.user_management.users.dtos.UserCreateRequest;
import ro.mycode.user_management.users.repository.UserRepository;
import ro.mycode.user_management.users.services.interfaces.UserCommandService;

import java.util.List;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserCommandService userCommandService;

    public DataSeeder(UserRepository userRepository, UserCommandService userCommandService) {
        this.userRepository = userRepository;
        this.userCommandService = userCommandService;
    }

    @Override
    public void run(String... args) {

        if (userRepository.count() > 0) {
            return;
        }

        List<UserCreateRequest> seed = List.of(
                new UserCreateRequest("Cristian", "Tudor", "cristian.tudor@gmail.com", "parola123", 30),
                new UserCreateRequest("Bogdan", "Alexandrescu", "bogdan.alexandrescu@gmail.com", "parola456", 41),
                new UserCreateRequest("Ana", "Stefanescu", "ana.stefanescu@yahoo.com", "parola789", 19),
                new UserCreateRequest("Radu", "Popescu", "radu.popescu@gmail.com", "parola000", 17),
                new UserCreateRequest("Maria", "Stere", "maria.stere@gmail.com", "parola111", 26));

        seed.forEach(userCommandService::addUser);

        System.out.println("Seeded " + seed.size() + " users");
    }
}
