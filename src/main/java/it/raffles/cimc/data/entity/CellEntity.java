package it.raffles.cimc.data.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CellEntity {

    private Object value;

    private Color color;

    private Alignment alignment;

    private Color backgroundColor;

    private Integer fontSize;

    private int rowIndex;

    private int columnIndex;

    private int width;

    private int height;

    private int[] bound;

    private int mergedRowIndex;

    private int mergedColumnIndex;

    private CellEntity parent;

    private boolean isHeader;

}
