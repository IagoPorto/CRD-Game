import java.util.Vector;

/**
  * This is a basic class with some learning tools: statistical learning, learning automata (LA) and Q-Learning (QL)
  *
  * @author  Juan C. Burguillo Rial
  * @version 2.0
  */
class LearningTools
{
final double dDecFactorLR = 0.99;   // Value that will decrement the learning rate in each generation
final double dEpsilon = 0.95;    // Used to avoid selecting always the best action
final double dMINLearnRate = 0.05;   // We keep learning, after convergence, during 5% of times

boolean bAllActions = false;    // At the beginning we did not try all actions
int iNewAction2Play;     // This is the new action to be played
int iNumActions = 2;      // For C or D for instance
int iLastAction;     // The last action that has been played by this player
int[] iNumTimesAction = new int [iNumActions];  // Number of times an action has been played
double[] dPayoffAction = new double [iNumActions]; // Accumulated payoff obtained by the different actions
StateAction oPresentStateAction;   // Contains the present state we are and the actions that are available
Vector oVStateActions;     // A vector containing strings with the possible States and Actions available at each one


/**
 * This method uses Learning Automata (LA) to select a new action depending on the
 * past experiences. The algorithm works as: store, adjust and generate a new action.
 * @param sState contains the present state
 * @param iNActions contains the number of actions that can be applied in this state
 * @param dReward is the reward obtained after performing the last action.
 */
public void vGetNewActionAutomata (String sState, int iNActions, double dReward) {
  boolean bFound;
  StateAction oStateProbs, oLastStateAction;

  bFound = false;       // Searching if we already have the state
  for (int i=0; i<oVStateActions.size(); i++) {
    oStateProbs = (StateAction) oVStateActions.elementAt(i);
    if (oStateProbs.sState.equals (sState)) {
      oPresentStateAction = oStateProbs;
      bFound = true;
      break;
    }
  }
                                                                     // If we didn't find it, then we add it
  if (!bFound) {
    oPresentStateAction = new StateAction (sState, iNActions, true);
    oVStateActions.add (oPresentStateAction);
  }

  if (oLastStateAction != null) {                  // Adjusting Probabilities
    if (dReward > 0)                    // If reward grows and the previous action was allowed --> reinforce last action
      for (int i=0; i<iNActions; i++)
        if (i == iLastAction)
          oLastStateAction.dValAction[i] += dLearnRate * (1.0 - oLastStateAction.dValAction[i]); // Reinforce the last action
        else
          oLastStateAction.dValAction[i] *= (1.0 - dLearnRate);  // The rest are weakened
  }
  
  double dValAcc = 0;       // Generating the new action based on probabilities
  double dValRandom = Math.random();
  for (int i=0; i<iNActions; i++) {
    dValAcc += oPresentStateAction.dValAction[i];
    if (dValRandom < dValAcc) {
      iNewAction = i;
      break;
    }
  }

  oLastStateAction = oPresentStateAction;   // Updating values for the next time
  dLearnRate *= dDecFactorLR;     // Reducing the learning rate
  if (dLearnRate < dMINLearnRate) dLearnRate = dMINLearnRate;
}






}  // from class LearningTools





/**
  * This is the basic class to store Q values (or probabilities) and actions for a certain state
  *
  * @author  Juan C. Burguillo Rial
  * @version 2.0
  */
public class StateAction implements Serializable
{
String sState;
double[] dValAction;

StateAction (String sAuxState, int iNActions) {
  sState = sAuxState;
  dValAction = new double[iNActions];
  }

StateAction (String sAuxState, int iNActions, boolean bLA) {
  this (sAuxState, iNActions);
  if (bLA) for (int i=0; i<iNActions; i++) // This constructor is used for LA and sets up initial probabilities
    dValAction[i] = 1.0 / iNActions;
  }


public String sGetState() {
  return sState;
}

public double dGetQAction (int i) {
  return dValAction[i];
}
}
