package br.ufpb.dsc.mercado.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider pedidosToolCallbackProvider(PedidosTools pedidosTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(pedidosTools)
                .build();
    }
}