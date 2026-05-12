package com.gn027c.hopper.api;

/**
 * Interface cấu hình tốc độ hopper.
 */
public interface IHopperConfig {

    /**
     * Số tick giữa mỗi lần chuyển item.
     * Vanilla default: 8 tick (0.4 giây).
     * Càng thấp càng nhanh.
     */
    int getTransferTick();

    /**
     * Số lượng item tối đa mỗi lần chuyển.
     * Vanilla default: 1.
     * Tối đa: 320 (5 stack x 64).
     */
    int getTransferAmount();

    /**
     * Số loại item mỗi lần chuyển.
     * Vanilla default: 1.
     * 0 = lấy tất cả loại cho đến khi đạt transferAmount.
     */
    int getTypeOfTransfer();

    String getString(String path);
    String getString(String path, String def);
    int getInt(String path);
    int getInt(String path, int def);
    boolean getBoolean(String path);
    boolean getBoolean(String path, boolean def);
    java.util.List<String> getStringList(String path);
    Object get(String path);
}
