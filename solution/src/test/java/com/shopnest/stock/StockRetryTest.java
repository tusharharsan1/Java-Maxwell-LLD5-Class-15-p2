package com.shopnest.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Checks the retry problem of Class 15 (The Patient Retry).
 * The warehouse is replaced by a mock, so every test decides exactly what the warehouse answers
 * on call 1, call 2, call 3 ... and then counts the calls that really reached it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class StockRetryTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PretendWarehouse warehouse;

    private static HttpServerErrorException serverError(int code) {
        return new HttpServerErrorException(HttpStatusCode.valueOf(code));
    }

    private static HttpClientErrorException clientError(int code) {
        return new HttpClientErrorException(HttpStatusCode.valueOf(code));
    }

    // ------------------------------------------------------------------
    // the happy path (also tells us the endpoint and the finished code still work)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A healthy warehouse: 200, the stock level, one call, and no Retry-After")
    void healthyWarehouse() throws Exception {
        when(warehouse.quantityOf("C3")).thenReturn(12);

        mvc.perform(get("/stock/C3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("C3"))
                .andExpect(jsonPath("$.quantity").value(12))
                .andExpect(header().doesNotExist("Retry-After"));
        verify(warehouse, times(1)).quantityOf("C3");
    }

    // ------------------------------------------------------------------
    // Tasks 1 and 2: the retry
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Tasks 1+2: a 503 and then a good answer: the customer sees 200, after 2 calls")
    void oneBadMomentIsHidden() throws Exception {
        when(warehouse.quantityOf("D4")).thenThrow(serverError(503)).thenReturn(5);

        mvc.perform(get("/stock/D4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("D4"))
                .andExpect(jsonPath("$.quantity").value(5));
        verify(warehouse, times(2)).quantityOf("D4");
    }

    @Test
    @DisplayName("Tasks 1+2: two bad moments and then a good answer: the last allowed try works (3 calls)")
    void theLastAllowedTryWorks() throws Exception {
        when(warehouse.quantityOf("E5")).thenThrow(serverError(503)).thenThrow(serverError(503)).thenReturn(8);

        mvc.perform(get("/stock/E5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(8));
        verify(warehouse, times(3)).quantityOf("E5");
    }

    @Test
    @DisplayName("Tasks 1+2: it gives up after 3 calls (1 first try and 2 retries), not 2 and not 4")
    void givesUpAfterThreeCalls() throws Exception {
        when(warehouse.quantityOf("F6")).thenThrow(serverError(503));

        mvc.perform(get("/stock/F6")).andExpect(status().isServiceUnavailable());
        verify(warehouse, times(3)).quantityOf("F6");
    }

    @ParameterizedTest(name = "Tasks 1+2: every 5xx is retried (HTTP {0})")
    @ValueSource(ints = {500, 502, 503, 504})
    void everyServerErrorIsRetried(int code) throws Exception {
        when(warehouse.quantityOf("G7")).thenThrow(serverError(code));

        mvc.perform(get("/stock/G7")).andExpect(status().isServiceUnavailable());
        verify(warehouse, times(3)).quantityOf("G7");
    }

    @ParameterizedTest(name = "Tasks 1+2: a 4xx is never retried and keeps its status (HTTP {0})")
    @ValueSource(ints = {400, 401, 404, 429})
    void aClientErrorIsNotRetried(int code) throws Exception {
        when(warehouse.quantityOf("H8")).thenThrow(clientError(code));

        mvc.perform(get("/stock/H8"))
                .andExpect(status().is(code))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(code));
        verify(warehouse, times(1)).quantityOf("H8");
    }

    @Test
    @DisplayName("Tasks 1+2: any other exception is not retried, and it is not turned into a 503")
    void anotherExceptionIsNotRetried() {
        when(warehouse.quantityOf("J9")).thenThrow(new IllegalStateException("not a warehouse answer"));

        assertThatThrownBy(() -> mvc.perform(get("/stock/J9")))
                .hasRootCauseInstanceOf(IllegalStateException.class);
        verify(warehouse, times(1)).quantityOf("J9");
    }

    @Test
    @DisplayName("Task 2: the five settings of @Retryable on WarehouseClient.availableQuantity")
    void theFiveSettings() throws Exception {
        Method method = WarehouseClient.class.getMethod("availableQuantity", String.class);
        Retryable retryable = AnnotatedElementUtils.findMergedAnnotation(method, Retryable.class);

        assertThat(retryable).as("@Retryable on WarehouseClient.availableQuantity").isNotNull();
        assertThat(retryable.includes()).as("includes").containsExactly(HttpServerErrorException.class);
        assertThat(number(retryable.maxRetries(), retryable.maxRetriesString())).as("maxRetries").isEqualTo(2L);
        assertThat(number(retryable.delay(), retryable.delayString())).as("delay").isEqualTo(100L);
        assertThat(number(retryable.jitter(), retryable.jitterString())).as("jitter").isEqualTo(50L);
        double multiplier = retryable.multiplierString().isEmpty()
                ? retryable.multiplier() : Double.parseDouble(retryable.multiplierString().trim());
        assertThat(multiplier).as("multiplier").isEqualTo(3.0);
        assertThat(retryable.timeUnit()).as("timeUnit (the delay is in milliseconds)").isEqualTo(TimeUnit.MILLISECONDS);
    }

    private static long number(long value, String text) {
        return text.isEmpty() ? value : Long.parseLong(text.trim());
    }

    @Test
    @DisplayName("Task 2: the retry waits between the calls (at least 100 ms + 150 ms), and does not wait for ever")
    void theRetryIsPatient() throws Exception {
        when(warehouse.quantityOf("K1")).thenThrow(serverError(503));

        long start = System.nanoTime();
        mvc.perform(get("/stock/K1")).andExpect(status().isServiceUnavailable());
        long millis = (System.nanoTime() - start) / 1_000_000;

        // the waits are about 100 (100 to 150) and about 300 (150 to 450) milliseconds
        assertThat(millis).as("time of the whole request, in ms").isGreaterThanOrEqualTo(240L).isLessThan(5_000L);
    }

    // ------------------------------------------------------------------
    // Task 3: the wrapper in StockService (unit test, no Spring)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Task 3: a server error becomes WarehouseUnavailableException, with the server error as its cause")
    void serverErrorIsWrapped() {
        WarehouseClient client = mock(WarehouseClient.class);
        HttpServerErrorException failure = serverError(503);
        when(client.availableQuantity("A1")).thenThrow(failure);

        assertThatThrownBy(() -> new StockService(client).stockOf("A1"))
                .isInstanceOf(WarehouseUnavailableException.class)
                .hasCause(failure);
    }

    @Test
    @DisplayName("Task 3: any other exception goes through unchanged (the same object)")
    void otherExceptionsGoThroughUnchanged() {
        WarehouseClient client = mock(WarehouseClient.class);
        HttpClientErrorException notFound = clientError(404);
        IllegalArgumentException odd = new IllegalArgumentException("odd");
        when(client.availableQuantity("A1")).thenThrow(notFound);
        when(client.availableQuantity("A2")).thenThrow(odd);
        StockService service = new StockService(client);

        assertThatThrownBy(() -> service.stockOf("A1")).isSameAs(notFound);
        assertThatThrownBy(() -> service.stockOf("A2")).isSameAs(odd);
    }

    @Test
    @DisplayName("Task 3: a good answer becomes a StockLevel with the same sku and quantity")
    void goodAnswerBecomesAStockLevel() {
        WarehouseClient client = mock(WarehouseClient.class);
        when(client.availableQuantity("Z26")).thenReturn(41);

        StockLevel level = new StockService(client).stockOf("Z26");

        assertThat(level.sku()).isEqualTo("Z26");
        assertThat(level.quantity()).isEqualTo(41);
    }

    // ------------------------------------------------------------------
    // Task 4: the clear 503
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Task 4: a warehouse that keeps failing gives 503, Retry-After: 3, application/problem+json, and the title")
    void theClearAnswer() throws Exception {
        when(warehouse.quantityOf("L2")).thenThrow(serverError(503));

        mvc.perform(get("/stock/L2"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "3"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").value("Stock check unavailable"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @DisplayName("Task 4: the detail is a sentence a person can read (not empty, no exception name)")
    void theDetailIsReadable() throws Exception {
        when(warehouse.quantityOf("M3")).thenThrow(serverError(503));

        String detail = com.jayway.jsonpath.JsonPath.read(
                mvc.perform(get("/stock/M3")).andReturn().getResponse().getContentAsString(), "$.detail");

        assertThat(detail.trim()).isNotEmpty().doesNotContain("Exception");
    }
}
