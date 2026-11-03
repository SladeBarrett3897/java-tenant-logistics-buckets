package com.example.logistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ShipmentTenantService {
    private final InfraiClient infrai;

    public ShipmentTenantService(InfraiClient infrai) { this.infrai = infrai; }

    public TenantPlan plan(String tenantId, String shipmentId, String proofKey, String exceptionCode) {
        return new TenantPlan("tenant-" + tenantId, shipmentId, proofKey, exceptionCode);
    }

    public List<String> provision(TenantPlan plan) throws IOException, InterruptedException {
        infrai.createBucket(plan.bucket(), "bucket-" + plan.bucket());
        infrai.createTenantKey("tenant-" + plan.bucket(), "key-" + plan.bucket());
        infrai.presignProof(plan.bucket(), plan.proofKey(), "proof-" + plan.shipmentId());
        return List.of("bucket-created", "tenant-key-created", "proof-upload-url-issued");
    }

    public List<String> offboard(String bucket, String temporaryKeyId) throws IOException, InterruptedException {
        List<String> actions = new ArrayList<>();
        infrai.deleteBucket(bucket);
        actions.add("bucket-deleted");
        infrai.revokeTenantKey(temporaryKeyId);
        actions.add("key-revoked");
        return actions;
    }

    public record TenantPlan(String bucket, String shipmentId, String proofKey, String exceptionCode) {}
}
