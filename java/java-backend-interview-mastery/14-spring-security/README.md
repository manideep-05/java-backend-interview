# 14 — Spring Security

> **Priority: P1.** Interviewers probe the filter-chain model, session-vs-JWT trade-offs, and the common JWT mistakes. Security answers must be careful and specific — vague answers here are a red flag.

**Prerequisites:** [11 Spring MVC](../11-spring-mvc-and-rest-apis/), [09 Spring Core](../09-spring-core-and-dependency-injection/).
**Related:** [25 Backend security](../25-backend-security/), [18 API engineering](../18-api-engineering/).

---

## A. Concept & intuition
**Authentication** = who you are; **Authorization** = what you're allowed to do. Spring Security implements both as a chain of servlet filters in front of your app, establishing a `SecurityContext` for the request and enforcing access rules. It exists so you don't hand-roll auth (and get it wrong).

## B. Internals

### The security filter chain
Spring Security is a `FilterChainProxy` of ordered filters, each handling one concern: extract credentials/token, authenticate, populate the `SecurityContext`, enforce authorization, handle CSRF/CORS, and translate auth failures to 401/403. For a request:
1. A filter extracts credentials (session cookie, `Authorization: Bearer <jwt>`, basic auth).
2. An `AuthenticationManager`/provider validates them, producing an `Authentication` with authorities.
3. It's stored in the `SecurityContextHolder` (a `ThreadLocal` — so it doesn't cross threads, like transactions; propagate it to async work explicitly, [06 §13](../06-java-concurrency/)).
4. Authorization filters/`@PreAuthorize` check authorities before the handler runs.

### Session vs JWT
- **Session-based**: server stores session state; the client holds a session cookie. Easy to revoke (drop the session), but stateful (sticky sessions / shared store needed to scale).
- **JWT (stateless)**: a signed token carries claims (sub, roles, exp); the server validates the signature and expiry without server state — scales horizontally, but **revocation is hard** (the token is valid until it expires). Mitigate with short-lived access tokens + refresh tokens and a revocation/denylist for the refresh token.

### Passwords
Store only a salted, slow **hash** (BCrypt/Argon2 via `PasswordEncoder`), never plaintext or fast hashes (MD5/SHA). Spring's `DelegatingPasswordEncoder` supports upgrades.

### CORS vs CSRF (commonly confused)
- **CORS** is a browser mechanism controlling which origins may call your API (you configure allowed origins/methods). Not a security boundary by itself.
- **CSRF** is an attack where a browser is tricked into sending an authenticated request using the user's cookie. CSRF protection (tokens) matters for **cookie/session** auth; for stateless **Bearer-token** APIs that don't use cookies, CSRF protection is typically disabled because the attack vector (automatic cookie attachment) doesn't apply.

## C. Code — a modern stateless config (Spring Security 6)
```java
@Configuration
@EnableMethodSecurity           // enables @PreAuthorize
class SecurityConfig {
    @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http
          .csrf(csrf -> csrf.disable())                 // OK for stateless Bearer APIs (no cookies)
          .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(a -> a
              .requestMatchers("/api/v1/public/**").permitAll()
              .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
              .anyRequest().authenticated())
          .oauth2ResourceServer(o -> o.jwt(org.springframework.security.config.Customizer.withDefaults()));
        return http.build();
    }
    @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }
}
```
```java
// Method-level authorization
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
public UserDto get(String userId) { ... } // deny unless admin or the owner
```

## D. Trade-offs

| Choice | Pros | Cons | Use when |
|---|---|---|---|
| Session | easy revocation, simple | stateful, scaling (sticky/shared store) | server-rendered apps, internal tools |
| JWT (access+refresh) | stateless, scales | revocation hard, token size, clock skew | stateless APIs, microservices |
| Role-based | simple | coarse | few roles |
| Authority/scope-based | fine-grained | more to manage | granular permissions |

## E. Interview Q&A

### SEC-01 (P1) — Authentication vs authorization; where does Spring Security do each?
**Strong answer:** Authentication proves identity (validate credentials/token → `Authentication` in the `SecurityContext`); authorization decides access (check authorities via URL rules or `@PreAuthorize`). Both happen in the filter chain before my controller: an auth filter populates the context, an authorization filter/method-security advisor enforces rules. The context is thread-bound, so async work needs explicit propagation.

<a id="sec-02"></a>
### SEC-02 (P1) — Common JWT implementation mistakes?
**Strong answer:** Not verifying the signature or accepting the `alg: none` / algorithm-confusion attack (treating an RS256 token as HS256 with the public key as the secret); not checking `exp` (and `nbf`/`iss`/`aud`); making access tokens long-lived so they can't be revoked (use short-lived access + refresh tokens + a refresh denylist); putting sensitive data in the payload (it's only base64, not encrypted); storing the token in `localStorage` where XSS can steal it (prefer httpOnly cookies or careful handling); and ignoring clock skew. I validate signature + standard claims, keep access tokens short-lived, and never trust unverified claims.
**Weak answer:** "JWT is secure because it's signed." Signed ≠ confidential, and signature-verification/expiry bugs are exactly where JWT breaks.

### SEC-03 (P1) — Session vs stateless (JWT) auth — trade-offs?
**Strong answer:** Sessions keep state server-side; revocation is trivial (delete the session) but scaling needs sticky sessions or a shared store. JWTs are stateless and scale horizontally, but you can't easily revoke a valid, unexpired token — so I use short-lived access tokens with refresh tokens and a denylist for refresh revocation. I pick sessions for server-rendered/internal apps and stateless tokens for distributed APIs.

### SEC-04 (P1) — CORS vs CSRF — what's the difference and when do you disable CSRF?
**Strong answer:** CORS is a browser policy controlling which origins can call the API; CSRF is an attack exploiting the browser auto-sending cookies on cross-site requests. CSRF protection matters for cookie/session auth; for a stateless Bearer-token API that doesn't use cookies, there's no automatic credential attachment, so CSRF protection is commonly disabled. Disabling CSRF on a cookie-authenticated app, however, would be a real vulnerability.

### SEC-05 (P1) — How should passwords be stored?
**Strong answer:** As a salted, computationally slow hash (BCrypt or Argon2) via a `PasswordEncoder`, never plaintext or fast hashes like MD5/SHA-256. The slowness and per-password salt defeat brute-force and rainbow tables. Spring's `DelegatingPasswordEncoder` lets me upgrade algorithms over time.

### SEC-06 (P2) — Role-based vs authority/scope-based access?
**Strong answer:** Roles are coarse groupings (`ROLE_ADMIN`); authorities/scopes are fine-grained permissions (`document:read`). I use roles for simple cases and authorities/scopes when access needs granularity, enforced with `hasAuthority`/`@PreAuthorize` expressions.

## F. Production relevance
Security mistakes are high-severity: unverified JWT signatures or missing expiry checks allow forged access; plaintext/fast-hashed passwords turn a DB leak into account takeover; disabling CSRF on a cookie app enables cross-site attacks; leaking role logic or stack traces aids attackers. The safe defaults: validate signature + claims, short-lived tokens + refresh denylist, BCrypt/Argon2, least-privilege authorities, secure actuator and secrets, and consistent 401/403 without information leakage. Pair with [25 Backend security](../25-backend-security/) for injection/input concerns.

## G. Revision

### 10 key takeaways
1. Authn = identity; authz = permissions; both in the filter chain pre-controller.
2. SecurityContext is thread-bound (propagate to async).
3. Sessions: easy revoke, stateful; JWT: stateless, hard revoke.
4. Use short-lived access + refresh tokens + refresh denylist.
5. Always verify JWT signature + exp/iss/aud; reject alg confusion.
6. JWT payload is not encrypted — no secrets in it.
7. Hash passwords with BCrypt/Argon2 + salt; never fast hashes.
8. CORS ≠ CSRF; disable CSRF only for cookieless stateless APIs.
9. Prefer least-privilege authorities/scopes; `@PreAuthorize` for method security.
10. Secure actuator and secrets; consistent 401/403.

### 10 rapid-fire
1. Who-you-are? (authn) 2. What-you-can-do? (authz) 3. Context storage? (ThreadLocal) 4. Password hash? (BCrypt/Argon2) 5. JWT revoke easily? (no) 6. Verify in JWT? (signature+exp) 7. CSRF matters for? (cookie auth) 8. Stateless session policy? (STATELESS) 9. Method security annotation? (@PreAuthorize) 10. JWT payload encrypted? (no, base64)

### 5 tricky
1. Why is a signed JWT not confidential? 2. Why can't you instantly revoke a JWT? 3. Why disable CSRF for a Bearer API but not a cookie app? 4. Why is MD5 password hashing dangerous? 5. What is the `alg:none`/algorithm-confusion attack?

### 5 exercises
1. Configure a stateless `SecurityFilterChain` with JWT resource server and role rules.
2. Add `@PreAuthorize` owner-or-admin checks and test both paths.
3. Implement BCrypt password hashing and verification.
4. Demonstrate why putting a secret in a JWT payload leaks it (decode base64).
5. Set up short-lived access + refresh tokens with a refresh denylist.

### One-page revision sheet
> Authn(identity)+authz(permissions) in the **filter chain** → Authentication in SecurityContext (ThreadLocal, propagate to async). Session(easy revoke, stateful) vs **JWT(stateless, hard revoke)** → short access + refresh + denylist. **Always verify JWT signature + exp/iss/aud; reject alg confusion; payload is base64 not encrypted.** Passwords: **BCrypt/Argon2 + salt**, never MD5/SHA. CORS(origin policy)≠CSRF(cookie attack); disable CSRF only for cookieless Bearer APIs. Least-privilege authorities/scopes; @PreAuthorize. Secure actuator/secrets; consistent 401/403.

### Prerequisites to revisit
- Filter chain & request lifecycle (ch.11); thread-bound context (ch.06). Deeper threats in ch.25.
