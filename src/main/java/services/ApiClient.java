package services;

import records.ApiRequest;
import records.ApiResponse;

import java.util.concurrent.CompletableFuture;

public sealed interface ApiClient permits JsonApiClient {
    enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH
    }

    <T> CompletableFuture<ApiResponse<T>> execute(ApiRequest<T> request);
}