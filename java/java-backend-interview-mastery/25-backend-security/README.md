# 25 — Backend Security

> **Priority: P1.** Beyond Spring Security's mechanics ([14](../14-spring-security/)), this is the OWASP-relevant engineering: injection, input validation, secrets, and secure defaults. Security answers must be precise.

**Prerequisites:** [14 Spring Security](../14-spring-security/), [15 SQL](../15-sql-and-relational-databases/), [18 API](../18-api-engineering/).

---

## A. Concept & intuition
Security is about not trusting input and limiting blast radius. Most backend vulnerabilities come from treating untrusted data as trusted (injection, deserialization), weak authn/authz, or exposed secrets. Secure defaults (parameterized queries, least privilege, encryption, validation) prevent whole classes of bugs cheaply.

## B. Core threats & defenses

### Injection (SQL and friends)
Concatenating user input into a query lets an attacker alter it. **Defense: parameterized queries / prepared statements** always — never string-concatenate user input into SQL. JPA/`@Query` with bound parameters and `PreparedStatement` are safe; `Statement` + concatenation is not. The same principle applies to OS commands, LDAP, and template injection.
```java
// BROKEN: SQL injection
String sql = "SELECT * FROM users WHERE email = '" + email + "'"; // ' OR '1'='1
// FIXED: parameterized
PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
ps.setString(1, email); // input is data, never code
```

### Input validation & output encoding
Validate at the boundary (type, length, format, allowlist) and reject early ([11 validation](../11-spring-mvc-and-rest-apis/)). For data rendered in a browser, **output-encode** to prevent XSS (the frontend's job, but APIs returning HTML must encode). Validate on the server even if the client validates.

### Authn/authz pitfalls
Enforce authorization on the **server** for every request (never trust the client); check object-level ownership (prevent **IDOR** — accessing `/orders/{id}` you don't own) with `@PreAuthorize`/explicit checks; apply least privilege. See [14](../14-spring-security/) for sessions/JWT.

### Secrets management
Never commit secrets or put them in source/images; load from environment or a secret manager (Vault, AWS Secrets Manager), rotate them, and keep them out of logs/error responses. Reference secrets by key name, not value, in code and logs.

### Transport & data at rest
TLS in transit ([23](../23-networking-and-http-fundamentals/)); encrypt sensitive data at rest; hash passwords with BCrypt/Argon2 ([14](../14-spring-security/)); minimize PII stored and mask it in logs.

### Insecure deserialization & SSRF
Don't deserialize untrusted data into arbitrary types (Java native deserialization gadgets → RCE); prefer safe formats (JSON with strict typing) and allowlists. Guard outbound requests built from user input against **SSRF** (don't let a user make your server fetch internal URLs/metadata endpoints).

### Dependency & supply-chain
Keep dependencies patched (known-CVE scanning), pin versions, and watch for typosquatting ([build chapter](../24-build-tools-and-deployment-fundamentals/)). Vulnerable libraries are a top breach vector.

### Error handling & info leakage
Return generic errors to clients; never leak stack traces, SQL, versions, or internal hostnames ([11](../11-spring-mvc-and-rest-apis/), [05](../05-exceptions-and-resource-management/)). Log details server-side with a correlation id.

### Rate limiting & abuse
Rate-limit auth endpoints to slow brute force; lock/alert on repeated failures; use CAPTCHA/MFA where appropriate.

## D. Trade-offs

| Area | Secure default | Note |
|---|---|---|
| DB access | parameterized queries | never concatenate input |
| Secrets | env/secret manager | never in code/images/logs |
| Authz | server-side, object-level | prevents IDOR |
| Deserialization | JSON + strict types/allowlist | avoid native Java deserialization |
| Errors | generic to client | details server-side only |
| Passwords | BCrypt/Argon2 + salt | never fast hashes |

## E. Interview Q&A

### SEC25-01 (P1) — How do you prevent SQL injection?
**Strong answer:** Always use parameterized queries / prepared statements (and ORM bound parameters) so user input is treated as data, never as part of the SQL text; never concatenate input into a query string. I also validate/allowlist inputs and apply least-privilege DB accounts so even a flaw has limited blast radius. The same bound-parameter principle applies to OS commands and LDAP.

### SEC25-02 (P1) — How should secrets be managed?
**Strong answer:** Never commit them or bake them into images; load from environment variables or a secret manager (Vault/AWS Secrets Manager/KMS), rotate regularly, scope access with least privilege, and keep them out of logs and error responses — I reference secrets by name, not value. In Spring Boot I externalize config and inject secrets at runtime ([10](../10-spring-boot-internals/)).

### SEC25-03 (P1) — What is IDOR and how do you prevent it?
**Strong answer:** Insecure Direct Object Reference — a user accesses another user's resource by changing an id (`/orders/42`) because the server only checks authentication, not ownership. I prevent it with object-level authorization: verify the authenticated principal owns or may access the specific resource (`@PreAuthorize` with the id, or an explicit check in the service), not just that they're logged in.

### SEC25-04 (P1) — Why is validating only on the client dangerous?
**Strong answer:** Clients are fully controllable by an attacker (curl, intercepting proxies), so client-side validation is a UX nicety, not a control. All validation and authorization must be enforced server-side at the boundary; I treat every incoming value as untrusted regardless of what the UI does.

### SEC25-05 (P2) — What's insecure deserialization and SSRF?
**Strong answer:** Insecure deserialization is reconstructing untrusted bytes into objects, which with Java native serialization can trigger gadget chains leading to remote code execution — I avoid native deserialization of untrusted input and use JSON with strict typing/allowlists. SSRF is tricking the server into making requests to attacker-chosen URLs (e.g., internal services or cloud metadata endpoints); I validate/allowlist outbound targets and block internal ranges when a URL comes from user input.

### SEC25-06 (P1) — How do you avoid leaking information in errors/logs?
**Strong answer:** Return generic, consistent error responses to clients (no stack traces, SQL, versions, or internal hostnames), map exceptions in a `@ControllerAdvice`, and log full details server-side keyed by a correlation id. I also mask PII and never log secrets or tokens.

## F. Production relevance
These defenses stop the breaches that make headlines: injection (parameterized queries), credential theft (secret managers, hashed passwords), broken access control/IDOR (server-side object-level authz), vulnerable dependencies (patching/scanning), and info leakage (generic errors). They're cheap when built in as defaults and catastrophic when missing. Pair with [14 Spring Security](../14-spring-security/) for authn/authz mechanics and [18 API](../18-api-engineering/) for rate limiting/idempotency.

## G. Revision

### 10 key takeaways
1. Never trust input; validate/allowlist at the boundary, server-side.
2. Parameterized queries always — never concatenate input into SQL.
3. Secrets via env/secret manager; never in code/images/logs; rotate.
4. Enforce authz server-side and at object level (prevent IDOR).
5. Hash passwords with BCrypt/Argon2; TLS in transit; encrypt sensitive data at rest.
6. Avoid native deserialization of untrusted data; guard against SSRF.
7. Patch dependencies; scan for CVEs; beware typosquatting.
8. Generic errors to clients; details + correlation id server-side.
9. Rate-limit auth endpoints; MFA for sensitive actions.
10. Secure defaults beat after-the-fact fixes.

### 10 rapid-fire
1. Stop SQL injection? (prepared statements) 2. Store secrets? (secret manager/env) 3. Access others' records by id? (IDOR) 4. Client-only validation? (unsafe) 5. Password hash? (BCrypt/Argon2) 6. Untrusted bytes→objects? (insecure deserialization) 7. Server fetches internal URL? (SSRF) 8. Leak in errors? (no stack traces) 9. Brute force defense? (rate limit/MFA) 10. Vulnerable libs? (patch/scan)

### 5 tricky
1. Why does ORM not automatically prevent injection? (native/concatenated queries still vulnerable) 2. Why is auth-only (no ownership check) exploitable? (IDOR) 3. Why is a signed JWT not confidential? (base64, see ch.14) 4. Why avoid Java native deserialization? 5. Why mask PII in logs?

### 5 exercises
1. Convert a concatenated query to a prepared statement; demonstrate the injection it stops.
2. Add an object-level ownership check to prevent IDOR.
3. Externalize a secret to env/secret manager and keep it out of logs.
4. Configure a dependency CVE scan in the build.
5. Add rate limiting to a login endpoint.

### One-page revision sheet
> **Don't trust input**: validate/allowlist server-side. **Parameterized queries always** (never concat → injection). Secrets: env/secret manager, rotate, never in code/images/logs (reference by name). Authz **server-side + object-level** (prevent IDOR). Passwords BCrypt/Argon2; TLS in transit; encrypt at rest; mask PII. Avoid native deserialization (RCE gadgets); guard SSRF (allowlist outbound). Patch/scan dependencies. **Generic errors to clients**, details+correlation id server-side. Rate-limit auth; MFA. Secure defaults.

### Prerequisites to revisit
- Spring Security authn/authz (ch.14); SQL/prepared statements (ch.15); API errors/rate limits (ch.11, 18).
