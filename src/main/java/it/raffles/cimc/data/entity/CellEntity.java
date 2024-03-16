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

    private Color backgroundColor;

    private Integer fontSize;

}
