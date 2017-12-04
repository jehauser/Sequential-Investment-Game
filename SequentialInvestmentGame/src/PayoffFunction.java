import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.Math.floor;
import static java.util.Arrays.sort;

public class PayoffFunction {


    PayoffFunction(int numPlayers, List<Strategy> strategySet, Game.GameCfg cfg, float epsilon) {
        this.numPlayers = numPlayers;
        this.strategySet = strategySet;
        this.cfg = cfg;
        executorService = Executors.newFixedThreadPool(16);
        payoffs = new ArrayList<>();
        this.epsilon = epsilon;
    }

    public void compute() throws ExecutionException, InterruptedException {

        Game game = new Game(cfg);
        List<Future<float[][]>> futureList = new ArrayList<>();

        int []strategyProfile = new int[numPlayers];
        for(int i = 0; i < strategyProfile.length; ++i) {
            strategyProfile[i] = 0;
        }

        do {
            // setup game
            game = new Game(cfg);
            for(int s : strategyProfile) {
                game.addPlayer(strategySet.get(s));
            }

            // simulate
            futureList.add(executorService.submit(game));


        } while(nextStrategyProfile(strategyProfile, strategySet.size()));



        for(int i=0; i<futureList.size(); ++i) {
            payoffs.add(binaryTable2WinPercentage(simResult2BinaryTable(futureList.get(i).get())));
            System.out.printf("%f %s done\n", (float)i / (futureList.size()-1) * 100, "%");
        }

        System.out.printf("payoff function computation finished\n");

        executorService.shutdown();

    }

    private boolean nextStrategyProfile(int[] strategyProfile, int numStrategies) {


        int i = strategyProfile.length-1;
        int resetDigit;
        while(true) {

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

    private int strategyProfile2Index(int[] strategyProfile, int numStrategies) throws Exception {


        Integer[] sortedStrategyProfileCopy = new Integer[strategyProfile.length];
        for(int i=0; i<strategyProfile.length; ++i) {
            sortedStrategyProfileCopy[i] = strategyProfile[i];
        }
        Arrays.sort(sortedStrategyProfileCopy);




        int[] tmpStrategyProfile = new int[strategyProfile.length];
        for(int i=0; i<tmpStrategyProfile.length; ++i) {
            tmpStrategyProfile[i] = 0;
        }
        int res = 0;
        while(true) {

            boolean areEqual = true;
            for(int i=0; i<sortedStrategyProfileCopy.length; ++i) {
                if(sortedStrategyProfileCopy[i] != tmpStrategyProfile[i]) {
                    areEqual = false;
                    break;
                }
            }

            if(areEqual) {
                return res;
            }

            res++;

            if(!nextStrategyProfile(tmpStrategyProfile, numStrategies)) {
                throw new Exception("FAIL");
            }




        }

    }

    public float getPayoff(int[] strategyProfile, int playerIndex) {


        int copyIndex = 0;
        try {
            copyIndex = strategyProfile2Index(strategyProfile, strategySet.size());
        } catch (Exception e) {
            e.printStackTrace();
        }


        int sortedPlayerIndex = 0;
        int playerStrategyNumber = strategyProfile[playerIndex];
        for(int i=0; i<strategyProfile.length; ++i) {
            if(strategyProfile[i] > playerStrategyNumber) {
                sortedPlayerIndex++;
            }
        }


        return payoffs.get(copyIndex)[sortedPlayerIndex];



    }



    public boolean isNashEquilibrium(int[] strategyProfile) {

        int[] alternativeStrategyProfile = new int[strategyProfile.length];
        for(int j=0; j<strategyProfile.length; ++j) {
            alternativeStrategyProfile[j] = strategyProfile[j];
        }

        for(int i=0; i<strategyProfile.length; ++i) {



            float currentPayoff = getPayoff(strategyProfile, i);

            for(int j=0; j<strategySet.size(); ++j) {

                if(j == strategyProfile[i])
                    continue;

                alternativeStrategyProfile[i] = j;

                if(getPayoff(alternativeStrategyProfile, i) - currentPayoff > epsilon) {
                    return false;
                }

            }

            alternativeStrategyProfile[i] = strategyProfile[i];

        }


        return true;

    }

    public void findNashEquilibria() {



        int []strategyProfile = new int[numPlayers];
        for(int i = 0; i < strategyProfile.length; ++i) {
            strategyProfile[i] = 0;
        }

        do {

            if(isNashEquilibrium(strategyProfile)) {

                System.out.print("[");
                for (int j = 0; j< strategyProfile.length-1; ++j) {
                    System.out.printf("%s, ", strategySet.get(strategyProfile[j]).toText());
                }
                System.out.printf("%s]", strategySet.get(strategyProfile[strategyProfile.length - 1]).toText());


                System.out.printf("\t is a nash equilibrium\n");
            }


        } while(nextStrategyProfile(strategyProfile, strategySet.size()));

        System.out.printf("finished checking for nash\n");




    }




    private int numPlayers;
    private List<Strategy> strategySet;
    private List<float[]> payoffs;
    Game.GameCfg cfg;
    private ExecutorService executorService;
    float epsilon;











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


}
