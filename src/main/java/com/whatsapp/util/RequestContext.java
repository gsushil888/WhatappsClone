package com.whatsapp.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

public class RequestContext {
    
    private static final ThreadLocal<RequestInfo> contextHolder = new ThreadLocal<>();
    
    public static void setContext(RequestInfo requestInfo) {
        contextHolder.set(requestInfo);
    }
    
    public static RequestInfo getContext() {
        return contextHolder.get();
    }
    
    public static String getCorrelationId() {
        RequestInfo context = getContext();
        return context != null ? context.getCorrelationId() : generateCorrelationId();
    }
    
    public static Long getUserId() {
        RequestInfo context = getContext();
        return context != null ? context.getUserId() : null;
    }
    
    public static String getSessionId() {
        RequestInfo context = getContext();
        return context != null ? context.getSessionId() : null;
    }
    
    public static Map<String, String> getDeviceInfo() {
        RequestInfo context = getContext();
        return context != null ? context.getDeviceInfo() : null;
    }
    
    public static Map<String, String> getHeaders() {
        RequestInfo context = getContext();
        return context != null ? context.getHeaders() : null;
    }
    
    public static String getDeviceFingerprint() {
        RequestInfo context = getContext();
        
        // First check headers for device fingerprint
        if (context != null && context.getHeaders() != null) {
            String headerFingerprint = context.getHeaders().get("x_device_fingerprint");
            if (headerFingerprint != null && !headerFingerprint.trim().isEmpty()) {
                return headerFingerprint;
            }
        }
        
        // Check device info for fingerprint
        if (context != null && context.getDeviceInfo() != null) {
            String fingerprint = context.getDeviceInfo().get("device_fingerprint");
            if (fingerprint != null && !fingerprint.trim().isEmpty()) {
                return fingerprint;
            }
        }
        
        // Generate fallback fingerprint from available info
        return generateFallbackFingerprint();
    }
    
    public static String getIpAddress() {
        RequestInfo context = getContext();
        return context != null ? context.getIpAddress() : "unknown";
    }
    
    public static String getUserAgent() {
        RequestInfo context = getContext();
        return context != null ? context.getUserAgent() : "unknown";
    }
    
    public static String getDeviceType() {
        RequestInfo context = getContext();
        if (context != null && context.getDeviceInfo() != null) {
            String deviceType = context.getDeviceInfo().get("device_type");
            if (deviceType != null) return deviceType;
        }
        return extractDeviceTypeFromUserAgent(getUserAgent());
    }
    
    public static String getDeviceOs() {
        RequestInfo context = getContext();
        if (context != null && context.getDeviceInfo() != null) {
            String deviceOs = context.getDeviceInfo().get("device_os");
            if (deviceOs != null) return deviceOs;
        }
        return extractOsFromUserAgent(getUserAgent());
    }
    
    public static String getDeviceBrowser() {
        RequestInfo context = getContext();
        if (context != null && context.getDeviceInfo() != null) {
            String deviceBrowser = context.getDeviceInfo().get("device_browser");
            if (deviceBrowser != null) return deviceBrowser;
        }
        return extractBrowserFromUserAgent(getUserAgent());
    }
    
    public static String getDeviceModel() {
        RequestInfo context = getContext();
        if (context != null && context.getDeviceInfo() != null) {
            String deviceModel = context.getDeviceInfo().get("device_model");
            if (deviceModel != null) return deviceModel;
        }
        return "Unknown";
    }
    
    private static String extractDeviceTypeFromUserAgent(String userAgent) {
        if (userAgent == null) return "WEB";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "MOBILE";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "TABLET";
        } else if (ua.contains("electron") || ua.contains("desktop")) {
            return "DESKTOP";
        }
        return "WEB";
    }
    
    private static String extractBrowserFromUserAgent(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("chrome")) return "Chrome";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("safari")) return "Safari";
        if (ua.contains("edge")) return "Edge";
        return "Unknown";
    }
    
    private static String extractOsFromUserAgent(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac")) return "macOS";
        if (ua.contains("linux")) return "Linux";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        return "Unknown";
    }
    
    public static void setRequestInfo(Map<String, String> requestInfo) {
        RequestInfo context = getContext();
        if (context != null) {
            // Update existing context with request info
            if (requestInfo.containsKey("deviceType")) {
                context.getDeviceInfo().put("device_type", requestInfo.get("deviceType"));
            }
            if (requestInfo.containsKey("os")) {
                context.getDeviceInfo().put("device_os", requestInfo.get("os"));
            }
            if (requestInfo.containsKey("osVersion")) {
                context.getDeviceInfo().put("device_os_version", requestInfo.get("osVersion"));
            }
            if (requestInfo.containsKey("appVersion")) {
                context.getDeviceInfo().put("app_version", requestInfo.get("appVersion"));
            }
            if (requestInfo.containsKey("deviceModel")) {
                context.getDeviceInfo().put("device_model", requestInfo.get("deviceModel"));
            }
            if (requestInfo.containsKey("deviceFingerprint")) {
                context.getDeviceInfo().put("device_fingerprint", requestInfo.get("deviceFingerprint"));
            }
        }
    }
    
    public static void clear() {
        contextHolder.remove();
    }
    
    public static String generateCorrelationId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    private static String generateFallbackFingerprint() {
        RequestInfo context = getContext();
        if (context == null) {
            return "fp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        
        StringBuilder fp = new StringBuilder();
        String userAgent = context.getUserAgent();
        String ipAddress = context.getIpAddress();
        
        if (userAgent != null && !"unknown".equals(userAgent)) {
            fp.append(userAgent.hashCode());
        }
        if (ipAddress != null && !"unknown".equals(ipAddress)) {
            fp.append("_").append(ipAddress.hashCode());
        }
        
        Map<String, String> deviceInfo = context.getDeviceInfo();
        if (deviceInfo != null) {
            deviceInfo.values().forEach(value -> {
                if (value != null) fp.append("_").append(value.hashCode());
            });
        }
        
        return fp.length() > 0 ? "fp_" + Math.abs(fp.toString().hashCode()) : 
               "fp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestInfo {
        private String correlationId;
        private Long userId;
        private String sessionId;
        private Map<String, String> deviceInfo;
        private Map<String, String> headers;
        private String ipAddress;
        private String userAgent;
        private long startTime;
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
    }
}