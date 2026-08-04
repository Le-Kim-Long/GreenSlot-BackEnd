package swp490.greeenslot.service;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Instant;

public interface ReportExportService {
    
    StreamingResponseBody exportRentalsToCSV(Instant startDate, Instant endDate);
    
    StreamingResponseBody exportRentalsToExcel(Instant startDate, Instant endDate);
    
    StreamingResponseBody exportAlertsToCSV(Instant startDate, Instant endDate);
    
    StreamingResponseBody exportAlertsToExcel(Instant startDate, Instant endDate);
    
    StreamingResponseBody exportTasksToCSV(Instant startDate, Instant endDate);
    
    StreamingResponseBody exportTasksToExcel(Instant startDate, Instant endDate);
}
