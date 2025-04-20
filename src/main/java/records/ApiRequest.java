package records;

import services.ApiClient;

import java.util.Map;

public record ApiRequest<T>(
        String path,
        ApiClient.HttpMethod method,
        Map<String, String> queryParams,
        Object body,
        Class<T> responseType
) {}