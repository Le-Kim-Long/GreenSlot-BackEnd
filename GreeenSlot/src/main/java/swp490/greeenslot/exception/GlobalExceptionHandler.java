package swp490.greeenslot.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import swp490.greeenslot.dto.MessageResponseDTO;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<MessageResponseDTO> handleValidation(Exception ex) {
        if (ex instanceof BindException bindEx) {
            String message = bindEx.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .filter(msg -> msg != null && !msg.isBlank())
                    .distinct()
                    .collect(Collectors.joining("; "));
            return ResponseEntity.badRequest().body(new MessageResponseDTO(!message.isEmpty() ? message : "Dữ liệu đầu vào không hợp lệ."));
        }
        return ResponseEntity.badRequest().body(new MessageResponseDTO("Dữ liệu đầu vào không hợp lệ."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponseDTO> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("IoT API key")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponseDTO(message));
        }
        return ResponseEntity.badRequest().body(new MessageResponseDTO(message != null ? message : "Yêu cầu không hợp lệ."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageResponseDTO> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.error("Database constraint violation: {}", ex.getMessage(), ex);
        return ResponseEntity.badRequest().body(new MessageResponseDTO(
                "Không thể thực hiện thao tác vì dữ liệu đang được liên kết với các bản ghi khác trong hệ thống."
        ));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<MessageResponseDTO> handleIOException(IOException ex) {
        logger.error("IO Exception: {}", ex.getMessage(), ex);
        return ResponseEntity.badRequest().body(new MessageResponseDTO("Lỗi xử lý tệp tin hoặc kết nối mạng."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<MessageResponseDTO> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest().body(new MessageResponseDTO("Kích thước tệp tin vượt quá dung lượng cho phép."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponseDTO> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        String message = ex.getMessage();
        // Ngăn chặn rò rỉ thông tin kỹ thuật SQL/DB ra ngoài giao diện
        if (message != null && (message.contains("could not execute") || message.contains("SQL") || message.contains("constraint") || message.contains("table") || message.contains("database"))) {
            return ResponseEntity.badRequest().body(new MessageResponseDTO("Thao tác dữ liệu không thành công. Vui lòng kiểm tra lại."));
        }
        return ResponseEntity.badRequest().body(new MessageResponseDTO(message != null ? message : "Đã có lỗi xảy ra."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponseDTO> handleGenericException(Exception ex) {
        logger.error("Unhandled server exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponseDTO(
                "Hệ thống đang bận hoặc gặp sự cố xử lý. Vui lòng thử lại sau."
        ));
    }
}
