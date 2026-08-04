package swp490.greeenslot.service;

public interface RentalExpirationService {
    void checkAndNotifyExpiringRentals();
    void checkAndNotifyExpiredRentals();
}
