package org.example.longpulling;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfigEntity {
    private String serviceName;
    private Long timestamp;
}
