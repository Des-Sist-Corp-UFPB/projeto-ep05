package br.ufpb.dsc.mercado.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Mercado Pago.
 *
 * Inicializa o SDK com a access token definida na variável de ambiente
 * MERCADOPAGO_ACCESS_TOKEN (ou application.yml).
 */
@Configuration
public class MercadoPagoConfiguration {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }
}
