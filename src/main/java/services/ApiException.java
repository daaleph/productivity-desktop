package services;

public class ApiException extends RuntimeException {
    private final int statusCode;
    private final String apiPath;

    /**
     * Constructs an API exception with a message and cause
     * @param message The descriptive error message
     * @param cause The underlying exception
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.apiPath = null;
    }

    /**
     * Constructs an API exception with HTTP status details
     * @param statusCode The HTTP status code
     * @param apiPath The API endpoint path
     * @param message The descriptive error message
     */
    public ApiException(int statusCode, String apiPath, String message) {
        super(message);
        this.statusCode = statusCode;
        this.apiPath = apiPath;
    }

    /**
     * @return The HTTP status code (-1 if not available)
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return The API endpoint path (null if not available)
     */
    public String getApiPath() {
        return apiPath;
    }

    @Override
    public String toString() {
        if (statusCode > 0) {
            return String.format("ApiException [status=%d, path=%s]: %s",
                    statusCode, apiPath, getMessage());
        }
        return String.format("ApiException: %s", getMessage());
    }
}