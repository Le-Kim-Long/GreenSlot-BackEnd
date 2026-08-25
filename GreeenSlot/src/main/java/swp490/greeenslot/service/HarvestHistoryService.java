package swp490.greeenslot.service;

import swp490.greeenslot.entity.HarvestHistory;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.User;

import java.util.List;

public interface HarvestHistoryService {

    /**
     * Snapshots the rental's current tree/planting state into a permanent history row.
     * Call this BEFORE the caller clears the rental's tree fields.
     *
     * @param method "SELF" (customer harvested it themselves) or "STAFF" (staff completed the task)
     * @param staff  the staff who completed it (null when method is SELF)
     */
    void recordHarvest(SlotRental rental, String method, User staff);

    void recordHarvest(SlotRental rental, String method, User staff, String pillarCodes);

    List<HarvestHistory> getMyHistory(String username);

    List<HarvestHistory> getHistoryForManager(String username);
}
