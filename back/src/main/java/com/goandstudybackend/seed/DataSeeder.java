package com.goandstudybackend.seed;

import com.goandstudybackend.entity.Admin;
import com.goandstudybackend.entity.Author;
import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookCategory;
import com.goandstudybackend.entity.BookEmbedding;
import com.goandstudybackend.entity.Publisher;
import com.goandstudybackend.entity.Staff;
import com.goandstudybackend.entity.SystemConfig;
import com.goandstudybackend.repository.AdminRepository;
import com.goandstudybackend.repository.AuthorRepository;
import com.goandstudybackend.repository.BookCategoryRepository;
import com.goandstudybackend.repository.BookEmbeddingRepository;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.PublisherRepository;
import com.goandstudybackend.repository.StaffRepository;
import com.goandstudybackend.repository.SystemConfigRepository;
import com.goandstudybackend.service.NvidiaEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final StaffRepository staffRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final BookRepository bookRepository;
    private final BookEmbeddingRepository bookEmbeddingRepository;
    private final PasswordEncoder passwordEncoder;
    private final NvidiaEmbeddingService nvidiaEmbeddingService;

    @Override
    public void run(String... args) {
        if (adminRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        seedAdmin(now);
        seedStaff(now);
        seedConfigs(now);
        seedCategories();
        seedAuthors(now);
        seedPublishers(now);
        seedBooks(now);
        log.info("GoAndStudy initial seed completed");
    }

    private void seedAdmin(LocalDateTime now) {
        adminRepository.save(Admin.builder()
                .id("AD001")
.username("Library Admin")
                .email("admin@gmail.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .plainPassword("admin123")
                .role("ROLE_ADMIN")
                .isSeeded(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void seedStaff(LocalDateTime now) {
        staffRepository.save(Staff.builder()
                .id("ST001")
                .name("Library Staff")
                .email("staff0001@gmail.com")
                .phone("0000000000")
                .passwordHash(passwordEncoder.encode("staff001"))
                .plainPassword("staff001")
                .role("ROLE_STAFF")
                .isActive(true)
                .createdByAdminId("AD001")
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void seedConfigs(LocalDateTime now) {
        systemConfigRepository.save(SystemConfig.builder()
                .id("loan_settings")
                .loanDurationDays(14)
                .maxActiveLoans(3)
                .fineRatePerDay(10.0)
                .gracePeriodDays(0)
                .updatedByAdminId("AD001")
                .updatedAt(now)
                .build());
        systemConfigRepository.save(SystemConfig.builder()
                .id("ai_settings")
                .aiModelId("meta/llama-3.1-8b-instruct")
                .aiMaxTokens(1000)
                .chatbotEnabled(true)
                .chatbotMaxMessagesPerSession(20)
                .chatbotMaxTokens(500)
                .chatbotContextWindow(5)
                .updatedByAdminId("AD001")
                .updatedAt(now)
                .build());
        systemConfigRepository.save(SystemConfig.builder()
                .id("feature_flags")
                .featureFlags(new LinkedHashMap<>(java.util.Map.of(
                        "aiRecommendations", true,
                        "reservations", true,
                        "reviews", true
                )))
                .updatedByAdminId("AD001")
                .updatedAt(now)
                .build());
    }

    private void seedCategories() {
        List<BookCategory> categories = List.of(
                category("CAT001", "Fiction", "fiction"),
                category("CAT002", "Self-help", "self-help"),
                category("CAT003", "History", "history"),
                category("CAT004", "Sci-Fi", "sci-fi"),
                category("CAT005", "Memoir", "memoir"),
                category("CAT006", "Philosophy", "philosophy"),
                category("CAT007", "Psychology", "psychology"),
                category("CAT008", "Biography", "biography"),
                category("CAT009", "Mystery", "mystery"),
                category("CAT010", "Technology", "technology"),
                category("CAT011", "Fantasy", "fantasy"),
                category("CAT012", "Romance", "romance"),
                category("CAT013", "Horror", "horror"),
                category("CAT014", "Poetry", "poetry"),
                category("CAT015", "Religious", "religious")
        );
        bookCategoryRepository.saveAll(categories);
    }

    private void seedAuthors(LocalDateTime now) {
        authorRepository.saveAll(List.of(
                author("AUT001", "Paulo Coelho", "Brazilian", List.of("Fiction"), now),
                author("AUT002", "James Clear", "American", List.of("Self-help"), now),
                author("AUT003", "Yuval Noah Harari", "Israeli", List.of("History"), now),
                author("AUT004", "Andy Weir", "American", List.of("Sci-Fi"), now),
                author("AUT005", "Tara Westover", "American", List.of("Memoir"), now)
        ));
    }

    private void seedPublishers(LocalDateTime now) {
        publisherRepository.saveAll(List.of(
                publisher("PUB001", "HarperOne", "USA", now),
                publisher("PUB002", "Avery", "USA", now),
                publisher("PUB003", "Harper", "USA", now),
                publisher("PUB004", "Ballantine Books", "USA", now),
                publisher("PUB005", "Random House", "USA", now)
        ));
    }

    private void seedBooks(LocalDateTime now) {
        createSeedBook(now, "9780062315007", "The Alchemist", "AUT001", "PUB001", "CAT001",
                "A shepherd boy named Santiago follows recurring dreams in search of treasure and discovers a deeper journey of purpose and self-belief.",
                "https://covers.openlibrary.org/b/isbn/9780062315007-L.jpg");
        createSeedBook(now, "9780735211292", "Atomic Habits", "AUT002", "PUB002", "CAT002",
                "A practical guide to building good habits, breaking bad ones, and improving through small, consistent changes.",
                "https://covers.openlibrary.org/b/isbn/9780735211292-L.jpg");
        createSeedBook(now, "9780062316097", "Sapiens", "AUT003", "PUB003", "CAT003",
                "A sweeping history of humankind exploring how culture, biology, and shared myths shaped modern civilization.",
                "https://covers.openlibrary.org/b/isbn/9780062316097-L.jpg");
        createSeedBook(now, "9780593135204", "Project Hail Mary", "AUT004", "PUB004", "CAT004",
                "A lone astronaut wakes far from Earth and must solve an impossible scientific crisis to save humanity.",
                "https://covers.openlibrary.org/b/isbn/9780593135204-L.jpg");
        createSeedBook(now, "9780399590504", "Educated", "AUT005", "PUB005", "CAT005",
                "A memoir about resilience, learning, and the transformative power of education against the odds.",
                "https://covers.openlibrary.org/b/isbn/9780399590504-L.jpg");
    }

    private void createSeedBook(LocalDateTime now,
                                String isbn,
                                String title,
                                String authorId,
                                String publisherId,
                                String categoryId,
                                String description,
                                String coverImageUrl) {
        List<Double> embedding = nvidiaEmbeddingService.generateEmbedding(title + " " + description);
        Book book = bookRepository.save(Book.builder()
                .isbn(isbn)
                .title(title)
                .authorIds(new ArrayList<>(List.of(authorId)))
                .publisherId(publisherId)
                .categoryIds(new ArrayList<>(List.of(categoryId)))
                .description(description)
                .language("en")
                .publishedYear(2021)
                .pageCount(300)
                .totalCopies(5)
                .availableCopies(5)
                .averageRating(0.0)
                .totalRatings(0)
                .totalIssues(0)
                .embedding(new ArrayList<>(embedding))
                .coverImageUrl(coverImageUrl)
                .coverImageThumbnailUrl(coverImageUrl.replace("-L.jpg", "-M.jpg"))
                .coverImageFormat("jpg")
                .hasCoverImage(true)
                .tags(new ArrayList<>(List.of(title.toLowerCase().replace(" ", "-"))))
                .isDeleted(false)
                .addedByStaffId("ST001")
                .createdAt(now)
                .updatedAt(now)
                .build());

        bookEmbeddingRepository.save(BookEmbedding.builder()
                .bookId(book.getId())
                .isbn(book.getIsbn())
                .embeddingModel("nvidia/nv-embedqa-e5-v5")
                .embedding(new ArrayList<>(embedding))
                .embeddingSource("seed_data")
                .dimensions(1024)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private BookCategory category(String id, String name, String slug) {
        return BookCategory.builder()
                .id(id)
                .name(name)
                .slug(slug)
                .description(name + " books")
                .bookCount(0)
                .popularityScore(0.0)
                .build();
    }

    private Author author(String id, String name, String nationality, List<String> genres, LocalDateTime now) {
        return Author.builder()
                .id(id)
                .name(name)
                .nationality(nationality)
                .genres(new ArrayList<>(genres))
                .totalBooks(1)
                .averageRating(0.0)
                .createdAt(now)
                .build();
    }

    private Publisher publisher(String id, String name, String country, LocalDateTime now) {
        return Publisher.builder()
                .id(id)
                .name(name)
                .country(country)
                .website("")
                .totalBooks(1)
                .createdAt(now)
                .build();
    }
}
