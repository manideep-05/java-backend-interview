# 10 — Spring Boot Internals

> **Priority: P1.** Boot's "magic" is auto-configuration. The interview signal is being able to explain *how* a bean got created and *how to debug* which configuration won — not just that `@SpringBootApplication` "sets everything up."

**Prerequisites:** [09 Spring Core](../09-spring-core-and-dependency-injection/).

---

## A. Concept & intuition
Spring Boot adds opinionated **auto-configuration**, **starters** (curated dependency sets), an **embedded server**, and **externalized configuration** on top of Spring, so you get a running service with minimal setup. The problem it solves: classic Spring required lots of manual XML/Java config and version juggling; Boot infers sensible defaults from the classpath and lets you override them.

## B. Internals

### Auto-configuration
`@SpringBootApplication` = `@SpringBootConfiguration` + `@ComponentScan` + `@EnableAutoConfiguration`. Auto-config classes (listed in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`) are **conditional**: they create beans only when conditions hold — `@ConditionalOnClass` (a library is on the classpath), `@ConditionalOnMissingBean` (you haven't defined your own), `@ConditionalOnProperty`, etc. So adding `spring-boot-starter-data-jpa` + a driver triggers a `DataSource`/`EntityManagerFactory`; defining your own `DataSource` bean makes Boot back off.

### Starters & dependency management
Starters are empty POMs aggregating compatible dependencies; the Boot BOM (`spring-boot-dependencies`) pins consistent versions so you don't resolve conflicting transitive versions yourself.

### Embedded server & startup
Boot embeds Tomcat (default), Jetty, or Undertow — the server lives *in* the app (executable JAR), not the reverse. Startup: create the `ApplicationContext`, run auto-config, start the embedded server, run `ApplicationRunner`/`CommandLineRunner`.

### Externalized configuration & precedence
Config comes from many sources with a defined precedence (highest wins): command-line args → `SPRING_APPLICATION_JSON` → OS env vars → `application-{profile}.yml`/`.properties` → `application.yml` → defaults. Profiles (`spring.profiles.active`) select environment-specific config. `@ConfigurationProperties` binds structured config to typed beans; secrets should come from env/secret managers, not committed files.

### Actuator
`spring-boot-starter-actuator` exposes operational endpoints: `/actuator/health`, `/metrics`, `/info`, `/conditions` (the auto-config report), `/env`, `/threaddump`, `/heapdump`. Lock these down in production.

## C. Code — debugging which bean won
```properties
# See the auto-configuration report: what matched, what didn't, and why
debug=true
# or hit /actuator/conditions at runtime
```
```java
// Override an auto-configured bean: Boot backs off due to @ConditionalOnMissingBean
@Configuration
class DataSourceConfig {
    @Bean DataSource dataSource() { /* custom */ return buildCustom(); } // Boot won't create its own
}
```
```java
// Typed config binding
@ConfigurationProperties(prefix = "app.notification")
record NotificationProps(int retries, Duration timeout) {} // app.notification.retries, .timeout
```

## D. Trade-offs

| Decision | Option | Guidance |
|---|---|---|
| Config source | file vs env/secret manager | secrets via env/secret manager, never committed |
| Server | embedded JAR vs WAR | embedded executable JAR is the modern default |
| Override auto-config | define your own bean vs properties | properties for tuning; own bean to replace |
| Actuator exposure | all vs minimal | expose minimal, secure it |

## E. Interview Q&A

### SB-01 (P1) — What does Spring Boot add over Spring, and how does auto-config work?
**Strong answer:** Starters (curated, version-aligned dependencies), auto-configuration (conditional beans based on classpath/properties/missing beans), an embedded server, and externalized config/actuator. Auto-config classes are `@Conditional`, so they create defaults only when appropriate and back off when I define my own bean (`@ConditionalOnMissingBean`). It's convention over configuration with easy overrides.

<a id="sb-02"></a>
### SB-02 (P1) — How do you find out which auto-configuration created a bean (or why one didn't appear)?
**Strong answer:** Enable `debug=true` (or hit `/actuator/conditions`) to get the auto-configuration report, which lists positive and negative matches and the condition that decided each — e.g., "DataSourceAutoConfiguration matched because a `DataSource` class is present and no user `DataSource` bean exists." For a missing bean I check which `@ConditionalOn...` failed. That report is the fastest way to debug "why is this bean here/absent?"

<a id="sb-03"></a>
### SB-03 (P1) — Explain configuration precedence.
**Strong answer:** Boot layers property sources with a defined order where later/higher sources override earlier ones: command-line args and env vars beat profile-specific files, which beat `application.yml`, which beats code defaults. Active profiles select `application-{profile}` files. This lets the same artifact run across environments by overriding with env vars/secret managers, keeping secrets out of the jar.

### SB-04 (P1) — Why can a Spring Boot app fail to start, and how do you diagnose it?
**Strong answer:** Common causes: a required bean/property missing, a port already in use, two beans matching one injection point (ambiguous), a failed `@ConfigurationProperties` binding, or a classpath/version conflict. Boot's startup failure analyzers print a targeted message and action; I also read the conditions report and the full stack trace, and check for duplicate/clashing dependency versions (the BOM usually prevents those).

### SB-05 (P2) — Embedded server vs traditional WAR?
**Strong answer:** Boot's default is an executable JAR with an embedded server (Tomcat/Jetty/Undertow) — simpler deployment, same artifact everywhere, container-friendly. A WAR deployed to an external servlet container is for legacy/standardized app-server environments. I default to the executable JAR.

## F. Production relevance
Understanding auto-config and the conditions report turns "mysterious bean behavior" into a quick diagnosis — critical during incidents and upgrades. Correct config precedence lets one immutable artifact move across environments with env-var overrides and externalized secrets (security + 12-factor). Actuator health/metrics/threaddump/heapdump endpoints are frontline operational tools (ch.22, 26) — but must be secured. Dependency management via the BOM prevents the version-conflict class of startup failures.

## G. Revision

### 10 key takeaways
1. `@SpringBootApplication` = config + component scan + enable-auto-config.
2. Auto-config beans are `@Conditional`; they back off when you define your own.
3. Starters + BOM pin compatible versions.
4. Embedded server lives in the app (executable JAR).
5. Config precedence: CLI/env > profile files > application.yml > defaults.
6. `@ConfigurationProperties` binds typed config.
7. Secrets via env/secret manager, not committed files.
8. `debug=true` / `/actuator/conditions` explain bean decisions.
9. Startup failure analyzers point to the cause.
10. Secure actuator in production.

### 10 rapid-fire
1. Default embedded server? (Tomcat) 2. Back-off condition? (@ConditionalOnMissingBean) 3. Auto-config report flag? (debug=true) 4. Pins versions? (Boot BOM) 5. Typed config annotation? (@ConfigurationProperties) 6. Highest precedence source? (command-line/env) 7. Health endpoint? (/actuator/health) 8. Select env config? (profiles) 9. Secrets live in? (env/secret manager) 10. Package format? (executable JAR)

### 5 tricky
1. Why didn't my custom DataSource get used / why did Boot create its own? 2. Why did a property not override in prod? (precedence/profile) 3. Why two beans ambiguous at startup? 4. Where to see why a bean is missing? (conditions report) 5. Why avoid committing application-secrets.yml?

### 5 exercises
1. Enable `debug=true` and read the conditions report for a JPA app.
2. Override an auto-configured bean and confirm Boot backs off.
3. Externalize a secret via env var and verify precedence over the yml.
4. Bind `@ConfigurationProperties` to a typed record and inject it.
5. Secure actuator endpoints and expose only health/info.

### One-page revision sheet
> Boot = starters + **conditional auto-config** + embedded server + externalized config/actuator. @SpringBootApplication=config+scan+enableAutoConfig. Auto-config @ConditionalOnClass/OnMissingBean/OnProperty → **backs off when you define your own**. BOM pins versions. **Precedence: CLI/env > profile files > application.yml > defaults.** @ConfigurationProperties=typed binding; secrets via env/secret manager. Debug beans: `debug=true` / /actuator/conditions. Secure actuator. Executable JAR default.

### Prerequisites to revisit
- Bean lifecycle & conditions (ch.09). Operational endpoints feed ch.22/26.
