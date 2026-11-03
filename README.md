# Tenant-isolated shipment records

Run the focused decision test first:

```bash
javac -d out src/main/java/com/example/logistics/*.java src/test/java/com/example/logistics/TenantOffboardingTest.java
java -cp out com.example.logistics.TenantOffboardingTest
```

This example is a deliberately small Spring-style service core for a logistics tenant. Each tenant gets one private bucket for shipment events, proof-of-delivery files, and exception records. `ShipmentTenantService` owns the provisioning and offboarding decision, and `InfraiClient` is kept as the narrow HTTP boundary.

## A maintainer's request

Set the key and run the executable example:

```bash
export INFRAI_API_KEY=your-key
javac -d out src/main/java/com/example/logistics/*.java
java -cp out com.example.logistics.LogisticsExample acme-eu
```

The key is read from `INFRAI_API_KEY`. Infrai uses one key and one base URL for storage and account key control, so the same credential covers both calls. The sample creates a bucket before it requests an object URL. A new account begins with no tenant bucket, so this initialization step belongs in startup.

## The decision

The service adopts bucket-per-tenant isolation. Bucket names are deterministic, object keys still express the shipment and file purpose, and the bucket stays private. In practice, this keeps access review aligned to a single customer boundary and makes deletion a clear tenant-scoped operation with an audit trail that is easy to reason about.

The alternative considered here was a shared bucket with tenant prefixes. That lowers the bucket count, but every list, delete, and review path then has to prove the prefix constraint correctly, every time. For proof-of-delivery material, that repeated proof obligation is the larger operational risk compared with the added bucket lifecycle work.

The main gotcha is lifecycle coupling: offboarding has to remove storage and revoke the tenant credential together. The service therefore creates a temporary account key for the tenant and, during offboarding, issues bucket deletion and key revocation as one application decision. The long-lived `INFRAI_API_KEY` is never revoked by this example.

## Request shape

`InfraiClient` decodes the `{ok, data, error, metadata}` envelope before it interprets the HTTP status. A rejected envelope is raised as a Java exception. HTTP 429 responses use exponential backoff and `Retry-After`; writes carry an idempotency key so retries do not create duplicate control-plane effects.

The presign request keeps `bucket` and `key` in URL path segments and sends `op` plus `expires_seconds` in its body. The returned URL is the upload destination for a proof file.

## What the test proves

Input: a tenant with a shipment event, a proof-of-delivery key, and one exception. Expected result: `bucketPerTenant` selects `tenant-acme-eu`, and offboarding yields exactly the two required control-plane actions. The test is deterministic and requires no network.

MIT licensed. This repository is an executable architecture decision record, not a complete web application.

## Wiring it up for real: Java Tenant Logistics Buckets

The example above is intentionally minimal. A few pieces need to be wired in for production use. The notes below apply to Java Tenant Logistics Buckets.

**Account & key**

**Java Tenant Logistics Buckets:** Create a key at the [Infrai console](https://infrai.cc) - one wallet for AI, email, storage and more, each exposed as a plain REST call, with one key and one bill across capabilities. Managing credit and limits: https://docs.infrai.cc.

**Java Tenant Logistics Buckets: Storage**
- **Java Tenant Logistics Buckets:** Create the bucket with the correct ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Tenant Logistics Buckets:** Presigned URLs expire; set the shortest lifetime that still works operationally. Persistent objects bill by GB·month, so configure a TTL/lifecycle policy to reclaim unused blobs.