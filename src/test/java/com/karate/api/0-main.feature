Feature: web and api

Background:
* def vFileChooserPath = Java.type('com.karate.api.JavaUtil').fileChoose()



Scenario: API Main feature for later purpose
* print 'Mainfile vFileChooserPath'+vFileChooserPath

# Calling first feature file
* call read('1-karate-driver-and-api.feature')











Enhanced Architecture Components
1. API Producer System (ABC System)
Exposes endpoints like:

/api/credit-card

/api/debit-card

/api/loan-details

Built with RESTful standards, OpenAPI/Swagger documentation.

Stateless microservices structure.

2. API Gateway Layer (Security & Traffic Control)
Tools: Kong, AWS API Gateway, Apigee, NGINX, or Spring Cloud Gateway

Responsibilities:

Authentication & Authorization: Supports OAuth2, JWT, or API keys.

Rate Limiting: Protects backend by throttling abusive traffic.

Caching: Reduces load on API producer.

Logging & Monitoring: Hooks with ELK, Prometheus, Datadog.

IP Whitelisting, CORS, SSL/TLS termination

3. Load Balancer
Between consumers and ABC system (or API Gateway and ABC backend).

Ensures even traffic distribution and availability.

Tools: AWS ALB/ELB, HAProxy, NGINX

4. Microservice Wrappers/Helpers for Consumers
Each testing framework gets a thin microservice adapter or helper module.

Responsibilities:

Converts API responses to formats understood by the consumer.

Adds retry logic, polling, mock data layers if needed.
Consumer Tool	Helper or Microservice Style Adapter
Selenium	Java-based API Client integrated into test setup
Karate DSL	Reusable feature files + JS or Java glue
Parasoft	Service virtualization or REST Client module
Sauce Labs	Test runner + microservice triggers + validation hooks

5. Developer & Test Portal (Optional Layer)
For QA/test teams to:

View API health

Fetch test tokens

Read schema documentation

Trigger test data generation

6. CI/CD Integration
Deploy API services, test adapters

Run test jobs automatically using Jenkins, GitHub Actions, GitLab CI

7. Monitoring & Observability
API Gateway + ABC logs to ELK / Loki / Datadog / New Relic

Alerts for:

Rate limiting breaches


7. Monitoring & Observability
API Gateway + ABC logs to ELK / Loki / Datadog / New Relic

Alerts for:

Rate limiting breaches

Failed auth

High latency calls

5xx errors


Best Practice Summary
Per Framework Adapter: Best for flexibility and deep control.

Shared Gateway Adapter: Best for uniformity and less duplication.

Always keep helpers modular, stateless, and easily updatable.

Would you like a comparison table of "per-framework helper" vs "shared gateway helper"?

    +---------------------+
                    |     ABC System      |
                    | (Central API Hub)   |
                    +---------------------+
                              |
        +---------------------+-----------------------+
        |                     |                       |
    +--------+          +-----------+            +-----------+
    | Helper |          | Helper    |            | Helper    |
    | for    |          | for       |            | for       |
    | Selenium          | Karate    |            | Parasoft  |
    +--------+          +-----------+            +-----------+
        |                     |                       |
    +------------+     +------------+          +--------------+
    | Selenium   |     | Karate     |          | Parasoft     |
    | Framework  |     | Framework  |          | Framework    |
    +------------+     +------------+          +--------------+


    Benefits of Per-Framework Helpers

Feature	Benefit
Tailored to Language/Tool	Match native format (Java, DSL, GUI) so testers don’t fight the tool.
Custom Retry & Logic	Each helper can handle retries, timeouts, token renewal, etc. as needed
Isolation of Changes	Changing ABC API only needs update in one helper, not in all test cases
Debuggability	Logs and traces close to the test context, easier troubleshooting
Reusable Across Projects	Once built, helpers can be imported into any test suite or pipeline


Helper Design Responsibilities
Each helper should:

Encapsulate HTTP calls to ABC APIs.

Handle authentication, rate limiting, error formatting.

Return clean data structures that the framework can easily consume.

Optionally log performance or telemetry data.





public class ABCClient {
    private final String baseUrl = "https://abc-system/api";

    public CreditCardResponse getCreditCardDetails(String userId) {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/credit-card/" + userId))
            .header("Authorization", "Bearer " + token)
            .build();
        HttpResponse<String> res = client.send(req, BodyHandlers.ofString());
        return parseCreditCard(res.body());
    }
}


CreditCardResponse data = ABCClient.getCreditCardDetails("user123");
assert data.getBalance() > 0;




import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ABCClient {

    private final WebClient webClient;

    public ABCClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://abc-system/api")
                .build();
    }

    public Mono<CreditCardResponse> getCreditCardDetails(String userId, String token) {
        return webClient.get()
                .uri("/credit-card/{userId}", userId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(CreditCardResponse.class);
    }
}




public class CreditCardResponse {
    private String cardNumber;
    private double balance;
    private String status;

    // Getters and setters
}



@Autowired
private ABCClient abcClient;

public void testCardDetails() {
    String token = "your-oauth-token"; // Can be fetched via another service
    abcClient.getCreditCardDetails("user123", token)
             .subscribe(response -> {
                 System.out.println("Balance: " + response.getBalance());
             });
}



CreditCardResponse response = abcClient
    .getCreditCardDetails("user123", token)
    .block();



<!-- Spring Boot Starter Web (Reactive stack) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Project Reactor (Optional, for Mono/Flux support explicitly) -->
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-core</artifactId>
</dependency>

<!-- Lombok (Optional, for DTO boilerplate reduction) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Jackson for JSON (usually included in spring-boot-starter) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>



<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.2.4</version> <!-- Use your Spring Boot version -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>


implementation 'org.springframework.boot:spring-boot-starter-webflux'
implementation 'io.projectreactor:reactor-core'
implementation 'com.fasterxml.jackson.core:jackson-databind'
compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'



Let me know if you also want to add:

OAuth2 token handling

Retry/backoff logic

Timeout handling

Logging with WebClient filters


Great! Here's the enhanced setup with WebClient support for:

OAuth2 Token Handling

Retry with backoff

Timeout handling

Logging



<!-- WebClient core -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- OAuth2 support (optional, if needed) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- Logging -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
</dependency>

<!-- Reactor Retry support -->
<dependency>
    <groupId>io.projectreactor.addons</groupId>
    <artifactId>reactor-extra</artifactId>
</dependency>


@Configuration
public class WebClientConfig {

    @Value("${api.base-url}")
    private String baseUrl;

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())  // Logging
                .filter(logResponse()) // Logging
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("Response status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}


@Service
public class ApiClientService {

    private final WebClient webClient;

    public ApiClientService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> fetchLoanDetails(String userId) {
        return webClient.get()
                .uri("/loan-details/{id}", userId)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                    .filter(this::isRetryableException))
                .timeout(Duration.ofSeconds(10));
    }

    private boolean isRetryableException(Throwable throwable) {
        return throwable instanceof TimeoutException
                || throwable instanceof ConnectException;
    }
}



@Bean
public WebClient webClient(ClientRegistrationRepository clients,
                           OAuth2AuthorizedClientRepository authRepo) {
    ServletOAuth2AuthorizedClientExchangeFilterFunction oauth =
        new ServletOAuth2AuthorizedClientExchangeFilterFunction(clients, authRepo);
    oauth.setDefaultOAuth2AuthorizedClient(true);

    return WebClient.builder()
            .apply(oauth.oauth2Configuration())
            .build();
}





 

