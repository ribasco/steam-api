package nl.pvanassen.steam.store.login;

import nl.pvanassen.steam.http.DefaultHandle;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

class DoLoginHandle extends DefaultHandle {

    private final ObjectMapper objectMapper;

    private boolean success;

    private String message;

    private boolean emailAuthNeeded;

    private String emailDomain;

    private String emailSteamId;

    private boolean captchaNeeded;

    private String captchaGid;

    DoLoginHandle(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String getCaptchaGid() {
        return captchaGid;
    }

    String getEmailDomain() {
        return emailDomain;
    }

    String getEmailSteamId() {
        return emailSteamId;
    }

    String getMessage() {
        return message;
    }

    @Override
    public void handle(InputStream stream) throws IOException {
        JsonNode node = objectMapper.readTree(stream);
        success = node.get("success").asBoolean();
        if (node.get("message") != null) {
            message = node.get("message").asText();
        }
        if (node.get("captcha_needed") != null) {
            captchaNeeded = node.get("captcha_needed").asBoolean();
            captchaGid = node.get("captcha_gid").asText();
        }
        if (node.get("emailauth_needed") != null) {
            emailAuthNeeded = node.get("emailauth_needed").asBoolean();
            emailDomain = node.get("emaildomain").asText();
            emailSteamId = node.get("emailsteamid").asText();
        }
    }

    boolean isCaptchaNeeded() {
        return captchaNeeded;
    }

    boolean isEmailAuthNeeded() {
        return emailAuthNeeded;
    }

    boolean isSuccess() {
        return success;
    }
}
