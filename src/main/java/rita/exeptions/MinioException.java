package rita.exeptions;


public class MinioException extends RuntimeException {
    public MinioException(String message) {
        super(message);
    }
    public MinioException(String message, Exception e) {
        super(message);
    }
}
