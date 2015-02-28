package negotiator.util;

import negotiator.Bid;
import negotiator.Domain;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.ValueDiscrete;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.utility.UtilitySpace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by chad on 2/28/15.
 */
public class BayesLogic {
    private UtilitySpace utilitySpace;
    private ArrayList<TreeSet<ValueFrequency>> valueFrequencies;
    int totalBids = 0;
    int P;

    public BayesLogic(UtilitySpace utilitySpace, int P) throws Exception {
        Domain domain = utilitySpace.getDomain();
        List<Issue> issues = domain.getIssues();
        this.P = P;

        this.utilitySpace = utilitySpace;
        valueFrequencies = new ArrayList<TreeSet<ValueFrequency>>(issues.size() - 1);
        for(int i = 0; i < issues.size() - 1; ++i) {
            //System.out.println("Adding TreeSet<ValueFrequency> vfs[" + (i) + "]");
            valueFrequencies.add(i, new TreeSet<ValueFrequency>(new VFComp()));
            IssueDiscrete di = (IssueDiscrete)domain.getIssue(i);
            EvaluatorDiscrete ed = (EvaluatorDiscrete)utilitySpace.getEvaluator(i+1);
            for(ValueDiscrete v: di.getValues()) {
//					System.out.println("\tAdding ValueFrequency element[" + v.value + ", " + (ed.getEvaluation(v) * ed.getWeight()) + "]");
                valueFrequencies.get(i).add(new ValueFrequency(v, ed.getEvaluation(v) * ed.getWeight(), P));
            }
        }

    }

    public Bid bayesBid(Bid bestBid) {
        Bid b = new Bid(bestBid);
        for(int i = 0; i < valueFrequencies.size(); ++i) {
            Iterator<ValueFrequency> itr = valueFrequencies.get(i).descendingIterator();
            ValueDiscrete highestExpectedUtilVal = null;
            double highestExpectedUtil = -1.0;
            while(itr.hasNext()) {
                ValueFrequency vfr = itr.next();
                double FP = 1;
                for(int p = 0; p < P; p++) FP *= vfr.opponentFrequency[p];
                //Math.pow(totalBids,P)
                double EU = FP * vfr.utility;
                if(EU > highestExpectedUtil) {
                    highestExpectedUtilVal = vfr.value;
                    highestExpectedUtil = EU;
                }
            }
//            ValueDiscrete highestExpectedUtilVal = (ValueDiscrete)(lastBid.getValue(i + 1));
//            while(itr.hasNext()) {
//                ValueFrequency vfr = itr.next();
//                if((((double)vfr.opponentFrequency)/(double)totalBids) * vfr.utility >= highestExpectedUtil) {
//                    highestExpectedUtil = ((double)vfr.opponentFrequency/(double)totalBids * vfr.utility);
//                    highestExpectedUtilVal = vfr.value;
//                }
//            }
            b.setValue(i + 1, highestExpectedUtilVal);
        }
        return b;
    }

    // Update the frequency of our proposed values based on a bid we're about to make
    public void updateOurFrequency(Bid bid) throws Exception {
        for(int i = 0; i < valueFrequencies.size(); ++i) {
            Iterator<ValueFrequency> itr = valueFrequencies.get(i).descendingIterator();
            while(itr.hasNext()) {
                ValueFrequency vfr = itr.next();
                if(vfr.value.toString().compareTo(((ValueDiscrete)bid.getValue(i+1)).toString()) == 0) {
                    vfr.ourFrequency++;
                }
                break;
            }
        }
    }

    // Update the frequency of opponents' proposed values based on a bid an opponent just made
    public void updateOpponentFrequency(Bid bid, int P) throws Exception {
        ++totalBids;
        for(int i = 0; i < valueFrequencies.size(); ++i) {
            Iterator<ValueFrequency> itr = valueFrequencies.get(i).descendingIterator();
            while(itr.hasNext()) {
                ValueFrequency vfr = itr.next();
                //if(vfr.value.toString().compareTo(((ValueDiscrete)bid.getValue(i+1)).toString()) == 0) {
                if(vfr.value == bid.getValue(i+1)) {
                    vfr.opponentFrequency[P]++;
                    break;
                }
            }
        }
    }
}
