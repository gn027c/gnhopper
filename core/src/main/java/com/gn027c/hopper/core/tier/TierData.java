package com.gn027c.hopper.core.tier;

import com.gn027c.hopper.api.ITierData;

import java.util.Map;

/**
 * Default implementation for ITierData.
 * Created from config.yml.
 */
public class TierData implements ITierData {
    private final String name;
    private final String displayName;
    private final int transferTick;      // -1 = auto
    private final int transferAmount;    // -1 = auto
    private final int typeOfTransfer;    // -1 = auto
    private final int filterSlots;
    private final String[] recipeShape;
    private final Map<Character, String> recipeIngredients;
    private final double cost;

    public TierData(String name, String displayName,
                    int transferTick, int transferAmount, int typeOfTransfer,
                    int filterSlots,
                    String[] recipeShape, Map<Character, String> recipeIngredients,
                    double cost) {
        this.name = name;
        this.displayName = displayName;
        this.transferTick = transferTick;
        this.transferAmount = transferAmount;
        this.typeOfTransfer = typeOfTransfer;
        this.filterSlots = filterSlots;
        this.recipeShape = recipeShape;
        this.recipeIngredients = recipeIngredients;
        this.cost = cost;
    }

    @Override public String getName() { return name; }
    @Override public String getDisplayName() { return displayName; }
    @Override public int getTransferTick() { return transferTick; }
    @Override public int getTransferAmount() { return transferAmount; }
    @Override public int getTypeOfTransfer() { return typeOfTransfer; }
    @Override public int getFilterSlots() { return filterSlots; }
    @Override public String[] getRecipeShape() { return recipeShape; }
    @Override public Map<Character, String> getRecipeIngredients() { return recipeIngredients; }
    @Override public double getCost() { return cost; }

    /**
     * Checks if the value is "auto".
     * -1 = auto (uses global default value).
     */
    public boolean isAutoTransferTick() { return transferTick == -1; }
    public boolean isAutoTransferAmount() { return transferAmount == -1; }
    public boolean isAutoTypeOfTransfer() { return typeOfTransfer == -1; }
    public boolean hasRecipe() { return recipeShape != null && recipeIngredients != null; }
}
