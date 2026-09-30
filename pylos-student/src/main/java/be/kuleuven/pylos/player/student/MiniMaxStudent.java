package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosGameIF;
import be.kuleuven.pylos.game.PylosLocation;
import be.kuleuven.pylos.game.PylosSphere;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;

public class MiniMaxStudent extends PylosPlayer {

    private PylosSphere LastBall;
    ArrayList<PylosSphere> allBalls;
    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        /* board methods
         * 	PylosLocation[] allLocations = board.getLocations();
         * 	PylosSphere[] allSpheres = board.getSpheres();
         * 	PylosSphere[] mySpheres = board.getSpheres(this);
         * 	PylosSphere myReserveSphere = board.getReserve(this); */

        /* game methods
         * game.moveSphere(myReserveSphere, allLocations[0]); */
        ArrayList<PylosLocation> allPossibleLocations = new ArrayList<>();
        for (PylosLocation loc : board.getLocations()) {
            if (loc.isUsable()) {
                allPossibleLocations.add(loc);
            }
        }
        LastBall = board.getReserve(this);
        PylosLocation location = allPossibleLocations.size() == 1 ? allPossibleLocations.get(0) : allPossibleLocations.get(getRandom().nextInt(allPossibleLocations.size() - 1));
        game.moveSphere(LastBall, location);
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        game.removeSphere(LastBall);
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        game.pass();
    }
}
