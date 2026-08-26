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

    @Autowired
    private swp490.greeenslot.repository.TreePlantingRequestRepository treePlantingRequestRepository;

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
                contentStream.showText("Rental & Cultivation Details:");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Slot Number: " + (rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A"));
                
                String targetPillarText = null;
                String targetTreeText = null;
                if (payment != null && payment.getVnpTxnRef() != null && payment.getVnpTxnRef().startsWith("PLANT_")) {
                    try {
                        String[] parts = payment.getVnpTxnRef().split("_");
                        if (parts.length >= 2) {
                            Long reqId = Long.parseLong(parts[1]);
                            var reqOpt = treePlantingRequestRepository.findById(reqId);
                            if (reqOpt.isPresent()) {
                                var req = reqOpt.get();
                                if (req.getTargetPillar() != null) {
                                    targetPillarText = "Trụ " + req.getTargetPillar().getPillarCode() + " (" + req.getTargetPillar().getEffectiveHoles() + " hốc)";
                                }
                                if (req.getNewTree() != null) {
                                    targetTreeText = req.getNewTree().getTreeName();
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (targetPillarText != null) {
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText("Cultivation Pillar: " + targetPillarText);
                    if (targetTreeText != null) {
                        contentStream.newLineAtOffset(0, -20);
                        contentStream.showText("Plant Variety: " + targetTreeText);
                    }
                } else {
                    java.util.List<swp490.greeenslot.entity.Pillar> rentedPillars = rental.getRentedPillars() != null && !rental.getRentedPillars().isEmpty()
                            ? rental.getRentedPillars()
                            : (rental.getGardenSlot() != null && rental.getGardenSlot().getPillars() != null ? rental.getGardenSlot().getPillars() : (rental.getGardenSlot() != null && rental.getGardenSlot().getPillar() != null ? java.util.List.of(rental.getGardenSlot().getPillar()) : java.util.List.of()));
                    
                    if (!rentedPillars.isEmpty()) {
                        for (swp490.greeenslot.entity.Pillar p : rentedPillars) {
                            swp490.greeenslot.entity.Tree pTree = p.getDefaultTree() != null ? p.getDefaultTree() : rental.getTree();
                            String cropName = pTree != null ? pTree.getTreeName() : "N/A";
                            contentStream.newLineAtOffset(0, -18);
                            contentStream.showText("Pillar " + p.getPillarCode() + " (" + p.getEffectiveHoles() + " holes) - Crop: " + cropName);
                        }
                    } else if (rental.getTree() != null) {
                        contentStream.newLineAtOffset(0, -20);
                        contentStream.showText("Plant Variety: " + rental.getTree().getTreeName());
                    }
                }

                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Location: " + (rental.getGardenSlot() != null && rental.getGardenSlot().getLocation() != null
                        ? rental.getGardenSlot().getLocation().getName()
                        : (rental.getGardenSlot() != null && rental.getGardenSlot().getPillar() != null && rental.getGardenSlot().getPillar().getLocation() != null
                            ? rental.getGardenSlot().getPillar().getLocation().getName() : "N/A")));
                if (rental.getStartTime() != null && rental.getEndTime() != null) {
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText("Rental Duration: " + rental.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) 
                            + " to " + rental.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                }
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(50, 480);
                contentStream.showText("Payment Breakdown:");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Total Paid: " + (payment != null ? String.format("%,d VNĐ", payment.getAmount().longValue()) : "0 VNĐ"));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Payment Status: " + (payment != null ? payment.getStatus().name() : rental.getStatus().name()));
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
