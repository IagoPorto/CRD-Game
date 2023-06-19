import javax.swing.*;// for GUI components
import java.awt.*; // for various graphical objects
import java.awt.event.ActionEvent;// for action events
import java.awt.event.ActionListener;// for action events
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class GUI extends JFrame implements ActionListener {
    JLabel leftPanelRoundsLabel;
    JList<String> list;
    private MainAgent mainAgent;
    private JPanel rightPanel;
    private JTextArea rightPanelLoggingTextArea;
    private LoggingOutputStream loggingOutputStream;
    JTable payoffTable;
    Object[] nullPointerWorkAround = {"Player", "ID", "Nº GenPlayed", "Payoff", "Average"};
    private int primero = 1;
    JScrollPane jScrollPane;
    GridBagConstraints c;
    JMenuBar menuBar = new JMenuBar();
    JPanel centralPanel;
    JPanel pane;
    JPanel centralTopSubpanel;
    JPanel leftPanel, leftTopSubpanel, leftBottomSubpanel;
    private int finalRound = 0;
    private int N, R, E, numGames, numGen, numGenPlayed, numBRPlayed, nTotalGamesPlayed;
    private double Pd;
    private JLabel leftPanelExtraInformation;

    public GUI() {
        initUI();
    }

    public GUI(MainAgent agent) {
        mainAgent = agent;
        initUI();
        loggingOutputStream = new LoggingOutputStream(rightPanelLoggingTextArea);
    }

    public void log(String s) {
        Runnable appendLine = () -> {
            rightPanelLoggingTextArea.append('[' + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - " + s);
            rightPanelLoggingTextArea.setCaretPosition(rightPanelLoggingTextArea.getDocument().getLength());
        };
        SwingUtilities.invokeLater(appendLine);
    }

    public OutputStream getLoggingOutputStream() {
        return loggingOutputStream;
    }

    public void logLine(String s) {
        log(s + "\n");
    }

    public void setPlayersUI(String[] players) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String s : players) {
            listModel.addElement(s);
        }
        list.setModel(listModel);
    }

    public void initUI() {
        setTitle("The CDR Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Cambiamos la imagen del pinguino por otra que es, aunque solo sea mínimamente, mejor
        setIconImage((new ImageIcon("./Logo.jpg").getImage()));//Logo of the game.
        setMinimumSize(new Dimension(800, 600));
        setPreferredSize(new Dimension(1200, 700));
        setLocation(460, 240);
        setName("The CDR Game");
        setJMenuBar(createMainMenuBar());
        setContentPane(createMainContentPane());
        pack();
        primero = 0;
        setVisible(true);
    }

    private Container createMainContentPane() {
        pane = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridy = 0;
        gc.weightx = 0.5;
        gc.weighty = 0.5;

        //LEFT PANEL
        gc.gridx = 0;
        gc.weightx = 1;
        pane.add(createLeftPanel(), gc);

        //CENTRAL PANEL
        gc.gridx = 1;
        gc.weightx = 5;
        pane.add(createCentralPanel(), gc);

        //RIGHT PANEL
        gc.gridx = 2;
        gc.weightx = 5;
        pane.add(createRightPanel(), gc);
        return pane;
    }

    private JPanel createLeftPanel() {
        leftPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;

        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;

        gc.gridy = 0;
        gc.weighty = 1;
        leftPanel.add(createLeftTopSubpanel(), gc);
        gc.gridy = 1;
        gc.weighty = 2;
        leftPanel.add(createLeftBottomSubpanel(), gc);

        return leftPanel;
    }

    private JPanel createLeftTopSubpanel() {
        
        if(primero == 1){
            leftTopSubpanel = new JPanel();
            leftTopSubpanel.setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            JButton leftPanelNewButton = new JButton("Battle Royal");
            leftPanelNewButton.addActionListener(actionEvent ->{ finalRound=1;
                                                                    mainAgent.newGame(0);});
            JButton leftPanelFinalButton = new JButton("Final");
            leftPanelFinalButton.addActionListener(actionEvent ->{
                if(finalRound == 1){
                    mainAgent.newGame(1);
                    finalRound = 0;
                }else{
                    JOptionPane.showMessageDialog(new Frame("Error"), "Play battle royal first"); }
            });         
            JButton leftPanelStopButton = new JButton("Stop");
            leftPanelStopButton.addActionListener(actionEvent -> mainAgent.stopExecution());
            JButton leftPanelContinueButton = new JButton("Continue");
            leftPanelContinueButton.addActionListener(actionEvent -> mainAgent.continueExecution());

            
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.anchor = GridBagConstraints.FIRST_LINE_START;
            gc.gridx = 0;
            gc.weightx = 0.5;
            gc.weighty = 0.5;
            gc.gridy = 1;
            leftTopSubpanel.add(leftPanelNewButton, gc);
            gc.gridy = 2;
            leftTopSubpanel.add(leftPanelFinalButton, gc);
            gc.gridy = 3;
            leftTopSubpanel.add(leftPanelStopButton, gc);
            gc.gridy = 4;
            leftTopSubpanel.add(leftPanelContinueButton, gc);

         }
         return leftTopSubpanel;
    }

    private JPanel createLeftBottomSubpanel(){//To show the parameters of the game

        leftBottomSubpanel = new JPanel();
        leftBottomSubpanel.setLayout(new GridBagLayout());
        leftPanelExtraInformation = new JLabel("<html>Parameters: <br/><br/>" +
                                                        "&nbsp;&nbsp;N = " + N + "<br/>" +
                                                        "&nbsp;&nbsp;R= " + R + "<br/>" +
                                                        "&nbsp;&nbsp;E = " + E + "<br/>" +
                                                        "&nbsp;&nbsp;Pd = " + Pd + "<br/>" +
                                                        "&nbsp;&nbsp;Total Games = " + numGames + "<br/>" +
                                                        "&nbsp;&nbsp;Total Gen = " + numGen + "<br/>" +
                                                        "&nbsp;&nbsp;B.R. Played = " + numBRPlayed + "<br/>" +
                                                        "&nbsp;&nbsp;Games Played = " + nTotalGamesPlayed + "<br/>" +
                                                        "&nbsp;&nbsp;Gen Played = " + numGenPlayed + "</html>");
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        leftBottomSubpanel.add(leftPanelExtraInformation, gc);

        return leftBottomSubpanel;
    }

    private JPanel createCentralPanel() {
        centralPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;

        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;

        gc.gridy = 0;
        gc.weighty = 1;
        centralPanel.add(createCentralTopSubpanel(), gc);
        gc.gridy = 1;
        gc.weighty = 4;
        centralPanel.add(createCentralBottomSubpanel(), gc);

        return centralPanel;
    }

    private JPanel createCentralTopSubpanel() {
        if(primero == 1){

            centralTopSubpanel = new JPanel(new GridBagLayout());
            DefaultListModel<String> listModel = new DefaultListModel<>();
            listModel.addElement("Empty");
            list = new JList<>(listModel);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setSelectedIndex(0);
            list.setVisibleRowCount(5);
            JScrollPane listScrollPane = new JScrollPane(list);

            JLabel info1 = new JLabel("Selected player info");
            JButton updatePlayersButton = new JButton("Update players");
            updatePlayersButton.addActionListener(actionEvent -> mainAgent.updatePlayers());
            
            GridBagConstraints gc = new GridBagConstraints();
            gc.weightx = 0.5;
            gc.weighty = 0.5;
            gc.anchor = GridBagConstraints.CENTER;

            gc.gridx = 0;
            gc.gridy = 0;
            gc.gridheight = 2;
            gc.fill = GridBagConstraints.BOTH;
            centralTopSubpanel.add(listScrollPane, gc);
            gc.gridx = 1;
            gc.gridheight = 1;
            gc.fill = GridBagConstraints.NONE;
            centralTopSubpanel.add(info1, gc);
            gc.gridy = 1;
            centralTopSubpanel.add(updatePlayersButton, gc);
        }
            return centralTopSubpanel;
    }

    private JPanel createCentralBottomSubpanel() {//To show the players and their statics
        JPanel centralBottomSubpanel = new JPanel(new GridBagLayout());
        if ( primero == 1){
            
            Object[][] data = new String[0][5]; 
        
            payoffTable = new JTable(data, nullPointerWorkAround);
            //payoffTable.setTableHeader(null);
            payoffTable.setEnabled(false);
            
        }
        
        JScrollPane player1ScrollPane = new JScrollPane(payoffTable);

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        JLabel pr = new JLabel("<html><br/>Players results: </html>");
        gc.gridy = 0;
        gc.gridx = 0;
        centralBottomSubpanel.add(pr, gc);
        gc.gridy = 1;
        gc.gridx = 0;
        gc.weighty = 2;
        centralBottomSubpanel.add(player1ScrollPane, gc);
        
        return centralBottomSubpanel;
    }

    private JPanel createRightPanel() {
        if(primero == 1){
            rightPanel = new JPanel(new GridBagLayout());
            c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.FIRST_LINE_START;
            c.weighty = 1d;
            c.weightx = 1d;

            rightPanelLoggingTextArea = new JTextArea("");
            rightPanelLoggingTextArea.setEditable(false);
            jScrollPane = new JScrollPane(rightPanelLoggingTextArea);
        
            rightPanel.add(jScrollPane, c);
            
            return rightPanel;
        }else{  
            rightPanel.add(jScrollPane, c);
            return rightPanel;
        }

           
    }

    private JMenuBar createMainMenuBar() {
        
        if(primero == 1){

            JMenu menuFile = new JMenu("File");//Rama File en el menú
            JMenuItem exitFileMenu = new JMenuItem("Exit");
            exitFileMenu.addActionListener(actionEvent -> {this.dispose(); //Cerramos la aplicación
                                                            System.exit(0);});

            JMenuItem newGameFileMenu = new JMenuItem("New Game");//Juego Nuevo
            newGameFileMenu.addActionListener(actionEvent -> mainAgent.newGame(0));

            menuFile.add(newGameFileMenu);//Añadimos newGame a File
            menuFile.add(exitFileMenu);//Añadimos exit a File
            menuBar.add(menuFile);//Añadimos File a la menuBar

            JMenu menuEdit = new JMenu("Edit");//Rama Edit en el menú
            JMenuItem resetPlayerEditMenu = new JMenuItem("Reset Total Players");//Reset all the players
            resetPlayerEditMenu.setToolTipText("Reset all player");
            resetPlayerEditMenu.setActionCommand("reset_players");
            resetPlayerEditMenu.addActionListener(actionEvent -> mainAgent.resetTotlaPlayers());

            JMenuItem deletePlayerEditMenu = new JMenuItem("Delete player");//Delete 1 player by ID
            deletePlayerEditMenu.setToolTipText("Delete one player by ID");
            deletePlayerEditMenu.addActionListener(actionEvent -> {
                String playerID = JOptionPane.showInputDialog(new Frame("Delete player by ID"), "Enter ID");
                mainAgent.deletePlayer(Integer.parseInt(playerID));
                logLine("El id del jugador que se eliminará es: " + playerID);
            });

            JMenuItem speedEditMenu = new JMenuItem("Speed of execution");//To set a delay in ms for debugging
            speedEditMenu.setToolTipText("Modify the speed of the game");
            speedEditMenu.addActionListener(actionEvent ->{
                String speed = JOptionPane.showInputDialog(new Frame("Configure delay"), "Enter delay in ms");
                mainAgent.setSpeed(speed);});

            menuEdit.add(resetPlayerEditMenu);//Añadimos resetPlayer a Edit
            menuEdit.add(speedEditMenu);//Añadimos la funcion de delay a Edit
            menuEdit.add(deletePlayerEditMenu);//Añadimos la funcion delete player a Edit
            menuBar.add(menuEdit);//Añadimos Edit a la menuBar

            JMenu menuRun = new JMenu("Run");

            JMenuItem parametersRunMenu = new JMenuItem("Parameters");
            parametersRunMenu.setToolTipText("Modify the parameters of the game");//Aquí hay que añadir algo para actualizar los datos
            parametersRunMenu.addActionListener(actionEvent ->{
                 String param = JOptionPane.showInputDialog(new Frame("Configure parameters"), "Enter parameters N,E,R,Pd");
                 mainAgent.changeParam(param);
                 logLine("Parameters " + param);});

            JMenuItem newRunMenu = new JMenuItem("New");
            newRunMenu.setToolTipText("Starts a new series of games");
            newRunMenu.addActionListener(actionEvent -> mainAgent.newGame(0));
            //newRunMenu.addActionListener(this);

            JMenuItem stopRunMenu = new JMenuItem("Stop");
            stopRunMenu.setToolTipText("Stops the execution of the current round");//funcion para Stop
            stopRunMenu.addActionListener(actionEvent -> mainAgent.stopExecution());

            JMenuItem continueRunMenu = new JMenuItem("Continue");//función para continue
            continueRunMenu.setToolTipText("Resume the execution");
            continueRunMenu.addActionListener(actionEvent -> mainAgent.continueExecution());

            JMenuItem roundNumberRunMenu = new JMenuItem("Number of games");
            roundNumberRunMenu.setToolTipText("Change the number of games");//función para cambiar el número de rondas
            roundNumberRunMenu.addActionListener(actionEvent -> {
                mainAgent.changeNG(JOptionPane.showInputDialog(new Frame("Configure number of games"), "How many games?"));});

            menuRun.add(newRunMenu);
            menuRun.add(stopRunMenu);//añadimos las cosas al menu
            menuRun.add(continueRunMenu);
            menuRun.add(roundNumberRunMenu);
            menuRun.add(parametersRunMenu);//Añadimos resetParametros a Edit
            menuBar.add(menuRun);

            JMenu menuWindow = new JMenu("Window");

            JCheckBoxMenuItem toggleVerboseWindowMenu = new JCheckBoxMenuItem("Verbose", true);
            toggleVerboseWindowMenu.addActionListener(actionEvent -> rightPanel.setVisible(toggleVerboseWindowMenu.getState()));

            menuWindow.add(toggleVerboseWindowMenu);
            menuBar.add(menuWindow);

            JMenu menuHelp = new JMenu("Help");
            JMenuItem aboutMe = new JMenuItem("About Me");
            aboutMe.addActionListener(actionEvent -> JOptionPane.showMessageDialog(new Frame("About Me"), "Iago Porto Montes")); 
            aboutMe.addActionListener(this);

            menuHelp.add(aboutMe);//Añadimos aboutMe a Help
            menuBar.add(menuHelp);//Añadimos Help a la menuBar

            return menuBar;
        }else{
            return menuBar;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton button = (JButton) e.getSource();
            logLine("Button " + button.getText());
        } else if (e.getSource() instanceof JMenuItem) {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            logLine("Menu " + menuItem.getText());
        }
    }

    public class LoggingOutputStream extends OutputStream {
        private JTextArea textArea;

        public LoggingOutputStream(JTextArea jTextArea) {
            textArea = jTextArea;
        }

        @Override
        public void write(int i) throws IOException {
            textArea.append(String.valueOf((char) i));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }

    public void setTable(Object[][] tableGui, int N, int E, int R, double Pd, int numGen, int numGames, int numGenPlayed, int numBRPlayed, int nTotalGamesPlayed){
        //Método para actualizar la tabla y parametros relevantes del juego.
        //Aunque actualiza todo el panel, aqui solucionar eso
        payoffTable = new JTable(tableGui, nullPointerWorkAround);
        payoffTable.setEnabled(false);
        setContentPane(createMainContentPane()); 
        pack();
         this.E = E;
         this.R = R;
         this.Pd = Pd;
         this.N = N;
         this.numGames = numGames;
         this.numGen = numGen;
         this.numGenPlayed = numGenPlayed;
         this.numBRPlayed = numBRPlayed;
         this.nTotalGamesPlayed = nTotalGamesPlayed;
    }
}
