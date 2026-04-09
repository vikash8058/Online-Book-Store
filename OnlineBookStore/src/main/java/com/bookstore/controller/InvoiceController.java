package com.bookstore.controller;

import com.bookstore.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /*
     * GET /api/orders/{id}/invoice
     * Returns PDF as downloadable file
     */
    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable("id") Long orderId)
            throws Exception {

        byte[] pdf = invoiceService.generateInvoice(orderId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=invoice-order-" + orderId + ".pdf")
                .body(pdf);
    }
}