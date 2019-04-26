package app.figure.RLE;

import app.common.IGameLife;
import app.figure.Exceptions.*;
import app.figure.IFigure;

import java.io.*;
import java.nio.file.*;
import java.util.regex.*;

public class RLEReader implements IFigure {
    /*
    * используется для нахождения
    * рармеров холста в RLE файле:
    * x = 36, y = 9, rule = B3/S23
    */
    private static          Pattern     sizePattern = Pattern.compile("([xy])[ ]?=[ ]?(\\d+),[ ]?([xy])[ ]?=[ ]?(\\d+)");

    private static final    char        ALIVE = 'o';
    private static final    char        DEAD = 'b';
    private static final    char        COMMENT = '#';
    private static final    char        EOF = '!';
    private static final    char        EOL = '$';
    private static final    char        SPACE = ' ';

    private                 String      path;

    public RLEReader(String path) {
        this.path = path;
    }

    @Override
    public void setFigure(IGameLife data, int xOffset, int yOffset) throws FigureException {
        /*
        * <tag>	description
        * b	    dead cell
        * o	    alive cell
        * $   	end of line
        * !     end of file
        *
        * [count]{o|b|$} may devide spaces
        */

        Path pathData = Paths.get(path);
        BufferedReader r = null;
        Counter count = new Counter();

        boolean sizeFind = false;//размеры холста встречаются только один раз
        try {
            r = Files.newBufferedReader(pathData);
            String line;
            int sx = xOffset;
            int sy = yOffset;

            boolean reading = true;

            while(reading && ((line = r.readLine()) != null)) {
                if (line.length() == 0) {continue;}
                if (line.charAt(0) == '#') {continue;}

                if (!sizeFind) {
                    sizeFind = true;
                    Matcher match = sizePattern.matcher(line);
                    if (match.find()) {
                        int x = 0;
                        int y = 0;

                        if ("x".equals(match.group(1)) && "y".equals(match.group(3))) {
                            y = Integer.parseInt(match.group(4));
                            x = Integer.parseInt(match.group(2));
                        }
                        else if ("y".equals(match.group(1)) && "x".equals(match.group(3))) {
                            x = Integer.parseInt(match.group(4));
                            y = Integer.parseInt(match.group(2));
                        }
                        else {
                            throw new RLEException("rle file don't have one sizePattern of plate");
                        }
                        if (!data.isUsedInfinitePlate() && (x+xOffset > data.getWidth() || y+yOffset > data.getHeight())) {
                            throw new RLEException("figure is bigger than plate");
                        }
                    }
                }
                else {
                    for (int i = 0; i < line.length(); i++) {
                        char cmd = line.charAt(i);

                        if (cmd == DEAD) {
                            sx+=count.getCountAndReset();
                        }
                        else if (cmd == ALIVE) {
                            int alive_count = count.getCountAndReset();
                            for (int j = sx; j < sx+alive_count; j++) {
                                data.setCurrent(j, sy);
                            }
                            sx+=alive_count;
                        }
                        else if (cmd == EOL) {
                            sy+=count.getCountAndReset();
                            sx = xOffset;
                        }
                        else if (cmd == COMMENT) {
                            break;
                        }
                        else if (cmd == EOF) {
                            reading = false;
                            break;
                        }
                        else if (cmd == SPACE) {
                            continue;
                        }
                        else {
                            count.appendNum(cmd);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                r.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
