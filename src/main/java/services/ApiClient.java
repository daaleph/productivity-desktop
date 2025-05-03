package services;

import records.ApiRequest;
import records.ApiResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public sealed interface ApiClient permits JsonApiClient {
    enum HttpMethod { GET, POST, PUT, DELETE, PATCH }

    <T> CompletableFuture<ApiResponse<T>> execute(ApiRequest<T> request);
    <T> ApiRequest<T> buildUserApiRequest(Class<T> responseType, String email, String... pathSegments);
    <T> T executeApiRequest(ApiRequest<T> request) throws InterruptedException, ExecutionException;
    <T> ApiRequest<T> buildWritingRequest(
            Class<T> responseType, HttpMethod method, String email, Object body, String... pathSegments
    );
}