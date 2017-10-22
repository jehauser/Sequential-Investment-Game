**Sequential Investment Game**

In this project we will investigate different strategies for the sequential investment game. The project will consist of two parts. We will first build a simulation for the game. In the second step we will optimize a players’ strategy. We will now further discuss these two parts.

**Simulation**

We will first try to write the simulation in MATLAB. The game lends itself very well to an object oriented approach. If MATLAB proves not to be well suited for OOP we will switch to a more natural language for such an approach (probably Java). The simulation will have the following parameters:

    N: the number of players
    K: the number of stages per round
    R: the number of rounds
    p: the probability that a player wins in a given stage
    s: the strategy for a given player
    M: the amount of money every player starts with

**Optimizing the strategy**

At first we will only consider constant strategies. That means a given player will invest a fixed percentage of their money in every stage. We will compare how different (constant) strategies perform against each other depending on the probability p, the number of players N and the number of stages per round K. The goal would be to find a nash equilibrium and/or an evolutionary stable strategy.

If time permits we will further explore adaptive strategies. That means the percentage of money a player invests in a given stage may depend on the player's current situation. A player knows the number of players N, the number of stages per round K, the number of round R, the probability p and the amount of money every player starts with M. A player, however, does not know how much money other players currently have during a round. This information is only revealed at the end of a round to determine the winner.

**Appendix**

Nash equilibrium:

A nash equilibrium is a strategy profile where no player can increase their payoff by changing their strategy assuming all other players do not change their current strategy.

Evolutionary stable strategy:

A strategy x is evolutionary stable if for all strategies y ≠ x at least one of the following conditions is true:

    f(x,y) > f(y,x)
    f(x,x) = f(y,x) and f(x,y) > f(y,y)

where f(x,y) is the payoff of strategy x in an environment where all others have strategy y.
