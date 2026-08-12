package swp490.greeenslot.service.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp490.greeenslot.entity.PaymentTransaction;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.repository.PaymentTransactionRepository;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.service.InvoiceService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Override
    public ByteArrayOutputStream generateInvoice(Long rentalId) throws IOException {
        SlotRental rental = slotRentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found with ID: " + rentalId));

        PaymentTransaction latestPayment = paymentTransactionRepository
                .findByRentalIdOrderByPaymentDateDesc(rentalId)
                .stream()
                .findFirst()
                .orElse(null);

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
        if (!isAdmin && rental.getUser() != null && !rental.getUser().getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("You do not have permission to access this invoice.");
        }

        return generatePdfInvoice(rental, latestPayment);
    }

    @Override
    public ByteArrayOutputStream generateInvoiceForPayment(Long paymentTransactionId) throws IOException {
        PaymentTransaction payment = paymentTransactionRepository.findById(paymentTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found with ID: " + paymentTransactionId));
        
        SlotRental rental = payment.getRental();
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
        if (!isAdmin && rental != null && rental.getUser() != null && !rental.getUser().getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("You do not have permission to access this invoice.");
        }

        return generatePdfInvoice(rental, payment);
    }

    private ByteArrayOutputStream generatePdfInvoice(SlotRental rental, PaymentTransaction payment) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("GREEN SLOT INVOICE");
                contentStream.endText();

                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 730);
                contentStream.showText("Invoice ID: " + (payment != null ? payment.getVnpTxnRef() : "INV-" + rental.getId()));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Date: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(50, 680);
                contentStream.showText("Customer Information:");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Name: " + (rental.getUser() != null ? rental.getUser().getFullName() : "N/A"));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Email: " + (rental.getUser() != null ? rental.getUser().getEmail() : "N/A"));
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(50, 620);
                contentStream.showText("Rental Details:");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Slot Number: " + (rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A"));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Location: " + (rental.getGardenSlot() != null && rental.getGardenSlot().getPillar() != null 
                        && rental.getGardenSlot().getPillar().getLocation() != null 
                        ? rental.getGardenSlot().getPillar().getLocation().getName() : "N/A"));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Start Date: " + rental.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("End Date: " + rental.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(50, 540);
                contentStream.showText("Payment Details:");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Amount: $" + (payment != null ? payment.getAmount().toString() : "0.00"));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Status: " + (payment != null ? payment.getStatus().name() : rental.getStatus().name()));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Transaction Ref: " + (payment != null ? payment.getVnpTxnRef() : "N/A"));
                contentStream.endText();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream;
        }
    }
}
