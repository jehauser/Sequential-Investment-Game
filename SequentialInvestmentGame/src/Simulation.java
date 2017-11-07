import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.abs;
import static java.lang.Math.floor;

public class Simulation {

    // region evolutionary stability test

    private void evolutionaryStabilityTest(Document doc) throws FileNotFoundException {

        // region variable declarations
        Game.GameCfg cfg = new Game.GameCfg();
        Game game;
        float epsilon;
        List<Integer> numStages;
        List<Float> p;
        List<Float> strategies;
        List<Integer> populationSize;
        PrintStream writer = System.out;

        // endregion


        // region load parameters from config file

        if(loadFloatRange(doc, "epsilon") != null) {
            epsilon = loadFloatRange(doc, "epsilon").get(0);}
        else { throw new ExceptionInInitializerError("epsilon not found in config"); }

        if(loadIntRange(doc, "numStages") != null) {
            numStages = loadIntRange(doc, "numStages");}
        else { throw new ExceptionInInitializerError("numStages not found in config"); }

        if(loadIntRange(doc, "numRounds") != null) {
            cfg.numRounds = loadIntRange(doc, "numRounds").get(0);}
        else { throw new ExceptionInInitializerError("numRounds not found in config"); }

        if(loadFloatRange(doc, "winProbability") != null) {
            p = loadFloatRange(doc, "winProbability");}
        else { throw new ExceptionInInitializerError("winProbability not found in config"); }

        if(loadFloatRange(doc, "startMoney") != null) {
            cfg.M = loadFloatRange(doc, "startMoney").get(0);}
        else { throw new ExceptionInInitializerError("startMoney not found in config"); }

        if(loadIntRange(doc, "populationSize") != null) {populationSize = loadIntRange(doc, "populationSize");}
        else { throw new ExceptionInInitializerError("populationSize not found in config"); }

        if(loadFloatRange(doc, "strategy") != null) {
            strategies = loadFloatRange(doc, "strategy");}
        else { throw new ExceptionInInitializerError("strategy not found in config"); }

        if(loadString(doc, "outputFileName") != null) { writer = new PrintStream(loadString(doc,"outputFileName")); }

        // endregion

        // region loop over numStages
        for(int numStages_ : numStages) {
            cfg.numStages = numStages_;

            // region loop over winProbabilities
            for(float p_ : p) {
                cfg.p = p_;

                // region loop over population size
                for(int populationSize_ : populationSize) {
                    // region evolutionary stability test

                    System.out.printf("numStages = %d, p = %f, populationSize = %d\n", numStages_, p_, populationSize_);

                    game = new Game(cfg);

                    // loop over all mStrategies to test if they are evolutionary stable
                    boolean[] isEvolutionaryStable = new boolean[strategies.size()];
                    for (int x = 0; x < strategies.size(); ++x) {
                        //System.out.printf("testing evolutionary stability of strategy %d\n", x);
                        isEvolutionaryStable[x] = isEvolutionaryStable(x, strategies, populationSize_, game, epsilon);
                    }

                    writer.printf("numStages = %d, p = %f, populationSize = %d\n", numStages_, p_, populationSize_);
                    for (int i = 0; i < strategies.size(); ++i) {
                        if (isEvolutionaryStable[i]) {
                            writer.printf("Strategy %d (%f) was evolutionary stable\n", i, strategies.get(i));
                        } else {
                            //writer.printf("Strategy %d (%f) was not evolutionary stable\n", i, strategies.get(i));
                        }
                    }
                    writer.println();
                    // endregion
                }

                // endregion

            }

            // endregion

        }
        // endregion

        writer.close();

    }

    private boolean isEvolutionaryStable(int xIndex, List<Float> strategies, int populationSize, Game game, float epsilon) {

        /* if N mPlayers play the same strategy each player has 1/N chance to win. so the expected payoff is 1/N.
            *  now test if any other strategy y performs better against x than x itself.*/

        boolean isEvolutionaryStable = true;

        // loop over all strategies y to test against.
        for(int y = 0; y < strategies.size(); ++y) {

            //System.out.printf("tesing %d vs %d\n", xIndex, y);

            // skip if x == y
            if (xIndex == y) {
                continue;
            }

            // setup simulation
            game.removeAllPlayers();
            game.addPlayer(strategies.get(y));
            for (int i = 1; i < populationSize; ++i) {
                game.addPlayer(strategies.get(xIndex));
            }

            // run simulation
            float[][] res = game.simulate();
            float[] expectedPayoff = binaryTable2WinPercentage(simResult2BinaryTable(res));

            // check if first condition is satisfied. if no, check second condition
            if (!(1.0f / populationSize > expectedPayoff[0])) {
                //System.out.printf("first condition failed for %d vs %d. expectedPayoff was %f\n", x, y, expectedPayoff[0]);

                // check if second condition is satisfied
                if (abs((1.0f / populationSize) - expectedPayoff[0]) <= epsilon) {

                    // check if x performs strictly better against y population than y.

                    // setup simulation
                    game.removeAllPlayers();
                    game.addPlayer(strategies.get(xIndex));
                    for (int i = 1; i < populationSize; ++i) {
                        game.addPlayer(strategies.get(y));
                    }

                    // run simulation
                    res = game.simulate();
                    expectedPayoff = binaryTable2WinPercentage(simResult2BinaryTable(res));

                    // check if second condition is satisfied. if yes, move on to check next y
                    if (!(expectedPayoff[0] > 1.0f / populationSize)) {
                        // second condition is also false. => x is not evolutionary stable
                        //System.out.printf("second (2) condition failed for %d vs %d\n", x, y);
                        isEvolutionaryStable = false;
                        break;
                    }
                } else {
                    // second condition is also false. => x is not evolutionary stable
                    //System.out.printf("second (1) condition failed for %d vs %d\n", x, y);
                    isEvolutionaryStable = false;
                    break;
                }


            }

        }

        return isEvolutionaryStable;
    }

    // endregion

    // region payoff function

    public void generatePayoffFunction(Document doc) throws FileNotFoundException {

        // region variable declarations

        Game.GameCfg cfg = new Game.GameCfg();
        Game game;
        int populationSize;
        List<Float> strategySet;
        int[] strategyProfile;
        PrintStream writer = System.out;

        // endregion

        // region load parameters from config file

        if(loadIntRange(doc, "numStages") != null) {
            cfg.numStages = loadIntRange(doc, "numStages").get(0);}
        else { throw new ExceptionInInitializerError("numStages not found in config"); }

        if(loadIntRange(doc, "numRounds") != null) {
            cfg.numRounds = loadIntRange(doc, "numRounds").get(0);}
        else { throw new ExceptionInInitializerError("numRounds not found in config"); }

        if(loadFloatRange(doc, "winProbability") != null) {
            cfg.p = loadFloatRange(doc, "winProbability").get(0);}
        else { throw new ExceptionInInitializerError("winProbability not found in config"); }

        if(loadFloatRange(doc, "startMoney") != null) {
            cfg.M = loadFloatRange(doc, "startMoney").get(0);}
        else { throw new ExceptionInInitializerError("startMoney not found in config"); }

        if(loadIntRange(doc, "populationSize") != null) {
            populationSize = loadIntRange(doc, "populationSize").get(0); }
        else { throw new ExceptionInInitializerError("populationSize not found in config"); }

        if(loadFloatRange(doc, "strategy") != null) {
            strategySet = loadFloatRange(doc, "strategy");}
        else { throw new ExceptionInInitializerError("strategy not found in config"); }

        if(loadString(doc, "outputFileName") != null) { writer = new PrintStream(loadString(doc,"outputFileName")); }

        // endregion

        // region loop over all strategy profiles

        game = new Game(cfg);

        strategyProfile = new int[populationSize];
        for(int i = 0; i < strategyProfile.length; ++i) {
            strategyProfile[i] = 0;
        }

        do {

            System.out.printf("computing payoff for: ");
            for(int i : strategyProfile)
                System.out.printf("%d | ", i);
            System.out.println();

            // setup game
            game.removeAllPlayers();
            for(int s : strategyProfile) {
                game.addPlayer(strategySet.get(s));
            }

            // simulate
            float[] expectedPayoff = binaryTable2WinPercentage(simResult2BinaryTable(game.simulate()));

            // region write to file
            writer.print("[");
            for(int i = 0; i < strategyProfile.length-1; ++i) {
                writer.printf("%f, ", strategySet.get(strategyProfile[i]));
            }
            writer.printf("%f] \t -> \t [", strategySet.get(strategyProfile[strategyProfile.length-1]));

            for(int i = 0; i < expectedPayoff.length-1; ++i) {
                writer.printf("%f, ", expectedPayoff[i]);
            }
            writer.printf("%f]\n", expectedPayoff[expectedPayoff.length-1]);
            // endregion


        } while(nextStrategyProfile(strategyProfile, strategySet.size()));

        // endregion



    }

    private boolean nextStrategyProfile(int[] strategyProfile, int numStrategies) {


        int i = strategyProfile.length-1;
        int resetDigit;
        while(true) {

            // check if current digit can be incremented
            if(strategyProfile[i] < numStrategies-1) {

                // increment current digit
                strategyProfile[i]++;

                // set resetDigit
                resetDigit = strategyProfile[i];

                // reset previous digits to resetDigit
                for(int j = i+1; j < strategyProfile.length; ++j) {
                    strategyProfile[j] = resetDigit;
                }

                // return true
                return true;

            }

            // check if i is at the most significant digit
            if(i == 0) {
                return false;
            }
            else {
                // move to next more significant digit
                i--;
            }

        }

    }

    // endregion

    // region result processing

    private int[][] simResult2BinaryTable(float[][] simRes) {

        // TODO: handle case where a row has no strict maximum!

        Random random = new Random();

        int[][] res = new int[simRes.length][simRes[0].length];
        List<Integer> currentRowMaxIndex = new ArrayList<>();

        // loop over rows of simRes
        for(int i = 0; i < simRes.length; ++i) {

            // loop over all elements of current row to find max
            currentRowMaxIndex.clear();
            currentRowMaxIndex.add(0);
            for(int j = 1; j < simRes[i].length; ++j) {
                if(simRes[i][j] > simRes[i][currentRowMaxIndex.get(0)]) {
                    currentRowMaxIndex.clear();
                    currentRowMaxIndex.add(j);
                }
                else if (simRes[i][j] == simRes[i][currentRowMaxIndex.get(0)]) {
                    currentRowMaxIndex.add(j);
                }
            }

            // loop over all elements of current row in result to set values
            int r = 0;
            if(currentRowMaxIndex.size() != 1) {
                float f = random.nextFloat();
                r = (int)floor(f * currentRowMaxIndex.size());
            }


            for(int j = 0; j < simRes[i].length; ++j) {
                if(j == currentRowMaxIndex.get(r)) {
                    res[i][j] = 1;
                }
                else {
                    res[i][j] = 0;
                }
            }
        }

        return res;

    }

    private float[] binaryTable2WinPercentage(int[][] binaryTable) {

        float[] res = new float[binaryTable[0].length];
        int numRows = binaryTable.length;

        // loop over all columns of binaryTable
        for(int j = 0; j < res.length; ++j) {

            // loop over all elements of current column and calculate average
            int sum = 0;
            for(int i = 0; i < numRows; ++i) {
                sum += binaryTable[i][j];
            }

            res[j] = ((float) sum) / numRows;
        }

        return res;
    }

    // endregion

    // region loaders

    private List<Integer> loadIntRange(Document doc, String s) {

        NodeList list = doc.getElementsByTagName(s);

        // check if element was found
        if(list.getLength() != 0) {

            String[] content = list.item(0).getTextContent().split(":");
            ArrayList<Integer> resList = new ArrayList<>();

            if(content.length == 1) {
                // it's a single value
                resList.add(Integer.parseInt(content[0]));
            }
            else {
                // it's a range of values
                int start = Integer.parseInt(content[0]);
                int step = Integer.parseInt(content[1]);
                int end = Integer.parseInt(content[2]);


                for(int x = start; x <= end; x += step) {
                    resList.add(x);
                }

            }
            return resList;
        }

        return null;
    }

    private List<Float> loadFloatRange(Document doc, String s) {

        NodeList list = doc.getElementsByTagName(s);

        // check if element was found
        if(list.getLength() != 0) {

            String[] content = list.item(0).getTextContent().split(":");
            ArrayList<Float> resList = new ArrayList<>();

            if(content.length == 1) {
                // it's a single value
                resList.add(Float.parseFloat(content[0]));
            }
            else {
                // it's a range of values
                float start = Float.parseFloat(content[0]);
                float step = Float.parseFloat(content[1]);
                float end = Float.parseFloat(content[2]);


                for(float x = start; x <= end; x += step) {
                    resList.add(x);
                }

            }
            return resList;
        }

        return null;
    }

    private String loadString(Document doc, String s) {

        NodeList list = doc.getElementsByTagName(s);

        if(list.getLength() != 0) {
            return list.item(0).getTextContent();
        }

        return null;
    }

    // endregion

    // region constructors

    public Simulation(String cfg) {

        try {
            File inputFile = new File(cfg);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            String mode = doc.getElementsByTagName("mode").item(0).getTextContent();

            if(mode.equals("evolutionaryStabilityTest")) {
                evolutionaryStabilityTest(doc);
            }
            else if(mode.equals("payoffFunction")) {
                generatePayoffFunction(doc);
            }
            else {
                throw new ExceptionInInitializerError("invalid mode in Game constructor");
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // endregion

    public static void main(String [ ] args) {

        new Simulation("config.xml");

    }
}
