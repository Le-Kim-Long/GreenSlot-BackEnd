package swp490.greeenslot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CameraPingRequestDTO {

    @JsonProperty("cam_id")
    private String camId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("ip")
    private String ip;

    @JsonProperty("stream_url")
    private String streamUrl;

    @JsonProperty("capture_url")
    private String captureUrl;

    // --- CONSTRUCTOR MẶC ĐỊNH ---
    public CameraPingRequestDTO() {
    }

    // --- CONSTRUCTOR ĐẦY ĐỦ THAM SỐ ---
    public CameraPingRequestDTO(String camId, String name, String ip, String streamUrl, String captureUrl) {
        this.camId = camId;
        this.name = name;
        this.ip = ip;
        this.streamUrl = streamUrl;
        this.captureUrl = captureUrl;
    }

    // --- GETTER VÀ SETTER CHO TẤT CẢ CÁC BIẾN ---

    public String getCamId() {
        return camId;
    }

    public void setCamId(String camId) {
        this.camId = camId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getCaptureUrl() {
        return captureUrl;
    }

    public void setCaptureUrl(String captureUrl) {
        this.captureUrl = captureUrl;
    }
}