# Add a module

A module is a business capability with its own package, schema and tag in the contract. Copy the shape of `notes`; background in [Architecture](../../architecture/overview.md).

1. **Package**: create `services/api/src/main/java/dev/ylp/<module>/` with a `package-info.java` annotated `@ApplicationModule(displayName = "...")`, and the sub-packages `domain`, `application`, `adapter/in/rest`, `adapter/out/postgres`.
2. **Hexagonal roles**: annotate the ports with `@SecondaryPort`, and add `package-info.java` files on `adapter/in` (`@PrimaryAdapter`) and `adapter/out` (`@SecondaryAdapter`), as in `notes`.
3. **Domain**: the aggregate (`@AggregateRoot`, identity field `@Identity`), its value objects as self-validating records, its repository port, and an error reason enum implementing `BusinessErrorReason` with the right `category()`.
4. **Use cases**: one class per use case with an `execute(Command)` method, declared as beans in a package-private `<Module>Configuration` class. Inject `Clock` and `UUIDGenerator` rather than calling `Instant.now()` or `UUID.randomUUID()`.
5. **Persistence**: a Liquibase changelog in `src/main/resources/db/changelog/<module>/` that creates the module's own schema and tables, included from `db.changelog-master.yaml`; a JPA entity mapped by hand to the domain; a package-private Spring Data repository wrapped by the adapter that implements the port.
6. **Contract**: a `<module>.yaml` file with its tag, referenced from `openapi.yaml`, then `./gradlew :bruno:updateCollection`. With two modules, the server code generation must split by tag so that each module owns its DTOs (issue #14).
7. **REST adapter**: a controller implementing the generated interface, a mapper, and `CurrentOwner` if resources are owned.
8. **Tests**: domain and use case tests without Spring, an abstract contract test for each port extended by the adapter's Testcontainers test, a `@WebMvcTest` for the controller, and an end-to-end ownership test for each owned resource.
9. **Verify**: `./gradlew check`. `ModularityTest` and `HexagonalArchitectureTest` fail on any boundary violation, and the coverage gate needs the new code tested.

When a module needs another module's data, call its use cases or listen to its events; never read its tables.
