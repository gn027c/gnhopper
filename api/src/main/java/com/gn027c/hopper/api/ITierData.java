package com.gn027c.hopper.api;

import java.util.Map;

/**
 * Dữ liệu định nghĩa một tier hopper.
 * Mỗi tier có thể ghi đè tốc độ mặc định và có số filter slot riêng.
 */
public interface ITierData {

    /** Tên nội bộ của tier (ví dụ: "bronze", "silver") */
    String getName();

    /** Tên hiển thị có màu sắc (ví dụ: "&6Hopper Đồng") */
    String getDisplayName();

    /**
     * Số tick giữa mỗi lần chuyển. -1 = "auto" (dùng giá trị mặc định).
     */
    int getTransferTick();

    /**
     * Số lượng item mỗi lần chuyển. -1 = "auto".
     */
    int getTransferAmount();

    /**
     * Số loại item mỗi lần chuyển. -1 = "auto".
     */
    int getTypeOfTransfer();

    /** Số slot filter cho tier này */
    int getFilterSlots();

    /**
     * Recipe shape (3 dòng, ví dụ "III", "IHI", "III").
     * Null nếu tier không có recipe (chỉ mua bằng tiền).
     */
    String[] getRecipeShape();

    /**
     * Map ký tự → material name cho recipe.
     * Null nếu tier không có recipe.
     */
    Map<Character, String> getRecipeIngredients();

    /** Giá mua bằng economy (Vault). 0 = miễn phí. */
    double getCost();
}
