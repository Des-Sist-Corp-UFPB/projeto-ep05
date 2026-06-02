package br.ufpb.dsc.mercado.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

/**
 * Utilitário leve e sem dependências externas para geração e validação de tokens JWT (HMAC-SHA256).
 */
@Component
public class TokenProvider {

    @Value("${app.jwt.secret:segredo_super_secreto_e_longo_para_geracao_do_token_jwt_mercado}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-seconds:86400}") // 24 horas padrão
    private long jwtExpirationSeconds;

    public String gerarToken(String email, String role) {
        try {
            // Header
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String encodedHeader = base64UrlEncode(header);

            // Payload
            long exp = Instant.now().getEpochSecond() + jwtExpirationSeconds;
            String payload = String.format("{\"sub\":\"%s\",\"role\":\"%s\",\"exp\":%d}", email, role, exp);
            String encodedPayload = base64UrlEncode(payload);

            // Signature
            String signatureInput = encodedHeader + "." + encodedPayload;
            String signature = hmacSha256(signatureInput, jwtSecret);

            return signatureInput + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar token JWT", e);
        }
    }

    public boolean validarToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];

            // Validar Assinatura
            String signatureInput = header + "." + payload;
            String expectedSignature = hmacSha256(signatureInput, jwtSecret);
            if (!expectedSignature.equals(signature)) {
                return false;
            }

            // Validar Expiração
            String decodedPayload = base64UrlDecode(payload);
            long exp = extrairExpClaim(decodedPayload);
            long agora = Instant.now().getEpochSecond();
            return agora < exp;
        } catch (java.security.NoSuchAlgorithmException | java.security.InvalidKeyException | RuntimeException e) {
            return false;
        }
    }

    public String extrairEmail(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = base64UrlDecode(parts[1]);
            return extrairSubClaim(payload);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao extrair e-mail do token JWT", e);
        }
    }

    // Métodos Auxiliares
    private String base64UrlEncode(String input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private String base64UrlDecode(String input) {
        return new String(Base64.getUrlDecoder().decode(input), StandardCharsets.UTF_8);
    }

    private String hmacSha256(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private long extrairExpClaim(String jsonPayload) {
        // Encontra o padrão "exp": <numero>
        int index = jsonPayload.indexOf("\"exp\":");
        if (index == -1) {
            throw new RuntimeException("Claim 'exp' não encontrado no token");
        }
        int start = index + 6;
        int end = jsonPayload.indexOf(",", start);
        if (end == -1) {
            end = jsonPayload.indexOf("}", start);
        }
        String expStr = jsonPayload.substring(start, end).trim();
        return Long.parseLong(expStr);
    }

    private String extrairSubClaim(String jsonPayload) {
        // Encontra o padrão "sub": "email"
        int index = jsonPayload.indexOf("\"sub\":\"");
        if (index == -1) {
            throw new RuntimeException("Claim 'sub' não encontrado no token");
        }
        int start = index + 7;
        int end = jsonPayload.indexOf("\"", start);
        return jsonPayload.substring(start, end);
    }
}
