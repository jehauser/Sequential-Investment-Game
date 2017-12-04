public class TimeLinearStrategy implements Strategy {

    TimeLinearStrategy(float a, float b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public float eval(float winProbability, int numStages, int currentStage, int numPlayers, float currentMoney) {

        float x = (float)currentStage / numStages;

        return (float) Math.min(Math.max(a*x+b, 0.0), 1.0);

    }

    @Override
    public String toText() {
        String res = Float.toString(a);
        res += "*x+";
        res += Float.toString(b);
        return res;

    }

    private float a, b;
}
