package app;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/*
* окно с кадром в виде массива пикселей (pixels, по строкам) вместо StdDraw
* кадр рисуется в главном потоке и выводится через show() (активная отрисовка без repaint)
* клавиатура и колесо мыши приходят в потоке Swing и копятся здесь до опроса из главного потока
*/
public class GameWindow {
    private final int               width;
    private final int               height;

    private final Canvas            canvas = new Canvas();
    private final BufferedImage     image;
    private final int[]             pixels;
    private BufferStrategy          strategy;

    private final boolean[]         pressedKeys = new boolean[KeyEvent.KEY_LAST + 1];
    private final ConcurrentLinkedQueue<Character> typedKeys = new ConcurrentLinkedQueue<>();

    private final Object            wheelLock = new Object();
    private double                  wheelRotation = 0;
    private int                     wheelPixelX = 0;
    private int                     wheelPixelY = 0;

    public GameWindow(String title, int width, int height) {
        this.width = width;
        this.height = height;

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        try {
            SwingUtilities.invokeAndWait(() -> createFrame(title));
        } catch (Exception e) {
            throw new IllegalStateException("can't create window", e);
        }
    }

    private void createFrame(String title) {
        canvas.setPreferredSize(new Dimension(width, height));
        canvas.setIgnoreRepaint(true);
        canvas.setFocusable(true);

        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { setPressed(e.getKeyCode(), true); }

            @Override
            public void keyReleased(KeyEvent e) { setPressed(e.getKeyCode(), false); }

            @Override
            public void keyTyped(KeyEvent e) { typedKeys.add(e.getKeyChar()); }
        });
        canvas.addMouseWheelListener(e -> {
            synchronized (wheelLock) {
                wheelRotation += e.getPreciseWheelRotation();
                wheelPixelX = e.getX();
                wheelPixelY = e.getY();
            }
        });
        canvas.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {//иначе отпускание клавиши вне окна потеряется
                synchronized (pressedKeys) { Arrays.fill(pressedKeys, false); }
            }
        });

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        canvas.createBufferStrategy(2);
        strategy = canvas.getBufferStrategy();
        canvas.requestFocus();
    }

    private void setPressed(int keyCode, boolean pressed) {
        if (keyCode >= 0 && keyCode < pressedKeys.length) {
            synchronized (pressedKeys) { pressedKeys[keyCode] = pressed; }
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    //пиксели кадра 0xRRGGBB, индекс y * width + x
    public int[] getPixels() { return pixels; }

    //для текста и прочего поверх пикселей, вызывающий должен сделать dispose()
    public Graphics2D createGraphics() { return image.createGraphics(); }

    public void clear(int color) {
        Arrays.fill(pixels, color);
    }

    //выводит кадр на экран
    public void show() {
        do {
            do {
                Graphics g = strategy.getDrawGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
            } while (strategy.contentsRestored());
            strategy.show();
        } while (strategy.contentsLost());
        Toolkit.getDefaultToolkit().sync();
    }

    public boolean isKeyPressed(int keyCode) {
        synchronized (pressedKeys) { return pressedKeys[keyCode]; }
    }

    //следующий набранный символ или null
    public Character nextKeyTyped() {
        return typedKeys.poll();
    }

    /*
    * возвращает накопленную прокрутку колеса и сбрасывает её,
    * в point записывается последнее положение указателя (пиксели холста)
    */
    public double takeWheelRotation(Point point) {
        synchronized (wheelLock) {
            double rotation = wheelRotation;
            wheelRotation = 0;
            point.setLocation(wheelPixelX, wheelPixelY);
            return rotation;
        }
    }
}
