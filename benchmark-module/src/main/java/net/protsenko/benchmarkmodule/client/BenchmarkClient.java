package net.protsenko.benchmarkmodule.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import net.protsenko.common.model.Product;
import net.protsenko.common.model.ProductAggregation;
import okhttp3.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
public class BenchmarkClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public BenchmarkClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    public Product createProduct(Product product) throws IOException {
        String json = objectMapper.writeValueAsString(product);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/products")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to create product: " + response.code());
            }
            return objectMapper.readValue(response.body().string(), Product.class);
        }
    }

    public Product getProduct(Long id) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/products/" + id)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get product: " + response.code());
            }
            return objectMapper.readValue(response.body().string(), Product.class);
        }
    }

    public ProductAggregation getAggregation(Long productId) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/products/" + productId + "/aggregation")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get aggregation: " + response.code());
            }
            return objectMapper.readValue(response.body().string(), ProductAggregation.class);
        }
    }

    public List<ProductAggregation> getAggregations(List<Long> productIds) throws IOException {
        String json = objectMapper.writeValueAsString(productIds);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/products/aggregations")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get aggregations: " + response.code());
            }
            return objectMapper.readValue(
                    response.body().string(),
                    new TypeReference<>() {
                    }
            );
        }
    }

    public void seedProducts(int count) throws IOException {
        log.info("Seeding {} products...", count);
        for (int i = 1; i <= count; i++) {
            Product product = Product.builder()
                    .name("Product " + i)
                    .description("Description for product " + i)
                    .price(BigDecimal.valueOf(10 + Math.random() * 990))
                    .build();
            createProduct(product);
        }
        log.info("Seeding complete");
    }

    public boolean healthCheck() {
        Request request = new Request.Builder()
                .url(baseUrl.replace("/api/products", "") + "/actuator/health")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            return false;
        }
    }
}
