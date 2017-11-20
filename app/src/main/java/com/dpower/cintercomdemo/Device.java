package com.dpower.cintercomdemo;

/**
 * 设备类
 */
public class Device {
    private String roomNumber; // 房号
    private String id; // 设备编号
    private String note; // 设备备注
    private String account; // 设备账号

    public Device(String roomNumber, String id, String note, String account) {
        this.roomNumber = roomNumber;
        this.id = id;
        this.note = note;
        this.account = account;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getId() {
        return id;
    }

    public String getNote() {
        return note;
    }

    public String getAccount() {
        return account;
    }
}
