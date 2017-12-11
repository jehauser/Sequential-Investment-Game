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
import java.util.concurrent.*;

import static java.lang.Math.abs;
import static java.lang.Math.floor;

public class Simulation {

    // region evolutionary stability test

    private void evolutionaryStabilityTest(Document doc) throws FileNotFoundException, ExecutionException, InterruptedException {

        // region variable declarations
        Game.GameCfg cfg = new Game.GameCfg();
        Game game;
        float epsilon;
        List<Integer> numStages;
        List<Float> p;
        List<Float> strategyStartValues;
        List<Float> strategyEndValues;
        List<Strategy> strategies;
        List<Integer> populationSize;
        PrintStream writer = System.out;
        List<List<List<List<Future>>>> futureLists;
        boolean[][][][] res;
        int numCallables = 0;
        int outputMode;
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

        if(loadFloatRange(doc, "strategyStartValues") != null) {
            strategyStartValues = loadFloatRange(doc, "strategyStartValues");
        }
        else { throw new ExceptionInInitializerError("strategyStartValues not found in config"); }

        if(loadFloatRange(doc, "strategyEndValues") != null) {
            strategyEndValues = loadFloatRange(doc, "strategyEndValues");
        }
        else { throw new ExceptionInInitializerError("strategyEndValues not found in config"); }

        strategies = new ArrayList<>();
        for(int i=0; i<strategyStartValues.size(); ++i) {
            for(int j=0; j<1; j++){

                // TODO: FIX
                float slope = (strategyEndValues.get(j) - strategyEndValues.get(i));
                float offset = strategyStartValues.get(i);
                System.out.printf("slope = %f | offset = %f\n", slope, offset);
                strategies.add(new TimeLinearStrategy(slope, offset));
            }
        }

        if(loadString(doc, "outputFileName") != null) { writer = new PrintStream(loadString(doc,"outputFileName")); }

        if(loadIntRange(doc, "outputMode") != null) {
            outputMode = loadIntRange(doc, "outputMode").get(0);}
        else { throw new ExceptionInInitializerError("outputMode not found in config"); }

        res = new boolean[numStages.size()][p.size()][populationSize.size()][strategies.size()];
        futureLists = new ArrayList<>();

        // endregion

        // region loop over numStages
        int i1 = 0;
        for(int numStages_ : numStages) {
            cfg.numStages = numStages_;
            futureLists.add(new ArrayList<>());

            // region loop over winProbabilities
            int i2 = 0;
            for(float p_ : p) {
                cfg.p = p_;
                futureLists.get(i1).add(new ArrayList<>());

                // region loop over population size
                int i3 = 0;
                for (int populationSize_ : populationSize) {
                    // region evolutionary stability test
                    futureLists.get(i1).get(i2).add(new ArrayList<>());
                    System.out.printf("numStages = %d, p = %f, populationSize = %d\n", numStages_, p_, populationSize_);

                    // loop over all mStrategies to test if they are evolutionary stable
                    int i4 = 0;
                    for (int x = 0; x < strategies.size(); ++x) {

                        game = new Game(cfg);
                        //System.out.printf("testing evolutionary stability of strategy %d\n", x);
                        futureLists.get(i1).get(i2).get(i3).add(executorService.submit(new isEvolutionaryStableCallable(x, strategies, populationSize_, game, epsilon)));
                        numCallables++;

                        i4++;
                    }
                    // endregion
                    i3++;
                }

                // endregion
                i2++;
            }

            // endregion

            i1++;
        }

        // endregion
        executorService.shutdown();

        int numCallablesDone = 0;
        for(i1 = 0; i1 < futureLists.size(); ++i1) {
            for(int i2 = 0; i2 < futureLists.get(i1).size(); ++i2) {
                for(int i3 = 0; i3 < futureLists.get(i1).get(i2).size(); ++i3) {
                    for(int i4 = 0; i4 < futureLists.get(i1).get(i2).get(i3).size(); ++i4) {
                        res[i1][i2][i3][i4] = (Boolean)futureLists.get(i1).get(i2).get(i3).get(i4).get();
                        numCallablesDone++;
                        System.out.printf("%f%s done\n", ((float)numCallablesDone) / numCallables * 100, "%");
                    }
                }
            }
        }



        writeToFile(writer, res, outputMode, numStages, p, populationSize);

        writer.close();

    }

    private boolean isEvolutionaryStable(int xIndex, List<Strategy> strategies, int populationSize, Game game, float epsilon) throws ExecutionException, InterruptedException {

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
            float[] expectedPayoff = binaryTable2WinPercentage(simResult2BinaryTable(game.simulate()));

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
                    expectedPayoff = binaryTable2WinPercentage(simResult2BinaryTable(game.simulate()));

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

    private class isEvolutionaryStableCallable implements Callable<Boolean> {

        private int xIndex;
        private List<Strategy> strategies;
        private int populationSize;
        private Game game;
        private float epsilon;

        public isEvolutionaryStableCallable(int xIndex, List<Strategy> strategies, int populationSize, Game game, float epsilon) {
            this.xIndex = xIndex;
            this.strategies = strategies;
            this.populationSize = populationSize;
            this.game = game;
            this.epsilon = epsilon;
        }

        @Override
        public Boolean call() throws Exception {
            return isEvolutionaryStable(xIndex, strategies, populationSize, game, epsilon);
        }
    };


    private void writeToFile(PrintStream writer, boolean[][][][] res, int mode, List<Integer> numStages, List<Float> p, List<Integer> populationSize) {

        // numStages | winProbability | populationSize | strategy

        // variable populaiton size
        if(mode == 0) {

            for(int i=0; i<res.length; ++i) {
                for(int j=0; j<res[0].length; ++j) {
                    writer.printf("numberOfStages = %d, winProbability = %f\n", numStages.get(i), p.get(j));
                    for(int k=0; k<res[0][0].length; ++k) {
                        for(int s=0; s<res[0][0][0].length; ++s) {
                            if(res[i][j][k][s])
                                writer.printf("1 ");
                            else
                                writer.printf("0 ");
                        }
                        writer.println();
                    }
                    writer.println();
                }
                writer.println();
            }

        }

        // variable number of stages
        if(mode == 1) {

            for(int k=0; k<res[0][0].length; ++k) {
                for(int j=0; j<res[0].length; ++j) {
                    writer.printf("populationSize = %d, winProbability = %f\n", populationSize.get(k), p.get(j));
                    for(int i=0; i<res.length; ++i) {
                        for(int s=0; s<res[0][0][0].length; ++s) {
                            if(res[i][j][k][s])
                                writer.printf("1 ");
                            else
                                writer.printf("0 ");
                        }
                        writer.println();
                    }
                    writer.println();
                }
                writer.println();
            }

        }

        if(mode == 2) {

            for(int k=0; k<res[0][0].length; ++k) {
                for(int i=0; i<res.length; ++i) {
                    writer.printf("populationSize = %d, numberOfStages = %d\n", populationSize.get(k), numStages.get(i));
                    for(int j=0; j<res[0].length; ++j) {
                        for(int s=0; s<res[0][0][0].length; ++s) {
                            if(res[i][j][k][s])
                                writer.printf("1 ");
                            else
                                writer.printf("0 ");
                        }
                        writer.println();
                    }
                    writer.println();
                }
                writer.println();
            }

        }


    }

    // endregion

    // region payoff function

    public void generatePayoffFunction(Document doc) throws FileNotFoundException, ExecutionException, InterruptedException {

        // region variable declarations

        float epsilon;
        Game.GameCfg cfg = new Game.GameCfg();
        Game game;
        int populationSize;
        List<Float> strategyStartValues;
        List<Float> strategyEndValues;
        List<Strategy> strategySet;
        PrintStream writer = System.out;

        PayoffFunction payoffFunction;

        // endregion

        // region load parameters from config file

        if(loadFloatRange(doc, "epsilon") != null) {
            epsilon = loadFloatRange(doc, "epsilon").get(0);}
        else { throw new ExceptionInInitializerError("epsilon not found in config"); }

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

        if(loadFloatRange(doc, "strategyStartValues") != null) {
            strategyStartValues = loadFloatRange(doc, "strategyStartValues");
        }
        else { throw new ExceptionInInitializerError("strategyStartValues not found in config"); }

        if(loadFloatRange(doc, "strategyEndValues") != null) {
            strategyEndValues = loadFloatRange(doc, "strategyEndValues");
        }
        else { throw new ExceptionInInitializerError("strategyEndValues not found in config"); }

        strategySet = new ArrayList<>();
        for(int i=0; i<strategyStartValues.size(); ++i) {
            for(int j=0; j<strategyEndValues.size(); j++){

                float slope = (strategyEndValues.get(j) - strategyStartValues.get(i));
                float offset = strategyStartValues.get(i);
                System.out.printf("slope = %f | offset = %f\n", slope, offset);
                strategySet.add(new TimeLinearStrategy(slope, offset));
            }
        }



        if(loadString(doc, "outputFileName") != null) { writer = new PrintStream(loadString(doc,"outputFileName")); }

        // endregion

        payoffFunction = new PayoffFunction(populationSize, strategySet, cfg, epsilon);
        payoffFunction.compute();

        payoffFunction.findNashEquilibria();





        int[] strategyProfile = new int[populationSize];
        for(int i=0; i<populationSize; ++i) {
            strategyProfile[i] = 0;
        }

        boolean done = false;
        while(!done) {



            writer.print("[");
            for (int j = 0; j< populationSize - 1; ++j) {
                writer.printf("%s, ", strategySet.get(strategyProfile[j]).toText());
            }
            writer.printf("%s] \t\t\t -> \t [", strategySet.get(strategyProfile[strategyProfile.length - 1]).toText());


            for (int j = 0; j < populationSize-1; ++j) {
                writer.printf("%f, ", payoffFunction.getPayoff(strategyProfile, j));
            }
            writer.printf("%f]\n", payoffFunction.getPayoff(strategyProfile, populationSize-1));



            int i = populationSize - 1;
            while(true) {
                // check if current digit can be incremented
                if (strategyProfile[i] < strategySet.size() - 1) {

                    // increment current digit
                    strategyProfile[i]++;


                    // reset previous digits to 0
                    for (int j = i + 1; j < strategyProfile.length; ++j) {
                        strategyProfile[j] = 0;
                    }

                    break;

                }

                // check if i is at the most significant digit
                if (i == 0) {
                    done = true;
                    break;
                } else {
                    // move to next more significant digit
                    i--;
                }
            }

        }



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

            executorService = Executors.newFixedThreadPool(16);

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

    private ExecutorService executorService;

    public static void main(String [ ] args) {

        new Simulation("config.xml");

    }
}
