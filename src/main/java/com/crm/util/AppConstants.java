package com.crm.util;

public final class AppConstants {
    private AppConstants() {}

    // Lead statuses
    public static final String LEAD_STATUS_NOT_CONTACTED = "NotContacted";
    public static final String LEAD_STATUS_CONTACTED     = "Contacted";
    public static final String LEAD_STATUS_QUALIFIED     = "Qualified Lead";
    public static final String LEAD_STATUS_WORKING       = "Working";
    public static final String LEAD_STATUS_QUOTATION     = "QuotationSent";
    public static final String LEAD_STATUS_NEGOTIATION   = "Negotiation";
    public static final String LEAD_STATUS_CONVERTED     = "Converted";

    // New simplified statuses
    public static final String LEAD_STATUS_NEW           = "New Lead";
    public static final String LEAD_STATUS_NEW_QUALIFIED = "Qualified";
    public static final String LEAD_STATUS_DISQUALIFIED  = "Disqualified";
    public static final String LEAD_STATUS_OPEN          = "Open";
    public static final String LEAD_STATUS_ONGOING       = "Ongoing";
    public static final String LEAD_STATUS_CLOSED        = "Closed";
    public static final String LEAD_STATUS_WON           = "Won";

    // Opportunity statuses
    public static final String OPP_STATUS_WON  = "Won";
    public static final String OPP_STATUS_LOST = "Lost";
    public static final String OPP_STATUS_OPEN = "Open";

    // Admin user ID
    public static final long ADMIN_USER_ID = 1L;

    // Indiamart
    public static final String INDIAMART_SOURCE = "IndiaMART";
    public static final String INDIAMART_DEFAULT_TYPE = "Hot";
    public static final String INDIAMART_DEFAULT_STATUS = "Qualified";
}
