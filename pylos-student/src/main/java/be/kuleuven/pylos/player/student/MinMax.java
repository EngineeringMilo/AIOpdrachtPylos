package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

public class MinMax extends PylosPlayer {

    private final int max_depth= 4;

    //controleert hoeveel verschil in ballen dr zijn
    private int evaluateBoard(PylosBoard board, PylosGameSimulator simulator) {
        if (simulator.getState() == PylosGameState.COMPLETED) {
            return (simulator.getWinner() == this.PLAYER_COLOR) ? 10000 : -10000;
        }
        int myReserves = board.getReservesSize(this.PLAYER_COLOR);
        int opponentReserves = board.getReservesSize(this.PLAYER_COLOR.other());
        return myReserves - opponentReserves;

    }
    private int minimax(PylosGameSimulator simulator, PylosBoard board, int depth, boolean maximizingPlayer) {

        if (depth==0 || simulator.getState() == PylosGameState.COMPLETED) {
            return evaluateBoard(board, simulator);
        }
        //huidige state en color opslaan
        PylosPlayerColor currentColor = simulator.getColor();
        PylosGameState currentState = simulator.getState();

        if (maximizingPlayer) {
            // maxEval = -infinity
            int maxEval = Integer.MIN_VALUE;

            // --- for each child of position ---

            if (currentState == PylosGameState.MOVE) {
                for (PylosSphere sphere : board.getSpheres(currentColor)) {
                    if (!sphere.isReserve()) {
                        for (PylosLocation location : board.getLocations()) {
                            if (sphere.canMoveTo(location)) {
                                PylosLocation prevLocation = sphere.getLocation();

                                simulator.moveSphere(sphere, location);
                                // eval = minimax(child, depth - 1, false) -> dynamisch via simulator.getColor()
                                int eval = minimax(simulator, board, depth - 1, simulator.getColor() == this.PLAYER_COLOR);
                                // maxEval = max(maxEval, eval)
                                maxEval = Math.max(maxEval, eval);

                                simulator.undoMoveSphere(sphere, prevLocation, currentState, currentColor);
                            }
                        }
                    }
                }
                PylosSphere reserveSphere = board.getReserve(currentColor);
                if (reserveSphere != null) {
                    for (PylosLocation location : board.getLocations()) {
                        if (location.isUsable()) {
                            simulator.moveSphere(reserveSphere, location);

                            int eval = minimax(simulator, board, depth - 1, simulator.getColor() == this.PLAYER_COLOR);
                            maxEval = Math.max(maxEval, eval);

                            simulator.undoAddSphere(reserveSphere, currentState, currentColor);
                        }
                    }
                }
            }
            else if (currentState == PylosGameState.REMOVE_FIRST) {
                for (PylosSphere sphere : board.getSpheres(currentColor)) {
                    if (sphere.canRemove()) {
                        PylosLocation prevLocation = sphere.getLocation();

                        simulator.removeSphere(sphere);

                        int eval = minimax(simulator, board, depth - 1, simulator.getColor() == this.PLAYER_COLOR);
                        maxEval = Math.max(maxEval, eval);

                        simulator.undoRemoveFirstSphere(sphere, prevLocation, currentState, currentColor);
                    }
                }
            }
            else if (currentState == PylosGameState.REMOVE_SECOND) {
                for (PylosSphere sphere : board.getSpheres(currentColor)) {
                    if (sphere.canRemove()) {
                        PylosLocation prevLocation = sphere.getLocation();

                        simulator.removeSphere(sphere);

                        int eval = minimax(simulator, board, depth - 1, simulator.getColor() == this.PLAYER_COLOR);
                        maxEval = Math.max(maxEval, eval);

                        simulator.undoRemoveSecondSphere(sphere, prevLocation, currentState, currentColor);
                    }
                }
                simulator.pass();
                int eval = minimax(simulator, board, depth - 1, simulator.getColor() == this.PLAYER_COLOR);
                maxEval = Math.max(maxEval, eval);
                simulator.undoPass(currentState, currentColor);
            }

            // return maxEval
            return maxEval;

            // else
        }
    }

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), PLAYER_COLOR, board);

    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {

    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {

    }
}
