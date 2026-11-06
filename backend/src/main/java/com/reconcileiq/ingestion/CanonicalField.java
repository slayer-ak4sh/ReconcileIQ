package com.reconcileiq.ingestion;

public enum CanonicalField {
    // Shared
    ORDER_ID("orderId", "Order ID"),
    CHANNEL_CODE("channelCode", "Channel"),
    ORDER_DATE("orderDate", "Order Date"),
    ORDER_VALUE("orderValue", "Order Value"),
    PAYMENT_TYPE("paymentType", "Payment Type"),
    DELIVERY_STATUS("deliveryStatus", "Delivery Status"),
    CUSTOMER_NAME("customerName", "Customer Name"),
    CUSTOMER_PHONE("customerPhone", "Customer Phone"),
    CUSTOMER_ADDRESS("customerAddress", "Customer Address"),

    // Settlement
    REFERENCE_NO("referenceNo", "Reference No"),
    SETTLEMENT_DATE("settlementDate", "Settlement Date"),
    GROSS_AMOUNT("grossAmount", "Gross Amount"),
    COMMISSION("commission", "Commission"),
    FIXED_FEE("fixedFee", "Fixed Fee"),
    COLLECTION_FEE("collectionFee", "Collection Fee"),
    TCS_AMOUNT("tcsAmount", "TCS Amount"),
    TDS_AMOUNT("tdsAmount", "TDS Amount"),
    NET_AMOUNT("netAmount", "Net Amount"),

    // COD remittance
    COURIER_CODE("courierCode", "Courier"),
    UTR_NO("utrNo", "UTR / Transaction ID"),
    REMITTANCE_DATE("remittanceDate", "Remittance Date"),
    COD_COLLECTED("codCollected", "COD Collected"),
    FEE_DEDUCTED("feeDeducted", "Fee Deducted"),
    NET_REMITTED("netRemitted", "Net Remitted"),

    // Freight invoice
    INVOICE_NO("invoiceNo", "Invoice No"),
    INVOICE_DATE("invoiceDate", "Invoice Date"),
    BILLED_WEIGHT("billedWeight", "Billed Weight (kg)"),
    FREIGHT_CHARGE("freightCharge", "Freight Charge"),
    COD_FEE("codFee", "COD Fee"),
    RETURN_FEE("returnFee", "Return Fee"),
    TOTAL_FREIGHT("totalFreight", "Total Freight"),

    // Warehouse scan
    WAYBILL_NO("waybillNo", "Waybill No"),
    SCAN_DATE("scanDate", "Scan Date"),
    WAREHOUSE_LOCATION("warehouseLocation", "Warehouse Location"),
    CONDITION_STATUS("conditionStatus", "Condition Status");

    public final String key;
    public final String label;

    CanonicalField(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public static CanonicalField fromKey(String key) {
        for (CanonicalField f : values()) {
            if (f.key.equalsIgnoreCase(key)) return f;
        }
        return null;
    }
}