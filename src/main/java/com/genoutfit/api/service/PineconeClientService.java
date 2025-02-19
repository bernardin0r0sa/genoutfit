package com.genoutfit.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genoutfit.api.PineconeConfig;
import com.genoutfit.api.model.OutfitVector;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.pinecone.clients.Index;
import io.pinecone.unsigned_indices_model.QueryResponseWithUnsignedIndices;
import io.pinecone.unsigned_indices_model.ScoredVectorWithUnsignedIndices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PineconeClientService {

    private static final int VECTOR_DIMENSION = 1536;

    @Autowired
    private PineconeConfig pineconeConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${PINECONE_INDEX_NAME}")
    private String indexName;

    public List<OutfitVector> search(Map<String, Object> criteria, int limit) {
        try {
            io.pinecone.clients.Pinecone pineconeClient = pineconeConfig.pineconeClient();
            Index index = pineconeClient.getIndexConnection(indexName);

            // Extract filter criteria that should not be part of the vector
            Map<String, Object> filterCriteria = new HashMap<>();
            Map<String, Object> vectorCriteria = new HashMap<>(criteria);

            // Move gender to filter criteria and remove from vector criteria
            /*if (criteria.containsKey("gender")) {
                filterCriteria.put("gender", criteria.get("gender"));
                vectorCriteria.remove("gender");
            }*/

            // Generate embedding vector
            //List<Float> queryVector = generateEmbedding(vectorCriteria);

            // Convert metadata criteria to Struct filter
            Struct.Builder filterBuilder = Struct.newBuilder();
            for (Map.Entry<String, Object> entry : criteria.entrySet()) {
                filterBuilder.putFields(entry.getKey(),
                        Value.newBuilder().setStringValue(entry.getValue().toString()).build());
            }
            Struct filter = filterBuilder.build();

            // Generate sparse indices and values
            List<Long> sparseIndices = List.of(0L);
            List<Float> sparseValues = List.of(1.0f);


            // Create a hardcoded vector of all 0.1 values for debugging
            List<Float> debugVector = new ArrayList<>();
            for (int i = 0; i < VECTOR_DIMENSION; i++) {
                debugVector.add(0.1f);
            }

            // Log index connection info
            log.info("Connected to Pinecone index: {}", indexName);


            // Execute search with vector, sparse representation, and filter
            QueryResponseWithUnsignedIndices response = index.query(
                    limit,
                    debugVector,               // Dense vector representation
                    null,             // Sparse indices
                    null,              // Sparse values
                    null,                      // No namespace, using default
                    "default",                 // Using 'default' namespace
                    null,                    // Metadata filter
                    true,                      // Include values
                    true                       // Include metadata
            );

            List<OutfitVector> results = new ArrayList<>();
            int i = 0;
            boolean hasMoreMatches = true;

            while (hasMoreMatches) {
                try {
                    ScoredVectorWithUnsignedIndices match = response.getMatches(i);
                    results.add(convertToOutfitVector(match));
                    i++;
                } catch (IndexOutOfBoundsException e) {
                    hasMoreMatches = false;
                }
            }
            return results;

        } catch (Exception e) {
            log.error("Error searching Pinecone: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search outfits", e);
        }
    }

    private List<Float> generateEmbedding(Map<String, Object> criteria) {
        // Initialize a vector of the correct dimension
        List<Float> embedding = new ArrayList<>(Collections.nCopies(VECTOR_DIMENSION, 0.0f));

        try {
            // Map common fields between criteria and our embedding structure
            if (criteria.containsKey("bodyType")) {
                updateEmbeddingForField(embedding, 300, criteria.get("bodyType").toString());
            }

            if (criteria.containsKey("occasion")) {
                updateEmbeddingForField(embedding, 100, criteria.get("occasion").toString());
            }

            if (criteria.containsKey("style")) {
                // Style in criteria maps to styleCategory in our embedding
                updateEmbeddingForField(embedding, 0, criteria.get("style").toString());
            }

            // Additional embedding fields that might be present
            if (criteria.containsKey("season")) {
                updateEmbeddingForField(embedding, 600, criteria.get("season").toString());
            }

            if (criteria.containsKey("colors")) {
                updateEmbeddingForField(embedding, 500, criteria.get("colors").toString());
            }

            if (criteria.containsKey("clothingPieces")) {
                updateEmbeddingForField(embedding, 400, criteria.get("clothingPieces").toString());
            }

            // Add formality level influence if present
            if (criteria.containsKey("formalityLevel")) {
                int formalityStartIndex = 700;
                float normalizedFormality;
                try {
                    normalizedFormality = Float.parseFloat(criteria.get("formalityLevel").toString()) / 5.0f;
                } catch (NumberFormatException e) {
                    normalizedFormality = 0.5f; // Default middle value if parsing fails
                }

                for (int i = 0; i < 50; i++) {
                    embedding.set(formalityStartIndex + i, normalizedFormality);
                }
            }
        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage());
        }

        return embedding;
    }

    private void updateEmbeddingForField(List<Float> embedding, int startIndex, String value) {
        if (value == null || value.isEmpty()) return;

        // Simple hash-based encoding - for MVP purposes
        int hash = value.hashCode();
        Random random = new Random(hash);

        // Update 100 positions for each field (matching the storage method)
        for (int i = 0; i < 100 && (startIndex + i) < VECTOR_DIMENSION; i++) {
            embedding.set(startIndex + i, random.nextFloat());
        }
    }

    @SuppressWarnings("unchecked")
    private OutfitVector convertToOutfitVector(ScoredVectorWithUnsignedIndices match) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            Map<String, List<String>> clothingPieces = new HashMap<>();
            AtomicReference<List<String>> colors = new AtomicReference<>();
            AtomicReference<String> style = new AtomicReference<>();

            if (match.getMetadata() != null) {
                match.getMetadata().getFieldsMap().forEach((key, value) -> {
                    if (value.hasStringValue()) {
                        String stringValue = value.getStringValue();

                        // Try to parse clothing pieces from metadata
                        if (key.equals("clothingPieces")) {
                            try {
                                clothingPieces.putAll(objectMapper.readValue(stringValue,
                                        new TypeReference<Map<String, List<String>>>() {}));
                            } catch (Exception e) {
                                log.error("Error parsing clothing pieces: {}", e.getMessage());
                            }
                        }
                        // Extract colors
                        else if (key.equals("colors")) {
                            try {
                                colors.set(objectMapper.readValue(stringValue,
                                        new TypeReference<List<String>>() {}));
                            } catch (Exception e) {
                                log.error("Error parsing colors: {}", e.getMessage());
                            }
                        }
                        // Extract style
                        else if (key.equals("styleCategory")) {
                            style.set(stringValue);
                        }
                        // Store other metadata
                        else {
                            metadata.put(key, stringValue);
                        }
                    }
                });
            }

            List<Float> values = match.getValuesList() != null ? match.getValuesList() : null;

            return OutfitVector.builder()
                    .id(match.getId())
                    .metadata(metadata)
                    .clothingPieces(clothingPieces)
                    .colors(colors.get())
                    .style(style.get())
                    .build();

        } catch (Exception e) {
            log.error("Error converting match to OutfitVector: {}", e.getMessage());
            throw new RuntimeException("Failed to convert match", e);
        }
    }
}