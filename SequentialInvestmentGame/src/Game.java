import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;


public class Game implements Callable<float[][]> {


    // region simulation and helpers

    private float[] playRound() {

        float[] res = new float[mPlayers.size()];

        for(int i = 0; i< mPlayers.size(); ++i) {
            res[i] = mPlayers.get(i).playRound(this);
            mPlayers.get(i).resetMoney();
        }

        return res;
    }

    public float[][] simulate() {

        float[][] res = new float[mNumRounds][mPlayers.size()];

        for(int i = 0; i< mNumRounds; ++i) {
            res[i] = playRound();
        }

        return res;
    }

    private boolean rollDice() {
        if(mRandom.nextDouble() <= mP)
            return true;
        return false;
    }

    // endregion


    // region member fields

    private List<Player> mPlayers;
    private int mNumStages;
    private int mNumRounds;
    private float mP;
    private float mM;
    private Random mRandom;

    // endregion


    // region getter/setters

    public int getmNumStages() {
        return mNumStages;
    }

    public float getP() { return mP; }

    public int getNumPlayers() { return mPlayers.size(); }

    public void addPlayer(Strategy strategy) {
        mPlayers.add(new Player(strategy, mM));
    }

    public void removeAllPlayers() {
        mPlayers.clear();
    }

    // endregion


    // region constructors

    public Game(int numStages, int numRounds, float p, float M) {
        mNumStages = numStages;
        mNumRounds = numRounds;
        mP = p;
        mM = M;
        mPlayers = new ArrayList<>();
        mRandom = new Random();
    }

    public Game(GameCfg cfg) {
        this(cfg.numStages, cfg.numRounds, cfg.p, cfg.M);
    }

    @Override
    public float[][] call() throws Exception {
        return simulate();
    }

    // endregion


    public static class GameCfg {

        public int numStages;
        public int numRounds;
        public float p;
        public float M;

    }

    private class Player {


        // region external functionalities

        public float playRound(Game game) {
            for(int i = 0; i<game.getmNumStages(); ++i) {
                playStage(game);
            }

            return mMoney;
        }

        public void resetMoney() {
            mMoney = mStartMoney;
            currentStage = 1;
        }

        // endregion


        // region helpers

        private void playStage(Game game) {

            float stake = mMoney * mStrategy.eval(game.getP(), game.getmNumStages(), currentStage, game.getNumPlayers(), mMoney);
            mMoney -= stake;

            if(game.rollDice()) {
                mMoney += 2 * stake;
            }

            currentStage++;

        }

        // endregion


        // region member fields

        private Strategy mStrategy;
        private float mMoney;
        private float mStartMoney;
        private int currentStage;

        // endregion


        // region getters/setters

        // endregion


        // region constructors

        public Player(Strategy strategy, float money) {
            this.mStrategy = strategy;
            this.mMoney = money;
            this.mStartMoney = money;
            currentStage = 1;
        }

        // endregion


    }

}


