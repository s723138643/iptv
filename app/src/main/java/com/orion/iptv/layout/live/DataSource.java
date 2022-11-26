package com.orion.iptv.layout.live;

import com.orion.iptv.bean.ChannelInfo;
import com.orion.player.ExtDataSource;

public class DataSource {
    public final ExtDataSource dataSource;
    public final ChannelInfo channelInfo;

    public DataSource(ExtDataSource dataSource, ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
        this.dataSource = dataSource;
    }
}
