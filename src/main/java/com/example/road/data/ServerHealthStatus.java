package com.example.road.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServerHealthStatus {
    private ServerInstance serverInstance;
    private boolean healthy;
    private long lastCheckedTimestamp;
}
