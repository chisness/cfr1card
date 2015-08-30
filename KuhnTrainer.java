import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;

public class KuhnTrainer {

public static final int PASS = 0, BET = 1, NUM_ACTIONS = 2;
public int nodecount = 0;
public static final Random random = new Random();
public TreeMap<String, Node> nodeMap = new TreeMap<String, Node>();

class Node {
String infoSet;
double[] regretSum = new double[NUM_ACTIONS],
  strategy = new double[NUM_ACTIONS],
  strategySum = new double[NUM_ACTIONS];

private double[] getStrategy(double realizationWeight) {
  double normalizingSum = 0;
  for (int a = 0; a < NUM_ACTIONS; a++) {
    strategy[a] = regretSum[a] > 0 ? regretSum[a] : 0; //regret matching
    normalizingSum += strategy[a];
  }
  for (int a = 0; a < NUM_ACTIONS; a++) {
    if (normalizingSum > 0)
      strategy[a] /= normalizingSum;
    else
      strategy[a] = 1.0 / NUM_ACTIONS; //uniform weighting if negative regretsum
    strategySum[a] += realizationWeight * strategy[a];
  }
  return strategy;
}

public double[] getAverageStrategy() {
  double[] avgStrategy = new double[NUM_ACTIONS];
  double normalizingSum = 0;
  for (int a = 0; a < NUM_ACTIONS; a++)
    normalizingSum += strategySum[a];
  for (int a = 0; a < NUM_ACTIONS; a++)
    if (normalizingSum > 0)
    avgStrategy[a] = strategySum[a] / normalizingSum;
  else
    avgStrategy[a] = 1.0 / NUM_ACTIONS;
  return avgStrategy;
}

public String toString() {
  return String.format("%4s: %s", infoSet, Arrays.toString(getAverageStrategy()));
}
}

public void train(int iterations, int decksize) {
  int[] cards = new int[decksize];
  long starttime = System.currentTimeMillis();
  for (int i = 0; i < decksize; i++) {
    cards[i]=i;
  }
  double util = 0;
  for (int i = 0; i < iterations; i++) { //shuffle
    for (int c1 = cards.length - 1; c1 > 0; c1--) {
      int c2 = random.nextInt(c1 + 1);
      int tmp = cards[c1];
      cards[c1] = cards[c2];
      cards[c2] = tmp;
    }
    util0 += cfr(cards, "", 1, 1, 0); //player 0
    util1 += cfr(cards, "", 1, 1, 1); //player 1
    //util += cfr(cards, "", 1, 1); //utility added from one iteration
  }
  long elapsedtime = System.currentTimeMillis() - starttime;
  System.out.println("Average game value: " + util / iterations); //empirical, should also get game value based on average strategy expected value
  System.out.println("Number of nodes touched: " + nodecount);
  System.out.println("Time elapsed in milliseconds: " + elapsedtime);
  for (Node n : nodeMap.values())
    System.out.println(n);
}

private double cfr(int[] cards, String history, double p0, double p1, int player_iteration) {
  int plays = history.length();
  int player = plays % 2;
  int opponent = 1 - player;
  if (plays > 1) {
    boolean terminalPass = history.charAt(plays - 1) == 'p';
    boolean doubleBet = history.substring(plays - 2, plays).equals("bb");
    boolean isPlayerCardHigher = cards[player] > cards[opponent]; //player = 0, opponent = 1, player card is higher so boolean is 1
    if (terminalPass)
      if (history.equals("pp"))
        return isPlayerCardHigher ? 1 : -1;
      else
        return 1;
    else if (doubleBet)
      return isPlayerCardHigher ? 2 : -2;
  }
    String infoSet = cards[player] + history;
    nodecount = nodecount + 1;
    Node node = nodeMap.get(infoSet);
    if (node == null) {
      node = new Node();
      node.infoSet = infoSet;
      nodeMap.put(infoSet, node);
    }
    double[] strategy = node.getStrategy(player == 0 ? p0 : p1);
    double[] util = new double[NUM_ACTIONS];
    double nodeUtil = 0;
    //varies now depending on which player's iteration we're working with
    if (player != player_iteration)
      for (int a = 0; a < NUM_ACTIONS; a++) {
        strategySum[a] += strategy[a];
        return player == 0
        ? cfr(cards, nextHistory, p0 * strategy[a], p1, player_iteration)
        : cfr(cards, nextHistory, p0, p1 * strategy[a], player_iteration)
    else
      for (int a = 0; a < NUM_ACTIONS; a++) {
        String nextHistory = history + (a == 0 ? "p" : "b");
        util[a] = player == 0
          ? - cfr(cards, nextHistory, p0 * strategy[a], p1) //p0
          : - cfr(cards, nextHistory, p0, p1 * strategy[a]);
        nodeUtil += strategy[a] * util[a]; //weighted utility for the node
      }
      for (int a = 0; a < NUM_ACTIONS; a++) {
        double regret = util[a] - nodeUtil;
        node.regretSum[a] += (player == 0 ? p1 : p0) * regret; //store regretsum for each node->action
      }
      return nodeUtil;
}

public static void main(String[] args) {
  int iterations = 10000;
  int decksize = 5;
  new KuhnTrainer().train(iterations, decksize);
}

}
