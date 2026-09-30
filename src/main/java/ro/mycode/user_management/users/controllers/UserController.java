package ro.mycode.user_management.users.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import ro.mycode.user_management.users.dtos.AverageAgeResponse;
import ro.mycode.user_management.users.dtos.ChangePasswordRequest;
import ro.mycode.user_management.users.dtos.EmailExistsResponse;
import ro.mycode.user_management.users.dtos.PageResponse;
import ro.mycode.user_management.users.dtos.UserCountResponse;
import ro.mycode.user_management.users.dtos.UserCreateRequest;
import ro.mycode.user_management.users.dtos.UserResponse;
import ro.mycode.user_management.users.dtos.UserSummary;
import ro.mycode.user_management.users.dtos.UserUpdateRequest;
import ro.mycode.user_management.users.services.interfaces.UserCommandService;
import ro.mycode.user_management.users.services.interfaces.UserQueryService;
import ro.mycode.user_management.web.ApiError;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Utilizatori", description = "Creare, citire, modificare si stergere de utilizatori")
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public UserController(UserCommandService userCommandService, UserQueryService userQueryService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
    }

    @PostMapping
    @Operation(summary = "Creeaza un utilizator",
            description = "Singura ruta care raspunde 201. Header-ul `Location` din raspuns "
                    + "conține adresa resursei create.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utilizator creat"),
            @ApiResponse(responseCode = "400", description = "Corp invalid; `details` spune ce camp",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Emailul este deja folosit",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))})
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request,
                                               @Parameter(hidden = true) UriComponentsBuilder uriBuilder) {

        UserResponse created = userCommandService.addUser(request);

        URI location = uriBuilder.path("/api/users/{id}").buildAndExpand(created.id()).toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Lista tuturor utilizatorilor",
            description = "Sortata dupa nume, apoi prenume. Pe o baza goala raspunde 200 cu `[]`.")
    @ApiResponse(responseCode = "200", description = "Lista, eventual goala")
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userQueryService.getUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Un utilizator dupa id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilizatorul cerut"),
            @ApiResponse(responseCode = "400", description = "Id-ul nu este un UUID",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Nu exista utilizator cu acest id",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))})
    public ResponseEntity<UserResponse> getById(
            @Parameter(description = "Identificatorul utilizatorului",
                    example = "3f2a8c14-5b77-4e21-9d3e-8a1c6b0f42de")
            @PathVariable UUID id) {

        return ResponseEntity.ok(userQueryService.getUserById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Caută utilizatori, paginat",
            description = "Filtrele sunt optionale si se combina. `name` se potrivește pe prenume "
                    + "SAU pe nume, fara sa conteze litera mare. Fara filtre, intoarce tot.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de rezultate"),
            @ApiResponse(responseCode = "400", description = "`minAge` nu e pozitiv sau nu e numar",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))})
    public ResponseEntity<PageResponse<UserResponse>> search(
            @Parameter(description = "Fragment din prenume sau nume", example = "tud")
            @RequestParam(required = false) String name,

            @Parameter(description = "Varsta minima, inclusiv", example = "18")
            @RequestParam(required = false) @Positive(message = "minAge must be greater than zero") Integer minAge,

            @ParameterObject
            @PageableDefault(size = 10, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(userQueryService.search(name, minAge, pageable));
    }

    @GetMapping("/by-email")
    @Operation(summary = "Un utilizator dupa email",
            description = "Pentru a verifica doar existența, fara 404, foloseste `/exists`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilizatorul cu acest email"),
            @ApiResponse(responseCode = "400", description = "Email lipsa sau malformat",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Nu exista utilizator cu acest email",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))})
    public ResponseEntity<UserResponse> getByEmail(
            @Parameter(description = "Emailul cautat, potrivire exacta",
                    example = "cristian.tudor@gmail.com", required = true)
            @RequestParam @NotBlank(message = "email is required")
            @Email(message = "Email must be a valid address") String email) {

        return ResponseEntity.ok(userQueryService.getUserByEmail(email));
    }

    @GetMapping("/by-name")
    @Operation(summary = "Utilizatorii cu un prenume si un nume date",
            description = "Ambele criterii trebuie indeplinite simultan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista, eventual goala"),
            @ApiResponse(responseCode = "400", description = "Lipsește un parametru obligatoriu",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))})
    public ResponseEntity<List<UserResponse>> getByFullName(
            @Parameter(description = "Prenumele exact", example = "Cristian", required = true)
            @RequestParam String firstName,

            @Parameter(description = "Numele exact", example = "Tudor", required = true)
            @RequestParam String lastName) {

        return ResponseEntity.ok(userQueryService.getUsersByFullName(firstName, lastName));
    }

    @GetMapping("/by-last-name")
    @Operation(summary = "Utilizatorii al caror nume conține un fragment",
            description = "Potrivire pe fragment, fara sa conteze litera mare.")
    @ApiResponse(responseCode = "200", description = "Lista, eventual goala")
    public ResponseEntity<List<UserResponse>> getByLastNameFragment(
            @Parameter(description = "Fragmentul cautat in numele de familie",
                    example = "ste", required = true)
            @RequestParam String contains) {

        return ResponseEntity.ok(userQueryService.getUsersByLastNameFragment(contains));
    }

    @GetMapping("/by-emails")
    @Operation(summary = "Utilizatorii cu emailurile date",
            description = "Emailurile care nu exista sunt ignorate in silențiu; lista intoarsa "
                    + "poate fi mai scurta decat cea cerută.")
    @ApiResponse(responseCode = "200", description = "Utilizatorii gasiți")
    public ResponseEntity<List<UserResponse>> getByEmails(
            @Parameter(description = "Emailuri separate prin virgula",
                    example = "cristian.tudor@gmail.com,ana.stefanescu@yahoo.com", required = true)
            @RequestParam List<String> emails) {

        return ResponseEntity.ok(userQueryService.getUsersByEmails(emails));
    }

    @GetMapping("/by-domain")
    @Operation(summary = "Utilizatorii cu email pe un domeniu")
    @ApiResponse(responseCode = "200", description = "Lista, eventual goala")
    public ResponseEntity<List<UserResponse>> getByEmailDomain(
            @Parameter(description = "Sufixul emailului; implicit `@gmail.com`", example = "@yahoo.com")
            @RequestParam(defaultValue = "@gmail.com") String domain) {

        return ResponseEntity.ok(userQueryService.getUsersByEmailDomain(domain));
    }

    @GetMapping("/older-than")
    @Operation(summary = "Utilizatorii mai in varsta de o limita",
            description = "Limita este exclusiva: `age=30` nu intoarce utilizatorii de 30 de ani.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista, eventual goala"),
            @ApiResponse(responseCode = "400", description = "`age` lipsește sau nu e numar",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))})
    public ResponseEntity<List<UserResponse>> getOlderThan(
            @Parameter(description = "Varsta, exclusiva", example = "25", required = true)
            @RequestParam int age) {

        return ResponseEntity.ok(userQueryService.getUsersOlderThan(age));
    }

    @GetMapping("/between-ages")
    @Operation(summary = "Utilizatorii dintr-un interval de varsta",
            description = "Ambele limite sunt inclusive.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista, eventual goala"),
            @ApiResponse(responseCode = "400", description = "Lipsește o limita sau nu e numar",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))})
    public ResponseEntity<List<UserResponse>> getBetweenAges(
            @Parameter(description = "Varsta minima, inclusiva", example = "18", required = true)
            @RequestParam int minAge,

            @Parameter(description = "Varsta maxima, inclusiva", example = "30", required = true)
            @RequestParam int maxAge) {

        return ResponseEntity.ok(userQueryService.getUsersBetweenAges(minAge, maxAge));
    }

    @GetMapping("/adults")
    @Operation(summary = "Utilizatorii majori",
            description = "Limita este inclusiva, spre deosebire de `/older-than`.")
    @ApiResponse(responseCode = "200", description = "Lista, eventual goala")
    public ResponseEntity<List<UserResponse>> getAdults(
            @Parameter(description = "Varsta minima, inclusiva; implicit 18", example = "18")
            @RequestParam(defaultValue = "18") int minAge) {

        return ResponseEntity.ok(userQueryService.getAdultUsers(minAge));
    }

    @GetMapping("/from-age")
    @Operation(summary = "Utilizatorii de la o varsta in sus, paginat",
            description = "Paginare implicita diferita de `/search`: 2 pe pagina, sortat "
                    + "descrescator pe varsta.")
    @ApiResponse(responseCode = "200", description = "Pagina de rezultate")
    public ResponseEntity<PageResponse<UserResponse>> getFromAge(
            @Parameter(description = "Varsta minima, inclusiva; implicit 18", example = "18")
            @RequestParam(defaultValue = "18") int age,

            @ParameterObject
            @PageableDefault(size = 2, sort = "age", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(userQueryService.getUsersFromAge(age, pageable));
    }

    @GetMapping("/top3-by-age")
    @Operation(summary = "Cei mai in varsta trei utilizatori",
            description = "Intoarce mai puțin de trei daca nu exista atatia.")
    @ApiResponse(responseCode = "200", description = "Cel mult trei utilizatori")
    public ResponseEntity<List<UserResponse>> getTop3ByAge() {
        return ResponseEntity.ok(userQueryService.getTop3ByAge());
    }

    @GetMapping("/summaries")
    @Operation(summary = "Proiecție cu trei campuri, pentru un nume de familie",
            description = "Citește din baza doar id, prenume si email, fara a incarca entitatea.")
    @ApiResponse(responseCode = "200", description = "Lista de proiecții, eventual goala")
    public ResponseEntity<List<UserSummary>> getSummaries(
            @Parameter(description = "Numele de familie, potrivire exacta",
                    example = "Tudor", required = true)
            @RequestParam String lastName) {

        return ResponseEntity.ok(userQueryService.getSummariesByLastName(lastName));
    }

    @GetMapping("/exists")
    @Operation(summary = "Daca un email este deja folosit",
            description = "Un email inexistent NU este o eroare: raspunsul este 200 cu "
                    + "`exists: false`. Pentru a obține utilizatorul, foloseste `/by-email`.")
    @ApiResponse(responseCode = "200", description = "Raspunsul da/nu")
    public ResponseEntity<EmailExistsResponse> emailExists(
            @Parameter(description = "Emailul verificat",
                    example = "cristian.tudor@gmail.com", required = true)
            @RequestParam String email) {

        return ResponseEntity.ok(new EmailExistsResponse(email, userQueryService.emailExists(email)));
    }

    @GetMapping("/count")
    @Operation(summary = "Cati utilizatori sunt sub o varsta",
            description = "Limita este exclusiva.")
    @ApiResponse(responseCode = "200", description = "Numarul cerut")
    public ResponseEntity<UserCountResponse> countYoungerThan(
            @Parameter(description = "Varsta, exclusiva", example = "30", required = true)
            @RequestParam int youngerThan) {

        return ResponseEntity.ok(
                new UserCountResponse(youngerThan, userQueryService.countUsersYoungerThan(youngerThan)));
    }

    @GetMapping("/average-age")
    @Operation(summary = "Media varstelor",
            description = "Pe o baza goala raspunde 200 cu `averageAge: null`, la fel cum lista "
                    + "raspunde 200 cu `[]`. Lipsa datelor nu este o eroare.")
    @ApiResponse(responseCode = "200", description = "Media, sau null daca nu exista utilizatori")
    public ResponseEntity<AverageAgeResponse> averageAge() {
        return ResponseEntity.ok(new AverageAgeResponse(userQueryService.getAverageAge()));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Modifica parțial un utilizator",
            description = "PATCH, nu PUT: campurile lipsa sau goale lasa valorile vechi neatinse. "
                    + "`PUT /api/users/{id}` nu exista si raspunde 405.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilizatorul, dupa modificare"),
            @ApiResponse(responseCode = "400", description = "Corp invalid",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Nu exista utilizator cu acest id",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Emailul nou aparține altui utilizator",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))})
    public ResponseEntity<UserResponse> update(
            @Parameter(description = "Identificatorul utilizatorului",
                    example = "3f2a8c14-5b77-4e21-9d3e-8a1c6b0f42de")
            @PathVariable UUID id,

            @Valid @RequestBody UserUpdateRequest request) {

        return ResponseEntity.ok(userCommandService.updateUser(id, request));
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Inlocuieste parola unui utilizator",
            description = "PUT, pentru ca parola este inlocuita in intregime. Raspunsul conține "
                    + "utilizatorul, niciodata parola.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilizatorul caruia i s-a schimbat parola"),
            @ApiResponse(responseCode = "400", description = "Parola lipsa sau mai scurta de 8 caractere",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Nu exista utilizator cu acest id",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))})
    public ResponseEntity<UserResponse> changePassword(
            @Parameter(description = "Identificatorul utilizatorului",
                    example = "3f2a8c14-5b77-4e21-9d3e-8a1c6b0f42de")
            @PathVariable UUID id,

            @Valid @RequestBody ChangePasswordRequest request) {

        return ResponseEntity.ok(userCommandService.changePassword(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Sterge un utilizator",
            description = "Raspunsul conține reprezentarea utilizatorului de dinainte de stergere, "
                    + "ca apelantul sa poata afisa ce a sters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilizatorul sters"),
            @ApiResponse(responseCode = "400", description = "Id-ul nu este un UUID",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Nu exista utilizator cu acest id",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))})
    public ResponseEntity<UserResponse> delete(
            @Parameter(description = "Identificatorul utilizatorului",
                    example = "3f2a8c14-5b77-4e21-9d3e-8a1c6b0f42de")
            @PathVariable UUID id) {

        return ResponseEntity.ok(userCommandService.deleteUser(id));
    }
}
