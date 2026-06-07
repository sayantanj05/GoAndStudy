package com.goandstudybackend.service;

import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookQueue;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.BookQueueRepository;
import com.goandstudybackend.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookQueueService {

    private final BookQueueRepository bookQueueRepository;
    private final BookRepository bookRepository;
    private final NotificationService notificationService;

    public Map<String, Object> getMemberQueue(String memberId) {
        List<BookQueue> queues = bookQueueRepository.findByMemberId(memberId);
        
        List<Map<String, Object>> queueList = queues.stream().map(this::mapQueueToResponse).collect(Collectors.toList());
        
        return Map.of("queues", queueList);
    }

    public Map<String, Object> createQueue(String memberId, String queueName, List<String> bookIds) {
        List<BookQueue.QueuedBook> queuedBooks = new ArrayList<>();
        int position = 1;
        
        for (String bookId : bookIds) {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book != null) {
                queuedBooks.add(BookQueue.QueuedBook.builder()
                        .bookId(bookId)
                        .title(book.getTitle())
                        .author(getAuthorName(book))
                        .position(position++)
                        .status("PENDING")
                        .build());
            }
        }

        BookQueue queue = BookQueue.builder()
                .memberId(memberId)
                .queueName(queueName != null ? queueName : "My Reading Queue")
                .books(queuedBooks)
                .currentPosition(0)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BookQueue saved = bookQueueRepository.save(queue);
        return mapQueueToResponse(saved);
    }

    public Map<String, Object> updateQueue(String memberId, String queueId, List<String> bookIds) {
        BookQueue queue = bookQueueRepository.findByMemberIdAndId(memberId, queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue not found"));

        List<BookQueue.QueuedBook> queuedBooks = new ArrayList<>();
        int position = 1;
        
        for (String bookId : bookIds) {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book != null) {
                queuedBooks.add(BookQueue.QueuedBook.builder()
                        .bookId(bookId)
                        .title(book.getTitle())
                        .author(getAuthorName(book))
                        .position(position++)
                        .status("PENDING")
                        .build());
            }
        }

        queue.setBooks(queuedBooks);
        queue.setCurrentPosition(0);
        queue.setUpdatedAt(LocalDateTime.now());

        BookQueue saved = bookQueueRepository.save(queue);
        return mapQueueToResponse(saved);
    }

    public void deleteQueue(String memberId, String queueId) {
        bookQueueRepository.deleteByMemberIdAndId(memberId, queueId);
    }

    public Map<String, Object> markBookCompleted(String memberId, String queueId, String bookId) {
        BookQueue queue = bookQueueRepository.findByMemberIdAndId(memberId, queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue not found"));

        boolean foundBook = false;
        int completedPosition = -1;
        
        for (BookQueue.QueuedBook qb : queue.getBooks()) {
            if (qb.getBookId().equals(bookId)) {
                qb.setStatus("COMPLETED");
                qb.setCompletedAt(LocalDateTime.now());
                foundBook = true;
                completedPosition = qb.getPosition();
                break;
            }
        }

        if (!foundBook) {
            throw new ResourceNotFoundException("Book not found in queue");
        }

        // Find next book and send notification
        final int finalCompletedPosition = completedPosition;
        BookQueue.QueuedBook nextBook = queue.getBooks().stream()
                .filter(b -> b.getPosition() > finalCompletedPosition && "PENDING".equals(b.getStatus()))
                .findFirst()
                .orElse(null);

        if (nextBook != null) {
            notificationService.createNotification(
                    memberId,
                    "NEXT_UP",
                    "Next Up: " + nextBook.getTitle(),
                    "You've completed your current book! The next book in your queue is ready: '" + nextBook.getTitle() + "' by " + nextBook.getAuthor(),
                    null,
                    nextBook.getBookId(),
                    "IN_APP",
                    "Sent",
                    null,
                    true
            );
            nextBook.setStatus("READING");
            nextBook.setStartedAt(LocalDateTime.now());
            queue.setCurrentPosition(nextBook.getPosition() - 1);
        } else {
            // Queue completed
            notificationService.createNotification(
                    memberId,
                    "QUEUE_COMPLETED",
                    "Queue Completed!",
                    "Congratulations! You've completed all books in your queue: " + queue.getQueueName(),
                    null,
                    null,
                    "IN_APP",
                    "Sent",
                    null,
                    true
            );
            queue.setActive(false);
        }

        queue.setUpdatedAt(LocalDateTime.now());
        BookQueue saved = bookQueueRepository.save(queue);
        return mapQueueToResponse(saved);
    }

    public void startFirstBook(String memberId, String queueId) {
        BookQueue queue = bookQueueRepository.findByMemberIdAndId(memberId, queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue not found"));

        if (queue.getBooks() != null && !queue.getBooks().isEmpty()) {
            BookQueue.QueuedBook firstBook = queue.getBooks().get(0);
            firstBook.setStatus("READING");
            firstBook.setStartedAt(LocalDateTime.now());
            queue.setCurrentPosition(0);
            queue.setUpdatedAt(LocalDateTime.now());
            bookQueueRepository.save(queue);
        }
    }

    private Map<String, Object> mapQueueToResponse(BookQueue queue) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", queue.getId());
        response.put("queueName", queue.getQueueName());
        response.put("books", queue.getBooks());
        response.put("currentPosition", queue.getCurrentPosition());
        response.put("isActive", queue.isActive());
        response.put("createdAt", queue.getCreatedAt());
        response.put("updatedAt", queue.getUpdatedAt());
        return response;
    }

    public void checkAndNotifyNextBook(String memberId, String completedBookId) {
        List<BookQueue> queues = bookQueueRepository.findByMemberId(memberId);
        
        for (BookQueue queue : queues) {
            if (!queue.isActive() || queue.getBooks() == null) continue;
            
            // Find the completed book in this queue
            BookQueue.QueuedBook completedBook = queue.getBooks().stream()
                    .filter(b -> b.getBookId().equals(completedBookId) && "READING".equals(b.getStatus()))
                    .findFirst()
                    .orElse(null);
            
            if (completedBook != null) {
                // Mark as completed
                completedBook.setStatus("COMPLETED");
                completedBook.setCompletedAt(LocalDateTime.now());
                
                // Find next book
                int completedPosition = completedBook.getPosition();
                BookQueue.QueuedBook nextBook = queue.getBooks().stream()
                        .filter(b -> b.getPosition() > completedPosition && "PENDING".equals(b.getStatus()))
                        .findFirst()
                        .orElse(null);
                
                if (nextBook != null) {
                    notificationService.createNotification(
                            memberId,
                            "NEXT_UP",
                            "Next Up: " + nextBook.getTitle(),
                            "You've completed '" + completedBook.getTitle() + "'! The next book in your queue is ready: '" + nextBook.getTitle() + "' by " + nextBook.getAuthor(),
                            null,
                            nextBook.getBookId(),
                            "IN_APP",
                            "Sent",
                            null,
                            true
                    );
                    nextBook.setStatus("READING");
                    nextBook.setStartedAt(LocalDateTime.now());
                    queue.setCurrentPosition(nextBook.getPosition() - 1);
                } else {
                    // Queue completed
                    notificationService.createNotification(
                            memberId,
                            "QUEUE_COMPLETED",
                            "Queue Completed!",
                            "Congratulations! You've completed all books in your queue: " + queue.getQueueName(),
                            null,
                            null,
                            "IN_APP",
                            "Sent",
                            null,
                            true
                    );
                    queue.setActive(false);
                }
                
                queue.setUpdatedAt(LocalDateTime.now());
                bookQueueRepository.save(queue);
                break; // Only process first matching queue
            }
        }
    }

    private String getAuthorName(Book book) {
        if (book.getAuthorIds() == null || book.getAuthorIds().isEmpty()) {
            return "Unknown Author";
        }
        return String.join(", ", book.getAuthorIds());
    }
}
