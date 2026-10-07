package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Jan on 20/02/2015.
 */
public class MCTS extends PylosPlayer {

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        /* board methods
         * 	PylosLocation[] allLocations = board.getLocations();
         * 	PylosSphere[] allSpheres = board.getSpheres();
         * 	PylosSphere[] mySpheres = board.getSpheres(this);
         * 	PylosSphere myReserveSphere = board.getReserve(this); */

        /* game methods
         * game.moveSphere(myReserveSphere, allLocations[0]); */
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        /* game methods
         * game.removeSphere(mySphere); */
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        /* game methods
         * game.removeSphere(mySphere);
         * game.pass() */
    }
}

class MCTSNode {
    Random r = new Random();
    int nACtions = 5;
    double epsilon = 1e-6;

    MCTSNode[] childeren;

    double nVisits;
    double TotValue;
    public void selectAction(){
        List<MCTSNode> visited = new LinkedList<MCTSNode>();
        MCTSNode curnode = this;
        visited.add(curnode);

        curnode.expand();
        MCTSNode newNode = curnode.select();
        visited.add(newNode);
        double value = rollOut(newNode);
        for(MCTSNode n: visited){
            n.updateStats(value);
        }
    }

    public void expand(){
        childeren = new MCTSNode[nACtions];
        for(int i = 0; i < nACtions; i++){
            childeren[i] = new MCTSNode();
        }
    }

    public MCTSNode select(){
        MCTSNode selected = null;
        double bestVal = Double.MIN_VALUE;
        for(MCTSNode c: childeren){
            // small random number to break ties randomly in unexpanded nodes
            double uctValue = c.TotValue / (c.nVisits + epsilon) + Math.sqrt(Math.log(nVisits+1) / (c.nVisits + epsilon)) + r.nextDouble() * epsilon;
            if(uctValue > bestVal){
                selected = c;
                bestVal = uctValue;
            }
        }
        return selected;
    }

    public boolean isLeaf(){
        return childeren == null;
    }

    public double rollOut(MCTSNode node){
        return r.nextInt(2);
    }

    public void updateStats(double value){
        nVisits++;
        TotValue += value;
    }
    public int arity(){
        return childeren == null ? 0 : childeren.length;
    }
}