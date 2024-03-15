package it.raffles.cimc.data;

import java.awt.Color;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class TableConfig {

    @Builder.Default
    private int margin = 20;

    @Builder.Default
    private int fontSize = 12;

    @Builder.Default
    private int cellHeight = 10 * 2 * 2;

    @Builder.Default
    private int cellPadding = 20;

    @Builder.Default
    private Color backgroundColor = Color.WHITE;

    @Builder.Default
    private Color headerColor = new Color(0,0,0);

    @Builder.Default
    private Color headerBackgroundColor = new Color(250, 250, 250);

    @Builder.Default
    private Color lineColor = new Color(232, 232, 232);

    @Builder.Default
    private Color textColor = new Color(89, 89, 89);

    @Builder.Default
    private Color titleColor = new Color(89, 89, 89);

    @Builder.Default
    private String fileExtension = "png";

    private String title;

}
