/*
 * Title: MineSweeper
 * Created by: Matthew Bruton
 * Created: May 2017
 */
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class MineSweeper extends Applet
    implements Runnable, KeyListener, MouseListener, MouseMotionListener {
  static final long serialVersionUID = 1L;

  Random gen = new Random();
  DecimalFormat display = new DecimalFormat("000");
  DecimalFormat record = new DecimalFormat("#.000");
  int xDim, yDim, delay, sectorSide;

  // properties
  Properties prop = new Properties();
  OutputStream output = null;
  InputStream input = null;

  // difficulty 1, 2, 3
  int sizeX = 9; // 30, 16, 9
  int sizeY = 9; // 16, 16, 9
  int mineNum = 10; // 99, 40, 10

  int mineX, mineY;
  int flagCount;
  int revealCount = 0;
  int[][] grid = new int[sizeX][sizeY];
  int gameOver = 0;
  boolean restarted = true;
  boolean started = false;
  boolean inGame = true;
  boolean finalized = false;
  boolean[][] revealed = new boolean[sizeX][sizeY];
  boolean[][] flagged = new boolean[sizeX][sizeY];
  boolean cheat = false;
  boolean autoComplete = false;
  int difficulty = 3;
  int hovered = 0;
  double bestTimeBeginner = 999;
  double bestTimeIntermediate = 999;
  double bestTimeExpert = 999;

  Image i;
  Graphics gg;

  double time = 0.0;

  // Colors
  Color c1 = new Color(1, 0, 254);
  Color c2 = new Color(1, 127, 1);
  Color c3 = new Color(254, 0, 0);
  Color c4 = new Color(1, 0, 128);
  Color c5 = new Color(129, 1, 2);
  Color c6 = new Color(0, 128, 129);
  Color c7 = new Color(0, 0, 0);
  Color c8 = new Color(128, 128, 128);
  Color background = new Color(185, 185, 185);
  Color foreground = new Color(222, 222, 222);
  Color boarder = new Color(123, 123, 123);
  Color beginnerGreen = new Color(47, 178, 0);
  Color intermediateOrange = new Color(178, 71, 0);
  Color expertRed = new Color(178, 0, 0);

  // Fonts
  Font numbers = new Font("Monospaced", Font.BOLD, 28);
  Font comments = new Font("Monospaced", Font.BOLD, 14);

  // Images
  Image flag;
  Image blank;
  Image mine;

  public void drawCenteredString(String s, int w0, int h0, int w, int h, Graphics page) {
    FontMetrics fm = page.getFontMetrics();
    int x = w0 + (w - w0 - fm.stringWidth(s)) / 2;
    int y = (fm.getAscent() + h0 + (h - h0 - (fm.getAscent() + fm.getDescent())) / 2);
    page.drawString(s, x, y);
  }

  public void drawLeftString(String s, int w0, int h0, int h, Graphics page) {
    FontMetrics fm = page.getFontMetrics();
    int x = w0;
    int y = (fm.getAscent() + h0 + (h - h0 - (fm.getAscent() + fm.getDescent())) / 2);
    page.drawString(s, x, y);
  }

  public void drawRightString(String s, int w0, int h0, int h, Graphics page) {
    FontMetrics fm = page.getFontMetrics();
    int x = w0 - fm.stringWidth(s);
    int y = (fm.getAscent() + h0 + (h - h0 - (fm.getAscent() + fm.getDescent())) / 2);
    page.drawString(s, x, y);
  }

  public MineSweeper() {
    setFocusable(true);
    setFocusTraversalKeysEnabled(false);
  }

  public void init() {
    sectorSide = 40;
    xDim = sizeX * sectorSide;
    yDim = sizeY * sectorSide + 50;
    delay = 1;
    flagCount = mineNum;

    this.setSize(xDim, yDim);
    setBackground(background);

    addMouseListener(this);
    addMouseMotionListener(this);

    flag = getImage(getDocumentBase(), "../images/flag.png");
    blank = getImage(getDocumentBase(), "../images/blank.png");
    mine = getImage(getDocumentBase(), "../images/mine.png");

    // load best times
    if (new File("bestTimes.properties").isFile()) {
      try {
        input = new FileInputStream("bestTimes.properties");
        // load properties
        prop.load(input);
        // get properties
        bestTimeBeginner = Double.parseDouble(prop.getProperty("bestTimeBeginner"));
        bestTimeIntermediate = Double.parseDouble(prop.getProperty("bestTimeIntermediate"));
        bestTimeExpert = Double.parseDouble(prop.getProperty("bestTimeExpert"));
        System.out.println("Best times loaded");

      } catch (IOException ex) {
        ex.printStackTrace();
      } finally {
        if (input != null) {
          try {
            input.close();
          } catch (IOException io) {
            io.printStackTrace();
          }
        }
      }
    } else
      System.out.println("No best times available");

  }

  public void start() {
    Thread thread = new Thread(this);
    thread.start();

    addKeyListener(this);
  }

  public void stop() {}

  public void mouseClicked(MouseEvent e) {}

  public void mouseReleased(MouseEvent e) {

    for (int i = 0; i < sizeX; i++) {
      for (int j = 0; j < sizeY; j++) {

        if (e.getX() >= i * sectorSide && e.getX() < (i + 1) * sectorSide) {
          if (e.getY() >= j * sectorSide && e.getY() < (j + 1) * sectorSide) {

            if (e.getButton() == MouseEvent.BUTTON1 && flagged[i][j] == false
                && revealed[i][j] == false && finalized == false) {
              revealed[i][j] = true;
              revealCount++;
              if (grid[i][j] < 0) { // lose check
                gameOver = -1;
              }
              if (gameOver == 0 && revealCount >= sizeX * sizeY - mineNum) { // win check
                gameOver = 1;
              }
            }
            if (e.getButton() == MouseEvent.BUTTON3 && revealed[i][j] == false
                && finalized == false) {
              if (flagged[i][j]) {
                flagged[i][j] = false;
                flagCount++;
              } else {
                flagged[i][j] = true;
                flagCount--;
              }
            }

            if (e.getButton() == MouseEvent.BUTTON2) {
              if (flagCount == 0)
                autoComplete = true;
              else if (revealed[i][j] == false && flagged[i][j] == false) {
                if (grid[i][j] < 0) {
                  flagged[i][j] = true;
                  flagCount--;
                } else {
                  revealed[i][j] = true;
                  revealCount++;
                }
                time += (10 - 5 * revealCount / (sizeX * sizeY - mineNum));
              }
            }

          }
        }

      }
    }

    if (e.getX() >= xDim / 2 - 25 && e.getX() <= xDim / 2 - 15 && e.getY() >= yDim - 40
        && e.getY() <= yDim - 30) {
      difficulty = 1;
      restarted = true;
    } else if (e.getX() >= xDim / 2 - 5 && e.getX() <= xDim / 2 + 5 && e.getY() >= yDim - 40
        && e.getY() <= yDim - 30) {
      difficulty = 2;
      restarted = true;
    } else if (e.getX() >= xDim / 2 + 15 && e.getX() <= xDim / 2 + 25 && e.getY() >= yDim - 40
        && e.getY() <= yDim - 30) {
      difficulty = 3;
      restarted = true;
    }

    started = true;

  }

  public void mouseEntered(MouseEvent e) {}

  public void mouseExited(MouseEvent e) {}

  public void mousePressed(MouseEvent e) {}

  public void mouseMoved(MouseEvent e) {
    if (e.getX() >= xDim / 2 - 25 && e.getX() <= xDim / 2 - 15 && e.getY() >= yDim - 40
        && e.getY() <= yDim - 30)
      hovered = 1;
    else if (e.getX() >= xDim / 2 - 5 && e.getX() <= xDim / 2 + 5 && e.getY() >= yDim - 40
        && e.getY() <= yDim - 30)
      hovered = 2;
    else if (e.getX() >= xDim / 2 + 15 && e.getX() <= xDim / 2 + 25 && e.getY() >= yDim - 40
        && e.getY() <= yDim - 30)
      hovered = 3;
    else
      hovered = 0;
  }

  public void mouseDragged(MouseEvent e) {}

  public void keyPressed(KeyEvent e) {}

  public void keyTyped(KeyEvent e) {}

  public void keyReleased(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
      restarted = true;
    }

    if (e.getKeyCode() == KeyEvent.VK_C) {
      if (cheat)
        cheat = false;
      else
        cheat = true;
    }
  }

  public void paint(Graphics g) {

    g.setFont(numbers);
    g.setColor(background);
    g.fillRect(0, 0, xDim, yDim);
    g.setColor(Color.black);
    g.drawRect(0, 0, xDim, yDim);

    //
    for (int i = 0; i < sizeX; i++) {
      for (int j = 0; j < sizeY; j++) {

        // boarders
        g.setColor(boarder);
        g.drawRect(i * sectorSide, j * sectorSide, sectorSide - 1, sectorSide - 1);

        // numbers here
        if (grid[i][j] > 0) {
          if (grid[i][j] == 1)
            g.setColor(c1);
          if (grid[i][j] == 2)
            g.setColor(c2);
          if (grid[i][j] == 3)
            g.setColor(c3);
          if (grid[i][j] == 4)
            g.setColor(c4);
          if (grid[i][j] == 5)
            g.setColor(c5);
          if (grid[i][j] == 6)
            g.setColor(c6);
          if (grid[i][j] == 7)
            g.setColor(c7);
          if (grid[i][j] == 8)
            g.setColor(c8);

          drawCenteredString(Integer.toString(grid[i][j]), i * sectorSide, j * sectorSide,
              (i + 1) * sectorSide, (j + 1) * sectorSide, g);
        }
        if (grid[i][j] < 0) {
          // g.setColor(Color.black);
          // g.fillOval(i * sectorSide, j * sectorSide, sectorSide, sectorSide);
          g.drawImage(mine, i * sectorSide, j * sectorSide, this);
        }


        if (revealed[i][j] == false && cheat == false) {
          // if (flagged[i][j])
          // g.setColor(Color.red);
          // else
          // g.setColor(foreground);
          // g.fillRect(i * sectorSide, j * sectorSide, sectorSide, sectorSide);
          if (flagged[i][j])
            g.drawImage(flag, i * sectorSide, j * sectorSide, this);
          else
            g.drawImage(blank, i * sectorSide, j * sectorSide, this);
        }


        if (gameOver == -1) {
          g.setColor(Color.red);
          g.fillRect(0, yDim - 50, xDim, 50);
        }
        if (gameOver == 1) {
          g.setColor(Color.green);
          g.fillRect(0, yDim - 50, xDim, 50);
        }

        g.setColor(Color.black);
        drawRightString(display.format(time), xDim - 25, yDim - 50, yDim, g);
        drawLeftString(display.format(flagCount), 25, yDim - 50, yDim, g);

        g.setColor(beginnerGreen);
        g.fillRect(xDim / 2 - 25, yDim - 40, 10, 10);
        g.setColor(intermediateOrange);
        g.fillRect(xDim / 2 - 5, yDim - 40, 10, 10);
        g.setColor(expertRed);
        g.fillRect(xDim / 2 + 15, yDim - 40, 10, 10);


        g.setFont(comments);
        if (hovered == 1) {
          g.setColor(beginnerGreen);
          drawCenteredString("BEGINNER: " + bestTimeBeginner, 0, yDim - 30, xDim, yDim, g);
        } else if (hovered == 2) {
          g.setColor(intermediateOrange);
          drawCenteredString("INTERMEDIATE: " + bestTimeIntermediate, 0, yDim - 30, xDim, yDim, g);
        } else if (hovered == 3) {
          g.setColor(expertRed);
          drawCenteredString("EXPERT: " + bestTimeExpert, 0, yDim - 30, xDim, yDim, g);
        }
        g.setFont(numbers); // needs to be reset for some reason

      }
    }
  }

  public void update(Graphics g) {
    if (i == null) {
      i = createImage(getSize().width, getSize().height);
      gg = i.getGraphics();
    }
    gg.setColor(getBackground());
    gg.fillRect(0, 0, this.getSize().width, this.getSize().height);
    gg.setColor(getForeground());
    paint(gg);

    g.drawImage(i, 0, 0, this);

  }

  public void run() {
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

    while (inGame) {
      if (restarted) {

        // ---------------------------
        // Difficulty
        // ---------------------------
        if (difficulty == 1) { // BEGINNER
          sizeX = 9;
          sizeY = 9;
          mineNum = 10;
        } else if (difficulty == 2) { // INTERMEDIATE
          sizeX = 16;
          sizeY = 16;
          mineNum = 40;
        } else if (difficulty == 3) { // EXPERT
          sizeX = 30;
          sizeY = 16;
          mineNum = 99;
        }
        xDim = sizeX * sectorSide;
        yDim = sizeY * sectorSide + 50;

        // ---------------------------
        // Reset Values
        // ---------------------------

        gameOver = 0;
        time = 0;
        flagCount = mineNum;
        revealCount = 0;
        autoComplete = false;
        finalized = false;

        grid = new int[sizeX][sizeY];
        revealed = new boolean[sizeX][sizeY];
        flagged = new boolean[sizeX][sizeY];

        this.resize(xDim, yDim);
        setBackground(background);

        for (int i = 0; i < sizeX; i++) {
          for (int j = 0; j < sizeY; j++) {
            grid[i][j] = 0;
            revealed[i][j] = false;
            flagged[i][j] = false;
          }
        }

        // ---------------------------
        // Place Mines
        // ---------------------------
        while (flagCount > 0) {
          mineX = gen.nextInt(sizeX);
          mineY = gen.nextInt(sizeY);

          if (grid[mineX][mineY] == 0) {
            grid[mineX][mineY] = -1;
            flagCount--;
          }
        }
        flagCount = mineNum;

        // ---------------------------
        // Set Numbers
        // ---------------------------
        for (int i = 0; i < sizeX; i++) {
          for (int j = 0; j < sizeY; j++) {

            if (grid[i][j] < 0) {
              for (int i2 = i - 1; i2 <= i + 1; i2++) {
                for (int j2 = j - 1; j2 <= j + 1; j2++) {
                  if (i2 >= 0 && i2 < sizeX && j2 >= 0 && j2 < sizeY && grid[i2][j2] >= 0)
                    grid[i2][j2]++;
                }
              }
            }

          }
        }
        started = false;
        restarted = false;
      }

      for (int i = 0; i < sizeX; i++) {
        for (int j = 0; j < sizeY; j++) {
          // autoComplete
          if (autoComplete) {
            if (flagged[i][j] == false && revealed[i][j] == false) {
              revealed[i][j] = true;
              revealCount++;
              if (grid[i][j] < 0) // lose check
                gameOver = -1;
            }
          }

          // win check incase last click was not a reveal
          if (gameOver == 0 && revealCount >= sizeX * sizeY - mineNum) {
            gameOver = 1;
          }

          // reveal open adjacent
          if (grid[i][j] == 0 && revealed[i][j]) {
            for (int i2 = i - 1; i2 <= i + 1; i2++) {
              for (int j2 = j - 1; j2 <= j + 1; j2++) {
                if (i2 >= 0 && i2 < sizeX && j2 >= 0 && j2 < sizeY && grid[i2][j2] >= 0
                    && revealed[i2][j2] == false) {
                  revealed[i2][j2] = true;
                  revealCount++;
                }
              }
            }
          }

        }
      }

      if (gameOver == 0) {
        if (started)
          time += 0.001;
        try {
          Thread.sleep(delay);
        } catch (InterruptedException Ex) {
        }
      } else if (finalized == false) {
        if (gameOver == 1) {
          time = Double.parseDouble(record.format(time));
          System.out.println("YOU WIN!");

          if (time < bestTimeBeginner && difficulty == 1)
            bestTimeBeginner = time;
          else if (time < bestTimeIntermediate && difficulty == 2)
            bestTimeIntermediate = time;
          else if (time < bestTimeExpert && difficulty == 3)
            bestTimeExpert = time;

          try {
            output = new FileOutputStream("bestTimes.properties");
            // set properties
            prop.setProperty("bestTimeBeginner", Double.toString(bestTimeBeginner));
            prop.setProperty("bestTimeIntermediate", Double.toString(bestTimeIntermediate));
            prop.setProperty("bestTimeExpert", Double.toString(bestTimeExpert));
            // save properties
            prop.store(output, null);

          } catch (IOException io) {
            io.printStackTrace();
          } finally {
            if (output != null) {
              try {
                output.close();
              } catch (IOException ex) {
                ex.printStackTrace();
              }
            }
          }

          System.out.println(time);
        } else if (gameOver == -1) {
          System.out.println("YOU LOSE!");
        }
        finalized = true;
      }

      repaint();

    }
  }
}
