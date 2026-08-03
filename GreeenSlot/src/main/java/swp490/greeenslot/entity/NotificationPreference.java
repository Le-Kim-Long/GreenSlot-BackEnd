package swp490.greeenslot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    private Boolean smsEnabled = false;

    @Column(name = "iot_alerts_enabled", nullable = false)
    private Boolean iotAlertsEnabled = true;

    @Column(name = "task_assignment_enabled", nullable = false)
    private Boolean taskAssignmentEnabled = true;

    @Column(name = "payment_alerts_enabled", nullable = false)
    private Boolean paymentAlertsEnabled = true;

    @Column(name = "rental_expiration_enabled", nullable = false)
    private Boolean rentalExpirationEnabled = true;

    @Column(name = "marketing_enabled", nullable = false)
    private Boolean marketingEnabled = false;
}
