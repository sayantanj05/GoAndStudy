package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.AddWishlistRequest;
import com.goandstudybackend.entity.Author;
import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookCategory;
import com.goandstudybackend.entity.Wishlist;
import com.goandstudybackend.exception.DuplicateResourceException;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.AuthorRepository;
import com.goandstudybackend.repository.BookCategoryRepository;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final ActivityLogService activityLogService;
    private final MemberPreferencesService memberPreferencesService;

    public Map<String, Object> getMyWishlist(String memberId) {
        List<Wishlist> wishlists = wishlistRepository.findByMemberIdOrderByAddedAtDesc(memberId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Wishlist wishlist : wishlists) {
            Book book = bookRepository.findById(wishlist.getBookId()).orElse(null);
            String authorName = "";
            if (book != null && !book.getAuthorIds().isEmpty()) {
                Author author = authorRepository.findById(book.getAuthorIds().get(0)).orElse(null);
                authorName = author != null ? author.getName() : "";
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bookId", wishlist.getBookId());
            item.put("bookTitle", wishlist.getBookTitle());
            item.put("bookCoverImageUrl", book == null ? "" : book.getCoverImageUrl());
            item.put("author", authorName);
            item.put("genre", wishlist.getBookGenre());
            item.put("availableCopies", book == null ? 0 : book.getAvailableCopies());
            item.put("addedAt", wishlist.getAddedAt());
            item.put("notifyOnAvailable", wishlist.isNotifyOnAvailable());
            items.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("wishlist", items);
        return response;
    }

    public Map<String, Object> addToWishlist(String memberId, AddWishlistRequest request) {
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        if (wishlistRepository.existsByMemberIdAndBookId(memberId, request.getBookId())) {
            throw new DuplicateResourceException("Book already in wishlist");
        }

        Wishlist wishlist = wishlistRepository.save(Wishlist.builder()
                .memberId(memberId)
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .bookGenre(getCategoryName(book.getCategoryIds()))
                .addedAt(LocalDateTime.now())
                .addedFrom(request.getAddedFrom() == null || request.getAddedFrom().isBlank() ? "browse" : request.getAddedFrom())
                .notifyOnAvailable(Boolean.TRUE.equals(request.getNotifyOnAvailable()))
                .build());

        activityLogService.log("WISHLIST_UPDATED", memberId, "ROLE_MEMBER", "wishlists", wishlist.getId());
        memberPreferencesService.recomputePreferences(memberId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("wishlistId", wishlist.getId());
        response.put("bookId", wishlist.getBookId());
        response.put("bookTitle", wishlist.getBookTitle());
        response.put("addedAt", wishlist.getAddedAt());
        return response;
    }

    public Map<String, Object> removeFromWishlist(String memberId, String bookId) {
        Wishlist wishlist = wishlistRepository.findByMemberIdAndBookId(memberId, bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist entry not found"));
        wishlistRepository.delete(wishlist);
        activityLogService.log("WISHLIST_UPDATED", memberId, "ROLE_MEMBER", "wishlists", wishlist.getId());
        memberPreferencesService.recomputePreferences(memberId);
        return Map.of("message", "Removed from wishlist");
    }

    private String getCategoryName(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return "Uncategorized";
        }
        try {
            BookCategory category = bookCategoryRepository.findById(categoryIds.getFirst()).orElse(null);
            return category != null ? category.getName() : "Uncategorized";
        } catch (Exception e) {
            return "Uncategorized";
        }
    }
}
