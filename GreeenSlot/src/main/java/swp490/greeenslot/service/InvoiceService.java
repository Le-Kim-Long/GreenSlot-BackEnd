package swp490.greeenslot.service;

import java.io.ByteArrayOutputStream;

public interface InvoiceService {
    
    ByteArrayOutputStream generateInvoice(Long rentalId) throws Exception;
    
    ByteArrayOutputStream generateInvoiceForPayment(Long paymentTransactionId) throws Exception;
}
