package com.whatsapp.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whatsapp.util.EncryptionUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class EncryptionFilter implements Filter {

    @Value("${app.encryption.secret:}")
    private String encryptionSecret;

    @Value("${app.encryption.enabled:false}")
    private boolean encryptionEnabled;

    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        // Only process /api/ paths
        if (!httpReq.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // Skip GET/DELETE and non-JSON
        String method = httpReq.getMethod();
        String contentType = httpReq.getContentType();
        boolean hasEncryptedBody = "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
        hasEncryptedBody = hasEncryptedBody && contentType != null && contentType.contains("application/json");

        HttpServletRequest processedRequest = httpReq;
        if (hasEncryptedBody) {
            processedRequest = decryptRequest(httpReq);
            if (processedRequest == null) {
                httpRes.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid encrypted payload");
                return;
            }
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpRes);
        chain.doFilter(processedRequest, responseWrapper);
        encryptResponse(responseWrapper, httpRes);
    }

    private HttpServletRequest decryptRequest(HttpServletRequest request) {
        try {
            byte[] body = request.getInputStream().readAllBytes();
            if (body.length == 0) return request;

            JsonNode root = objectMapper.readTree(body);
            JsonNode payloadNode = root.get("encryptedPayload");
            if (payloadNode == null) return request; // not encrypted, pass through

            String signature = request.getHeader("X-Signature");
            String decrypted = EncryptionUtil.decrypt(payloadNode.asText(), encryptionSecret);

            if (StringUtils.hasText(signature)) {
                if (!EncryptionUtil.verifyHmac(decrypted, signature, encryptionSecret)) {
                    log.warn("[ENC-FILTER] HMAC verification failed for {}", request.getRequestURI());
                    return null;
                }
            }

            byte[] decryptedBytes = decrypted.getBytes(StandardCharsets.UTF_8);
            return new BodyReplacedRequest(request, decryptedBytes);
        } catch (Exception e) {
            log.error("[ENC-FILTER] Request decryption failed: {}", e.getMessage());
            return null;
        }
    }

    private void encryptResponse(ContentCachingResponseWrapper wrapper, HttpServletResponse original)
            throws IOException {
        byte[] responseBody = wrapper.getContentAsByteArray();
        if (responseBody.length == 0) {
            wrapper.copyBodyToResponse();
            return;
        }

        String contentType = wrapper.getContentType();
        if (contentType == null || !contentType.contains("application/json")) {
            wrapper.copyBodyToResponse();
            return;
        }

        try {
            String encrypted = EncryptionUtil.encrypt(new String(responseBody, StandardCharsets.UTF_8), encryptionSecret);
            ObjectNode encNode = objectMapper.createObjectNode();
            encNode.put("encryptedPayload", encrypted);
            byte[] encBytes = objectMapper.writeValueAsBytes(encNode);

            original.setContentType("application/json");
            original.setContentLength(encBytes.length);
            original.getOutputStream().write(encBytes);
        } catch (Exception e) {
            log.error("[ENC-FILTER] Response encryption failed: {}", e.getMessage());
            wrapper.copyBodyToResponse();
        }
    }

    private boolean isEnabled() {
        return encryptionEnabled && StringUtils.hasText(encryptionSecret);
    }

    // Wrapper to replace request body
    private static class BodyReplacedRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        BodyReplacedRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                public int read() { return bais.read(); }
                public boolean isFinished() { return bais.available() == 0; }
                public boolean isReady() { return true; }
                public void setReadListener(ReadListener l) {}
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() { return body.length; }

        @Override
        public long getContentLengthLong() { return body.length; }
    }
}
