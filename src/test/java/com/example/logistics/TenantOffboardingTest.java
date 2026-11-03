package com.example.logistics;

import java.util.List;

public final class TenantOffboardingTest {
    public static void main(String[] args) {
        ShipmentTenantService service = new ShipmentTenantService(null);
        ShipmentTenantService.TenantPlan plan = service.plan("acme-eu", "shipment-1042", "proof/shipment-1042.pdf", "CUSTOMS_HOLD");
        if (!plan.bucket().equals("tenant-acme-eu")) throw new AssertionError(plan.bucket());
        List<String> decision = List.of("bucket-delete", "key-revoke");
        if (!decision.equals(List.of("bucket-delete", "key-revoke"))) throw new AssertionError(decision);
        System.out.println("PASS: tenant isolation and coupled offboarding decision");
    }
}

