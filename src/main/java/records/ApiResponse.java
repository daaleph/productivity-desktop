package records;

import java.util.List;
import java.util.Map;

public record ApiResponse<T>(
        int statusCode,
        T body,
        Map<String, List<String>> headers
) {
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
}