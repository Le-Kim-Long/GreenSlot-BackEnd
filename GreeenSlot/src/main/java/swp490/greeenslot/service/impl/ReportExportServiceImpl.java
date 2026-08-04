package swp490.greeenslot.service.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import swp490.greeenslot.entity.Alert;
import swp490.greeenslot.entity.GardeningTask;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.repository.AlertRepository;
import swp490.greeenslot.repository.GardeningTaskRepository;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.service.ReportExportService;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportExportServiceImpl implements ReportExportService {

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private GardeningTaskRepository gardeningTaskRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public StreamingResponseBody exportRentalsToCSV(Instant startDate, Instant endDate) {
        return outputStream -> {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                // Write CSV header
                writer.println("ID,Customer,Slot Number,Start Time,End Time,Status,Tree Status");
                
                // Fetch rentals in date range
                LocalDateTime start = LocalDateTime.ofInstant(startDate, java.time.ZoneId.systemDefault());
                LocalDateTime end = LocalDateTime.ofInstant(endDate, java.time.ZoneId.systemDefault());
                
                List<SlotRental> rentals = slotRentalRepository.findAll().stream()
                        .filter(r -> r.getStartTime().isAfter(start) && r.getStartTime().isBefore(end))
                        .toList();
                
                // Write rental data
                for (SlotRental rental : rentals) {
                    writer.println(String.format("%d,%s,%s,%s,%s,%s,%s",
                            rental.getId(),
                            rental.getUser() != null ? rental.getUser().getFullName() : "N/A",
                            rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A",
                            rental.getStartTime().format(DATE_FORMATTER),
                            rental.getEndTime().format(DATE_FORMATTER),
                            rental.getStatus().name(),
                            rental.getTreeStatus() != null ? rental.getTreeStatus().name() : "N/A"
                    ));
                }
            }
        };
    }

    @Override
    public StreamingResponseBody exportRentalsToExcel(Instant startDate, Instant endDate) {
        return outputStream -> {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Rentals");
                
                // Create header row
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("ID");
                headerRow.createCell(1).setCellValue("Customer");
                headerRow.createCell(2).setCellValue("Slot Number");
                headerRow.createCell(3).setCellValue("Start Time");
                headerRow.createCell(4).setCellValue("End Time");
                headerRow.createCell(5).setCellValue("Status");
                headerRow.createCell(6).setCellValue("Tree Status");
                
                // Fetch rentals in date range
                LocalDateTime start = LocalDateTime.ofInstant(startDate, java.time.ZoneId.systemDefault());
                LocalDateTime end = LocalDateTime.ofInstant(endDate, java.time.ZoneId.systemDefault());
                
                List<SlotRental> rentals = slotRentalRepository.findAll().stream()
                        .filter(r -> r.getStartTime().isAfter(start) && r.getStartTime().isBefore(end))
                        .toList();
                
                // Write rental data
                int rowNum = 1;
                for (SlotRental rental : rentals) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(rental.getId());
                    row.createCell(1).setCellValue(rental.getUser() != null ? rental.getUser().getFullName() : "N/A");
                    row.createCell(2).setCellValue(rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A");
                    row.createCell(3).setCellValue(rental.getStartTime().format(DATE_FORMATTER));
                    row.createCell(4).setCellValue(rental.getEndTime().format(DATE_FORMATTER));
                    row.createCell(5).setCellValue(rental.getStatus().name());
                    row.createCell(6).setCellValue(rental.getTreeStatus() != null ? rental.getTreeStatus().name() : "N/A");
                }
                
                workbook.write(outputStream);
            }
        };
    }

    @Override
    public StreamingResponseBody exportAlertsToCSV(Instant startDate, Instant endDate) {
        return outputStream -> {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                writer.println("ID,Alert Type,Description,Status,Sensor Type,Actual Value,Threshold Value,Created At");
                
                LocalDateTime start = LocalDateTime.ofInstant(startDate, java.time.ZoneId.systemDefault());
                LocalDateTime end = LocalDateTime.ofInstant(endDate, java.time.ZoneId.systemDefault());
                
                List<Alert> alerts = alertRepository.findAll().stream()
                        .filter(a -> a.getCreatedAt().isAfter(start) && a.getCreatedAt().isBefore(end))
                        .toList();
                
                for (Alert alert : alerts) {
                    writer.println(String.format("%d,%s,%s,%s,%s,%.2f,%.2f,%s",
                            alert.getId(),
                            alert.getAlertType(),
                            alert.getDescription().replace(",", ";"),
                            alert.getStatus().name(),
                            alert.getSensorType(),
                            alert.getActualValue(),
                            alert.getThresholdValue(),
                            alert.getCreatedAt().format(DATE_FORMATTER)
                    ));
                }
            }
        };
    }

    @Override
    public StreamingResponseBody exportAlertsToExcel(Instant startDate, Instant endDate) {
        return outputStream -> {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Alerts");
                
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("ID");
                headerRow.createCell(1).setCellValue("Alert Type");
                headerRow.createCell(2).setCellValue("Description");
                headerRow.createCell(3).setCellValue("Status");
                headerRow.createCell(4).setCellValue("Sensor Type");
                headerRow.createCell(5).setCellValue("Actual Value");
                headerRow.createCell(6).setCellValue("Threshold Value");
                headerRow.createCell(7).setCellValue("Created At");
                
                LocalDateTime start = LocalDateTime.ofInstant(startDate, java.time.ZoneId.systemDefault());
                LocalDateTime end = LocalDateTime.ofInstant(endDate, java.time.ZoneId.systemDefault());
                
                List<Alert> alerts = alertRepository.findAll().stream()
                        .filter(a -> a.getCreatedAt().isAfter(start) && a.getCreatedAt().isBefore(end))
                        .toList();
                
                int rowNum = 1;
                for (Alert alert : alerts) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(alert.getId());
                    row.createCell(1).setCellValue(alert.getAlertType());
                    row.createCell(2).setCellValue(alert.getDescription());
                    row.createCell(3).setCellValue(alert.getStatus().name());
                    row.createCell(4).setCellValue(alert.getSensorType());
                    row.createCell(5).setCellValue(alert.getActualValue());
                    row.createCell(6).setCellValue(alert.getThresholdValue());
                    row.createCell(7).setCellValue(alert.getCreatedAt().format(DATE_FORMATTER));
                }
                
                workbook.write(outputStream);
            }
        };
    }

    @Override
    public StreamingResponseBody exportTasksToCSV(Instant startDate, Instant endDate) {
        return outputStream -> {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                writer.println("ID,Task Name,Description,Status,Task Type,Assigned Staff,Created At");
                
                LocalDateTime start = LocalDateTime.ofInstant(startDate, java.time.ZoneId.systemDefault());
                LocalDateTime end = LocalDateTime.ofInstant(endDate, java.time.ZoneId.systemDefault());
                
                List<GardeningTask> tasks = gardeningTaskRepository.findAll().stream()
                        .filter(t -> t.getCreatedAt().isAfter(start) && t.getCreatedAt().isBefore(end))
                        .toList();
                
                for (GardeningTask task : tasks) {
                    writer.println(String.format("%d,%s,%s,%s,%s,%s,%s",
                            task.getId(),
                            task.getTaskName(),
                            task.getDescription().replace(",", ";"),
                            task.getStatus().name(),
                            task.getTaskType().name(),
                            task.getAssignedStaff() != null ? task.getAssignedStaff().getFullName() : "Unassigned",
                            task.getCreatedAt().format(DATE_FORMATTER)
                    ));
                }
            }
        };
    }

    @Override
    public StreamingResponseBody exportTasksToExcel(Instant startDate, Instant endDate) {
        return outputStream -> {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Tasks");
                
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("ID");
                headerRow.createCell(1).setCellValue("Task Name");
                headerRow.createCell(2).setCellValue("Description");
                headerRow.createCell(3).setCellValue("Status");
                headerRow.createCell(4).setCellValue("Task Type");
                headerRow.createCell(5).setCellValue("Assigned Staff");
                headerRow.createCell(6).setCellValue("Created At");
                
                LocalDateTime start = LocalDateTime.ofInstant(startDate, java.time.ZoneId.systemDefault());
                LocalDateTime end = LocalDateTime.ofInstant(endDate, java.time.ZoneId.systemDefault());
                
                List<GardeningTask> tasks = gardeningTaskRepository.findAll().stream()
                        .filter(t -> t.getCreatedAt().isAfter(start) && t.getCreatedAt().isBefore(end))
                        .toList();
                
                int rowNum = 1;
                for (GardeningTask task : tasks) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(task.getId());
                    row.createCell(1).setCellValue(task.getTaskName());
                    row.createCell(2).setCellValue(task.getDescription());
                    row.createCell(3).setCellValue(task.getStatus().name());
                    row.createCell(4).setCellValue(task.getTaskType().name());
                    row.createCell(5).setCellValue(task.getAssignedStaff() != null ? task.getAssignedStaff().getFullName() : "Unassigned");
                    row.createCell(6).setCellValue(task.getCreatedAt().format(DATE_FORMATTER));
                }
                
                workbook.write(outputStream);
            }
        };
    }
}
