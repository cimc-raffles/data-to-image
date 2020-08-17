package it.raffles.cimc.data.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CellEntity {

	private int rowIndex;

	private int columnIndex;

	private int startX;

	private int startY;

	private int endX;

	private int endY;

	private Integer width;

	private Integer height;

	private Integer rowSpan;

	private Integer colSpan;

	private ColumnEntity headerEntity;

	private String key;

	private Object value;

	private Boolean isHeader;

}
