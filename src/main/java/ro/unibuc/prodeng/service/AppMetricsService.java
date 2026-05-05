package ro.unibuc.prodeng.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import ro.unibuc.prodeng.repository.DocumentRepository;
import ro.unibuc.prodeng.repository.UserRepository;

/**
 * Central service for all custom application metrics.
 *
 * Metrics overview:
 * CATEGORY | METRIC NAME | TYPE
 * ────────────────────────────────────────────────────────────────
 * Business | app_users_created_total | Counter
 * Business | app_documents_created_total | Counter
 * Error | app_users_creation_failed_total | Counter
 * Error | app_document_access_denied_total | Counter
 * Performance | app_document_lookup_duration_seconds | Timer
 * Resource | app_active_registered_users | Gauge
 * Domain-specific| app_documents_total | Gauge
 */
@Service
public class AppMetricsService {

        // ── Business metrics ──────────────────────────────────────────────────────

        /** Total number of users successfully registered. */
        private final Counter usersCreatedCounter;

        /** Total number of documents successfully created. */
        private final Counter documentsCreatedCounter;

        // ── Error metrics ─────────────────────────────────────────────────────────

        /** Total number of failed user-creation attempts (e.g. duplicate e-mail). */
        private final Counter usersCreationFailedCounter;

        /**
         * Total number of access-denied events when modifying/deleting a document.
         * Tagged with {@code reason} so callers can distinguish edit vs. delete
         * denials.
         */
        private final Counter documentAccessDeniedCounter;

        // ── Performance metrics ───────────────────────────────────────────────────

        /** Latency of document-lookup operations (getLatestByGroupId). */
        private final Timer documentLookupTimer;

        // ── Constructor ───────────────────────────────────────────────────────────

        public AppMetricsService(MeterRegistry registry,
                        UserRepository userRepository,
                        DocumentRepository documentRepository) {

                // Business
                this.usersCreatedCounter = Counter.builder("app_users_created_total")
                                .description("Total number of users successfully created")
                                .tag("category", "business")
                                .register(registry);

                this.documentsCreatedCounter = Counter.builder("app_documents_created_total")
                                .description("Total number of documents successfully created")
                                .tag("category", "business")
                                .register(registry);

                // Error
                this.usersCreationFailedCounter = Counter.builder("app_users_creation_failed_total")
                                .description("Total number of failed user creation attempts")
                                .tag("category", "error")
                                .register(registry);

                this.documentAccessDeniedCounter = Counter.builder("app_document_access_denied_total")
                                .description("Total number of document access-denied events")
                                .tag("category", "error")
                                .register(registry);

                // Performance
                this.documentLookupTimer = Timer.builder("app_document_lookup_duration_seconds")
                                .description("Time taken to look up the latest version of a document")
                                .tag("category", "performance")
                                .register(registry);

                // Resource – live count pulled from DB on each scrape
                Gauge.builder("app_active_registered_users", userRepository, repo -> (double) repo.count())
                                .description("Number of currently registered users in the database")
                                .tag("category", "resource")
                                .register(registry);

                // Domain-specific – live document count (all versions) on each scrape
                Gauge.builder("app_documents_total", documentRepository, repo -> (double) repo.count())
                                .description("Total number of document records (all versions) stored in the database")
                                .tag("category", "domain")
                                .register(registry);
        }

        // ── Public recording helpers ──────────────────────────────────────────────

        public void recordUserCreated() {
                usersCreatedCounter.increment();
        }

        public void recordUserCreationFailed() {
                usersCreationFailedCounter.increment();
        }

        public void recordDocumentCreated() {
                documentsCreatedCounter.increment();
        }

        public void recordDocumentAccessDenied() {
                documentAccessDeniedCounter.increment();
        }

        public Timer getDocumentLookupTimer() {
                return documentLookupTimer;
        }
}
