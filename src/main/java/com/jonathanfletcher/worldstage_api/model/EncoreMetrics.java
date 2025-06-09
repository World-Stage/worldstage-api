package com.jonathanfletcher.worldstage_api.model;

import lombok.Builder;
import lombok.Data;


@Builder
@Data
public class EncoreMetrics {
    private int encoreTotal;

    private Integer encoreNeeded;

    private int encoreProgressPercent;
}
