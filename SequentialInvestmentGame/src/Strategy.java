public interface Strategy {

    float eval(float winProbability, int numStages, int currentStage, int numPlayers, float currentMoney);

    String toText();

}
