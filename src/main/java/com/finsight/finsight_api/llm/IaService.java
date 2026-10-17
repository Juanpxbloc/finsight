package com.finsight.finsight_api.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class IaService {
    private final ChatClient chatClient;

    public IaService(ChatClient.Builder builder ) {
        this.chatClient = builder.build();
    }

    public String preguntar(String pregunta) {

        return chatClient
                .prompt()
                .system("Actúa como un experto en finanzas y contabilidad, y responde a la pregunta de manera clara y concisa. Responde en español y NO respondas nada que no esté relacionado con finanzas y contabilidad.")
                .user(pregunta)
                .call()
                .content();
    }

}
