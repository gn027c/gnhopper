package com.gn027c.hopper.paper.hopper;

/**
 * Tracking state of an active hopper.
 */
public class HopperState {
    private int tickCounter;
    private String tierName;
    
    // Hibernation feature (optimization)
    private boolean hibernating;
    private int failCount;
    private long lastWakeUp;

    private final java.util.LinkedList<String> transferHistory = new java.util.LinkedList<>();
    private final long createdAt;

    public HopperState() {
        this.tickCounter = 0;
        this.tierName = null;
        this.hibernating = false;
        this.failCount = 0;
        this.lastWakeUp = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
    }

    public void addHistory(String entry) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        transferHistory.addFirst("[" + time + "] " + entry);
        if (transferHistory.size() > 10) {
            transferHistory.removeLast();
        }
    }

    public java.util.List<String> getHistory() {
        return transferHistory;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public void incrementTick() {
        tickCounter++;
    }

    public void resetTick() {
        tickCounter = 0;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public boolean hasTier() {
        return tierName != null;
    }

    // Hibernation management
    public boolean isHibernating() {
        return hibernating;
    }

    public void setHibernating(boolean hibernating) {
        this.hibernating = hibernating;
        if (!hibernating) {
            this.failCount = 0;
            this.lastWakeUp = System.currentTimeMillis();
        }
    }

    public void incrementFail() {
        failCount++;
    }

    public int getFailCount() {
        return failCount;
    }

    public void resetFail() {
        failCount = 0;
    }
}
