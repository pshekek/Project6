package rita.security.jwt;


import org.springframework.stereotype.Service;
import rita.dto.JwtAuthenticationDTO;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    @Value("08cae397745cc2e92dc42e5869ee3995711bbb81585ad52c1a5208be4a5b334f4770c6bf")
    private String jwtSecret;

    public JwtAuthenticationDTO generateAuthToken(String login) {
        JwtAuthenticationDTO dto = new JwtAuthenticationDTO();
        dto.setToken(generateJwtToken(login));
        dto.setRefreshToken(generateRefreshToken(login));
        return dto;
    }

    public JwtAuthenticationDTO refreshBaseToken(String login, String refreshToken) {
        JwtAuthenticationDTO dto = new JwtAuthenticationDTO();
        dto.setToken(generateJwtToken(login));
        dto.setRefreshToken(refreshToken);
        return dto;
    }

    public String getLoginFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject(); // login
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid or expired JWT token", e);
        }
    }

    public boolean validateJwtToken (String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("ExpiredJwtException", e);
        } catch (UnsupportedJwtException e) {
            log.error("UnsupportedJwtException", e);
        } catch (MalformedJwtException e) {
            log.error("MalformedJwtException", e);
        } catch (SecurityException e) {
            log.error("SecurityException", e);
        } catch (Exception e) {
            log.error("Фуфлыжный токен", e);
        }
        return false;
    }

    public String generateJwtToken(String login) {
        Date date = Date.from(LocalDateTime.now().plusHours(1)
                .atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder().setSubject(login)
                .setExpiration(date)
                .signWith(getSignKey())
                .compact();
    }


    public String generateRefreshToken(String login) {
        Date date = Date.from(LocalDateTime.now().plusDays(1)
                .atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder().setSubject(login)
                .setExpiration(date)
                .signWith(getSignKey())
                .compact();
    }


    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}

