package com.renzzle.backend.domain.puzzle.community.util;

import com.renzzle.backend.domain.puzzle.shared.util.ZobristHashUtils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class SharedPuzzlePreviewRenderer {

    // 2:1, the shape KakaoTalk fills in a chat bubble without cropping
    public static final int WIDTH = 1200;
    public static final int HEIGHT = 600;

    private static final int LINES = 15;
    private static final double BOARD_MARGIN = 0.8;
    private static final double BOARD_SPAN = LINES - 1 + BOARD_MARGIN * 2;
    private static final double STONE_PADDING = 1.5;
    private static final int[][] STAR_POINTS = {{3, 3}, {3, 11}, {7, 7}, {11, 3}, {11, 11}};

    // Same colors as the share page board (COLORS in puzzle.js), the app's board. The page draws lines and the white
    // stone edge in translucent ink; here they are blended in advance, so crossing lines don't darken
    private static final Color BOARD = new Color(0xE7C99A);
    private static final Color LINE = new Color(0xB59E7A);
    private static final Color FRAME = new Color(0x6F634E);
    private static final Color BLACK_STONE = new Color(0x2C2A25);
    private static final Color WHITE_STONE = new Color(0xFFFEFB);
    private static final Color WHITE_STONE_EDGE = new Color(0x706E6A);

    private SharedPuzzlePreviewRenderer() {
    }

    // The board's full width in a band around the stones, so they stay large in a small chat bubble
    public static byte[] render(String boardStatus) {
        int[] cells = ZobristHashUtils.parseCellIndexes(boardStatus);
        View view = View.around(cells);

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            graphics.setColor(BOARD);
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
            paintGrid(graphics, view);
            for (int i = 0; i < cells.length; i++) {
                paintStone(graphics, view, cells[i], i % 2 == 0);
            }
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return png.toByteArray();
    }

    // Inner lines light, the frame and star points darker
    private static void paintGrid(Graphics2D graphics, View view) {
        float lineWidth = (float) Math.max(2, view.cell() / 24);
        for (int i = 0; i < LINES; i++) {
            boolean edge = i == 0 || i == LINES - 1;
            graphics.setColor(edge ? FRAME : LINE);
            graphics.setStroke(new BasicStroke(edge ? lineWidth * 1.5f : lineWidth));
            graphics.draw(new Line2D.Double(view.x(i), view.y(0), view.x(i), view.y(LINES - 1)));
            graphics.draw(new Line2D.Double(view.x(0), view.y(i), view.x(LINES - 1), view.y(i)));
        }

        graphics.setColor(FRAME);
        double starRadius = view.cell() * 0.11;
        for (int[] star : STAR_POINTS) {
            double x = view.x(star[1]);
            double y = view.y(star[0]);
            graphics.fill(new Ellipse2D.Double(x - starRadius, y - starRadius, starRadius * 2, starRadius * 2));
        }
    }

    // Cells are column-major (col * 15 + row), with row 0 at the bottom as on the page
    private static void paintStone(Graphics2D graphics, View view, int cell, boolean black) {
        double radius = view.cell() * 0.47;
        double x = view.x(cell / LINES);
        double y = view.y(cell % LINES);
        graphics.setColor(black ? BLACK_STONE : WHITE_STONE);
        graphics.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
        if (!black) {
            // Inside the stone's edge, as the page draws it
            double edge = Math.max(1.5, view.cell() / 24);
            double inner = radius - edge / 2;
            graphics.setColor(WHITE_STONE_EDGE);
            graphics.setStroke(new BasicStroke((float) edge));
            graphics.draw(new Ellipse2D.Double(x - inner, y - inner, inner * 2, inner * 2));
        }
    }

    // The visible part of the board in cell units. Stones spread over more rows than the band holds zoom it out,
    // down to the whole board between blank sides
    private record View(double centerCol, double centerRow, double cell) {

        static View around(int[] cells) {
            int minRow = LINES;
            int maxRow = -1;
            for (int cell : cells) {
                minRow = Math.min(minRow, cell % LINES);
                maxRow = Math.max(maxRow, cell % LINES);
            }

            double bandRows = BOARD_SPAN * HEIGHT / WIDTH;
            double rows = Math.min(BOARD_SPAN, Math.max(bandRows, maxRow - minRow + STONE_PADDING * 2));
            double low = -BOARD_MARGIN + rows / 2;
            double high = LINES - 1 + BOARD_MARGIN - rows / 2;
            double centerRow = Math.max(low, Math.min((minRow + maxRow) / 2.0, high));
            return new View((LINES - 1) / 2.0, centerRow, HEIGHT / rows);
        }

        double x(int col) {
            return WIDTH / 2.0 + (col - centerCol) * cell;
        }

        double y(int row) {
            return HEIGHT / 2.0 - (row - centerRow) * cell;
        }
    }
}
