package com.softropic.promora.security.config;



import com.softropic.promora.security.exposed.util.AuthoritiesConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public final class AppEndpoints {

    public static final String                    SECURED           = "/v1/**";
    public static final String                    SECURED_API           = "/api/**";
    public static final String                    ACTUATOR           = "/manage/**";
    public static final String                    REFRESH           = "/refresh";
    public static final Map<String, String[]> SECURED_MAPPINGS;
    public static final List<String>          SECURED_ENDPOINTS; //"/api/register"
    public static final List<String> PUBLIC_STATIC_RESOURCES = List.of("/", "/assets/**", "/scripts/**", "/i18n/**", "/favicon.ico", "/icons/**", "/index.html");
    public static final List<String> PUBLIC_ENDPOINTS = List.of("/v1/account/register**", "/v1/account/activate/**",
                                                                "/v1/account/reset_password/init", "/v1/account/reset_password/finish",
                                                                "/api/v1/search/schedules", "/api/v1/reservations/**", "/api/v1/emails/**",
                                                                "/api/v1/cities/**", "/api/v1/payments/payment", "/api/v1/tickets/eticket/**", "/api/v1/bookings/**");
    public static final List<String> ALL_UNRESTRICTED;

    private static final String[] SECURED_AUTHORITIES = new String[]{AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER, AuthoritiesConstants.LTD_ADMIN};


    static {

        SECURED_MAPPINGS = Map.of(SECURED, Arrays.copyOf(SECURED_AUTHORITIES, SECURED_AUTHORITIES.length),
                                  SECURED_API, Arrays.copyOf(SECURED_AUTHORITIES, SECURED_AUTHORITIES.length),
                                  ACTUATOR, new String[]{AuthoritiesConstants.ADMIN},
                                  REFRESH, Arrays.copyOf(SECURED_AUTHORITIES, SECURED_AUTHORITIES.length));
        SECURED_ENDPOINTS = List.copyOf(SECURED_MAPPINGS.keySet());
        ALL_UNRESTRICTED = new ArrayList<>(PUBLIC_STATIC_RESOURCES);
        ALL_UNRESTRICTED.addAll(PUBLIC_ENDPOINTS);
    }
    private AppEndpoints(){}

}
