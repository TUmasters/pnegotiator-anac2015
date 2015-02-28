package negotiator.groupn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.DeadlineType;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.ValueDiscrete;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.Evaluator;
import negotiator.utility.UtilitySpace;

import java.util.HashMap;

/**
 * This is your negotiation party.
 */
public class GroupO extends AbstractNegotiationParty {

    /**
     * Please keep this constructor. This is called by genius.
     *
     * @param utilitySpace Your utility space.
     * @param deadlines The deadlines set for this negotiation.
     * @param timeline Value counting from 0 (start) to 1 (end).
     * @param randomSeed If you use any randomization, use this seed for it.
     */

    int maxChoice =0;
    double agreeVal =1;
    Bid lastBid;
    int[] issueOrder;
    int[][] issueValOrder;
    HashMap<AgentID, Party> parties;
    public GroupO(UtilitySpace utilitySpace,
                  Map<DeadlineType, Object> deadlines,
                  Timeline timeline,
                  long randomSeed) {
        // Make sure that this constructor calls it's parent.
        super(utilitySpace, deadlines, timeline, randomSeed);
        initializeCounts(utilitySpace);
    }

    /**
     * Each round this method gets called and ask you to accept or offer. The first party in
     * the first round is a bit different, it can only propose an offer.
     *
     * @param validActions Either a list containing both accept and offer or only offer.
     * @return The chosen action.
     */
    @Override
    public Action chooseAction(List<Class> validActions) {

        // with 50% chance, counter offer
        // if we are the first party, also offer.
        double d=0;
        try {
            d = utilitySpace.getUtility(lastBid);
        } catch (Exception e1) {
            // TODO Auto-generated catch block

        }
        agreeVal*=.999;
        Bid b = generateRandomBid();
        try {
            while(utilitySpace.getUtility(b) < this.stopValue())
            {
                b = generateRandomBid();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //Bid b = createBid();
        Iterator it = parties.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry pair = (Map.Entry)it.next();
            Party p = (Party) pair.getValue();
            System.out.println(pair.getKey() + " = " + p.getPredictedUtility(b, utilitySpace));
            // it.remove(); // avoids a ConcurrentModificationException
        }
        if(d==1)
            return new Accept();
        else if (!validActions.contains(Accept.class) || Math.random() > 0) {
            return new Offer(b);
        }
        else {
            return new Accept();
        }
    }


    /**
     * All offers proposed by the other parties will be received as a message.
     * You can use this information to your advantage, for example to predict their utility.
     *
     * @param sender The party that did the action.
     * @param action The action that party did.
     */
    @Override
    public void receiveMessage(Object sender, Action action) {
        super.receiveMessage(sender, action);
        Bid b = Action.getBidFromAction(action);
        lastBid=b;
        System.out.println(action.getAgent());
        if(!parties.containsKey(action.getAgent()))
            parties.put(action.getAgent(), new Party(utilitySpace.getNrOfEvaluators(),maxChoice));

        Party p = parties.get(action.getAgent());
        if(b!=null && !action.equals(new Accept()) )
        {
            ArrayList<Issue> issues = b.getIssues();
            try {
                System.out.println(issues);
                for(int i =0;i<issues.size();i++)
                {
                    IssueDiscrete id = (IssueDiscrete)utilitySpace.getIssue(i);
                    int choice = id.getValueIndex((ValueDiscrete)b.getValue(i+1));
                    p.counts[i][choice]++;
                }
                p.calcWeights();
                p.show();
                System.out.println(b.getValue(1));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                System.out.println(b);
                e.printStackTrace();
            }
        }
        // Here you can listen to other parties' messages
    }

    public void initializeCounts(UtilitySpace us)
    {

        int noOfIssues = us.getNrOfEvaluators();
        issueOrder = new int[noOfIssues];
        for(int i=0;i<issueOrder.length;i++)
        {
            issueOrder[i]=i;
        }
        for(int i=0;i<issueOrder.length;i++)
        {
            for(int j=i+1;j<issueOrder.length;j++)
            {
                if(us.getWeight(issueOrder[i])<us.getWeight(issueOrder[j]))
                {
                    int temp = issueOrder[i];
                    issueOrder[i]=issueOrder[j];
                    issueOrder[j]=temp;
                }
            }
        }
        for(int i =0; i<noOfIssues ; i++)
        {
            int tempmax = ((IssueDiscrete)(us.getIssue(i))).getNumberOfValues();
            if(tempmax>maxChoice)
                maxChoice = tempmax;
        }
        issueValOrder = new int[noOfIssues][maxChoice];
        Bid temp1 = new Bid();
        Bid temp2 = new Bid();
        System.out.println(utilitySpace.getEvaluators());
        for(int i =0; i<noOfIssues ; i++)
        {
            for(int j =0; j<maxChoice ; j++)
            {
                issueValOrder[i][j] = j+1;
            }
        }
        //TODO Not Ordering
        for(int i =0; i<noOfIssues ; i++)
        {
            for(int j =0; j<maxChoice ; j++)
            {
                try {
                    temp1.setValue(i, ((IssueDiscrete)us.getIssue(i)).getValue(j));
                } catch (Exception e) {
                    continue;
                }

                for(int k =j; k<maxChoice ; k++)
                {
                    try {
                        temp2.setValue(i, ((IssueDiscrete)us.getIssue(i)).getValue(k));
                    } catch (Exception e) {
                        continue;
                    }
                    try {
                        if(us.getEvaluation(i, temp1) < us.getEvaluation(i, temp2))

                        {
                            int t = issueValOrder[i][j];
                            issueValOrder[i][j]=issueValOrder[i][k];
                            issueValOrder[i][k]=t;
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
            System.out.println(issueValOrder[i][0]);
        }
        parties = new HashMap<AgentID, Party>();
    }

    double stopValue()
    {
        return agreeVal;
    }

    public Bid createBid()
    {
        Vector<Bid> possBids = this.getFeasibleBids();
        return possBids.get(0);
    }
    public Vector<Bid> getFeasibleBids()
    {
        Vector<Bid> bids = new Vector<Bid>();
        Bid b=new Bid();
        for(int i = 0; i< issueOrder.length;i++)
        {
            try {
                System.out.println(i);
                b.setValue(issueOrder[i], ((IssueDiscrete)(utilitySpace.getIssue(issueOrder[i]))).getValue(issueValOrder[issueOrder[i]][0]));
            } catch (Exception e) {

            }

        }
        bids = recurseBids(b, bids,0);
        System.out.println("Vector Size:" +bids.size());
        return bids;
    }

    public Vector<Bid> recurseBids(Bid b,Vector<Bid> v,int is)
    {	Vector<Bid> v1 = new Vector<Bid>();
        if(is==issueOrder.length)
        {
            v.addElement(b);
            return v;
        }
        for(int i=0;i<issueValOrder[is].length;i++)
        {
            Bid b1=new Bid(b);
            System.out.println(b1);
            try {


                b1.setValue(is, ((IssueDiscrete)(utilitySpace.getIssue(is))).getValue(issueValOrder[issueOrder[is]][i]));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if(utilitySpace.getUtility(b1)>this.stopValue())
                {
                    v1.addAll(recurseBids(b1, v, is+1));
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return v1;
    }
}
