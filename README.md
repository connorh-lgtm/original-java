# Legacy RealWorld App

The **incrementally modernized** version of a legacy Java 8 servlet-based blog API implementing the [RealWorld](https://github.com/gothinkster/realworld) specification. This branch represents the **"after" state**, showing how the legacy codebase can be improved step by step without a full rewrite.

> **See the [`main` branch](https://github.com/connorh-lgtm/original-java/tree/main) for the original legacy code.** The [Spring Boot RealWorld Example App](https://github.com/connorh-lgtm/spring-boot-realworld-example-app) represents the full-framework modernization target.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 8 |
| Web Framework | Raw `javax.servlet.http.HttpServlet` |
| Build System | Apache Ant with manual JAR management |
| Database | SQLite via raw JDBC |
| JSON | Gson (manual serialization) |
| Auth | `HttpSession`-based (no JWT) |
| Configuration | XML (`web.xml`) |
| Dependency Injection | None (static `ServiceLocator`) |
| ORM | None (inline SQL with `PreparedStatement`) |

## Project Structure

```
legacy-realworld-app/
├── build.xml                          # Ant build file (manual classpath)
├── lib/                               # Manually managed JAR dependencies
│   └── README.txt                     #   Download instructions for JARs
├── web/
│   └── WEB-INF/
│       └── web.xml                    # Servlet declarations & mappings
└── src/com/legacy/realworld/
    ├── model/
    │   ├── Article.java               # Article entity (manual getters/setters)
    │   ├── User.java                  # User entity (plain text password!)
    │   └── Comment.java               # Comment entity
    ├── servlet/
    │   ├── ArticleServlet.java        # Fat servlet: CRUD + SQL + auth + JSON
    │   ├── UserServlet.java           # Fat servlet: register + login
    │   └── CommentServlet.java        # Fat servlet: comment CRUD
    └── util/
        ├── DatabaseUtil.java          # Static DB access (no connection pool)
        ├── AuthUtil.java              # Static session-based auth checks
        ├── JsonUtil.java              # Manual JSON serialization helpers
        └── ServiceLocator.java        # Poor man's dependency injection
```

## API Endpoints

These match the [RealWorld API spec](https://realworld-docs.netlify.app/specifications/backend/endpoints/) and are compatible with the modern Spring Boot version:

| Method | URL | Description |
|--------|-----|-------------|
| `POST` | `/api/users` | Register a new user |
| `POST` | `/api/users/login` | Login |
| `GET` | `/api/users` | Get current user |
| `GET` | `/api/articles` | List articles |
| `GET` | `/api/articles/:slug` | Get article by slug |
| `POST` | `/api/articles` | Create article (auth required) |
| `PUT` | `/api/articles/:slug` | Update article (auth required) |
| `DELETE` | `/api/articles/:slug` | Delete article (auth required) |
| `GET` | `/api/articles/:slug/comments` | List comments for article |
| `POST` | `/api/articles/:slug/comments` | Add comment (auth required) |
| `DELETE` | `/api/articles/:slug/comments/:id` | Delete comment (auth required) |

## Anti-Patterns Present (For Demo Purposes)

This codebase intentionally demonstrates the following legacy anti-patterns:

### Architecture & Design
- **No dependency injection framework** — Static `ServiceLocator` with string-based lookups instead of constructor injection
- **Fat servlets with mixed concerns** — Business logic, SQL, auth, and JSON serialization all in one class
- **Manual servlet configuration via `web.xml`** — No annotation-based routing

### Data Access
- **Inline SQL with no ORM or mapper** — Raw JDBC `PreparedStatement` with SQL strings in servlet code
- **No connection pooling** — `DriverManager.getConnection()` called on every request
- **No database migrations** — `CREATE TABLE IF NOT EXISTS` in a static initializer
- **No indexes** — All queries do full table scans

### Security
- **Plain text password storage** — No hashing, no salting, no `PasswordEncoder`
- **Password comparison with `.equals()`** — Vulnerable to timing attacks
- **Session-based auth with manual checks** — `HttpSession` instead of stateless JWT
- **Auth checks copy-pasted** in every servlet method instead of a filter

### Data Handling
- **`java.util.Date` with `SimpleDateFormat`** — Not thread-safe; shared static instances will corrupt under concurrent access
- **Manual JSON serialization** — Field-by-field `JsonObject` construction instead of annotation-driven serialization
- **No input validation framework** — Missing fields silently become `null`

### Code Quality
- **No automated tests** — Zero test coverage
- **`e.printStackTrace()` for error handling** — No logging framework
- **Manual resource cleanup** — `try/finally` blocks instead of try-with-resources
- **Dead code and commented-out blocks** — Abandoned features left in place
- **Ant build with manual JAR management** — Dependencies downloaded by hand into `lib/`

## Building

```bash
# 1. Download required JARs into lib/ (see lib/README.txt)
# 2. Build with Ant
ant clean compile war

# The WAR file will be at build/legacy-realworld.war
# Deploy to any Servlet 3.1+ container (Tomcat 8+, Jetty 9+, etc.)
```

## Database

Uses SQLite with a file-based database (`realworld.db`). The schema is auto-created on first startup via `DatabaseUtil.initializeDatabase()`. Both this legacy app and the modern Spring Boot version use SQLite, so you could theoretically point both at the same database file.

## Modernization Target

The modern equivalent of this codebase is the **[Spring Boot RealWorld Example App](https://github.com/connorh-lgtm/spring-boot-realworld-example-app)**, which demonstrates:

- Java 17 with Spring Boot 2.7
- Constructor-based dependency injection
- Clean layered architecture (Controller → Service → Repository)
- MyBatis data mappers with Flyway migrations
- Spring Security with JWT authentication
- `java.time.Instant` for timestamps
- Jackson with annotation-driven serialization
- Bean Validation (`@Valid`, `@NotBlank`)
- Comprehensive test suite
- Gradle build with dependency resolution

## File-by-File Comparison

| Legacy File | Modern Equivalent(s) | Key Improvements |
|---|---|---|
| `build.xml` + `lib/` | `build.gradle` | Gradle dependency resolution, no manual JARs |
| `web/WEB-INF/web.xml` | `@RestController` + `@RequestMapping` annotations | Annotation-based routing, no XML |
| `model/Article.java` | `core/article/Article.java` | Lombok `@Getter`, `java.time.Instant`, `@EqualsAndHashCode` |
| `model/User.java` | `core/user/User.java` | Lombok, password never stored as plain text |
| `model/Comment.java` | `core/comment/Comment.java` | Lombok, `Instant` timestamps |
| `servlet/ArticleServlet.java` | `api/ArticlesApi.java` + `api/ArticleApi.java` + `ArticleCommandService` + MyBatis mappers | Separate controller/service/repository layers |
| `servlet/UserServlet.java` | `api/UsersApi.java` + `UserService` + `PasswordEncoder` + `JwtService` | Hashed passwords, JWT tokens, validation |
| `servlet/CommentServlet.java` | `api/CommentsApi.java` + `CommentRepository` + `CommentQueryService` | `@PathVariable`, clean separation |
| `util/DatabaseUtil.java` | Spring Boot auto-config + HikariCP + Flyway | Connection pooling, migrations, auto-configuration |
| `util/AuthUtil.java` | `security/JwtTokenFilter.java` + Spring Security | Single filter, JWT validation, `@AuthenticationPrincipal` |
| `util/JsonUtil.java` | Jackson auto-configuration + `@JsonIgnore` | Automatic serialization, annotation-driven |
| `util/ServiceLocator.java` | Spring `ApplicationContext` + `@Autowired` | Type-safe DI, lifecycle management, testability |

## Demo Walkthrough Suggestions

1. **Start with `UserServlet.java`** — Show plain text passwords and `.equals()` comparison, then show Spring Security's `PasswordEncoder`
2. **Compare `ArticleServlet.java` with `ArticlesApi.java`** — Count the lines, show the mixed concerns vs. clean layers
3. **Show `DatabaseUtil.getConnection()`** — Explain why connection pooling matters, show HikariCP config
4. **Highlight the `SimpleDateFormat` thread-safety bug** — This is a classic Java concurrency trap
5. **Walk through `web.xml` vs. `@RequestMapping`** — XML config vs. annotations
6. **Show `ServiceLocator` vs. `@Autowired`** — Static registry vs. constructor injection

## License

This project is for demonstration and educational purposes.
