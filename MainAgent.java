import agents.*;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;//An atomic behaviour. This abstract class models behaviours
                                            // that are made by a single, monolithic task and cannot be interrupted.
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.*;

import java.io.PrintStream;
import java.util.ArrayList;

public class MainAgent extends Agent {

    private GUI gui; //The Graphic User Interface
    private AID[] playerAgents;
    private GameParametersStruct parameters = new GameParametersStruct();//Struct with some parameters of the CDR Game
    private String[] playerNames;//Names of the players
    private ArrayList<PlayerInformation> totalPlayers; // Player list with data about them
    private ArrayList<PlayerInformation> playersToPlay = new ArrayList<>(); // Players selected to play the game
    private Object[][] tableGui; //Table data in the gui
    private int totalContributions; //total contributions in one game
    private double randNum;//número aleatorio para comprobar si el threshold se supera o no
    private int min;
    private ArrayList<PlayerInformation> finalistPlayers;
    private int finalRound;
    private Boolean stop = false;//to stop the execution of the game (and continue).
    private long speed;//To delay the execution.
    //parameters to visualizated in the gui.
    private int numBRPlayed = 0;//number of Battle Royal Played
    private int numberOfGen;//number of Gen played
    private int nTotalGamesPlayed = 0;//number of Games played

    @Override
    protected void setup() {
        gui = new GUI(this); //iniciamos el gui
        System.setOut(new PrintStream(gui.getLoggingOutputStream()));

        updatePlayers();//llamamos a los jugadores
        gui.logLine("Agent " + getAID().getName() + " is ready.");
    }

    public int updatePlayers() {//update players and set the gui table
        gui.logLine("Updating player list");
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                gui.logLine("Found " + result.length + " players");
            }
            playerAgents = new AID[result.length];
            for (int i = 0; i < result.length; ++i) {
                playerAgents[i] = result[i].getName();
            }
            parameters.T = (parameters.N*parameters.E/2);
        } catch (FIPAException fe) {
            gui.logLine(fe.getMessage());
        }
        //Provisional
        playerNames = new String[playerAgents.length];
        for (int i = 0; i < playerAgents.length; i++) {
            playerNames[i] = playerAgents[i].getName();
        }
        gui.setPlayersUI(playerNames);


        totalPlayers = new ArrayList<>();
        for (AID a : playerAgents) {//genera los ids de los agentes y añadira los agentes a la lista de los jugadores
            gui.logLine(a.getName().split("@")[0].split("r")[1]);
            totalPlayers.add(new PlayerInformation(a));
        }
        tableGui = new Object[totalPlayers.size()][5];//la tabla tendrá tantas filas como jugadores y 5 columnos
                                                //Las 5 columnas son: Jugador, id, numGenPlayed, payoff, average
        gui.logLine(totalPlayers.toString());
        ordenar(totalPlayers);
        gui.logLine(totalPlayers.toString());
        pintarTabla();
        gui.logLine("El tamaño de totalPlayers es: " + totalPlayers.size());

        return 0;
    }

    public void deletePlayer(int id){//To delete a player by ID
        for(PlayerInformation player: totalPlayers){
            if(id == player.id){
                totalPlayers.remove(player);
                gui.logLine("El jugador con id " + player.id + " ha sido eliminado.");
                break;
            }
        }
        ordenar(totalPlayers);
        pintarTabla();
    }

    public int newGame(int Round) {
        finalRound = Round;
        addBehaviour(new GameManager());//añadimos el comportamiento para el nuevo juego
        min = 0;
        return 0;
    }

    public void setSpeed(String s){//set de delay time in the execution
        speed = Long.parseLong(s);
    }

    public void continueExecution(){
        stop = false;
    }
    public void stopExecution(){
        stop = true;
    }
    
    public void changeParam(String param){
        
        parameters = new GameParametersStruct(Integer.parseInt(param.split(",")[0]),
                                              Integer.parseInt(param.split(",")[1]),
                                              Integer.parseInt(param.split(",")[2]),
                                              Double.parseDouble(param.split(",")[3]),
                                              parameters.numGames);
        pintarTabla();
    }

    public void changeNG(String num){

        parameters.setNumGames(Integer.parseInt(num));
        pintarTabla();
    }

    public void resetTotlaPlayers(){//Reset All players

        updatePlayers();
        totalPlayers = new ArrayList<>();
        for (AID a : playerAgents) {//genera los ids de los agentes y añadira los agentes a la lista de los jugadores
            gui.logLine(a.getName().split("@")[0].split("r")[1]);
            totalPlayers.add(new PlayerInformation(a));
        }

    }

    public void resetActualPlayers(){//Reset the players that stay in the game

        for(PlayerInformation player: totalPlayers){
            player.av = 0;
            player.er = parameters.E;
            player.numGenerationsPlayed = 0;
            player.total = 0;
        }
        
    }

    public void ordenar(ArrayList<PlayerInformation> p){// a short function to PlayerInformation arrays
        Collections.sort(p);
    }

    private void pintarTabla(){//Método que crea una string para enviarselo al gui y que actualice la tabla y valores relacionados con los paremtros del juego

        int position = 0;
        tableGui = new Object[totalPlayers.size()][5];
        for(PlayerInformation player: totalPlayers){
            tableGui[position][0] = player.aid.getName().split("@")[0];
            tableGui[position][1] = player.id;
            tableGui[position][2] = player.numGenerationsPlayed;
            tableGui[position][3] = player.total;
            tableGui[position][4] = player.av;
            position++;
        }
        gui.setTable(tableGui,parameters.N, parameters.E, parameters.R, parameters.Pd, parameters.Gen, parameters.numGames, numberOfGen, numBRPlayed, nTotalGamesPlayed);
        gui.setPlayersUI(playerNames);
    }

    /**
     * In this behavior this agent manages the course of a match during all the
     * rounds.
     */
    private class GameManager extends SimpleBehaviour {

        @Override
        public void action() {
            //Assign the IDs
            if(finalRound == 0){
                battleRoyal();
            }else{
                finalRound();              
            }
            
        }

        private void battleRoyal(){
            nTotalGamesPlayed = 0;
            numBRPlayed = 0;

            //Initialize (inform ID)
            for (PlayerInformation player : totalPlayers) {//envía la información a cada agente
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("Id#" + player.id + "#" + parameters.N + "," + parameters.E + "," + parameters.R + "," + parameters.Pd + "," + parameters.numGames);
                msg.addReceiver(player.aid); //envio de los parametros a cada Agente N,E,R,Pd,NumGames.
                //gui.logLine(msg.getContent());
                send(msg);
            }

            while(totalPlayers.size() > 5){
                numBRPlayed++;
                for(int w = 0; w < parameters.Gen; w++){
                    numberOfGen = w;
                    playersToPlay = playerSelection();
                    ordenar(playersToPlay);
                    for(int z = 0; z < parameters.numGames; z++){
                        playGame();
                        waitALittleBit();
                        speedFunction();
                        nTotalGamesPlayed++;
                    }
                    for(PlayerInformation player: playersToPlay){//actualizamos valores de los jugadores: número de gneraciones jugadas
                        // y el average total siendo av=total/numGenPlayed
                        player.numGenerationsPlayed++;
                        player.av = player.total/player.numGenerationsPlayed;
                    }
                    updatePlayerPrameters(playersToPlay);
                    ordenar(totalPlayers);
                    if(numberOfGen % 10 == 0){
                        pintarTabla();
                    }
                }
                killTheLoser();
            }
            finalistSelection();
            preparationForTheFinal();
            pintarTabla();
        }

        private void preparationForTheFinal(){//preparativos para la gran final
            totalPlayers.removeAll(totalPlayers);
            totalPlayers.addAll(finalistPlayers);
            finalistPlayers.removeAll(finalistPlayers);
            ordenar(totalPlayers);
            for( PlayerInformation player: totalPlayers){
                player = new PlayerInformation(player.aid);
                finalistPlayers.add(player);
            }
            ordenar(finalistPlayers);
            totalPlayers.removeAll(totalPlayers);
            totalPlayers.addAll(finalistPlayers);
            playersToPlay.removeAll(playersToPlay);
            playersToPlay.addAll(finalistPlayers);
            numberOfGen = 0;
        }

        private void finalRound(){//Final fase of the CDR Game.

            for(int i = 0; i < 1000; i++){
                playGame();
                waitALittleBit();
                speedFunction();
                pintarTabla();
                nTotalGamesPlayed++;
            }
            updatePlayerPrameters(playersToPlay);
            int winner = 0;
            int idWinner = 0;
            gui.logLine(totalPlayers.toString());
            for(PlayerInformation player: totalPlayers){
                if(player.total > winner){
                    winner = player.total;
                    idWinner = player.id;
                }
            }
            pintarTabla();
            gui.logLine("El ganador es el jugador con id: " + idWinner);
            gui.logLine("Con un payoff total de: " + winner);
        }

        private void killTheLoser(){//search, find and delete the plyaer(s) with less payoff

            int minPayoff = totalPlayers.get(0).av, numLosers = 0;

            for(PlayerInformation player: totalPlayers){//to find the min payoff
                if(minPayoff > player.av){
                    minPayoff = player.av;
                }
            }
            gui.logLine("El perdedor tiene un payoff de: " + minPayoff);
            for(PlayerInformation player: totalPlayers){//to find the number of players with the min payoff
                if(minPayoff == player.av){
                    numLosers++;
                }
            }
            gui.logLine("Hay " + numLosers + " con el mismo payoff");
            while(numLosers >= 1){
                for(PlayerInformation player: totalPlayers){//Kill the losers
                    if(minPayoff == player.av){
                        totalPlayers.remove(player);
                        gui.logLine("Jugador eliminado. Ahora hay " + totalPlayers.size() + " jugadores.");
                        break;
                    }
                }
                if(totalPlayers.size() == 5){
                    break;
                }
                numLosers--;
            }
            gui.logLine(totalPlayers.toString());
        }

        private void waitALittleBit(){//To stop and continue function
            while(stop){
                try{
                    Thread.sleep(50);
                } catch(Exception e){
                    e.printStackTrace();
                }

            }
        }

        private void speedFunction(){//To dealy the execution
            try{
                Thread.sleep(speed);
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        private void playGame() {//A jugar!!
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            String s;
            int n;
            ACLMessage move;
            s = "NewGame#";
            n = 0;
            for (PlayerInformation player : playersToPlay) {//comunicamos el nuevo juego a los jugadores
                msg.addReceiver(player.aid);
                if (n != (playersToPlay.size() - 1)) s = s + player.id + ",";
                else s = s + player.id;
                player.er = parameters.E;
                n++;
            }
            msg.setContent(s);
            //gui.logLine(msg.getContent());
            send(msg);
            totalContributions = 0;

            for(int i = 0; i < parameters.R; i++){//Bucle para las rondas de un juego
                
                //n = 0;

                for (PlayerInformation player: playersToPlay){//Pedimos y recibimos la acción a cada jugador
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setContent("Action");
                    msg.addReceiver(player.aid);
                    send(msg);
                    move = blockingReceive();
                    player.action = Integer.parseInt(move.getContent().split("#")[1]);
                    totalContributions += player.action;//la contribución total será la suma de todas las acciones de cada jugador
                    player.er -= player.action;//restamos al endowment personal de cada jugador su acción
                    
                }

                msg = new ACLMessage(ACLMessage.INFORM);
                n = 0;
                s = "Results#";
                for (PlayerInformation player : playersToPlay) {
                    msg.addReceiver(player.aid);
                    if (n != (playersToPlay.size() - 1)) s = s + player.action + ",";
                    else s = s + player.action;
                    n++;
                }
                msg.setContent(s);
                send(msg);
            }

            randNum = (Math.random());//generamos un numero aleatorio entre 0 y 1;
            //Si las contribuciones son mayores o iguales que el threshold  o
            //el número aleatorio, entre 0 y 1, es mayor a 0.8
            //El resultado es el endowment acumulado
            //sino es cero
            if(totalContributions >= parameters.T || randNum > parameters.Pd){
                msg = new ACLMessage(ACLMessage.INFORM);
                n = 0;
                s = "GameOver#";
                for (PlayerInformation player : playersToPlay) {
                    msg.addReceiver(player.aid);
                    player.total += player.er;
                    if (n != (playersToPlay.size() - 1)) s = s + player.er + ",";
                    else s = s + player.er;
                    n++;
                }
                msg.setContent(s);
                send(msg);
            }else{
                msg = new ACLMessage(ACLMessage.INFORM);
                n = 0;
                s = "GameOver#";
                for (PlayerInformation player : playersToPlay) {
                    
                    msg.addReceiver(player.aid);
                    player.total += player.er;
                    if (n != (playersToPlay.size() - 1)) s = s + "0,";
                    else s = s + "0";
                    n++;
                }
                msg.setContent(s);
                send(msg);
            }    
        }

        private void finalistSelection(){

            int max = 0;
            int id = 0;
            finalistPlayers  = new ArrayList<>();
            ordenar(totalPlayers);
            for(int i = 0; i < 5; i++){
                for(PlayerInformation player: totalPlayers){
                    if(player.av > max){
                        max = player.av;
                        id = totalPlayers.indexOf(player);
                    }
                }
                finalistPlayers.add(totalPlayers.get(id));
                totalPlayers.remove(id);
                max = 0;
            }
            ordenar(finalistPlayers);
            gui.logLine("Los finalistas son: " + finalistPlayers.toString());

        }

        private void updatePlayerPrameters(ArrayList<PlayerInformation> p){

            for(PlayerInformation pl: p){
                for(int y = 0; y < totalPlayers.size(); y++){
                    if(totalPlayers.get(y).id == pl.id){
                        totalPlayers.remove(y);
                        totalPlayers.add(pl);
                    }
                }
            }

        }

        private ArrayList<PlayerInformation> playerSelection(){//The random selection of players to play the game, start with the min Gen played.

            ArrayList<PlayerInformation> selection = new ArrayList<>();
            ArrayList<PlayerInformation> partialSelection = new ArrayList<>();
            Boolean ready = false;
            

            while(!ready){
                partialSelection = findMinGamesPlayed(totalPlayers, min);//find the players with min Gen played
                if(partialSelection.size() > (parameters.N - selection.size())){//If there are more players with the same Gen played than N --> select randomly
                    partialSelection = randomSelection(partialSelection, parameters.N - selection.size());
                    for(PlayerInformation player: partialSelection) selection.add(player);
                    ready = true;
                }else{
                    if(partialSelection.size() == (parameters.N - selection.size())){// If the player withmin Gen played are = N --> start play
                        for(PlayerInformation player: partialSelection) selection.add(player);
                        ready = true;
                    }else{
                        for(PlayerInformation player: partialSelection) selection.add(player);//If there are less players with min Gen played than N --> find the next min Gen played
                        min++;
                    }
                }
            }

            return selection;
        }

        //n --> number of player to select randomly
        //To select ramdonly a number of players
        private ArrayList<PlayerInformation> randomSelection(ArrayList<PlayerInformation> p, int n){

            int r;
            ArrayList<PlayerInformation> randomPlayers = new ArrayList<>();

            for(int i = 0; i < n; i++){
                r = (int)(Math.random()*p.size());
                randomPlayers.add(p.get(r));
                p.remove(r);
            }

            return randomPlayers;

        }

        //To find the player with the  Min Gen played 
        private ArrayList<PlayerInformation> findMinGamesPlayed(ArrayList<PlayerInformation> p, int min){

            ArrayList<PlayerInformation> selection = new ArrayList<>();

            for(PlayerInformation player: p) if(min == player.numGenerationsPlayed) selection.add(player);
        
            return selection;
        }

        @Override
        public boolean done() {
            return true;
        }

        public void reset(){//To reset the players
            totalPlayers.removeAll(totalPlayers);
            for (AID a : playerAgents) {//genera los ids de los agentes y añadira los agentes a la lista de los jugadores
                gui.logLine(a.getName().split("@")[0].split("r")[1]);
                totalPlayers.add(new PlayerInformation(a));
            }
            ordenar(totalPlayers);
            pintarTabla();
        }
    }

    public class PlayerInformation implements Comparable<PlayerInformation> {//informacion de los jugadores

        AID aid;
        String name;
        int id;
        int er;// endowment restante
        int action;//The action selected in 1 round
        int total;//total endowment of the games played
        int av; //average
        int numGenerationsPlayed;

        @Override
        public int compareTo(PlayerInformation p){
            if(this.id < p.id) return -this.id;
            else return p.id;
        }

        public String toString(){
            return Integer.toString(id);
        }

        public PlayerInformation(AID a) {
            aid = a;
            id = Integer.parseInt(a.getName().split("@")[0].split("r")[1]);
            er = parameters.E;
            action = 0;
            total = 0;
            av = 0;
            numGenerationsPlayed = 0;

        }

    }

    public class GameParametersStruct { //parametros del juego

        int N; //número de jugadores
        int R; //número de rondas
        int E; //endowment
        double Pd; //Probabilidad desastre
        int T; //Umbral
        int numGames;
        int Gen;

        public GameParametersStruct() {
            N = 5;
            R = 10;
            E = 40;
            Pd = 0.8;
            T = (E*N)/2;
            numGames = 100;
            Gen = 250;
        }

        public GameParametersStruct(int n, int e, int r, double pd, int nG){
            N = n;
            R = r;
            E = e;
            Pd = pd;
            T = (E*N)/2;
            numGames = nG;
        }

        public void setNumGames(int numG){
            numGames = numG;
        }
    }
}
