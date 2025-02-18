package com.genoutfit.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genoutfit.api.model.OutfitVector;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.pinecone.clients.Index;
import io.pinecone.configs.PineconeConfig;
import io.pinecone.configs.PineconeConnection;
import io.pinecone.unsigned_indices_model.QueryResponseWithUnsignedIndices;
import io.pinecone.unsigned_indices_model.ScoredVectorWithUnsignedIndices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PineconeClient {

    @Autowired
    private PineconeConfig pineconeConfig;

    @Autowired
    private ObjectMapper objectMapper;

    public List<OutfitVector> search(Map<String, Object> criteria, int limit) {
        try {
            PineconeConnection connection = new PineconeConnection(pineconeConfig);
            Index index = new Index(connection, "outfits");

            // Convert metadata criteria to Struct filter
            Struct.Builder filterBuilder = Struct.newBuilder();
            for (Map.Entry<String, Object> entry : criteria.entrySet()) {
                filterBuilder.putFields(entry.getKey(),
                        Value.newBuilder().setStringValue(entry.getValue().toString()).build());
            }
            Struct filter = filterBuilder.build();

            // Execute search
            QueryResponseWithUnsignedIndices response = index.query(
                    limit,
                    null,               // You'll need to add your vector here
                    null,
                    null,
                    null,
                    "fashion",          // Using 'fashion' namespace as per your code
                    filter,
                    true,
                    true
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