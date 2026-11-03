package com.example.logistics;

public final class LogisticsExample {
    public static void main(String[] args) throws Exception {
        String tenant = args.length == 0 ? "acme-eu" : args[0];
        ShipmentTenantService service = new ShipmentTenantService(new InfraiClient());
        ShipmentTenantService.TenantPlan plan = service.plan(tenant, "shipment-1042", "proof/shipment-1042.pdf", "CUSTOMS_HOLD");
        System.out.println("Tenant bucket: " + plan.bucket());
        System.out.println("Provisioned actions: " + service.provision(plan));
    }
}

