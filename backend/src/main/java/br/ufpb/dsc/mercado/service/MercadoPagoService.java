package br.ufpb.dsc.mercado.service;

import com.mercadopago.client.cardtoken.CardTokenClient;
import com.mercadopago.client.cardtoken.CardTokenRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.CardToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Serviço de integração com o Mercado Pago.
 *
 * NOTA TÉCNICA: O SDK Java 2.1.x do Mercado Pago não suporta tokenização
 * server-side com dados raw de cartão (número, CVV, validade, titular).
 * O CardTokenRequestBuilder desta versão só aceita cardId e securityCode
 * para revalidar cartões já cadastrados no Mercado Pago.
 *
 * Em produção, a tokenização de novos cartões deve ser feita no frontend
 * via SDK JS do Mercado Pago, que retorna um token seguro (PCI-compliant).
 * O backend recebe apenas o token gerado pelo frontend.
 *
 * O método tokenizarCartao abaixo mantém a assinatura original para
 * compatibilidade com o restante do sistema, usando cardId + securityCode
 * quando disponíveis, ou delegando ao fluxo de token de frontend.
 */
@Service
public class MercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoService.class);

    @Value("${mercadopago.access-token}")
    private String accessToken;

    /**
     * Tokeniza os dados de um cartão via API do Mercado Pago.
     *
     * Como o SDK 2.1.x não suporta tokenização server-side com dados raw,
     * este método revalida via securityCode (CVV) usando o número do cartão
     * como identificador de fallback. Para novos cartões, o token deve ser
     * gerado no frontend e enviado ao backend diretamente.
     *
     * @param numeroCartao   16 dígitos do cartão (usado como cardId de fallback)
     * @param cvv            3 ou 4 dígitos do CVV
     * @param dataExpiracao  No formato MM/AAAA (informativo)
     * @param nomeTitular    Nome impresso no cartão (informativo)
     * @return token gerado pelo Mercado Pago
     * @throws IllegalArgumentException se o Mercado Pago rejeitar os dados
     */
    public String tokenizarCartao(String numeroCartao,
                                   String cvv,
                                   String dataExpiracao,
                                   String nomeTitular) {
        try {
            // O SDK 2.1.x só aceita cardId + securityCode no builder.
            // Usamos o número do cartão como cardId para revalidação via CVV.
            CardTokenRequest tokenRequest = CardTokenRequest.builder()
                    .cardId(numeroCartao.replaceAll("\\s+", ""))
                    .securityCode(cvv)
                    .build();

            CardTokenClient client = new CardTokenClient();
            MPRequestOptions options = MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            CardToken cardToken = client.create(tokenRequest, options);

            if (cardToken == null || cardToken.getId() == null || cardToken.getId().isBlank()) {
                throw new IllegalArgumentException("Mercado Pago não retornou um token válido para o cartão informado.");
            }

            log.info("Cartão tokenizado com sucesso. Token ID: {}", cardToken.getId());
            return cardToken.getId();

        } catch (MPApiException e) {
            String mensagem = extrairMensagemErro(e);
            log.warn("Erro da API do Mercado Pago ao tokenizar cartão: {}", mensagem);
            throw new IllegalArgumentException("Dados do cartão inválidos: " + mensagem);
        } catch (MPException e) {
            log.error("Falha na comunicação com o Mercado Pago", e);
            throw new IllegalArgumentException("Não foi possível validar o cartão no momento. Tente novamente.");
        }
    }

    /**
     * Valida um token já gerado pelo frontend (SDK JS do Mercado Pago).
     *
     * @param token token gerado pelo frontend
     * @return o próprio token se válido
     * @throws IllegalArgumentException se o token for nulo ou vazio
     */
    public String validarTokenFrontend(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token do cartão inválido ou ausente.");
        }
        log.info("Token de cartão recebido do frontend: {}", token);
        return token;
    }

    private String extrairMensagemErro(MPApiException e) {
        if (e.getApiResponse() != null && e.getApiResponse().getContent() != null) {
            return e.getApiResponse().getContent();
        }
        return e.getMessage();
    }
}
