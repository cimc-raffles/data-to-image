package it.raffles.cimc.data;

import it.raffles.cimc.data.entity.ColumnEntity;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class DataConverter {

    private List<List<Object>> data;

    private List<ColumnEntity> headers;

    private TableConfig tableConfig;

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

    public BufferedImage toImage() throws IOException {

        this.tableConfig = this.getTableConfig();
        int cellHeight = tableConfig.getCellHeight();

        int rows = data.size();

        // Custom column widths
        int[] columnWidths = this.headers.stream().mapToInt(ColumnEntity::getWidth).toArray();

        // Calculate the total height including header row and title row
        int totalHeight = (rows + 2) * cellHeight;

        // Calculate total width
        int totalWidth = 0;
        for (int width : columnWidths) {
            totalWidth += width;
        }

        // Create a blank image with white background
        BufferedImage image = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        // background
        graphics.setBackground(this.tableConfig.getBackgroundColor());
        graphics.fillRect(0, 0, totalWidth, totalHeight);

        // title
        drawTitle(graphics, totalWidth);

        // grid
        drawGrid(graphics, totalWidth, totalHeight, cellHeight, columnWidths);

        // headers
        drawHeaders(graphics, cellHeight, columnWidths);

        // cells
        drawCells(graphics, cellHeight, columnWidths);

        // draw
        graphics.drawImage(image.getScaledInstance(totalWidth, totalHeight, Image.SCALE_SMOOTH), 0, 0, null);
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


    // 绘制表格标题
    private void drawTitle(Graphics2D graphics, int totalWidth) {
        String title = this.tableConfig.getTitle();
        if (null == title || title.isEmpty())
            return;

        FontMetrics fontMetrics = graphics.getFontMetrics();
        Font titleFont = fontMetrics.getFont().deriveFont(Font.BOLD, (float) (this.tableConfig.getFontSize() + 2));
        graphics.setFont(titleFont);
        graphics.setColor(this.tableConfig.getTitleColor());

        int titleWidth = fontMetrics.stringWidth(title);
        int titleX = (totalWidth - titleWidth) / 2;
        int titleY = (this.tableConfig.getCellHeight() - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();

        setAntialiasing(graphics);
        graphics.drawString(title, titleX, titleY);
        removeAntialiasing(graphics);
    }

    // 绘制表格网格线
    private void drawGrid(Graphics2D graphics, int totalWidth, int totalHeight, int cellHeight, int[] columnWidths) {
        graphics.setColor(this.tableConfig.getLineColor());
        // Draw horizontal lines
        for (int i = 1; i <= totalHeight / cellHeight; i++) {
            graphics.drawLine(0, i * cellHeight, totalWidth, i * cellHeight);
        }
        graphics.drawLine(0, totalHeight - 1, totalWidth, totalHeight - 1);


        // Draw vertical lines
        int xPosition = 0;
        for (int width : columnWidths) {
            graphics.drawLine(xPosition, cellHeight, xPosition, totalHeight);
            xPosition += width;
        }
        graphics.drawLine(totalWidth - 1, cellHeight, totalWidth - 1, totalHeight);

    }

    // 绘制表头
    private void drawHeaders(Graphics2D graphics, int cellHeight, int[] columnWidths) {
        FontMetrics fontMetrics = graphics.getFontMetrics();
        Font headerFont = fontMetrics.getFont().deriveFont(Font.BOLD, (float) (tableConfig.getFontSize() + 1));
        graphics.setFont(headerFont);

        int xStart = 0;

        for (int i = 0; i < this.headers.size(); i++) {
            String header = this.headers.get(i).getName();
            int stringWidth = graphics.getFontMetrics().stringWidth(header);
            int x = xStart + (columnWidths[i] - stringWidth) / 2;
            int y = cellHeight + cellHeight / 2 + graphics.getFontMetrics().getAscent() / 2; // Vertically center the header

            // Set header background color
            graphics.setColor(this.tableConfig.getHeaderBackgroundColor());
            graphics.fillRect(xStart + 1, cellHeight + 1, columnWidths[i] - (i + 1 == this.headers.size() ? 2 : 1), cellHeight - 1);

            // Set text color
            graphics.setColor(this.tableConfig.getHeaderColor());
            graphics.drawString(header, x, y);
            xStart += columnWidths[i]; // Move to the start of the next column
        }
    }

    // 绘制单元格内容
    private void drawCells(Graphics2D graphics, int cellHeight, int[] columnWidths) {
        FontMetrics fontMetrics = graphics.getFontMetrics();
        Font cellFont = fontMetrics.getFont().deriveFont((float) (tableConfig.getFontSize()));
        graphics.setFont(cellFont);
        graphics.setColor(this.tableConfig.getTextColor());

        int cellPadding = this.tableConfig.getCellPadding();

        for (int i = 0; i < this.data.size(); i++) {
            int xContentStart = 0; // Reset xStart for each row
            for (int j = 0; j < this.data.get(i).size(); j++) {
                String cellContent = String.valueOf(this.data.get(i).get(j));
                int stringWidth = graphics.getFontMetrics().stringWidth(cellContent);
                int x;
                switch (this.headers.get(j).getAlignment()) {
                    case CENTER:
                        x = xContentStart + (columnWidths[j] - stringWidth) / 2;
                        break;
                    case RIGHT:
                        x = xContentStart + columnWidths[j] - stringWidth - cellPadding;
                        break;
                    default:
                        x = xContentStart + cellPadding;
                }
                int y = (i + 2) * cellHeight + cellHeight / 2 + graphics.getFontMetrics().getAscent() / 2; // Vertically center the content
                graphics.drawString(cellContent, x, y);
                xContentStart += columnWidths[j]; // Move to the start of the next column
            }
        }
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
