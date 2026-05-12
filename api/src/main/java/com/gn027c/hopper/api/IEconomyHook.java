package com.gn027c.hopper.api;

/**
 * Interface hook vào hệ thống economy (Vault / EssentialsX).
 */
public interface IEconomyHook {

    /** Kiểm tra economy có khả dụng trên server không */
    boolean isAvailable();

    /**
     * Rút tiền từ tài khoản người chơi.
     * @return true nếu thành công
     */
    boolean withdraw(java.util.UUID playerId, double amount);

    /** Lấy số dư tài khoản người chơi */
    double getBalance(java.util.UUID playerId);

    /** Format số tiền theo economy provider */
    String format(double amount);
}
