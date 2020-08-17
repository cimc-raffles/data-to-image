package it.raffles.cimc.data;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import it.raffles.cimc.data.entity.CellEntity;
import it.raffles.cimc.data.entity.ColumnEntity;

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

		TableConfig tableConfig = this.getTableConfig();

		// bound
		int width = tableConfig.getWidth();
		int height = tableConfig.getHeight();

		int span = tableConfig.getSpan();
		int cellHeight = tableConfig.getCellHeight();

		int textFontSize = tableConfig.getFontSize();

		int dataRows = data.size();

		Map<Integer, Integer> columnWidthMap = new HashMap<>();
		List<List<CellEntity>> cells = getCellEntities(headers, data, columnWidthMap);

		int rows = cells.size();
		int headerRows = rows - dataRows;

		int columns = cells.stream().mapToInt(List::size).reduce(Integer::max).getAsInt();

		int cellWidth = (int) Math.floor((width - span - span) / columns);
		if (null != columnWidthMap && !columnWidthMap.isEmpty()) {
			cellWidth = (int) Math
					.floor((width - span - span - columnWidthMap.values().stream().reduce(Integer::sum).get())
							/ (columns - columnWidthMap.size()));
		}

		String title = tableConfig.getTitle();

		boolean hasTitle = null != title && 0 < title.length();

		int titleHeight = hasTitle ? cellHeight : 0;
		height = span + titleHeight + cellHeight * rows + span;

		// graphics
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();

		// font
//		GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
//		try {
//			Font font = Font.createFont(Font.TRUETYPE_FONT,
//					this.getClass().getClassLoader().getResourceAsStream("fonts/NotoSansCJKsc-Regular.otf"));
//			environment.registerFont(font);
//		} catch (FontFormatException | IOException e) {
//			e.printStackTrace();
//		}

		FontMetrics fontMetrics = graphics.getFontMetrics();
		Font titleFont = fontMetrics.getFont().deriveFont(Font.BOLD, (float) (textFontSize + 2));
		Font headerFont = fontMetrics.getFont().deriveFont(Font.BOLD, (float) textFontSize);
		Font cellFont = fontMetrics.getFont().deriveFont((float) textFontSize);

		Color headerColor = tableConfig.getHeaderColor();
		Color lineColor = tableConfig.getLineColor();
		Color textColor = tableConfig.getTextColor();
		Color titleColor = tableConfig.getTitleColor();

		// background
		graphics.setBackground(Color.WHITE);
		graphics.fillRect(0, 0, width, height);

		// title
		if (null != title && 0 < title.length()) {
			graphics.setFont(titleFont);
			graphics.setColor(titleColor);

			Rectangle2D stringBounds = fontMetrics.getStringBounds(title, graphics);

			setAntialiasing(graphics);
			graphics.drawString(title, (int) (width - span - stringBounds.getWidth()) / 2,
					span + titleHeight / 2 + fontMetrics.getHeight() / 2);
			removeAntialiasing(graphics);
		}

		// table
		int headerStartY = span + titleHeight;
		int headerHeight = cellHeight * headerRows;
		graphics.setColor(headerColor);
		graphics.fillRect(span, headerStartY, width - span - span, headerHeight);

		for (int x = 0; x < cells.size(); ++x) {
			int rowIndex = x;
			List<CellEntity> record = cells.get(x);

			int lastRightTopX = span;
			for (int y = 0; y < record.size(); ++y) {
				int columnIndex = y;
				CellEntity cell = record.get(y);
				Integer currentCellWidth = columnWidthMap.get(columnIndex);
				if (null == currentCellWidth)
					currentCellWidth = cellWidth;

				// coordinate
				int leftTopX = lastRightTopX;
				int leftTopY = headerStartY + cellHeight * rowIndex;

				int leftBottomX = leftTopX;
				int leftBottomY = leftTopY + cellHeight;

				int rightTopX = leftTopX + currentCellWidth;

				if (null != cell.getColSpan()) {
					ColumnEntity headerEntity = cell.getHeaderEntity();
					List<ColumnEntity> headerChildren = headerEntity.getChildren();
					if (null != headerChildren && !headerChildren.isEmpty()) {
						currentCellWidth = 0;
						ColumnEntity[] noWidthHeaders = headerChildren.stream().filter(item -> null == item.getWidth())
								.toArray(ColumnEntity[]::new);
						ColumnEntity[] hasWidthHeaders = headerChildren.stream().filter(item -> null != item.getWidth())
								.toArray(ColumnEntity[]::new);
						if (hasWidthHeaders.length > 0)
							currentCellWidth += Arrays.stream(hasWidthHeaders).mapToInt(ColumnEntity::getWidth)
									.reduce(Integer::sum).getAsInt();
						if (noWidthHeaders.length > 0)
							currentCellWidth += cellWidth * noWidthHeaders.length;
						rightTopX = leftTopX + currentCellWidth;
					}
				}

				if (columnIndex + 1 == record.size())
					rightTopX = width - span;

				int rightTopY = leftTopY;

				int rightBottomX = rightTopX;
				int rightBottomY = leftBottomY;

				lastRightTopX = rightTopX;

				graphics.setColor(lineColor);
				// left vertical line
				if (0 == columnIndex)
					graphics.drawLine(leftTopX, leftTopY, leftBottomX, leftBottomY);

				// right vertical line
				graphics.drawLine(rightTopX, rightTopY, rightBottomX, rightBottomY);

				// top line
				if (0 == rowIndex)
					graphics.drawLine(leftTopX, leftTopY, rightTopX, rightTopY);

				// bottom line
				if (null == cell.getRowSpan())
					graphics.drawLine(leftBottomX, leftBottomY, rightBottomX, rightBottomY);

				// text
				String text = null == cell.getValue() ? "" : String.valueOf(cell.getValue());
				graphics.setColor(textColor);

				graphics.setFont(null != cell.getIsHeader() && cell.getIsHeader() ? headerFont : cellFont);

				setAntialiasing(graphics);
				int textX = leftBottomX + span / 2;
				int textY = leftBottomY - cellHeight / 2 + fontMetrics.getHeight() / 2;
				Integer textRowSpan = cell.getRowSpan();
				Integer textColSpan = cell.getColSpan();

				if (null != textRowSpan) {
					textY = leftBottomY + cellHeight * (textRowSpan - 1) - cellHeight * textRowSpan / 2
							+ fontMetrics.getHeight() / 2;
				}
				if (null != textColSpan) {
					Rectangle2D stringBounds = fontMetrics.getStringBounds(text, graphics);
					textX = (int) (leftBottomX + currentCellWidth / 2 - stringBounds.getWidth() / 2d);
				}
				graphics.drawString(text, textX, textY);
				removeAntialiasing(graphics);
			}
		}

		// draw
		graphics.drawImage(image.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
		graphics.dispose();

		return image;
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

	private static List<List<CellEntity>> getCellEntities(List<ColumnEntity> headers, List<List<Object>> data,
			Map<Integer, Integer> columnWidthMap) {

		int size = headers.size();
		List<CellEntity> newRow = new ArrayList<>();
		List<List<CellEntity>> cells = new ArrayList<>();
		cells.add(newRow);

		for (int x = 0; x < size; ++x)
			newRow.add(new CellEntity());

		int columnIndex = 0;
		int rowIndex = 0;
		for (ColumnEntity x : headers) {
			reduceHeaderChildrenRows(rowIndex, columnIndex, x, cells, columnWidthMap);
			++columnIndex;
		}

		int headerRows = cells.size();
		for (int x = 0; x < data.size(); ++x) {
			List<CellEntity> newDataRow = new ArrayList<>();
			cells.add(newDataRow);
			for (int y = 0; y < data.get(x).size(); ++y) {
				CellEntity newCell = CellEntity.builder().isHeader(false).rowIndex(headerRows + x).columnIndex(y)
						.value(data.get(x).get(y)).build();
				newDataRow.add(newCell);
			}
		}

		return cells;
	}

	// TODO: reduce
	public static void reduceHeaderChildrenRows(int rowIndex, int columnIndex, ColumnEntity header,
			List<List<CellEntity>> headerCells, Map<Integer, Integer> columnWidthMap) {

		List<CellEntity> currentRow = headerCells.get(rowIndex);
		CellEntity currentCell = currentRow.get(columnIndex);
		if (null == currentCell)
			currentCell = new CellEntity();
		currentCell.setHeaderEntity(header);
		currentCell.setColumnIndex(columnIndex);
		currentCell.setRowIndex(rowIndex);
		currentCell.setValue(header.getName());

		currentCell.setRowSpan(null);
		currentCell.setIsHeader(true);

		Integer currentCellWidth = header.getWidth();
		if (null != currentCellWidth) {
			currentCell.setWidth(currentCellWidth);
			columnWidthMap.put(columnIndex, currentCellWidth);
		}

		List<ColumnEntity> children = header.getChildren();
		if (null != children && !children.isEmpty()) {
			// set rowSpan
			for (int x = 0; x < currentRow.size(); ++x) {
				CellEntity tempCell = currentRow.get(x);
				Integer tempRowSpan = tempCell.getRowSpan();
				tempCell.setRowSpan(x == columnIndex ? null : (null == tempRowSpan ? 2 : tempRowSpan + 1));
			}
			// set colSpan
			currentCell.setColSpan(children.size());
			// add row
			if (rowIndex + 1 + 1 > headerCells.size()) {
				List<CellEntity> newRow = new ArrayList<>();
				headerCells.add(newRow);
				for (int x = 0; x < currentRow.size() - 1 + children.size(); ++x)
					newRow.add(CellEntity.builder().rowIndex(rowIndex + 1).columnIndex(x).isHeader(true).build());
			}
			for (int x = 0; x < children.size(); ++x) {
				reduceHeaderChildrenRows(rowIndex + 1, columnIndex + x, children.get(x), headerCells, columnWidthMap);
			}
		}
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
