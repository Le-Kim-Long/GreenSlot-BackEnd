package swp490.greeenslot.config; // Đổi lại package cho đúng project của bạn

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@Component
public class UdpDiscoveryServer {

    // Tự động lấy cổng của Spring Boot từ application.properties (mặc định 8080)
    @Value("${server.port:8080}")
    private int serverPort;

    private final int UDP_PORT = 8888; // Cổng UDP quy ước để ESP32 gọi tới

    @EventListener(ApplicationReadyEvent.class)
    public void startUdpListener() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
                socket.setBroadcast(true);
                System.out.println("==================================================");
                System.out.println("[UDP DISCOVERY] Đang lắng nghe ESP32 tại cổng UDP: " + UDP_PORT);
                System.out.println("==================================================");

                byte[] receiveBuffer = new byte[1024];
                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket); // Chờ tín hiệu hét lên từ ESP32

                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

                    // Nếu đúng mật lệnh từ camera GreenSlot
                    if ("DISCOVER_GREENSLOT_SERVER".equals(message)) {
                        InetAddress espIp = receivePacket.getAddress();
                        int espPort = receivePacket.getPort();

                        // Trả lời lại: "GREENSLOT_SERVER:<Cổng_Web>"
                        String replyMessage = "GREENSLOT_SERVER:" + serverPort;
                        byte[] sendData = replyMessage.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, espIp, espPort);
                        socket.send(sendPacket);

                        System.out.println("[UDP DISCOVERY] Đã chỉ đường cho ESP32 (" + espIp.getHostAddress() + ") -> Server Port: " + serverPort);
                    }
                }
            } catch (Exception e) {
                System.err.println("[UDP DISCOVERY] Lỗi: " + e.getMessage());
            }
        }).start();
    }
}