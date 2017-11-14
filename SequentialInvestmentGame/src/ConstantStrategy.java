public class ConstantStrategy implements Strategy {

    public ConstantStrategy(float c) {
        mC = c;
    }

    @Override
    public float eval(float winProbability, int numStages, int currentStage, int numPlayers, float currentMoney) {
        return mC;
    }

    private float mC;
}
