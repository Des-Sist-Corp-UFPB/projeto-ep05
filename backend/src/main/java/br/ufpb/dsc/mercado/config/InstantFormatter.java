package br.ufpb.dsc.mercado.config;

import org.springframework.format.Formatter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Converte entre Instant e o formato usado por campos <input type="datetime-local">
 * (ex: "2026-08-19T19:56"), tanto na leitura do formulário (parse) quanto na
 * exibição do valor já salvo ao reabrir o formulário de edição (print).
 */
public class InstantFormatter implements Formatter<Instant> {

    // Formato aceito na leitura: com ou sem segundos
    private static final DateTimeFormatter PARSE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]");

    // Formato usado para preencher o input datetime-local (sem segundos)
    private static final DateTimeFormatter PRINT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    public Instant parse(String text, Locale locale) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();

        // Aceita também um Instant ISO completo, caso venha de outro lugar (ex: API/JSON)
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // não é ISO completo, tenta o formato do datetime-local
        }

        LocalDateTime localDateTime = LocalDateTime.parse(value, PARSE_FORMAT);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    @Override
    public String print(Instant instant, Locale locale) {
        if (instant == null) {
            return "";
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(PRINT_FORMAT);
    }
}
