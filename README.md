> **Where the files are:** the finished code and the tests are in the `solution/` folder of this branch (`test-file`). The starter code (with the `TODO` notes) is on the branch `main`.

# The Patient Retry at ShopNest

## Problem Statement
ShopNest shows how many pieces of an item are in stock. The number comes from the warehouse service, which is a "friend" that our shop calls. Sometimes the warehouse has a bad moment and answers with HTTP 503, and a second try, a moment later, works. A **retry** means that the shop tries again, so the customer never sees the small failure. But a good retry is **patient** and has a **limit**, and when it still fails, the shop must **tell the caller the truth**. In class, you did this for the auth service: `@EnableResilientMethods`, `@Retryable`, and a clear 503 with `Retry-After`. Now you do the same for the warehouse, in a small Spring Boot application. You need about 12 lines of code, in 4 small tasks. There is no database and no internet: the warehouse is pretend, and you can make it fail with one URL.

## Requirements
### What the application does
| Request | Answer | Written already? |
|---|---|---|
| `GET /stock/{sku}` | the stock level, as JSON: `{"sku":"A1","quantity":7}` | yes |
| `GET /pretend/warehouse?fail=100&code=503` | makes the pretend warehouse fail (`fail` = how many calls out of 100, `code` = the HTTP error) | yes |

### The retry (Tasks 1 and 2)
The call to the warehouse is in `WarehouseClient.availableQuantity`. It must be retried with these numbers:

| Setting of `@Retryable` | Value | What it means |
|---|---|---|
| `includes` | `HttpServerErrorException.class` | retry only a server error (HTTP 5xx) |
| `maxRetries` | 2 | 2 retries **after** the first try, so at most 3 calls in all |
| `delay` | 100 | the first wait is 100 ms |
| `multiplier` | 3 | each wait is 3 times the one before: 100, then 300 |
| `jitter` | 50 | a small random change on each wait, so that many shops do not come back together |

| What the warehouse answers | Retry? | Why |
|---|---|---|
| HTTP 5xx (for example 503) | **yes** | the warehouse has a bad moment, so the next try may work |
| HTTP 4xx (for example 404) | no | the question itself is wrong, and asking again changes nothing |

### The answers (Tasks 3 and 4)
| When | The shop answers |
|---|---|
| the warehouse works (also after a retry) | `200` and `{"sku":"A1","quantity":7}` |
| the warehouse still answers 5xx after all the retries | `503`, with the header `Retry-After: 3`, the content type `application/problem+json`, the title `Stock check unavailable`, and a short detail that a person can read |
| the warehouse answers 4xx (for example 404) | the same status (this is already written, and it is your example) |

### The rules
- **`@Retryable` goes on `WarehouseClient`, and it is called from `StockService`.** A retry works through a proxy, so a call from a method of the same class would skip it.
- **The switch.** `@Retryable` does nothing until `@EnableResilientMethods` is on the application class.
- **At most 3 calls to the warehouse** for one request that fails with 5xx (1 first try and 2 retries).
- **A 4xx is never retried**, and it is passed on with its own status.
- **Do not catch `Exception` or `RuntimeException` in `StockService`.** Catch only `HttpServerErrorException`, so that a 404 goes through unchanged.
- The pretend warehouse writes one line in the console for every call it gets, so you can count the calls that really reach it.

To achieve these requirements we need to do 4 things:
- **Task 1:** turn on Spring's retry feature in `StockApplication`.
- **Task 2:** add the retry to `WarehouseClient.availableQuantity`.
- **Task 3:** finish `StockService.stockOf`: turn a failing warehouse into a `WarehouseUnavailableException`.
- **Task 4:** add the handler for `WarehouseUnavailableException` to `ApiExceptionHandler`: the clear 503.

## Instructions
1. There is one TODO note in each of these four files: `StockApplication.java` (`TODO 1`), `WarehouseClient.java` (`TODO 2`), `StockService.java` (`TODO 3`) and `ApiExceptionHandler.java` (`TODO 4`).
2. Do not modify the other files (they are finished), the dependencies in `pom.xml` (Spring's retry is part of Spring Framework 7, so no extra library is needed), the names of the classes and methods, or the dependencies that are injected in the classes. Feel free to add new methods or imports if required.
3. Start the application (`mvn spring-boot:run`, or run `StockApplication` in your IDE). It runs on port 8080. Then try these commands in a second terminal, and compare with the answers below. When all four tasks are finished, you should see this:
   ```
   $ curl localhost:8080/stock/A1
   {"sku":"A1","quantity":7}

   $ curl "localhost:8080/pretend/warehouse?fail=100&code=503"
   warehouse is now: fail=100 %, code=503

   $ curl -i localhost:8080/stock/A1
   HTTP/1.1 503
   Retry-After: 3
   Content-Type: application/problem+json
   ...
   {"detail":"(your own words)","instance":"/stock/A1","status":503,"title":"Stock check unavailable"}
   ```
   Look at the console of your application too. The pretend warehouse wrote **3 lines** for that one request (`pretend warehouse: A1 -> 503`): 1 first try and 2 retries. The answer came after a little less than half a second, because of the two waits (about 100 and about 300 milliseconds).

   Now the case that must **not** be retried:
   ```
   $ curl "localhost:8080/pretend/warehouse?fail=100&code=404"
   warehouse is now: fail=100 %, code=404

   $ curl -i localhost:8080/stock/A1
   HTTP/1.1 404
   Content-Type: application/problem+json
   ...
   ```
   This time the console shows **1 line**. A 404 is not retried.

   Optional: make the warehouse flaky, with `curl "localhost:8080/pretend/warehouse?fail=70&code=503"`, and send `curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/stock/A1` 100 times. With the retry, about 2 in 3 answers are `200`. Without the retry (the starter), only about 3 in 10 are.

   Before you start, the application already runs: the healthy answer works, but with `fail=100&code=503` the shop answers `500` and the console shows only **1 line** for the request (no retry, and no clear 503).
4. Your code will be checked with the test file `src/test/java/com/shopnest/stock/StockRetryTest.java`. It checks the cases in the Requirements above, and the rules. While you write your code, use the commands in instruction 3 to check your work.
5. You do not need a database, the internet or a key. It works with Java 17 and Maven, like the project from class.
6. Hints (all of these use ideas from class):
   - **Task 1 (the switch).** One line on the application class, under `@SpringBootApplication`. The import is `org.springframework.resilience.annotation.EnableResilientMethods`:
     ```java
     @SpringBootApplication
     @EnableResilientMethods
     public class StockApplication {
     ```
   - **Task 2 (the retry).** One annotation above the method. Be careful with the imports: `org.springframework.resilience.annotation.Retryable` (this one is part of Spring Framework 7; it is not the Spring Retry library) and `org.springframework.web.client.HttpServerErrorException`. Write the five settings from the table above:
     ```java
     @Retryable(includes = HttpServerErrorException.class, maxRetries = 2, delay = 100, multiplier = 3, jitter = 50)
     public int availableQuantity(String sku) {
     ```
   - **Task 3 (the wrapper).** Put the line that is already there inside a `try`, and throw the clear exception in a `catch` for `HttpServerErrorException` only. After all the retries, the last exception comes out of `WarehouseClient` as it is:
     ```java
     try {
         return new StockLevel(sku, warehouseClient.availableQuantity(sku));
     } catch (HttpServerErrorException e) {
         throw new WarehouseUnavailableException(e);
     }
     ```
   - **Task 4 (the clear 503).** Copy the style of the handler that is already there. A `ProblemDetail` has `setTitle(...)`, and `ResponseEntity` has `.header(name, value)`:
     ```java
     @ExceptionHandler(WarehouseUnavailableException.class)
     public ResponseEntity<ProblemDetail> handleUnavailable(WarehouseUnavailableException e) {
         ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                 "We cannot check the stock right now. Please try again in a few seconds.");
         problem.setTitle("Stock check unavailable");
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                 .header("Retry-After", "3")
                 .body(problem);
     }
     ```
   - **If something looks wrong.** If the 503 case shows only 1 line in the console, the retry does not work: check `@EnableResilientMethods` (Task 1), the annotation (Task 2), the import (it must be `org.springframework.resilience.annotation.Retryable`), and that `StockService` is a different class from `WarehouseClient`. If the 503 case shows 4 lines, `maxRetries` counts the retries **after** the first try: 2 retries make 3 calls. If the 404 case shows 3 lines, `includes` is too wide: a 4xx must not be retried. If the 404 case answers `503`, you catch too much in `StockService`: catch only `HttpServerErrorException`. If the answer is `500` and not `503`, Task 3 or Task 4 is missing. If the answer has no `Retry-After` header, add it in the handler. If the content type is not `application/problem+json`, the body must be a `ProblemDetail`.
