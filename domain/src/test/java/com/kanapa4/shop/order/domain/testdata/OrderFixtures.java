package com.kanapa4.shop.order.domain.testdata;

import com.kanapa4.shop.order.model.Address;
import com.kanapa4.shop.order.model.CartSnapshot;
import com.kanapa4.shop.order.model.Customer;
import com.kanapa4.shop.order.model.DeliveryMethod;
import com.kanapa4.shop.order.model.Invoice;
import com.kanapa4.shop.order.model.IssuedInvoice;
import com.kanapa4.shop.order.model.Order;
import com.kanapa4.shop.order.model.OrderItem;
import com.kanapa4.shop.order.model.OrderStatus;
import com.kanapa4.shop.order.model.PaymentMethod;
import com.kanapa4.shop.order.model.Price;
import com.kanapa4.shop.order.model.SelectedOption;
import com.kanapa4.shop.order.model.VatRate;
import com.kanapa4.shop.order.pub.request.AddressRequest;
import com.kanapa4.shop.order.pub.request.CheckoutRequest;
import com.kanapa4.shop.order.pub.request.CustomerRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderFixtures {

    public static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    public static final UUID CART_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    public static final UUID ORDER_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    public static final UUID INVOICE_ID = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");
    public static final UUID PRODUCT_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
    public static final String ORDER_NUMBER = "ORD-20261018-ABC123";
    public static final BigDecimal CART_TOTAL = new BigDecimal("10398.00");

    private OrderFixtures() {
    }

    public static Price laptopPrice() {
        return new Price(new BigDecimal("5199.00"), "PLN", VatRate.VAT_23);
    }

    public static OrderItem laptopItem() {
        return new OrderItem(null, PRODUCT_ID, "Laptop Pro 15", laptopPrice(), 2,
                List.of(new SelectedOption("RAM", "RAM_32", "32 GB", new BigDecimal("400.00"))));
    }

    public static CartSnapshot cartSnapshot() {
        return new CartSnapshot(CART_ID, USER_ID, List.of(laptopItem()), CART_TOTAL, "PLN");
    }

    public static CartSnapshot emptyCart() {
        return new CartSnapshot(CART_ID, USER_ID, List.of(), BigDecimal.ZERO, "PLN");
    }

    public static Customer customer() {
        return new Customer("Jan", "Kowalski", "jan@example.com", "+48123456789");
    }

    public static Address address() {
        return new Address("Marszalkowska 1", "Warszawa", "00-001", "PL");
    }

    public static CustomerRequest customerRequest() {
        return new CustomerRequest("Jan", "Kowalski", "jan@example.com", "+48123456789");
    }

    public static AddressRequest addressRequest() {
        return new AddressRequest("Marszalkowska 1", "Warszawa", "00-001", "PL");
    }

    public static CheckoutRequest checkoutRequest(BigDecimal expectedTotal) {
        return checkoutRequest(com.kanapa4.shop.order.pub.type.DeliveryMethod.COURIER,
                addressRequest(), null, expectedTotal);
    }

    public static CheckoutRequest checkoutRequest(com.kanapa4.shop.order.pub.type.DeliveryMethod deliveryMethod,
                                                  AddressRequest shippingAddress,
                                                  AddressRequest billingAddress,
                                                  BigDecimal expectedTotal) {
        return new CheckoutRequest(customerRequest(), deliveryMethod,
                com.kanapa4.shop.order.pub.type.PaymentMethod.CARD, shippingAddress, billingAddress, expectedTotal);
    }

    public static Order order(OrderStatus status) {
        return order(status, Invoice.builder().id(INVOICE_ID).build());
    }

    public static Order order(OrderStatus status, Invoice invoice) {
        return Order.builder()
                .id(ORDER_ID)
                .orderNumber(ORDER_NUMBER)
                .userId(USER_ID)
                .status(status)
                .deliveryMethod(DeliveryMethod.COURIER)
                .paymentMethod(PaymentMethod.CARD)
                .customer(customer())
                .shippingAddress(address())
                .billingAddress(address())
                .items(List.of(laptopItem()))
                .itemsTotal(CART_TOTAL)
                .deliveryCost(new BigDecimal("15.00"))
                .grandTotal(new BigDecimal("10413.00"))
                .currency("PLN")
                .invoice(invoice)
                .createdAt(Instant.parse("2026-10-18T10:00:00Z"))
                .build();
    }

    public static IssuedInvoice issuedInvoice() {
        return new IssuedInvoice("ext-1", "FV/2026/10/1", "https://invoices.example.com/ext-1.pdf",
                Instant.parse("2026-10-18T12:00:00Z"));
    }
}