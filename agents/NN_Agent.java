package agents; 

import jade.core.AID;//jade agent identifier
import jade.core.Agent;//The Agent class is the common superclass for user defined software agents
import jade.core.behaviours.CyclicBehaviour;//Atomic behaviour that must be executed forever.
import jade.domain.DFService;//This class provides a set of static methods to communicate with a DF Service 
                             //that complies with FIPA specifications.
import jade.domain.FIPAAgentManagement.DFAgentDescription;//This class implements the concept of the 
                                                          // fipa-agent-management  ontology representing the 
                                                          //description of an agent in the DF catalogue
import jade.domain.FIPAAgentManagement.ServiceDescription;//This class models a service data type.
import jade.domain.FIPAException;//This class represents a generic FIPAException, i.e. one of NotUnderstood,
                                 //Failure,Refuse, as defined in jade.domain.FIPAAgentManagement.
import jade.lang.acl.ACLMessage;//The class ACLMessage implements an ACL message compliant to the FIPA 2000 

import java.util.ArrayList;
import java.util.Random;
//"FIPA ACL Message Structure Specification" (fipa000061) specifications.
//import java.util.Random;//An instance of this class is used to generate a stream of pseudorandom numbers.

public class NN_Agent extends Agent {

    private State state;
    private AID mainAgent;
    private ArrayList<PlayerInformation> players = new ArrayList<>();
    private int N, R, E, Ei, Ri, Raux; //numPlayers, rounds, endowment, Probabily of disaster, number of Games. Endowment inicial
    private int myID;
    private ACLMessage msg; //message
    private int[] msgIds;
    private int myPossitionInArray;
    
    private int move;
    private int iGridSide;
    //private int jGridSide;            // Side of the SOM 2D grid
    //private int iCellSize;            // Size in pixels of a SOM neuron in the grid
    //private int[][] iNumTimesBMU;         // Number of times a cell has been a BMU
    //private int[] iBMU_Pos = new int[2];     // BMU position in the grid

    private int inputSize;               // Size of the input vector
    //private int iRadio;                 // BMU radio to modify neurons
    //private double dLearnRate = 1.0;          // Learning rate for this SOM
    //private double dDecLearnRate = 0.999;       // Used to reduce the learning rate
    //private double[] dBMU_Vector = null;        // BMU state
    private double[][] dGrid;             // SOM square grid + vector state per neuron
    private double[][] electionMatrix = new double[5][10];
    private int[] movesPerGame = new int[10];
    private double win = 0.8, fail = 0.3;
    private boolean ganador;

    protected void setup() {//Inicialización del agente
        state = State.s0NoConfig; //Se le asigna el estado 0 no configurado

        //Register in the yellow pages as a player
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());//Se le asigna un id 
        myID = Integer.parseInt(getAID().getName().split("@")[0].split("r")[1]); 
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");//tipo jugador
        sd.setName("Game");//nombre Juego
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new Play());//Se le añade el comportamiento al jugador
        Random r = new Random();
        for(int i = 0; i < 10; i++){
            movesPerGame[i] = r.nextInt(5);      
        }
        System.out.println("mmmmmmmmmmmmmmmmmmmmmm");
        for (int i = 0; i < 5; i++){
            for(int j = 0; j < 10; j++){
                electionMatrix[i][j] = 0.2*(i + 1);
            }
        }
        System.out.println("NN_Agent " + getAID().getName() + " is ready.");

    }

    protected void takeDown() {//Finalización del Agente
        //Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        System.out.println("RL_Agent " + getAID().getName() + " terminating.");
    }

    private enum State {//Máquina de estados que se usará para el comportamiento
        s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
    }

    private class Play extends CyclicBehaviour {//Comportamiento del agente random
        @Override //Forzar al compilador a comprobar en tiempo de compilación que estás sobrescribiendo correctamente un método
        public void action() {
            //System.out.println(getAID().getName().split("@")[0] + ": " + state.name());
            msg = blockingReceive();//Recibe un mensaje ACL de la cola de mensajes del agente. Este método bloquea
                                    // y suspende todo el agente hasta que un mensaje esté disponible en la cola.
            
            if (msg != null) {
                //System.out.println(getAID().getName().split("@")[0] + ": " + " received " + msg.getContent() + " from " + msg.getSender().getName()); //DELETEME
                //-------- Agent logic
                switch (state) {//switch para la máquina de estados
                    case s0NoConfig://Obtiene un mensaje comprueba los parámetros, si están Ok pasa al siguiente estado
                        //If INFORM Id#_#_,_,_,_ PROCESS SETUP --> go to state 1
                        //Else ERROR
                        if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) { //ACLmessage.INFORM = 7,
                                                                                                                // constant identifying the FIPA performative
                            boolean parametersUpdated = false;
                            try {
                                parametersUpdated = validateSetupMessage(msg);
                                resetMatrix();
                            } catch (NumberFormatException e) {
                                System.out.println(getAID().getName().split("@")[0] + ": " + state.name() + " - Bad message");
                            }
                            if (parametersUpdated) state = State.s1AwaitingGame;//Si todo OK pasamos al estado 1

                        } else {
                            System.out.println(getAID().getName().split("@")[0] + ": " + state.name() + " - Unexpected message");
                        }
                        break;
                    case s1AwaitingGame://2 opciones: actualización de datos como en el caso anterior,
                                        //            o actualización de ids
                        //If INFORM NEWGAME#_,_ PROCESS NEWGAME --> go to state 2
                        //If INFORM Id#_#_,_,_,_ PROCESS SETUP --> stay at s1
                        //Else ERROR
                        //Todo I probably should check if the new game message comes from the main agent who sent the parameters
                        if (msg.getPerformative() == ACLMessage.INFORM) { //msg.getPerformative, return the integer representing the performative of this object
                            if (msg.getContent().startsWith("Id#")) { //Game settings updated
                                try {
                                    validateSetupMessage(msg);
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName().split("@")[0] + ": " + state.name() + " - Bad message");
                                }
                            } else if (msg.getContent().startsWith("NewGame#")) {
                                boolean gameStarted = false;
                                try {
                                    gameStarted = validateNewGame(msg.getContent());
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName().split("@")[0] + ": " + state.name() + " - Bad message");
                                }
                                if (gameStarted) state = State.s2Round;
                            }
                        } else {
                            System.out.println(getAID().getName().split("@")[0] + ": " + state.name() + " - Unexpected message");
                        }
                        break;
                    case s2Round://Envía la jugada, termina el juego o no pasa nada.
                        //If REQUEST POSITION --> INFORM POSITION --> go to state 3
                        //If INFORM CHANGED stay at state 2
                        //If INFORM ENDGAME go to state 1
                        //Else error
                        if (msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().startsWith("Action")) {
                            ACLMessage msgS = new ACLMessage(ACLMessage.INFORM);
                            msgS.addReceiver(mainAgent);
                            R--;//Restamos 1 al número de rondas
                            move = selectMove(movesPerGame);
                            movesPerGame[Raux] = move;
                            Raux++;
                            //System.out.println(getAID().getName().split("@")[0] + ": " + " sent " + msgS.getContent());
                            msgS.setContent("Action#" + move);//selección de jugada que va a realizar
                            //System.out.println("Mi eleccion es: " + move);
                            send(msgS);
                            E -= move;//restamos a nuestro endowment el valor aleatorio de la acción escogida
                            state = State.s3AwaitingResult;
                        } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("GameOver#")) {
                            boolean gameOver = false;
                            try {
                                System.out.println(""+movesPerGame[0]+movesPerGame[1]+movesPerGame[2]+movesPerGame[3]+movesPerGame[4]+movesPerGame[5]+movesPerGame[6]+movesPerGame[7]+movesPerGame[8]+movesPerGame[9]);
                                gameOver = validateGameOver(msg.getContent());
                                trainSOM();
                            } catch (NumberFormatException e) {
                                System.out.println(getAID().getName().split("@")[0] + ": " + state.name() + " - Bad message");
                            }
                            if(gameOver) state = State.s1AwaitingGame;
                        } else {
                            System.out.println(getAID().getName().split("@")[0] + ": " + state.name() + " - Unexpected message:" + msg.getContent());
                        }
                        break;
                    case s3AwaitingResult://Al obtener los resultados va a la siguiente ronda
                        //If INFORM RESULTS --> go to state 2
                        //Else error
                        if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {
                            //Process results
                            boolean gameResults = false;
                            try {
                                gameResults = validateResults(msg.getContent());
                                sumarContribuciones();
                                
                            } catch (NumberFormatException e) {
                                System.out.println(getAID().getName().split("@")[0] + ": " + state.name() + " - Bad message");
                            }
                            if(gameResults && R > 0) state = State.s2Round;//Si los resultados están bien y aun no ha acabado el juego, esperamos a que nos pida la acción
                            if(gameResults && R == 0) state = State.s2Round;//Si los resultados están bien y ha acabado el juego, esperamos el nuevo juego
                        } else {
                            System.out.println(getAID().getName().split("@")[0] + ": " + state.name() + " - Unexpected message");
                        }
                        break;
                }
            }
        }

        /**
         * Validates and extracts the parameters from the setup message
         *
         * @param msg ACLMessage to process
         * @return true on success, false on failure
         */
        private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {//validación y posterior actualización de datos 
            int tN, tE, tR;
            String msgContent = msg.getContent();

            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 3) return false;
            if (!contentSplit[0].equals("Id")) return false;

            String[] parametersSplit = contentSplit[2].split(",");
            if (parametersSplit.length != 5) return false;
            tN = Integer.parseInt(parametersSplit[0]);
            tE = Integer.parseInt(parametersSplit[1]);
            tR = Integer.parseInt(parametersSplit[2]);

            //At this point everything should be fine, updating class variables
            mainAgent = msg.getSender();
            N = tN;
            E = tE;
            Ei = tE;
            R = tR;
            Ri = tR;
            return true;
        }

        /**
         * Processes the contents of the New Game message
         * @param msgContent Content of the message
         * @return true if the message is valid
         */
        public boolean validateNewGame(String msgContent) {//validación de datos y actualización de Ids.
            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 2) return false;
            if (!contentSplit[0].equals("NewGame")) return false;
            String[] idSplit = contentSplit[1].split(",");
            if (idSplit.length < 2 ) return false;
            msgIds = new int[idSplit.length];

            for(int i = 0; i <= idSplit.length - 1; i++){
                msgIds[i] = Integer.parseInt(idSplit[i]);
                //System.out.println(idSplit[i]);
                players.add(new PlayerInformation(msgIds[i]));
                
            }

            for(int i = 0; i <= idSplit.length - 1; i++){
                if(msgIds[i] == myID){
                    myPossitionInArray = i;
                    //System.out.println("Mi posición en el array es: " + myPossitionInArray);
                    break;
                }
            }
            
            E = Ei;//Actualizamos el endowment para el siguiente juego
            R = Ri;//Actualizamos las rondas para el siguiente juego
            Raux = 0;
            return true;
        }

        public boolean validateResults(String msgContent){
            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 2) return false;
            if (!contentSplit[0].equals("Results")) return false;
            String[] contributionsSplit = contentSplit[1].split(",");
            if(contributionsSplit.length != N) return false;
            int[] msgContribution = new int[contributionsSplit.length];

            for(int i = 0; i <= contributionsSplit.length - 1; i++){
                msgContribution[i] = Integer.parseInt(contributionsSplit[i]);
                players.get(i).action = msgContribution[i];
            }

            
            return true;
        }

        public void sumarContribuciones(){
            for(int i = 0; i < N -1; i++){
                players.get(i).contribution += players.get(i).action;
            }
        }

        public boolean validateGameOver(String msgContent){
            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 2) return false;
            if (!contentSplit[0].equals("GameOver")) return false;
            String[] resultsSplit = contentSplit[1].split(",");
            if(resultsSplit.length != N) return false;
            int[] msgResult = new int[resultsSplit.length];
            ganador = true;
            for(int i = 0; i < msgResult.length; i++){
                if(i != myPossitionInArray){
                    if(msgResult[myPossitionInArray] < msgResult[i]){
                        ganador = false;
                        break;
                    }
                }
            }

            return true;
        }

        public void resetMatrix(){
            iGridSide = 5;
            dGrid = new double[5][R];
            for(int i = 0; i < 5; i++){
                for(int j = 0; j < R; j++){

                    dGrid[i][j] = (double) Math.random();
                    
                }
            }
        }

        public int selectMove(int[] movesPerGameAux){
            double dNorm, dNormMin = Double.MAX_VALUE;
            inputSize = movesPerGameAux.length;
            int aux = 0;
            int jugada = 0;
            int election = 0;
            inputSize = movesPerGameAux.length;
            for (int i=0; i < iGridSide; i++) {           // Finding the BMU
        
                dNorm = 0;
                for (int k=0; k<inputSize; k++){           // Calculating the norm
                    dNorm += (movesPerGameAux[k] - dGrid[i][Raux]) * ((movesPerGameAux[k] - dGrid[i][Raux]));
            
                    if (dNorm < dNormMin) {
                        dNormMin = dNorm; 
                        jugada = i;
                    }
                }                       // Leaving the loop with the x,y positions for the BMU

            }
            for (int i = 0; i < 5; i++) {
                if(i != 4){
                    if (electionMatrix[i][Raux] < jugada && jugada < electionMatrix[i+1][Raux]) {

                        jugada = election;
                    }
                }else{
                     if (electionMatrix[i][Raux] < jugada && jugada < electionMatrix[0][Raux]) {

                        jugada = election;
                    }
                }
                election++;
            }
            return jugada;
        }

        public void trainSOM(){
            
            String[] endowments = msg.getContent().split("#")[1].split(",");
            int myEndowment = Integer.parseInt(endowments[myPossitionInArray]);
            boolean lose = false, boom = false;
            int allHaveZero = 0;

            for(int i = 0; i < endowments.length; i++) {

                if(myEndowment < Integer.parseInt(endowments[i]) ){ 
                    lose = true;
                }
                if(Integer.parseInt(endowments[i]) == 0){
                    allHaveZero++;
                }
            }
            if(allHaveZero == endowments.length) boom = true;
                    
            for(int i = 0; i < movesPerGame.length; i++) ponderation(i, movesPerGame[i], lose, boom);
                
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 10; j++) {
                    electionMatrix[i][j] = electionMatrix[i][j]/electionMatrix[4][j];
                }
            }
        }
    }

    public void ponderation(int n, int pos, boolean l, boolean b){
        if(!l && !b){
            electionMatrix[pos][n] = win*(1.0 - electionMatrix[pos][n]);

        }
        if(l && !b){
            electionMatrix[pos][n] = fail*(1.0 - electionMatrix[pos][n]);

        }
    }

    public class PlayerInformation {

        int id;
        int contribution;//contribución de la partida
        int action;//acción de una ronda
        int total;//endowment total de todos los juegos

        public PlayerInformation(int i) {
            id = i;
            contribution = 0;
            action = 0;
            total = 0;
        }
    }

}
