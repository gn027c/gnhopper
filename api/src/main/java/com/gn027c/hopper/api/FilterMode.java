package com.gn027c.hopper.api;

/**
 * Chế độ lọc item cho hopper.
 */
public enum FilterMode {
    /**
     * Dùng item frame gắn lên hopper để lọc.
     * Mặc định cho phép lọc 1 loại item.
     */
    ITEM_FRAME,

    /**
     * Dùng giao diện GUI trong game.
     * Hỗ trợ nâng cấp tier, custom filter slots.
     */
    GUI
}
