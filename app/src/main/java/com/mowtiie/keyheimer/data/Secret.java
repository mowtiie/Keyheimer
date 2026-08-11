package com.mowtiie.keyheimer.data;

public class Secret {

    public enum IntervalUnit {
        DAY,
        WEEK,
        MONTH
    }

    private String id;
    private String name;
    private byte[] salt;
    private String hash;
    private int iterations;
    private String hint;
    private int intervalValue;
    private IntervalUnit intervalUnit;
    private long nextTriggerAt;
    private Long lastVerifiedAt;
    private int successCount;
    private int failCount;
    private boolean active;
    private long createdAt;
    private long updatedAt;
    private int reminderHour;
    private int reminderMinute;

    public Secret() {
    }

    public Secret(String id, String name, byte[] salt, String hash, int iterations,
                  String hint, int intervalValue, IntervalUnit intervalUnit,
                  long nextTriggerAt, Long lastVerifiedAt,
                  int successCount, int failCount, boolean active,
                  long createdAt, long updatedAt, int reminderHour, int reminderMinute) {
        this.id = id;
        this.name = name;
        this.salt = salt;
        this.hash = hash;
        this.iterations = iterations;
        this.hint = hint;
        this.intervalValue = intervalValue;
        this.intervalUnit = intervalUnit;
        this.nextTriggerAt = nextTriggerAt;
        this.lastVerifiedAt = lastVerifiedAt;
        this.successCount = successCount;
        this.failCount = failCount;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.reminderHour = reminderHour;
        this.reminderMinute = reminderMinute;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public int getIntervalValue() {
        return intervalValue;
    }

    public void setIntervalValue(int intervalValue) {
        this.intervalValue = intervalValue;
    }

    public IntervalUnit getIntervalUnit() {
        return intervalUnit;
    }

    public void setIntervalUnit(IntervalUnit intervalUnit) {
        this.intervalUnit = intervalUnit;
    }

    public long getNextTriggerAt() {
        return nextTriggerAt;
    }

    public void setNextTriggerAt(long nextTriggerAt) {
        this.nextTriggerAt = nextTriggerAt;
    }

    public Long getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(Long lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getReminderHour() {
        return reminderHour;
    }

    public void setReminderHour(int reminderHour) {
        this.reminderHour = reminderHour;
    }

    public int getReminderMinute() {
        return reminderMinute;
    }

    public void setReminderMinute(int reminderMinute) {
        this.reminderMinute = reminderMinute;
    }
}