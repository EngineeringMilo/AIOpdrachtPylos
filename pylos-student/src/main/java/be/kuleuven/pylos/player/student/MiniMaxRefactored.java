package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.List;

public class MiniMaxRefactored extends PylosPlayer {

    private static final int WIN = 1_000_000;

    private enum Kind { ADD, MOVE, REMOVE, PASS }

    private static class Move {
        final Kind kind;
        final PylosSphere sphere;
        final PylosLocation to;
        // undo-info
        PylosGameState prevState;
        PylosPlayerColor prevColor;
        PylosLocation prevLocation;

        Move(Kind kind, PylosSphere sphere, PylosLocation to) {
            this.kind = kind;
            this.sphere = sphere;
            this.to = to;
        }
    }

    private final int diepte;

    public MiniMaxRefactored(int diepte) {
        this.diepte = diepte;
    }

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        execute(game, choose(game, board));
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        execute(game, choose(game, board));
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        execute(game, choose(game, board));
    }

    private Move choose(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), PLAYER_COLOR, board);

        Move best = null;
        int bestValue = Integer.MIN_VALUE;

        for (Move m : generateMoves(sim, board)) {
            apply(m, sim);
            int value = minimax(childDepth(m, diepte), sim, board, bestValue, Integer.MAX_VALUE);
            undo(m, sim);

            if (best == null || value > bestValue) {
                bestValue = value;
                best = m;
            }
        }
        return best;
    }

    private void execute(PylosGameIF game, Move m) {
        switch (m.kind) {
            case ADD:
            case MOVE:
                game.moveSphere(m.sphere, m.to);
                break;
            case REMOVE:
                game.removeSphere(m.sphere);
                break;
            case PASS:
                game.pass();
                break;
        }
    }

    private int minimax(int depth, PylosGameSimulator sim, PylosBoard board, int alpha, int beta) {
        if (sim.getState() == PylosGameState.COMPLETED) {
            boolean iWon = sim.getColor() != PLAYER_COLOR;
            return iWon ? WIN + depth : -WIN - depth;
        }
        if (depth <= 0 && sim.getState() == PylosGameState.MOVE) return eval(board);

        List<Move> moves = generateMoves(sim, board);
        if (moves.isEmpty()) return eval(board);

        boolean max = sim.getColor() == PLAYER_COLOR;
        int best = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Move m : moves) {
            apply(m, sim);
            int v = minimax(childDepth(m, depth), sim, board, alpha, beta);
            undo(m, sim);

            if (max) {
                best = Math.max(best, v);
                alpha = Math.max(alpha, v);
            } else {
                best = Math.min(best, v);
                beta = Math.min(beta, v);
            }
            if (beta <= alpha) break;
        }
        return best;
    }

    // diepte enkel verlagen bij een zet vanuit MOVE
    private int childDepth(Move m, int depth) {
        return m.prevState == PylosGameState.MOVE ? depth - 1 : depth;
    }


    private List<Move> generateMoves(PylosGameSimulator sim, PylosBoard board) {
        List<Move> moves = new ArrayList<>();
        PylosPlayerColor color = sim.getColor();
        PylosGameState state = sim.getState();

        if (state == PylosGameState.MOVE) {
            PylosSphere reserve = board.getReserve(color);
            for (PylosLocation loc : board.getLocations()) {
                if (!loc.isUsable()) continue;
                if (reserve != null) moves.add(new Move(Kind.ADD, reserve, loc));
                for (PylosSphere s : board.getSpheres(color)) {
                    if (!s.isReserve() && s.canMoveTo(loc)) {
                        moves.add(new Move(Kind.MOVE, s, loc));
                    }
                }
            }
        } else {
            if (state == PylosGameState.REMOVE_SECOND) {
                moves.add(new Move(Kind.PASS, null, null)); // tweede remove is optioneel
            }
            for (PylosSphere s : board.getSpheres(color)) {
                if (s.canRemove()) moves.add(new Move(Kind.REMOVE, s, null));
            }
        }
        return moves;
    }

    private void apply(Move m, PylosGameSimulator sim) {
        m.prevState = sim.getState();
        m.prevColor = sim.getColor();
        switch (m.kind) {
            case ADD:
                sim.moveSphere(m.sphere, m.to);
                break;
            case MOVE:
                m.prevLocation = m.sphere.getLocation();
                sim.moveSphere(m.sphere, m.to);
                break;
            case REMOVE:
                m.prevLocation = m.sphere.getLocation();
                sim.removeSphere(m.sphere);
                break;
            case PASS:
                sim.pass();
                break;
        }
    }

    private void undo(Move m, PylosGameSimulator sim) {
        switch (m.kind) {
            case ADD:
                sim.undoAddSphere(m.sphere, m.prevState, m.prevColor);
                break;
            case MOVE:
                sim.undoMoveSphere(m.sphere, m.prevLocation, m.prevState, m.prevColor);
                break;
            case REMOVE:
                if (m.prevState == PylosGameState.REMOVE_FIRST) {
                    sim.undoRemoveFirstSphere(m.sphere, m.prevLocation, m.prevState, m.prevColor);
                } else {
                    sim.undoRemoveSecondSphere(m.sphere, m.prevLocation, m.prevState, m.prevColor);
                }
                break;
            case PASS:
                sim.undoPass(m.prevState, m.prevColor);
                break;
        }
    }

    private int eval(PylosBoard board) {

        PylosPlayerColor me = PLAYER_COLOR;
        PylosPlayerColor opp = PLAYER_COLOR.other();

        int score = 0;


        //mss weights met statics make dak da zo kan verandere vanbove in de functie ipv hier zo met weights te gaan spele
        //reservespheres
        score += (board.getReservesSize(me)-board.getReservesSize(opp))*50;

        //vierkanten bekijke, hoeveel hebk er van mij hoeveel van enemy zo score reducten of bijdoen
        for (PylosSquare square : board.getAllSquares()) {
            int myCount = square.getInSquare(me);
            int oppCount = square.getInSquare(opp);

            //gemengd, doek niks mee, late staan?
            if (myCount > 0 && oppCount > 0) {
                continue;
            }

            //mijn spheres in vierkant
            if (myCount == 3) {
                score += 30;
            } else if (myCount == 2) {
                score += 5;
            }

            //sphere vn opps in vierkant
            if (oppCount == 3) {
                score -= 30;
            } else if (oppCount == 2) {
                score -= 5;
            }
        }

        //spheres dat niet in de reserve liggen krijgen ook nog punte!
        for (PylosSphere sphere : board.getSpheres(PLAYER_COLOR)){
            if(!sphere.isReserve()) {
                PylosLocation locatie = sphere.getLocation();
                score+=(locatie.Z * 5);

                //mss ook nog de bollen die vrijgespeeld kunne worde ofz als ze me 3 zijn?????? idfk
            }
        }

        for (PylosSphere sphere : board.getSpheres(PLAYER_COLOR.other())) {
            if (!sphere.isReserve()) {
                PylosLocation loc = sphere.getLocation();
                score -= (loc.Z * 5);
            }
        }

        return score;
    }
}
