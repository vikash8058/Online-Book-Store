package com.bookstore.service;

import com.bookstore.exception.OrderNotFoundException;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.model.Payment;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.PaymentRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    // ── Fonts ──
    private static final Font FONT_TITLE =
            new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, new BaseColor(40, 53, 147));
    private static final Font FONT_HEADING =
            new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, new BaseColor(40, 53, 147));
    private static final Font FONT_SUBHEADING =
            new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
    private static final Font FONT_NORMAL =
            new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(60, 60, 60));
    private static final Font FONT_BOLD =
            new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(30, 30, 30));
    private static final Font FONT_SMALL =
            new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(100, 100, 100));
    private static final Font FONT_TOTAL =
            new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, new BaseColor(40, 53, 147));

    private static final BaseColor BRAND_COLOR   = new BaseColor(40, 53, 147);
    private static final BaseColor LIGHT_BLUE    = new BaseColor(232, 234, 246);
    private static final BaseColor TABLE_HEADER  = new BaseColor(63, 81, 181);
    private static final BaseColor ROW_ALT       = new BaseColor(245, 246, 255);

    /*
     * Main method — generates invoice PDF as byte array
     * Called by InvoiceController → returned as download response
     */
    public byte[] generateInvoice(Long orderId) throws Exception {

        // Fetch order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Fetch payment (may be null for old orders before UC13)
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, out);
        document.open();

        // ── Header 
        addHeader(document, order);

        // ── Order Info 
        addOrderInfo(document, order, payment);

        // ── Delivery Address 
        if (order.getDeliveryAddressLine() != null) {
            addDeliveryAddress(document, order);
        }

        // ── Items Table 
        addItemsTable(document, order);

        // ── Payment Summary 
        addPaymentSummary(document, order, payment);

        // ── Footer 
        addFooter(document);

        document.close();
        return out.toByteArray();
    }

    // ── Header Section 
    private void addHeader(Document document, Order order) throws DocumentException {

        // Brand name
        Paragraph brand = new Paragraph("📚 Book Wala", FONT_TITLE);
        brand.setAlignment(Element.ALIGN_LEFT);
        document.add(brand);

        Paragraph tagline = new Paragraph("  Your Online Book Store", FONT_SMALL);
        tagline.setAlignment(Element.ALIGN_LEFT);
        tagline.setSpacingAfter(4);
        document.add(tagline);

        // Horizontal line
        LineSeparator line = new LineSeparator(2, 100, BRAND_COLOR, Element.ALIGN_CENTER, -5);
        document.add(new Chunk(line));

        // Invoice title + number side by side
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 1});
        headerTable.setSpacingBefore(12);
        headerTable.setSpacingAfter(12);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        Paragraph invoiceTitle = new Paragraph("INVOICE", FONT_HEADING);
        leftCell.addElement(invoiceTitle);
        headerTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph invoiceNum = new Paragraph(
            "Invoice #INV-" + String.format("%05d", order.getId()), FONT_BOLD);
        invoiceNum.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(invoiceNum);
        Paragraph dateP = new Paragraph(
            "Date: " + order.getOrderDate()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy")), FONT_NORMAL);
        dateP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(dateP);
        headerTable.addCell(rightCell);

        document.add(headerTable);
    }

    // ── Order Info Section ──
    private void addOrderInfo(Document document, Order order, Payment payment)
            throws DocumentException {

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 1});
        infoTable.setSpacingAfter(16);

        // Order details cell
        PdfPCell orderCell = new PdfPCell();
        orderCell.setBackgroundColor(LIGHT_BLUE);
        orderCell.setPadding(10);
        orderCell.setBorder(Rectangle.NO_BORDER);
        orderCell.addElement(new Paragraph("Order Details", FONT_HEADING));
        orderCell.addElement(new Paragraph(
            "Order ID: #" + order.getId(), FONT_NORMAL));
        orderCell.addElement(new Paragraph(
            "Order Date: " + order.getOrderDate()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")), FONT_NORMAL));
        orderCell.addElement(new Paragraph(
            "Status: " + order.getStatus().name(), FONT_NORMAL));
        infoTable.addCell(orderCell);

        // Payment details cell
        PdfPCell payCell = new PdfPCell();
        payCell.setBackgroundColor(LIGHT_BLUE);
        payCell.setPadding(10);
        payCell.setBorder(Rectangle.NO_BORDER);
        payCell.addElement(new Paragraph("Payment Details", FONT_HEADING));

        if (payment != null) {
            payCell.addElement(new Paragraph(
                "Mode: " + (payment.getPaymentMode() == com.bookstore.model.PaymentMode.COD
                    ? "Cash on Delivery" : "Online (Razorpay)"), FONT_NORMAL));
            payCell.addElement(new Paragraph(
                "Status: " + payment.getStatus().name(), FONT_NORMAL));
            if (payment.getRazorpayPaymentId() != null) {
                payCell.addElement(new Paragraph(
                    "Payment ID: " + payment.getRazorpayPaymentId(), FONT_NORMAL));
            }
            if (payment.getPaidAt() != null) {
                payCell.addElement(new Paragraph(
                    "Paid At: " + payment.getPaidAt()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")),
                    FONT_NORMAL));
            }
        } else {
            payCell.addElement(new Paragraph("Payment info not available", FONT_SMALL));
        }
        infoTable.addCell(payCell);

        document.add(infoTable);
    }

    // ── Delivery Address Section ──
    private void addDeliveryAddress(Document document, Order order)
            throws DocumentException {

        Paragraph addrHeading = new Paragraph("Delivery Address", FONT_HEADING);
        addrHeading.setSpacingBefore(8);
        addrHeading.setSpacingAfter(6);
        document.add(addrHeading);

        PdfPTable addrTable = new PdfPTable(1);
        addrTable.setWidthPercentage(50);
        addrTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        addrTable.setSpacingAfter(16);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(LIGHT_BLUE);
        cell.setPadding(10);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.addElement(new Paragraph(order.getDeliveryFullName(), FONT_BOLD));
        cell.addElement(new Paragraph(order.getDeliveryAddressLine(), FONT_NORMAL));
        cell.addElement(new Paragraph(
            order.getDeliveryCity() + ", " + order.getDeliveryState()
            + " - " + order.getDeliveryPincode(), FONT_NORMAL));
        cell.addElement(new Paragraph("Phone: " + order.getDeliveryPhone(), FONT_NORMAL));
        addrTable.addCell(cell);

        document.add(addrTable);
    }

    // ── Items Table ──
    private void addItemsTable(Document document, Order order)
            throws DocumentException {

        Paragraph itemsHeading = new Paragraph("Order Items", FONT_HEADING);
        itemsHeading.setSpacingAfter(8);
        document.add(itemsHeading);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 1.5f, 2, 2});
        table.setSpacingAfter(16);

        // Table header
        String[] headers = { "Book Title", "Qty", "Unit Price", "Total" };
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, FONT_SUBHEADING));
            hCell.setBackgroundColor(TABLE_HEADER);
            hCell.setPadding(8);
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            hCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(hCell);
        }

        // Table rows
        int rowNum = 0;
        for (OrderItem item : order.getOrderItems()) {
            BaseColor rowColor = (rowNum % 2 == 0) ? BaseColor.WHITE : ROW_ALT;

            PdfPCell titleCell = new PdfPCell(
                new Phrase(item.getBook().getTitle(), FONT_NORMAL));
            titleCell.setBackgroundColor(rowColor);
            titleCell.setPadding(7);
            titleCell.setBorder(Rectangle.BOTTOM);
            titleCell.setBorderColor(new BaseColor(220, 220, 220));
            table.addCell(titleCell);

            PdfPCell qtyCell = new PdfPCell(
                new Phrase(String.valueOf(item.getQuantity()), FONT_NORMAL));
            qtyCell.setBackgroundColor(rowColor);
            qtyCell.setPadding(7);
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qtyCell.setBorder(Rectangle.BOTTOM);
            qtyCell.setBorderColor(new BaseColor(220, 220, 220));
            table.addCell(qtyCell);

            PdfPCell priceCell = new PdfPCell(
                new Phrase("Rs. " + String.format("%.2f", item.getPrice()), FONT_NORMAL));
            priceCell.setBackgroundColor(rowColor);
            priceCell.setPadding(7);
            priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            priceCell.setBorder(Rectangle.BOTTOM);
            priceCell.setBorderColor(new BaseColor(220, 220, 220));
            table.addCell(priceCell);

            double itemTotal = item.getPrice() * item.getQuantity();
            PdfPCell totalCell = new PdfPCell(
                new Phrase("Rs. " + String.format("%.2f", itemTotal), FONT_BOLD));
            totalCell.setBackgroundColor(rowColor);
            totalCell.setPadding(7);
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalCell.setBorder(Rectangle.BOTTOM);
            totalCell.setBorderColor(new BaseColor(220, 220, 220));
            table.addCell(totalCell);

            rowNum++;
        }

        document.add(table);
    }

    // ── Payment Summary ──
    private void addPaymentSummary(Document document, Order order, Payment payment)
            throws DocumentException {

        // Right-aligned summary box
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(40);
        summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.setWidths(new float[]{1.5f, 1});
        summaryTable.setSpacingAfter(24);

        addSummaryRow(summaryTable, "Subtotal:", "Rs. " +
            String.format("%.2f", order.getTotalAmount()), false);
        addSummaryRow(summaryTable, "Delivery:", "FREE", false);
        addSummaryRow(summaryTable, "Grand Total:",
            "Rs. " + String.format("%.2f", order.getTotalAmount()), true);

        document.add(summaryTable);
    }

    private void addSummaryRow(PdfPTable table, String label,
                                String value, boolean isTotal) {
        Font labelFont = isTotal ? FONT_TOTAL : FONT_NORMAL;
        Font valueFont = isTotal ? FONT_TOTAL : FONT_BOLD;
        BaseColor bg = isTotal ? LIGHT_BLUE : BaseColor.WHITE;

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(bg);
        labelCell.setBorder(isTotal ? Rectangle.BOX : Rectangle.BOTTOM);
        labelCell.setBorderColor(new BaseColor(200, 200, 200));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(6);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBackgroundColor(bg);
        valueCell.setBorder(isTotal ? Rectangle.BOX : Rectangle.BOTTOM);
        valueCell.setBorderColor(new BaseColor(200, 200, 200));
        table.addCell(valueCell);
    }

    // ── Footer ──
    private void addFooter(Document document) throws DocumentException {

        LineSeparator line = new LineSeparator(1, 100,
            new BaseColor(200, 200, 200), Element.ALIGN_CENTER, -5);
        document.add(new Chunk(line));

        Paragraph footer = new Paragraph(
            "Thank you for shopping with Book Wala!\n" +
            "For any queries, contact: support@bookwala.com",
            FONT_SMALL);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        document.add(footer);
    }
}