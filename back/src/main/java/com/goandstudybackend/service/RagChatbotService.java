package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.ChatbotFeedbackRequest;
import com.goandstudybackend.dto.request.ChatbotMessageRequest;
import com.goandstudybackend.dto.response.ChatbotMessageResponse;
import com.goandstudybackend.entity.BookEmbedding;
import com.goandstudybackend.entity.ChatbotFeedback;
import com.goandstudybackend.entity.ChatbotSession;
import com.goandstudybackend.repository.BookEmbeddingRepository;
import com.goandstudybackend.repository.ChatbotFeedbackRepository;
import com.goandstudybackend.repository.ChatbotSessionRepository;
import com.goandstudybackend.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagChatbotService {

    private final NvidiaEmbeddingService nvidiaEmbeddingService;
    private final NvidiaLlmService nvidiaLlmService;
    private final BookEmbeddingRepository bookEmbeddingRepository;
    private final ChatbotSessionRepository chatbotSessionRepository;
    private final ChatbotFeedbackRepository chatbotFeedbackRepository;
    private final MongoTemplate mongoTemplate;

    public ChatbotMessageResponse processMessage(String memberId, ChatbotMessageRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = createNewSession(memberId);
        }

        // Get recent context
        List<Map<String, Object>> context = getSessionContext(memberId, sessionId, 4);

        List<BookEmbedding> relevantBooks = new ArrayList<>();
        NvidiaLlmService.TextGenerationResult llmResult;
        String response;
        try {
            relevantBooks = retrieveRelevantBooks(request.getMessage(), 5);
            String contextStr = buildContext(context, relevantBooks);
            llmResult = nvidiaLlmService.generateDetailed(contextStr + "\n\nUser: " + request.getMessage() + "\nAssistant:", 500);
            response = llmResult.getText();
            if (response == null || response.isBlank()) {
                response = fallbackReply(request.getMessage(), relevantBooks);
            }
        } catch (Exception exception) {
            System.err.println("RAG chatbot fallback for member " + memberId + ": " + exception.getMessage());
            response = fallbackReply(request.getMessage(), relevantBooks);
            llmResult = NvidiaLlmService.TextGenerationResult.builder()
                    .text(response)
                    .tokensUsed(0)
                    .latencyMs(0)
                    .build();
        }
        
        // Save message
        saveMessage(memberId, sessionId, request.getMessage(), response, relevantBooks);
        
        System.out.println("Chatbot response generated for member " + memberId + " in session " + sessionId);
        return ChatbotMessageResponse.builder()
                .sessionId(sessionId)
                .userMessage(request.getMessage())
                .assistantReply(response)
                .tokensUsed(llmResult.getTokensUsed())
                .latencyMs(llmResult.getLatencyMs())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String createNewSession(String memberId) {
        ChatbotSession session = ChatbotSession.builder()
                .memberId(memberId)
                .messages(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .totalMessages(0)
                .build();
        ChatbotSession saved = chatbotSessionRepository.save(session);
        System.out.println("New chatbot session created: " + saved.getId() + " for member " + memberId);
        return saved.getId();
    }

    private List<Map<String, Object>> getSessionContext(String memberId, String sessionId, int limit) {
        Optional<ChatbotSession> optSession = chatbotSessionRepository.findById(sessionId);
        if (optSession.isEmpty() || !optSession.get().getMemberId().equals(memberId)) {
            return new ArrayList<>();
        }
        ChatbotSession session = optSession.get();
        return session.getMessages().stream()
                .sorted(Comparator.comparing(ChatbotSession.ChatbotMessage::getTimestamp))
                .limit(limit * 2)
                .map(msg -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("role", msg.getRole());
                    map.put("content", msg.getContent());
                    map.put("isUser", "user".equals(msg.getRole()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<BookEmbedding> retrieveRelevantBooks(String query, int topK) {
        // Embed query
        List<Double> queryEmbedding = nvidiaEmbeddingService.generateEmbedding(query);
        
        // Simple cosine similarity search (until vector index is created)
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.sample(topK * 3) // Sample candidates
        );
        
        List<BookEmbedding> candidates = mongoTemplate.aggregate(aggregation, BookEmbedding.class, BookEmbedding.class).getMappedResults();
        if (candidates == null || candidates.isEmpty()) {
            candidates = bookEmbeddingRepository.findAll().stream().limit(topK * 3L).toList();
        }
        return candidates.stream()
                .map(b -> {
                    b.setScore(cosineSimilarity(b.getEmbedding(), queryEmbedding)); // Add score
                    return b;
                })
                .filter(b -> b.getScore() > 0.7)
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }
    
    private double cosineSimilarity(List<Double> vec1, List<Double> vec2) {
        if (vec1 == null || vec2 == null || vec1.isEmpty() || vec2.isEmpty() || vec1.size() != vec2.size()) {
            return 0.0;
        }
        double dot = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (int i = 0; i < vec1.size(); i++) {
            dot += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }
        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator == 0.0 ? 0.0 : dot / denominator;
    }

    private String buildContext(List<Map<String, Object>> sessionContext, List<BookEmbedding> books) {
        StringBuilder context = new StringBuilder("You are a helpful library assistant for GoAndStudy Smart Library.\n\nRecent conversation:\n");
        for (Map<String, Object> msg : sessionContext) {
            String role = (Boolean) msg.get("isUser") ? "User" : "Assistant";
            context.append(role).append(": ").append(msg.get("content")).append("\n");
        }
        
        context.append("\nRelevant books from library:\n");
        for (BookEmbedding book : books) {
            context.append("- ").append(book.getTitle() != null ? book.getTitle() : book.getIsbn())
                   .append(" (").append(String.format("%.2f", book.getScore())).append(")\n");
        }
        context.append("\nAnswer questions about these books, library services, or general reading advice.");
        return context.toString();
    }

    private String fallbackReply(String message, List<BookEmbedding> books) {
        String lower = message == null ? "" : message.toLowerCase();
        if (lower.contains("recommend") || lower.contains("book")) {
            if (books != null && !books.isEmpty()) {
                String titles = books.stream()
                        .map(book -> book.getTitle() != null && !book.getTitle().isBlank() ? book.getTitle() : book.getIsbn())
                        .filter(Objects::nonNull)
                        .limit(3)
                        .collect(Collectors.joining(", "));
                if (!titles.isBlank()) {
                    return "Here are a few library titles to explore: " + titles + ". You can open Browse Books for availability and reservation options.";
                }
            }
            return "I can help you find books, check borrowing rules, and plan what to read next. Try searching the catalog by title, author, genre, or ISBN.";
        }
        if (lower.contains("loan") || lower.contains("due") || lower.contains("fine")) {
            return "For loans, due dates, renewals, and fines, open My Loans or ask the staff desk for help with a specific book.";
        }
        return "I am ready to help with books, recommendations, loans, reservations, and library questions. What would you like to explore?";
    }

    private void saveMessage(String memberId, String sessionId, String userMessage, String assistantMessage, List<BookEmbedding> sources) {
        ChatbotSession session = chatbotSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        ChatbotSession.ChatbotMessage userMsg = ChatbotSession.ChatbotMessage.builder()
            .role("user")
            .content(userMessage)
            .timestamp(LocalDateTime.now())
            .build();
            
        ChatbotSession.ChatbotMessage assistantMsg = ChatbotSession.ChatbotMessage.builder()
            .role("assistant")
            .content(assistantMessage)
            .retrievedDocs(sources.stream().map(b -> b.getBookId()).collect(Collectors.toList()))
            .timestamp(LocalDateTime.now())
            .build();
        
        session.getMessages().add(userMsg);
        session.getMessages().add(assistantMsg);
        session.setUpdatedAt(LocalDateTime.now());
        chatbotSessionRepository.save(session);
    }

    public Map<String, Object> getMySessions(String memberId) {
        List<ChatbotSession> sessions = chatbotSessionRepository.findByMemberIdAndIsActiveTrueOrderByUpdatedAtDesc(memberId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessions", sessions.stream().map(this::mapSessionSummary).collect(Collectors.toList()));
        response.put("total", sessions.size());
        return response;
    }

    public Map<String, Object> getSessionHistory(String memberId, String sessionId) {
        ChatbotSession session = chatbotSessionRepository.findByIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("session", mapSessionDetail(session));
        return response;
    }

    public Map<String, Object> submitFeedback(String memberId, ChatbotFeedbackRequest request) {
        ChatbotFeedback feedback = ChatbotFeedback.builder()
                .sessionId(request.getSessionId())
                .memberId(memberId)
                .messageIndex(Integer.parseInt(request.getMessageId()))
                .rating(request.getRating())
                .feedbackText(request.getFeedback())
                .createdAt(LocalDateTime.now())
                .build();
        chatbotFeedbackRepository.save(feedback);
        return Map.of("success", true, "id", feedback.getId());
    }

    public Map<String, Object> endSession(String memberId, String sessionId) {
        ChatbotSession session = chatbotSessionRepository.findByIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setActive(false);
        session.setEndedAt(LocalDateTime.now());
        chatbotSessionRepository.save(session);
        return Map.of("success", true);
    }

    private Map<String, Object> mapSessionSummary(ChatbotSession session) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", session.getId());
        summary.put("title", deriveSessionTitle(session.getMessages()));
        summary.put("messageCount", session.getMessages().size());
        summary.put("lastUpdated", session.getUpdatedAt());
        summary.put("isActive", session.isActive());
        return summary;
    }

    private Map<String, Object> mapSessionDetail(ChatbotSession session) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", session.getId());
        detail.put("memberId", session.getMemberId());
        detail.put("messages", session.getMessages());
        detail.put("createdAt", session.getCreatedAt());
        detail.put("updatedAt", session.getUpdatedAt());
        return detail;
    }

    private String deriveSessionTitle(List<ChatbotSession.ChatbotMessage> messages) {
        if (messages.isEmpty()) return "New Chat";
        String firstMsg = messages.get(0).getContent();
        return firstMsg.length() > 50 ? firstMsg.substring(0, 50) + "..." : firstMsg;
    }
}

