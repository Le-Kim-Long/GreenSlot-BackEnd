package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.service.InvoiceService;

import java.io.ByteArrayOutputStream;

@CrossOrigin(origins = {"https://greenslot-taupe.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoice Management", description = "APIs for generating PDF invoices")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/rental/{rentalId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate invoice for rental", description = "Generates a PDF invoice for a specific rental")
    public ResponseEntity<byte[]> generateInvoiceForRental(@PathVariable Long rentalId) {
        try {
            ByteArrayOutputStream pdf = invoiceService.generateInvoice(rentalId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "invoice_rental_" + rentalId + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdf.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/payment/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate invoice for payment", description = "Generates a PDF invoice for a specific payment transaction")
    public ResponseEntity<byte[]> generateInvoiceForPayment(@PathVariable Long paymentId) {
        try {
            ByteArrayOutputStream pdf = invoiceService.generateInvoiceForPayment(paymentId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "invoice_payment_" + paymentId + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdf.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
