Opa Embedded Policy Example
---

# Compile REGO Policy to WASM
```
opa build \
-t wasm \
-o scratch/bundle.tar.gz \
-e app/rbac \
src/main/resources/policy/app/rbac/policy.rego
```

# Extract compiled wasm module
```
tar xzf scratch/bundle.tar.gz -C src/main/resources/policy "/policy.wasm"
```

# Build

```
mvn clean package
```

# Run

```
mvn exec:java
```