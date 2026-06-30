package br.ufpb.dsc.mercado.service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Serviço de integração com o Mercado Pago.
 *
 * A tokenização do cartão é feita pelo SDK JS do Mercado Pago no FRONTEND.
 * O backend recebe apenas o token gerado e o usa para cobrar via Payment API.
 *
 * Fluxo:
 *   1. Frontend chama mp.createCardToken(cardData) → recebe token
 *   2. Frontend envia token + dados mascarados para POST /api/clientes/cartoes
 *   3. Backend salva o cartão com o token
 *   4. Na criação do pedido, backend chama cobrarComToken() com o token salvo
 */
@Service
public class MercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoService.class);

    /**
     * Processa o pagamento usando o token de cartão gerado pelo frontend.
     *
     * @param token         token gerado pelo SDK JS do Mercado Pago
     * @param valor         valor total a cobrar
     * @param emailPagador  e-mail do cliente (obrigatório pelo Mercado Pago)
     * @param descricao     descrição do pedido
     * @param parcelas      número de parcelas (1 = à vista)
     * @return Payment com o resultado da cobrança
     * @throws IllegalArgumentException se o Mercado Pago recusar o pagamento
     */
    public Payment cobrarComToken(String token, BigDecimal valor,
                                  String emailPagador, String descricao,
                                  int parcelas) {
        try {
            PaymentCreateRequest paymentRequest = PaymentCreateRequest.builder()
                    .transactionAmount(valor)
                    .token(token)
                    .description(descricao)
                    .installments(parcelas)
                    .paymentMethodId("visa") // será detectado automaticamente pelo token
                    .payer(
                        PaymentPayerRequest.builder()
                            .email(emailPagador)
                            .build()
                    )
                    .build();

            PaymentClient client = new PaymentClient();
            Payment payment = client.create(paymentRequest);

            if (payment == null) {
                throw new IllegalArgumentException("Mercado Pago não retornou resposta ao processar o pagamento.");
            }

            String status = payment.getStatus();
            log.info("Pagamento processado. ID: {}, Status: {}, StatusDetail: {}",
                    payment.getId(), status, payment.getStatusDetail());

            // approved = aprovado, in_process = em análise (ambos aceitáveis)
            if ("approved".equals(status) || "in_process".equals(status) || "authorized".equals(status)) {
                return payment;
            }

            // rejected ou qualquer outro status
            String detalhe = payment.getStatusDetail() != null ? payment.getStatusDetail() : status;
            throw new IllegalArgumentException("Pagamento recusado pelo Mercado Pago: " + traduzirRejeicao(detalhe));

        } catch (MPApiException e) {
            String mensagem = e.getApiResponse() != null ? e.getApiResponse().getContent() : e.getMessage();
            log.warn("Erro da API do Mercado Pago: {}", mensagem);
            throw new IllegalArgumentException("Erro ao processar pagamento: " + mensagem);
        } catch (MPException e) {
            log.error("Falha na comunicação com o Mercado Pago", e);
            throw new IllegalArgumentException("Não foi possível conectar ao Mercado Pago. Tente novamente.");
        }
    }

    /** Traduz os códigos de rejeição mais comuns para português. */
    private String traduzirRejeicao(String detalhe) {
        return switch (detalhe) {
            case "cc_rejected_insufficient_amount"    -> "saldo insuficiente no cartão";
            case "cc_rejected_bad_filled_card_number" -> "número do cartão inválido";
            case "cc_rejected_bad_filled_date"        -> "data de validade inválida";
            case "cc_rejected_bad_filled_security_code" -> "código de segurança inválido";
            case "cc_rejected_blacklist"              -> "cartão bloqueado";
            case "cc_rejected_call_for_authorize"     -> "cartão requer autorização da operadora";
            case "cc_rejected_duplicated_payment"     -> "pagamento duplicado detectado";
            case "cc_rejected_high_risk"              -> "pagamento recusado por segurança";
            default -> detalhe;
        };
    }
}
