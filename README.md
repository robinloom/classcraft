# ClassCraft

Zero-boilerplate Design Patterns for Java.

ClassCraft is a compile-time annotation processor: annotate a class (or one of its
constructors/fields), and it generates a companion class next to it — no reflection,
no runtime dependency on ClassCraft itself, fully debuggable/steppable generated
source under `target/generated-sources`.

## Features

### `@GenerateBuilder`

Staged builder on a constructor. Required parameters must be supplied — in
declared order — before `build()` becomes available; `@Nullable` parameters are
optional and can be set in any order afterwards.

```java
public class User {
    @GenerateBuilder
    public User(String name, String email, @Nullable String phone) { ... }
}

User alice = UserBuilder.builder()
    .name("Alice")
    .email("alice@example.com")
    .phone("555-1234")
    .build();
```

### `@GenerateDTO`

A plain companion class with the same fields, getters (+ setters unless
`mutable = false`), an all-args constructor, and optionally
`equals()`/`hashCode()`/`toString()`.

```java
@GenerateDTO
public class User { private final String name; private final String email; ... }

UserDTO dto = new UserDTO("Alice", "alice@example.com");
```

### `@GenerateMapper`

Bidirectional static mapper between two classes with the same (non-`@Ignore`d)
fields, using setters or an all-args constructor depending on which the target
class provides.

```java
@GenerateMapper(to = "com.example.UserDTO")
public class User { ... }

UserDTO dto = UserMapper.toUserDTO(user);
User restored = UserMapper.toUser(dto);
```

### `@GenerateWither`

Copy-with-one-field-changed, without a builder or manual field-by-field copying.

```java
@GenerateWither
public class User { ... }

User updated = UserWither.withEmail(alice, "alice@work.example.com");
```

### `@GenerateLogger`

A structured, reflection-free renderer/logger — complementary to
[JWeaver](https://github.com/robinloom/jweaver)'s generic runtime object
rendering: this one is compile-time and tailored per class.

```java
@GenerateLogger
public class User { ... }

UserLogger.line(user);   // User{name="Alice", email="alice@example.com"} — single line, log-pipeline-safe
UserLogger.tree(user);   // multi-line ASCII tree — for console/debug output
UserLogger.info("User created", user); // SLF4J convenience wrapper, built on line()
```

### Field markers

- **`@Ignore`** — skip a field in `@GenerateDTO`/`@GenerateWither`/`@GenerateMapper`/`@GenerateLogger`.
- **`@Sensitive`** — mask a field's value in `@GenerateLogger`'s output instead of reading it (`mask` attribute, default `"***"`). Has no effect on the other generators.
- **`@Nullable`** — marks a `@GenerateBuilder` constructor parameter as optional. Also recognizes JSpecify, JSR-305, JetBrains and Checker Framework `@Nullable` equivalents.

All generator annotations accept a `suffix` attribute to customize the generated
class name (e.g. `@GenerateDTO(suffix = "TO")` → `UserTO` instead of `UserDTO`).

## License

[Apache License 2.0](LICENSE.txt)
