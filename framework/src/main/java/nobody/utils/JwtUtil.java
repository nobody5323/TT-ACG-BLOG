package nobody.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT utility based on jjwt 0.12.x.
 */
public final class JwtUtil {

    public static final long JWT_TTL = Duration.ofHours(24).toMillis();
    public static final String JWT_KEY = "sangeng";

    private JwtUtil() {
    }

    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String createJWT(String subject) {
        return createJWT(getUUID(), subject, JWT_TTL);
    }

    public static String createJWT(String subject, Long ttlMillis) {
        return createJWT(getUUID(), subject, ttlMillis);
    }

    public static String createJWT(String id, String subject, Long ttlMillis) {
        long effectiveTtl = ttlMillis == null ? JWT_TTL : ttlMillis;
        Instant now = Instant.now();
        Instant exp = now.plusMillis(effectiveTtl);

        return Jwts.builder()
                .id(id)
                .subject(subject)
                .issuer("sg")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(generalKey())
                .compact();
    }

    public static Claims parseJWT(String jwt) {
        return Jwts.parser()
                .verifyWith(generalKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    public static SecretKey generalKey() {
        byte[] material = JWT_KEY.getBytes(StandardCharsets.UTF_8);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(material);
            return Keys.hmacShaKeyFor(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM does not support SHA-256", e);
        }
    }
}
