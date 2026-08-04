package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import swp490.greeenslot.service.ReportExportService;

import java.time.Instant;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Report Export", description = "APIs for exporting reports in CSV and Excel formats")
public class ReportExportController {

    @Autowired
    private ReportExportService reportExportService;

    @GetMapping("/rentals/csv")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Export rentals to CSV", description = "Exports rental data to CSV format for a given date range")
    public ResponseEntity<StreamingResponseBody> exportRentalsToCSV(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {
        
        StreamingResponseBody stream = reportExportService.exportRentalsToCSV(startDate, endDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "rentals_" + System.currentTimeMillis() + ".csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(stream);
    }

    @GetMapping("/rentals/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Export rentals to Excel", description = "Exports rental data to Excel format for a given date range")
    public ResponseEntity<StreamingResponseBody> exportRentalsToExcel(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {
        
        StreamingResponseBody stream = reportExportService.exportRentalsToExcel(startDate, endDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "rentals_" + System.currentTimeMillis() + ".xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(stream);
    }

    @GetMapping("/alerts/csv")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Export alerts to CSV", description = "Exports alert data to CSV format for a given date range")
    public ResponseEntity<StreamingResponseBody> exportAlertsToCSV(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {
        
        StreamingResponseBody stream = reportExportService.exportAlertsToCSV(startDate, endDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "alerts_" + System.currentTimeMillis() + ".csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(stream);
    }

    @GetMapping("/alerts/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Export alerts to Excel", description = "Exports alert data to Excel format for a given date range")
    public ResponseEntity<StreamingResponseBody> exportAlertsToExcel(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {
        
        StreamingResponseBody stream = reportExportService.exportAlertsToExcel(startDate, endDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "alerts_" + System.currentTimeMillis() + ".xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(stream);
    }

    @GetMapping("/tasks/csv")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Export tasks to CSV", description = "Exports task data to CSV format for a given date range")
    public ResponseEntity<StreamingResponseBody> exportTasksToCSV(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {
        
        StreamingResponseBody stream = reportExportService.exportTasksToCSV(startDate, endDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "tasks_" + System.currentTimeMillis() + ".csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(stream);
    }

    @GetMapping("/tasks/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Export tasks to Excel", description = "Exports task data to Excel format for a given date range")
    public ResponseEntity<StreamingResponseBody> exportTasksToExcel(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {
        
        StreamingResponseBody stream = reportExportService.exportTasksToExcel(startDate, endDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "tasks_" + System.currentTimeMillis() + ".xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(stream);
    }
}
