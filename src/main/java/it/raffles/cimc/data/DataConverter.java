package it.raffles.cimc.data;

import it.raffles.cimc.data.entity.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataConverter {

    private List<List<Object>> data;

    private List<ColumnEntity> headers;

    private TableConfig tableConfig;

    private final List<CellEntity> computedHeaders = new ArrayList<>();

    public DataConverter() {
    }

    public DataConverter(List<ColumnEntity> headers, List<List<Object>> data) {
        this.headers = headers;
        this.data = data;
    }

    public DataConverter(List<ColumnEntity> headers, List<List<Object>> data, TableConfig tableConfig) {
        this.headers = headers;
        this.data = data;
        this.tableConfig = tableConfig;
    }

    public BufferedImage toImage() {

        this.tableConfig = this.getTableConfig();
        int cellHeight = tableConfig.getCellHeight();

        // header
        drawHeaders();

        // title
        drawTitle();

        int rows = data.size();

        // Custom column widths
        int[] columnWidths = this.headers.stream().mapToInt(ColumnEntity::getWidth).toArray();

        // Calculate the total height including header row and title row
        int totalHeight = rows * cellHeight + 1;

        // Calculate total width
        int totalWidth = 1;
        for (int width : columnWidths) {
            totalWidth += width;
        }

        // Create a blank image with white background
        BufferedImage image = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        // background
        graphics.setBackground(this.tableConfig.getBackgroundColor());
        graphics.fillRect(0, 0, totalWidth, totalHeight);

        setAntialiasing(graphics);

        // grid
        drawGrid(graphics);

        //merge
        if (this.tableConfig.getMergedRegions() != null)
            mergeCells(graphics);

        // draw
        removeAntialiasing(graphics);

//        graphics.drawImage(image.getScaledInstance(totalWidth, totalHeight, Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();

        return this.tableConfig.getMargin() > 0 ? setImageMargin(image, this.tableConfig.getMargin()) : image;
    }

    public byte[] toImageData() throws IOException {

        BufferedImage image = this.toImage();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, this.getTableConfig().getFileExtension(), output);
        output.flush();

        byte[] imageBytes = output.toByteArray();
        output.close();

        return imageBytes;
    }


    public void addMergedRegion(CellRangeAddress address) {
        if (null == this.tableConfig.getMergedRegions())
            this.tableConfig.setMergedRegions(new ArrayList<>());
        this.tableConfig.getMergedRegions().add(address);
    }


    private void drawHeaders() {

        List<Object> childrenData = new ArrayList<>();
        boolean hasChildren = false;
        for (ColumnEntity header : this.headers) {
            CellEntity cellData = CellEntity.builder().isHeader(true).alignment(Alignment.CENTER).value(header.getName()).width(header.getWidth()).fontSize(this.tableConfig.getHeaderFontSize()).color(this.tableConfig.getHeaderColor()).backgroundColor(this.tableConfig.getHeaderBackgroundColor()).build();
            if (header.getChildren() != null) {
                hasChildren = true;
                List<ColumnEntity> children = header.getChildren();
                for (ColumnEntity childData : children) {
                    CellEntity childCellData = CellEntity.builder().isHeader(true).alignment(Alignment.CENTER).parent(cellData).value(childData.getName()).width(childData.getWidth()).fontSize(this.tableConfig.getHeaderFontSize()).color(this.tableConfig.getHeaderColor()).backgroundColor(this.tableConfig.getHeaderBackgroundColor()).build();
                    childrenData.add(childCellData);
                    this.computedHeaders.add(childCellData);
                }
            } else {
                childrenData.add(cellData);
                this.computedHeaders.add(cellData);
            }
        }

        this.data.add(0, childrenData);

        if (!hasChildren)
            return;

        List<Object> parentData = new ArrayList<>();
        for (Object childrenDatum : childrenData) {
            CellEntity childData = (CellEntity) childrenDatum;
            if (null == childData.getParent()) {
                parentData.add(childData);
            } else {
                parentData.add(childData.getParent());
            }
        }
        this.data.add(0, parentData);
    }

    private void drawGrid(Graphics2D graphics) {

        for (int i = 0; i < data.size(); ++i) {
            List<Object> rowData = data.get(i);

            int xContentStart = 0; // Reset xStart for each row

            int cellHeight = this.tableConfig.getCellHeight();
//            int offsetHeight = null == this.tableConfig.getTitle() && null == this.tableConfig.getTitles() ? 0 : cellHeight;

            for (int j = 0; j < rowData.size(); ++j) {
                Object vo = rowData.get(j);
                CellEntity cellData = isCellEntity(vo) ? (CellEntity) vo : CellEntity.builder().value(vo).build();
                int width = this.computedHeaders.get(j).getWidth();
                int[] bound = new int[]{xContentStart, i * cellHeight, xContentStart + width, (i + 1) * cellHeight};
                cellData.setRowIndex(i);
                cellData.setColumnIndex(j);
                cellData.setBound(bound);
                cellData.setHeight(cellHeight);
                cellData.setWidth(width);

                rowData.set(j, cellData);

                graphics.setColor(this.tableConfig.getLineColor());
                if (i == 0)
                    graphics.drawLine(bound[0], bound[1], bound[2], bound[1]);
                if (j == 0)
                    graphics.drawLine(bound[0], bound[1], bound[0], bound[3]);

                if (cellData.getBorder() == null || !Border.NO_BOTTOM.equals(cellData.getBorder()))
                    graphics.drawLine(bound[0], bound[3], bound[2], bound[3]);

                if (cellData.getBorder() == null || !Border.NO_RIGHT.equals(cellData.getBorder()))
                    graphics.drawLine(bound[2], bound[1], bound[2], bound[3]);

                String value = getCellValue(cellData);


                drawText(graphics, cellData, xContentStart, i * cellHeight, width, cellHeight, this.computedHeaders.get(j).getAlignment());
                xContentStart += width; // Move to the start of the next column

                graphics.setColor(this.tableConfig.getTextColor());
                this.resetFontPlain(graphics);
            }
        }
    }

    //绘制文字
    private void drawText(Graphics2D graphics, Object cellData, int x, int y, int width, int height, Alignment alignment) {

        int cellPadding = this.tableConfig.getCellPadding();
        String cellContent = getCellValue(cellData);

        int cellFontSize = this.tableConfig.getFontSize();
        graphics.setFont(graphics.getFontMetrics().getFont().deriveFont(Font.PLAIN, cellFontSize));

        if (cellData instanceof CellEntity) {
            if (null != ((CellEntity) cellData).getFontSize()) {
                cellFontSize = ((CellEntity) cellData).getFontSize();
                graphics.setFont(graphics.getFontMetrics().getFont().deriveFont(Font.PLAIN, cellFontSize));
            }
            if (((CellEntity) cellData).isHeader()) {
                alignment = Alignment.CENTER;
                int headerFontSize = null == this.tableConfig.getHeaderFontSize() ? tableConfig.getFontSize() + 1 : this.tableConfig.getHeaderFontSize();
                graphics.getFontMetrics().getFont().deriveFont(Font.BOLD, headerFontSize);
            }
        }

        int stringWidth = graphics.getFontMetrics().stringWidth(cellContent);
        int xContent;
        switch (alignment) {
            case CENTER:
                xContent = x + (width - stringWidth) / 2;
                break;
            case RIGHT:
                xContent = x + width - stringWidth - cellPadding;
                break;
            default:
                xContent = x + cellPadding;
        }
        int yContent = y + height / 2 + graphics.getFontMetrics().getAscent() / 2; // Vertically center the content

        if (isCellEntity(cellData)) {
            // draw cell background
            if (null != ((CellEntity) cellData).getBackgroundColor()) {
                Color cellBackgroundColor = ((CellEntity) cellData).getBackgroundColor();
                graphics.setColor(cellBackgroundColor);
                graphics.fillRect(x + 1, y, width - 1, height);
//                graphics.setColor(this.tableConfig.getLineColor());
//                graphics.drawRect(x, y, width, height);
            }
            if (null != ((CellEntity) cellData).getColor()) {
                graphics.setColor(((CellEntity) cellData).getColor());
            } else {
                graphics.setColor(this.tableConfig.getTextColor());
            }
        }

        if (isCellEntityList(((CellEntity) cellData).getValue())) {
            List<CellEntity> values = (List<CellEntity>) ((CellEntity) cellData).getValue();
            for (CellEntity cell : values) {
                Color cellColor = cell.getColor();
                if (null != cellColor)
                    graphics.setColor(cellColor);

                String cellValue = String.valueOf(cell.getValue());

                graphics.drawString(cellValue, xContent, yContent);
                xContent += graphics.getFontMetrics().stringWidth(cellValue);

                //reset title color
                graphics.setColor(this.tableConfig.getTitleColor());
            }
        } else {
            graphics.drawString(cellContent, xContent, yContent);

        }

        // reset font color
        graphics.setColor(this.tableConfig.getTextColor());
        graphics.setFont(graphics.getFontMetrics().getFont().deriveFont(Font.PLAIN));
    }

    // 绘制表格标题
    private void drawTitle() {
        Object title = null == this.tableConfig.getTitles() ? this.tableConfig.getTitle() : this.tableConfig.getTitles();
        if (null == title)
            return;
        List<Object> items = new ArrayList<>();
        for (CellEntity ignored : this.computedHeaders) {
            items.add(CellEntity.builder().isHeader(true).value(title).build());
        }
        this.data.add(0, items);
        this.addMergedRegion(CellRangeAddress.builder().firstRow(0).lastRow(0).firstCol(0).lastCol(this.computedHeaders.size() - 1).build());
    }


    // set image margin
    private BufferedImage setImageMargin(BufferedImage image, int margin) {
        int newWidth = image.getWidth() + 2 * margin;
        int newHeight = image.getHeight() + 2 * margin;
        // Create a new BufferedImage with increased dimensions
        BufferedImage imageWithMargin = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = imageWithMargin.createGraphics();

        // Set background color for the margin
        graphics.setColor(this.tableConfig.getBackgroundColor()); // Replace with desired color
        graphics.fillRect(0, 0, newWidth, newHeight);

        // Draw the original image onto the new BufferedImage with the desired margin
        graphics.drawImage(image, margin, margin, null);

        // Dispose of the Graphics2D object
        graphics.dispose();

        return imageWithMargin;
    }

    private boolean isCellEntity(Object cellData) {
        return cellData instanceof CellEntity;
    }


    public static boolean isCellEntityList(Object cellData) {
        return cellData instanceof List && ((List<?>) cellData).stream().allMatch(item -> item instanceof CellEntity);
    }

    private String getCellValue(Object cellData) {
        if (cellData instanceof String)
            return String.valueOf(cellData);
        if (cellData instanceof CellEntity) {
            Object value = ((CellEntity) cellData).getValue();
            if (value instanceof String)
                return String.valueOf(value);
            if (value instanceof CellEntity)
                return String.valueOf(((CellEntity) value).getValue());
            if (isCellEntityList(value))
                return ((List<CellEntity>) value).stream().map(item -> String.valueOf(item.getValue())).collect(Collectors.joining());
        }
        return String.valueOf(isCellEntity(cellData) ? ((CellEntity) cellData).getValue() : cellData);
    }

    private void mergeCells(Graphics2D graphics) {
        for (CellRangeAddress range : this.tableConfig.getMergedRegions()) {
            graphics.setColor(this.tableConfig.getLineColor());
            CellEntity cell = getMergedCell(range);
            int[] bound = cell.getBound();

            int width = bound[2] - bound[0];
            int height = bound[3] - bound[1];
            graphics.clearRect(bound[0], bound[1], width, height);
            graphics.drawRect(bound[0], bound[1], width, height);

            if (cell.isHeader()) {
                graphics.setFont(this.getHeaderFont(graphics));
            }

            drawText(graphics, this.data.get(range.getFirstRow()).get(range.getFirstCol()), bound[0], bound[1], width, height, Alignment.LEFT);
            graphics.setColor(this.tableConfig.getLineColor());
        }
    }

    /**
     * 获取合并的单元格宽度
     *
     * @return 合并的单元格宽度
     */
    private CellEntity getMergedCell(CellRangeAddress mergedRegion) {

        CellEntity firstCell = toCellEntity(this.data.get(mergedRegion.getFirstRow()).get(mergedRegion.getFirstCol()));
        CellEntity lastCell = toCellEntity(this.data.get(mergedRegion.getLastRow()).get(mergedRegion.getLastCol()));

        int[] firstBound = firstCell.getBound();
        int[] lastBound = lastCell.getBound();

        int[] bound = new int[]{firstBound[0], firstBound[1], lastBound[2], lastBound[3]};

        return CellEntity.builder().bound(bound).value(firstCell.getValue()).isHeader(firstCell.isHeader()).build();
    }

    private CellEntity toCellEntity(Object item) {
        return item instanceof CellEntity ? (CellEntity) item : CellEntity.builder().value(item).build();
    }

    private Font getHeaderFont(Graphics2D graphics) {
        FontMetrics fontMetrics = graphics.getFontMetrics();
        int headerFontSize = null == this.tableConfig.getHeaderFontSize() ? tableConfig.getFontSize() + 1 : this.tableConfig.getHeaderFontSize();
        return fontMetrics.getFont().deriveFont(Font.BOLD, (float) headerFontSize);
    }

    private void resetFontPlain(Graphics2D graphics) {
        graphics.setFont(graphics.getFont().deriveFont(Font.PLAIN));
    }

    // activate antialiasing and fractional metrics
    private void setAntialiasing(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    // turn off antialiasing for higher visual precision of the lines
    private void removeAntialiasing(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    public List<List<Object>> getData() {
        return data;
    }

    public void setData(List<List<Object>> data) {
        this.data = data;
    }

    public List<ColumnEntity> getHeaders() {
        return headers;
    }

    public void setHeaders(List<ColumnEntity> headers) {
        this.headers = headers;
    }

    public TableConfig getTableConfig() {
        return null == this.tableConfig ? new TableConfig() : this.tableConfig;
    }

    public void setTableConfig(TableConfig tableConfig) {
        this.tableConfig = null == tableConfig ? new TableConfig() : tableConfig;
    }

}
