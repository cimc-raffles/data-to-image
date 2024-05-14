package it.raffles.cimc.data.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ColumnEntity {

    private String key;

    private String name;

    private Integer width;

    private Alignment alignment;

    private List<ColumnEntity> children;
}
