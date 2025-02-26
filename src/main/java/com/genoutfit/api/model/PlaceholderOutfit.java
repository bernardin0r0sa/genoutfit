package com.genoutfit.api.model;

public class PlaceholderOutfit {
    private final String outfitId;
    private final int imageIndex;
    private final String placeholderImageUrl;
    private final String occasionName;

    public PlaceholderOutfit(String outfitId, int imageIndex, String placeholderImageUrl, String occasionName) {
        this.outfitId = outfitId;
        this.imageIndex = imageIndex;
        this.placeholderImageUrl = placeholderImageUrl;
        this.occasionName = occasionName;
    }

    public String getOutfitId() {
        return outfitId;
    }

    public int getImageIndex() {
        return imageIndex;
    }

    public String getPlaceholderImageUrl() {
        return placeholderImageUrl;
    }

    public String getOccasionName() {
        return occasionName;
    }
}
