package negotiator.util;

import negotiator.Bid;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.Value;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.utility.UtilitySpace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 * Created by chad on 2/27/15.
 */
public class BestBids {
    private Bid minBid;
    private ArrayList<Tuple<Bid,Double>> preferredBids;

    int[] iprefs;
    int[][] vprefs;

    UtilitySpace utilitySpace;
    public BestBids(UtilitySpace utilitySpace) {
        this.utilitySpace = utilitySpace;
        try {
            this.genIssuePreferenceOrder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void genIssuePreferenceOrder() throws Exception {
        int N = utilitySpace.getDomain().getIssues().size();
        Tuple<Integer, Double> issues[] = new Tuple[N];
        iprefs = new int[N];
        vprefs = new int[N][];
        for(int i = 0; i < N; i++) {
            IssueDiscrete di = (IssueDiscrete)utilitySpace.getDomain().getIssue(i);
            EvaluatorDiscrete de = (EvaluatorDiscrete)utilitySpace.getEvaluator(i+1);
            issues[i] = new Tuple<Integer, Double>(i, de.getWeight());
            int M = di.getNumberOfValues();
            Tuple<Integer,Double> values[] = new Tuple[M];
            for(int j = 0; j < M; j++) {
                double w = de.getEvaluation(di.getValue(j));
                values[j] = new Tuple<Integer,Double>(j, w);
            }
            Arrays.sort(values);
            vprefs[i] = new int[M];
            for(int k = 0; k < M; k++)
                vprefs[i][k] = values[k].key;
        }
        Arrays.sort(issues);
        for(int i = 0; i < N; i++)
            iprefs[i] = issues[i].key;
    }

    public Bid getRandomBid(Random rand) {
        return preferredBids.get(rand.nextInt(preferredBids.size())).key;
    }

    public Bid getMinBid() {
        if(minBid == null) {
            minBid = Collections.min(preferredBids).key;
        }
        return minBid;
    }

    public void genPreferredBids(int count) throws Exception {
        Bid best = utilitySpace.getMaxUtilityBid();
        genIssuePreferenceOrder();
        System.out.println(Arrays.toString(iprefs));
        System.out.println(Arrays.toString(vprefs));
        maxCount = count;
        preferredBids = genBids(best, 0);
    }

    int maxCount;
    int count = 0;
    private ArrayList<Tuple<Bid,Double>> genBids(Bid b, int i) throws Exception {
        ArrayList<Tuple<Bid,Double>> bids = new ArrayList<Tuple<Bid,Double>>();
        int I = iprefs[i];
        int M = vprefs[I].length;
        if(i < iprefs.length-1) {
            for(int j = M-1; j >= 0 && count < maxCount; j--) {
                Bid b1 = new Bid(b);
                Value v = ((IssueDiscrete)utilitySpace.getDomain().getIssue(I)).getValue(vprefs[I][j]);
                b1.setValue(I+1,v);
                bids.addAll(genBids(b1, i + 1));
            }
        }
        else {
            for(int j = M-1; j >= 0 && count < maxCount; j--) {
                Bid b1 = new Bid(b);
                Value v = ((IssueDiscrete)utilitySpace.getDomain().getIssue(I)).getValue(vprefs[I][j]);
                b1.setValue(I+1,v);
                bids.add(new Tuple<Bid,Double>(b1,utilitySpace.getUtility(b1)));
                count++;
            }
        }
        return bids;
    }
}
