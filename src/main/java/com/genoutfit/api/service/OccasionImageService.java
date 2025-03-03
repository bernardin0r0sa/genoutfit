package com.genoutfit.api.service;

import com.genoutfit.api.model.Gender;
import com.genoutfit.api.model.User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import jakarta.annotation.PostConstruct;


@Service
public class OccasionImageService {

    private static final Map<String, Map<String, String>> OCCASION_IMAGES = new HashMap<>();
    private static final Random random = new Random();

    @PostConstruct
    public void init() {
        // Initialize the image mappings
        // Format: OCCASION -> {ETHNICITY_BODYTYPE -> imagePath}

        // Date Night images
        Map<String, String> dateNightImages = new HashMap<>();
        dateNightImages.put("BLACK_MEDIUM", "/assets/images/occasions/date_night_black_medium.jpg");
        dateNightImages.put("BLACK_SLIM", "/assets/images/occasions/date_night_black_slim.jpg");
        dateNightImages.put("BLACK_PLUS_SIZE", "/assets/images/occasions/date_night_black_plus.jpg");
        dateNightImages.put("WHITE_MEDIUM", "/assets/images/occasions/date_night_white_medium.jpg");
        dateNightImages.put("WHITE_SLIM", "/assets/images/occasions/date_night_white_slim.jpg");
        dateNightImages.put("WHITE_PLUS_SIZE", "/assets/images/occasions/date_night_white_plus.jpg");
        dateNightImages.put("ASIAN_MEDIUM", "/assets/images/occasions/date_night_asian_medium.jpg");
        dateNightImages.put("ASIAN_SLIM", "/assets/images/occasions/date_night_asian_slim.jpg");
        dateNightImages.put("ASIAN_PLUS_SIZE", "/assets/images/occasions/date_night_asian_plus.jpg");
        dateNightImages.put("LATINO_MEDIUM", "/assets/images/occasions/date_night_latino_medium.jpg");
        dateNightImages.put("LATINO_SLIM", "/assets/images/occasions/date_night_latino_slim.jpg");
        dateNightImages.put("LATINO_PLUS_SIZE", "/assets/images/occasions/date_night_latino_plus.jpg");
        OCCASION_IMAGES.put("DATE_NIGHT", dateNightImages);

        // Office Party images
        Map<String, String> officePartyImages = new HashMap<>();
        officePartyImages.put("BLACK_MEDIUM", "/assets/images/occasions/office_party_black_medium.jpg");
        officePartyImages.put("BLACK_SLIM", "/assets/images/occasions/office_party_black_slim.jpg");
        officePartyImages.put("BLACK_PLUS_SIZE", "/assets/images/occasions/office_party_black_plus.jpg");
        officePartyImages.put("WHITE_MEDIUM", "/assets/images/occasions/office_party_white_medium.jpg");
        officePartyImages.put("WHITE_SLIM", "/assets/images/occasions/office_party_white_slim.jpg");
        officePartyImages.put("WHITE_PLUS_SIZE", "/assets/images/occasions/office_party_white_plus.jpg");
        officePartyImages.put("ASIAN_MEDIUM", "/assets/images/occasions/office_party_asian_medium.jpg");
        officePartyImages.put("ASIAN_SLIM", "/assets/images/occasions/office_party_asian_slim.jpg");
        officePartyImages.put("ASIAN_PLUS_SIZE", "/assets/images/occasions/office_party_asian_plus.jpg");
        officePartyImages.put("LATINO_MEDIUM", "/assets/images/occasions/office_party_latino_medium.jpg");
        officePartyImages.put("LATINO_SLIM", "/assets/images/occasions/office_party_latino_slim.jpg");
        officePartyImages.put("LATINO_PLUS_SIZE", "/assets/images/occasions/office_party_latino_plus.jpg");
        OCCASION_IMAGES.put("OFFICE_PARTY", officePartyImages);

        // Wedding Guest images
        Map<String, String> weddingGuestImages = new HashMap<>();
        weddingGuestImages.put("BLACK_MEDIUM", "/assets/images/occasions/wedding_guest_black_medium.jpg");
        weddingGuestImages.put("BLACK_SLIM", "/assets/images/occasions/wedding_guest_black_slim.jpg");
        weddingGuestImages.put("BLACK_PLUS_SIZE", "/assets/images/occasions/wedding_guest_black_plus.jpg");
        weddingGuestImages.put("WHITE_MEDIUM", "/assets/images/occasions/wedding_guest_white_medium.jpg");
        weddingGuestImages.put("WHITE_SLIM", "/assets/images/occasions/wedding_guest_white_slim.jpg");
        weddingGuestImages.put("WHITE_PLUS_SIZE", "/assets/images/occasions/wedding_guest_white_plus.jpg");
        weddingGuestImages.put("ASIAN_MEDIUM", "/assets/images/occasions/wedding_guest_asian_medium.jpg");
        weddingGuestImages.put("ASIAN_SLIM", "/assets/images/occasions/wedding_guest_asian_slim.jpg");
        weddingGuestImages.put("ASIAN_PLUS_SIZE", "/assets/images/occasions/wedding_guest_asian_plus.jpg");
        weddingGuestImages.put("LATINO_MEDIUM", "/assets/images/occasions/wedding_guest_latino_medium.jpg");
        weddingGuestImages.put("LATINO_SLIM", "/assets/images/occasions/wedding_guest_latino_slim.jpg");
        weddingGuestImages.put("LATINO_PLUS_SIZE", "/assets/images/occasions/wedding_guest_latino_plus.jpg");
        OCCASION_IMAGES.put("WEDDING_GUEST", weddingGuestImages);

        // Casual Outing images
        Map<String, String> casualOutingImages = new HashMap<>();
        casualOutingImages.put("BLACK_MEDIUM", "/assets/images/occasions/casual_outing_black_medium.jpg");
        casualOutingImages.put("BLACK_SLIM", "/assets/images/occasions/casual_outing_black_slim.jpg");
        casualOutingImages.put("BLACK_PLUS_SIZE", "/assets/images/occasions/casual_outing_black_plus.jpg");
        casualOutingImages.put("WHITE_MEDIUM", "/assets/images/occasions/casual_outing_white_medium.jpg");
        casualOutingImages.put("WHITE_SLIM", "/assets/images/occasions/casual_outing_white_slim.jpg");
        casualOutingImages.put("WHITE_PLUS_SIZE", "/assets/images/occasions/casual_outing_white_plus.jpg");
        casualOutingImages.put("ASIAN_MEDIUM", "/assets/images/occasions/casual_outing_asian_medium.jpg");
        casualOutingImages.put("ASIAN_SLIM", "/assets/images/occasions/casual_outing_asian_slim.jpg");
        casualOutingImages.put("ASIAN_PLUS_SIZE", "/assets/images/occasions/casual_outing_asian_plus.jpg");
        casualOutingImages.put("LATINO_MEDIUM", "/assets/images/occasions/casual_outing_latino_medium.jpg");
        casualOutingImages.put("LATINO_SLIM", "/assets/images/occasions/casual_outing_latino_slim.jpg");
        casualOutingImages.put("LATINO_PLUS_SIZE", "/assets/images/occasions/casual_outing_latino_plus.jpg");
        OCCASION_IMAGES.put("CASUAL_OUTING", casualOutingImages);

        // Formal Event images
        Map<String, String> formalEventImages = new HashMap<>();
        formalEventImages.put("BLACK_MEDIUM", "/assets/images/occasions/formal_event_black_medium.jpg");
        formalEventImages.put("BLACK_SLIM", "/assets/images/occasions/formal_event_black_slim.jpg");
        formalEventImages.put("BLACK_PLUS_SIZE", "/assets/images/occasions/formal_event_black_plus.jpg");
        formalEventImages.put("WHITE_MEDIUM", "/assets/images/occasions/formal_event_white_medium.jpg");
        formalEventImages.put("WHITE_SLIM", "/assets/images/occasions/formal_event_white_slim.jpg");
        formalEventImages.put("WHITE_PLUS_SIZE", "/assets/images/occasions/formal_event_white_plus.jpg");
        formalEventImages.put("ASIAN_MEDIUM", "/assets/images/occasions/formal_event_asian_medium.jpg");
        formalEventImages.put("ASIAN_SLIM", "/assets/images/occasions/formal_event_asian_slim.jpg");
        formalEventImages.put("ASIAN_PLUS_SIZE", "/assets/images/occasions/formal_event_asian_plus.jpg");
        formalEventImages.put("LATINO_MEDIUM", "/assets/images/occasions/formal_event_latino_medium.jpg");
        formalEventImages.put("LATINO_SLIM", "/assets/images/occasions/formal_event_latino_slim.jpg");
        formalEventImages.put("LATINO_PLUS_SIZE", "/assets/images/occasions/formal_event_latino_plus.jpg");
        OCCASION_IMAGES.put("FORMAL_EVENT", formalEventImages);

        // Beach Vacation images
        Map<String, String> beachVacationImages = new HashMap<>();
        beachVacationImages.put("BLACK_MEDIUM", "/assets/images/occasions/beach_vacation_black_medium.jpg");
        beachVacationImages.put("BLACK_SLIM", "/assets/images/occasions/beach_vacation_black_slim.jpg");
        beachVacationImages.put("BLACK_PLUS_SIZE", "/assets/images/occasions/beach_vacation_black_plus.jpg");
        beachVacationImages.put("WHITE_MEDIUM", "/assets/images/occasions/beach_vacation_white_medium.jpg");
        beachVacationImages.put("WHITE_SLIM", "/assets/images/occasions/beach_vacation_white_slim.jpg");
        beachVacationImages.put("WHITE_PLUS_SIZE", "/assets/images/occasions/beach_vacation_white_plus.jpg");
        beachVacationImages.put("ASIAN_MEDIUM", "/assets/images/occasions/beach_vacation_asian_medium.jpg");
        beachVacationImages.put("ASIAN_SLIM", "/assets/images/occasions/beach_vacation_asian_slim.jpg");
        beachVacationImages.put("ASIAN_PLUS_SIZE", "/assets/images/occasions/beach_vacation_asian_plus.jpg");
        beachVacationImages.put("LATINO_MEDIUM", "/assets/images/occasions/beach_vacation_latino_medium.jpg");
        beachVacationImages.put("LATINO_SLIM", "/assets/images/occasions/beach_vacation_latino_slim.jpg");
        beachVacationImages.put("LATINO_PLUS_SIZE", "/assets/images/occasions/beach_vacation_latino_plus.jpg");
        OCCASION_IMAGES.put("BEACH_VACATION", beachVacationImages);

        // Business Casual images
        Map<String, String> businessCasualImages = new HashMap<>();
        businessCasualImages.put("BLACK_MEDIUM", "/assets/images/occasions/business_casual_black_medium.jpg");
        businessCasualImages.put("BLACK_SLIM", "/assets/images/occasions/business_casual_black_slim.jpg");
        businessCasualImages.put("BLACK_PLUS_SIZE", "/assets/images/occasions/business_casual_black_plus.jpg");
        businessCasualImages.put("WHITE_MEDIUM", "/assets/images/occasions/business_casual_white_medium.jpg");
        businessCasualImages.put("WHITE_SLIM", "/assets/images/occasions/business_casual_white_slim.jpg");
        businessCasualImages.put("WHITE_PLUS_SIZE", "/assets/images/occasions/business_casual_white_plus.jpg");
        businessCasualImages.put("ASIAN_MEDIUM", "/assets/images/occasions/business_casual_asian_medium.jpg");
        businessCasualImages.put("ASIAN_SLIM", "/assets/images/occasions/business_casual_asian_slim.jpg");
        businessCasualImages.put("ASIAN_PLUS_SIZE", "/assets/images/occasions/business_casual_asian_plus.jpg");
        businessCasualImages.put("LATINO_MEDIUM", "/assets/images/occasions/business_casual_latino_medium.jpg");
        businessCasualImages.put("LATINO_SLIM", "/assets/images/occasions/business_casual_latino_slim.jpg");
        businessCasualImages.put("LATINO_PLUS_SIZE", "/assets/images/occasions/business_casual_latino_plus.jpg");
        OCCASION_IMAGES.put("BUSINESS_CASUAL", businessCasualImages);

        // Party images
        Map<String, String> partyImages = new HashMap<>();
        partyImages.put("BLACK_MEDIUM", "/assets/images/occasions/party_black_medium.jpg");
        partyImages.put("BLACK_SLIM", "/assets/images/occasions/party_black_slim.jpg");
        partyImages.put("BLACK_PLUS_SIZE", "/assets/images/occasions/party_black_plus.jpg");
        partyImages.put("WHITE_MEDIUM", "/assets/images/occasions/party_white_medium.jpg");
        partyImages.put("WHITE_SLIM", "/assets/images/occasions/party_white_slim.jpg");
        partyImages.put("WHITE_PLUS_SIZE", "/assets/images/occasions/party_white_plus.jpg");
        partyImages.put("ASIAN_MEDIUM", "/assets/images/occasions/party_asian_medium.jpg");
        partyImages.put("ASIAN_SLIM", "/assets/images/occasions/party_asian_slim.jpg");
        partyImages.put("ASIAN_PLUS_SIZE", "/assets/images/occasions/party_asian_plus.jpg");
        partyImages.put("LATINO_MEDIUM", "/assets/images/occasions/party_latino_medium.jpg");
        partyImages.put("LATINO_SLIM", "/assets/images/occasions/party_latino_slim.jpg");
        partyImages.put("LATINO_PLUS_SIZE", "/assets/images/occasions/party_latino_plus.jpg");
        OCCASION_IMAGES.put("PARTY", partyImages);

        // Gala images
        Map<String, String> galaImages = new HashMap<>();
        galaImages.put("BLACK_MEDIUM", "/assets/images/occasions/gala_black_medium.jpg");
        galaImages.put("BLACK_SLIM", "/assets/images/occasions/gala_black_slim.jpg");
        galaImages.put("BLACK_PLUS_SIZE", "/assets/images/occasions/gala_black_plus.jpg");
        galaImages.put("WHITE_MEDIUM", "/assets/images/occasions/gala_white_medium.jpg");
        galaImages.put("WHITE_SLIM", "/assets/images/occasions/gala_white_slim.jpg");
        galaImages.put("WHITE_PLUS_SIZE", "/assets/images/occasions/gala_white_plus.jpg");
        galaImages.put("ASIAN_MEDIUM", "/assets/images/occasions/gala_asian_medium.jpg");
        galaImages.put("ASIAN_SLIM", "/assets/images/occasions/gala_asian_slim.jpg");
        galaImages.put("ASIAN_PLUS_SIZE", "/assets/images/occasions/gala_asian_plus.jpg");
        galaImages.put("LATINO_MEDIUM", "/assets/images/occasions/gala_latino_medium.jpg");
        galaImages.put("LATINO_SLIM", "/assets/images/occasions/gala_latino_slim.jpg");
        galaImages.put("LATINO_PLUS_SIZE", "/assets/images/occasions/gala_latino_plus.jpg");
        OCCASION_IMAGES.put("GALA", galaImages);
    }

    public Map<String, String> getPersonalizedImages(User user) {
        // For male users, use generic diverse images
        if (user.getGender() == Gender.MALE) {
            return getMaleGenericImages();
        }

        Map<String, String> result = new HashMap<>();

        // Convert user characteristics to lookup key
        String ethnicity = normalizeEthnicity(user.getEthnicity().name());
        String bodyType = user.getBodyType().name();
        String key = ethnicity + "_" + bodyType;

        // For each occasion, find the matching image
        for (Map.Entry<String, Map<String, String>> entry : OCCASION_IMAGES.entrySet()) {
            String occasion = entry.getKey();
            Map<String, String> images = entry.getValue();

            // Get the image path for this user's characteristics
            String imagePath = images.getOrDefault(key, getDefaultImage(occasion, ethnicity));

            result.put(occasion, imagePath);
        }

        return result;
    }

    private Map<String, String> getMaleGenericImages() {
        Map<String, String> result = new HashMap<>();

        // Add generic male images for each occasion
        result.put("DATE_NIGHT", "/assets/images/occasions/male/date_night.jpg");
        result.put("OFFICE_PARTY", "/assets/images/occasions/male/office_party.jpg");
        result.put("WEDDING_GUEST", "/assets/images/occasions/male/wedding_guest.jpg");
        result.put("CASUAL_OUTING", "/assets/images/occasions/male/casual_outing.jpg");
        result.put("FORMAL_EVENT", "/assets/images/occasions/male/formal_event.jpg");
        result.put("BEACH_VACATION", "/assets/images/occasions/male/beach_vacation.jpg");
        result.put("BUSINESS_CASUAL", "/assets/images/occasions/male/business_casual.jpg");
        result.put("PARTY", "/assets/images/occasions/male/party.jpg");
        result.put("GALA", "/assets/images/occasions/male/gala.jpg");

        return result;
    }

    /**
     * Normalizes ethnicity groups as requested:
     * - All Asian ethnicities (EAST_ASIAN, SOUTH_ASIAN, SOUTHEAST_ASIAN) -> ASIAN
     * - MIXED and MIDDLE_EASTERN -> Random selection from available ethnicities
     */
    private String normalizeEthnicity(String ethnicity) {
        if (ethnicity.contains("ASIAN")) {
            return "ASIAN";
        } else if (ethnicity.equals("MIXED") || ethnicity.equals("MIDDLE_EASTERN")) {
            // For mixed or middle eastern, randomly select an ethnicity
            return getRandomEthnicity(ethnicity);
        }

        // Return original ethnicity for WHITE, BLACK, LATINO ethnicities
        return ethnicity;
    }

    /**
     * Returns a random ethnicity for MIXED users, with more specific mapping for MIDDLE_EASTERN
     */
    private String getRandomEthnicity(String originalEthnicity) {
        if (originalEthnicity.equals("MIDDLE_EASTERN")) {
            // Middle Eastern mapped primarily to WHITE
            return "WHITE";
        } else {
            // For mixed, select randomly from the main ethnicities
            String[] ethnicities = {"WHITE", "BLACK", "LATINO", "ASIAN"};
            return ethnicities[random.nextInt(ethnicities.length)];
        }
    }

    private String getDefaultImage(String occasion, String ethnicity) {
        // Get the normalized ethnicity string
        String ethnicityPath;

        // Convert to lowercase and standardize pattern for file path
        String occasionPath = occasion.toLowerCase();

        // Use a medium body type as default
        return "/assets/images/occasions/" + occasionPath + "_" + ethnicity.toLowerCase() + "_medium.jpg";
    }
}