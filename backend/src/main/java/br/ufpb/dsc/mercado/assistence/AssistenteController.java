package br.ufpb.dsc.mercado.assistence;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistente")
public class AssistenteController {

    private final ChatClient chatClient;

    public AssistenteController(ChatClient assistenteChatClient) {
        this.chatClient = assistenteChatClient;
    }

    public record ChatRequest(String mensagem) {}
    public record ChatResponse(String resposta) {}

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request, Authentication authentication) {
        String clienteEmail = authentication.getName(); // vem do JWT, não do corpo da requisição

        String resposta = chatClient.prompt()
                .system(s -> s.param("clienteEmail", clienteEmail))
                .user(request.mensagem())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, clienteEmail))
                .call()
                .content();

        return new ChatResponse(resposta);
    }
}