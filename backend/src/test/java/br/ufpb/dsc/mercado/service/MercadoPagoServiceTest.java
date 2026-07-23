package br.ufpb.dsc.mercado.service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPResponse;
import com.mercadopago.resources.payment.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MercadoPagoService — Testes Unitários")
class MercadoPagoServiceTest {

    @Mock PaymentClient paymentClient;
    @Mock Payment payment;

    private MercadoPagoService service;

    @BeforeEach
    void setUp() {
        service = new MercadoPagoService(paymentClient);
    }

    @Test
    @DisplayName("Deve retornar o pagamento quando status for 'approved'")
    void cobrarComToken_aprovado() throws Exception {
        when(payment.getStatus()).thenReturn("approved");
        when(payment.getId()).thenReturn(123L);
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        Payment resultado = service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1);

        assertThat(resultado).isSameAs(payment);
        verify(paymentClient).create(any(PaymentCreateRequest.class));
    }

    @Test
    @DisplayName("Deve aceitar status 'in_process' como válido")
    void cobrarComToken_emAnalise() throws Exception {
        when(payment.getStatus()).thenReturn("in_process");
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        Payment resultado = service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1);

        assertThat(resultado).isSameAs(payment);
    }

    @Test
    @DisplayName("Deve aceitar status 'authorized' como válido")
    void cobrarComToken_autorizado() throws Exception {
        when(payment.getStatus()).thenReturn("authorized");
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        Payment resultado = service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1);

        assertThat(resultado).isSameAs(payment);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o Mercado Pago não retorna resposta")
    void cobrarComToken_respostaNula() throws Exception {
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não retornou resposta");
    }

    @Test
    @DisplayName("Deve traduzir rejeição por saldo insuficiente")
    void cobrarComToken_recusadoSaldoInsuficiente() throws Exception {
        when(payment.getStatus()).thenReturn("rejected");
        when(payment.getStatusDetail()).thenReturn("cc_rejected_insufficient_amount");
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        assertThatThrownBy(() -> service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("saldo insuficiente no cartão");
    }

    @Test
    @DisplayName("Deve traduzir rejeição por número de cartão inválido")
    void cobrarComToken_recusadoNumeroInvalido() throws Exception {
        when(payment.getStatus()).thenReturn("rejected");
        when(payment.getStatusDetail()).thenReturn("cc_rejected_bad_filled_card_number");
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        assertThatThrownBy(() -> service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .hasMessageContaining("número do cartão inválido");
    }

    @Test
    @DisplayName("Deve traduzir rejeição por data de validade inválida")
    void cobrarComToken_recusadoDataInvalida() throws Exception {
        when(payment.getStatus()).thenReturn("rejected");
        when(payment.getStatusDetail()).thenReturn("cc_rejected_bad_filled_date");
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        assertThatThrownBy(() -> service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .hasMessageContaining("data de validade inválida");
    }

    @Test
    @DisplayName("Deve traduzir rejeição por código de segurança inválido")
    void cobrarComToken_recusadoCvvInvalido() throws Exception {
        when(payment.getStatus()).thenReturn("rejected");
        when(payment.getStatusDetail()).thenReturn("cc_rejected_bad_filled_security_code");
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        assertThatThrownBy(() -> service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .hasMessageContaining("código de segurança inválido");
    }

    @Test
    @DisplayName("Deve traduzir rejeição por cartão bloqueado")
    void cobrarComToken_recusadoBloqueado() throws Exception {
        when(payment.getStatus()).thenReturn("rejected");
        when(payment.getStatusDetail()).thenReturn("cc_rejected_blacklist");
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        assertThatThrownBy(() -> service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .hasMessageContaining("cartão bloqueado");
    }

    @Test
    @DisplayName("Deve traduzir rejeição que exige autorização da operadora")
    void cobrarComToken_recusadoRequerAutorizacao() throws Exception {
        when(payment.getStatus()).thenReturn("rejected");
        when(payment.getStatusDetail()).thenReturn("cc_rejected_call_for_authorize");
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        assertThatThrownBy(() -> service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .hasMessageContaining("requer autorização da operadora");
    }

    @Test
    @DisplayName("Deve traduzir rejeição por pagamento duplicado")
    void cobrarComToken_recusadoDuplicado() throws Exception {
        when(payment.getStatus()).thenReturn("rejected");
        when(payment.getStatusDetail()).thenReturn("cc_rejected_duplicated_payment");
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        assertThatThrownBy(() -> service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .hasMessageContaining("pagamento duplicado detectado");
    }

    @Test
    @DisplayName("Deve traduzir rejeição por alto risco")
    void cobrarComToken_recusadoAltoRisco() throws Exception {
        when(payment.getStatus()).thenReturn("rejected");
        when(payment.getStatusDetail()).thenReturn("cc_rejected_high_risk");
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        assertThatThrownBy(() -> service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .hasMessageContaining("pagamento recusado por segurança");
    }

    @Test
    @DisplayName("Deve usar o status como mensagem quando não há detalhe (status desconhecido)")
    void cobrarComToken_recusadoSemDetalheConhecido() throws Exception {
        when(payment.getStatus()).thenReturn("rejected");
        when(payment.getStatusDetail()).thenReturn(null);
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        assertThatThrownBy(() -> service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rejected");
    }

    @Test
    @DisplayName("Deve traduzir detalhe desconhecido repassando o próprio texto")
    void cobrarComToken_detalheDesconhecido() throws Exception {
        when(payment.getStatus()).thenReturn("rejected");
        when(payment.getStatusDetail()).thenReturn("algum_motivo_nao_mapeado");
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(payment);

        assertThatThrownBy(() -> service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .hasMessageContaining("algum_motivo_nao_mapeado");
    }

    @Test
    @DisplayName("Deve lançar exceção amigável quando a API do Mercado Pago retorna erro (MPApiException)")
    void cobrarComToken_erroApi() throws Exception {
        MPResponse apiResponse = mock(MPResponse.class);
        when(apiResponse.getContent()).thenReturn("{\"message\":\"token inválido\"}");
        MPApiException erro = mock(MPApiException.class);
        when(erro.getApiResponse()).thenReturn(apiResponse);
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenThrow(erro);

        assertThatThrownBy(() -> service.cobrarComToken("token-invalido", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Erro ao processar pagamento")
                .hasMessageContaining("token inválido");
    }

    @Test
    @DisplayName("Deve usar a mensagem da exceção quando MPApiException não tem ApiResponse")
    void cobrarComToken_erroApiSemApiResponse() throws Exception {
        MPApiException erro = mock(MPApiException.class);
        when(erro.getApiResponse()).thenReturn(null);
        when(erro.getMessage()).thenReturn("falha desconhecida");
        when(paymentClient.create(any(PaymentCreateRequest.class))).thenThrow(erro);

        assertThatThrownBy(() -> service.cobrarComToken("token-x", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("falha desconhecida");
    }

    @Test
    @DisplayName("Deve lançar exceção amigável quando há falha de comunicação (MPException)")
    void cobrarComToken_falhaComunicacao() throws Exception {
        when(paymentClient.create(any(PaymentCreateRequest.class)))
                .thenThrow(new MPException("timeout"));

        assertThatThrownBy(() -> service.cobrarComToken("token-abc", BigDecimal.TEN,
                "cliente@email.com", "Pedido #1", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Não foi possível conectar ao Mercado Pago");
    }
}
