package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;//create behaviours that keep executing continuously 
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.leap.Serializable;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

//QLEARNIN AGENT!!!!!
public class RL_Agent2 extends Agent{
    final double dDecFactorLR = 0.99;   // Value that will decrement the learning rate in each generation
    final double dEpsilon = 0.95;    // Used to avoid selecting always the best action
    final double dMINLearnRate = 0.05;   // We keep learning, after convergence, during 5% of times

    StateAction oLastStateAction = null;
    double dLearnRate = 0.90;
    final double dGamma = 0.5;
    private State state;
    private AID mainAgent; //tables use this to record agent names and addresses.
    private int myId ;
    private int E;//player parameters
    private ACLMessage msg;
    int iNumActions;
    //private int electionMatrix[][] = new int[5][10]
    //private int movesPerGame[] = {0,1,2,3,2,4,1,2,0,4}
    double dReward = 0;
    int action = 0;
    ArrayList<StateAction> oVStateActions = new ArrayList<>();

    


    public int getE() {
        return E;
    }
    public void setE(int e) {
        this.E = e;
    }

    public  int iNewAction ;
    StateAction oPresentStateAction = new StateAction("FIRST", 5);

    protected void setup() {
        state = State.s0NoConfig;

        //Register in the yellow pages as a player
        DFAgentDescription dfd = new DFAgentDescription();// representing the description of an agent in the DF catalogue.
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();//models a service data type.
        sd.setType("Player");
        sd.setName("Game");
        dfd.addServices(sd);//Add a service description to the service slot collection of this object.
        try {
            DFService.register(this, dfd);//register an agent
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new Play());//This method adds a new behaviour to the agent.
        System.out.println("RandomAgent " + getAID().getName() + " is ready.");//indicate the players name

    }

    protected void takeDown() {
        //Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        System.out.println("RandomPlayer " + getAID().getName() + " terminating.");
    }

    private enum State {
        s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult //posible states of an agent
        
    }

    private class Play extends CyclicBehaviour {
        @Override
        public void action() {
           // System.out.println(getAID().getName() + ":" + state.name());
            msg = blockingReceive();//Receives an ACL message from the agent message queue
            if (msg != null) {
                //System.out.println(getAID().getName() + " received " + msg.getContent() + " from " + msg.getSender().getName()); //DELETEME
                //-------- Agent logic
                switch (state) {
                    case s0NoConfig:
                        //If INFORM Id#_#_,_,_,_ PROCESS SETUP --> go to state 1
                        //Else ERROR
                        if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
                            boolean parametersUpdated = false;
                            try {
                                parametersUpdated = validateSetupMessage(msg);
                            } catch (NumberFormatException e) {
                                System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                            }
                            if (parametersUpdated) state = State.s1AwaitingGame;

                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                    case s1AwaitingGame:
                        //If INFORM NEWGAME#_,_ PROCESS NEWGAME --> go to state 2
                        //If INFORM Id#_#_,_,_,_ PROCESS SETUP --> stay at s1
                        //Else ERROR
                        //TODO I probably should check if the new game message comes from the main agent who sent the parameters
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            if (msg.getContent().startsWith("Id#")) { //Game settings updated
                                try {
                                    validateSetupMessage(msg);
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                            } else if (msg.getContent().startsWith("NewGame#")) {
                                boolean gameStarted = false;
                                try {
                                    gameStarted = validateNewGame(msg.getContent());
                                   
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                                if (gameStarted) state = State.s2Round;
                            }
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                    case s2Round:
                        //If REQUEST Action --> INFORM POSITION --> go to state 3
                        //If INFORM CHANGED stay at state 2
                        //If INFORM ENDGAME go to state 1
                        //Else error
                        if (msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().startsWith("Action")) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(mainAgent);

                            action = vGetNewActionQLearning(oPresentStateAction.sState, oPresentStateAction.dValAction.length, dReward);
                            msg.setContent("Action#" + action);
                            setE(getE() - action);
			
                            if(getE() < 5) {
                                oPresentStateAction = new StateAction("LESS", getE());
                            }
                           
                           
                            send(msg);
                            state = State.s3AwaitingResult;
                        //} else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Changed#")) {
                            // Process changed message, in this case nothing
                        } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("GameOver")) {
                            state = State.s1AwaitingGame;
                            
                            
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message:" + msg.getContent());
                        }
                        break;
                    case s3AwaitingResult:
                        //If INFORM RESULTS --> go to state 2
                        //Else error
                        if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {
                            //Process results
                            parametersQLearning(msg,action);
                            
                            state = State.s2Round;

                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                }
            }
        }

        
        

        public void ActualizaParameterE(int action) {

			setE(getE() - action);
			
			if(getE() < 5) {
				oPresentStateAction = new StateAction("LESS", getE());
			}
		}

        private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {
            int tN, tE, tR,tNumGame, tMyId;//parameters
            double tPd;
            String msgContent = msg.getContent();

            String[] contentSplit = msgContent.split("#");//separate message by #
            if (contentSplit.length != 3) return false;//comrobate if there are 3 parts
            if (!contentSplit[0].equals("Id")) return false;
            tMyId = Integer.parseInt(contentSplit[1]);

            String[] parametersSplit = contentSplit[2].split(",");
            if (parametersSplit.length != 5) return false;
            tN = Integer.parseInt(parametersSplit[0]);
            tE = Integer.parseInt(parametersSplit[1]);
            tR = Integer.parseInt(parametersSplit[2]);
            tPd = Float.parseFloat(parametersSplit[3]);
            tNumGame = Integer.parseInt(parametersSplit[4]);

            //At this point everything should be fine, updating class variables
            mainAgent = msg.getSender();
            setE(tE);
            myId = tMyId;
            return true;
        }

        public boolean validateNewGame(String msgContent) {
            
            int msgID; 

            ArrayList<Integer> oponents = new ArrayList<Integer>();

            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 2) return false;
            if (!contentSplit[0].equals("NewGame")) return false;
            String[] idSplit = contentSplit[1].split(",");
           // if (idSplit.length != N) return false;

            for(int i = 0;i < idSplit.length; i++){
                msgID = Integer.parseInt(idSplit[i]);

                if (myId == msgID) {
      
                } else {
                    oponents.add(msgID);
                   
                }
            }
            if(oponents.size() == (idSplit.length-1)){
                return true;
            }else{
                return false;

            }

           
            
        }

       

		public void parametersQLearning(ACLMessage msg, int lastAction) {
			int mayor = 0, menor = 0, coincide = 0;
		


			String[] actions = msg.getContent().split("#");
			String[] action = actions[1].split(",");

			for(int i = 0; i < action.length;i++) {

				if(i == myId){ 
                    continue;
                }else{
				
                    if(Integer.parseInt(action[i]) < lastAction)  menor++;
                    if(Integer.parseInt(action[i]) > lastAction) mayor++;
                    if(Integer.parseInt(action[i]) == lastAction) coincide++;

                }
				
			}
			if((mayor > coincide) & (mayor > menor)) {
				oPresentStateAction.sState = "mayor";
				dReward = -1 ;
					
			}
			
			if((coincide > mayor) & (coincide > menor) ) {
				
				oPresentStateAction.sState = "coincide";
				dReward = 0;
			
			}
			
			if((menor > mayor) & (menor > coincide)){
				oPresentStateAction.sState = "menor";
				dReward = 1;
				
			}
			
			oPresentStateAction.sState = "coincide";
			dReward = 0;
		
		}
    
public int vGetNewActionQLearning (String sState, int iNActions, double dReward) {
    boolean bFound;
    int iBest=-1, iNumBest=1,iAction = 0;
    double  dQmax;
    StateAction oStateAction;
   
    bFound = false;       // Searching if we already have the state
    for (int i=0; i<oVStateActions.size(); i++) {
        oStateAction = oVStateActions.get(i); 
      if (oStateAction.sState.equals (sState)) {
        oPresentStateAction = oStateAction;
        bFound = true;
        break;
      }
    }
                                                                       // If we didn't find it, then we add it
    if (!bFound) {
      oPresentStateAction = new StateAction (sState, iNActions);
      oVStateActions.add (oPresentStateAction);
    }
    ///
    dQmax = 0;
    for (int i=0; i<iNActions; i++) {     // Determining the action to get Qmax{a'}
      if (oPresentStateAction.dValAction[i] > dQmax) {
        iBest = i;
        iNumBest = 1;       // Reseting the number of best actions
        dQmax = oPresentStateAction.dValAction[i];
      }
      else if ( (oPresentStateAction.dValAction[i] == dQmax) && (dQmax > 0) ) { // If there is another one equal we must select one of them randomly
        iNumBest++;
        if (Math.random() < 1.0 / (double) iNumBest) {    // Choose randomly with reducing probabilities
          iBest = i;
       dQmax = oPresentStateAction.dValAction[i]; 
        }
      }
    }
                        // Adjusting Q(s,a)
    if (oLastStateAction != null)
      oLastStateAction.dValAction[iAction] +=  dLearnRate * (dReward + dGamma * dQmax - oLastStateAction.dValAction[iAction]); 
  
    if ( (iBest > -1) && (Math.random() > dEpsilon) )    // Using the e-greedy policy to select the best action or any of the rest
      iNewAction = iBest;
    else do {
      iNewAction = (int) (Math.random() * (double) iNumActions);
    } while (iNewAction == iBest);
   
    oLastStateAction = oPresentStateAction;    // Updating values for the next time
    dLearnRate *= dDecFactorLR;      // Reducing the learning rate
    if (dLearnRate < dMINLearnRate) dLearnRate = dMINLearnRate;
  
    return iNewAction;
}
  
  
  } 
      
      
        
      
        
  
      
    /**
  * This is the basic class to store Q values (or probabilities) and actions for a certain state
  *
  * @author  Juan C. Burguillo Rial
  * @version 2.0
  */
  public class StateAction implements Serializable {
    String sState;
    double[] dValAction;

    StateAction(String sAuxState, int iNActions) {
        sState = sAuxState;
        dValAction = new double[iNActions];
    }

    StateAction(String sAuxState, int iNActions, boolean bLA) {
        this(sAuxState, iNActions);
        if (bLA)
            for (int i = 0; i < iNActions; i++) // This constructor is used for LA and sets up initial probabilities
                dValAction[i] = 1.0 / iNActions;
    }

    public String sGetState() {
        return sState;
    }

    public double dGetQAction(int i) {
        return dValAction[i];
    }
}


   
    
    
}

