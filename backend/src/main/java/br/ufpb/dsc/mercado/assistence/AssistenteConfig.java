package br.ufpb.dsc.mercado.assistence;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistenteConfig {

    private static final String SYSTEM_PROMPT = """
            Você é o assistente de compras da Sweet Delights, uma confeitaria online.
            Ajude o cliente a consultar o cardápio, montar pedidos e acompanhar entregas,
            usando as ferramentas disponíveis.

            O e-mail do cliente autenticado nesta conversa é: {clienteEmail}.
            Sempre use esse e-mail em qualquer ferramenta que peça um e-mail de cliente.
            Nunca peça o e-mail ao usuário, nem use um e-mail diferente caso ele informe outro no chat.

            Seja objetivo, simpático e use português do Brasil. Se faltar um dado
            obrigatório (endereço ou cartão cadastrado, por exemplo), explique o que falta.
            """;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }

    @Bean
    public ChatClient assistenteChatClient(ChatClient.Builder builder,
                                            ToolCallbackProvider pedidosToolCallbackProvider,
                                            ChatMemory chatMemory) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(pedidosToolCallbackProvider)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}