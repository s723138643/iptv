package com.orion.iptv.bean;

public class ChannelInfo {
    public final GroupInfo groupInfo;
    public final int channelNumber;
    public final String channelName;

    public ChannelInfo(int channelNumber, String channelName, GroupInfo groupInfo) {
        this.channelNumber = channelNumber;
        this.channelName = channelName;
        this.groupInfo = groupInfo;
    }
}
