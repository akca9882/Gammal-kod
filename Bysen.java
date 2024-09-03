import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;
import javax.swing.*;
import static java.util.stream.Collectors.*;
import static javax.swing.SwingUtilities.*;

public class Bysen extends JPanel {
    enum Creatures {
        Bysen("Du hör ett mummel"),
        Troll("Du ser en trollring"),
        Vittra("Du känner ett kallt isande drag");

    	Creatures(String warning) {
            this.warning = warning;
        }
        final String warning;
    }

    static final Random rand = new Random();

    final int roomSize = 45;
    final int playerSize = 16;

    boolean gameOver = true;
    int currRoom, numArrows, creatureRoom;
    List<String> messages;
    Set<Creatures>[] creatures;

    public Bysen() {
        setPreferredSize(new Dimension(721, 687));
        setBackground(Color.white);
        setForeground(Color.lightGray);
        setFont(new Font("SansSerif", Font.PLAIN, 18));
        setFocusable(true);

        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {

                if (gameOver) {
                    startNewGame();

                } else {
                    int selectedRoom = -1;
                    int ex = e.getX();
                    int ey = e.getY();

                    for (int link : links[currRoom]) {
                        int cx = rooms[link][0];
                        int cy = rooms[link][1];
                        if (insideRoom(ex, ey, cx, cy)) {
                            selectedRoom = link;
                            break;
                        }
                    }

                    if (selectedRoom == -1)
                        return;

                    if (isLeftMouseButton(e)) {
                        currRoom = selectedRoom;
                        situation();

                    } else if (isRightMouseButton(e)) {
                        throwNet(selectedRoom);
                    }
                }
                repaint();
            }

            boolean insideRoom(int ex, int ey, int cx, int cy) {
                return ((ex > cx && ex < cx + roomSize)
                        && (ey > cy && ey < cy + roomSize));
            }
        });
    }

    void startNewGame() {
        numArrows = 3;
        currRoom = rand.nextInt(rooms.length);
        messages = new ArrayList<>();

        creatures = new Set[rooms.length];
        for (int i = 0; i < rooms.length; i++)
        	creatures[i] = EnumSet.noneOf(Creatures.class);

        // varelser kan dela rum (om de inte är identiska)
        int[] ordinals = {0, 1, 1, 1, 2, 2};
        Creatures[] values = Creatures.values();
        for (int ord : ordinals) {
            int room;
            do {
                room = rand.nextInt(rooms.length);
            } while (tooClose(room) || creatures[room].contains(values[ord]));

            if (ord == 0)
            	creatureRoom = room;

            creatures[room].add(values[ord]);
        }

        gameOver = false;
    }

    // placera inte varelser nära startrummet
    boolean tooClose(int room) {
        if (currRoom == room)
            return true;
        for (int link : links[currRoom])
            if (room == link)
                return true;
        return false;
    }

    void situation() {
        Set<Creatures> set = creatures[currRoom];

        if (set.contains(Creatures.Bysen)) {
            messages.add("Bysen lockar dig att gå vilse");
            gameOver = true;

        } else if (set.contains(Creatures.Troll)) {
            messages.add("Du faller ner i trollringen");
            gameOver = true;

        } else if (set.contains(Creatures.Vittra)) {
            messages.add("Vittran kör iväg dig till ett slumpat rum");

            // förflytta, men undvik 2 förflyttningar i rad
            do {
                currRoom = rand.nextInt(rooms.length);
            } while (creatures[currRoom].contains(Creatures.Vittra));

            // flytta vittran, men inte till spelarens rum eller ett rum med vittra
            set.remove(Creatures.Vittra);
            int newRoom;
            do {
                newRoom = rand.nextInt(rooms.length);
            } while (newRoom == currRoom || creatures[newRoom].contains(Creatures.Vittra));
            creatures[newRoom].add(Creatures.Vittra);

            // omvärdera
            situation();

        } else {

            // se sig om 
            for (int link : links[currRoom]) {
                for (Creatures creature : creatures[link])
                    messages.add(creature.warning);
            }
        }
    }

    void throwNet(int room) {
        if (creatures[room].contains(Creatures.Bysen)) {
            messages.add("Du vinner! Du har fångat Bysen!");
            gameOver = true;

        } else {
            numArrows--;
            if (numArrows == 0) {
                messages.add("Oops! Inga inga nät kvar.");
                gameOver = true;

            } else if (rand.nextInt(4) != 0) { // 75 %
            	creatures[creatureRoom].remove(Creatures.Bysen);
                creatureRoom = links[creatureRoom][rand.nextInt(3)];

                if (creatureRoom == currRoom) {
                    messages.add("Du väckte Bysen och han är inte glad!");
                    gameOver = true;

                } else {
                    messages.add("Du råkade se Bysen och han bara försvann");
                    creatures[creatureRoom].add(Creatures.Bysen);
                }
            }
        }
    }

    void drawPlayer(Graphics2D g) {
        int x = rooms[currRoom][0] + (roomSize - playerSize) / 2;
        int y = rooms[currRoom][1] + (roomSize - playerSize) - 2;

        Path2D player = new Path2D.Double();
        player.moveTo(x, y);
        player.lineTo(x + playerSize, y);
        player.lineTo(x + playerSize / 2, y - playerSize);
        player.closePath();

        g.setColor(Color.white);
        g.fill(player);
        g.setStroke(new BasicStroke(1));
        g.setColor(Color.black);
        g.draw(player);
    }

    void drawStartScreen(Graphics2D g) {
        g.setColor(new Color(0xDDFFFFFF, true));
        g.fillRect(0, 0, getWidth(), getHeight() - 60);

        g.setColor(Color.darkGray);
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        g.drawString("Fånga Bysen!", 160, 240);

        g.setFont(getFont());
        g.drawString("Vänsterklicka för att flytta, Högerklicka för att skjuta", 210, 310);
        g.drawString("Var försiktig väsen kan befinna sig i samma rum som du", 175, 345);
        g.drawString("Klicka för att starta", 310, 380);
    }

    void drawRooms(Graphics2D g) {
        g.setColor(Color.darkGray);
        g.setStroke(new BasicStroke(2));

        for (int i = 0; i < links.length; i++) {
            for (int link : links[i]) {
                int x1 = rooms[i][0] + roomSize / 2;
                int y1 = rooms[i][1] + roomSize / 2;
                int x2 = rooms[link][0] + roomSize / 2;
                int y2 = rooms[link][1] + roomSize / 2;
                g.drawLine(x1, y1, x2, y2);
            }
        }

        g.setColor(Color.orange);
        for (int[] r : rooms)
            g.fillOval(r[0], r[1], roomSize, roomSize);

        if (!gameOver) {
            g.setColor(Color.magenta);
            for (int link : links[currRoom])
                g.fillOval(rooms[link][0], rooms[link][1], roomSize, roomSize);
        }

        g.setColor(Color.darkGray);
        for (int[] r : rooms)
            g.drawOval(r[0], r[1], roomSize, roomSize);
    }

    void drawMessage(Graphics2D g) {
        if (!gameOver)
            g.drawString("pilar  " + numArrows, 610, 30);

        if (messages != null) {
            g.setColor(Color.black);

            // ta bort lika meddelanden
            messages = messages.stream().distinct().collect(toList());

            // slå ihop max tre
            String msg = messages.stream().limit(3).collect(joining(" & "));
            g.drawString(msg, 20, getHeight() - 40);

            // om det finns mer, skriv ut nedanför
            if (messages.size() > 3) {
                g.drawString("& " + messages.get(3), 20, getHeight() - 17);
            }

            messages.clear();
        }
    }

    @Override
    public void paintComponent(Graphics gg) {
        super.paintComponent(gg);
        Graphics2D g = (Graphics2D) gg;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        drawRooms(g);
        if (gameOver) {
            drawStartScreen(g);
        } else {
            drawPlayer(g);
        }
        drawMessage(g);
    }

    public static void main(String[] args) {
        invokeLater(() -> {
            JFrame f = new JFrame();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setTitle("Fånga Bysen");
            f.setResizable(false);
            f.add(new Bysen(), BorderLayout.CENTER);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    int[][] rooms = {{334, 20}, {609, 220}, {499, 540}, {169, 540}, {62, 220},
    {169, 255}, {232, 168}, {334, 136}, {435, 168}, {499, 255}, {499, 361},
    {435, 447}, {334, 480}, {232, 447}, {169, 361}, {254, 336}, {285, 238},
    {387, 238}, {418, 336}, {334, 393}};

    int[][] links = {{4, 7, 1}, {0, 9, 2}, {1, 11, 3}, {4, 13, 2}, {0, 5, 3},
    {4, 6, 14}, {7, 16, 5}, {6, 0, 8}, {7, 17, 9}, {8, 1, 10}, {9, 18, 11},
    {10, 2, 12}, {13, 19, 11}, {14, 3, 12}, {5, 15, 13}, {14, 16, 19},
    {6, 17, 15}, {16, 8, 18}, {19, 10, 17}, {15, 12, 18}};
}