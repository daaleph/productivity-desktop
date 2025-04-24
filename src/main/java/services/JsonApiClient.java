// JsonApiClient.java
package services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import records.ApiRequest;
import records.ApiResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static data.Abbreviations.getAbbreviation;

public final class JsonApiClient implements ApiClient {

    private final String EMAIL = getAbbreviation("email");
    private static final String BASE_URL = "http://localhost:4000/api";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public <T> ApiRequest<T> buildUserApiRequest(Class<T> responseType, String email, String... pathSegments) {
        String path = "/" + String.join("/", pathSegments);
        return new ApiRequest<T>(
                path,
                ApiClient.HttpMethod.GET,
                Map.of(EMAIL, email),
                null,
                responseType
        );
    }

     @Override
     public <T> T executeApiRequest(ApiRequest<T> request) throws InterruptedException {
         try {
             ApiResponse<T> response = this.execute(request).get();
             if (!response.isSuccess()) {
                 throw new ApiException(
                     response.statusCode(),
                     request.path(),
                     "API request failed with status: " + response.statusCode()
                 );
             }
             return response.body();
         } catch (ExecutionException e) {
             Throwable cause = e.getCause();
             if (cause instanceof ApiException) throw (ApiException) cause;
             throw new ApiException("Failed to execute API request: ", e);
         }
     }

    @Override
    public <T> CompletableFuture<ApiResponse<T>> execute(ApiRequest<T> request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest httpRequest = buildHttpRequest(request);
                HttpResponse<String> response = CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                return processResponse(response, request.responseType());
            } catch (Exception e) {
                throw new ApiException("API request failed", e);
            }
        });
    }

    private HttpRequest buildHttpRequest(ApiRequest<?> request) throws JsonProcessingException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(buildUri(request))
                .header("Content-Type", "application/json");

        return switch (request.method()) {
            case GET -> builder.GET().build();
            case DELETE -> builder.DELETE().build();
            default -> {
                String methodName = request.method().name();
                HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers
                        .ofString(MAPPER.writeValueAsString(request.body()));
                yield builder.method(methodName, bodyPublisher).build();
            }
        };
    }

    private URI buildUri(ApiRequest<?> request) {
        var urlBuilder = new StringBuilder(BASE_URL).append(request.path());
        if (!request.queryParams().isEmpty()) {
            urlBuilder.append("?");
            request.queryParams().forEach((k, v) ->
                    urlBuilder.append(k).append("=").append(v).append("&"));
            urlBuilder.setLength(urlBuilder.length() - 1);
        }
        return URI.create(urlBuilder.toString());
    }

    private <T> ApiResponse<T> processResponse(
            HttpResponse<String> response,
            Class<T> type
    ) throws JsonProcessingException {
        return new ApiResponse<T>(
            response.statusCode(),
            MAPPER.readValue(response.body(), type),
            response.headers().map()
        );
    }
}