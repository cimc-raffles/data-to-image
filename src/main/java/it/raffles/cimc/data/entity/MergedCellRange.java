package it.raffles.cimc.data.entity;

import lombok.Data;

@Data
public class MergedCellRange {

    private Integer rowIndex;

    private Integer columnIndex;

    private CellRangeAddress address;
}
