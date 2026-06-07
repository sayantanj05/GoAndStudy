package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.ReserveBookRequest;
import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.Reservation;
import com.goandstudybackend.exception.DuplicateResourceException;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final ActivityLogService activityLogService;
    private final SystemConfigService systemConfigService;

    public Map<String, Object> reserveBook(String memberId, ReserveBookRequest request) {
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        log.info("Reservation attempt - bookId: {}, totalCopies: {}, availableCopies: {}, isDeleted: {}", 
            request.getBookId(), book.getTotalCopies(), book.getAvailableCopies(), book.isDeleted());
        if (book.isDeleted()) {
            throw new IllegalArgumentException("Cannot reserve deleted book");
        }
        if (book.getTotalCopies() <= 0) {
            throw new IllegalArgumentException("Book has no copies in library");
        }
        if (book.getAvailableCopies() > 0) {
            throw new IllegalArgumentException("Book is available. Visit counter to borrow.");
        }
        if (!systemConfigService.isFeatureEnabled("reservations")) {
            throw new IllegalArgumentException("Reservations are currently disabled");
        }
        if (reservationRepository.existsByMemberIdAndBookIdAndStatus(memberId, request.getBookId(), "Pending")) {
            throw new DuplicateResourceException("Already in queue");
        }

        int queuePosition = (int) reservationRepository.countByBookIdAndStatus(request.getBookId(), "Pending") + 1;
        Reservation reservation = reservationRepository.save(Reservation.builder()
                .memberId(memberId)
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .status("Pending")
                .queuePosition(queuePosition)
                .reservedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build());
        activityLogService.log("RESERVATION_MADE", memberId, "ROLE_MEMBER", "reservations", reservation.getId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("reservationId", reservation.getId());
        response.put("bookId", book.getId());
        response.put("bookTitle", book.getTitle());
        response.put("queuePosition", queuePosition);
        response.put("status", reservation.getStatus());
        return response;
    }

    public Map<String, Object> getMyReservations(String memberId) {
        List<Reservation> reservations = reservationRepository.findByMemberIdAndStatusIn(memberId, List.of("Pending", "Notified"));
        List<Map<String, Object>> items = new ArrayList<>();
        for (Reservation reservation : reservations) {
            Book book = bookRepository.findById(reservation.getBookId()).orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("reservationId", reservation.getId());
            item.put("bookTitle", reservation.getBookTitle());
            item.put("bookCoverImageUrl", book == null ? "" : book.getCoverImageUrl());
            item.put("queuePosition", reservation.getQueuePosition());
            item.put("status", reservation.getStatus());
            item.put("reservedAt", reservation.getReservedAt());
            item.put("expiresAt", reservation.getExpiresAt());
            items.add(item);
        }
        return Map.of("reservations", items);
    }

    public Map<String, Object> cancelReservation(String memberId, String reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        if (!reservation.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Reservation does not belong to this member");
        }
        if (!"Pending".equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException("Reservation cannot be cancelled");
        }

        int removedPosition = reservation.getQueuePosition();
        String bookId = reservation.getBookId();
        reservation.setStatus("Cancelled");
        reservationRepository.save(reservation);

        List<Reservation> queue = reservationRepository.findByBookIdAndStatusOrderByQueuePositionAsc(bookId, "Pending");
        for (Reservation pending : queue) {
            if (pending.getQueuePosition() > removedPosition) {
                pending.setQueuePosition(pending.getQueuePosition() - 1);
                reservationRepository.save(pending);
            }
        }
        activityLogService.log("RESERVATION_CANCELLED", memberId, "ROLE_MEMBER", "reservations", reservationId);
        return Map.of("message", "Reservation cancelled", "reservationId", reservationId);
    }
}
