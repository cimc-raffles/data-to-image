package it.raffles.cimc.data.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CellRangeAddress {

    /**
     * 起始行  （从0起始计数）
     */
    private int firstRow;

    /**
     * 结束行
     */
    private int lastRow;

    /**
     * 起始列  （从0起计数）
     */
    private int firstCol;

    /**
     * 结束列
     */
    private int lastCol;
}
