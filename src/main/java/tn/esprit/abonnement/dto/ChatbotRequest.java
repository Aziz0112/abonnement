package tn.esprit.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotRequest {
    private Long userId;
    private String message;
    private Object conversationContext;
}