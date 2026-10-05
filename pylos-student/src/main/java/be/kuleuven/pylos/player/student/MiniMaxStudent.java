    package be.kuleuven.pylos.player.student;

    import be.kuleuven.pylos.game.*;
    import be.kuleuven.pylos.player.PylosPlayer;
    import java.util.ArrayList;

    public class MiniMaxStudent extends PylosPlayer {

        ArrayList<PylosSphere> allBalls = new ArrayList<>();

        @Override
        public void doMove(PylosGameIF game, PylosBoard board) {
            /* board methods
             * 	PylosLocation[] allLocations = board.getLocations();
             * 	PylosSphere[] allSpheres = board.getSpheres();
             * 	PylosSphere[] mySpheres = board.getSpheres(this);
             * 	PylosSphere myReserveSphere = board.getReserve(this); */

            /* game methods
             * game.moveSphere(myReserveSphere, allLocations[0]); */

            PylosGameSimulator gameSimulator = new PylosGameSimulator(game.getState(), PLAYER_COLOR, board);
            int bestValue = Integer.MIN_VALUE;
            PylosLocation bestLocation = null;
            PylosSphere lastBall = board.getReserve(this);
            for (PylosLocation loc: board.getLocations()){
                if(loc.isUsable()){
                    int value = minimax(3, loc, true, gameSimulator, board);
                    if(value > bestValue){
                        bestValue = value;
                        bestLocation = loc;
                    }
                }
            }
            lastBall = board.getReserve(this);
            allBalls.add(lastBall);
            game.moveSphere(lastBall, bestLocation);
        }

        private int minimax(int depth,PylosLocation position,boolean maximizingplayer, PylosGameSimulator game, PylosBoard board){
            if (depth == 0 || game.getWinner() == PLAYER_COLOR) return eval(board);
            if(maximizingplayer){
                int maxVal = Integer.MIN_VALUE;
                for (PylosLocation loc: board.getLocations()) {
                    int val = minimax(depth - 1, loc, false , game, board);
                    maxVal = Math.max(maxVal, val);
                }
                return maxVal;
            }
            else {
                int minVal = Integer.MAX_VALUE;
                for (PylosLocation loc: board.getLocations()) {
                    int val = minimax(depth - 1, loc, true , game, board);
                    minVal = Math.min(minVal, val);
                }
                return minVal;
            }
        }

        private int eval(PylosBoard board) {
            return board.getReservesSize(PLAYER_COLOR);
        }

        @Override
        public void doRemove(PylosGameIF game, PylosBoard board) {
            ArrayList<PylosSphere> removableSpeheres = new ArrayList<>();
            for (PylosSphere ps: board.getSpheres(PLAYER_COLOR)){
                if(ps.canRemove()) removableSpeheres.add(ps);
            }
            game.removeSphere(removableSpeheres.get(getRandom().nextInt(removableSpeheres.size())));
        }

        @Override
        public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
            ArrayList<PylosSphere> removableSpeheres = new ArrayList<>();
            for (PylosSphere ps: board.getSpheres(PLAYER_COLOR)){
                if(ps.canRemove()) removableSpeheres.add(ps);
            }
            if(removableSpeheres.isEmpty()) {
                game.pass();
            }else{
               game.removeSphere(removableSpeheres.get(getRandom().nextInt(removableSpeheres.size())));
            }
        }
    }
