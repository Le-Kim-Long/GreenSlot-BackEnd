package swp490.greeenslot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class GreeenSlotApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreeenSlotApplication.class, args);
    }

}
